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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * @CreateDate: 2026/11/18
 * @Author: jlj
 * @Description: Android Model Content Demo（Khronos Duck + three.js 示例 GLB）。
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
    DemoModelAsset("Flamingo", "models/Flamingo.glb", "three.js examples"),
    DemoModelAsset("Parrot", "models/Parrot.glb", "three.js examples"),
    DemoModelAsset("Stork", "models/Stork.glb", "three.js examples"),
    DemoModelAsset("Horse", "models/Horse.glb", "three.js examples"),
    DemoModelAsset("Soldier", "models/Soldier.glb", "three.js examples"),
    DemoModelAsset("SittingBox", "models/SittingBox.glb", "three.js examples"),
    DemoModelAsset("Tokyo", "models/LittlestTokyo.glb", "three.js examples · 较大"),
    DemoModelAsset("IonDrive", "models/PrimaryIonDrive.glb", "three.js examples · 较大"),
    DemoModelAsset("Duck", "models/Duck.glb", "Khronos · SCEA Shared Source"),
)

private val DEMO_MODEL_ASSET_ROWS = DEMO_MODEL_ASSETS.chunked(3)

/** 默认 Key 灯 ≈ (0.4, 0.9, 0.2) 的球坐标初值。 */
private const val DEFAULT_LIGHT_AZIMUTH = 63f
private const val DEFAULT_LIGHT_ELEVATION = 64f
private const val DEFAULT_LIGHT_INTENSITY = 1f

@Composable
private fun HolmModelScreen() {
    val statusBarHeight = LocalActivity.current.pageData.statusBarHeight
    var executor by remember { mutableStateOf(HolmExecutor.GL) }
    var asset by remember { mutableStateOf(DEMO_MODEL_ASSETS.first()) }
    var backendText by remember { mutableStateOf("waiting…") }
    var perfText by remember { mutableStateOf("fps=—") }
    var errorText by remember { mutableStateOf("") }
    var lightsOpen by remember { mutableStateOf(false) }
    var litEnabled by remember { mutableStateOf(true) }
    var lightAzimuth by remember { mutableStateOf(DEFAULT_LIGHT_AZIMUTH) }
    var lightElevation by remember { mutableStateOf(DEFAULT_LIGHT_ELEVATION) }
    var lightIntensity by remember { mutableStateOf(DEFAULT_LIGHT_INTENSITY) }

    val model = remember(asset.path) {
        HolmModelContent.fromAssets(
            path = asset.path,
            lights = buildDemoLights(
                lit = litEnabled,
                azimuthDeg = lightAzimuth,
                elevationDeg = lightElevation,
                intensity = lightIntensity,
            ),
            orbitEnabled = true,
        )
    }

    fun pushLights() {
        model.setLights(
            buildDemoLights(
                lit = litEnabled,
                azimuthDeg = lightAzimuth,
                elevationDeg = lightElevation,
                intensity = lightIntensity,
            ),
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
            text = "executor=$executor | ${asset.path} | $perfText | drag orbit | $backendText",
            fontSize = 13.sp,
            lineHeight = 13.sp,
            color = DemoTheme.TextSecondary,
        )
        if (errorText.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = errorText, fontSize = 13.sp, color = Color(0xFFFF3355))
        }
        Spacer(modifier = Modifier.height(12.dp))
        DEMO_MODEL_ASSET_ROWS.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                row.forEach { item ->
                    ModelExecChip(
                        label = item.label,
                        active = asset.path == item.path,
                        onClick = {
                            asset = item
                            errorText = ""
                            backendText = "waiting…"
                            perfText = "fps=—"
                        },
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            ModelExecChip(
                label = "renderer-gl",
                active = !HolmExecutor.isWgpu(executor),
                onClick = {
                    executor = HolmExecutor.GL
                    errorText = ""
                    backendText = "waiting…"
                    perfText = "fps=—"
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
                    perfText = "fps=—"
                },
            )
            Spacer(modifier = Modifier.width(8.dp))
            ModelExecChip(
                label = if (lightsOpen) {
                    "灯光 ▴"
                } else {
                    val summary = if (litEnabled) {
                        "I=${(lightIntensity * 10f).roundToInt() / 10f}"
                    } else {
                        "Unlit"
                    }
                    "灯光 · $summary ▾"
                },
                active = lightsOpen,
                onClick = { lightsOpen = !lightsOpen },
            )
        }
        if (lightsOpen) {
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DemoTheme.Surface)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    ModelExecChip(
                        label = "Unlit",
                        active = !litEnabled,
                        onClick = {
                            litEnabled = false
                            pushLights()
                        },
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    ModelExecChip(
                        label = "Lit",
                        active = litEnabled,
                        onClick = {
                            litEnabled = true
                            pushLights()
                        },
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                DemoDpSlider(
                    label = "方位角",
                    value = lightAzimuth,
                    valueRange = 0f..360f,
                    snapToInt = true,
                    steps = 71,
                    formatValue = { "${it.roundToInt()}°" },
                    onValueChange = { next ->
                        lightAzimuth = next
                        if (litEnabled) pushLights()
                    },
                )
                DemoDpSlider(
                    label = "仰角",
                    value = lightElevation,
                    valueRange = -89f..89f,
                    snapToInt = true,
                    steps = 35,
                    formatValue = { "${it.roundToInt()}°" },
                    onValueChange = { next ->
                        lightElevation = next
                        if (litEnabled) pushLights()
                    },
                )
                DemoDpSlider(
                    label = "强度",
                    value = lightIntensity,
                    valueRange = 0f..2f,
                    snapToInt = false,
                    steps = 39,
                    formatValue = { v ->
                        val tenths = (v * 10f).roundToInt() / 10f
                        "$tenths"
                    },
                    onValueChange = { next ->
                        lightIntensity = next
                        if (litEnabled) pushLights()
                    },
                )
            }
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
                onPerf = { perf ->
                    val ms = ((perf.frameMs * 10f).toInt() / 10f)
                    perfText = "fps=${perf.fps.toInt()} (${ms}ms)"
                },
                onError = { err ->
                    errorText = "onError ${err.code}: ${err.message}"
                },
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Android / iOS 双轨 Present；Ohos/H5 → MODEL_UNSUPPORTED。${asset.note}。详见 assets/models/ATTRIBUTION.md",
            fontSize = 11.sp,
            color = DemoTheme.TextSecondary,
        )
    }
}

private fun buildDemoLights(
    lit: Boolean,
    azimuthDeg: Float,
    elevationDeg: Float,
    intensity: Float,
): List<HolmModelLight> {
    if (!lit) return emptyList()
    val az = azimuthDeg * (PI / 180.0)
    val el = elevationDeg * (PI / 180.0)
    val cy = cos(el).toFloat()
    return listOf(
        HolmModelLight(
            directionX = cy * sin(az).toFloat(),
            directionY = sin(el).toFloat(),
            directionZ = cy * cos(az).toFloat(),
            intensity = intensity,
        ),
    )
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
