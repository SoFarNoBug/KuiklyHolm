package com.jlj.kuiklybase.holm

import com.tencent.kuikly.core.render.web.export.IKuiklyRenderViewExport
import com.tencent.kuikly.core.render.web.ktx.KuiklyRenderCallback
import kotlinx.browser.document
import org.w3c.dom.Element
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.Node
import org.w3c.dom.events.Event

/**
 * @CreateDate: 2026/11/12
 * @Author: jlj
 * @Description: H5 KRHolmSurface — Lite WebGL 泵 / Runtime wgpu Session 泵帧；Kotlin 转发 prop。
 */
class KRHolmSurfaceView : IKuiklyRenderViewExport {

    private var _canvas: HTMLCanvasElement = newCanvasElement()
    private var _playing: Boolean = true
    private var _scene: String = ""
    private var _liquidBoost: Float = HolmSurfaceAttr.DEFAULT_LIQUID_BOOST
    private var _liquidBend: Float = HolmSurfaceAttr.DEFAULT_LIQUID_BEND
    private var _warpAmount: Float = HolmSurfaceAttr.DEFAULT_WARP_AMOUNT
    private var _scrollSpeed: Float = HolmSurfaceAttr.DEFAULT_SCROLL_SPEED
    private var _maxFps: Int = HolmSurfaceAttr.DEFAULT_MAX_FPS
    private var _executor: String = HolmSurfaceAttr.DEFAULT_EXECUTOR
    private var _fragmentShader: String = ""
    private var _vertexShader: String = ""
    private var _runtimeBlockedNotified: Boolean = false
    private var _runtimeSessionStarted: Boolean = false
    private var _runtimeLoadWaiting: Boolean = false
    private var _runtimeLoadPollId: Int = 0
    private var _runtimeSession: dynamic = null
    private var _runtimeRafId: Int = 0
    private var _runtimeTimerId: Int = 0
    private var _runtimeLastTs: Double = 0.0
    private var _runtimeAccMs: Double = 0.0
    private var _runtimePerfWindowMs: Double = 0.0
    private var _runtimePerfFrames: Int = 0
    private var _runtimeUsesWorker: Boolean = false
    private var _runtimeMainFallbackTried: Boolean = false
    private var _pendingErrorCode: String? = null
    private var _pendingErrorMessage: String? = null
    private var _onReady: KuiklyRenderCallback? = null
    private var _onError: KuiklyRenderCallback? = null
    private var _onPerf: KuiklyRenderCallback? = null
    private var _onShaderCompiled: KuiklyRenderCallback? = null
    private var _lastLiteWrapMs: Float = 0f
    private var _readyFired: Boolean = false
    private var _backend: String = HolmExecutor.BACKEND_WEBGL
    private var _started: Boolean = false
    private var _attached: Boolean = false
    private var _controlTransferred: Boolean = false
    private var _lastW: Int = 0
    private var _lastH: Int = 0
    private var _worker: dynamic = null
    private var _workerUrl: String? = null
    private var _mainPump: dynamic = null
    private var _resizeObserver: dynamic = null
    private var _visibilityHandler: ((Event) -> Unit)? = null
    private var _pumpId: Int = 0

    override val ele: HTMLElement
        get() = _canvas

    override fun setProp(propKey: String, propValue: Any): Boolean {
        return when (propKey) {
            HolmSurfaceAttr.PROP_SCENE -> {
                val next = propValue.toString()
                if (next != _scene) {
                    _scene = next
                    postScene()
                }
                true
            }
            HolmSurfaceAttr.PROP_FRAGMENT_SHADER -> {
                val next = propValue.toString()
                if (next != _fragmentShader) {
                    _fragmentShader = next
                    // 切轨过渡：错方言先只存值，等 executor 落地再喂泵
                    if (fragmentMatchesCurrentExecutor(_fragmentShader)) {
                        postScene()
                    } else {
                        fireShaderCompiled(false, 0f, 0f, _executor)
                    }
                }
                true
            }
            HolmSurfaceAttr.PROP_VERTEX_SHADER -> {
                val next = propValue.toString()
                if (next != _vertexShader) {
                    _vertexShader = next
                    if (!HolmExecutor.isRuntime(_executor)) {
                        postScene()
                    }
                }
                true
            }
            HolmSurfaceAttr.PROP_PLAYING -> {
                _playing = propValue.toBool()
                postPlaying(effectivePlaying())
                true
            }
            HolmSurfaceAttr.PROP_LIQUID_BOOST -> {
                _liquidBoost = propValue.toHolmFloat(HolmSurfaceAttr.DEFAULT_LIQUID_BOOST)
                postSet()
                true
            }
            HolmSurfaceAttr.PROP_LIQUID_BEND -> {
                _liquidBend = propValue.toHolmFloat(HolmSurfaceAttr.DEFAULT_LIQUID_BEND)
                postSet()
                true
            }
            HolmSurfaceAttr.PROP_WARP_AMOUNT -> {
                _warpAmount = propValue.toHolmFloat(HolmSurfaceAttr.DEFAULT_WARP_AMOUNT)
                postSet()
                true
            }
            HolmSurfaceAttr.PROP_SCROLL_SPEED -> {
                _scrollSpeed = propValue.toHolmFloat(HolmSurfaceAttr.DEFAULT_SCROLL_SPEED)
                postSet()
                true
            }
            HolmSurfaceAttr.PROP_MAX_FPS -> {
                _maxFps = propValue.toHolmInt(HolmSurfaceAttr.DEFAULT_MAX_FPS).coerceAtLeast(0)
                postSet()
                // 主线程 Runtime：跨 60 切换 rAF/timer 须立刻重调度
                if (HolmExecutor.isRuntime(_executor) && !_runtimeUsesWorker && _runtimeSession != null) {
                    startRuntimeLoop()
                }
                true
            }
            HolmSurfaceAttr.PROP_EXECUTOR -> {
                val next = HolmExecutor.normalize(propValue.toString())
                if (next != _executor) {
                    _executor = next
                    _readyFired = false
                    _runtimeBlockedNotified = false
                    _runtimeSessionStarted = false
                    clearWasmLoadPoll()
                    applyExecutorMode()
                }
                true
            }
            HolmSurfaceEvent.ON_READY -> {
                _onReady = propValue.unsafeCast<KuiklyRenderCallback>()
                if (_readyFired) fireReady()
                true
            }
            HolmSurfaceEvent.ON_ERROR -> {
                _onError = propValue.unsafeCast<KuiklyRenderCallback>()
                // Attr 可能先于 Event 触发 BLOCKED；与 onReady 对称重放
                replayPendingError()
                true
            }
            HolmSurfaceEvent.ON_PERF -> {
                _onPerf = propValue.unsafeCast<KuiklyRenderCallback>()
                postRuntimePerfInterest(true)
                true
            }
            HolmSurfaceEvent.ON_SHADER_COMPILED -> {
                _onShaderCompiled = propValue.unsafeCast<KuiklyRenderCallback>()
                true
            }
            else -> super.setProp(propKey, propValue)
        }
    }

    override fun onAddToParent(parent: Element) {
        _attached = true
        if (HolmExecutor.isRuntime(_executor)) {
            applyExecutorMode()
        } else if (!_started) {
            startPump()
        }
        attachObservers()
        if (!HolmExecutor.isRuntime(_executor)) {
            postPlaying(effectivePlaying())
        }
    }

    override fun onRemoveFromParent(parent: Element) {
        _attached = false
        clearWasmLoadPoll()
        detachObservers()
        destroyPump()
    }

    private fun startPump() {
        if (_started) return
        try {
            ensurePumpDispatch()
            // 同实例 remount：上次 transfer 过的 canvas 必须换新才能再 Worker / 主线程 getContext
            if (_controlTransferred) {
                replaceCanvasInPlace()
            }
            val size = readCssSize()
            _lastW = size.first
            _lastH = size.second
            val workerOk = if (canUseWorkerPath()) {
                startWorkerPath(size.first, size.second)
            } else {
                false
            }
            if (!workerOk) {
                startMainPath(size.first, size.second)
            }
            _started = true
        } catch (t: Throwable) {
            fireError("PUMP_START", t.message ?: "startPump failed")
        }
    }

    private fun canUseWorkerPath(): Boolean {
        val hasWorker = js("typeof Worker !== 'undefined'").unsafeCast<Boolean>()
        val hasTransfer = js(
            "typeof HTMLCanvasElement !== 'undefined' && typeof HTMLCanvasElement.prototype.transferControlToOffscreen === 'function'",
        ).unsafeCast<Boolean>()
        return hasWorker && hasTransfer
    }

    /** @return true = Worker 已接管并 init 成功 */
    private fun startWorkerPath(w: Int, h: Int): Boolean {
        val script = HolmGlPumpSource.SCRIPT
        val init = buildInitPayload(w, h)
        val pumpId = registerPumpView(this)
        _pumpId = pumpId
        val result = js(
            """
            (function(script, canvas, init, pumpId) {
              var blob = new Blob([script], { type: 'application/javascript' });
              var url = URL.createObjectURL(blob);
              var worker = new Worker(url);
              worker.onmessage = function(e) {
                if (typeof self.__holmPumpDispatch === 'function') {
                  self.__holmPumpDispatch(pumpId, e.data);
                }
              };
              worker.onerror = function(e) {
                if (typeof self.__holmPumpDispatch === 'function') {
                  self.__holmPumpDispatch(pumpId, {
                    type: 'error',
                    code: 'WORKER_ERROR',
                    message: (e && e.message) ? e.message : 'worker error'
                  });
                }
              };
              var transferred = false;
              try {
                var offscreen = canvas.transferControlToOffscreen();
                transferred = true;
                init.canvas = offscreen;
                worker.postMessage(init, [offscreen]);
                return { ok: true, transferred: true, worker: worker, url: url };
              } catch (err) {
                if (typeof self.__holmPumpDispatch === 'function') {
                  self.__holmPumpDispatch(pumpId, {
                    type: 'error',
                    code: 'WORKER_INIT',
                    message: String(err && err.message ? err.message : err)
                  });
                }
                try { worker.terminate(); } catch (_) {}
                try { URL.revokeObjectURL(url); } catch (_) {}
                return { ok: false, transferred: transferred, worker: null, url: null };
              }
            })
            """,
        ).unsafeCast<(String, HTMLCanvasElement, dynamic, Int) -> dynamic>()(
            script,
            _canvas,
            init,
            pumpId,
        )
        val ok = result.ok == true
        if (ok) {
            _worker = result.worker
            _workerUrl = result.url as? String
            _controlTransferred = true
            _backend = "webgl-worker"
            return true
        }
        unregisterPumpView(_pumpId)
        _pumpId = 0
        _worker = null
        _workerUrl = null
        if (result.transferred == true) {
            replaceCanvasInPlace()
        }
        return false
    }

    private fun startMainPath(w: Int, h: Int) {
        ensureMainPumpInstalled()
        val pumpId = registerPumpView(this)
        _pumpId = pumpId
        val pump = js(
            """
            (function(create, pumpId) {
              return create(function(m) {
                if (typeof self.__holmPumpDispatch === 'function') {
                  self.__holmPumpDispatch(pumpId, m);
                }
              }, 'webgl');
            })
            """,
        ).unsafeCast<(dynamic, Int) -> dynamic>()(
            js("self.createHolmGlPump"),
            pumpId,
        )
        _mainPump = pump
        _backend = "webgl"
        val init = buildInitPayload(w, h)
        init.canvas = _canvas
        pump.init(init)
    }

    private fun ensureMainPumpInstalled() {
        val installed = js("typeof self.createHolmGlPump === 'function'").unsafeCast<Boolean>()
        if (installed) return
        val script = HolmGlPumpSource.SCRIPT
        js("(function(src){ (0, eval)(src); })").unsafeCast<(String) -> Unit>()(script)
    }

    private fun buildInitPayload(w: Int, h: Int): dynamic {
        // postMessage structured-clone 需要 JS 原始 number/boolean，不能塞 Kotlin 装箱 Float
        val o = js("{}")
        o.type = "init"
        o.vert = HolmShaderResolve.vertex(_vertexShader)
        o.frag = resolveLiteFragmentOrEmpty()
        o.wrapMs = _lastLiteWrapMs.toDouble()
        o.scene = _scene
        o.liquidBoost = _liquidBoost.toDouble()
        o.liquidBend = _liquidBend.toDouble()
        o.warpAmount = _warpAmount.toDouble()
        o.scrollSpeed = _scrollSpeed.toDouble()
        o.maxFps = _maxFps
        o.playing = effectivePlaying()
        o.w = w
        o.h = h
        o.customFs = _fragmentShader
        return o
    }

    private fun postSet() {
        if (HolmExecutor.isRuntime(_executor)) {
            if (_runtimeUsesWorker) {
                if (!_started) return
                val msg = js("{}")
                msg.type = "set"
                msg.liquidBoost = _liquidBoost.toDouble()
                msg.liquidBend = _liquidBend.toDouble()
                msg.maxFps = _maxFps
                dispatch(msg)
                return
            }
            val session = _runtimeSession ?: return
            HolmRuntimeNative.sessionSetLiquid(session, _liquidBoost, _liquidBend)
            return
        }
        if (!_started) return
        val msg = js("{}")
        msg.type = "set"
        msg.liquidBoost = _liquidBoost.toDouble()
        msg.liquidBend = _liquidBend.toDouble()
        msg.warpAmount = _warpAmount.toDouble()
        msg.scrollSpeed = _scrollSpeed.toDouble()
        msg.maxFps = _maxFps
        dispatch(msg)
    }

    private fun postScene() {
        if (HolmExecutor.isRuntime(_executor)) {
            if (_runtimeUsesWorker) {
                if (!_started) return
                val msg = js("{}")
                msg.type = "scene"
                msg.scene = _scene
                msg.customFs = _fragmentShader
                dispatch(msg)
                return
            }
            val session = _runtimeSession ?: return
            applyRuntimeShaders(session)
            return
        }
        if (!_started) return
        val msg = js("{}")
        msg.type = "scene"
        msg.scene = _scene
        msg.vert = HolmShaderResolve.vertex(_vertexShader)
        msg.frag = resolveLiteFragmentOrEmpty()
        msg.wrapMs = _lastLiteWrapMs.toDouble()
        dispatch(msg)
    }

    /** wrap 失败：返回空串，泵侧 rebuild 失败并保留上次程序（无官方席回退）。 */
    private fun resolveLiteFragmentOrEmpty(): String {
        val t0 = js("typeof performance !== 'undefined' && performance.now ? performance.now() : Date.now()")
            .unsafeCast<Double>()
        return try {
            val frag = HolmShaderResolve.fragment(_scene, _fragmentShader)
            val t1 = js("typeof performance !== 'undefined' && performance.now ? performance.now() : Date.now()")
                .unsafeCast<Double>()
            _lastLiteWrapMs = (t1 - t0).toFloat()
            frag
        } catch (t: Throwable) {
            val t1 = js("typeof performance !== 'undefined' && performance.now ? performance.now() : Date.now()")
                .unsafeCast<Double>()
            _lastLiteWrapMs = (t1 - t0).toFloat()
            fireError("SHADER_WRAP", t.message ?: "wrap failed")
            fireShaderCompiled(false, _lastLiteWrapMs, 0f, HolmExecutor.LITE)
            ""
        }
    }

    private fun fragmentMatchesCurrentExecutor(src: String): Boolean {
        if (src.isBlank()) return true
        val wgsl = HolmShaderResolve.looksLikeWgslFragment(src)
        return if (HolmExecutor.isRuntime(_executor)) wgsl else !wgsl
    }

    private fun applyRuntimeShaders(session: dynamic) {
        val custom = _fragmentShader.trim()
        // 切换样本/轨：先卸上一档，再装新；失败停 ClearTri
        HolmRuntimeNative.sessionSetCustomFs(session, "")
        if (custom.isNotEmpty()) {
            if (!HolmShaderResolve.looksLikeWgslFragment(custom)) {
                // 仍持 GLSL 正文时跳过，等 Attr 换成 WGSL（已卸旧 custom）
                HolmRuntimeNative.sessionSetScene(session, _scene)
                flushRuntimeSessionErrors(session)
                fireShaderCompiled(false, 0f, 0f, HolmExecutor.RUNTIME)
                return
            }
            val t0 = js("typeof performance !== 'undefined' && performance.now ? performance.now() : Date.now()")
                .unsafeCast<Double>()
            val err = HolmRuntimeNative.sessionSetCustomFs(session, custom)
            val t1 = js("typeof performance !== 'undefined' && performance.now ? performance.now() : Date.now()")
                .unsafeCast<Double>()
            val compileMs = (t1 - t0).toFloat()
            val drained = flushRuntimeSessionErrors(session)
            if (err != null) {
                // wasm 已 present_clear_only + playing=false；主线程泵对齐停
                HolmRuntimeNative.sessionSetPlaying(session, false)
                _playing = false
                postPlaying(false)
                if (drained == 0) {
                    fireError("SHADER_CUSTOM", err)
                }
                fireShaderCompiled(false, 0f, compileMs, HolmExecutor.RUNTIME)
            } else {
                clearPendingError()
                fireShaderCompiled(true, 0f, compileMs, HolmExecutor.RUNTIME)
            }
        } else {
            HolmRuntimeNative.sessionSetScene(session, _scene)
            flushRuntimeSessionErrors(session)
            clearPendingError()
            // 空片元 ClearTri：ok=false，对齐 Lite 拒绝语义（审查 P2）
            fireShaderCompiled(false, 0f, 0f, HolmExecutor.RUNTIME)
        }
    }

    /** @return 已投递条数 */
    private fun flushRuntimeSessionErrors(session: dynamic): Int {
        if (session == null) return 0
        val errs = HolmRuntimeNative.sessionDrainErrors(session)
        if (errs.isEmpty()) return 0
        for (item in errs) {
            val code = (item.code as? String)?.ifBlank { null } ?: "WGPU_VALIDATION"
            val message = (item.message as? String) ?: ""
            fireError(code, message)
        }
        return errs.size
    }

    private fun postResize(w: Int, h: Int) {
        if (w == _lastW && h == _lastH) return
        _lastW = w
        _lastH = h
        if (HolmExecutor.isRuntime(_executor)) {
            if (_runtimeUsesWorker) {
                if (!_started) return
                val msg = js("{}")
                msg.type = "resize"
                msg.w = w
                msg.h = h
                dispatch(msg)
                return
            }
            val session = _runtimeSession ?: return
            // 与 Lite 一致：CSS 像素缓冲，不乘 DPR
            _canvas.width = w.coerceAtLeast(1)
            _canvas.height = h.coerceAtLeast(1)
            HolmRuntimeNative.sessionResize(session, w.coerceAtLeast(1), h.coerceAtLeast(1))
            return
        }
        if (!_started) return
        val msg = js("{}")
        msg.type = "resize"
        msg.w = w
        msg.h = h
        dispatch(msg)
    }

    private fun postPlaying(playing: Boolean) {
        if (HolmExecutor.isRuntime(_executor)) {
            if (_runtimeUsesWorker) {
                if (!_started) return
                val msg = js("{}")
                msg.type = "playing"
                msg.playing = playing
                dispatch(msg)
                return
            }
            val session = _runtimeSession ?: return
            HolmRuntimeNative.sessionSetPlaying(session, playing)
            if (playing) {
                if (_runtimeRafId == 0) startRuntimeLoop()
            } else {
                stopRuntimeLoop()
            }
            return
        }
        if (!_started) return
        val msg = js("{}")
        msg.type = "playing"
        msg.playing = playing
        dispatch(msg)
    }

    private fun dispatch(msg: dynamic) {
        val worker = _worker
        if (worker != null) {
            try {
                worker.postMessage(msg)
            } catch (t: Throwable) {
                fireError("POST_MESSAGE", t.message ?: "postMessage failed")
            }
            return
        }
        val pump = _mainPump ?: return
        when ((msg.type as? Any)?.toString()) {
            "set" -> pump.set(msg)
            "resize" -> pump.resize(msg)
            "scene" -> pump.scene(msg)
            "playing" -> pump.playing(msg)
            "destroy" -> pump.destroy()
        }
    }

    internal fun onPumpMessage(data: dynamic) {
        when ((data.type as? Any)?.toString()) {
            "ready" -> {
                val backend = (data.backend as? Any)?.toString()
                if (!backend.isNullOrBlank()) {
                    _backend = backend
                }
                clearPendingError()
                if (!_readyFired) {
                    _readyFired = true
                    fireReady()
                }
            }
            "error" -> {
                val code = (data.code as? Any)?.toString() ?: "PUMP_ERROR"
                val message = (data.message as? Any)?.toString() ?: "pump error"
                // Runtime Worker 异步建 Session 失败：与主线程 onError 同契约 + 尝试主线程回退
                if (_runtimeUsesWorker && HolmExecutor.isRuntime(_executor) &&
                    (code == "CREATE_SESSION" || code == "IMPORT_SCRIPTS" || code == "NO_BINDGEN" ||
                        code == "NO_WASM_URL" || code == "TICK")
                ) {
                    handleRuntimeWorkerFatal(code, message)
                    return
                }
                // 同步 transfer 失败会先打 WORKER_INIT，随后主线程回退；不抢先 BLOCKED
                if (code == "WORKER_INIT" || code == "WORKER_ERROR") {
                    fireError(code, message)
                    return
                }
                fireError(code, message)
            }
            "perf" -> {
                if (_onPerf == null) return
                val fps = (data.fps as? Number)?.toFloat() ?: 0f
                val frameMs = (data.frameMs as? Number)?.toFloat() ?: 0f
                firePerf(fps, frameMs)
            }
            "shaderCompiled" -> {
                val okRaw = data.ok
                val ok = when (okRaw) {
                    is Boolean -> okRaw
                    is Number -> okRaw.toInt() != 0
                    else -> false
                }
                val wrapMs = (data.wrapMs as? Number)?.toFloat() ?: 0f
                val compileMs = (data.compileMs as? Number)?.toFloat() ?: 0f
                val executor = (data.executor as? Any)?.toString()
                    ?.ifBlank { null }
                    ?: _executor
                fireShaderCompiled(ok, wrapMs, compileMs, executor)
            }
        }
    }

    private fun attachObservers() {
        if (_resizeObserver == null) {
            val onResize: () -> Unit = {
                val size = readCssSize()
                postResize(size.first, size.second)
            }
            _resizeObserver = js(
                """
                (function(canvas, cb) {
                  if (typeof ResizeObserver === 'undefined') {
                    window.addEventListener('resize', cb);
                    return { disconnect: function() { window.removeEventListener('resize', cb); } };
                  }
                  var ro = new ResizeObserver(function() { cb(); });
                  ro.observe(canvas);
                  return ro;
                })
                """,
            ).unsafeCast<(HTMLCanvasElement, () -> Unit) -> dynamic>()(_canvas, onResize)
        }
        if (_visibilityHandler == null) {
            val handler: (Event) -> Unit = {
                postPlaying(effectivePlaying())
            }
            _visibilityHandler = handler
            document.addEventListener("visibilitychange", handler)
        }
    }

    private fun detachObservers() {
        val ro = _resizeObserver
        if (ro != null) {
            try {
                ro.disconnect()
            } catch (_: Throwable) {
                // ignore
            }
            _resizeObserver = null
        }
        val handler = _visibilityHandler
        if (handler != null) {
            document.removeEventListener("visibilitychange", handler)
            _visibilityHandler = null
        }
    }

    private fun destroyRuntimeSession() {
        stopRuntimeLoop()
        val session = _runtimeSession
        _runtimeSession = null
        _runtimeSessionStarted = false
        _runtimeUsesWorker = false
        _runtimeMainFallbackTried = false
        if (session != null) {
            HolmRuntimeNative.sessionDestroy(session)
        }
    }

    private fun clearWasmLoadPoll() {
        if (_runtimeLoadPollId != 0) {
            js("clearInterval").unsafeCast<(Int) -> Unit>()(_runtimeLoadPollId)
            _runtimeLoadPollId = 0
        }
        _runtimeLoadWaiting = false
    }

    private fun postRuntimePerfInterest(want: Boolean) {
        if (!_runtimeUsesWorker || !_started) return
        val msg = js("{}")
        msg.type = "set"
        msg.reportPerf = want
        dispatch(msg)
    }

    /**
     * Worker 建 Session 失败：拆 Worker、换 canvas，优先主线程回退一次；再失败则 BLOCKED+ready。
     */
    private fun handleRuntimeWorkerFatal(code: String, message: String) {
        if (!_attached || !HolmExecutor.isRuntime(_executor)) {
            _runtimeSessionStarted = false
            _backend = HolmExecutor.BACKEND_WGPU_BLOCKED
            if (!_runtimeBlockedNotified) {
                _runtimeBlockedNotified = true
                fireError(HolmExecutor.ERROR_BLOCKED, "$code: $message")
            }
            if (!_readyFired) {
                _readyFired = true
                fireReady()
            }
            return
        }
        // 拆掉已 transfer 的 Worker 画布
        destroyPump()
        if (_attached) {
            attachObservers()
        }
        _runtimeSessionStarted = false
        if (_runtimeMainFallbackTried) {
            _backend = HolmExecutor.BACKEND_WGPU_BLOCKED
            if (!_runtimeBlockedNotified) {
                _runtimeBlockedNotified = true
                fireError(HolmExecutor.ERROR_BLOCKED, "$code: $message")
            }
            if (!_readyFired) {
                _readyFired = true
                fireReady()
            }
            return
        }
        _runtimeMainFallbackTried = true
        startRuntimeMainSession()
    }

    private fun stopRuntimeLoop() {
        if (_runtimeRafId != 0) {
            js("cancelAnimationFrame").unsafeCast<(Int) -> Unit>()(_runtimeRafId)
            _runtimeRafId = 0
        }
        if (_runtimeTimerId != 0) {
            js("clearTimeout").unsafeCast<(Int) -> Unit>()(_runtimeTimerId)
            _runtimeTimerId = 0
        }
        _runtimeLastTs = 0.0
        _runtimeAccMs = 0.0
        _runtimePerfWindowMs = 0.0
        _runtimePerfFrames = 0
    }

    private fun startRuntimeLoop() {
        if (_runtimeUsesWorker) return
        stopRuntimeLoop()
        if (!effectivePlaying()) return
        lateinit var onFrame: (Double) -> Unit
        lateinit var scheduleNext: (Double) -> Unit
        onFrame = frame@{ now ->
            _runtimeRafId = 0
            _runtimeTimerId = 0
            if (!HolmExecutor.isRuntime(_executor) || _runtimeSession == null || _runtimeUsesWorker) {
                return@frame
            }
            if (!effectivePlaying()) {
                return@frame
            }
            if (_runtimeLastTs == 0.0) {
                _runtimeLastTs = now
            }
            val dtMs = (now - _runtimeLastTs).coerceIn(0.0, 50.0)
            _runtimeLastTs = now
            val minFrameMs = if (_maxFps > 0) 1000.0 / _maxFps.toDouble() else 0.0
            _runtimeAccMs += dtMs
            if (minFrameMs <= 0.0 || _runtimeAccMs >= minFrameMs) {
                val dtSecs = (_runtimeAccMs / 1000.0).toFloat()
                _runtimeAccMs = 0.0
                val session = _runtimeSession
                val tickErr = HolmRuntimeNative.sessionTick(session, dtSecs)
                val drained = flushRuntimeSessionErrors(session)
                if (tickErr != null) {
                    _backend = HolmExecutor.BACKEND_WGPU_BLOCKED
                    if (!_runtimeBlockedNotified) {
                        _runtimeBlockedNotified = true
                        if (drained == 0) {
                            fireError(
                                "TICK",
                                tickErr.ifBlank { "holm-runtime tick failed" },
                            )
                        }
                    }
                    stopRuntimeLoop()
                    return@frame
                }
                // 与 Lite 对齐：有 onPerf 时约 1s 汇总一次，禁止每帧打穿 Compose
                if (_onPerf != null) {
                    if (_runtimePerfWindowMs == 0.0) {
                        _runtimePerfWindowMs = now
                        _runtimePerfFrames = 0
                    } else {
                        _runtimePerfFrames += 1
                        val elapsed = now - _runtimePerfWindowMs
                        if (elapsed >= 1000.0) {
                            val fps = (_runtimePerfFrames * 1000.0 / elapsed).toFloat()
                            val frameMs = if (fps > 0.01f) 1000f / fps else 0f
                            firePerf(fps, frameMs)
                            _runtimePerfWindowMs = now
                            _runtimePerfFrames = 0
                        }
                    }
                }
            }
            scheduleNext(now)
        }
        scheduleNext = sched@{ now ->
            if (!effectivePlaying() || _runtimeSession == null || _runtimeUsesWorker) return@sched
            // 0=不限：rAF（对齐 Lite，靠 AutoNoVsync 吃高刷）；>60：timer；1..60：rAF+跳帧
            if (_maxFps > 60) {
                val interval = 1000.0 / _maxFps.toDouble()
                val delay =
                    if (_runtimeAccMs >= interval) 0.0 else (interval - _runtimeAccMs).coerceIn(0.0, interval)
                _runtimeTimerId = js(
                    """
                    (function(cb, delay) {
                      return setTimeout(function() { cb(performance.now()); }, delay);
                    })
                    """,
                ).unsafeCast<((Double) -> Unit, Double) -> Int>()(onFrame, delay)
            } else {
                _runtimeRafId = js("requestAnimationFrame").unsafeCast<((Double) -> Unit) -> Int>()(onFrame)
            }
        }
        scheduleNext(js("performance.now()").unsafeCast<Double>())
    }

    /** Runtime Worker 路径：OffscreenCanvas + wasm Session，主线程只转发。 */
    private fun startRuntimeWorkerPath(w: Int, h: Int): Boolean {
        val script = HolmRuntimePumpSource.SCRIPT
        val glueUrl = js("new URL('holm-runtime/holm_runtime.js', location.href).href")
            .unsafeCast<String>()
        val wasmUrl = js("new URL('holm-runtime/holm_runtime_bg.wasm', location.href).href")
            .unsafeCast<String>()
        val init = js("{}")
        init.type = "init"
        init.glueUrl = glueUrl
        init.wasmUrl = wasmUrl
        init.scene = _scene
        init.liquidBoost = _liquidBoost.toDouble()
        init.liquidBend = _liquidBend.toDouble()
        init.maxFps = _maxFps
        init.playing = effectivePlaying()
        init.reportPerf = _onPerf != null
        init.w = w
        init.h = h
        init.customFs = _fragmentShader
        val pumpId = registerPumpView(this)
        _pumpId = pumpId
        val result = js(
            """
            (function(script, canvas, init, pumpId) {
              var blob = new Blob([script], { type: 'application/javascript' });
              var url = URL.createObjectURL(blob);
              var worker = new Worker(url);
              worker.onmessage = function(e) {
                if (typeof self.__holmPumpDispatch === 'function') {
                  self.__holmPumpDispatch(pumpId, e.data);
                }
              };
              worker.onerror = function(e) {
                if (typeof self.__holmPumpDispatch === 'function') {
                  self.__holmPumpDispatch(pumpId, {
                    type: 'error',
                    code: 'WORKER_ERROR',
                    message: (e && e.message) ? e.message : 'worker error'
                  });
                }
              };
              var transferred = false;
              try {
                var offscreen = canvas.transferControlToOffscreen();
                transferred = true;
                init.canvas = offscreen;
                worker.postMessage(init, [offscreen]);
                return { ok: true, transferred: true, worker: worker, url: url };
              } catch (err) {
                if (typeof self.__holmPumpDispatch === 'function') {
                  self.__holmPumpDispatch(pumpId, {
                    type: 'error',
                    code: 'WORKER_INIT',
                    message: String(err && err.message ? err.message : err)
                  });
                }
                try { worker.terminate(); } catch (_) {}
                try { URL.revokeObjectURL(url); } catch (_) {}
                return { ok: false, transferred: transferred, worker: null, url: null };
              }
            })
            """,
        ).unsafeCast<(String, HTMLCanvasElement, dynamic, Int) -> dynamic>()(
            script,
            _canvas,
            init,
            pumpId,
        )
        val ok = result.ok == true
        if (ok) {
            _worker = result.worker
            _workerUrl = result.url as? String
            _controlTransferred = true
            _runtimeUsesWorker = true
            _backend = HolmExecutor.BACKEND_WGPU_M0
            return true
        }
        unregisterPumpView(_pumpId)
        _pumpId = 0
        _worker = null
        _workerUrl = null
        _runtimeUsesWorker = false
        if (result.transferred == true) {
            replaceCanvasInPlace()
        }
        return false
    }

    private fun destroyPump() {
        destroyRuntimeSession()
        unregisterPumpView(_pumpId)
        _pumpId = 0
        if (_worker != null) {
            val msg = js("{}")
            msg.type = "destroy"
            try {
                _worker.postMessage(msg)
            } catch (_: Throwable) {
                // ignore
            }
            try {
                _worker.terminate()
            } catch (_: Throwable) {
                // ignore
            }
            _worker = null
            val url = _workerUrl
            if (url != null) {
                js("URL.revokeObjectURL").unsafeCast<(String) -> Unit>()(url)
                _workerUrl = null
            }
        }
        val pump = _mainPump
        if (pump != null) {
            try {
                pump.destroy()
            } catch (_: Throwable) {
                // ignore
            }
            _mainPump = null
        }
        if (_controlTransferred) {
            replaceCanvasInPlace()
        }
        _started = false
        _readyFired = false
        clearWasmLoadPoll()
    }

    private fun replaceCanvasInPlace() {
        val old = _canvas
        val next = newCanvasElement()
        val parent: Node? = old.parentNode
        if (parent != null) {
            parent.replaceChild(next, old)
        }
        _canvas = next
        _controlTransferred = false
        // 旧 RO 盯的是旧节点；清掉以便下次 attachObservers 绑新 canvas
        val ro = _resizeObserver
        if (ro != null) {
            try {
                ro.disconnect()
            } catch (_: Throwable) {
                // ignore
            }
            _resizeObserver = null
        }
    }

    private fun effectivePlaying(): Boolean {
        val hidden = document.asDynamic().hidden == true
        return _playing && !hidden
    }

    private fun readCssSize(): Pair<Int, Int> {
        val w = _canvas.clientWidth.coerceAtLeast(1)
        val h = _canvas.clientHeight.coerceAtLeast(1)
        return w to h
    }

    private fun applyExecutorMode() {
        if (HolmExecutor.isRuntime(_executor)) {
            if (_started) {
                destroyPump()
            }
            runCatching {
                _canvas.style.setProperty("background", "#1E2A4A")
            }
            if (!HolmRuntimeNative.isAvailable()) {
                if (HolmRuntimeNative.isLoadPending()) {
                    if (!_runtimeLoadWaiting) {
                        _runtimeLoadWaiting = true
                        // wasm 仍在 init：短轮询，避免首屏永久 BLOCKED；保存 id 便于卸岛取消
                        _runtimeLoadPollId = js(
                            """
                            (function(retry) {
                              var n = 0;
                              var t = setInterval(function() {
                                n++;
                                if (window.__holmRuntime && window.__holmRuntime.ready === true) {
                                  clearInterval(t); retry(t);
                                } else if (window.__holmRuntime && window.__holmRuntime.error) {
                                  clearInterval(t); retry(t);
                                } else if (n >= 40) {
                                  clearInterval(t); retry(t);
                                }
                              }, 50);
                              return t;
                            })
                            """,
                        ).unsafeCast<(((Int) -> Unit) -> Int)>()({ id ->
                            if (_runtimeLoadPollId == id) {
                                _runtimeLoadPollId = 0
                            }
                            _runtimeLoadWaiting = false
                            if (_attached && HolmExecutor.isRuntime(_executor)) {
                                applyExecutorMode()
                            }
                        })
                    }
                    return
                }
                _backend = HolmExecutor.BACKEND_WGPU_BLOCKED
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
            if (_runtimeSessionStarted) return
            _runtimeSessionStarted = true
            _runtimeMainFallbackTried = false
            val size = readCssSize()
            _lastW = size.first
            _lastH = size.second
            // 优先 Worker+Offscreen（对齐 Lite 主线程卸载）；失败再主线程 Session
            if (canUseWorkerPath() && startRuntimeWorkerPath(size.first, size.second)) {
                _started = true
                runCatching { _canvas.style.removeProperty("background") }
                clearPendingError()
                // ready / 失败由 Worker 异步上报
                return
            }
            startRuntimeMainSession()
        } else {
            // P0：runtime→lite 必须对称拆 Worker/Offscreen，再挂 Lite 泵
            clearWasmLoadPoll()
            runCatching {
                _canvas.style.removeProperty("background")
            }
            clearPendingError()
            destroyPump()
            if (_attached) {
                attachObservers()
            }
            startPump()
        }
    }

    /** 主线程 Runtime Session（Worker 不可用或 Worker 异步失败回退）。 */
    private fun startRuntimeMainSession() {
        if (!_attached || !HolmExecutor.isRuntime(_executor)) {
            _runtimeSessionStarted = false
            return
        }
        _runtimeSessionStarted = true
        HolmRuntimeNative.createSession(
            canvas = _canvas,
            onSuccess = { session, backend ->
                if (!_attached || !HolmExecutor.isRuntime(_executor)) {
                    HolmRuntimeNative.sessionDestroy(session)
                    return@createSession
                }
                _runtimeSession = session
                _runtimeUsesWorker = false
                _backend = if (backend.isNotBlank()) backend else HolmExecutor.BACKEND_WGPU_M0
                runCatching { _canvas.style.removeProperty("background") }
                applyRuntimeShaders(session)
                HolmRuntimeNative.sessionSetLiquid(session, _liquidBoost, _liquidBend)
                HolmRuntimeNative.sessionSetPlaying(session, effectivePlaying())
                clearPendingError()
                _readyFired = true
                fireReady()
                startRuntimeLoop()
            },
            onError = { message ->
                if (!_attached || !HolmExecutor.isRuntime(_executor)) return@createSession
                _runtimeSessionStarted = false
                _backend = HolmExecutor.BACKEND_WGPU_BLOCKED
                if (!_runtimeBlockedNotified) {
                    _runtimeBlockedNotified = true
                    fireError(HolmExecutor.ERROR_BLOCKED, message)
                }
                _readyFired = true
                fireReady()
            },
        )
    }

    private fun fireReady() {
        // Runtime 路径以 present 结果写入的 `_backend` 为准（失败不得假报 wgpu-m0）
        _onReady?.invoke(
            mapOf(
                "backend" to _backend,
                "quality" to "auto",
                "scene" to _scene,
                "executor" to _executor,
            ),
        )
    }

    private fun fireError(code: String, message: String) {
        _pendingErrorCode = code
        _pendingErrorMessage = message
        val callback = _onError ?: return
        callback.invoke(
            mapOf(
                "code" to code,
                "message" to message,
            ),
        )
    }

    private fun replayPendingError() {
        val code = _pendingErrorCode ?: return
        val message = _pendingErrorMessage ?: return
        val callback = _onError ?: return
        callback.invoke(
            mapOf(
                "code" to code,
                "message" to message,
            ),
        )
    }

    private fun clearPendingError() {
        _pendingErrorCode = null
        _pendingErrorMessage = null
        _runtimeBlockedNotified = false
    }

    private fun firePerf(fps: Float, frameMs: Float) {
        val callback = _onPerf ?: return
        callback.invoke(
            mapOf(
                "fps" to fps,
                "frameMs" to frameMs,
            ),
        )
    }

    private fun fireShaderCompiled(ok: Boolean, wrapMs: Float, compileMs: Float, executor: String) {
        val callback = _onShaderCompiled ?: return
        callback.invoke(
            mapOf(
                "ok" to ok,
                "wrapMs" to wrapMs,
                "compileMs" to compileMs,
                "executor" to executor,
            ),
        )
    }

    companion object {
        const val VIEW_NAME: String = HolmSurfaceView.VIEW_NAME

        private var _nextPumpId: Int = 1
        private val _pumpViews: MutableMap<Int, KRHolmSurfaceView> = mutableMapOf()
        private var _dispatchReady: Boolean = false

        private fun newCanvasElement(): HTMLCanvasElement {
            val canvas = document.createElement("canvas").unsafeCast<HTMLCanvasElement>()
            canvas.style.asDynamic().width = "100%"
            canvas.style.asDynamic().height = "100%"
            canvas.style.asDynamic().display = "block"
            canvas.style.asDynamic().background = "#0d1219"
            return canvas
        }

        private fun ensurePumpDispatch() {
            if (_dispatchReady) return
            _dispatchReady = true
            val dispatch: (dynamic, dynamic) -> Unit = dispatch@{ id, data ->
                // JS number → Map<Int> 须显式 toInt，否则找不到 view
                val key = (id as? Number)?.toInt() ?: return@dispatch
                _pumpViews[key]?.onPumpMessage(data)
            }
            // 禁止 js("self").asDynamic()：会编译成 self.asDynamic() 运行时炸
            js("(function(fn){ self.__holmPumpDispatch = fn; })").unsafeCast<(dynamic) -> Unit>()(dispatch)
        }

        private fun registerPumpView(view: KRHolmSurfaceView): Int {
            ensurePumpDispatch()
            val id = _nextPumpId
            _nextPumpId = id + 1
            _pumpViews[id] = view
            return id
        }

        private fun unregisterPumpView(id: Int) {
            if (id != 0) {
                _pumpViews.remove(id)
            }
        }
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
