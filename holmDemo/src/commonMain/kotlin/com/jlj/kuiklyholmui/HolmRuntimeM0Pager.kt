package com.jlj.kuiklyholmui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import kotlinx.coroutines.delay

/**
 * @CreateDate: 2026/11/12
 * @Author: jlj
 * @Description: Demo 主推：多样本 chip（隧道 + IQ SDF 原语）+ gl/wgpu 双轨与 JSONL 采集。
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
private val ISLAND_FAIL_BLUE = Color(0xFF051447)
private val ERROR_RED = Color(0xFFFF3355)
/** 点「开始采集」后自动记 JSONL 的时长；到期停采，便于复制给 AI。 */
private const val COLLECT_DURATION_MS: Long = 10_000L

@Composable
private fun HolmRuntimeM0Screen() {
    val statusBarHeight = LocalActivity.current.pageData.statusBarHeight
    var backendText by remember { mutableStateOf("waiting onReady…") }
    var backendId by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf("") }
    var perfText by remember { mutableStateOf("fps=—") }
    var executor by remember { mutableStateOf(HolmExecutor.RUNTIME) }
    var maxFps by remember { mutableStateOf(HolmSurfaceAttr.DEFAULT_MAX_FPS) }
    var islandSize by remember { mutableStateOf(IntSize(1, 1)) }
    var collecting by remember { mutableStateOf(false) }
    var runId by remember { mutableStateOf(0) }
    var sampleIndex by remember { mutableStateOf(0) }
    var sample by remember { mutableStateOf(DemoSample.TUNNEL) }
    // SHADER_/WGPU_ 失败后必须 false，否则 Attr 持续 playing=true 会把泵拉起来播 ClearTri
    var surfacePlaying by remember { mutableStateOf(true) }
    val scene = NeonTunnelShaders.SCENE_CUSTOM
    val fragmentShader = sample.fragmentFor(executor)

    // runId 变一次 = 新一轮 10s；到期自动停并打 END（给人看的页面不堆 JSON）
    LaunchedEffect(runId, collecting) {
        if (!collecting) return@LaunchedEffect
        delay(COLLECT_DURATION_MS)
        if (collecting) {
            collecting = false
            println("[holm-perf] END runId=$runId samples=$sampleIndex reason=timeout")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DemoTheme.Background)
            .padding(top = (statusBarHeight + 16).dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
    ) {
        Text(
            text = "Holm · 示例 / 双轨",
            fontSize = 20.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Bold,
            color = DemoTheme.TextPrimary,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "executor=$executor | sample=${sample.id} | scene=$scene | maxFps=$maxFps | $perfText | ${islandSize.width}x${islandSize.height} | $backendText",
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
                surfacePlaying = true
                errorText = ""
                // 等 onReady 前勿粘旧 backend（审查 P1-doc-drift）
                backendId = ""
                backendText = "waiting onReady…"
            },
        )
        Spacer(modifier = Modifier.height(8.dp))
        RuntimeExampleRow(
            sample = sample,
            onSelect = { next ->
                // 仅换 fragmentShader 不会再 onReady；勿清 backend（审查 P2）
                // 换样本须清 sticky 红字，否则「故意失败」残留挡下一档
                errorText = ""
                surfacePlaying = true
                sample = next
            },
        )
        if (sample == DemoSample.IQ_PRIMITIVES) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = IqPrimitivesShaders.UI_CREDIT,
                fontSize = 11.sp,
                lineHeight = 12.sp,
                color = DemoTheme.TextSecondary,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        RuntimeMaxFpsRow(value = maxFps, onSelect = { maxFps = it })
        Spacer(modifier = Modifier.height(8.dp))
        RuntimeChip(
            label = if (collecting) "停止采集（10s）" else "开始采集（10s）",
            active = collecting,
            onClick = {
                if (collecting) {
                    collecting = false
                    println("[holm-perf] END runId=$runId samples=$sampleIndex reason=manual")
                } else {
                    runId += 1
                    sampleIndex = 0
                    collecting = true
                    println(
                        "[holm-perf] BEGIN runId=$runId executor=$executor scene=$scene " +
                            "maxFps=$maxFps example=${sample.jsonExample} sample=${sample.id} " +
                            "backend=$backendId ${islandSize.width}x${islandSize.height}",
                    )
                }
            },
        )
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
                executor = executor,
                fragmentShader = fragmentShader,
                onReady = { ready ->
                    errorText = ""
                    backendId = ready.backend
                    backendText = "onReady backend=${ready.backend} quality=${ready.quality}"
                },
                onError = { err ->
                    val code = err.code
                    // 成功换档后迟到的 SHADER_* 回调勿盖住新样本
                    if ((code == "SHADER_CUSTOM" || code == "SHADER_COMPILE") &&
                        sample != DemoSample.BAD_COMPILE
                    ) {
                        return@HolmSurface
                    }
                    errorText = "onError $code: ${err.message}"
                    // SHADER_WRAP 是切轨/空片元过渡噪声，勿停泵；真正编不过才停
                    if (code == "SHADER_CUSTOM" || code == "SHADER_COMPILE" ||
                        code.startsWith("WGPU_") || code == "TICK" || code == "SURFACE"
                    ) {
                        surfacePlaying = false
                    }
                },
                onPerf = { perf ->
                    val ms = ((perf.frameMs * 10f).toInt() / 10f)
                    perfText = "fps=${perf.fps.toInt()} (${ms}ms)"
                    // 泵恢复出帧后清残留 error；SHADER_*/WGPU_* 须业务可见，不被 fps 冲掉
                    if (errorText.isNotEmpty() && perf.fps > 0.01f) {
                        val sticky = errorText.contains("SHADER_") ||
                            errorText.contains("WGPU_") ||
                            errorText.contains("TICK") ||
                            errorText.contains("SURFACE")
                        if (!sticky) {
                            errorText = ""
                        }
                    }
                    if (collecting) {
                        sampleIndex += 1
                        val line = buildString {
                            append('{')
                            append("\"runId\":").append(runId).append(',')
                            append("\"ts\":").append(sampleIndex).append(',')
                            append("\"sampleIndex\":").append(sampleIndex).append(',')
                            append("\"executor\":\"").append(executor).append("\",")
                            append("\"backend\":\"").append(escapeJson(backendId)).append("\",")
                            append("\"maxFps\":").append(maxFps).append(',')
                            append("\"example\":").append(sample.jsonExample).append(',')
                            append("\"sample\":\"").append(sample.id).append("\",")
                            append("\"fps\":").append(perf.fps).append(',')
                            append("\"frameMs\":").append(perf.frameMs).append(',')
                            append("\"width\":").append(islandSize.width).append(',')
                            append("\"height\":").append(islandSize.height)
                            append('}')
                        }
                        // H5：println → console；Android：logcat。前缀便于 AI/DevTools 过滤
                        println("[holm-perf] $line")
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
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = if (collecting) {
                "采集中…控制台 [holm-perf]，约 10s 自动停。"
            } else {
                "开始采集 → 控制台/[logcat] 静默打 [holm-perf] JSONL（约 10s）。"
            },
            fontSize = 12.sp,
            lineHeight = 14.sp,
            color = DemoTheme.TextSecondary,
        )
    }
}

@Composable
private fun RuntimeExecutorRow(
    executor: String,
    onSelect: (String) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "轨",
            fontSize = 13.sp,
            lineHeight = 13.sp,
            color = DemoTheme.TextSecondary,
            modifier = Modifier
                .padding(end = 8.dp)
                .align(Alignment.CenterVertically),
        )
        RuntimeChip(
            label = "Lite",
            active = !HolmExecutor.isRuntime(executor),
            onClick = { onSelect(HolmExecutor.LITE) },
        )
        Spacer(modifier = Modifier.width(8.dp))
        RuntimeChip(
            label = "Runtime",
            active = HolmExecutor.isRuntime(executor),
            onClick = { onSelect(HolmExecutor.RUNTIME) },
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

private fun escapeJson(s: String): String =
    s.replace("\\", "\\\\").replace("\"", "\\\"")
