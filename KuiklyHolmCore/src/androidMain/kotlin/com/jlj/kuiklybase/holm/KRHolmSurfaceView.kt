package com.jlj.kuiklybase.holm

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.view.Choreographer
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import com.tencent.kuikly.core.render.android.expand.component.KRView
import com.tencent.kuikly.core.render.android.export.KuiklyRenderCallback
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import org.json.JSONObject

/**
 * @CreateDate: 2026/11/12
 * @Author: jlj
 * @Description: Android KRHolmSurface — Lite=GLSurfaceView GLES；Runtime=SurfaceView + 渲染线程外泵 JNI tick。
 */
class KRHolmSurfaceView(context: Context) : KRView(context) {

    private val _glView = GLSurfaceView(context)
    private val _renderer = HolmGlesRenderer()
    private val _runtimeView = SurfaceView(context)
    private var _playing: Boolean = true
    private var _scene: String = ""
    private var _liquidBoost: Float = HolmSurfaceAttr.DEFAULT_LIQUID_BOOST
    private var _liquidBend: Float = HolmSurfaceAttr.DEFAULT_LIQUID_BEND
    private var _warpAmount: Float = HolmSurfaceAttr.DEFAULT_WARP_AMOUNT
    private var _scrollSpeed: Float = HolmSurfaceAttr.DEFAULT_SCROLL_SPEED
    private var _maxFps: Int = HolmSurfaceAttr.DEFAULT_MAX_FPS
    private var _renderScale: Float = HolmSurfaceAttr.DEFAULT_RENDER_SCALE
    private var _layoutW: Int = 0
    private var _layoutH: Int = 0
    private var _executor: String = HolmSurfaceAttr.DEFAULT_EXECUTOR
    private var _fragmentShader: String = ""
    private var _vertexShader: String = ""
    private var _runtimeBlockedNotified: Boolean = false
    private var _pendingErrorCode: String? = null
    private var _pendingErrorMessage: String? = null
    private var _onReady: KuiklyRenderCallback? = null
    private var _onError: KuiklyRenderCallback? = null
    private var _onPerf: KuiklyRenderCallback? = null
    private var _onShaderCompiled: KuiklyRenderCallback? = null
    private var _readyFired: Boolean = false
    private var _surfaceAlive: Boolean = false
    private var _appliedSurfaceFps: Float = -1f

    private var _fullscreenPass: HolmFullscreenPass? = null
    /** UI 与渲染线程共享；Surface 有效性以本旗标为准。 */
    private val _runtimeSurfaceValid = AtomicBoolean(false)
    private var _runtimeWidth: Int = 1
    private var _runtimeHeight: Int = 1
    private var _runtimeLastFrameNs: Long = 0L
    private var _runtimePerfWindowNs: Long = 0L
    private var _runtimePerfFrames: Int = 0
    private var _runtimePumpPosted: Boolean = false
    private var _runtimeBackendId: String = HolmExecutor.BACKEND_WGPU_M0
    /** Provider 未注册 / 未 setDefault：onReady 须诚实 BLOCKED，禁止回报假 gles2。 */
    private var _providerBlocked: Boolean = false
    private val _runtimeEagerDestroying = AtomicBoolean(false)

    private var _renderThread = HandlerThread("holm-runtime").also { it.start() }
    private var _renderHandler = Handler(_renderThread.looper)
    private var _choreographer: Choreographer? = null

    private val _glSurfaceCallback = object : SurfaceHolder.Callback {
        override fun surfaceCreated(holder: SurfaceHolder) {
            _surfaceAlive = true
            _appliedSurfaceFps = -1f
            applySurfaceFrameRate()
        }

        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            applySurfaceFrameRate()
        }

        override fun surfaceDestroyed(holder: SurfaceHolder) {
            _surfaceAlive = false
            _appliedSurfaceFps = -1f
        }
    }

    private val _runtimeSurfaceCallback = object : SurfaceHolder.Callback {
        override fun surfaceCreated(holder: SurfaceHolder) {
            if (!HolmExecutor.isRuntime(_executor)) return
            val surface = holder.surface ?: return
            if (!surface.isValid) return
            _runtimeSurfaceValid.set(true)
            val frame = holder.surfaceFrame
            _runtimeWidth = frame.width().coerceAtLeast(1)
            _runtimeHeight = frame.height().coerceAtLeast(1)
            applyRuntimeSurfaceFrameRate(surface)
            postCreateRuntimeSession(surface, _runtimeWidth, _runtimeHeight)
        }

        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            if (!HolmExecutor.isRuntime(_executor)) return
            val w = width.coerceAtLeast(1)
            val h = height.coerceAtLeast(1)
            _runtimeWidth = w
            _runtimeHeight = h
            val surface = holder.surface
            if (surface != null && surface.isValid) {
                applyRuntimeSurfaceFrameRate(surface)
            }
            ensureRenderThread()
            _renderHandler.post {
                val pass = _fullscreenPass
                if (pass != null && _runtimeSurfaceValid.get()) {
                    pass.resize(w, h)
                }
            }
        }

        override fun surfaceDestroyed(holder: SurfaceHolder) {
            _runtimeSurfaceValid.set(false)
            // 急拆：在返回前停用 Surface，避免 native 继续持有
            eagerDestroyRuntimeSession(waitMs = 800L)
        }
    }

    private val _frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            _runtimePumpPosted = false
            val pass = _fullscreenPass
            if (!HolmExecutor.isRuntime(_executor) || !_playing || pass == null) {
                return
            }
            if (_runtimeLastFrameNs == 0L) {
                _runtimeLastFrameNs = frameTimeNanos
            }
            val dtSecs = ((frameTimeNanos - _runtimeLastFrameNs) / 1_000_000_000.0)
                .toFloat()
                .coerceIn(0f, MAX_DT_SEC)
            _runtimeLastFrameNs = frameTimeNanos
            if (!_runtimeSurfaceValid.get()) {
                destroyRuntimeSessionOnRenderThread()
                return
            }
            val ok = pass.tick(dtSecs)
            if (!ok) {
                // 只走 drain（Rust 已 emit）；空则回落 lastError，避免双发
                val drained = flushRuntimeErrors(pass)
                if (drained == 0) {
                    val code = pass.lastErrorCode().ifBlank { "TICK" }
                    val detail = pass.lastErrorMessage()
                        .ifBlank { "DualTrack B BLOCKED: holm_runtime tick failed. No silent GLES stand-in." }
                    post {
                        if (!_runtimeBlockedNotified) {
                            _runtimeBlockedNotified = true
                            fireError(code, detail)
                        }
                    }
                } else if (!_runtimeBlockedNotified) {
                    _runtimeBlockedNotified = true
                }
                HolmRuntimeNative.markPresentable(false)
                destroyRuntimeSessionOnRenderThread()
                return
            }
            flushRuntimeErrors(pass)
            if (!HolmRuntimeNative.isM0Presentable()) {
                HolmRuntimeNative.markPresentable(true)
                _runtimeBackendId = pass.backendId()
                post {
                    if (!_readyFired) {
                        _readyFired = true
                        fireReady()
                    }
                }
            }
            tickRuntimePerf(frameTimeNanos)
            scheduleRuntimePump()
        }
    }

    init {
        HolmUmbrellaBootstrap.ensure()
        _glView.setEGLContextClientVersion(2)
        _glView.setRenderer(_renderer)
        _glView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        _glView.holder.addCallback(_glSurfaceCallback)
        addView(
            _glView,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT),
        )

        _runtimeView.holder.addCallback(_runtimeSurfaceCallback)
        _runtimeView.visibility = View.GONE
        addView(
            _runtimeView,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT),
        )

        _renderer.onReady = {
            post {
                if (!_readyFired && !HolmExecutor.isRuntime(_executor)) {
                    _readyFired = true
                    fireReady()
                }
            }
        }
        _renderer.onError = { code, message ->
            post { fireError(code, message) }
        }
        _renderer.onPerf = { fps, frameMs ->
            post { firePerf(fps, frameMs) }
        }
        _renderer.onShaderCompiled = { ok, wrapMs, compileMs ->
            post { fireShaderCompiled(ok, wrapMs, compileMs, HolmExecutor.LITE) }
        }

        ensureRenderThread()
    }

    override fun setProp(propKey: String, propValue: Any): Boolean {
        return when (propKey) {
            HolmSurfaceAttr.PROP_SCENE -> {
                val next = propValue.toString()
                if (next != _scene) {
                    _scene = next
                    if (HolmExecutor.isRuntime(_executor)) {
                        postRuntime { applyRuntimeShaders(it) }
                    } else {
                        _glView.queueEvent {
                            _renderer.setScene(next, _fragmentShader, _vertexShader)
                        }
                    }
                }
                true
            }
            HolmSurfaceAttr.PROP_FRAGMENT_SHADER -> {
                val next = propValue.toString()
                if (next != _fragmentShader) {
                    _fragmentShader = next
                    // 双轨热切时 Compose 先写源码再写 executor：只把「当前轨方言」交给活跃后端，
                    // 错方言只入库，勿 SHADER_* / 蓝屏；进轨时 applyExecutorMode 会重装。
                    if (HolmExecutor.isRuntime(_executor)) {
                        if (HolmShaderResolve.looksLikeWgslFragment(next)) {
                            postRuntime { applyRuntimeShaders(it) }
                        } else {
                            post { fireShaderCompiled(false, 0f, 0f, HolmExecutor.RUNTIME) }
                        }
                    } else if (!HolmShaderResolve.looksLikeWgslFragment(next)) {
                        _glView.queueEvent {
                            _renderer.setShaders(_scene, _fragmentShader, _vertexShader)
                        }
                    } else {
                        post { fireShaderCompiled(false, 0f, 0f, HolmExecutor.LITE) }
                    }
                }
                true
            }
            HolmSurfaceAttr.PROP_VERTEX_SHADER -> {
                val next = propValue.toString()
                if (next != _vertexShader) {
                    _vertexShader = next
                    if (!HolmExecutor.isRuntime(_executor)) {
                        _glView.queueEvent {
                            _renderer.setShaders(_scene, _fragmentShader, _vertexShader)
                        }
                    }
                }
                true
            }
            HolmSurfaceAttr.PROP_PLAYING -> {
                _playing = propValue.toBool()
                if (HolmExecutor.isRuntime(_executor)) {
                    postRuntime { it.setPlaying(_playing) }
                    if (_playing) {
                        ensureRenderThread()
                        _renderHandler.post { scheduleRuntimePump() }
                    }
                } else {
                    applyPlayingRenderMode()
                }
                true
            }
            HolmSurfaceAttr.PROP_LIQUID_BOOST -> {
                val next = propValue.toHolmFloat(HolmSurfaceAttr.DEFAULT_LIQUID_BOOST)
                if (next != _liquidBoost) {
                    _liquidBoost = next
                    if (HolmExecutor.isRuntime(_executor)) {
                        postRuntime {
                            it.setLiquid(_liquidBoost, _liquidBend)
                        }
                    } else {
                        _glView.queueEvent { _renderer.setLiquidBoost(next) }
                    }
                }
                true
            }
            HolmSurfaceAttr.PROP_LIQUID_BEND -> {
                val next = propValue.toHolmFloat(HolmSurfaceAttr.DEFAULT_LIQUID_BEND)
                if (next != _liquidBend) {
                    _liquidBend = next
                    if (HolmExecutor.isRuntime(_executor)) {
                        postRuntime {
                            it.setLiquid(_liquidBoost, _liquidBend)
                        }
                    } else {
                        _glView.queueEvent { _renderer.setLiquidBend(next) }
                    }
                }
                true
            }
            HolmSurfaceAttr.PROP_WARP_AMOUNT -> {
                val next = propValue.toHolmFloat(HolmSurfaceAttr.DEFAULT_WARP_AMOUNT)
                if (next != _warpAmount) {
                    _warpAmount = next
                    if (!HolmExecutor.isRuntime(_executor)) {
                        _glView.queueEvent { _renderer.setWarpAmount(next) }
                    }
                }
                true
            }
            HolmSurfaceAttr.PROP_SCROLL_SPEED -> {
                val next = propValue.toHolmFloat(HolmSurfaceAttr.DEFAULT_SCROLL_SPEED)
                if (next != _scrollSpeed) {
                    _scrollSpeed = next
                    if (!HolmExecutor.isRuntime(_executor)) {
                        _glView.queueEvent { _renderer.setScrollSpeed(next) }
                    }
                }
                true
            }
            HolmSurfaceAttr.PROP_MAX_FPS -> {
                val next = propValue.toHolmInt(HolmSurfaceAttr.DEFAULT_MAX_FPS).coerceAtLeast(0)
                if (next != _maxFps) {
                    _maxFps = next
                    applySurfaceFrameRate()
                    if (HolmExecutor.isRuntime(_executor) && _runtimeSurfaceValid.get()) {
                        val surface = _runtimeView.holder.surface
                        if (surface != null && surface.isValid) {
                            applyRuntimeSurfaceFrameRate(surface)
                        }
                    }
                }
                true
            }
            HolmSurfaceAttr.PROP_RENDER_SCALE -> {
                val next = HolmSurfaceAttr.normalizeRenderScale(
                    propValue.toHolmFloat(HolmSurfaceAttr.DEFAULT_RENDER_SCALE),
                )
                if (next != _renderScale) {
                    _renderScale = next
                    applyBufferGeometry()
                }
                true
            }
            HolmSurfaceAttr.PROP_EXECUTOR -> {
                val next = HolmExecutor.normalize(propValue.toString())
                if (next != _executor) {
                    _executor = next
                    _readyFired = false
                    _runtimeBlockedNotified = false
                    applyExecutorMode()
                }
                true
            }
            HolmSurfaceEvent.ON_READY -> {
                _onReady = propValue as? KuiklyRenderCallback
                if (_readyFired) {
                    fireReady()
                }
                true
            }
            HolmSurfaceEvent.ON_ERROR -> {
                _onError = propValue as? KuiklyRenderCallback
                // 仅重放创建期 BLOCKED；SHADER_* 全量 replay 会在成功换档后 sticky 红字
                if (_pendingErrorCode == HolmExecutor.ERROR_BLOCKED) {
                    replayPendingError()
                }
                true
            }
            HolmSurfaceEvent.ON_PERF -> {
                _onPerf = propValue as? KuiklyRenderCallback
                true
            }
            HolmSurfaceEvent.ON_SHADER_COMPILED -> {
                _onShaderCompiled = propValue as? KuiklyRenderCallback
                true
            }
            else -> super.setProp(propKey, propValue)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w != _layoutW || h != _layoutH) {
            _layoutW = w
            _layoutH = h
            applyBufferGeometry()
        }
    }

    /** layout px × renderScale → Surface 缓冲；1.0 恢复跟随 layout。 */
    private fun applyBufferGeometry() {
        if (_layoutW <= 0 || _layoutH <= 0) {
            return
        }
        val scale = HolmSurfaceAttr.normalizeRenderScale(_renderScale)
        if (scale >= 0.999f) {
            _glView.holder.setSizeFromLayout()
            _runtimeView.holder.setSizeFromLayout()
            return
        }
        val bufW = kotlin.math.floor(_layoutW * scale).toInt().coerceAtLeast(1)
        val bufH = kotlin.math.floor(_layoutH * scale).toInt().coerceAtLeast(1)
        _glView.holder.setFixedSize(bufW, bufH)
        _runtimeView.holder.setFixedSize(bufW, bufH)
    }

    override fun onDetachedFromWindow() {
        if (HolmExecutor.isRuntime(_executor)) {
            eagerDestroyRuntimeSession(waitMs = 800L)
        } else {
            _glView.queueEvent {
                _renderer.release()
            }
            _glView.onPause()
        }
        _choreographer = null
        _renderThread.quitSafely()
        super.onDetachedFromWindow()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        ensureRenderThread()
        if (HolmExecutor.isRuntime(_executor)) {
            applyExecutorMode()
        } else {
            _glView.onResume()
            applySurfaceFrameRate()
        }
    }

    /**
     * API 31+：把 maxFps 写成 Surface 帧率**提示**（0→峰值档 120）；同值不重复申请。
     *
     * 已知限制（本单不修）：
     * - `setFrameRate` 对系统只是建议，`RENDERMODE_CONTINUOUSLY` 仍常按显示 vsync（如 60）回调
     *   `onDrawFrame`，故 Demo 设 30 时 `onPerf` 仍可能报 ~60——那是**真实 GL 提交率**，不是统计错误。
     * - 在 CONTINUOUSLY 里用累加器跳过 clear/draw 会 swap 未画缓冲 → 闪烁，已去掉。
     * - 若将来要「设 30 则真实提交也到 ~30」，需改 WHEN_DIRTY + Choreographer/`requestRender` 自建时钟。
     */
    private fun applySurfaceFrameRate() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        if (!_surfaceAlive) return
        val surface = _glView.holder.surface ?: return
        if (!surface.isValid) return
        val fps = if (_maxFps <= 0) UNLIMITED_SURFACE_FPS else _maxFps.toFloat()
        if (fps == _appliedSurfaceFps) return
        runCatching {
            surface.setFrameRate(fps, Surface.FRAME_RATE_COMPATIBILITY_DEFAULT)
            _appliedSurfaceFps = fps
        }
    }

    private fun applyRuntimeSurfaceFrameRate(surface: Surface) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        if (!surface.isValid) return
        val fps = if (_maxFps <= 0) UNLIMITED_SURFACE_FPS else _maxFps.toFloat()
        runCatching {
            surface.setFrameRate(fps, Surface.FRAME_RATE_COMPATIBILITY_DEFAULT)
        }
    }

    private fun applyPlayingRenderMode() {
        if (HolmExecutor.isRuntime(_executor)) {
            _glView.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
            return
        }
        _glView.renderMode = if (_playing) {
            GLSurfaceView.RENDERMODE_CONTINUOUSLY
        } else {
            GLSurfaceView.RENDERMODE_WHEN_DIRTY
        }
    }

    private fun applyExecutorMode() {
        HolmUmbrellaBootstrap.ensure()
        try {
            HolmProviders.requireForExecutor(_executor)
            _providerBlocked = false
        } catch (e: IllegalStateException) {
            _providerBlocked = true
            setBackgroundColor(0xFF1E2A4A.toInt())
            if (!_runtimeBlockedNotified) {
                _runtimeBlockedNotified = true
                fireError(HolmExecutor.ERROR_BLOCKED, e.message ?: HolmProviders.missingDefaultMessage())
            }
            if (!_readyFired) {
                _readyFired = true
                fireReady()
            }
            return
        }
        if (HolmExecutor.isRuntime(_executor)) {
            // 切 Runtime：停 Lite GLES，挂 SurfaceView；create 在 surfaceCreated
            // onPause 会丢 EGL；先失效 program，避免切回 Lite 时 same-check 误跳过重编 → 蓝屏
            _renderer.markContextLost()
            _glView.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
            _glView.onPause()
            _glView.visibility = View.GONE
            if (!HolmRuntimeNative.isLibraryLoaded()) {
                HolmRuntimeNative.markPresentable(false)
                _runtimeView.visibility = View.GONE
                setBackgroundColor(0xFF1E2A4A.toInt())
                if (!_runtimeBlockedNotified) {
                    _runtimeBlockedNotified = true
                    fireError(HolmExecutor.ERROR_BLOCKED, HolmRuntimeNative.blockedMessage())
                }
                if (!_readyFired) {
                    _readyFired = true
                    fireReady()
                }
                return
            }
            background = null
            clearPendingError()
            ensureRenderThread()
            _runtimeView.visibility = View.VISIBLE
            // Surface 已在则立即 create；否则等 callback
            val holder = _runtimeView.holder
            val surface = holder.surface
            if (surface != null && surface.isValid && _runtimeSurfaceValid.get()) {
                val frame = holder.surfaceFrame
                postCreateRuntimeSession(
                    surface,
                    frame.width().coerceAtLeast(1),
                    frame.height().coerceAtLeast(1),
                )
            }
        } else {
            // 切 Lite：急拆 Session，还原 GLSurfaceView，并强制用当前片元重编（Runtime 期未喂 GLES）
            eagerDestroyRuntimeSession(waitMs = 800L)
            HolmRuntimeNative.markPresentable(false)
            _runtimeView.visibility = View.GONE
            background = null
            // 丢弃可能过期的 program / surfaceReady（pause 后 same-check 会误判已就绪）
            _renderer.markContextLost()
            _glView.visibility = View.VISIBLE
            clearPendingError()
            _glView.onResume()
            applyPlayingRenderMode()
            val scene = _scene
            val frag = _fragmentShader
            val vert = _vertexShader
            _glView.queueEvent {
                _renderer.setShaders(scene, frag, vert)
            }
            // Surface 可能已在：不依赖 onSurfaceCreated 再发 onReady（防 backend 粘 wgpu）
            if (!_readyFired) {
                _readyFired = true
                fireReady()
            }
        }
    }

    private fun postCreateRuntimeSession(surface: Surface, width: Int, height: Int) {
        ensureRenderThread()
        _renderHandler.post {
            if (!HolmExecutor.isRuntime(_executor) || !_runtimeSurfaceValid.get()) return@post
            if (_fullscreenPass != null) {
                destroyRuntimeSessionOnRenderThread()
            }
            val pass = HolmRuntimeNative.createFullscreenPass(surface, width, height)
            if (pass == null) {
                HolmRuntimeNative.markPresentable(false)
                post {
                    setBackgroundColor(0xFF1E2A4A.toInt())
                    if (!_runtimeBlockedNotified) {
                        _runtimeBlockedNotified = true
                        fireError(HolmExecutor.ERROR_BLOCKED, HolmRuntimeNative.blockedMessage())
                    }
                    if (!_readyFired) {
                        _readyFired = true
                        fireReady()
                    }
                }
                return@post
            }
            _fullscreenPass = pass
            applyRuntimeShaders(pass)
            pass.setLiquid(_liquidBoost, _liquidBend)
            pass.setPlaying(_playing)
            _runtimeLastFrameNs = 0L
            _runtimePerfWindowNs = 0L
            _runtimePerfFrames = 0
            // 首 tick 成功才 markPresentable（在 doFrame）
            scheduleRuntimePump()
        }
    }

    private fun postRuntime(block: (HolmFullscreenPass) -> Unit) {
        ensureRenderThread()
        _renderHandler.post {
            val pass = _fullscreenPass
            if (pass != null) {
                block(pass)
            }
        }
    }

    /** 对齐 H5：先卸上一档 custom，再装新；失败则 ClearTri，不残留旧特效。 */
    private fun applyRuntimeShaders(pass: HolmFullscreenPass) {
        val custom = _fragmentShader.trim()
        // 切换样本/轨：先停掉上一档
        pass.setCustomFs("")
        if (custom.isNotEmpty()) {
            if (!HolmShaderResolve.looksLikeWgslFragment(custom)) {
                pass.setScene(_scene)
                post {
                    fireError(
                        "SHADER_CUSTOM",
                        "Runtime fragmentShader must be WGSL (non-WGSL custom ignored)",
                    )
                    fireShaderCompiled(false, 0f, 0f, HolmExecutor.RUNTIME)
                }
                return
            }
            val t0 = System.nanoTime()
            val ok = pass.setCustomFs(custom)
            val compileMs = (System.nanoTime() - t0) / 1_000_000f
            // 失败已 emit 进 pending：只 drain 一次，勿再读 lastError 双发
            val drained = flushRuntimeErrors(pass)
            if (!ok) {
                // native 已清屏停泵；宿主对齐，避免 Choreographer 继续空转 ClearTri
                pass.setPlaying(false)
                _playing = false
                if (drained == 0) {
                    post {
                        fireError("SHADER_CUSTOM", "Runtime setCustomFs failed")
                        fireShaderCompiled(false, 0f, compileMs, HolmExecutor.RUNTIME)
                    }
                } else {
                    post { fireShaderCompiled(false, 0f, compileMs, HolmExecutor.RUNTIME) }
                }
            } else {
                post { fireShaderCompiled(true, 0f, compileMs, HolmExecutor.RUNTIME) }
            }
        } else {
            pass.setScene(_scene)
            // 空片元 ClearTri：ok=false，对齐 Lite 拒绝语义（审查 P2）
            post { fireShaderCompiled(false, 0f, 0f, HolmExecutor.RUNTIME) }
        }
    }

    /** @return 已投递的错误条数 */
    private fun flushRuntimeErrors(pass: HolmFullscreenPass): Int {
        val errs = pass.drainErrors()
        if (errs.isEmpty()) return 0
        post {
            for ((code, message) in errs) {
                fireError(code.ifBlank { "WGPU_VALIDATION" }, message)
            }
        }
        return errs.size
    }

    private fun scheduleRuntimePump() {
        if (_runtimePumpPosted) return
        if (!HolmExecutor.isRuntime(_executor) || !_playing || _fullscreenPass == null) return
        if (!_runtimeSurfaceValid.get()) return
        ensureRenderThread()
        val choreographer = _choreographer ?: Choreographer.getInstance().also { _choreographer = it }
        _runtimePumpPosted = true
        choreographer.postFrameCallback(_frameCallback)
    }

    /** 卸岛 quit 后再次挂载时重建渲染线程。 */
    private fun ensureRenderThread() {
        if (_renderThread.isAlive) return
        _renderThread = HandlerThread("holm-runtime").also { it.start() }
        _renderHandler = Handler(_renderThread.looper)
        _choreographer = null
        _renderHandler.post {
            _choreographer = Choreographer.getInstance()
        }
    }

    private fun stopRuntimePump() {
        val choreographer = _choreographer
        if (choreographer != null && _runtimePumpPosted) {
            choreographer.removeFrameCallback(_frameCallback)
        }
        _runtimePumpPosted = false
        _runtimeLastFrameNs = 0L
    }

    private fun destroyRuntimeSessionOnRenderThread() {
        stopRuntimePump()
        val pass = _fullscreenPass
        _fullscreenPass = null
        pass?.destroy()
    }

    private fun eagerDestroyRuntimeSession(waitMs: Long) {
        if (!_runtimeEagerDestroying.compareAndSet(false, true)) {
            return
        }
        try {
            _runtimeSurfaceValid.set(false)
            if (!_renderThread.isAlive) {
                _fullscreenPass = null
                HolmRuntimeNative.markPresentable(false)
                return
            }
            val latch = CountDownLatch(1)
            _renderHandler.post {
                try {
                    destroyRuntimeSessionOnRenderThread()
                    HolmRuntimeNative.markPresentable(false)
                } finally {
                    latch.countDown()
                }
            }
            val finished = latch.await(waitMs, TimeUnit.MILLISECONDS)
            if (!finished) {
                // 超时：挡住新 tick/create，再投一次幂等销毁（禁继续泵死 Surface）
                _runtimeSurfaceValid.set(false)
                _renderHandler.post {
                    destroyRuntimeSessionOnRenderThread()
                    HolmRuntimeNative.markPresentable(false)
                }
            }
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        } finally {
            _runtimeEagerDestroying.set(false)
        }
    }

    private fun tickRuntimePerf(nowNs: Long) {
        if (_onPerf == null) return
        if (_runtimePerfWindowNs == 0L) {
            _runtimePerfWindowNs = nowNs
            _runtimePerfFrames = 0
            return
        }
        _runtimePerfFrames += 1
        val elapsed = nowNs - _runtimePerfWindowNs
        if (elapsed >= 1_000_000_000L) {
            val fps = _runtimePerfFrames * 1_000_000_000f / elapsed.toFloat()
            val frameMs = if (fps > 0.01f) 1000f / fps else 0f
            post { firePerf(fps, frameMs) }
            _runtimePerfWindowNs = nowNs
            _runtimePerfFrames = 0
        }
    }

    private fun fireReady() {
        val backend = when {
            _providerBlocked -> HolmExecutor.BACKEND_WGPU_BLOCKED
            HolmExecutor.isRuntime(_executor) && HolmRuntimeNative.isAvailable() ->
                _runtimeBackendId.ifEmpty { HolmExecutor.BACKEND_WGPU_M0 }
            HolmExecutor.isRuntime(_executor) -> HolmExecutor.BACKEND_WGPU_BLOCKED
            else -> HolmExecutor.BACKEND_GLES2
        }
        val payload = JSONObject()
            .put("backend", backend)
            .put("quality", "auto")
            .put("scene", _scene)
            .put("executor", _executor)
        _onReady?.invoke(payload)
    }

    private fun fireError(code: String, message: String) {
        _pendingErrorCode = code
        _pendingErrorMessage = message
        val callback = _onError ?: return
        val payload = JSONObject()
            .put("code", code)
            .put("message", message)
        callback.invoke(payload)
    }

    private fun replayPendingError() {
        val code = _pendingErrorCode ?: return
        val message = _pendingErrorMessage ?: return
        val callback = _onError ?: return
        val payload = JSONObject()
            .put("code", code)
            .put("message", message)
        callback.invoke(payload)
    }

    private fun clearPendingError() {
        _pendingErrorCode = null
        _pendingErrorMessage = null
        _runtimeBlockedNotified = false
    }

    private fun firePerf(fps: Float, frameMs: Float) {
        val payload = JSONObject()
            .put("fps", fps.toDouble())
            .put("frameMs", frameMs.toDouble())
        _onPerf?.invoke(payload)
    }

    private fun fireShaderCompiled(ok: Boolean, wrapMs: Float, compileMs: Float, executor: String) {
        val payload = JSONObject()
            .put("ok", ok)
            .put("wrapMs", wrapMs.toDouble())
            .put("compileMs", compileMs.toDouble())
            .put("executor", executor)
        _onShaderCompiled?.invoke(payload)
    }

    companion object {
        const val VIEW_NAME: String = HolmSurfaceView.VIEW_NAME
        private const val UNLIMITED_SURFACE_FPS: Float = 120f
        private const val MAX_DT_SEC: Float = 0.05f
    }
}

private class HolmGlesRenderer : GLSurfaceView.Renderer {
    var onReady: (() -> Unit)? = null
    var onError: ((String, String) -> Unit)? = null
    var onPerf: ((Float, Float) -> Unit)? = null
    /** ok, wrapMs, compileMs — 调用方 post 到 UI 并填 executor=lite */
    var onShaderCompiled: ((Boolean, Float, Float) -> Unit)? = null

    private var _program: Int = 0
    private var _timeLoc: Int = -1
    private var _resolutionLoc: Int = -1
    private var _liquidBoostLoc: Int = -1
    private var _liquidBendLoc: Int = -1
    private var _warpAmountLoc: Int = -1
    private var _scrollSpeedLoc: Int = -1
    private var _posLoc: Int = -1
    private var _animTimeSec: Float = 0f
    private var _lastFrameNs: Long = 0L
    private var _quad: FloatBuffer? = null
    private var _ready: Boolean = false
    private var _scene: String = ""
    private var _fragmentBody: String = ""
    private var _vertexSrc: String = ""
    private var _liquidBoost: Float = HolmSurfaceAttr.DEFAULT_LIQUID_BOOST
    private var _liquidBend: Float = HolmSurfaceAttr.DEFAULT_LIQUID_BEND
    private var _warpAmount: Float = HolmSurfaceAttr.DEFAULT_WARP_AMOUNT
    private var _scrollSpeed: Float = HolmSurfaceAttr.DEFAULT_SCROLL_SPEED
    private var _surfaceReady: Boolean = false
    private var _viewW: Int = 1
    private var _viewH: Int = 1
    private var _shaderFailed: Boolean = false
    private var _fpsWindowNs: Long = 0L
    private var _frameCount: Int = 0

    fun setScene(scene: String, fragmentBody: String = _fragmentBody, vertexSrc: String = _vertexSrc) {
        setShaders(scene, fragmentBody, vertexSrc)
    }

    fun setShaders(scene: String, fragmentBody: String, vertexSrc: String) {
        val same =
            scene == _scene &&
                fragmentBody == _fragmentBody &&
                vertexSrc == _vertexSrc &&
                _program != 0 &&
                !_shaderFailed
        if (same) return
        _scene = scene
        _fragmentBody = fragmentBody
        _vertexSrc = vertexSrc
        if (_surfaceReady) {
            rebuildProgram()
        }
    }

    /**
     * EGL / GLSurfaceView 上下文已丢或即将丢：作废 program 句柄，保留源码。
     * 可在 UI 线程调用（不碰 GLES）；下次 setShaders / onSurfaceCreated 必重编。
     */
    fun markContextLost() {
        _program = 0
        _surfaceReady = false
        _shaderFailed = false
        _ready = false
    }

    fun setLiquidBoost(value: Float) {
        _liquidBoost = value
    }

    fun setLiquidBend(value: Float) {
        _liquidBend = value
    }

    fun setWarpAmount(value: Float) {
        _warpAmount = value
    }

    fun setScrollSpeed(value: Float) {
        _scrollSpeed = value
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        _animTimeSec = 0f
        _lastFrameNs = 0L
        _surfaceReady = true
        ensureQuad()
        rebuildProgram()
        if (_program != 0 && !_ready) {
            _ready = true
            onReady?.invoke()
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        _viewW = width.coerceAtLeast(1)
        _viewH = height.coerceAtLeast(1)
        GLES20.glViewport(0, 0, _viewW, _viewH)
    }

    override fun onDrawFrame(gl: GL10?) {
        if (_program == 0) {
            GLES20.glClearColor(0.02f, 0.05f, 0.28f, 1f)
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            return
        }
        val nowNs = System.nanoTime()
        if (_lastFrameNs == 0L) {
            _lastFrameNs = nowNs
        } else {
            val dtSec = ((nowNs - _lastFrameNs) / 1_000_000_000.0).toFloat()
            _animTimeSec += dtSec.coerceIn(0f, MAX_DT_SEC)
            _lastFrameNs = nowNs
        }
        GLES20.glClearColor(0.05f, 0.07f, 0.1f, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glUseProgram(_program)
        if (_timeLoc >= 0) {
            GLES20.glUniform1f(_timeLoc, _animTimeSec)
        }
        if (_resolutionLoc >= 0) {
            GLES20.glUniform2f(_resolutionLoc, _viewW.toFloat(), _viewH.toFloat())
        }
        if (_liquidBoostLoc >= 0) {
            GLES20.glUniform1f(_liquidBoostLoc, _liquidBoost)
        }
        if (_liquidBendLoc >= 0) {
            GLES20.glUniform1f(_liquidBendLoc, _liquidBend)
        }
        if (_warpAmountLoc >= 0) {
            GLES20.glUniform1f(_warpAmountLoc, _warpAmount)
        }
        if (_scrollSpeedLoc >= 0) {
            GLES20.glUniform1f(_scrollSpeedLoc, _scrollSpeed)
        }
        val buf = _quad ?: return
        buf.position(0)
        GLES20.glEnableVertexAttribArray(_posLoc)
        GLES20.glVertexAttribPointer(_posLoc, 2, GLES20.GL_FLOAT, false, 0, buf)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GLES20.glDisableVertexAttribArray(_posLoc)
        tickPerf(nowNs)
    }

    private fun tickPerf(nowNs: Long) {
        if (_fpsWindowNs == 0L) {
            _fpsWindowNs = nowNs
            _frameCount = 0
            return
        }
        _frameCount += 1
        val elapsed = nowNs - _fpsWindowNs
        if (elapsed >= 1_000_000_000L) {
            val fps = _frameCount * 1_000_000_000f / elapsed.toFloat()
            val frameMs = if (fps > 0.01f) 1000f / fps else 0f
            onPerf?.invoke(fps, frameMs)
            _fpsWindowNs = nowNs
            _frameCount = 0
        }
    }

    fun release() {
        if (_program != 0) {
            GLES20.glDeleteProgram(_program)
            _program = 0
        }
        _surfaceReady = false
    }

    private fun ensureQuad() {
        if (_quad != null) return
        val verts = floatArrayOf(
            -1f, -1f,
            1f, -1f,
            -1f, 1f,
            1f, 1f,
        )
        _quad = ByteBuffer.allocateDirect(verts.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(verts)
        _quad?.position(0)
    }

    private fun dropLiteProgram() {
        if (_program != 0) {
            GLES20.glDeleteProgram(_program)
            _program = 0
        }
    }

    private fun rebuildProgram() {
        val vertSrc = HolmShaderResolve.vertex(_vertexSrc)
        val wrapT0 = System.nanoTime()
        val fragSrc = try {
            HolmShaderResolve.fragment(_scene, _fragmentBody)
        } catch (t: Throwable) {
            val wrapMs = (System.nanoTime() - wrapT0) / 1_000_000f
            _shaderFailed = true
            dropLiteProgram()
            onError?.invoke("SHADER_WRAP", t.message ?: "wrap failed")
            onShaderCompiled?.invoke(false, wrapMs, 0f)
            return
        }
        val wrapMs = (System.nanoTime() - wrapT0) / 1_000_000f
        val compileT0 = System.nanoTime()
        val vert = compile(GLES20.GL_VERTEX_SHADER, vertSrc)
        val frag = compile(GLES20.GL_FRAGMENT_SHADER, fragSrc)
        if (vert == 0 || frag == 0) {
            val compileMs = (System.nanoTime() - compileT0) / 1_000_000f
            // 切换样本失败：卸掉上一档，深蓝清屏（勿继续播旧特效）
            _shaderFailed = true
            dropLiteProgram()
            onShaderCompiled?.invoke(false, wrapMs, compileMs)
            return
        }
        val program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vert)
        GLES20.glAttachShader(program, frag)
        GLES20.glLinkProgram(program)
        val link = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, link, 0)
        if (link[0] == 0) {
            val compileMs = (System.nanoTime() - compileT0) / 1_000_000f
            _shaderFailed = true
            onError?.invoke("SHADER_LINK", GLES20.glGetProgramInfoLog(program) ?: "link failed")
            GLES20.glDeleteProgram(program)
            dropLiteProgram()
            onShaderCompiled?.invoke(false, wrapMs, compileMs)
            return
        }
        val compileMs = (System.nanoTime() - compileT0) / 1_000_000f
        dropLiteProgram()
        _program = program
        _shaderFailed = false
        _posLoc = GLES20.glGetAttribLocation(_program, "a_position")
        _timeLoc = GLES20.glGetUniformLocation(_program, "u_time")
        _resolutionLoc = GLES20.glGetUniformLocation(_program, "u_resolution")
        _liquidBoostLoc = GLES20.glGetUniformLocation(_program, "u_liquidBoost")
        _liquidBendLoc = GLES20.glGetUniformLocation(_program, "u_liquidBend")
        _warpAmountLoc = GLES20.glGetUniformLocation(_program, "u_warpAmount")
        _scrollSpeedLoc = GLES20.glGetUniformLocation(_program, "u_scrollSpeed")
        onShaderCompiled?.invoke(true, wrapMs, compileMs)
    }

    private fun compile(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)
        val status = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)
        if (status[0] == 0) {
            val log = GLES20.glGetShaderInfoLog(shader)
            GLES20.glDeleteShader(shader)
            onError?.invoke("SHADER_COMPILE", log ?: "compile failed")
            return 0
        }
        return shader
    }

    companion object {
        private const val MAX_DT_SEC: Float = 0.05f
    }
}

private fun Any.toBool(): Boolean = when (this) {
    is Boolean -> this
    is Number -> this.toInt() != 0
    is String -> this == "1" || this.equals("true", ignoreCase = true)
    else -> false
}

private fun Any.toHolmFloat(default: Float): Float = when (this) {
    is Number -> this.toFloat()
    is String -> this.toFloatOrNull() ?: default
    else -> default
}

private fun Any.toHolmInt(default: Int): Int = when (this) {
    is Number -> this.toInt()
    is String -> this.toIntOrNull() ?: default
    else -> default
}
