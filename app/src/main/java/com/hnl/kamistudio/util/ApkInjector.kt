package com.hnl.kamistudio.util

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import org.jf.dexlib2.Opcodes
import org.jf.dexlib2.iface.ClassDef
import org.jf.dexlib2.writer.DexWriter
import org.jf.smali.Smali
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.cert.X509Certificate
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * APK卡密注入引擎
 * 流程：解压APK → 修改Manifest → 注入卡密验证dex → 写入卡密列表 → 重打包 → v1签名
 */
object ApkInjector {

    private const val TAG = "ApkInjector"

    data class InjectConfig(
        val verifyTitle: String = "请输入卡密",
        val verifySubtitle: String = "本软件需要卡密验证后使用",
        val kamiList: List<String> = emptyList()
    )

    data class InjectResult(
        val success: Boolean,
        val outputFile: File? = null,
        val message: String = ""
    )

    fun inject(context: Context, apkUri: Uri, config: InjectConfig, onProgress: (Int, String) -> Unit): InjectResult {
        return try {
            onProgress(5, "读取APK...")
            val inputFile = File(context.cacheDir, "input_${System.currentTimeMillis()}.apk")
            context.contentResolver.openInputStream(apkUri)?.use { input ->
                FileOutputStream(inputFile).use { output -> input.copyTo(output) }
            } ?: return InjectResult(false, message = "无法读取APK")

            onProgress(10, "解压APK...")
            val workDir = File(context.cacheDir, "inject_${System.currentTimeMillis()}")
            workDir.deleteRecursively()
            workDir.mkdirs()
            unzip(inputFile, workDir)
            inputFile.delete()

            onProgress(20, "解析Manifest...")
            val manifestInfo = readManifestInfo(workDir)
            if (manifestInfo.launcherActivity.isEmpty()) {
                return InjectResult(false, message = "无法识别APK的启动界面")
            }
            Log.d(TAG, "包名: ${manifestInfo.packageName}, 启动: ${manifestInfo.launcherActivity}")

            onProgress(35, "修改Manifest...")
            patchManifest(workDir, manifestInfo.launcherActivity, config)

            onProgress(50, "生成卡密验证模块...")
            generateVerifyDex(workDir, manifestInfo.launcherActivity, config)

            onProgress(65, "写入卡密数据...")
            writeKamiAssets(workDir, config.kamiList)

            onProgress(75, "重新打包...")
            val unsigned = File(workDir, "unsigned.apk")
            repack(workDir, unsigned)

            onProgress(85, "签名...")
            val outDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "KamiStudio")
            outDir.mkdirs()
            val output = File(outDir, "卡密版_${System.currentTimeMillis()}.apk")
            signV1(unsigned, output, context)

            workDir.deleteRecursively()
            onProgress(100, "完成")
            InjectResult(true, output, "注入成功，已保存到 Download/KamiStudio/")
        } catch (e: Throwable) {
            Log.e(TAG, "注入失败", e)
            InjectResult(false, message = "注入失败: ${e.message ?: e.javaClass.simpleName}")
        }
    }

    // ===== 解压 =====
    private fun unzip(apk: File, dir: File) {
        ZipInputStream(FileInputStream(apk)).use { zis ->
            var entry: ZipEntry?
            while (zis.nextEntry.also { entry = it } != null) {
                val f = File(dir, entry!!.name)
                if (entry!!.isDirectory) f.mkdirs()
                else {
                    f.parentFile?.mkdirs()
                    FileOutputStream(f).use { zis.copyTo(it) }
                }
                zis.closeEntry()
            }
        }
    }

    // ===== 打包 =====
    private fun repack(dir: File, out: File) {
        ZipOutputStream(FileOutputStream(out)).use { zos ->
            dir.walkTopDown()
                .filter { it.isFile && it.name != "unsigned.apk" && !it.path.contains("META-INF") }
                .forEach { f ->
                    val name = f.relativeTo(dir).path.replace("\\", "/")
                    zos.putNextEntry(ZipEntry(name))
                    FileInputStream(f).use { it.copyTo(zos) }
                    zos.closeEntry()
                }
        }
    }

    // ===== 读取Manifest信息 =====
    data class ManifestInfo(val packageName: String, val launcherActivity: String)

    private fun readManifestInfo(dir: File): ManifestInfo {
        val manifestFile = File(dir, "AndroidManifest.xml")
        val parser = org.jf.dexlib2.axml.AXmlParser(manifestFile.readBytes())
        val root = parser.parse()
        var pkg = ""
        var launcher = ""

        fun walk(node: org.jf.dexlib2.axml.AXmlNode) {
            if (node.tag == "manifest") {
                pkg = node.attributes.firstOrNull { it.name == "package" }?.value?.toString() ?: ""
            }
            if (node.tag == "activity") {
                val name = node.attributes.firstOrNull { it.name == "name" }?.value?.toString() ?: ""
                val isLauncher = node.children.any { it.tag == "intent-filter" &&
                    it.children.any { c -> c.tag == "category" &&
                        c.attributes.firstOrNull { a -> a.name == "name" }?.value?.toString() == "android.intent.category.LAUNCHER" } }
                if (isLauncher && launcher.isEmpty()) {
                    launcher = if (name.startsWith(".")) "$pkg$name" else name
                }
            }
            node.children.forEach { walk(it) }
        }
        walk(root)
        return ManifestInfo(pkg, launcher)
    }

    // ===== 修改Manifest =====
    private fun patchManifest(dir: File, originalActivity: String, config: InjectConfig) {
        val manifestFile = File(dir, "AndroidManifest.xml")
        val parser = org.jf.dexlib2.axml.AXmlParser(manifestFile.readBytes())
        val root = parser.parse()
        val app = findNode(root, "application") ?: throw Exception("Manifest中找不到application节点")

        // 移除原启动Activity的LAUNCHER category
        val shortName = originalActivity.substringAfterLast('.')
        for (act in app.children.filter { it.tag == "activity" }) {
            val an = act.attributes.firstOrNull { it.name == "name" }?.value?.toString() ?: ""
            if (an == originalActivity || an == ".$shortName" || an == shortName) {
                act.children.filter { it.tag == "intent-filter" }.forEach { filter ->
                    filter.children.removeAll { it.tag == "category" &&
                        it.attributes.firstOrNull { a -> a.name == "name" }?.value?.toString() == "android.intent.category.LAUNCHER" }
                }
            }
        }

        // 添加卡密验证Activity
        val verifyAct = org.jf.dexlib2.axml.AXmlNode("activity").apply {
            attributes.add(org.jf.dexlib2.axml.AXmlAttribute("name", "com.hnl.kamiverify.KamiVerifyActivity"))
            attributes.add(org.jf.dexlib2.axml.AXmlAttribute("exported", "true"))
            attributes.add(org.jf.dexlib2.axml.AXmlAttribute("theme", "@android:style/Theme.Translucent.NoTitleBar"))
            children.add(org.jf.dexlib2.axml.AXmlNode("intent-filter").apply {
                children.add(org.jf.dexlib2.axml.AXmlNode("action").apply {
                    attributes.add(org.jf.dexlib2.axml.AXmlAttribute("name", "android.intent.action.MAIN"))
                })
                children.add(org.jf.dexlib2.axml.AXmlNode("category").apply {
                    attributes.add(org.jf.dexlib2.axml.AXmlAttribute("name", "android.intent.category.LAUNCHER"))
                })
            })
            children.add(org.jf.dexlib2.axml.AXmlNode("meta-data").apply {
                attributes.add(org.jf.dexlib2.axml.AXmlAttribute("name", "kami_target"))
                attributes.add(org.jf.dexlib2.axml.AXmlAttribute("value", originalActivity))
            })
        }
        app.children.add(verifyAct)

        // 序列化回二进制
        val serializer = org.jf.dexlib2.axml.AXmlSerializer()
        manifestFile.writeBytes(serializer.serialize(root))
    }

    private fun findNode(node: org.jf.dexlib2.axml.AXmlNode, tag: String): org.jf.dexlib2.axml.AXmlNode? {
        if (node.tag == tag) return node
        return node.children.firstNotNullOfOrNull { findNode(it, tag) }
    }

    // ===== 生成卡密验证dex =====
    private fun generateVerifyDex(dir: File, targetActivity: String, config: InjectConfig) {
        val smali = buildVerifySmali(targetActivity, config.verifyTitle, config.verifySubtitle)
        val opcodes = Opcodes.forApi(26)
        val classes: List<ClassDef> = Smali.assemble(smali, opcodes)

        // 检查是否已有classes2.dex（多dex情况）
        val dexFile = File(dir, "classes2.dex")
        val writer = DexWriter(opcodes)
        writer.writeTo(FileOutputStream(dexFile), classes)
    }

    /**
     * 构建完整的smali代码（主类 + 两个内部类监听器）
     */
    private fun buildVerifySmali(target: String, title: String, subtitle: String): String {
        val targetClass = target.replace('.', '/')
        return """
.class public Lcom/hnl/kamiverify/KamiVerifyActivity;
.super Landroid/app/Activity;
.source "KamiVerifyActivity.java"

.field private et:Landroid/widget/EditText;
.field private list:Ljava/util/ArrayList;
.field private target:Ljava/lang/String;

.method public constructor <init>()V
    .registers 1
    invoke-direct {p0}, Landroid/app/Activity;-><init>()V
    return-void
.end method

.method protected onCreate(Landroid/os/Bundle;)V
    .registers 5
    invoke-super {p0, p1}, Landroid/app/Activity;->onCreate(Landroid/os/Bundle;)V

    # init list
    new-instance v0, Ljava/util/ArrayList;
    invoke-direct {v0}, Ljava/util/ArrayList;-><init>()V
    iput-object v0, p0, Lcom/hnl/kamiverify/KamiVerifyActivity;->list:Ljava/util/ArrayList;

    # set target
    const-string v0, "$target"
    iput-object v0, p0, Lcom/hnl/kamiverify/KamiVerifyActivity;->target:Ljava/lang/String;

    # load kami list from assets
    :try_start
    invoke-virtual {p0}, Lcom/hnl/kamiverify/KamiVerifyActivity;->getAssets()Landroid/content/res/AssetManager;
    move-result-object v0
    const-string v1, "kami_list.txt"
    invoke-virtual {v0, v1}, Landroid/content/res/AssetManager;->open(Ljava/lang/String;)Ljava/io/InputStream;
    move-result-object v0
    new-instance v1, Ljava/io/BufferedReader;
    new-instance v2, Ljava/io/InputStreamReader;
    invoke-direct {v2, v0}, Ljava/io/InputStreamReader;-><init>(Ljava/io/InputStream;)V
    invoke-direct {v1, v2}, Ljava/io/BufferedReader;-><init>(Ljava/io/Reader;)V
    :read_loop
    invoke-virtual {v1}, Ljava/io/BufferedReader;->readLine()Ljava/lang/String;
    move-result-object v0
    if-eqz v0, :read_done
    iget-object v2, p0, Lcom/hnl/kamiverify/KamiVerifyActivity;->list:Ljava/util/ArrayList;
    invoke-virtual {v2, v0}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z
    goto :read_loop
    :read_done
    invoke-virtual {v1}, Ljava/io/BufferedReader;->close()V
    :try_end
    .catch Ljava/lang/Exception; {:try_start .. :try_end} :catch_all

    :catch_all
    # show dialog
    invoke-direct {p0}, Lcom/hnl/kamiverify/KamiVerifyActivity;->showDialog()V
    return-void
.end method

.method private showDialog()V
    .registers 5
    new-instance v0, Landroid/app/AlertDialog${'$'}Builder;
    invoke-direct {v0, p0}, Landroid/app/AlertDialog${'$'}Builder;-><init>(Landroid/content/Context;)V

    const-string v1, "$title"
    invoke-virtual {v0, v1}, Landroid/app/AlertDialog${'$'}Builder;->setTitle(Ljava/lang/CharSequence;)Landroid/app/AlertDialog${'$'}Builder;

    const-string v1, "$subtitle"
    invoke-virtual {v0, v1}, Landroid/app/AlertDialog${'$'}Builder;->setMessage(Ljava/lang/CharSequence;)Landroid/app/AlertDialog${'$'}Builder;

    new-instance v1, Landroid/widget/EditText;
    invoke-direct {v1, p0}, Landroid/widget/EditText;-><init>(Landroid/content/Context;)V
    iput-object v1, p0, Lcom/hnl/kamiverify/KamiVerifyActivity;->et:Landroid/widget/EditText;
    const-string v2, "请输入卡密"
    invoke-virtual {v1, v2}, Landroid/widget/EditText;->setHint(Ljava/lang/CharSequence;)V
    invoke-virtual {v0, v1}, Landroid/app/AlertDialog${'$'}Builder;->setView(Landroid/view/View;)Landroid/app/AlertDialog${'$'}Builder;

    new-instance v1, Lcom/hnl/kamiverify/KamiVerifyActivity${'$'}1;
    invoke-direct {v1, p0}, Lcom/hnl/kamiverify/KamiVerifyActivity${'$'}1;-><init>(Lcom/hnl/kamiverify/KamiVerifyActivity;)V
    const-string v2, "验证"
    invoke-virtual {v0, v2, v1}, Landroid/app/AlertDialog${'$'}Builder;->setPositiveButton(Ljava/lang/CharSequence;Landroid/content/DialogInterface${'$'}OnClickListener;)Landroid/app/AlertDialog${'$'}Builder;

    new-instance v1, Lcom/hnl/kamiverify/KamiVerifyActivity${'$'}2;
    invoke-direct {v1, p0}, Lcom/hnl/kamiverify/KamiVerifyActivity${'$'}2;-><init>(Lcom/hnl/kamiverify/KamiVerifyActivity;)V
    const-string v2, "退出"
    invoke-virtual {v0, v2, v1}, Landroid/app/AlertDialog${'$'}Builder;->setNegativeButton(Ljava/lang/CharSequence;Landroid/content/DialogInterface${'$'}OnClickListener;)Landroid/app/AlertDialog${'$'}Builder;

    const/4 v1, 0x0
    invoke-virtual {v0, v1}, Landroid/app/AlertDialog${'$'}Builder;->setCancelable(Z)Landroid/app/AlertDialog${'$'}Builder;
    invoke-virtual {v0}, Landroid/app/AlertDialog${'$'}Builder;->create()Landroid/app/AlertDialog;
    move-result-object v0
    invoke-virtual {v0}, Landroid/app/AlertDialog;->show()V
    return-void
.end method

.method private check(Ljava/lang/String;)Z
    .registers 3
    iget-object v0, p0, Lcom/hnl/kamiverify/KamiVerifyActivity;->list:Ljava/util/ArrayList;
    invoke-virtual {v0, p1}, Ljava/util/ArrayList;->contains(Ljava/lang/Object;)Z
    move-result v0
    return v0
.end method

.method private launch()V
    .registers 4
    iget-object v0, p0, Lcom/hnl/kamiverify/KamiVerifyActivity;->target:Ljava/lang/String;
    if-eqz v0, :done
    new-instance v1, Landroid/content/Intent;
    invoke-direct {v1}, Landroid/content/Intent;-><init>()V
    invoke-virtual {p0}, Lcom/hnl/kamiverify/KamiVerifyActivity;->getPackageName()Ljava/lang/String;
    move-result-object v2
    invoke-virtual {v1, v2, v0}, Landroid/content/Intent;->setClassName(Ljava/lang/String;Ljava/lang/String;)Landroid/content/Intent;
    const/high16 v2, 0x1000
    invoke-virtual {v1, v2}, Landroid/content/Intent;->addFlags(I)Landroid/content/Intent;
    invoke-virtual {p0, v1}, Lcom/hnl/kamiverify/KamiVerifyActivity;->startActivity(Landroid/content/Intent;)V
    invoke-virtual {p0}, Lcom/hnl/kamiverify/KamiVerifyActivity;->finish()V
    :done
    return-void
.end method


# ===== 内部类1：确定按钮监听 =====
.class Lcom/hnl/kamiverify/KamiVerifyActivity${'$'}1;
.super Ljava/lang/Object;
.implements Landroid/content/DialogInterface${'$'}OnClickListener;
.source "KamiVerifyActivity.java"

.field final synthetic this${'$'}0:Lcom/hnl/kamiverify/KamiVerifyActivity;

.method constructor <init>(Lcom/hnl/kamiverify/KamiVerifyActivity;)V
    .registers 2
    iput-object p1, p0, Lcom/hnl/kamiverify/KamiVerifyActivity${'$'}1;->this${'$'}0:Lcom/hnl/kamiverify/KamiVerifyActivity;
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    return-void
.end method

.method public onClick(Landroid/content/DialogInterface;I)V
    .registers 4
    iget-object v0, p0, Lcom/hnl/kamiverify/KamiVerifyActivity${'$'}1;->this${'$'}0:Lcom/hnl/kamiverify/KamiVerifyActivity;
    iget-object v1, v0, Lcom/hnl/kamiverify/KamiVerifyActivity;->et:Landroid/widget/EditText;
    invoke-virtual {v1}, Landroid/widget/EditText;->getText()Landroid/text/Editable;
    move-result-object v1
    invoke-virtual {v1}, Ljava/lang/Object;->toString()Ljava/lang/String;
    move-result-object v1
    invoke-virtual {v0, v1}, Lcom/hnl/kamiverify/KamiVerifyActivity;->check(Ljava/lang/String;)Z
    move-result v1
    if-eqz v1, :fail
    invoke-virtual {v0}, Lcom/hnl/kamiverify/KamiVerifyActivity;->launch()V
    goto :end
    :fail
    const-string v1, "卡密错误，请重新输入"
    const/4 v2, 0x0
    invoke-static {v0, v1, v2}, Landroid/widget/Toast;->makeText(Landroid/content/Context;Ljava/lang/CharSequence;I)Landroid/widget/Toast;
    move-result-object v0
    invoke-virtual {v0}, Landroid/widget/Toast;->show()V
    :end
    return-void
.end method


# ===== 内部类2：取消按钮监听 =====
.class Lcom/hnl/kamiverify/KamiVerifyActivity${'$'}2;
.super Ljava/lang/Object;
.implements Landroid/content/DialogInterface${'$'}OnClickListener;
.source "KamiVerifyActivity.java"

.field final synthetic this${'$'}0:Lcom/hnl/kamiverify/KamiVerifyActivity;

.method constructor <init>(Lcom/hnl/kamiverify/KamiVerifyActivity;)V
    .registers 2
    iput-object p1, p0, Lcom/hnl/kamiverify/KamiVerifyActivity${'$'}2;->this${'$'}0:Lcom/hnl/kamiverify/KamiVerifyActivity;
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    return-void
.end method

.method public onClick(Landroid/content/DialogInterface;I)V
    .registers 2
    iget-object v0, p0, Lcom/hnl/kamiverify/KamiVerifyActivity${'$'}2;->this${'$'}0:Lcom/hnl/kamiverify/KamiVerifyActivity;
    invoke-virtual {v0}, Lcom/hnl/kamiverify/KamiVerifyActivity;->finish()V
    return-void
.end method
"""
    }

    // ===== 写入卡密列表到assets =====
    private fun writeKamiAssets(dir: File, list: List<String>) {
        val assets = File(dir, "assets")
        assets.mkdirs()
        File(assets, "kami_list.txt").writeText(list.joinToString("\n"))
    }

    // ===== 签名（apksig官方库，v1+v2）=====
    private fun signV1(unsigned: File, output: File, context: Context) {
        val keystoreFile = File(context.filesDir, "kami_debug.keystore")
        if (!keystoreFile.exists()) createKeystore(keystoreFile)

        val ks = KeyStore.getInstance("JKS")
        FileInputStream(keystoreFile).use { ks.load(it, "android".toCharArray()) }
        val privateKey = ks.getKey("androiddebugkey", "android".toCharArray()) as java.security.PrivateKey
        val cert = ks.getCertificate("androiddebugkey") as X509Certificate

        val signerConfig = com.android.apksig.ApkSigner.SignerConfig.Builder(
            "KamiStudio",
            privateKey,
            listOf(cert)
        ).build()

        val signer = com.android.apksig.ApkSigner.Builder(
            listOf(signerConfig),
            unsigned
        )
        signer.setOutputApk(output)
        signer.setV1SigningEnabled(true)
        signer.setV2SigningEnabled(true)
        signer.build().sign()
    }

    private fun createKeystore(file: File) {
        val kpg = KeyPairGenerator.getInstance("RSA")
        kpg.initialize(2048)
        val kp = kpg.generateKeyPair()

        val owner = org.bouncycastle.asn1.x500.X500Name("CN=KamiStudio, O=KamiStudio, C=CN")
        val builder = org.bouncycastle.cert.X509v3CertificateBuilder(
            owner,
            java.math.BigInteger.valueOf(System.currentTimeMillis()),
            java.util.Date(System.currentTimeMillis() - 86400000L),
            java.util.Date(System.currentTimeMillis() + 30L * 365 * 86400000L),
            owner,
            org.bouncycastle.operator.jcajce.JcaContentSignerBuilder("SHA256withRSA").build(kp.private),
            org.bouncycastle.operator.jcajce.JcaSubjectPublicKeyInfoBuilder().build(kp.public)
        )
        val cert = builder.build(org.bouncycastle.operator.jcajce.JcaCertStore(emptyList()))

        val ks = KeyStore.getInstance("JKS")
        ks.load(null, null)
        ks.setKeyEntry("androiddebugkey", kp.private, "android".toCharArray(), arrayOf(cert))
        FileOutputStream(file).use { ks.store(it, "android".toCharArray()) }
    }
}
