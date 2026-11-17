package com.jlj.kuiklyholmui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jlj.kuiklyholmui.base.BasePager
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.platform.LocalActivity
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page

/**
 * @CreateDate: 2026/11/16
 * @Author: jlj
 * @Description: ADR 0013 v1.1 冒烟：无 HolmSurface；headless Compute ×2 回读。
 */
@Page("kmp_wgpu_compute", supportInLocal = true)
internal class ComputeReadbackPager : BasePager() {
    override fun willInit() {
        super.willInit()
        HolmDemoBootstrap.ensure()
        setContent {
            ComputeReadbackScreen()
        }
    }
}

@Composable
private fun ComputeReadbackScreen() {
    val statusBarHeight = LocalActivity.current.pageData.statusBarHeight
    var status by remember { mutableStateOf("idle · tap Run") }
    var running by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DemoTheme.Background)
            .padding(top = (statusBarHeight + 16).dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
    ) {
        Text(
            text = "kmp-wgpu · Compute 回读",
            fontSize = 20.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Bold,
            color = DemoTheme.TextPrimary,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "无 Surface · FloatArray ×2 · ADR 0013 v1.1",
            fontSize = 13.sp,
            lineHeight = 13.sp,
            color = DemoTheme.TextSecondary,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(if (running) DemoTheme.Surface else DemoTheme.Primary)
                .clickable(enabled = !running) {
                    running = true
                    status = "running…"
                    runComputeReadbackSample { result ->
                        running = false
                        status = result.fold(
                            onSuccess = { it },
                            onFailure = { "FAIL: ${it.message}" },
                        )
                    }
                }
                .padding(horizontal = 40.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (running) "Running…" else "Run sample ×2",
                fontSize = 16.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Bold,
                color = DemoTheme.TextPrimary,
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = status,
            fontSize = 14.sp,
            lineHeight = 18.sp,
            color = if (status.startsWith("FAIL")) Color(0xFFFF3355) else DemoTheme.TextPrimary,
        )
    }
}
