package com.jlj.kuiklyholmui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.tencent.kuikly.compose.foundation.layout.fillMaxHeight
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
 * @CreateDate: 2026/11/12
 * @Author: jlj
 * @Description: Lite 五席尖刺页 — frost/glow/liquid_approx/gradient_bg/fullscreen_warp（策略 A）。
 */
@Page("holm_spike", supportInLocal = true)
internal class HolmSpikePager : BasePager() {
    override fun willInit() {
        super.willInit()
        setContent {
            HolmSpikeScreen()
        }
    }
}

private val LIQUID_BOOST_STEPS = floatArrayOf(1.15f, HolmSurfaceAttr.DEFAULT_LIQUID_BOOST, 1.6f)
private val LIQUID_BEND_STEPS = floatArrayOf(0.02f, HolmSurfaceAttr.DEFAULT_LIQUID_BEND, 0.10f)
private val WARP_AMOUNT_STEPS = floatArrayOf(0.015f, HolmSurfaceAttr.DEFAULT_WARP_AMOUNT, 0.09f)
private val SCROLL_SPEED_STEPS = floatArrayOf(0.4f, HolmSurfaceAttr.DEFAULT_SCROLL_SPEED, 2.0f)
private val MAX_FPS_STEPS = intArrayOf(0, 30, HolmSurfaceAttr.DEFAULT_MAX_FPS, 120)
private val MAX_FPS_LABELS = arrayOf("0", "30", "60", "120")

@Composable
private fun HolmSpikeScreen() {
    val statusBarHeight = LocalActivity.current.pageData.statusBarHeight
    var scene by remember { mutableStateOf(HolmSceneShaders.SCENE_FROST) }
    var liquidBoost by remember { mutableStateOf(HolmSurfaceAttr.DEFAULT_LIQUID_BOOST) }
    var liquidBend by remember { mutableStateOf(HolmSurfaceAttr.DEFAULT_LIQUID_BEND) }
    var warpAmount by remember { mutableStateOf(HolmSurfaceAttr.DEFAULT_WARP_AMOUNT) }
    var scrollSpeed by remember { mutableStateOf(HolmSurfaceAttr.DEFAULT_SCROLL_SPEED) }
    var maxFps by remember { mutableStateOf(HolmSurfaceAttr.DEFAULT_MAX_FPS) }
    var backendText by remember { mutableStateOf("waiting onReady…") }
    var errorText by remember { mutableStateOf("") }
    var perfText by remember { mutableStateOf("fps=—") }
    val isLiquid = scene == HolmSceneShaders.SCENE_LIQUID_APPROX
    val isWarp = scene == HolmSceneShaders.SCENE_FULLSCREEN_WARP
    val isGradient = scene == HolmSceneShaders.SCENE_GRADIENT_BG
    val knobStatus = when {
        isLiquid -> "boost=$liquidBoost | bend=$liquidBend"
        isWarp -> "warpAmount=$warpAmount"
        isGradient -> "scrollSpeed=$scrollSpeed"
        else -> "knobs=—"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1030))
            .padding(top = (statusBarHeight + 16).dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
    ) {
        Text(
            text = "Holm · Lite 特效",
            fontSize = 20.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Bold,
            color = DemoTheme.TextPrimary,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "scene=$scene | $knobStatus | maxFps=$maxFps | $perfText | $backendText",
            fontSize = 13.sp,
            lineHeight = 13.sp,
            color = DemoTheme.TextSecondary,
        )
        if (errorText.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = errorText,
                fontSize = 13.sp,
                lineHeight = 13.sp,
                color = DemoTheme.Primary,
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        SceneToggleRow(
            scene = scene,
            onSelect = { scene = it },
        )
        Spacer(modifier = Modifier.height(8.dp))
        IntParamStepRow(
            label = "maxFps",
            value = maxFps,
            steps = MAX_FPS_STEPS,
            labels = MAX_FPS_LABELS,
            onSelect = { maxFps = it },
        )
        if (isLiquid) {
            Spacer(modifier = Modifier.height(8.dp))
            ParamStepRow(
                label = "Boost",
                value = liquidBoost,
                steps = LIQUID_BOOST_STEPS,
                onSelect = { liquidBoost = it },
            )
            Spacer(modifier = Modifier.height(8.dp))
            ParamStepRow(
                label = "Bend",
                value = liquidBend,
                steps = LIQUID_BEND_STEPS,
                onSelect = { liquidBend = it },
            )
        }
        if (isWarp) {
            Spacer(modifier = Modifier.height(8.dp))
            ParamStepRow(
                label = "Warp",
                value = warpAmount,
                steps = WARP_AMOUNT_STEPS,
                onSelect = { warpAmount = it },
            )
        }
        if (isGradient) {
            Spacer(modifier = Modifier.height(8.dp))
            ParamStepRow(
                label = "Speed",
                value = scrollSpeed,
                steps = SCROLL_SPEED_STEPS,
                onSelect = { scrollSpeed = it },
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        ContrastBackdrop()
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
                warpAmount = warpAmount,
                scrollSpeed = scrollSpeed,
                maxFps = maxFps,
                onReady = { ready ->
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
            text = "五席：霜化/柔光/液态/渐变/场扭曲；旋钮按 scene 显示；maxFps=0 不限",
            fontSize = 12.sp,
            lineHeight = 14.sp,
            color = DemoTheme.TextSecondary,
        )
    }
}

@Composable
private fun SceneToggleRow(
    scene: String,
    onSelect: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            SceneChip(
                label = "霜化",
                active = scene == HolmSceneShaders.SCENE_FROST,
                onClick = { onSelect(HolmSceneShaders.SCENE_FROST) },
            )
            Spacer(modifier = Modifier.width(8.dp))
            SceneChip(
                label = "柔光",
                active = scene == HolmSceneShaders.SCENE_GLOW,
                onClick = { onSelect(HolmSceneShaders.SCENE_GLOW) },
            )
            Spacer(modifier = Modifier.width(8.dp))
            SceneChip(
                label = "液态",
                active = scene == HolmSceneShaders.SCENE_LIQUID_APPROX,
                onClick = { onSelect(HolmSceneShaders.SCENE_LIQUID_APPROX) },
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            SceneChip(
                label = "渐变",
                active = scene == HolmSceneShaders.SCENE_GRADIENT_BG,
                onClick = { onSelect(HolmSceneShaders.SCENE_GRADIENT_BG) },
            )
            Spacer(modifier = Modifier.width(8.dp))
            SceneChip(
                label = "场扭曲",
                active = scene == HolmSceneShaders.SCENE_FULLSCREEN_WARP,
                onClick = { onSelect(HolmSceneShaders.SCENE_FULLSCREEN_WARP) },
            )
        }
    }
}

@Composable
private fun ParamStepRow(
    label: String,
    value: Float,
    steps: FloatArray,
    onSelect: (Float) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            fontSize = 13.sp,
            lineHeight = 13.sp,
            color = DemoTheme.TextSecondary,
            modifier = Modifier.width(56.dp),
        )
        SceneChip(
            label = "Lo",
            active = value == steps[0],
            onClick = { onSelect(steps[0]) },
        )
        Spacer(modifier = Modifier.width(8.dp))
        SceneChip(
            label = "默认",
            active = value == steps[1],
            onClick = { onSelect(steps[1]) },
        )
        Spacer(modifier = Modifier.width(8.dp))
        SceneChip(
            label = "Hi",
            active = value == steps[2],
            onClick = { onSelect(steps[2]) },
        )
    }
}

@Composable
private fun IntParamStepRow(
    label: String,
    value: Int,
    steps: IntArray,
    labels: Array<String>,
    onSelect: (Int) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            fontSize = 13.sp,
            lineHeight = 13.sp,
            color = DemoTheme.TextSecondary,
            modifier = Modifier.width(56.dp),
        )
        val count = minOf(steps.size, labels.size)
        for (i in 0 until count) {
            if (i > 0) {
                Spacer(modifier = Modifier.width(8.dp))
            }
            val step = steps[i]
            SceneChip(
                label = labels[i],
                active = value == step,
                onClick = { onSelect(step) },
            )
        }
    }
}

@Composable
private fun SceneChip(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    val bg = if (active) DemoTheme.Primary else Color(0xFF2A2F38)
    Box(
        modifier = Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            lineHeight = 14.sp,
            color = DemoTheme.TextPrimary,
        )
    }
}

@Composable
private fun ContrastBackdrop() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(8.dp)),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(Color(0xFF266FFF)),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(Color(0xFFFF5A3D)),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(Color(0xFF2ECC71)),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(Color(0xFFF1C40F)),
        )
    }
}
