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
 * @Description: HolmSurface 薄壳 Declarative 节点；viewName=KRHolmSurface。
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
 * @Description: HolmSurface Attr（ADR 0016：contentId + 岛级旋钮；废 fragmentShader）。
 */
class HolmSurfaceAttr : ComposeAttr() {
    fun playing(value: Boolean): HolmSurfaceAttr {
        PROP_PLAYING with if (value) 1 else 0
        return this
    }

    /**
     * 期望的 GL 提交上限（fps）。默认 60；`0`=不限制；`<0` 按 0。
     */
    fun maxFps(value: Int): HolmSurfaceAttr {
        PROP_MAX_FPS with value.coerceAtLeast(0)
        return this
    }

    /**
     * GPU 缓冲相对**当前平台基线**的缩放（与 [maxFps] 正交）。
     */
    fun renderScale(value: Float): HolmSurfaceAttr {
        PROP_RENDER_SCALE with normalizeRenderScale(value)
        return this
    }

    /**
     * 双渲染后端：`renderer-gl` 或 `renderer-wgpu`。
     */
    fun executor(value: String): HolmSurfaceAttr {
        PROP_EXECUTOR with HolmExecutor.normalize(value)
        return this
    }

    /**
     * [HolmContentRegistry] 登记 id；空串=无 Content（清屏不报错）。
     */
    fun contentId(value: String): HolmSurfaceAttr {
        PROP_CONTENT_ID with value
        return this
    }

    /**
     * NVI 桥（Ohos ETS / iOS ObjC 无 Registry 堆访问）；Compose 内部写，**非**公开业务 API。
     */
    fun bridgeContentKind(value: String): HolmSurfaceAttr {
        PROP_BRIDGE_CONTENT_KIND with value
        return this
    }

    fun bridgeFragmentShader(value: String): HolmSurfaceAttr {
        PROP_BRIDGE_FRAGMENT with value
        return this
    }

    fun bridgeVertexShader(value: String): HolmSurfaceAttr {
        PROP_BRIDGE_VERTEX with value
        return this
    }

    fun bridgeWarpAmount(value: Float): HolmSurfaceAttr {
        PROP_BRIDGE_WARP with value
        return this
    }

    fun bridgeScrollSpeed(value: Float): HolmSurfaceAttr {
        PROP_BRIDGE_SCROLL with value
        return this
    }

    companion object {
        const val PROP_PLAYING: String = "playing"
        const val PROP_MAX_FPS: String = "maxFps"
        const val PROP_RENDER_SCALE: String = "renderScale"
        const val PROP_EXECUTOR: String = "executor"
        const val PROP_CONTENT_ID: String = "contentId"
        /** shader | model | empty — 供无 Registry 的原生壳 */
        const val PROP_BRIDGE_CONTENT_KIND: String = "contentKind"
        const val PROP_BRIDGE_FRAGMENT: String = "fragmentShader"
        const val PROP_BRIDGE_VERTEX: String = "vertexShader"
        const val PROP_BRIDGE_WARP: String = "warpAmount"
        const val PROP_BRIDGE_SCROLL: String = "scrollSpeed"
        const val CONTENT_KIND_EMPTY: String = "empty"
        const val CONTENT_KIND_SHADER: String = "shader"
        const val CONTENT_KIND_MODEL: String = "model"
        const val DEFAULT_WARP_AMOUNT: Float = 0.04f
        const val DEFAULT_SCROLL_SPEED: Float = 1.0f
        const val DEFAULT_MAX_FPS: Int = 60
        const val DEFAULT_RENDER_SCALE: Float = 1.0f
        const val DEFAULT_EXECUTOR: String = HolmExecutor.GL

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
 * [content] 经 [HolmContentRegistry] 登记为 contentId；null/空 = 清屏。
 */
@Composable
fun HolmSurface(
    content: HolmContent? = null,
    modifier: Modifier = Modifier,
    playing: Boolean = true,
    maxFps: Int = HolmSurfaceAttr.DEFAULT_MAX_FPS,
    renderScale: Float = HolmSurfaceAttr.DEFAULT_RENDER_SCALE,
    executor: String = HolmSurfaceAttr.DEFAULT_EXECUTOR,
    onReady: ((ReadyParams) -> Unit)? = null,
    onError: ((ErrorParams) -> Unit)? = null,
    onPerf: ((PerfParams) -> Unit)? = null,
    onShaderCompiled: ((ShaderCompiledParams) -> Unit)? = null,
) {
    MakeKuiklyComposeNode<HolmSurfaceView>(
        factory = { HolmSurfaceView() },
        modifier = modifier,
        viewUpdate = { view ->
            val contentId = if (content == null) {
                ""
            } else {
                HolmContentRegistry.register(content)
            }
            view.getViewAttr().run {
                // 先挂 Content 再切 executor，避免热切首包错方言
                contentId(contentId)
                when (content) {
                    null -> {
                        bridgeContentKind(HolmSurfaceAttr.CONTENT_KIND_EMPTY)
                        bridgeFragmentShader("")
                        bridgeVertexShader("")
                    }
                    is HolmShaderContent -> {
                        bridgeContentKind(HolmSurfaceAttr.CONTENT_KIND_SHADER)
                        bridgeFragmentShader(content.source)
                        bridgeVertexShader(content.vertexSource)
                        bridgeWarpAmount(content.warpAmount)
                        bridgeScrollSpeed(content.scrollSpeed)
                    }
                    is HolmModelContent -> {
                        bridgeContentKind(HolmSurfaceAttr.CONTENT_KIND_MODEL)
                        bridgeFragmentShader("")
                        bridgeVertexShader("")
                    }
                }
                playing(playing)
                maxFps(maxFps)
                renderScale(renderScale)
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
