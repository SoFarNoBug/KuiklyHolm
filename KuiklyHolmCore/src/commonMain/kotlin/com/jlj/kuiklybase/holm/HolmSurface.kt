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
 * @Description: HolmSurface Attr（scene / playing / liquidBoost / liquidBend / warpAmount / scrollSpeed / maxFps）。
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

    /** liquid_approx：边缘色散/高光强度（原 ior）。其它 scene 忽略。 */
    fun liquidBoost(value: Float): HolmSurfaceAttr {
        PROP_LIQUID_BOOST with value
        return this
    }

    /** liquid_approx：边缘 UV 拧曲幅度（原 warp）。其它 scene 忽略。 */
    fun liquidBend(value: Float): HolmSurfaceAttr {
        PROP_LIQUID_BEND with value
        return this
    }

    /** fullscreen_warp：全屏场扭曲幅度。其它 scene 忽略。 */
    fun warpAmount(value: Float): HolmSurfaceAttr {
        PROP_WARP_AMOUNT with value
        return this
    }

    /** gradient_bg：条带平移速率。其它 scene 忽略。 */
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

    companion object {
        const val PROP_SCENE: String = "scene"
        const val PROP_PLAYING: String = "playing"
        const val PROP_LIQUID_BOOST: String = "liquidBoost"
        const val PROP_LIQUID_BEND: String = "liquidBend"
        const val PROP_WARP_AMOUNT: String = "warpAmount"
        const val PROP_SCROLL_SPEED: String = "scrollSpeed"
        const val PROP_MAX_FPS: String = "maxFps"
        const val DEFAULT_LIQUID_BOOST: Float = 1.35f
        const val DEFAULT_LIQUID_BEND: Float = 0.05f
        const val DEFAULT_WARP_AMOUNT: Float = 0.04f
        const val DEFAULT_SCROLL_SPEED: Float = 1.0f
        const val DEFAULT_MAX_FPS: Int = 60
    }
}

/**
 * @CreateDate: 2026/11/12
 * @Author: jlj
 * @Description: HolmSurface Event（onReady / onError / onPerf）。
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

    companion object {
        const val ON_READY: String = "onReady"
        const val ON_ERROR: String = "onError"
        const val ON_PERF: String = "onPerf"
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
    liquidBoost: Float = HolmSurfaceAttr.DEFAULT_LIQUID_BOOST,
    liquidBend: Float = HolmSurfaceAttr.DEFAULT_LIQUID_BEND,
    warpAmount: Float = HolmSurfaceAttr.DEFAULT_WARP_AMOUNT,
    scrollSpeed: Float = HolmSurfaceAttr.DEFAULT_SCROLL_SPEED,
    maxFps: Int = HolmSurfaceAttr.DEFAULT_MAX_FPS,
    onReady: ((ReadyParams) -> Unit)? = null,
    onError: ((ErrorParams) -> Unit)? = null,
    onPerf: ((PerfParams) -> Unit)? = null,
) {
    MakeKuiklyComposeNode<HolmSurfaceView>(
        factory = { HolmSurfaceView() },
        modifier = modifier,
        viewUpdate = { view ->
            view.getViewAttr().run {
                scene(scene)
                playing(playing)
                liquidBoost(liquidBoost)
                liquidBend(liquidBend)
                warpAmount(warpAmount)
                scrollSpeed(scrollSpeed)
                maxFps(maxFps)
            }
            val readyHandler = onReady
            val errorHandler = onError
            val perfHandler = onPerf
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
            }
        },
    )
}
