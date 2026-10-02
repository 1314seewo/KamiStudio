package com.hnl.kamistudio.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FileDownload
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
import com.hnl.kamistudio.data.KamiEntity
import com.hnl.kamistudio.ui.components.LiquidGlassButton
import com.hnl.kamistudio.ui.components.LiquidGlassCard
import com.hnl.kamistudio.ui.components.LiquidGlassTextField
import com.hnl.kamistudio.util.ExportHelper
import com.hnl.kamistudio.util.KamiGenerator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.launch

@Composable
fun GenerateScreen(dao: KamiDao) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var prefix by remember { mutableStateOf("VIP-") }
    var length by remember { mutableStateOf("20") }
    var count by remember { mutableStateOf("10") }
    var selectedType by remember { mutableStateOf("monthly") }
    var customDays by remember { mutableStateOf("30") }
    var batchName by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var generatedCodes by remember { mutableStateOf<List<String>>(emptyList()) }
    var showResult by remember { mutableStateOf(false) }

    val types = listOf(
        "daily" to "日卡",
        "weekly" to "周卡",
        "monthly" to "月卡",
        "quarterly" to "季卡",
        "yearly" to "年卡",
        "forever" to "永久卡",
        "custom" to "自定义"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("生成卡密", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)

        LiquidGlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("卡密前缀", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                LiquidGlassTextField(value = prefix, onValueChange = { prefix = it }, placeholder = "如 VIP-、PRO-")

                Text("卡密位数", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("16", "20", "24", "32").forEach { len ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (length == len) Color(0xFF6C5CE7) else Color(0x22FFFFFF))
                                .clickable { length = len }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(len, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                Text("生成数量", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                LiquidGlassTextField(value = count, onValueChange = { count = it.filter { c -> c.isDigit() } }, placeholder = "1-1000")

                Text("卡密类型", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    types.chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { (key, label) ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (selectedType == key) Color(0xFF6C5CE7) else Color(0x22FFFFFF))
                                        .clickable { selectedType = key }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(label, color = Color.White, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                if (selectedType == "custom") {
                    Text("自定义天数", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                    LiquidGlassTextField(value = customDays, onValueChange = { customDays = it.filter { c -> c.isDigit() } }, placeholder = "天数")
                }

                Text("批次名称（可选）", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                LiquidGlassTextField(value = batchName, onValueChange = { batchName = it }, placeholder = "如 2024年10月批次")

                Text("备注（可选）", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                LiquidGlassTextField(value = note, onValueChange = { note = it }, placeholder = "备注信息")
            }
        }

        LiquidGlassButton(
            text = "生成卡密",
            onClick = {
                val cnt = count.toIntOrNull() ?: 10
                val len = length.toIntOrNull() ?: 20
                if (cnt in 1..1000 && len >= 8) {
                    generatedCodes = KamiGenerator.generateBatch(prefix, len, cnt)
                    showResult = true
                    val days = KamiGenerator.getTypeDays(selectedType, customDays.toIntOrNull() ?: 30)
                    val entities = generatedCodes.map { code ->
                        KamiEntity(
                            code = code,
                            type = selectedType,
                            prefix = prefix,
                            validDays = days,
                            batchName = batchName,
                            note = note
                        )
                    }
                    scope.launch { dao.insertAll(entities) }
                    Toast.makeText(context, "已生成 ${generatedCodes.size} 张卡密并保存", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "请输入有效的数量和位数", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        if (showResult && generatedCodes.isNotEmpty()) {
            LiquidGlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("生成结果（${generatedCodes.size}张）", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "复制全部", tint = Color(0xFF74B9FF), modifier = Modifier
                                .size(20.dp)
                                .clickable {
                                    val all = generatedCodes.joinToString("\n")
                                    android.content.ClipData.newPlainText("卡密", all).also {
                                        (context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager).setPrimaryClip(it)
                                    }
                                    Toast.makeText(context, "已复制全部卡密", Toast.LENGTH_SHORT).show()
                                })
                            Icon(Icons.Default.FileDownload, contentDescription = "导出", tint = Color(0xFF55EFC4), modifier = Modifier
                                .size(20.dp)
                                .clickable {
                                    scope.launch {
                                        val all = dao.getAll().first()
                                        val file = ExportHelper.exportToTxt(context, all, batchName.ifEmpty { "kami_batch" })
                                        if (file != null) {
                                            ExportHelper.shareFile(context, file)
                                        }
                                    }
                                })
                        }
                    }
                    generatedCodes.take(20).forEach { code ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x11FFFFFF))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(code, color = Color.White, fontSize = 13.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                            Text(KamiGenerator.getTypeLabel(selectedType), color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                        }
                    }
                    if (generatedCodes.size > 20) {
                        Text("...还有 ${generatedCodes.size - 20} 张，请到卡密管理查看", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
