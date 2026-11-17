package com.jlj.kuiklybase.holm

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.extension.MakeKuiklyComposeNode
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.DeclarativeBaseView
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

/**
 * @CreateDate: 2026/11/12
 * @Author: jlj
 * @Description: HolmSurface 薄壳 Declarative 节点（Stage 0）；viewName=KRHolmSurface。
 */
class HolmSurfaceView : DeclarativeBaseView<HolmSurfaceAttr, HolmSurfaceEvent>() {

    override fun createAttr(): HolmSurfaceAttr = HolmSurfaceAttr()

    override fun createEvent(): HolmSurfaceEvent = HolmSurfaceEvent()

    override fun viewName(): String = VIEW_NAME

    companion object {
        const val VIEW_NAME: String = "KRHolmSurface"
    }
}

/**
 * @CreateDate: 2026/11/12
 * @Author: jlj
 * @Description: HolmSurface Attr（scene / playing / knobs / maxFps / renderScale / executor / fragmentShader / vertexShader）。
 * liquid 席已废（ADR 0015），不再暴露 liquidBoost/liquidBend。
 */
class HolmSurfaceAttr : ComposeAttr() {
    fun scene(value: String): HolmSurfaceAttr {
        PROP_SCENE with value
        return this
    }

    fun playing(value: Boolean): HolmSurfaceAttr {
        PROP_PLAYING with if (value) 1 else 0
        return this
    }

    /** 预留 uniform：全屏场扭曲幅度（历史 warp 席）。 */
    fun warpAmount(value: Float): HolmSurfaceAttr {
        PROP_WARP_AMOUNT with value
        return this
    }

    /** 预留 uniform：条带平移速率（历史 gradient 席）。 */
    fun scrollSpeed(value: Float): HolmSurfaceAttr {
        PROP_SCROLL_SPEED with value
        return this
    }

    /**
     * 期望的 GL 提交上限（fps）。默认 60；`0`=不限制；`<0` 按 0。
     *
     * 平台差异：H5 泵用 vsync + nextDue 跳过提交，上报 fps 大致贴档。
     * Android 当前仅 `Surface.setFrameRate` 提示，CONTINUOUSLY 下真实提交仍可能≈显示刷新；
     * `onPerf.fps` 计的是真实 draw 次数，不是「期望 maxFps」。详见 androidMain `applySurfaceFrameRate`。
     */
    fun maxFps(value: Int): HolmSurfaceAttr {
        PROP_MAX_FPS with value.coerceAtLeast(0)
        return this
    }

    /**
     * GPU 缓冲相对**当前平台基线**的缩放（与 [maxFps] 正交）。
     * 默认 `1` = 与改前相同像素数；`0.5` ≈ 半分辨率（糊一点换帧率）。
     * `≤0` / 非有限 → `1`；本波夹紧到 `(0, 1]`（禁止超采样）。
     *
     * 基线因端而异：Ohos/Android=物理 Surface px；iOS renderer-gl=点像素；iOS renderer-wgpu=`points×nativeScale`；H5=CSS px。
     */
    fun renderScale(value: Float): HolmSurfaceAttr {
        PROP_RENDER_SCALE with normalizeRenderScale(value)
        return this
    }

    /**
     * 双渲染后端：`renderer-gl`（默认，holm-renderer-gl）或 `renderer-wgpu`（holm-renderer-wgpu / kmp-wgpu Present）。
     * renderer-wgpu 缺 native 时平台侧 `onError(BLOCKED)`，不静默回落 GLES。
     */
    fun executor(value: String): HolmSurfaceAttr {
        PROP_EXECUTOR with HolmExecutor.normalize(value)
        return this
    }

    /**
     * 业务片元源码。renderer-gl：Shadertoy **正文**（库补壳）；renderer-wgpu：WGSL 片元段（含 `@fragment fn fs_main`）。
     * 空字符串：无官方席回退 → renderer-gl error/清屏；renderer-wgpu → ClearTri。
     */
    fun fragmentShader(value: String): HolmSurfaceAttr {
        PROP_FRAGMENT_SHADER with value
        return this
    }

    /**
     * 可选顶点源码；空=内置全屏 vert（renderer-gl）。renderer-wgpu 忽略（固定全屏 VS）。
     */
    fun vertexShader(value: String): HolmSurfaceAttr {
        PROP_VERTEX_SHADER with value
        return this
    }

    companion object {
        const val PROP_SCENE: String = "scene"
        const val PROP_PLAYING: String = "playing"
        const val PROP_WARP_AMOUNT: String = "warpAmount"
        const val PROP_SCROLL_SPEED: String = "scrollSpeed"
        const val PROP_MAX_FPS: String = "maxFps"
        const val PROP_RENDER_SCALE: String = "renderScale"
        const val PROP_EXECUTOR: String = "executor"
        const val PROP_FRAGMENT_SHADER: String = "fragmentShader"
        const val PROP_VERTEX_SHADER: String = "vertexShader"
        const val DEFAULT_WARP_AMOUNT: Float = 0.04f
        const val DEFAULT_SCROLL_SPEED: Float = 1.0f
        const val DEFAULT_MAX_FPS: Int = 60
        const val DEFAULT_RENDER_SCALE: Float = 1.0f
        const val DEFAULT_EXECUTOR: String = HolmExecutor.GL

        /** `≤0` / NaN/Inf → 1；否则夹紧到 `(0, 1]`。 */
        fun normalizeRenderScale(value: Float): Float {
            if (!value.isFinite() || value <= 0f) {
                return DEFAULT_RENDER_SCALE
            }
            return if (value > 1f) 1f else value
        }
    }
}

/**
 * @CreateDate: 2026/11/12
 * @Author: jlj
 * @Description: HolmSurface Event（onReady / onError / onPerf / onShaderCompiled）。
 */
class HolmSurfaceEvent : ComposeEvent() {
    fun onReady(handler: (ReadyParams) -> Unit) {
        register(ON_READY) {
            handler(ReadyParams.decode(it))
        }
    }

    fun onError(handler: (ErrorParams) -> Unit) {
        register(ON_ERROR) {
            handler(ErrorParams.decode(it))
        }
    }

    fun onPerf(handler: (PerfParams) -> Unit) {
        register(ON_PERF) {
            handler(PerfParams.decode(it))
        }
    }

    fun onShaderCompiled(handler: (ShaderCompiledParams) -> Unit) {
        register(ON_SHADER_COMPILED) {
            handler(ShaderCompiledParams.decode(it))
        }
    }

    companion object {
        const val ON_READY: String = "onReady"
        const val ON_ERROR: String = "onError"
        const val ON_PERF: String = "onPerf"
        const val ON_SHADER_COMPILED: String = "onShaderCompiled"
    }
}

data class ReadyParams(
    val backend: String,
    val quality: String,
) {
    companion object {
        fun decode(params: Any?): ReadyParams {
            val obj = params.toHolmJson()
            return ReadyParams(
                backend = obj.optString("backend", ""),
                quality = obj.optString("quality", "auto"),
            )
        }
    }
}

data class ErrorParams(
    val code: String,
    val message: String,
) {
    companion object {
        fun decode(params: Any?): ErrorParams {
            val obj = params.toHolmJson()
            return ErrorParams(
                code = obj.optString("code", ""),
                message = obj.optString("message", ""),
            )
        }
    }
}

/**
 * GL **真实提交** FPS（约 1Hz 窗口）；只在实际 clear/draw 后计数。
 * 与 Attr `maxFps`（期望上限）可能不一致——Android 上设 30 仍报 ~60 时，说明仍在每 vsync 提交。
 */
data class PerfParams(
    val fps: Float,
    val frameMs: Float,
) {
    companion object {
        fun decode(params: Any?): PerfParams {
            val obj = params.toHolmJson()
            return PerfParams(
                fps = obj.optDouble("fps", 0.0).toFloat(),
                frameMs = obj.optDouble("frameMs", 0.0).toFloat(),
            )
        }
    }
}

/**
 * 每次片元装载尝试结束（成败皆可）；ADR 0012。
 * renderer-wgpu 的 [wrapMs] 恒为 0；未跑到的步骤填 0。
 */
data class ShaderCompiledParams(
    val ok: Boolean,
    val wrapMs: Float,
    val compileMs: Float,
    val executor: String,
) {
    companion object {
        fun decode(params: Any?): ShaderCompiledParams {
            val obj = params.toHolmJson()
            val okRaw = obj.opt("ok")
            val ok = when (okRaw) {
                is Boolean -> okRaw
                is Number -> okRaw.toInt() != 0
                is String -> okRaw == "1" || okRaw.equals("true", ignoreCase = true)
                else -> obj.optBoolean("ok", false)
            }
            return ShaderCompiledParams(
                ok = ok,
                wrapMs = obj.optDouble("wrapMs", 0.0).toFloat(),
                compileMs = obj.optDouble("compileMs", 0.0).toFloat(),
                executor = obj.optString("executor", ""),
            )
        }
    }
}

/** H5 回调常为 Map；Android 多为 JSONObject；兼容 JSON 字符串。 */
private fun Any?.toHolmJson(): JSONObject = when (this) {
    is JSONObject -> this
    is Map<*, *> -> {
        val obj = JSONObject()
        for ((k, v) in this) {
            if (k != null) {
                obj.put(k.toString(), v)
            }
        }
        obj
    }
    is String -> runCatching { JSONObject(this) }.getOrElse { JSONObject() }
    else -> JSONObject()
}

fun ViewContainer<*, *>.HolmSurface(init: HolmSurfaceView.() -> Unit) {
    addChild(HolmSurfaceView(), init)
}

/**
 * Compose 入口（禁止 ComposeView 子类扩展）。
 */
@Composable
fun HolmSurface(
    scene: String,
    modifier: Modifier = Modifier,
    playing: Boolean = true,
    warpAmount: Float = HolmSurfaceAttr.DEFAULT_WARP_AMOUNT,
    scrollSpeed: Float = HolmSurfaceAttr.DEFAULT_SCROLL_SPEED,
    maxFps: Int = HolmSurfaceAttr.DEFAULT_MAX_FPS,
    renderScale: Float = HolmSurfaceAttr.DEFAULT_RENDER_SCALE,
    executor: String = HolmSurfaceAttr.DEFAULT_EXECUTOR,
    fragmentShader: String = "",
    vertexShader: String = "",
    onReady: ((ReadyParams) -> Unit)? = null,
    onError: ((ErrorParams) -> Unit)? = null,
    onPerf: ((PerfParams) -> Unit)? = null,
    onShaderCompiled: ((ShaderCompiledParams) -> Unit)? = null,
) {
    MakeKuiklyComposeNode<HolmSurfaceView>(
        factory = { HolmSurfaceView() },
        modifier = modifier,
        viewUpdate = { view ->
            view.getViewAttr().run {
                scene(scene)
                playing(playing)
                warpAmount(warpAmount)
                scrollSpeed(scrollSpeed)
                maxFps(maxFps)
                renderScale(renderScale)
                // 先写源码再切 executor，避免热切时首包带错方言（审查 P1-concurrency）
                fragmentShader(fragmentShader)
                vertexShader(vertexShader)
                executor(executor)
            }
            val readyHandler = onReady
            val errorHandler = onError
            val perfHandler = onPerf
            val compiledHandler = onShaderCompiled
            view.getViewEvent().run {
                if (readyHandler != null) {
                    onReady(readyHandler)
                }
                if (errorHandler != null) {
                    onError(errorHandler)
                }
                if (perfHandler != null) {
                    onPerf(perfHandler)
                }
                if (compiledHandler != null) {
                    onShaderCompiled(compiledHandler)
                }
            }
        },
    )
}
