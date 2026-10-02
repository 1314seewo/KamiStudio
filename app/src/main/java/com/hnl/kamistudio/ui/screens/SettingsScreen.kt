package com.hnl.kamistudio.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.hnl.kamistudio.data.KamiDao
import com.hnl.kamistudio.ui.components.LiquidGlassCard
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(dao: KamiDao, onSupportAuthor: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showClearConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("设置", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)

        // 关于
        LiquidGlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF6C5CE7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("卡密工坊", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("版本 1.0.0", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                    }
                }
            }
        }

        // 支持作者
        LiquidGlassCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSupportAuthor() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFFD79A8), modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text("支持作者", color = Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f))
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(18.dp))
            }
        }

        // 数据管理
        LiquidGlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("数据管理", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                SettingRow("导出所有卡密（TXT）", Icons.Default.FileDownload, Color(0xFF55EFC4)) {
                    scope.launch {
                        val all = dao.getAll().first()
                        val saved = com.hnl.kamistudio.util.ExportHelper.exportToTxt(context, all, "kami_export")
                        if (saved != null) com.hnl.kamistudio.util.ExportHelper.shareSavedFile(context, saved)
                    }
                }
                SettingRow("导出所有卡密（CSV）", Icons.Default.FileDownload, Color(0xFF74B9FF)) {
                    scope.launch {
                        val all = dao.getAll().first()
                        val saved = com.hnl.kamistudio.util.ExportHelper.exportToCsv(context, all, "kami_export")
                        if (saved != null) com.hnl.kamistudio.util.ExportHelper.shareSavedFile(context, saved)
                    }
                }
                SettingRow("清空所有卡密数据", Icons.Default.DeleteForever, Color(0xFFE17055)) {
                    showClearConfirm = true
                }
            }
        }

        // 使用说明
        LiquidGlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF74B9FF), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("使用说明", color = Color(0xFF74B9FF), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                Text(
                    "1. 在「生成卡密」页面批量生成卡密\n" +
                    "2. 在「APK注入」页面配置验证参数，生成Smali注入代码\n" +
                    "3. 使用NP Manager（手机端）或apktool（电脑端）将代码注入目标APK\n" +
                    "4. 重新签名后安装，APK启动时会先显示卡密验证界面\n" +
                    "5. 验证界面和本应用均采用iOS液态玻璃设计风格",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        Text("卡密工坊 © 2024 HNL", color = Color.White.copy(alpha = 0.3f), fontSize = 11.sp)
    }

    // 清空确认弹窗
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("确认清空", color = Color.White) },
            text = { Text("此操作将删除所有卡密数据，且无法恢复。确定继续吗？", color = Color.White.copy(alpha = 0.7f)) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { dao.deleteAll() }
                    showClearConfirm = false
                    Toast.makeText(context, "已清空所有数据", Toast.LENGTH_SHORT).show()
                }) {
                    Text("确认清空", color = Color(0xFFE17055))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("取消", color = Color.White)
                }
            }
        )
    }
}

@Composable
private fun SettingRow(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(16.dp))
    }
}
