package com.jlj.kuiklyholmui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jlj.kuiklybase.holm.HolmExecutor
import com.jlj.kuiklybase.holm.HolmSceneShaders
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
import com.tencent.kuikly.compose.ui.platform.LocalActivity
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page

/**
 * @CreateDate: 2026/11/12
 * @Author: jlj
 * @Description: DualTrack B · Runtime 旁路；前三席 frost/glow/liquid 观感近似对照 Lite。
 */
@Page("holm_runtime_m0", supportInLocal = true)
internal class HolmRuntimeM0Pager : BasePager() {
    override fun willInit() {
        super.willInit()
        setContent {
            HolmRuntimeM0Screen()
        }
    }
}

private val RUNTIME_MAX_FPS_STEPS = intArrayOf(0, 30, HolmSurfaceAttr.DEFAULT_MAX_FPS, 120)
private val RUNTIME_MAX_FPS_LABELS = arrayOf("0", "30", "60", "120")

@Composable
private fun HolmRuntimeM0Screen() {
    val statusBarHeight = LocalActivity.current.pageData.statusBarHeight
    var backendText by remember { mutableStateOf("waiting onReady…") }
    var errorText by remember { mutableStateOf("") }
    var perfText by remember { mutableStateOf("fps=—") }
    var scene by remember { mutableStateOf(HolmSceneShaders.SCENE_FROST) }
    var maxFps by remember { mutableStateOf(HolmSurfaceAttr.DEFAULT_MAX_FPS) }
    var liquidBoost by remember { mutableStateOf(HolmSurfaceAttr.DEFAULT_LIQUID_BOOST) }
    var liquidBend by remember { mutableStateOf(HolmSurfaceAttr.DEFAULT_LIQUID_BEND) }
    val isLiquid = scene == HolmSceneShaders.SCENE_LIQUID_APPROX

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DemoTheme.Background)
            .padding(top = (statusBarHeight + 16).dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
    ) {
        Text(
            text = "Holm · Runtime Lite3",
            fontSize = 20.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Bold,
            color = DemoTheme.TextPrimary,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "executor=${HolmExecutor.RUNTIME} | scene=$scene | maxFps=$maxFps | $perfText | $backendText",
            fontSize = 13.sp,
            lineHeight = 13.sp,
            color = DemoTheme.TextSecondary,
        )
        if (errorText.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = errorText,
                fontSize = 13.sp,
                lineHeight = 14.sp,
                color = DemoTheme.Primary,
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        RuntimeSceneRow(scene = scene, onSelect = { scene = it })
        Spacer(modifier = Modifier.height(8.dp))
        RuntimeMaxFpsRow(value = maxFps, onSelect = { maxFps = it })
        if (isLiquid) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "boost=$liquidBoost bend=$liquidBend（点切换）",
                fontSize = 12.sp,
                lineHeight = 12.sp,
                color = DemoTheme.TextSecondary,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                RuntimeChip(
                    label = "boost+",
                    active = false,
                    onClick = { liquidBoost = (liquidBoost + 0.25f).coerceAtMost(2.5f) },
                )
                Spacer(modifier = Modifier.width(8.dp))
                RuntimeChip(
                    label = "boost-",
                    active = false,
                    onClick = { liquidBoost = (liquidBoost - 0.25f).coerceAtLeast(1.0f) },
                )
                Spacer(modifier = Modifier.width(8.dp))
                RuntimeChip(
                    label = "bend+",
                    active = false,
                    onClick = { liquidBend = (liquidBend + 0.03f).coerceAtMost(0.2f) },
                )
                Spacer(modifier = Modifier.width(8.dp))
                RuntimeChip(
                    label = "bend-",
                    active = false,
                    onClick = { liquidBend = (liquidBend - 0.03f).coerceAtLeast(0.0f) },
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(DemoTheme.Surface),
            contentAlignment = Alignment.Center,
        ) {
            HolmSurface(
                scene = scene,
                modifier = Modifier.fillMaxSize(),
                playing = true,
                liquidBoost = liquidBoost,
                liquidBend = liquidBend,
                maxFps = maxFps,
                executor = HolmExecutor.RUNTIME,
                onReady = { ready ->
                    errorText = ""
                    backendText = "onReady backend=${ready.backend} quality=${ready.quality}"
                },
                onError = { err ->
                    errorText = "onError ${err.code}: ${err.message}"
                },
                onPerf = { perf ->
                    val ms = ((perf.frameMs * 10f).toInt() / 10f)
                    perfText = "fps=${perf.fps.toInt()} (${ms}ms)"
                },
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "对照 C：回 Home 进 Lite。maxFps>60=timer；0=rAF+AutoNoVsync。backend 可能为 wgpu-m0-gl。失败 BLOCKED。",
            fontSize = 12.sp,
            lineHeight = 14.sp,
            color = DemoTheme.TextSecondary,
        )
    }
}

@Composable
private fun RuntimeSceneRow(
    scene: String,
    onSelect: (String) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        RuntimeChip(
            label = "霜化",
            active = scene == HolmSceneShaders.SCENE_FROST,
            onClick = { onSelect(HolmSceneShaders.SCENE_FROST) },
        )
        Spacer(modifier = Modifier.width(8.dp))
        RuntimeChip(
            label = "柔光",
            active = scene == HolmSceneShaders.SCENE_GLOW,
            onClick = { onSelect(HolmSceneShaders.SCENE_GLOW) },
        )
        Spacer(modifier = Modifier.width(8.dp))
        RuntimeChip(
            label = "液态",
            active = scene == HolmSceneShaders.SCENE_LIQUID_APPROX,
            onClick = { onSelect(HolmSceneShaders.SCENE_LIQUID_APPROX) },
        )
    }
}

@Composable
private fun RuntimeMaxFpsRow(
    value: Int,
    onSelect: (Int) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "maxFps",
            fontSize = 13.sp,
            lineHeight = 13.sp,
            color = DemoTheme.TextSecondary,
            modifier = Modifier
                .padding(end = 8.dp)
                .align(Alignment.CenterVertically),
        )
        val count = minOf(RUNTIME_MAX_FPS_STEPS.size, RUNTIME_MAX_FPS_LABELS.size)
        for (i in 0 until count) {
            if (i > 0) {
                Spacer(modifier = Modifier.width(8.dp))
            }
            val step = RUNTIME_MAX_FPS_STEPS[i]
            RuntimeChip(
                label = RUNTIME_MAX_FPS_LABELS[i],
                active = value == step,
                onClick = { onSelect(step) },
            )
        }
    }
}

@Composable
private fun RuntimeChip(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (active) DemoTheme.Primary else DemoTheme.Surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            lineHeight = 13.sp,
            color = DemoTheme.TextPrimary,
        )
    }
}
