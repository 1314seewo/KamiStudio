package com.hnl.kamistudio.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hnl.kamistudio.data.KamiDao
import com.hnl.kamistudio.ui.components.LiquidGlassCard
import com.hnl.kamistudio.ui.theme.GlassAccent
import com.hnl.kamistudio.ui.theme.GlassPrimary
import com.hnl.kamistudio.ui.theme.GlassSecondary
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

@Composable
fun DashboardScreen(
    dao: KamiDao,
    onNavigate: (String) -> Unit,
    onSupportAuthor: () -> Unit
) {
    val allCodes by dao.getAll().collectAsState(initial = emptyList())
    val total = allCodes.size
    val active = allCodes.count { it.status == "active" }
    val inactive = allCodes.count { it.status == "inactive" }

    val menuItems = listOf(
        MenuItem("生成卡密", Icons.Default.AddCircle, GlassPrimary, "generate"),
        MenuItem("卡密管理", Icons.Default.List, GlassSecondary, "manage"),
        MenuItem("APK注入", Icons.Default.Build, GlassAccent, "inject"),
        MenuItem("验证预览", Icons.Default.Visibility, Color(0xFF00CEC9), "preview"),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 标题
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("卡密工坊", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text("专业卡密系统制作工具", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x22FFFFFF))
                    .clickable { onSupportAuthor() }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text("支持作者", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }

        // 统计卡片
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard("总卡密", total.toString(), GlassPrimary, Modifier.weight(1f))
            StatCard("已激活", active.toString(), Color(0xFF00B894), Modifier.weight(1f))
            StatCard("待激活", inactive.toString(), Color(0xFFFDCB6E), Modifier.weight(1f))
        }

        // 功能菜单
        Text("功能", color = Color.White.copy(alpha = 0.8f), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(menuItems) { item ->
                LiquidGlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate(item.route) }
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(item.color.copy(alpha = 0.6f), item.color.copy(alpha = 0.3f))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(item.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(item.label, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // 底部提示
        LiquidGlassCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF74B9FF), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    "APK注入需配合NP Manager使用，详见注入页面的操作指南",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    LiquidGlassCard(modifier = modifier, cornerRadius = 18.dp) {
        Column {
            Text(value, color = color, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(label, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
        }
    }
}

private data class MenuItem(
    val label: String,
    val icon: ImageVector,
    val color: Color,
    val route: String
)
