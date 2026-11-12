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
 * @Description: H5 KRHolmSurface — OffscreenCanvas Worker 纯 JS 泵帧（回退主线程泵）；Kotlin 只转发 prop。
 */
class KRHolmSurfaceView : IKuiklyRenderViewExport {

    private var _canvas: HTMLCanvasElement = newCanvasElement()
    private var _playing: Boolean = true
    private var _scene: String = HolmSceneShaders.SCENE_FROST
    private var _liquidBoost: Float = HolmSurfaceAttr.DEFAULT_LIQUID_BOOST
    private var _liquidBend: Float = HolmSurfaceAttr.DEFAULT_LIQUID_BEND
    private var _warpAmount: Float = HolmSurfaceAttr.DEFAULT_WARP_AMOUNT
    private var _scrollSpeed: Float = HolmSurfaceAttr.DEFAULT_SCROLL_SPEED
    private var _maxFps: Int = HolmSurfaceAttr.DEFAULT_MAX_FPS
    private var _onReady: KuiklyRenderCallback? = null
    private var _onError: KuiklyRenderCallback? = null
    private var _onPerf: KuiklyRenderCallback? = null
    private var _readyFired: Boolean = false
    private var _backend: String = "webgl"
    private var _started: Boolean = false
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
                true
            }
            HolmSurfaceEvent.ON_READY -> {
                _onReady = propValue.unsafeCast<KuiklyRenderCallback>()
                if (_readyFired) fireReady()
                true
            }
            HolmSurfaceEvent.ON_ERROR -> {
                _onError = propValue.unsafeCast<KuiklyRenderCallback>()
                true
            }
            HolmSurfaceEvent.ON_PERF -> {
                _onPerf = propValue.unsafeCast<KuiklyRenderCallback>()
                true
            }
            else -> super.setProp(propKey, propValue)
        }
    }

    override fun onAddToParent(parent: Element) {
        if (!_started) {
            startPump()
        }
        attachObservers()
        postPlaying(effectivePlaying())
    }

    override fun onRemoveFromParent(parent: Element) {
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
        o.vert = HolmSceneShaders.gles2Vertex()
        o.frag = HolmSceneShaders.gles2Fragment(_scene)
        o.scene = _scene
        o.liquidBoost = _liquidBoost.toDouble()
        o.liquidBend = _liquidBend.toDouble()
        o.warpAmount = _warpAmount.toDouble()
        o.scrollSpeed = _scrollSpeed.toDouble()
        o.maxFps = _maxFps
        o.playing = effectivePlaying()
        o.w = w
        o.h = h
        return o
    }

    private fun postSet() {
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
        if (!_started) return
        val msg = js("{}")
        msg.type = "scene"
        msg.scene = _scene
        msg.vert = HolmSceneShaders.gles2Vertex()
        msg.frag = HolmSceneShaders.gles2Fragment(_scene)
        dispatch(msg)
    }

    private fun postResize(w: Int, h: Int) {
        if (!_started) return
        if (w == _lastW && h == _lastH) return
        _lastW = w
        _lastH = h
        val msg = js("{}")
        msg.type = "resize"
        msg.w = w
        msg.h = h
        dispatch(msg)
    }

    private fun postPlaying(playing: Boolean) {
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
                if (!_readyFired) {
                    _readyFired = true
                    fireReady()
                }
            }
            "error" -> {
                fireError(
                    (data.code as? Any)?.toString() ?: "PUMP_ERROR",
                    (data.message as? Any)?.toString() ?: "pump error",
                )
            }
            "perf" -> {
                val fps = (data.fps as? Number)?.toFloat() ?: 0f
                val frameMs = (data.frameMs as? Number)?.toFloat() ?: 0f
                firePerf(fps, frameMs)
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

    private fun destroyPump() {
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

    private fun fireReady() {
        _onReady?.invoke(
            mapOf(
                "backend" to _backend,
                "quality" to "auto",
                "scene" to _scene,
            ),
        )
    }

    private fun fireError(code: String, message: String) {
        _onError?.invoke(
            mapOf(
                "code" to code,
                "message" to message,
            ),
        )
    }

    private fun firePerf(fps: Float, frameMs: Float) {
        _onPerf?.invoke(
            mapOf(
                "fps" to fps,
                "frameMs" to frameMs,
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
