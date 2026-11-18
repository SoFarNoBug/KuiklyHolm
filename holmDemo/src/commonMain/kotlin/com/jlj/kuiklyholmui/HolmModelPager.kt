package com.jlj.kuiklyholmui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jlj.kuiklybase.holm.HolmExecutor
import com.jlj.kuiklybase.holm.HolmModelContent
import com.jlj.kuiklybase.holm.HolmModelLight
import com.jlj.kuiklybase.holm.HolmSurface
import com.jlj.kuiklybase.holm.HolmSurfaceAttr
import com.jlj.kuiklyholmui.base.BasePager
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.width
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
 * @CreateDate: 2026/11/18
 * @Author: jlj
 * @Description: Android Model Content Demo（Khronos Box/Duck + 本地 hero 立方体）。
 */
@Page("holm_model", supportInLocal = true)
internal class HolmModelPager : BasePager() {
    override fun willInit() {
        super.willInit()
        setContent {
            HolmModelScreen()
        }
    }
}

private data class DemoModelAsset(
    val label: String,
    val path: String,
    val note: String,
)

private val DEMO_MODEL_ASSETS = listOf(
    DemoModelAsset("Duck", "models/Duck.glb", "Khronos · SCEA Shared Source"),
    DemoModelAsset("Box", "models/Box.glb", "Khronos · CC BY 4.0 Cesium"),
    DemoModelAsset("hero", "models/hero.glb", "本仓程序立方体（缺文件回落）"),
)

@Composable
private fun HolmModelScreen() {
    val statusBarHeight = LocalActivity.current.pageData.statusBarHeight
    var executor by remember { mutableStateOf(HolmExecutor.GL) }
    var asset by remember { mutableStateOf(DEMO_MODEL_ASSETS.first()) }
    var backendText by remember { mutableStateOf("waiting…") }
    var errorText by remember { mutableStateOf("") }
    val model = remember(asset.path) {
        HolmModelContent.fromAssets(
            path = asset.path,
            lights = listOf(
                HolmModelLight(
                    directionX = 0.4f,
                    directionY = 0.9f,
                    directionZ = 0.2f,
                    intensity = 1f,
                ),
            ),
            orbitEnabled = true,
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DemoTheme.Background)
            .padding(top = (statusBarHeight + 16).dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
    ) {
        Text(
            text = "Holm · Model / glTF",
            fontSize = 20.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Bold,
            color = DemoTheme.TextPrimary,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "executor=$executor | ${asset.path} | drag orbit | $backendText",
            fontSize = 13.sp,
            lineHeight = 13.sp,
            color = DemoTheme.TextSecondary,
        )
        if (errorText.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = errorText, fontSize = 13.sp, color = Color(0xFFFF3355))
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            DEMO_MODEL_ASSETS.forEach { item ->
                ModelExecChip(
                    label = item.label,
                    active = asset.path == item.path,
                    onClick = {
                        asset = item
                        errorText = ""
                        backendText = "waiting…"
                    },
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            ModelExecChip(
                label = "renderer-gl",
                active = !HolmExecutor.isWgpu(executor),
                onClick = {
                    executor = HolmExecutor.GL
                    errorText = ""
                    backendText = "waiting…"
                },
            )
            Spacer(modifier = Modifier.width(8.dp))
            ModelExecChip(
                label = "renderer-wgpu",
                active = HolmExecutor.isWgpu(executor),
                onClick = {
                    executor = HolmExecutor.WGPU
                    errorText = ""
                    backendText = "waiting…"
                },
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(DemoTheme.Surface),
            contentAlignment = Alignment.Center,
        ) {
            HolmSurface(
                content = model,
                modifier = Modifier.fillMaxSize(),
                playing = true,
                maxFps = HolmSurfaceAttr.DEFAULT_MAX_FPS,
                executor = executor,
                onReady = { ready ->
                    errorText = ""
                    backendText = "onReady backend=${ready.backend}"
                },
                onError = { err ->
                    errorText = "onError ${err.code}: ${err.message}"
                },
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "非 Android → MODEL_UNSUPPORTED。${asset.note}。详见 assets/models/ATTRIBUTION.md",
            fontSize = 11.sp,
            color = DemoTheme.TextSecondary,
        )
    }
}

@Composable
private fun ModelExecChip(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (active) DemoTheme.Primary else DemoTheme.Surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = DemoTheme.TextPrimary,
        )
    }
}
