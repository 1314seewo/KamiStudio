package com.hnl.kamistudio.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hnl.kamistudio.ui.components.LiquidGlassButton
import com.hnl.kamistudio.ui.components.LiquidGlassCard
import com.hnl.kamistudio.ui.components.LiquidGlassTextField

@Composable
fun PreviewScreen(onSupportAuthor: () -> Unit) {
    var inputCode by remember { mutableStateOf("") }
    var verifyState by remember { mutableStateOf(VerifyState.IDLE) }
    var title by remember { mutableStateOf("卡密验证") }
    var subtitle by remember { mutableStateOf("请输入卡密以继续使用") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("验证界面预览", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)

        // 配置
        LiquidGlassCard(cornerRadius = 16.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("预览配置", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                LiquidGlassTextField(value = title, onValueChange = { title = it }, placeholder = "标题")
                LiquidGlassTextField(value = subtitle, onValueChange = { subtitle = it }, placeholder = "副标题")
            }
        }

        // 手机外框预览
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            // 手机外框
            Box(
                modifier = Modifier
                    .width(280.dp)
                    .height(520.dp)
                    .clip(RoundedCornerShape(36.dp))
                    .background(Color(0xFF1A1A2E))
                    .border(3.dp, Color(0xFF4A4A6A), RoundedCornerShape(36.dp))
                    .padding(8.dp)
            ) {
                // 屏幕内容
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(28.dp))
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFF0F0C29), Color(0xFF302B63))
                            )
                        )
                ) {
                    // 状态栏
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp, start = 20.dp, end = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("9:41", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.SignalCellularAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                            Icon(Icons.Default.Wifi, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                            Icon(Icons.Default.BatteryFull, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                        }
                    }

                    // 验证界面内容
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Logo区域
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(Color(0xFF6C5CE7), Color(0xFFA29BFE))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(subtitle, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)

                        Spacer(modifier = Modifier.height(28.dp))

                        // 卡密输入框 - 液态玻璃
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(Color(0x26FFFFFF), Color(0x0DFFFFFF))
                                    )
                                )
                                .border(1.dp, Color(0x44FFFFFF), RoundedCornerShape(14.dp))
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            TextField(
                                value = inputCode,
                                onValueChange = { inputCode = it },
                                placeholder = { Text("请输入卡密", color = Color.White.copy(alpha = 0.4f), fontSize = 13.sp) },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    cursorColor = Color(0xFFA29BFE),
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 验证按钮 - 液态玻璃
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(Color(0xFF6C5CE7), Color(0xFFA29BFE))
                                    )
                                )
                                .border(1.dp, Color(0x66FFFFFF), RoundedCornerShape(14.dp))
                                .clickable {
                                    verifyState = VerifyState.LOADING
                                    kotlinx.coroutines.GlobalScope.launch {
                                        kotlinx.coroutines.delay(1500)
                                        verifyState = if (inputCode.length >= 8) VerifyState.SUCCESS else VerifyState.FAIL
                                    }
                                }
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            when (verifyState) {
                                VerifyState.LOADING -> CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                else -> Text("验证", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 支持作者按钮
                        Text(
                            "支持作者",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp,
                            modifier = Modifier.clickable { onSupportAuthor() }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // 验证结果
                        AnimatedVisibility(visible = verifyState == VerifyState.SUCCESS) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF00B894).copy(alpha = 0.2f))
                                    .padding(10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("验证成功，正在进入...", color = Color(0xFF00B894), fontSize = 12.sp)
                            }
                        }
                        AnimatedVisibility(visible = verifyState == VerifyState.FAIL) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFE17055).copy(alpha = 0.2f))
                                    .padding(10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("卡密错误，请重新输入", color = Color(0xFFE17055), fontSize = 12.sp)
                            }
                        }
                    }

                    // 底部横条
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 8.dp)
                            .width(100.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White.copy(alpha = 0.3f))
                    )
                }
            }
        }

        Text("提示：输入任意8位以上字符可模拟验证成功", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp)
    }
}

private enum class VerifyState { IDLE, LOADING, SUCCESS, FAIL }
