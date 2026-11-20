package com.jlj.kuiklybase.holm

import com.jlj.kuiklybase.holm.gltf.GltfDocument
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
    private var _warpAmount: Float = HolmSurfaceAttr.DEFAULT_WARP_AMOUNT
    private var _scrollSpeed: Float = HolmSurfaceAttr.DEFAULT_SCROLL_SPEED
    private var _maxFps: Int = HolmSurfaceAttr.DEFAULT_MAX_FPS
    private var _renderScale: Float = HolmSurfaceAttr.DEFAULT_RENDER_SCALE
    private var _executor: String = HolmSurfaceAttr.DEFAULT_EXECUTOR
    private var _contentId: String = ""
    /** Attr 桥 contentKind；Model 时禁止抢先 Lite Offscreen。 */
    private var _bridgeContentKind: String = ""
    private var _bridgeModelAsset: String = ""
    private var _bridgeModelFileUri: String = ""
    private var _bridgeModelOrbit: Boolean = true
    private var _bridgeModelYaw: Float = 0f
    private var _bridgeModelPitch: Float = 0f
    private var _bridgeModelLightsRaw: String = ""
    private var _bridgeModelSolidRaw: String = ""
    /** H5 双包 fromBytes：Base64 过 NVI（Demo 主路径仍用 asset）。 */
    private var _bridgeModelBytesB64: String = ""
    private var _fragmentShader: String = ""
    private var _vertexShader: String = ""
    private var _runtimeBlockedNotified: Boolean = false
    private var _runtimeSessionStarted: Boolean = false
    private var _runtimeLoadWaiting: Boolean = false
    private var _runtimeLoadPollId: Int = 0
    private var _runtimeSession: dynamic = null
    private var _runtimePass: HolmFullscreenPass? = null
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

    // ADR 0020 · Model Present（WebGL / wasm ModelPass）
    private var _modelMode: Boolean = false
    private var _modelContent: HolmModelContent? = null
    private var _modelRenderer: HolmWebGlModelRenderer? = null
    private var _modelPass: HolmModelPass? = null
    private var _modelPacked: HolmModelMeshUpload.Packed? = null
    private var _lastModelDoc: GltfDocument? = null
    private var _modelYaw: Float = 0.4f
    private var _modelPitch: Float = 0.3f
    private val _modelMvp = FloatArray(16)
    private var _modelRafId: Int = 0
    private var _modelLastTs: Double = 0.0
    private var _modelAccMs: Double = 0.0
    private var _modelPerfWindowMs: Double = 0.0
    private var _modelPerfFrames: Int = 0
    private var _modelPointerDown: Boolean = false
    private var _modelLastPointerX: Float = 0f
    private var _modelLastPointerY: Float = 0f
    private var _modelPointerHandler: ((Event) -> Unit)? = null
    /** none | webgl | webgpu — 切 API 必须换 canvas（浏览器上下文互斥）。 */
    private var _canvasContextKind: String = "none"
    private var _modelLoadGen: Int = 0
    /** 已成功 Present 的资产键（a:path / f:uri）；灯光热更勿整模重载。 */
    private var _modelLoadedSource: String = ""

    internal fun nextModelLoadGen(): Int {
        _modelLoadGen += 1
        return _modelLoadGen
    }

    internal fun modelLoadGen(): Int = _modelLoadGen

    internal fun bumpModelLoadGen() {
        _modelLoadGen += 1
    }

    override val ele: HTMLElement
        get() = _canvas

    override fun setProp(propKey: String, propValue: Any): Boolean {
        return when (propKey) {
            HolmSurfaceAttr.PROP_CONTENT_ID -> {
                val next = propValue.toString()
                if (next != _contentId) {
                    if (_contentId.isNotEmpty()) {
                        HolmContentRegistry.unregister(_contentId)
                    }
                    _contentId = next
                    applyHolmContentFromRegistry()
                }
                true
            }
            HolmSurfaceAttr.PROP_BRIDGE_CONTENT_KIND -> {
                val kind = propValue.toString()
                _bridgeContentKind = kind
                if (kind == HolmSurfaceAttr.CONTENT_KIND_MODEL) {
                    _fragmentShader = ""
                    _vertexShader = ""
                    // Attr 常晚于 onAddToParent：若 Lite Worker 已 Offscreen，立刻拆掉
                    if (_started && !_modelMode) {
                        destroyPump()
                    }
                    // H5 双包：Registry 常 miss，等 modelAssetPath 桥；同堆命中则立刻 Present
                    applyModelFromBridgeOrRegistry()
                }
                true
            }
            HolmSurfaceAttr.PROP_BRIDGE_MODEL_ASSET -> {
                val next = propValue.toString()
                if (next != _bridgeModelAsset) {
                    _bridgeModelAsset = next
                    applyModelFromBridgeOrRegistry()
                }
                true
            }
            HolmSurfaceAttr.PROP_BRIDGE_MODEL_FILE_URI -> {
                val next = propValue.toString()
                if (next != _bridgeModelFileUri) {
                    _bridgeModelFileUri = next
                    applyModelFromBridgeOrRegistry()
                }
                true
            }
            HolmSurfaceAttr.PROP_BRIDGE_MODEL_ORBIT -> {
                _bridgeModelOrbit = propValue.toBool()
                applyModelBridgeOrbitLightsOnly()
                true
            }
            HolmSurfaceAttr.PROP_BRIDGE_MODEL_YAW -> {
                // 仅缓存；已 Present 后轨道角以手势为准，勿每帧用 Content 初值盖掉
                _bridgeModelYaw = propValue.toHolmFloat(0f)
                true
            }
            HolmSurfaceAttr.PROP_BRIDGE_MODEL_PITCH -> {
                _bridgeModelPitch = propValue.toHolmFloat(0f)
                true
            }
            HolmSurfaceAttr.PROP_BRIDGE_MODEL_LIGHTS -> {
                val next = propValue.toString()
                if (next != _bridgeModelLightsRaw) {
                    _bridgeModelLightsRaw = next
                    applyModelBridgeOrbitLightsOnly()
                }
                true
            }
            HolmSurfaceAttr.PROP_BRIDGE_MODEL_SOLID -> {
                val next = propValue.toString()
                if (next != _bridgeModelSolidRaw) {
                    _bridgeModelSolidRaw = next
                    // 同源已 Present：只热更色，勿整模重载（与 lights 同路径）
                    if (_modelMode && _modelPacked != null && _modelLoadedSource.isNotEmpty()) {
                        applyModelBridgeOrbitLightsOnly()
                    } else {
                        applyModelFromBridgeOrRegistry()
                    }
                }
                true
            }
            HolmSurfaceAttr.PROP_BRIDGE_MODEL_BYTES_B64 -> {
                val next = propValue.toString()
                if (next != _bridgeModelBytesB64) {
                    _bridgeModelBytesB64 = next
                    applyModelFromBridgeOrRegistry()
                }
                true
            }
            HolmSurfaceAttr.PROP_BRIDGE_FRAGMENT -> {
                val next = propValue.toString()
                if (next != _fragmentShader) {
                    _fragmentShader = next
                    // Model 岛 / 过渡空片元：禁止再走 Lite wrap → SHADER_WRAP
                    if (isModelContentActiveOrPending()) {
                        // no-op
                    } else if (fragmentMatchesCurrentExecutor(_fragmentShader)) {
                        postScene()
                    } else {
                        fireShaderCompiled(false, 0f, 0f, _executor)
                    }
                }
                true
            }
            HolmSurfaceAttr.PROP_BRIDGE_VERTEX -> {
                val next = propValue.toString()
                if (next != _vertexShader) {
                    _vertexShader = next
                    if (!isModelContentActiveOrPending() && !HolmExecutor.isWgpu(_executor)) {
                        postScene()
                    }
                }
                true
            }
            HolmSurfaceAttr.PROP_PLAYING -> {
                _playing = propValue.toBool()
                if (_modelMode) {
                    postModelPlaying(effectivePlaying())
                } else {
                    postPlaying(effectivePlaying())
                }
                true
            }
            HolmSurfaceAttr.PROP_BRIDGE_WARP -> {
                _warpAmount = propValue.toHolmFloat(HolmSurfaceAttr.DEFAULT_WARP_AMOUNT)
                postSet()
                true
            }
            HolmSurfaceAttr.PROP_BRIDGE_SCROLL -> {
                _scrollSpeed = propValue.toHolmFloat(HolmSurfaceAttr.DEFAULT_SCROLL_SPEED)
                postSet()
                true
            }
            HolmSurfaceAttr.PROP_MAX_FPS -> {
                _maxFps = propValue.toHolmInt(HolmSurfaceAttr.DEFAULT_MAX_FPS).coerceAtLeast(0)
                postSet()
                // 主线程 Runtime：跨 60 切换 rAF/timer 须立刻重调度
                if (HolmExecutor.isWgpu(_executor) && !_runtimeUsesWorker && _runtimeSession != null) {
                    startRuntimeLoop()
                }
                true
            }
            HolmSurfaceAttr.PROP_RENDER_SCALE -> {
                val next = HolmSurfaceAttr.normalizeRenderScale(
                    propValue.toHolmFloat(HolmSurfaceAttr.DEFAULT_RENDER_SCALE),
                )
                if (next != _renderScale) {
                    _renderScale = next
                    // 强制走 postResize（绕过 lastW/H 相等短路）
                    _lastW = 0
                    _lastH = 0
                    val size = readBufferSize()
                    postResize(size.first, size.second)
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
                    if (_modelMode) {
                        val content = _modelContent
                        if (content != null) {
                            enterModelPresentMode(content)
                            _modelPacked?.let { packed ->
                                if (HolmExecutor.isWgpu(_executor)) {
                                    _modelPass?.let { pass ->
                                        pass.setMeshes(packed.verts, packed.indices)
                                        pushModelFrameToPass(pass)
                                    }
                                } else {
                                    _modelRenderer?.setOrbit(_modelYaw, _modelPitch, content.orbitEnabled)
                                    _modelRenderer?.setAppearance(packed.color, content.lights)
                                    _modelRenderer?.uploadPacked(packed)
                                }
                            }
                        } else {
                            applyExecutorMode()
                        }
                    } else {
                        applyExecutorMode()
                    }
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
                // Attr 可能先于 Event 触发 BLOCKED；仅重放创建期 BLOCKED。
                // SHADER_*/TICK 等若每轮 Compose 重绑都 replay，会在成功换档后把旧红字打回（P2-h5-error-sticky-ui）。
                if (_pendingErrorCode == HolmExecutor.ERROR_BLOCKED) {
                    replayPendingError()
                }
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

    private fun applyHolmContentFromRegistry() {
        if (_contentId.isEmpty()) {
            clearModelPresent()
            _fragmentShader = ""
            _vertexShader = ""
            postScene()
            return
        }
        when (val content = HolmContentRegistry.get(_contentId)) {
            null -> {
                // H5：业务包 Registry 与本 View 分堆；Model 走 bridge attrs
                if (_bridgeContentKind == HolmSurfaceAttr.CONTENT_KIND_MODEL) {
                    applyModelFromBridgeOrRegistry()
                } else {
                    clearModelPresent()
                    _fragmentShader = ""
                    _vertexShader = ""
                    postScene()
                }
            }
            is HolmShaderContent -> {
                clearModelPresent()
                _warpAmount = content.warpAmount
                _scrollSpeed = content.scrollSpeed
                _fragmentShader = content.source
                _vertexShader = content.vertexSource
                postSet()
                if (fragmentMatchesCurrentExecutor(_fragmentShader)) {
                    postScene()
                } else {
                    fireShaderCompiled(false, 0f, 0f, _executor)
                }
            }
            is HolmModelContent -> {
                _fragmentShader = ""
                _vertexShader = ""
                // 先标 mode，避免并发 startPump / resolveLite 打出 SHADER_WRAP
                _modelMode = true
                beginModelPresent(content)
            }
        }
    }

    /** Registry 同堆优先；否则用 NVI bridge 重建 HolmModelContent（ADR 0020 H5）。 */
    private fun applyModelFromBridgeOrRegistry() {
        if (_bridgeContentKind != HolmSurfaceAttr.CONTENT_KIND_MODEL) return
        val fromReg = HolmContentRegistry.get(_contentId) as? HolmModelContent
        if (fromReg != null) {
            _modelMode = true
            beginModelPresent(fromReg)
            return
        }
        val asset = _bridgeModelAsset.trim()
        val fileUri = _bridgeModelFileUri.trim()
        val bytesB64 = _bridgeModelBytesB64.trim()
        if (asset.isEmpty() && fileUri.isEmpty() && bytesB64.isEmpty()) {
            return
        }
        val sourceKey = when {
            asset.isNotEmpty() -> "a:$asset"
            fileUri.isNotEmpty() -> "f:$fileUri"
            else -> "b:${bytesB64.length}:${bytesB64.hashCode()}"
        }
        val lights = HolmModelNviCodec.decodeLights(_bridgeModelLightsRaw)
        // 同源且已有网格：只热更灯/色/轨道，禁止整模重载（Unlit/Lit 否则 Tokyo 卡顿）
        if (
            _modelMode &&
            sourceKey.isNotEmpty() &&
            sourceKey == _modelLoadedSource &&
            _modelPacked != null
        ) {
            applyModelBridgeOrbitLightsOnly()
            return
        }
        val color = HolmModelNviCodec.decodeColor(_bridgeModelSolidRaw)
        val content = when {
            asset.isNotEmpty() -> HolmModelContent.fromAssets(
                path = asset,
                solidColorRgba = color,
                lights = lights,
                orbitEnabled = _bridgeModelOrbit,
                yaw = _bridgeModelYaw,
                pitch = _bridgeModelPitch,
            )
            fileUri.isNotEmpty() -> HolmModelContent.fromFileUri(
                uri = fileUri,
                solidColorRgba = color,
                lights = lights,
                orbitEnabled = _bridgeModelOrbit,
                yaw = _bridgeModelYaw,
                pitch = _bridgeModelPitch,
            )
            else -> {
                val decoded = HolmModelNviCodec.decodeBytesB64(bytesB64)
                    ?: return
                HolmModelContent.fromBytes(
                    data = decoded,
                    solidColorRgba = color,
                    lights = lights,
                    orbitEnabled = _bridgeModelOrbit,
                    yaw = _bridgeModelYaw,
                    pitch = _bridgeModelPitch,
                )
            }
        }
        _modelMode = true
        beginModelPresent(content)
    }

    private fun applyModelBridgeOrbitLightsOnly() {
        if (!_modelMode || _bridgeContentKind != HolmSurfaceAttr.CONTENT_KIND_MODEL) {
            applyModelFromBridgeOrRegistry()
            return
        }
        val content = _modelContent ?: run {
            applyModelFromBridgeOrRegistry()
            return
        }
        val lights = HolmModelNviCodec.decodeLights(_bridgeModelLightsRaw)
        content.setLights(lights)
        content.setOrbitEnabled(_bridgeModelOrbit)
        // solid 热更：就地改 packed.color（勿吞进同源 return）
        val packed = _modelPacked
        if (packed != null) {
            applySolidColorToPacked(packed)
        }
        // 保留 View 上拖转后的 _modelYaw/_modelPitch（灯光热更不得重置视角）
        if (!HolmExecutor.isWgpu(_executor)) {
            if (packed != null) {
                _modelRenderer?.setAppearance(packed.color, lights)
            }
            _modelRenderer?.setOrbit(_modelYaw, _modelPitch, content.orbitEnabled)
        } else {
            _modelPass?.let { pushModelFrameToPass(it) }
        }
    }

    private fun applySolidColorToPacked(packed: HolmModelMeshUpload.Packed) {
        val solid = HolmModelNviCodec.decodeColor(_bridgeModelSolidRaw)
        if (solid != null && solid.size >= 4) {
            packed.color[0] = solid[0]
            packed.color[1] = solid[1]
            packed.color[2] = solid[2]
            packed.color[3] = solid[3]
            return
        }
        val factor = _lastModelDoc?.baseColorFactor ?: return
        if (factor.size >= 3) {
            packed.color[0] = factor[0]
            packed.color[1] = factor[1]
            packed.color[2] = factor[2]
            packed.color[3] = factor.getOrElse(3) { 1f }
        }
    }

    private fun clearModelPresent() {
        if (_modelMode || _modelContent != null) {
            HolmJsModelPresenter.detach(this)
        }
        _modelContent = null
        _modelLoadedSource = ""
    }

    private fun beginModelPresent(content: HolmModelContent) {
        _modelContent = content
        // 同岛切资产：须再发 onReady（Demo 会先把文案打回 waiting…）
        _readyFired = false
        HolmJsModelPresenter.attach(this, content)
    }

    override fun onAddToParent(parent: Element) {
        _attached = true
        if (HolmExecutor.isWgpu(_executor)) {
            applyExecutorMode()
        } else if (!_started && !isModelContentActiveOrPending()) {
            // Model 岛禁止 Lite Worker/Offscreen（否则 canvas transfer → WebGL Model 永挂）
            startPump()
        }
        attachObservers()
        if (_modelMode) {
            postModelPlaying(effectivePlaying())
        } else if (!HolmExecutor.isWgpu(_executor)) {
            postPlaying(effectivePlaying())
        }
    }

    /** Attr/Registry/本 View 任一路表明 Model — 禁止 Lite Offscreen。 */
    private fun isModelContentActiveOrPending(): Boolean {
        if (_modelMode || _modelContent != null) return true
        if (_bridgeContentKind == HolmSurfaceAttr.CONTENT_KIND_MODEL) return true
        if (_contentId.isEmpty()) return false
        return HolmContentRegistry.get(_contentId) is HolmModelContent
    }

    override fun onRemoveFromParent(parent: Element) {
        _attached = false
        HolmJsModelPresenter.cancelPending(this)
        if (_contentId.isNotEmpty()) {
            HolmContentRegistry.unregister(_contentId)
            _contentId = ""
        }
        clearWasmLoadPoll()
        detachModelPointer()
        detachObservers()
        destroyModelHost()
        destroyPump()
    }

    private fun startPump() {
        if (_started) return
        if (_modelMode || isModelContentActiveOrPending()) return
        try {
            ensurePumpDispatch()
            // 同实例 remount：上次 transfer 过的 canvas 必须换新才能再 Worker / 主线程 getContext
            if (_controlTransferred) {
                replaceCanvasInPlace()
            }
            val size = readBufferSize()
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
        if (_modelMode) return
        if (HolmExecutor.isWgpu(_executor)) {
            if (_runtimeUsesWorker) {
                if (!_started) return
                val msg = js("{}")
                msg.type = "set"
                msg.maxFps = _maxFps
                dispatch(msg)
                return
            }
            return
        }
        if (!_started) return
        val msg = js("{}")
        msg.type = "set"
        msg.warpAmount = _warpAmount.toDouble()
        msg.scrollSpeed = _scrollSpeed.toDouble()
        msg.maxFps = _maxFps
        dispatch(msg)
    }

    private fun postScene() {
        if (_modelMode) return
        if (HolmExecutor.isWgpu(_executor)) {
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
        // Model 岛 / kind=model 过渡：无片元，禁止 SHADER_WRAP
        if (_modelMode || isModelContentActiveOrPending()) {
            _lastLiteWrapMs = 0f
            return ""
        }
        // 空 scene+空 custom：官方席已删；静默空串（切 Content 过渡噪声，勿红字）
        if (_fragmentShader.isBlank() && _scene.isBlank()) {
            _lastLiteWrapMs = 0f
            return ""
        }
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
            fireShaderCompiled(false, _lastLiteWrapMs, 0f, HolmExecutor.GL)
            ""
        }
    }

    private fun fragmentMatchesCurrentExecutor(src: String): Boolean {
        if (src.isBlank()) return true
        val wgsl = HolmShaderResolve.looksLikeWgslFragment(src)
        return if (HolmExecutor.isWgpu(_executor)) wgsl else !wgsl
    }

    
    private fun applyRuntimeShaders(pass: HolmFullscreenPass) {
        val custom = _fragmentShader.trim()
        pass.setCustomFs("")
        if (custom.isNotEmpty()) {
            if (!HolmShaderResolve.looksLikeWgslFragment(custom)) {
                // ADR 0015 removed: pass.setScene(_scene)
                fireError(
                    "SHADER_CUSTOM",
                    "Runtime fragmentShader must be WGSL (non-WGSL custom ignored)",
                )
                fireShaderCompiled(false, 0f, 0f, HolmExecutor.WGPU)
                return
            }
            val t0 = js("typeof performance !== 'undefined' && performance.now ? performance.now() : Date.now()")
                .unsafeCast<Double>()
            val ok = pass.setCustomFs(custom)
            val t1 = js("typeof performance !== 'undefined' && performance.now ? performance.now() : Date.now()")
                .unsafeCast<Double>()
            val compileMs = (t1 - t0).toFloat()
            flushRuntimePassErrors(pass)
            if (!ok) {
                pass.setPlaying(false)
                _playing = false
                fireError("SHADER_CUSTOM", "Runtime setCustomFs failed")
                fireShaderCompiled(false, 0f, compileMs, HolmExecutor.WGPU)
            } else {
                discardRuntimeSessionErrors(pass.device.handle.asDynamic())
                clearPendingError()
                pass.setPlaying(effectivePlaying())
                if (effectivePlaying() && _runtimeRafId == 0 && _runtimeTimerId == 0) {
                    startRuntimeLoop()
                }
                fireShaderCompiled(true, 0f, compileMs, HolmExecutor.WGPU)
            }
        } else {
            // ADR 0015 removed: pass.setScene(_scene)
            fireShaderCompiled(false, 0f, 0f, HolmExecutor.WGPU)
        }
    }

    private fun flushRuntimePassErrors(pass: HolmFullscreenPass) {
        for ((code, message) in pass.drainErrors()) {
            fireError(code.ifBlank { "WGPU_VALIDATION" }, message)
        }
    }

    private fun applyRuntimeShaders(session: dynamic) {
        val custom = _fragmentShader.trim()
        // 切换样本/轨：先卸上一档，再装新；失败停 ClearTri
        HolmWgpuNative.sessionSetCustomFs(session, "")
        if (custom.isNotEmpty()) {
            if (!HolmShaderResolve.looksLikeWgslFragment(custom)) {
                // 仍持 GLSL 正文时跳过，等 Attr 换成 WGSL（已卸旧 custom）
                // ADR 0015 removed: HolmWgpuNative.sessionSetScene(session, _scene)
                discardRuntimeSessionErrors(session)
                fireShaderCompiled(false, 0f, 0f, HolmExecutor.WGPU)
                return
            }
            val t0 = js("typeof performance !== 'undefined' && performance.now ? performance.now() : Date.now()")
                .unsafeCast<Double>()
            val err = HolmWgpuNative.sessionSetCustomFs(session, custom)
            val t1 = js("typeof performance !== 'undefined' && performance.now ? performance.now() : Date.now()")
                .unsafeCast<Double>()
            val compileMs = (t1 - t0).toFloat()
            if (err != null) {
                // wasm 已 present_clear_only + playing=false；主线程泵对齐停
                val drained = flushRuntimeSessionErrors(session)
                HolmWgpuNative.sessionSetPlaying(session, false)
                _playing = false
                postPlaying(false)
                if (drained == 0) {
                    fireError("SHADER_CUSTOM", err)
                }
                fireShaderCompiled(false, 0f, compileMs, HolmExecutor.WGPU)
            } else {
                // 成功：丢掉上一失败/迟到 uncaptured，禁止再 fireError 盖住新特效（P2-h5-error-sticky-ui）
                discardRuntimeSessionErrors(session)
                clearPendingError()
                HolmWgpuNative.sessionSetPlaying(session, effectivePlaying())
                if (effectivePlaying() && _runtimeRafId == 0 && _runtimeTimerId == 0) {
                    startRuntimeLoop()
                }
                fireShaderCompiled(true, 0f, compileMs, HolmExecutor.WGPU)
            }
        } else {
            // ADR 0015 removed: HolmWgpuNative.sessionSetScene(session, _scene)
            discardRuntimeSessionErrors(session)
            clearPendingError()
            // 空片元 ClearTri：ok=false，对齐 Lite 拒绝语义（审查 P2）
            fireShaderCompiled(false, 0f, 0f, HolmExecutor.WGPU)
        }
    }

    /** 取出 pending 但不投递 onError（成功换档 / 成功 tick 清残留） */
    private fun discardRuntimeSessionErrors(session: dynamic) {
        if (session == null) return
        HolmWgpuNative.sessionDrainErrors(session)
    }

    /** @return 已投递条数 */
    private fun flushRuntimeSessionErrors(session: dynamic): Int {
        if (session == null) return 0
        val errs = HolmWgpuNative.sessionDrainErrors(session)
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
        if (_modelMode) {
            _canvas.width = w.coerceAtLeast(1)
            _canvas.height = h.coerceAtLeast(1)
            if (HolmExecutor.isWgpu(_executor)) {
                _modelPass?.resize(w.coerceAtLeast(1), h.coerceAtLeast(1))
            } else {
                _modelRenderer?.resize(w.coerceAtLeast(1), h.coerceAtLeast(1))
            }
            return
        }
        if (HolmExecutor.isWgpu(_executor)) {
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
            HolmWgpuNative.sessionResize(session, w.coerceAtLeast(1), h.coerceAtLeast(1))
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
        if (HolmExecutor.isWgpu(_executor)) {
            if (_runtimeUsesWorker) {
                if (!_started) return
                val msg = js("{}")
                msg.type = "playing"
                msg.playing = playing
                dispatch(msg)
                return
            }
            val session = _runtimeSession ?: return
            HolmWgpuNative.sessionSetPlaying(session, playing)
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
                if (_runtimeUsesWorker && HolmExecutor.isWgpu(_executor) &&
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
                val size = readBufferSize()
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
                if (_modelMode) {
                    postModelPlaying(effectivePlaying())
                } else {
                    postPlaying(effectivePlaying())
                }
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
        val pass = _runtimePass
        _runtimePass = null
        val session = _runtimeSession
        _runtimeSession = null
        _runtimeSessionStarted = false
        _runtimeUsesWorker = false
        _runtimeMainFallbackTried = false
        if (pass != null) {
            pass.destroy()
        } else if (session != null) {
            HolmWgpuNative.sessionDestroy(session)
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
        if (!_attached || !HolmExecutor.isWgpu(_executor)) {
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
            if (!HolmExecutor.isWgpu(_executor) || _runtimeSession == null || _runtimeUsesWorker) {
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
                val tickErr = HolmWgpuNative.sessionTick(session, dtSecs)
                if (tickErr != null) {
                    val drained = flushRuntimeSessionErrors(session)
                    _backend = HolmExecutor.BACKEND_WGPU_BLOCKED
                    if (!_runtimeBlockedNotified) {
                        _runtimeBlockedNotified = true
                        if (drained == 0) {
                            fireError(
                                "TICK",
                                tickErr.ifBlank { "kmp-wgpu-native tick failed" },
                            )
                        }
                    }
                    stopRuntimeLoop()
                    return@frame
                }
                // 成功帧：丢弃迟到 uncaptured，勿把上一档 SHADER_* 再打到业务
                discardRuntimeSessionErrors(session)
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
        val script = HolmWgpuPumpSource.SCRIPT
        val glueUrl = js("new URL('kmp-wgpu-native/kmp_wgpu_native.js', location.href).href")
            .unsafeCast<String>()
        val wasmUrl = js("new URL('kmp-wgpu-native/kmp_wgpu_native_bg.wasm', location.href).href")
            .unsafeCast<String>()
        val init = js("{}")
        init.type = "init"
        init.glueUrl = glueUrl
        init.wasmUrl = wasmUrl
        init.scene = _scene
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

    /** CSS 布局尺寸 × renderScale → canvas backing（基线=CSS px，不乘 DPR）。 */
    private fun readBufferSize(): Pair<Int, Int> {
        val cssW = _canvas.clientWidth.coerceAtLeast(1)
        val cssH = _canvas.clientHeight.coerceAtLeast(1)
        val scale = HolmSurfaceAttr.normalizeRenderScale(_renderScale)
        val w = kotlin.math.floor(cssW * scale).toInt().coerceAtLeast(1)
        val h = kotlin.math.floor(cssH * scale).toInt().coerceAtLeast(1)
        return w to h
    }

    private fun applyExecutorMode() {
        if (_modelMode) {
            // Model 切轨由 enterModelPresentMode 承接，勿走 Shader Lite/Fullscreen
            return
        }
        HolmUmbrellaBootstrap.ensure()
        try {
            HolmProviders.requireForExecutor(_executor)
        } catch (e: IllegalStateException) {
            _backend = HolmExecutor.BACKEND_WGPU_BLOCKED
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
        if (HolmExecutor.isWgpu(_executor)) {
            if (_started) {
                destroyPump()
            }
            runCatching {
                _canvas.style.setProperty("background", "#1E2A4A")
            }
            if (!HolmWgpuNative.isAvailable()) {
                if (HolmWgpuNative.isLoadPending()) {
                    if (!_runtimeLoadWaiting) {
                        _runtimeLoadWaiting = true
                        // wasm 仍在 init：短轮询，避免首屏永久 BLOCKED；保存 id 便于卸岛取消
                        _runtimeLoadPollId = js(
                            """
                            (function(retry) {
                              var n = 0;
                              var t = setInterval(function() {
                                n++;
                                if (window.__kmpWgpu && window.__kmpWgpu.ready === true) {
                                  clearInterval(t); retry(t);
                                } else if (window.__kmpWgpu && window.__kmpWgpu.error) {
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
                            if (_attached && HolmExecutor.isWgpu(_executor)) {
                                applyExecutorMode()
                            }
                        })
                    }
                    return
                }
                _backend = HolmExecutor.BACKEND_WGPU_BLOCKED
                if (!_runtimeBlockedNotified) {
                    _runtimeBlockedNotified = true
                    fireError(HolmExecutor.ERROR_BLOCKED, HolmWgpuNative.blockedMessage())
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
            val size = readBufferSize()
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
        if (!_attached || !HolmExecutor.isWgpu(_executor)) {
            _runtimeSessionStarted = false
            return
        }
        _runtimeSessionStarted = true
        val size = readBufferSize()
        _lastW = size.first
        _lastH = size.second
        HolmWgpuNative.createFullscreenPass(
            canvas = _canvas,
            width = size.first,
            height = size.second,
            onSuccess = { pass ->
                if (!_attached || !HolmExecutor.isWgpu(_executor)) {
                    pass.destroy()
                    return@createFullscreenPass
                }
                _runtimePass = pass
                _runtimeSession = pass.device.handle.asDynamic()
                _runtimeUsesWorker = false
                val backend = pass.backendId()
                _backend = if (backend.isNotBlank()) backend else HolmExecutor.BACKEND_WGPU_M0
                runCatching { _canvas.style.removeProperty("background") }
                applyRuntimeShaders(pass)
                pass.setPlaying(effectivePlaying())
                clearPendingError()
                _readyFired = true
                fireReady()
                startRuntimeLoop()
            },
            onError = { message ->
                if (!_attached || !HolmExecutor.isWgpu(_executor)) return@createFullscreenPass
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
        // Runtime / Model 路径以 present 结果写入的 `_backend` 为准（失败不得假报 wgpu-m0）
        val backend = when {
            _modelMode && HolmExecutor.isWgpu(_executor) ->
                _backend.ifBlank { HolmExecutor.BACKEND_WGPU_M0 }
            _modelMode -> "webgl-model"
            else -> _backend
        }
        if (_modelMode && !HolmExecutor.isWgpu(_executor)) {
            _backend = "webgl-model"
        }
        _onReady?.invoke(
            mapOf(
                "backend" to backend,
                "quality" to "auto",
                "scene" to _scene,
                "executor" to _executor,
            ),
        )
    }

    // region ADR 0020 Model Present

    internal fun enterModelPresentMode(content: HolmModelContent) {
        val alreadyModel = _modelMode
        val stayOnWgpu = alreadyModel && HolmExecutor.isWgpu(_executor) && _modelPass != null
        val stayOnWebGl = alreadyModel && !HolmExecutor.isWgpu(_executor) && _modelRenderer != null
        _modelContent = content
        _modelMode = true
        if (!alreadyModel) {
            _readyFired = false
            _modelYaw = content.yaw
            _modelPitch = content.pitch.coerceIn(-1.2f, 1.2f)
        }
        HolmUmbrellaBootstrap.ensure()
        try {
            HolmProviders.requireForExecutor(_executor)
        } catch (e: IllegalStateException) {
            _backend = HolmExecutor.BACKEND_WGPU_BLOCKED
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
        if (HolmExecutor.isWgpu(_executor)) {
            if (stayOnWgpu) return
            destroyModelWebGl()
            destroyPump()
            // P0：WebGL↔WebGPU / Shader→Model 须换元，禁止复用已 branding 的 canvas
            resetCanvasForModelApi("webgpu")
            attachModelPointer()
            if (!HolmWgpuNative.isAvailable()) {
                if (HolmWgpuNative.isLoadPending()) {
                    // 等 wasm ready 后再进 ModelPass
                    if (!_runtimeLoadWaiting) {
                        _runtimeLoadWaiting = true
                        _runtimeLoadPollId = js(
                            """
                            (function(retry) {
                              var n = 0;
                              var t = setInterval(function() {
                                n++;
                                if (window.__kmpWgpu && window.__kmpWgpu.ready === true) {
                                  clearInterval(t); retry(t);
                                } else if (window.__kmpWgpu && window.__kmpWgpu.error) {
                                  clearInterval(t); retry(t);
                                } else if (n >= 40) {
                                  clearInterval(t); retry(t);
                                }
                              }, 50);
                              return t;
                            })
                            """,
                        ).unsafeCast<(((Int) -> Unit) -> Int)>()({ id ->
                            if (_runtimeLoadPollId == id) _runtimeLoadPollId = 0
                            _runtimeLoadWaiting = false
                            if (_attached && _modelMode && HolmExecutor.isWgpu(_executor)) {
                                enterModelPresentMode(content)
                            }
                        })
                    }
                    return
                }
                _backend = HolmExecutor.BACKEND_WGPU_BLOCKED
                if (!_runtimeBlockedNotified) {
                    _runtimeBlockedNotified = true
                    fireError(HolmExecutor.ERROR_BLOCKED, HolmWgpuNative.blockedMessage())
                }
                if (!_readyFired) {
                    _readyFired = true
                    fireReady()
                }
                return
            }
            startModelWgpuPass()
        } else {
            if (stayOnWebGl) {
                val color = content.solidColorRgba ?: floatArrayOf(0.85f, 0.55f, 0.25f, 1f)
                _modelRenderer?.setAppearance(color, content.lights)
                _modelRenderer?.setOrbit(_modelYaw, _modelPitch, content.orbitEnabled)
                return
            }
            destroyModelWgpu()
            destroyPump()
            resetCanvasForModelApi("webgl")
            attachModelPointer()
            startModelWebGl()
        }
    }

    /**
     * 浏览器：同一 canvas 不能在 WebGL / WebGPU 间切换上下文。
     * 非 stay 路径一律换元（含 Shader Lite/Runtime 残留 branding）；重绑 RO + pointer。
     */
    private fun resetCanvasForModelApi(@Suppress("UNUSED_PARAMETER") api: String) {
        detachModelPointer()
        replaceCanvasInPlace()
        _canvasContextKind = "none"
        if (_attached) {
            attachObservers()
        }
    }

    internal fun onModelDocumentReady(content: HolmModelContent, doc: GltfDocument) {
        if (!_modelMode) return
        _modelContent = content
        _lastModelDoc = doc
        val packed = HolmModelMeshUpload.pack(doc, content.solidColorRgba)
        _modelPacked = packed
        val asset = content.assetPath?.trim().orEmpty()
        val fileUri = content.fileUri?.trim().orEmpty()
        val bytesB64 = _bridgeModelBytesB64.trim()
        val contentBytes = content.bytes
        _modelLoadedSource = when {
            asset.isNotEmpty() -> "a:$asset"
            fileUri.isNotEmpty() -> "f:$fileUri"
            contentBytes != null && bytesB64.isNotEmpty() ->
                "b:${bytesB64.length}:${bytesB64.hashCode()}"
            contentBytes != null -> "b:${contentBytes.size}"
            else -> ""
        }
        if (HolmExecutor.isWgpu(_executor)) {
            val pass = _modelPass
            if (pass != null) {
                pass.setMeshes(packed.verts, packed.indices)
                pushModelFrameToPass(pass)
                _readyFired = true
                fireReady()
            }
        } else {
            _modelRenderer?.setAppearance(packed.color, content.lights)
            _modelRenderer?.setOrbit(_modelYaw, _modelPitch, content.orbitEnabled)
            _modelRenderer?.uploadPacked(packed)
            // 切资产后 Demo 会先把文案打回 waiting…；同岛热换也必须再发 onReady
            _readyFired = true
            fireReady()
        }
    }

    internal fun onModelLoadFailed(code: String, message: String) {
        fireError(code, message)
        _modelPacked = null
        _lastModelDoc = null
        if (HolmExecutor.isWgpu(_executor)) {
            _modelPass?.setMeshes(FloatArray(0), IntArray(0))
        } else {
            _modelRenderer?.clearGpu()
        }
        // P2：失败也 ready，避免 Demo 永停 waiting…
        if (!_readyFired) {
            _readyFired = true
            fireReady()
        }
    }

    internal fun exitModelPresentMode() {
        _modelMode = false
        _modelContent = null
        _modelPacked = null
        _lastModelDoc = null
        _modelLoadedSource = ""
        detachModelPointer()
        destroyModelHost()
        // 回 Shader 泵：WebGPU branding 或残留 Offscreen 都须换元
        if (_canvasContextKind == "webgpu" || canvasControlTransferred()) {
            replaceCanvasInPlace()
            _canvasContextKind = "none"
            if (_attached) attachObservers()
        }
        if (_attached && _bridgeContentKind != HolmSurfaceAttr.CONTENT_KIND_MODEL) {
            applyExecutorMode()
        }
    }

    private fun startModelWebGl() {
        destroyModelWebGl()
        // 若仍被 Lite Worker Offscreen 占着，再换一次元
        if (canvasControlTransferred()) {
            detachModelPointer()
            replaceCanvasInPlace()
            _canvasContextKind = "none"
            if (_attached) attachObservers()
            attachModelPointer()
        }
        val size = readBufferSize()
        _lastW = size.first
        _lastH = size.second
        _canvas.width = size.first
        _canvas.height = size.second
        val renderer = HolmWebGlModelRenderer()
        renderer.onReady = {
            _backend = "webgl-model"
            clearPendingError()
            if (!_readyFired && _modelPacked != null) {
                _readyFired = true
                fireReady()
            }
        }
        renderer.onError = { code, message ->
            fireError(code, message)
            if (!_readyFired) {
                _readyFired = true
                fireReady()
            }
        }
        renderer.onPerf = { fps, frameMs -> firePerf(fps, frameMs) }
        renderer.lightsProvider = { _modelContent?.lights.orEmpty() }
        _modelRenderer = renderer
        val color = _modelContent?.solidColorRgba ?: floatArrayOf(0.85f, 0.55f, 0.25f, 1f)
        renderer.setAppearance(color, _modelContent?.lights.orEmpty())
        renderer.setOrbit(_modelYaw, _modelPitch, _modelContent?.orbitEnabled ?: true)
        renderer.setPlaying(effectivePlaying())
        var ok = renderer.attach(_canvas, size.first, size.second)
        if (!ok && canvasControlTransferred()) {
            // attach 失败且仍 Offscreen：换元重试一次
            detachModelPointer()
            replaceCanvasInPlace()
            _canvasContextKind = "none"
            if (_attached) attachObservers()
            attachModelPointer()
            _canvas.width = size.first
            _canvas.height = size.second
            ok = renderer.attach(_canvas, size.first, size.second)
        }
        if (!ok) {
            _modelRenderer = null
            if (!_readyFired) {
                _readyFired = true
                fireReady()
            }
            return
        }
        _canvasContextKind = "webgl"
        _modelPacked?.let { renderer.uploadPacked(it) }
        _started = true
        _backend = "webgl-model"
        clearPendingError()
    }

    /** canvas 是否已 transferControlToOffscreen（getContext 会抛）。 */
    private fun canvasControlTransferred(): Boolean {
        return try {
            js(
                """
                (function(c) {
                  try {
                    c.getContext('webgl');
                    return false;
                  } catch (e) {
                    var m = (e && e.message) ? String(e.message) : String(e);
                    return /offscreen/i.test(m);
                  }
                })
                """,
            ).unsafeCast<(HTMLCanvasElement) -> Boolean>()(_canvas)
        } catch (_: Throwable) {
            false
        }
    }

    private fun startModelWgpuPass() {
        destroyModelWgpu()
        val size = readBufferSize()
        _lastW = size.first
        _lastH = size.second
        HolmWgpuNative.createModelPass(
            canvas = _canvas,
            width = size.first,
            height = size.second,
            onSuccess = { pass ->
                if (!_attached || !_modelMode || !HolmExecutor.isWgpu(_executor)) {
                    pass.destroy()
                    return@createModelPass
                }
                _modelPass = pass
                _canvasContextKind = "webgpu"
                val backend = pass.backendId()
                _backend = if (backend.isNotBlank()) backend else HolmExecutor.BACKEND_WGPU_M0
                _modelPacked?.let { packed ->
                    pass.setMeshes(packed.verts, packed.indices)
                    pushModelFrameToPass(pass)
                }
                pass.setPlaying(effectivePlaying())
                clearPendingError()
                _started = true
                if (!_readyFired && _modelPacked != null) {
                    _readyFired = true
                    fireReady()
                }
                startModelWgpuLoop()
            },
            onError = { message ->
                if (!_attached || !_modelMode) return@createModelPass
                _backend = HolmExecutor.BACKEND_WGPU_BLOCKED
                if (!_runtimeBlockedNotified) {
                    _runtimeBlockedNotified = true
                    fireError(HolmExecutor.ERROR_BLOCKED, message)
                }
                if (!_readyFired) {
                    _readyFired = true
                    fireReady()
                }
            },
        )
    }

    private fun pushModelFrameToPass(pass: HolmModelPass) {
        val packed = _modelPacked ?: return
        val content = _modelContent
        val aspect = if (_lastH > 0) _lastW.toFloat() / _lastH.toFloat() else 1f
        HolmModelMeshUpload.mvp(
            _modelMvp,
            packed.center,
            packed.radius,
            _modelYaw,
            _modelPitch,
            aspect,
        )
        val lights = content?.lights.orEmpty()
        val lit = lights.isNotEmpty()
        val lightDir = if (lit) {
            floatArrayOf(lights[0].directionX, lights[0].directionY, lights[0].directionZ)
        } else {
            floatArrayOf(0.3f, 0.8f, 0.4f)
        }
        val intensity = if (lit) lights[0].intensity.coerceAtLeast(0f) else 1f
        val color = if (lit && intensity != 1f) {
            floatArrayOf(
                packed.color[0] * intensity,
                packed.color[1] * intensity,
                packed.color[2] * intensity,
                packed.color[3],
            )
        } else {
            packed.color
        }
        pass.setFrame(_modelMvp, color, lit, lightDir)
    }

    private fun startModelWgpuLoop() {
        stopModelWgpuLoop()
        if (!effectivePlaying() || _modelPass == null) return
        lateinit var onFrame: (Double) -> Unit
        lateinit var scheduleNext: (Double) -> Unit
        onFrame = frame@{ now ->
            _modelRafId = 0
            if (!_modelMode || !HolmExecutor.isWgpu(_executor) || _modelPass == null) return@frame
            if (!effectivePlaying()) return@frame
            // 对齐 WebGL Model：每 rAF 一帧。勿用 maxFps=60 累加器（浏览器 rAF 常 <16.67ms → 半帧≈30）
            if (_modelLastTs == 0.0) _modelLastTs = now
            val dtMs = (now - _modelLastTs).coerceIn(0.0, 50.0)
            _modelLastTs = now
            if (_maxFps in 1..59) {
                _modelAccMs += dtMs
                val minFrameMs = 1000.0 / _maxFps.toDouble()
                if (_modelAccMs < minFrameMs) {
                    scheduleNext(now)
                    return@frame
                }
                // 保留余量，避免清零导致长期偏慢（抖动时少丢节拍）
                _modelAccMs -= minFrameMs
                if (_modelAccMs > minFrameMs * 2.0) {
                    _modelAccMs = 0.0
                }
            } else {
                _modelAccMs = 0.0
            }
            val dtSecs = (dtMs / 1000.0).toFloat().coerceAtLeast(1f / 120f)
            val pass = _modelPass ?: return@frame
            pushModelFrameToPass(pass)
            val ok = pass.tick(dtSecs)
            if (!ok) {
                val drained = flushModelErrors(pass)
                _backend = HolmExecutor.BACKEND_WGPU_BLOCKED
                if (!_runtimeBlockedNotified) {
                    _runtimeBlockedNotified = true
                    if (drained == 0) {
                        fireError(
                            pass.lastErrorCode().ifBlank { "TICK" },
                            pass.lastErrorMessage().ifBlank { "ModelPass tick failed" },
                        )
                    }
                }
                stopModelWgpuLoop()
                return@frame
            }
            flushModelErrors(pass)
            if (_onPerf != null) {
                if (_modelPerfWindowMs == 0.0) {
                    _modelPerfWindowMs = now
                    _modelPerfFrames = 0
                } else {
                    _modelPerfFrames += 1
                    val elapsed = now - _modelPerfWindowMs
                    if (elapsed >= 1000.0) {
                        val fps = (_modelPerfFrames * 1000.0 / elapsed).toFloat()
                        val frameMs = if (fps > 0.01f) 1000f / fps else 0f
                        firePerf(fps, frameMs)
                        _modelPerfWindowMs = now
                        _modelPerfFrames = 0
                    }
                }
            }
            scheduleNext(now)
        }
        scheduleNext = sched@{ _ ->
            if (!effectivePlaying() || _modelPass == null) return@sched
            _modelRafId = js("requestAnimationFrame").unsafeCast<((Double) -> Unit) -> Int>()(onFrame)
        }
        scheduleNext(js("performance.now()").unsafeCast<Double>())
    }

    private fun stopModelWgpuLoop() {
        if (_modelRafId != 0) {
            js("cancelAnimationFrame").unsafeCast<(Int) -> Unit>()(_modelRafId)
            _modelRafId = 0
        }
        _modelLastTs = 0.0
        _modelAccMs = 0.0
        _modelPerfWindowMs = 0.0
        _modelPerfFrames = 0
    }

    private fun flushModelErrors(pass: HolmModelPass): Int {
        val errs = pass.drainErrors()
        if (errs.isEmpty()) return 0
        for ((code, message) in errs) {
            fireError(code.ifBlank { "WGPU_VALIDATION" }, message)
        }
        return errs.size
    }

    private fun postModelPlaying(playing: Boolean) {
        if (HolmExecutor.isWgpu(_executor)) {
            _modelPass?.setPlaying(playing)
            if (playing) startModelWgpuLoop() else stopModelWgpuLoop()
        } else {
            _modelRenderer?.setPlaying(playing)
        }
    }

    private fun destroyModelHost() {
        destroyModelWebGl()
        destroyModelWgpu()
    }

    private fun destroyModelWebGl() {
        val renderer = _modelRenderer
        _modelRenderer = null
        renderer?.destroy()
    }

    private fun destroyModelWgpu() {
        stopModelWgpuLoop()
        val pass = _modelPass
        _modelPass = null
        pass?.destroy()
    }

    private fun attachModelPointer() {
        // 换 canvas 后必须重绑；旧 handler 已在 detachModelPointer 清掉
        if (_modelPointerHandler != null) return
        val handler: (Event) -> Unit = handler@{ ev ->
            if (!_modelMode) return@handler
            val content = _modelContent
            if (content == null || !content.orbitEnabled) return@handler
            val type = ev.type
            when (type) {
                "pointerdown" -> {
                    val pe = ev.asDynamic()
                    _modelPointerDown = true
                    _modelLastPointerX = (pe.clientX as Number).toFloat()
                    _modelLastPointerY = (pe.clientY as Number).toFloat()
                    try {
                        _canvas.asDynamic().setPointerCapture(pe.pointerId)
                    } catch (_: Throwable) {
                        // ignore
                    }
                    ev.preventDefault()
                }
                "pointermove" -> {
                    if (!_modelPointerDown) return@handler
                    val pe = ev.asDynamic()
                    val x = (pe.clientX as Number).toFloat()
                    val y = (pe.clientY as Number).toFloat()
                    val dx = x - _modelLastPointerX
                    val dy = y - _modelLastPointerY
                    _modelLastPointerX = x
                    _modelLastPointerY = y
                    _modelYaw -= dx * 0.01f
                    _modelPitch = (_modelPitch + dy * 0.01f).coerceIn(-1.2f, 1.2f)
                    content.setYaw(_modelYaw)
                    content.setPitch(_modelPitch)
                    if (!HolmExecutor.isWgpu(_executor)) {
                        _modelRenderer?.nudgeOrbit(-dx, dy)
                    }
                    ev.preventDefault()
                }
                "pointerup", "pointercancel" -> {
                    _modelPointerDown = false
                    val pe = ev.asDynamic()
                    try {
                        _canvas.asDynamic().releasePointerCapture(pe.pointerId)
                    } catch (_: Throwable) {
                        // ignore
                    }
                }
            }
        }
        _modelPointerHandler = handler
        _canvas.addEventListener("pointerdown", handler)
        _canvas.addEventListener("pointermove", handler)
        _canvas.addEventListener("pointerup", handler)
        _canvas.addEventListener("pointercancel", handler)
        _canvas.style.asDynamic().touchAction = "none"
    }

    private fun detachModelPointer() {
        val handler = _modelPointerHandler ?: return
        _canvas.removeEventListener("pointerdown", handler)
        _canvas.removeEventListener("pointermove", handler)
        _canvas.removeEventListener("pointerup", handler)
        _canvas.removeEventListener("pointercancel", handler)
        _modelPointerHandler = null
        _modelPointerDown = false
        runCatching { _canvas.style.asDynamic().touchAction = "" }
    }

    // endregion

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
