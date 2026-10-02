package com.hnl.kamistudio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hnl.kamistudio.ui.screens.*
import com.hnl.kamistudio.ui.theme.KamiStudioTheme
import com.hnl.kamistudio.ui.theme.GlassBackgroundStart
import com.hnl.kamistudio.ui.theme.GlassBackgroundEnd

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KamiStudioTheme {
                KamiAppContent()
            }
        }
    }
}

@Composable
fun KamiAppContent() {
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as KamiApp
    val dao = app.database.kamiDao()
    var currentRoute by remember { mutableStateOf("dashboard") }
    var showSupportDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(GlassBackgroundStart, GlassBackgroundEnd)
                )
            )
    ) {
        // 内容区域
        Box(modifier = Modifier.fillMaxSize().padding(bottom = 70.dp)) {
            when (currentRoute) {
                "dashboard" -> DashboardScreen(dao, onNavigate = { currentRoute = it }, onSupportAuthor = { showSupportDialog = true })
                "generate" -> GenerateScreen(dao)
                "manage" -> ManageScreen(dao)
                "inject" -> InjectScreen()
                "preview" -> PreviewScreen(onSupportAuthor = { showSupportDialog = true })
                "settings" -> SettingsScreen(dao, onSupportAuthor = { showSupportDialog = true })
            }
        }

        // 底部导航栏 - 液态玻璃
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0x33FFFFFF), Color(0x11FFFFFF))
                    )
                )
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                BottomNavItem("dashboard", Icons.Default.Home, "首页", currentRoute) { currentRoute = it }
                BottomNavItem("generate", Icons.Default.AddCircle, "生成", currentRoute) { currentRoute = it }
                BottomNavItem("inject", Icons.Default.Build, "注入", currentRoute) { currentRoute = it }
                BottomNavItem("manage", Icons.Default.List, "管理", currentRoute) { currentRoute = it }
                BottomNavItem("settings", Icons.Default.Settings, "设置", currentRoute) { currentRoute = it }
            }
        }

        // 支持作者弹窗
        if (showSupportDialog) {
            SupportAuthorDialog(onDismiss = { showSupportDialog = false })
        }
    }
}

@Composable
private fun BottomNavItem(
    route: String,
    icon: ImageVector,
    label: String,
    currentRoute: String,
    onSelect: (String) -> Unit
) {
    val isSelected = currentRoute == route
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onSelect(route) }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (isSelected) Color(0xFFA29BFE) else Color.White.copy(alpha = 0.5f),
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            label,
            color = if (isSelected) Color(0xFFA29BFE) else Color.White.copy(alpha = 0.5f),
            fontSize = 10.sp
        )
    }
}

@Composable
private fun SupportAuthorDialog(onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0x44FFFFFF), Color(0x1AFFFFFF))
                    )
                )
                .padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("支持作者", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("感谢你的支持，你的鼓励是我持续更新的动力", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                Spacer(modifier = Modifier.height(16.dp))

                // 赞赏码图片 - 使用内置资源，无需网络
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.reward_qr),
                        contentDescription = "微信赞赏码",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text("HNL 的赞赏码", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)

                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(Color(0xFF6C5CE7), Color(0xFFA29BFE))
                            )
                        )
                        .clickable { onDismiss() }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("感谢支持", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
