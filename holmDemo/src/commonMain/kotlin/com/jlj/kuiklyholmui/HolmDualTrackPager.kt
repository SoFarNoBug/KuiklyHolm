package com.jlj.kuiklyholmui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jlj.kuiklybase.holm.HolmExecutor
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
import com.tencent.kuikly.compose.ui.layout.onSizeChanged
import com.tencent.kuikly.compose.ui.platform.LocalActivity
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.IntSize
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page

/**
 * @CreateDate: 2026/11/12
 * @Author: jlj
 * @Description: Demo 主推：多样本 chip（隧道 / IQ / 程序化海洋 / 故意失败）+ renderer-gl / renderer-wgpu 双渲染后端。
 */
@Page("holm_dualtrack", supportInLocal = true)
internal class HolmDualTrackPager : BasePager() {
    override fun willInit() {
        super.willInit()
        setContent {
            HolmDualTrackScreen()
        }
    }
}

private val RUNTIME_MAX_FPS_STEPS = intArrayOf(0, 30, HolmSurfaceAttr.DEFAULT_MAX_FPS, 120)
private val RUNTIME_MAX_FPS_LABELS = arrayOf("0", "30", "60", "120")
private val RUNTIME_RENDER_SCALE_STEPS = floatArrayOf(1.0f, 0.5f, 0.25f)
private val RUNTIME_RENDER_SCALE_LABELS = arrayOf("1.0", "0.5", "0.25")
private val ISLAND_FAIL_BLUE = Color(0xFF051447)
private val ERROR_RED = Color(0xFFFF3355)

@Composable
private fun HolmDualTrackScreen() {
    val statusBarHeight = LocalActivity.current.pageData.statusBarHeight
    var backendText by remember { mutableStateOf("waiting onReady…") }
    var errorText by remember { mutableStateOf("") }
    var perfText by remember { mutableStateOf("fps=—") }
    var compileText by remember { mutableStateOf("compile=—") }
    var executor by remember { mutableStateOf(HolmExecutor.WGPU) }
    var maxFps by remember { mutableStateOf(HolmSurfaceAttr.DEFAULT_MAX_FPS) }
    var renderScale by remember { mutableStateOf(HolmSurfaceAttr.DEFAULT_RENDER_SCALE) }
    var islandSize by remember { mutableStateOf(IntSize(1, 1)) }
    var sample by remember { mutableStateOf(DemoSample.TUNNEL) }
    // SHADER_/WGPU_ 失败后必须 false，否则 Attr 持续 playing=true 会把泵拉起来播 ClearTri
    var surfacePlaying by remember { mutableStateOf(true) }
    // 换样本代数：成功 compile 后丢弃迟到的上一档 onError（P2-h5-error-sticky-ui）
    var shaderSeq by remember { mutableStateOf(0) }
    var compiledOkSeq by remember { mutableStateOf(-1) }
    val scene = NeonTunnelShaders.SCENE_CUSTOM
    val fragmentShader = sample.fragmentFor(executor)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DemoTheme.Background)
            .padding(top = (statusBarHeight + 16).dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
    ) {
        Text(
            text = "Holm · 示例 / 双渲染后端",
            fontSize = 20.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Bold,
            color = DemoTheme.TextPrimary,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "executor=$executor | sample=${sample.id} | scene=$scene | maxFps=$maxFps | scale=$renderScale | $perfText | $compileText | ${islandSize.width}x${islandSize.height} | $backendText",
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
                color = ERROR_RED,
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        RuntimeExecutorRow(
            executor = executor,
            onSelect = { next ->
                executor = next
                shaderSeq += 1
                compiledOkSeq = -1
                surfacePlaying = true
                errorText = ""
                // 等 onReady 前勿粘旧 backend（审查 P1-doc-drift）
                backendText = "waiting onReady…"
            },
        )
        Spacer(modifier = Modifier.height(8.dp))
        RuntimeExampleRow(
            sample = sample,
            onSelect = { next ->
                // 仅换 fragmentShader 不会再 onReady；勿清 backend（审查 P2）
                // 换样本须清 sticky 红字，否则「故意失败」残留挡下一档
                shaderSeq += 1
                compiledOkSeq = -1
                errorText = ""
                surfacePlaying = true
                sample = next
            },
        )
        sample.uiCredit()?.let { credit ->
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = credit,
                fontSize = 11.sp,
                lineHeight = 12.sp,
                color = DemoTheme.TextSecondary,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        RuntimeMaxFpsRow(value = maxFps, onSelect = { maxFps = it })
        Spacer(modifier = Modifier.height(8.dp))
        RuntimeRenderScaleRow(value = renderScale, onSelect = { renderScale = it })
        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (errorText.isNotEmpty()) ISLAND_FAIL_BLUE else DemoTheme.Surface)
                .onSizeChanged { islandSize = it },
            contentAlignment = Alignment.Center,
        ) {
            HolmSurface(
                scene = scene,
                modifier = Modifier.fillMaxSize(),
                playing = surfacePlaying,
                maxFps = maxFps,
                renderScale = renderScale,
                executor = executor,
                fragmentShader = fragmentShader,
                onReady = { ready ->
                    errorText = ""
                    backendText = "onReady backend=${ready.backend} quality=${ready.quality}"
                },
                onError = { err ->
                    val code = err.code
                    // 本代已 compile ok：忽略迟到 SHADER_*（换档后 sticky）
                    val ignoreStaleShader =
                        compiledOkSeq == shaderSeq &&
                            (code == "SHADER_CUSTOM" || code == "SHADER_COMPILE" || code == "SHADER_WRAP")
                    if (!ignoreStaleShader) {
                        // 正样本编挂也要红字（勿仅「故意失败」才显示）；换轨/换样本已清 errorText
                        errorText = "onError $code: ${err.message}"
                        // SHADER_WRAP 是切轨/空片元过渡噪声，勿停泵；真正编不过才停
                        if (code == "SHADER_CUSTOM" || code == "SHADER_COMPILE" ||
                            code.startsWith("WGPU_") || code == "TICK" || code == "SURFACE"
                        ) {
                            surfacePlaying = false
                        }
                    }
                },
                onShaderCompiled = { compiled ->
                    val wrap = ((compiled.wrapMs * 10f).toInt() / 10f)
                    val compile = ((compiled.compileMs * 10f).toInt() / 10f)
                    val okLabel = if (compiled.ok) "ok" else "fail"
                    compileText =
                        "compile $okLabel wrap=${wrap}ms compile=${compile}ms (${compiled.executor})"
                    // 同样本编过：清残留红字并恢复泵（失败样本曾 surfacePlaying=false）
                    if (compiled.ok) {
                        compiledOkSeq = shaderSeq
                        errorText = ""
                        surfacePlaying = true
                    }
                },
                onPerf = { perf ->
                    val ms = ((perf.frameMs * 10f).toInt() / 10f)
                    perfText = "fps=${perf.fps.toInt()} (${ms}ms)"
                    // 泵已出帧且本代 compile ok：清残留红字
                    if (errorText.isNotEmpty() &&
                        perf.fps > 0.01f &&
                        compiledOkSeq == shaderSeq
                    ) {
                        errorText = ""
                    }
                },
            )
            if (errorText.isNotEmpty()) {
                Text(
                    text = errorText,
                    fontSize = 13.sp,
                    lineHeight = 15.sp,
                    color = ERROR_RED,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }
    }
}

@Composable
private fun RuntimeExecutorRow(
    executor: String,
    onSelect: (String) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "渲染后端",
            fontSize = 13.sp,
            lineHeight = 13.sp,
            color = DemoTheme.TextSecondary,
            modifier = Modifier
                .padding(end = 8.dp)
                .align(Alignment.CenterVertically),
        )
        RuntimeChip(
            label = "renderer-gl",
            active = !HolmExecutor.isWgpu(executor),
            onClick = { onSelect(HolmExecutor.GL) },
        )
        Spacer(modifier = Modifier.width(8.dp))
        RuntimeChip(
            label = "renderer-wgpu",
            active = HolmExecutor.isWgpu(executor),
            onClick = { onSelect(HolmExecutor.WGPU) },
        )
    }
}

@Composable
private fun RuntimeExampleRow(
    sample: DemoSample,
    onSelect: (DemoSample) -> Unit,
) {
    val items = DemoSampleCatalog.ALL
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "示例",
            fontSize = 13.sp,
            lineHeight = 13.sp,
            color = DemoTheme.TextSecondary,
            modifier = Modifier
                .padding(end = 8.dp)
                .align(Alignment.Top),
        )
        // 每行最多 3 chip，避免窄屏挤出（样本增多仍分行）
        Column(modifier = Modifier.weight(1f)) {
            items.chunked(3).forEachIndexed { rowIndex, rowItems ->
                if (rowIndex > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    rowItems.forEachIndexed { i, item ->
                        if (i > 0) {
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        RuntimeChip(
                            label = item.label,
                            active = sample == item,
                            compact = true,
                            onClick = { onSelect(item) },
                        )
                    }
                }
            }
        }
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
private fun RuntimeRenderScaleRow(
    value: Float,
    onSelect: (Float) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "scale",
            fontSize = 13.sp,
            lineHeight = 13.sp,
            color = DemoTheme.TextSecondary,
            modifier = Modifier
                .padding(end = 8.dp)
                .align(Alignment.CenterVertically),
        )
        val count = minOf(RUNTIME_RENDER_SCALE_STEPS.size, RUNTIME_RENDER_SCALE_LABELS.size)
        for (i in 0 until count) {
            if (i > 0) {
                Spacer(modifier = Modifier.width(8.dp))
            }
            val step = RUNTIME_RENDER_SCALE_STEPS[i]
            RuntimeChip(
                label = RUNTIME_RENDER_SCALE_LABELS[i],
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
    compact: Boolean = false,
) {
    val hPad = if (compact) 10.dp else 12.dp
    val vPad = if (compact) 6.dp else 8.dp
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (active) DemoTheme.Primary else DemoTheme.Surface)
            .clickable(onClick = onClick)
            .padding(horizontal = hPad, vertical = vPad),
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            lineHeight = 13.sp,
            color = DemoTheme.TextPrimary,
        )
    }
}
