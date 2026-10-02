package com.hnl.kamistudio.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hnl.kamistudio.ui.components.LiquidGlassButton
import com.hnl.kamistudio.ui.components.LiquidGlassCard
import com.hnl.kamistudio.ui.components.LiquidGlassTextField
import com.hnl.kamistudio.util.ApkInjector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun InjectScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedApkUri by remember { mutableStateOf<Uri?>(null) }
    var selectedApkName by remember { mutableStateOf("") }
    var verifyTitle by remember { mutableStateOf("卡密验证") }
    var verifySubtitle by remember { mutableStateOf("请输入卡密以继续使用") }
    var kamiInput by remember { mutableStateOf("") }
    var isInjecting by remember { mutableStateOf(false) }
    var injectProgress by remember { mutableStateOf(0) }
    var injectMessage by remember { mutableStateOf("") }
    var resultMessage by remember { mutableStateOf("") }
    var resultSuccess by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedApkUri = uri
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (it.moveToFirst() && nameIndex >= 0) {
                    selectedApkName = it.getString(nameIndex) ?: "未知APK"
                }
            }
            resultMessage = ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("APK一键注入", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)

        LiquidGlassCard(cornerRadius = 16.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFFDCB6E), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("全自动注入", color = Color(0xFFFDCB6E), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                Text(
                    "选择APK → 输入卡密 → 点击注入，自动完成：\n修改Manifest → 注入卡密验证 → 重打包 → 签名\n输出文件保存到 Download/KamiStudio/",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }

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
                    }
                }
            }
        }

        LiquidGlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("第二步：配置卡密", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text("卡密（每行一个）", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                LiquidGlassTextField(
                    value = kamiInput,
                    onValueChange = { kamiInput = it },
                    placeholder = "ABCD-1234-EFGH\nWXYZ-5678-IJKL",
                    singleLine = false,
                    modifier = Modifier.height(100.dp)
                )
                Text("验证界面标题", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                LiquidGlassTextField(value = verifyTitle, onValueChange = { verifyTitle = it })
                Text("验证界面副标题", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                LiquidGlassTextField(value = verifySubtitle, onValueChange = { verifySubtitle = it })
            }
        }

        if (isInjecting) {
            LiquidGlassCard {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    CircularProgressIndicator(color = Color(0xFF6C5CE7), modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("$injectProgress%", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(injectMessage, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { injectProgress / 100f },
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF6C5CE7),
                        trackColor = Color(0x1AFFFFFF)
                    )
                }
            }
        } else {
            LiquidGlassButton(
                text = "开始一键注入",
                onClick = {
                    if (selectedApkUri == null) {
                        Toast.makeText(context, "请先选择APK文件", Toast.LENGTH_SHORT).show()
                        return@LiquidGlassButton
                    }
                    if (kamiInput.isBlank()) {
                        Toast.makeText(context, "请输入至少一个卡密", Toast.LENGTH_SHORT).show()
                        return@LiquidGlassButton
                    }
                    val kamiList = kamiInput.lines().map { it.trim() }.filter { it.isNotEmpty() }
                    if (kamiList.isEmpty()) {
                        Toast.makeText(context, "卡密格式不正确", Toast.LENGTH_SHORT).show()
                        return@LiquidGlassButton
                    }
                    isInjecting = true
                    resultMessage = ""
                    scope.launch {
                        val result = withContext(Dispatchers.IO) {
                            ApkInjector.inject(
                                context = context,
                                apkUri = selectedApkUri!!,
                                config = ApkInjector.InjectConfig(
                                    verifyTitle = verifyTitle,
                                    verifySubtitle = verifySubtitle,
                                    kamiList = kamiList
                                ),
                                onProgress = { progress, msg ->
                                    scope.launch {
                                        injectProgress = progress
                                        injectMessage = msg
                                    }
                                }
                            )
                        }
                        isInjecting = false
                        resultSuccess = result.success
                        resultMessage = result.message + if (result.outputFile != null) "\n文件: ${result.outputFile.name}" else ""
                        if (result.success) {
                            Toast.makeText(context, "注入成功！", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (resultMessage.isNotEmpty()) {
            LiquidGlassCard(cornerRadius = 16.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (resultSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = null,
                            tint = if (resultSuccess) Color(0xFF55EFC4) else Color(0xFFFF7675),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (resultSuccess) "注入成功" else "注入失败",
                            color = if (resultSuccess) Color(0xFF55EFC4) else Color(0xFFFF7675),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(resultMessage, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, lineHeight = 16.sp)
                    if (resultSuccess) {
                        Text("请到 文件管理 → Download → KamiStudio 查看生成的APK", color = Color(0xFFFDCB6E), fontSize = 11.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}
