package com.hnl.kamistudio.ui.screens

import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hnl.kamistudio.ui.components.LiquidGlassButton
import com.hnl.kamistudio.ui.components.LiquidGlassCard
import com.hnl.kamistudio.ui.components.LiquidGlassTextField
import com.hnl.kamistudio.util.SmaliGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun InjectScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedApkUri by remember { mutableStateOf<Uri?>(null) }
    var selectedApkName by remember { mutableStateOf("") }
    var detectedPackage by remember { mutableStateOf("") }
    var verifyTitle by remember { mutableStateOf("卡密验证") }
    var verifySubtitle by remember { mutableStateOf("请输入卡密以继续使用") }
    var serverUrl by remember { mutableStateOf("") }
    var offlineMode by remember { mutableStateOf(true) }
    var forceVerify by remember { mutableStateOf(true) }
    var deviceBinding by remember { mutableStateOf(true) }
    var targetActivity by remember { mutableStateOf("") }
    var signatureCheck by remember { mutableStateOf(true) }
    var debugCheck by remember { mutableStateOf(true) }
    var emulatorCheck by remember { mutableStateOf(true) }
    var rootCheck by remember { mutableStateOf(false) }
    var isDetecting by remember { mutableStateOf(false) }

    var showCode by remember { mutableStateOf(false) }
    var generatedSmali by remember { mutableStateOf("") }
    var generatedManifest by remember { mutableStateOf("") }
    var generatedGuide by remember { mutableStateOf("") }
    var generatedProguard by remember { mutableStateOf("") }
    var activeTab by remember { mutableStateOf("smali") }

    // 文件选择器
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedApkUri = uri
            // 获取文件名
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (it.moveToFirst() && nameIndex >= 0) {
                    selectedApkName = it.getString(nameIndex) ?: "未知APK"
                }
            }
            // 自动检测包名和启动Activity
            isDetecting = true
            scope.launch {
                val info = withContext(Dispatchers.IO) {
                    try {
                        // 复制到本地缓存
                        val tempFile = File(context.cacheDir, "temp_${System.currentTimeMillis()}.apk")
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            tempFile.outputStream().use { output -> input.copyTo(output) }
                        }
                        val pm = context.packageManager
                        val pkgInfo = pm.getPackageArchiveInfo(tempFile.absolutePath, PackageManager.GET_ACTIVITIES)
                        tempFile.delete()
                        if (pkgInfo != null) {
                            val pkg = pkgInfo.packageName
                            // 找启动Activity
                            var launcher = ""
                            pkgInfo.activities?.forEach { activity ->
                                if (activity.exported && activity.name.isNotEmpty()) {
                                    // 简单判断：第一个exported的activity通常是启动的
                                    if (launcher.isEmpty()) launcher = activity.name
                                }
                            }
                            // 用getLaunchIntentForPackage更准确
                            val intent = pm.getLaunchIntentForPackage(pkg)
                            if (intent?.component != null) {
                                launcher = intent.component!!.className
                            }
                            pkg to launcher
                        } else null
                    } catch (e: Exception) {
                        null
                    }
                }
                isDetecting = false
                if (info != null) {
                    detectedPackage = info.first
                    if (info.second.isNotEmpty()) targetActivity = info.second
                    Toast.makeText(context, "已识别: ${info.first}", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "无法自动识别，请手动填写启动Activity", Toast.LENGTH_LONG).show()
                }
            }
            showCode = false
        }
    }

    val config = SmaliGenerator.InjectConfig(
        verifyTitle = verifyTitle,
        verifySubtitle = verifySubtitle,
        serverUrl = serverUrl,
        offlineMode = offlineMode,
        forceVerify = forceVerify,
        deviceBinding = deviceBinding,
        targetActivity = targetActivity,
        signatureCheck = signatureCheck,
        debugCheck = debugCheck,
        emulatorCheck = emulatorCheck,
        rootCheck = rootCheck
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("APK卡密注入", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)

        // 第一步：选择APK
        LiquidGlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("第一步：选择APK", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x1AFFFFFF))
                        .clickable { filePicker.launch("application/vnd.android.package-archive") }
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            if (selectedApkUri != null) Icons.Default.CheckCircle else Icons.Default.FileUpload,
                            contentDescription = null,
                            tint = if (selectedApkUri != null) Color(0xFF55EFC4) else Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            if (selectedApkName.isNotEmpty()) selectedApkName else "点击选择APK文件",
                            color = if (selectedApkName.isNotEmpty()) Color.White else Color.White.copy(alpha = 0.5f),
                            fontSize = 13.sp
                        )
                        if (detectedPackage.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("包名: $detectedPackage", color = Color(0xFF74B9FF), fontSize = 11.sp)
                        }
                        if (isDetecting) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("正在识别...", color = Color(0xFFFDCB6E), fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // 第二步：验证界面配置
        LiquidGlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("第二步：配置验证界面", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text("界面标题", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                LiquidGlassTextField(value = verifyTitle, onValueChange = { verifyTitle = it })
                Text("副标题/提示文字", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                LiquidGlassTextField(value = verifySubtitle, onValueChange = { verifySubtitle = it })
                Text("原启动Activity（验证成功后跳转，已自动填充）", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                LiquidGlassTextField(value = targetActivity, onValueChange = { targetActivity = it }, placeholder = "com.example.MainActivity")
            }
        }

        // 第三步：验证模式
        LiquidGlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("第三步：验证模式", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                ToggleRow("离线验证（卡密内置在APK内）", offlineMode) { offlineMode = it }
                if (!offlineMode) {
                    Text("验证服务器地址", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                    LiquidGlassTextField(value = serverUrl, onValueChange = { serverUrl = it }, placeholder = "https://your-api.com/verify")
                }
                ToggleRow("强制验证（无法跳过，失败退出应用）", forceVerify) { forceVerify = it }
                ToggleRow("设备绑定（一卡一机，换设备失效）", deviceBinding) { deviceBinding = it }
            }
        }

        // 第四步：安全防护
        LiquidGlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("第四步：防绕过安全防护", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                ToggleRow("签名校验（检测APK是否被重签名）", signatureCheck) { signatureCheck = it }
                ToggleRow("调试检测（检测调试器附加）", debugCheck) { debugCheck = it }
                ToggleRow("模拟器检测", emulatorCheck) { emulatorCheck = it }
                ToggleRow("Root检测", rootCheck) { rootCheck = it }
            }
        }

        // 生成按钮
        LiquidGlassButton(
            text = "生成卡密注入包",
            onClick = {
                if (targetActivity.isEmpty()) {
                    Toast.makeText(context, "请先选择APK或手动填写启动Activity", Toast.LENGTH_SHORT).show()
                    return@LiquidGlassButton
                }
                generatedSmali = SmaliGenerator.generateVerifyActivitySmali(config)
                generatedManifest = SmaliGenerator.generateManifestConfig(config)
                generatedGuide = SmaliGenerator.generateInjectGuide(config)
                generatedProguard = SmaliGenerator.generateProguardRules()
                showCode = true
                Toast.makeText(context, "注入包已生成，可导出为zip", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth()
        )

        // 生成结果
        if (showCode) {
            LiquidGlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("smali" to "Smali代码", "manifest" to "Manifest", "guide" to "注入指南", "proguard" to "混淆规则").forEach { (key, label) ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (activeTab == key) Color(0xFF6C5CE7) else Color(0x1AFFFFFF))
                                    .clickable { activeTab = key }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(label, color = Color.White, fontSize = 11.sp)
                            }
                        }
                    }

                    val codeToShow = when (activeTab) {
                        "smali" -> generatedSmali
                        "manifest" -> generatedManifest
                        "guide" -> generatedGuide
                        else -> generatedProguard
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0D0D1A))
                            .padding(12.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            codeToShow,
                            color = Color(0xFFA8E6CF),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LiquidGlassButton(
                            text = "复制代码",
                            onClick = {
                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("code", codeToShow))
                                Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
                            },
                            primary = false,
                            modifier = Modifier.weight(1f)
                        )
                        LiquidGlassButton(
                            text = "导出注入包",
                            onClick = {
                                scope.launch {
                                    val zipFile = com.hnl.kamistudio.util.ExportHelper.exportInjectPackage(
                                        context, generatedSmali, generatedManifest, generatedGuide, generatedProguard
                                    )
                                    if (zipFile != null) {
                                        Toast.makeText(context, "已保存到: Download/KamiStudio/${zipFile.name}", Toast.LENGTH_LONG).show()
                                        com.hnl.kamistudio.util.ExportHelper.shareFile(context, zipFile)
                                    } else {
                                        Toast.makeText(context, "导出失败，请检查存储权限", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 使用说明
        LiquidGlassCard(cornerRadius = 16.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF74B9FF), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("注入操作指南", color = Color(0xFF74B9FF), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                Text(
                    "1. 选择APK → 自动识别启动Activity → 生成注入包 → 导出zip\n" +
                    "2. 手机装 NP Manager（免费），打开目标APK\n" +
                    "3. 按「注入指南」把smali和Manifest配置粘进去\n" +
                    "4. NP Manager一键签名 → 安装 → 启动即显示卡密验证\n\n" +
                    "电脑端可用 apktool 命令行，流程一样",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF6C5CE7),
                checkedTrackColor = Color(0xFF6C5CE7).copy(alpha = 0.4f),
                uncheckedThumbColor = Color(0xFF636E72),
                uncheckedTrackColor = Color(0xFF2D3436)
            )
        )
    }
}
