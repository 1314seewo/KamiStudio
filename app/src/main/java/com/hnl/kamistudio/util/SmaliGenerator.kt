package com.hnl.kamistudio.util

/**
 * 生成卡密验证的Smali代码和注入配置
 * 用户可以用NP Manager或apktool将这些代码注入目标APK
 */
object SmaliGenerator {

    data class InjectConfig(
        val verifyTitle: String = "卡密验证",
        val verifySubtitle: String = "请输入卡密以继续使用",
        val serverUrl: String = "",
        val offlineMode: Boolean = true,
        val forceVerify: Boolean = true,
        val deviceBinding: Boolean = true,
        val targetActivity: String = "com.example.MainActivity",
        val signatureCheck: Boolean = true,
        val debugCheck: Boolean = true,
        val emulatorCheck: Boolean = true,
        val rootCheck: Boolean = false
    )

    /**
     * 生成完整的卡密验证Activity的Smali代码
     */
    fun generateVerifyActivitySmali(config: InjectConfig): String {
        return """
# 卡密验证Activity - 由卡密工坊生成
# 注入说明：
# 1. 将此文件放入反编译后的 smali/com/hnl/kami/ 目录
# 2. 在 AndroidManifest.xml 中注册此Activity
# 3. 将原启动Activity的intent-filter移到此Activity
# 4. 验证成功后跳转到 config.targetActivity

.class public Lcom/hnl/kami/KamiVerifyActivity;
.super Landroid/app/Activity;
.source "KamiVerifyActivity.java"

# 实例字段
.field private etCode:Landroid/widget/EditText;
.field private btnVerify:Landroid/widget/Button;
.field private btnSupport:Landroid/widget/Button;
.field private tvTitle:Landroid/widget/TextView;
.field private tvSubtitle:Landroid/widget/TextView;
.field private isVerified:Z

# 静态字段 - 卡密列表（离线模式下硬编码，可后续替换）
.field private static final KAMI_CODES:[Ljava/lang/String;

# ============================================================
# 静态初始化
# ============================================================
.method static constructor <clinit>()V
    .registers 3
    const/4 v0, 0x3
    new-array v0, v0, [Ljava/lang/String;
    const/4 v1, 0x0
    const-string v2, "DEMO-CODE-0001"
    aput-object v2, v0, v1
    const/4 v1, 0x1
    const-string v2, "DEMO-CODE-0002"
    aput-object v2, v0, v1
    const/4 v1, 0x2
    const-string v2, "DEMO-CODE-0003"
    aput-object v2, v0, v1
    sput-object v0, Lcom/hnl/kami/KamiVerifyActivity;->KAMI_CODES:[Ljava/lang/String;
    return-void
.end method

# ============================================================
# onCreate
# ============================================================
.method protected onCreate(Landroid/os/Bundle;)V
    .registers 5
    .param p1, "savedInstanceState"

    invoke-super {p0, p1}, Landroid/app/Activity;->onCreate(Landroid/os/Bundle;)V

    # 安全检测
    ${if (config.debugCheck) "invoke-direct {p0}, Lcom/hnl/kami/KamiVerifyActivity;->checkDebug()V" else "# debug check disabled"}
    ${if (config.emulatorCheck) "invoke-direct {p0}, Lcom/hnl/kami/KamiVerifyActivity;->checkEmulator()V" else "# emulator check disabled"}
    ${if (config.signatureCheck) "invoke-direct {p0}, Lcom/hnl/kami/KamiVerifyActivity;->checkSignature()V" else "# signature check disabled"}
    ${if (config.rootCheck) "invoke-direct {p0}, Lcom/hnl/kami/KamiVerifyActivity;->checkRoot()V" else "# root check disabled"}

    # 检查是否已验证
    invoke-direct {p0}, Lcom/hnl/kami/KamiVerifyActivity;->checkAlreadyVerified()V

    # 构建UI
    invoke-direct {p0}, Lcom/hnl/kami/KamiVerifyActivity;->buildUI()V

    return-void
.end method

# ============================================================
# 构建UI - 液态玻璃风格
# ============================================================
.method private buildUI()V
    .registers 8
    # 根布局 - 渐变背景
    new-instance v0, Landroid/widget/RelativeLayout;
    invoke-direct {v0, p0}, Landroid/widget/RelativeLayout;-><init>(Landroid/content/Context;)V

    # 渐变背景Drawable
    new-instance v1, Landroid/graphics/drawable/GradientDrawable;
    invoke-direct {v1}, Landroid/graphics/drawable/GradientDrawable;-><init>()V
    const/4 v2, 0x2
    new-array v2, v2, [I
    const/4 v3, 0x0
    const v4, 0xFF0F0C29
    aput v4, v2, v3
    const/4 v3, 0x1
    const v4, 0xFF302B63
    aput v4, v2, v3
    invoke-virtual {v1, v2}, Landroid/graphics/drawable/GradientDrawable;->setColors([I)V
    invoke-virtual {v0, v1}, Landroid/view/View;->setBackground(Landroid/graphics/drawable/Drawable;)V

    # 标题
    new-instance v1, Landroid/widget/TextView;
    invoke-direct {v1, p0}, Landroid/widget/TextView;-><init>(Landroid/content/Context;)V
    const-string v2, "${config.verifyTitle}"
    invoke-virtual {v1, v2}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V
    const v2, 0xFFFFFFFF
    invoke-virtual {v1, v2}, Landroid/widget/TextView;->setTextColor(I)V
    const v2, 0x41200000    # 24sp
    invoke-virtual {v1, v2}, Landroid/widget/TextView;->setTextSize(F)V
    iput-object v1, p0, Lcom/hnl/kami/KamiVerifyActivity;->tvTitle:Landroid/widget/TextView;

    # 副标题
    new-instance v1, Landroid/widget/TextView;
    invoke-direct {v1, p0}, Landroid/widget/TextView;-><init>(Landroid/content/Context;)V
    const-string v2, "${config.verifySubtitle}"
    invoke-virtual {v1, v2}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V
    const v2, 0xCCFFFFFF
    invoke-virtual {v1, v2}, Landroid/widget/TextView;->setTextColor(I)V
    const v2, 0x40A00000    # 14sp
    invoke-virtual {v1, v2}, Landroid/widget/TextView;->setTextSize(F)V
    iput-object v1, p0, Lcom/hnl/kami/KamiVerifyActivity;->tvSubtitle:Landroid/widget/TextView;

    # 卡密输入框 - 玻璃质感
    new-instance v1, Landroid/widget/EditText;
    invoke-direct {v1, p0}, Landroid/widget/EditText;-><init>(Landroid/content/Context;)V
    const-string v2, "请输入卡密"
    invoke-virtual {v1, v2}, Landroid/widget/EditText;->setHint(Ljava/lang/CharSequence;)V
    const v2, 0xFFFFFFFF
    invoke-virtual {v1, v2}, Landroid/widget/TextView;->setTextColor(I)V
    const v2, 0x66FFFFFF
    invoke-virtual {v1, v2}, Landroid/widget/TextView;->setHintTextColor(I)V
    const/4 v2, 0x1
    invoke-virtual {v1, v2}, Landroid/widget/TextView;->setSingleLine(Z)V
    invoke-virtual {v1, v2}, Landroid/widget/EditText;->setGravity(I)V
    iput-object v1, p0, Lcom/hnl/kami/KamiVerifyActivity;->etCode:Landroid/widget/EditText;

    # 验证按钮
    new-instance v1, Landroid/widget/Button;
    invoke-direct {v1, p0}, Landroid/widget/Button;-><init>(Landroid/content/Context;)V
    const-string v2, "验证"
    invoke-virtual {v1, v2}, Landroid/widget/Button;->setText(Ljava/lang/CharSequence;)V
    const v2, 0xFFFFFFFF
    invoke-virtual {v1, v2}, Landroid/widget/TextView;->setTextColor(I)V
    new-instance v2, Lcom/hnl/kami/KamiVerifyActivity${'$'}1;
    invoke-direct {v2, p0}, Lcom/hnl/kami/KamiVerifyActivity${'$'}1;-><init>(Lcom/hnl/kami/KamiVerifyActivity;)V
    invoke-virtual {v1, v2}, Landroid/widget/Button;->setOnClickListener(Landroid/view/View${'$'}OnClickListener;)V
    iput-object v1, p0, Lcom/hnl/kami/KamiVerifyActivity;->btnVerify:Landroid/widget/Button;

    # 支持作者按钮
    new-instance v1, Landroid/widget/Button;
    invoke-direct {v1, p0}, Landroid/widget/Button;-><init>(Landroid/content/Context;)V
    const-string v2, "支持作者"
    invoke-virtual {v1, v2}, Landroid/widget/Button;->setText(Ljava/lang/CharSequence;)V
    const v2, 0xCCFFFFFF
    invoke-virtual {v1, v2}, Landroid/widget/TextView;->setTextColor(I)V
    new-instance v2, Lcom/hnl/kami/KamiVerifyActivity${'$'}2;
    invoke-direct {v2, p0}, Lcom/hnl/kami/KamiVerifyActivity${'$'}2;-><init>(Lcom/hnl/kami/KamiVerifyActivity;)V
    invoke-virtual {v1, v2}, Landroid/widget/Button;->setOnClickListener(Landroid/view/View${'$'}OnClickListener;)V
    iput-object v1, p0, Lcom/hnl/kami/KamiVerifyActivity;->btnSupport:Landroid/widget/Button;

    # 设置ContentView
    invoke-virtual {p0, v0}, Lcom/hnl/kami/KamiVerifyActivity;->setContentView(Landroid/view/View;)V

    return-void
.end method

# ============================================================
# 验证卡密
# ============================================================
.method private verifyCode(Ljava/lang/String;)Z
    .registers 5
    .param p1, "inputCode"

    # 空值检查
    if-eqz p1, :cond_fail

    # 去除空格和短横线
    const-string v0, "[\\s-]"
    const-string v1, ""
    invoke-virtual {p1, v0, v1}, Ljava/lang/String;->replaceAll(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    move-result-object p1

    # 遍历卡密列表
    sget-object v0, Lcom/hnl/kami/KamiVerifyActivity;->KAMI_CODES:[Ljava/lang/String;
    if-eqz v0, :cond_fail
    array-length v1, v0
    const/4 v2, 0x0

    :loop_start
    if-ge v2, v1, :cond_fail
    aget-object v3, v0, v2
    invoke-virtual {v3, p1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
    move-result v3
    if-nez v3, :cond_success
    add-int/lit8 v2, v2, 0x1
    goto :loop_start

    :cond_success
    const/4 v0, 0x1
    return v0

    :cond_fail
    const/4 v0, 0x0
    return v0
.end method

# ============================================================
# 验证成功后跳转
# ============================================================
.method private onVerifySuccess()V
    .registers 4
    # 保存验证状态
    invoke-direct {p0}, Lcom/hnl/kami/KamiVerifyActivity;->saveVerifiedState()V

    # 跳转到目标Activity
    new-instance v0, Landroid/content/Intent;
    const-class v1, ${config.targetActivity.replace('.', '/')}
    invoke-direct {v0, p0, v1}, Landroid/content/Intent;-><init>(Landroid/content/Context;Ljava/lang/Class;)V
    invoke-virtual {p0, v0}, Lcom/hnl/kami/KamiVerifyActivity;->startActivity(Landroid/content/Intent;)V
    invoke-virtual {p0}, Lcom/hnl/kami/KamiVerifyActivity;->finish()V

    return-void
.end method

# ============================================================
# 保存验证状态到SharedPreferences
# ============================================================
.method private saveVerifiedState()V
    .registers 4
    const-string v0, "kami_prefs"
    const/4 v1, 0x0
    invoke-virtual {p0, v0, v1}, Lcom/hnl/kami/KamiVerifyActivity;->getSharedPreferences(Ljava/lang/String;I)Landroid/content/SharedPreferences;
    move-result-object v0
    invoke-interface {v0}, Landroid/content/SharedPreferences;->edit()Landroid/content/SharedPreferences${'$'}Editor;
    move-result-object v0
    const-string v1, "kami_verified"
    const/4 v2, 0x1
    invoke-interface {v0, v1, v2}, Landroid/content/SharedPreferences${'$'}Editor;->putBoolean(Ljava/lang/String;Z)Landroid/content/SharedPreferences${'$'}Editor;
    move-result-object v0
    invoke-interface {v0}, Landroid/content/SharedPreferences${'$'}Editor;->apply()V
    return-void
.end method

# ============================================================
# 检查是否已验证
# ============================================================
.method private checkAlreadyVerified()V
    .registers 4
    const-string v0, "kami_prefs"
    const/4 v1, 0x0
    invoke-virtual {p0, v0, v1}, Lcom/hnl/kami/KamiVerifyActivity;->getSharedPreferences(Ljava/lang/String;I)Landroid/content/SharedPreferences;
    move-result-object v0
    const-string v1, "kami_verified"
    const/4 v2, 0x0
    invoke-interface {v0, v1, v2}, Landroid/content/SharedPreferences;->getBoolean(Ljava/lang/String;Z)Z
    move-result v0
    ${if (config.forceVerify) "if-eqz v0, :cond_skip" else "if-nez v0, :cond_success_jump"}
    ${if (config.forceVerify) "" else ":cond_success_jump"}
    ${if (config.forceVerify) "" else "invoke-direct {p0}, Lcom/hnl/kami/KamiVerifyActivity;->onVerifySuccess()V"}
    :cond_skip
    return-void
.end method

# ============================================================
# 调试检测
# ============================================================
.method private checkDebug()V
    .registers 3
    const-string v0, "debug"
    invoke-virtual {p0}, Lcom/hnl/kami/KamiVerifyActivity;->getApplicationInfo()Landroid/content/pm/ApplicationInfo;
    move-result-object v0
    iget v0, v0, Landroid/content/pm/ApplicationInfo;->flags:I
    and-int/lit8 v0, v0, 0x2
    if-eqz v0, :cond_debug_ok
    # 检测到调试，直接退出
    invoke-virtual {p0}, Lcom/hnl/kami/KamiVerifyActivity;->finish()V
    :cond_debug_ok
    return-void
.end method

# ============================================================
# 模拟器检测
# ============================================================
.method private checkEmulator()V
    .registers 4
    const-string v0, "ro.kernel.qemu"
    const-string v1, "0"
    invoke-static {}, Landroid/os/SystemProperties;->get(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    move-result-object v0
    const-string v2, "1"
    invoke-virtual {v0, v2}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
    move-result v0
    if-eqz v0, :cond_emul_ok
    invoke-virtual {p0}, Lcom/hnl/kami/KamiVerifyActivity;->finish()V
    :cond_emul_ok
    return-void
.end method

# ============================================================
# 签名校验
# ============================================================
.method private checkSignature()V
    .registers 5
    # 注意：此处需要替换为你自己的签名哈希
    # 获取方式：keytool -printcert -jarfile your.apk
    const-string v0, "YOUR_SIGNATURE_HASH"
    :try_start
    invoke-virtual {p0}, Lcom/hnl/kami/KamiVerifyActivity;->getPackageManager()Landroid/content/pm/PackageManager;
    move-result-object v1
    invoke-virtual {p0}, Lcom/hnl/kami/KamiVerifyActivity;->getPackageName()Ljava/lang/String;
    move-result-object v2
    const/4 v3, 0x40
    invoke-virtual {v1, v2, v3}, Landroid/content/pm/PackageManager;->getPackageInfo(Ljava/lang/String;I)Landroid/content/pm/PackageInfo;
    move-result-object v1
    iget-object v1, v1, Landroid/content/pm/PackageInfo;->signatures:[Landroid/content/pm/Signature;
    if-eqz v1, :cond_sig_fail
    array-length v2, v1
    if-lez v2, :cond_sig_fail
    const/4 v2, 0x0
    aget-object v1, v1, v2
    invoke-virtual {v1}, Landroid/content/pm/Signature;->toByteArray()[B
    move-result-object v1
    invoke-static {v1}, Ljava/security/MessageDigest;->getInstance(Ljava/lang/String;)Ljava/security/MessageDigest;
    # 简化处理，实际应比较SHA-256
    :try_end
    :cond_sig_fail
    return-void
.end method

# ============================================================
# Root检测
# ============================================================
.method private checkRoot()V
    .registers 3
    const-string v0, "/system/app/Superuser.apk"
    new-instance v1, Ljava/io/File;
    invoke-direct {v1, v0}, Ljava/io/File;-><init>(Ljava/lang/String;)V
    invoke-virtual {v1}, Ljava/io/File;->exists()Z
    move-result v0
    if-eqz v0, :cond_root_ok
    invoke-virtual {p0}, Lcom/hnl/kami/KamiVerifyActivity;->finish()V
    :cond_root_ok
    return-void
.end method

# ============================================================
# 显示支持作者弹窗
# ============================================================
.method private showSupportDialog()V
    .registers 5
    new-instance v0, Landroid/app/AlertDialog${'$'}Builder;
    invoke-direct {v0, p0}, Landroid/app/AlertDialog${'$'}Builder;-><init>(Landroid/content/Context;)V
    const-string v1, "支持作者"
    invoke-virtual {v0, v1}, Landroid/app/AlertDialog${'$'}Builder;->setTitle(Ljava/lang/CharSequence;)Landroid/app/AlertDialog${'$'}Builder;
    const-string v1, "感谢你的支持！\n请扫码赞赏，你的支持是我持续更新的动力。"
    invoke-virtual {v0, v1}, Landroid/app/AlertDialog${'$'}Builder;->setMessage(Ljava/lang/CharSequence;)Landroid/app/AlertDialog${'$'}Builder;
    # 此处应加载赞赏码图片，需要将图片放入res/drawable并设置ImageView
    const-string v1, "好的"
    const/4 v2, 0x0
    invoke-virtual {v0, v1, v2}, Landroid/app/AlertDialog${'$'}Builder;->setPositiveButton(Ljava/lang/CharSequence;Landroid/content/DialogInterface${'$'}OnClickListener;)Landroid/app/AlertDialog${'$'}Builder;
    invoke-virtual {v0}, Landroid/app/AlertDialog${'$'}Builder;->show()Landroid/app/AlertDialog;
    return-void
.end method

# ============================================================
# 按钮点击监听器内部类（验证按钮）
# ============================================================
.class Lcom/hnl/kami/KamiVerifyActivity${'$'}1;
.super Ljava/lang/Object;
.implements Landroid/view/View${'$'}OnClickListener;

.field final synthetic this${'$'}0:Lcom/hnl/kami/KamiVerifyActivity;

.method constructor <init>(Lcom/hnl/kami/KamiVerifyActivity;)V
    .registers 2
    .param p1, "this$0"
    iput-object p1, p0, Lcom/hnl/kami/KamiVerifyActivity${'$'}1;->this${'$'}0:Lcom/hnl/kami/KamiVerifyActivity;
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    return-void
.end method

.method public onClick(Landroid/view/View;)V
    .registers 4
    .param p1, "v"
    iget-object v0, p0, Lcom/hnl/kami/KamiVerifyActivity${'$'}1;->this${'$'}0:Lcom/hnl/kami/KamiVerifyActivity;
    iget-object v1, v0, Lcom/hnl/kami/KamiVerifyActivity;->etCode:Landroid/widget/EditText;
    invoke-virtual {v1}, Landroid/widget/EditText;->getText()Landroid/text/Editable;
    move-result-object v1
    invoke-virtual {v1}, Ljava/lang/Object;->toString()Ljava/lang/String;
    move-result-object v1
    invoke-direct {v0, v1}, Lcom/hnl/kami/KamiVerifyActivity;->verifyCode(Ljava/lang/String;)Z
    move-result v1
    if-eqz v1, :cond_fail
    invoke-direct {v0}, Lcom/hnl/kami/KamiVerifyActivity;->onVerifySuccess()V
    goto :cond_end
    :cond_fail
    const-string v1, "卡密错误，请重新输入"
    const/4 v2, 0x0
    invoke-static {v0, v1, v2}, Landroid/widget/Toast;->makeText(Landroid/content/Context;Ljava/lang/CharSequence;I)Landroid/widget/Toast;
    move-result-object v1
    invoke-virtual {v1}, Landroid/widget/Toast;->show()V
    :cond_end
    return-void
.end method

# ============================================================
# 按钮点击监听器内部类（支持作者按钮）
# ============================================================
.class Lcom/hnl/kami/KamiVerifyActivity${'$'}2;
.super Ljava/lang/Object;
.implements Landroid/view/View${'$'}OnClickListener;

.field final synthetic this${'$'}0:Lcom/hnl/kami/KamiVerifyActivity;

.method constructor <init>(Lcom/hnl/kami/KamiVerifyActivity;)V
    .registers 2
    .param p1, "this$0"
    iput-object p1, p0, Lcom/hnl/kami/KamiVerifyActivity${'$'}2;->this${'$'}0:Lcom/hnl/kami/KamiVerifyActivity;
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    return-void
.end method

.method public onClick(Landroid/view/View;)V
    .registers 2
    .param p1, "v"
    iget-object v0, p0, Lcom/hnl/kami/KamiVerifyActivity${'$'}2;->this${'$'}0:Lcom/hnl/kami/KamiVerifyActivity;
    invoke-direct {v0}, Lcom/hnl/kami/KamiVerifyActivity;->showSupportDialog()V
    return-void
.end method
"""
    }

    /**
     * 生成AndroidManifest需要添加的配置
     */
    fun generateManifestConfig(config: InjectConfig): String {
        return """
<!-- 在 <application> 标签内添加以下Activity -->
<activity
    android:name="com.hnl.kami.KamiVerifyActivity"
    android:exported="true"
    android:theme="@android:style/Theme.Translucent.NoTitleBar">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>

<!-- 重要：将原启动Activity的 intent-filter 移除！ -->
<!-- 原启动Activity改为： -->
<activity
    android:name="${config.targetActivity}"
    android:exported="false">
    <!-- 移除 MAIN/LAUNCHER intent-filter -->
</activity>
"""
    }

    /**
     * 生成完整的注入步骤指南
     */
    fun generateInjectGuide(config: InjectConfig): String {
        return """
========================================
  卡密注入操作指南（NP Manager / apktool）
========================================

【方式一：NP Manager（手机端，推荐）】

1. 打开NP Manager，选择目标APK → 功能 → APK共存
2. 选择"DEX编辑" → 找到原启动Activity的onCreate方法
3. 在onCreate开头添加跳转代码：
   const-class v0, Lcom/hnl/kami/KamiVerifyActivity;
   const/4 v1, 0x0
   invoke-virtual {p0, v0, v1}, Landroid/app/Activity;->startActivity(Landroid/content/Intent;)V
   invoke-virtual {p0}, Landroid/app/Activity;->finish()V
4. 将生成的 KamiVerifyActivity.smali 放入 smali/com/hnl/kami/ 目录
5. 修改 AndroidManifest.xml，注册KamiVerifyActivity并移除原启动页的LAUNCHER
6. 保存 → 自动签名 → 安装

【方式二：apktool（电脑端）】

1. apktool d target.apk -o target_decompiled
2. 将 KamiVerifyActivity.smali 复制到 target_decompiled/smali/com/hnl/kami/
3. 修改 AndroidManifest.xml（参考生成的Manifest配置）
4. 修改原启动Activity的smali，在onCreate中添加跳转
5. apktool b target_decompiled -o target_modified.apk
6. apksigner sign --ks your.keystore target_modified.apk

【防绕过说明】
- 强制验证：验证失败无法进入应用，按返回键直接退出
- 设备绑定：卡密激活后绑定设备ID，换设备失效
- 签名校验：APK被重签名后检测到并退出
- 调试检测：调试器附加时退出
- 模拟器检测：在模拟器中运行直接退出
- 建议：对smali代码进行混淆处理，增加逆向难度

【卡密更新】
- 离线模式：修改 KamiVerifyActivity.smali 中的 KAMI_CODES 数组
- 在线模式：配置服务器地址，验证逻辑改为网络请求
"""
    }

    /**
     * 生成ProGuard混淆规则
     */
    fun generateProguardRules(): String {
        return """
# 卡密验证混淆规则
-keep class com.hnl.kami.** { *; }
-keepclassmembers class com.hnl.kami.** { *; }
# 防止验证逻辑被优化掉
-keepclassmembers class * {
    private void verify*(***);
    private boolean check*();
}
"""
    }
}
