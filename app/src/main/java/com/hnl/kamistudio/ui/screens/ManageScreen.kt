package com.hnl.kamistudio.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.hnl.kamistudio.data.KamiEntity
import com.hnl.kamistudio.ui.components.LiquidGlassCard
import com.hnl.kamistudio.ui.components.LiquidGlassTextField
import com.hnl.kamistudio.util.ExportHelper
import com.hnl.kamistudio.util.KamiGenerator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ManageScreen(dao: KamiDao) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf("all") }

    val allCodes by if (searchQuery.isNotEmpty()) {
        dao.search(searchQuery).collectAsState(initial = emptyList())
    } else if (selectedStatus != "all") {
        dao.getByStatus(selectedStatus).collectAsState(initial = emptyList())
    } else {
        dao.getAll().collectAsState(initial = emptyList())
    }

    val statuses = listOf("all" to "全部", "inactive" to "未激活", "active" to "已激活", "expired" to "已过期", "disabled" to "已禁用")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("卡密管理", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Icon(
                Icons.Default.FileDownload,
                contentDescription = "导出",
                tint = Color(0xFF55EFC4),
                modifier = Modifier
                    .size(24.dp)
                    .clickable {
                        scope.launch {
                            val all = dao.getAll().first()
                            val saved = ExportHelper.exportToCsv(context, all, "kami_all")
                            if (saved != null) ExportHelper.shareSavedFile(context, saved)
                        }
                    }
            )
        }

        LiquidGlassTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = "搜索卡密或备注...",
            modifier = Modifier.fillMaxWidth()
        )

        // 状态筛选
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            statuses.forEach { (key, label) ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selectedStatus == key) Color(0xFF6C5CE7) else Color(0x1AFFFFFF))
                        .clickable { selectedStatus = key }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(label, color = Color.White, fontSize = 11.sp)
                }
            }
        }

        Text("共 ${allCodes.size} 张卡密", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(allCodes) { kami ->
                KamiItemCard(
                    kami = kami,
                    onCopy = {
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("卡密", kami.code))
                        Toast.makeText(context, "已复制卡密", Toast.LENGTH_SHORT).show()
                    },
                    onToggleStatus = {
                        scope.launch {
                            val newStatus = if (kami.status == "disabled") "inactive" else "disabled"
                            dao.update(kami.copy(status = newStatus))
                        }
                    },
                    onDelete = {
                        scope.launch { dao.delete(kami) }
                    }
                )
            }
        }
    }
}

@Composable
private fun KamiItemCard(
    kami: KamiEntity,
    onCopy: () -> Unit,
    onToggleStatus: () -> Unit,
    onDelete: () -> Unit
) {
    val statusColor = when (kami.status) {
        "active" -> Color(0xFF00B894)
        "inactive" -> Color(0xFFFDCB6E)
        "expired" -> Color(0xFFE17055)
        "disabled" -> Color(0xFF636E72)
        else -> Color.White
    }
    val statusLabel = when (kami.status) {
        "active" -> "已激活"
        "inactive" -> "未激活"
        "expired" -> "已过期"
        "disabled" -> "已禁用"
        else -> kami.status
    }

    LiquidGlassCard(cornerRadius = 16.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    kami.code,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    fontWeight = FontWeight.Medium
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(statusColor.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(statusLabel, color = statusColor, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "${KamiGenerator.getTypeLabel(kami.type)} | 生成: ${SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(kami.createdAt))}",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp
                )
            }
            if (kami.note.isNotEmpty()) {
                Text("备注: ${kami.note}", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SmallActionButton("复制", Icons.Default.ContentCopy, Color(0xFF74B9FF), onCopy)
                SmallActionButton(if (kami.status == "disabled") "启用" else "禁用", Icons.Default.Block, Color(0xFFFDCB6E), onToggleStatus)
                SmallActionButton("删除", Icons.Default.Delete, Color(0xFFE17055), onDelete)
            }
        }
    }
}

@Composable
private fun SmallActionButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.15f))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Text(label, color = color, fontSize = 11.sp)
    }
}
