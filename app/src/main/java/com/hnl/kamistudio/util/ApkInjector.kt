package com.hnl.kamistudio.util

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * APK一键注入引擎
 * 流程：解压APK → AXmlEditor修改Manifest → 注入预编译dex → 写入卡密列表 → 重打包 → apksig签名
 */
object ApkInjector {

    private const val TAG = "ApkInjector"
    private const val VERIFY_ACTIVITY = "com.hnl.kamiverify.KamiVerifyActivity"

    data class InjectConfig(
        val verifyTitle: String = "卡密验证",
        val verifySubtitle: String = "请输入卡密以继续使用",
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

            // 用PackageManager获取启动Activity
            onProgress(10, "解析APK信息...")
            val pm = context.packageManager
            val pkgInfo = pm.getPackageArchiveInfo(inputFile.absolutePath, PackageManager.GET_ACTIVITIES)
                ?: return InjectResult(false, message = "无法解析APK")
            val packageName = pkgInfo.packageName
            val launchIntent = pm.getLaunchIntentForPackage(packageName)
            val targetActivity = launchIntent?.component?.className
                ?: pkgInfo.activities?.firstOrNull()?.name
                ?: return InjectResult(false, message = "无法找到启动Activity")

            Log.d(TAG, "包名: $packageName, 启动Activity: $targetActivity")

            onProgress(15, "解压APK...")
            val workDir = File(context.cacheDir, "inject_${System.currentTimeMillis()}")
            workDir.deleteRecursively()
            workDir.mkdirs()
            unzip(inputFile, workDir)
            inputFile.delete()

            onProgress(25, "修改AndroidManifest...")
            val manifestFile = File(workDir, "AndroidManifest.xml")
            val doc = AXmlEditor.parse(manifestFile.readBytes())

            // 移除原启动Activity的LAUNCHER
            val launcherActivity = AXmlEditor.findLauncherActivity(doc)
            if (launcherActivity != null) {
                AXmlEditor.removeLauncherFilter(doc, launcherActivity)
            }

            // 添加卡密验证Activity
            AXmlEditor.addVerifyActivity(doc, VERIFY_ACTIVITY)

            // 写回Manifest
            manifestFile.writeBytes(AXmlEditor.serialize(doc))
            Log.d(TAG, "Manifest修改完成")

            onProgress(40, "注入卡密验证模块...")
            // 从assets读取预编译的dex
            val dexInputStream = context.assets.open("dex/kami_verify.dex")
            val dexBytes = dexInputStream.readBytes()
            dexInputStream.close()

            // 替换dex中的占位符（目标Activity、标题、副标题）
            val modifiedDex = replaceDexPlaceholders(dexBytes, targetActivity, config.verifyTitle, config.verifySubtitle)

            // 检查是否已有classes2.dex
            var dexIndex = 2
            while (File(workDir, "classes$dexIndex.dex").exists()) {
                dexIndex++
            }
            File(workDir, "classes$dexIndex.dex").writeBytes(modifiedDex)

            onProgress(55, "写入卡密数据...")
            val assetsDir = File(workDir, "assets")
            assetsDir.mkdirs()
            File(assetsDir, "kami_list.txt").writeText(config.kamiList.joinToString("\n"))

            onProgress(65, "重新打包...")
            val unsignedApk = File(workDir, "unsigned.apk")
            repack(workDir, unsignedApk)

            onProgress(80, "签名...")
            val outDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "KamiStudio")
            outDir.mkdirs()
            val outputApk = File(outDir, "卡密版_${System.currentTimeMillis()}.apk")
            signApk(context, unsignedApk, outputApk)

            workDir.deleteRecursively()
            onProgress(100, "完成！")
            InjectResult(true, outputApk, "注入成功，已保存到 Download/KamiStudio/")
        } catch (e: Throwable) {
            Log.e(TAG, "注入失败", e)
            InjectResult(false, message = "注入失败: ${e.message ?: e.javaClass.simpleName}")
        }
    }

    /**
     * 替换dex中的字符串占位符
     * 注意：dex中的字符串是UTF-16LE格式，替换时长度必须相同或更短（用null填充）
     */
    private fun replaceDexPlaceholders(dexBytes: ByteArray, targetActivity: String, title: String, subtitle: String): ByteArray {
        var result = dexBytes

        // 替换目标Activity占位符
        result = replaceStringInDex(result, "TARGET_ACTIVITY_PLACEHOLDER", targetActivity)
        // 替换标题
        result = replaceStringInDex(result, "VERIFY_TITLE_PLACEHOLDER", title)
        // 替换副标题
        result = replaceStringInDex(result, "VERIFY_SUBTITLE_PLACEHOLDER", subtitle)

        return result
    }

    /**
     * 在dex的字符串数据中替换字符串
     * dex中的字符串是MUTF-8格式，以0x00结尾
     * 替换时新字符串长度不能超过原字符串长度，多余部分用0x00填充
     */
    private fun replaceStringInDex(dexBytes: ByteArray, oldStr: String, newStr: String): ByteArray {
        val oldBytes = oldStr.toByteArray(Charsets.UTF_8)
        val newBytes = newStr.toByteArray(Charsets.UTF_8)

        if (newBytes.size > oldBytes.size) {
            Log.w(TAG, "新字符串太长，跳过替换: $oldStr -> $newStr")
            return dexBytes
        }

        // 查找oldBytes的位置（后面跟0x00）
        var pos = 0
        while (pos < dexBytes.size - oldBytes.size) {
            var found = true
            for (i in oldBytes.indices) {
                if (dexBytes[pos + i] != oldBytes[i]) {
                    found = false
                    break
                }
            }
            if (found && pos + oldBytes.size < dexBytes.size && dexBytes[pos + oldBytes.size] == 0x00.toByte()) {
                // 替换
                System.arraycopy(newBytes, 0, dexBytes, pos, newBytes.size)
                // 多余部分用0x00填充
                for (i in newBytes.size until oldBytes.size) {
                    dexBytes[pos + i] = 0x00
                }
                break // 只替换第一个匹配
            }
            pos++
        }
        return dexBytes
    }

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

    private fun signApk(context: Context, unsigned: File, output: File) {
        // 用apksig签名
        val keystoreFile = File(context.filesDir, "kami_debug.keystore")
        if (!keystoreFile.exists()) createKeystore(keystoreFile)

        val ks = java.security.KeyStore.getInstance("JKS")
        FileInputStream(keystoreFile).use { ks.load(it, "android".toCharArray()) }
        val privateKey = ks.getKey("androiddebugkey", "android".toCharArray()) as java.security.PrivateKey
        val cert = ks.getCertificate("androiddebugkey") as java.security.cert.X509Certificate

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
        val kpg = java.security.KeyPairGenerator.getInstance("RSA")
        kpg.initialize(2048)
        val kp = kpg.generateKeyPair()

        val owner = org.bouncycastle.asn1.x500.X500Name("CN=KamiStudio, O=KamiStudio, C=CN")
        val pubKeyInfo = org.bouncycastle.asn1.x509.SubjectPublicKeyInfo.getInstance(kp.public.encoded)
        val builder = org.bouncycastle.cert.X509v3CertificateBuilder(
            owner,
            java.math.BigInteger.valueOf(System.currentTimeMillis()),
            java.util.Date(System.currentTimeMillis() - 86400000L),
            java.util.Date(System.currentTimeMillis() + 30L * 365 * 86400000L),
            owner,
            pubKeyInfo
        )
        val signer = org.bouncycastle.operator.jcajce.JcaContentSignerBuilder("SHA256withRSA").build(kp.private)
        val certHolder = builder.build(signer)
        val cert = org.bouncycastle.cert.jcajce.JcaX509CertificateConverter().getCertificate(certHolder)

        val ks = java.security.KeyStore.getInstance("JKS")
        ks.load(null, null)
        ks.setKeyEntry("androiddebugkey", kp.private, "android".toCharArray(), arrayOf(cert))
        FileOutputStream(file).use { ks.store(it, "android".toCharArray()) }
    }
}
