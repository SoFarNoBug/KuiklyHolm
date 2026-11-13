package com.jlj.kuiklybase.holm

import android.content.Context
import android.os.Build
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.view.Surface
import android.view.SurfaceHolder
import com.tencent.kuikly.core.render.android.expand.component.KRView
import com.tencent.kuikly.core.render.android.export.KuiklyRenderCallback
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import org.json.JSONObject

/**
 * @CreateDate: 2026/11/12
 * @Author: jlj
 * @Description: Android KRHolmSurface — GLES2 执行器；按 scene 热换同源 shader（策略 A）。
 */
class KRHolmSurfaceView(context: Context) : KRView(context) {

    private val _glView = GLSurfaceView(context)
    private val _renderer = HolmGlesRenderer()
    private var _playing: Boolean = true
    private var _scene: String = HolmSceneShaders.SCENE_FROST
    private var _liquidBoost: Float = HolmSurfaceAttr.DEFAULT_LIQUID_BOOST
    private var _liquidBend: Float = HolmSurfaceAttr.DEFAULT_LIQUID_BEND
    private var _warpAmount: Float = HolmSurfaceAttr.DEFAULT_WARP_AMOUNT
    private var _scrollSpeed: Float = HolmSurfaceAttr.DEFAULT_SCROLL_SPEED
    private var _maxFps: Int = HolmSurfaceAttr.DEFAULT_MAX_FPS
    private var _executor: String = HolmSurfaceAttr.DEFAULT_EXECUTOR
    private var _fragmentShader: String = ""
    private var _vertexShader: String = ""
    private var _runtimeBlockedNotified: Boolean = false
    private var _pendingErrorCode: String? = null
    private var _pendingErrorMessage: String? = null
    private var _onReady: KuiklyRenderCallback? = null
    private var _onError: KuiklyRenderCallback? = null
    private var _onPerf: KuiklyRenderCallback? = null
    private var _readyFired: Boolean = false
    private var _surfaceAlive: Boolean = false
    private var _appliedSurfaceFps: Float = -1f

    private val _surfaceCallback = object : SurfaceHolder.Callback {
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

    init {
        _glView.setEGLContextClientVersion(2)
        _glView.setRenderer(_renderer)
        _glView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        _glView.holder.addCallback(_surfaceCallback)
        addView(
            _glView,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT),
        )
        _renderer.onReady = {
            post {
                if (!_readyFired) {
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
    }

    override fun setProp(propKey: String, propValue: Any): Boolean {
        return when (propKey) {
            HolmSurfaceAttr.PROP_SCENE -> {
                val next = propValue.toString()
                if (next != _scene) {
                    _scene = next
                    _glView.queueEvent {
                        _renderer.setScene(next, _fragmentShader, _vertexShader)
                    }
                }
                true
            }
            HolmSurfaceAttr.PROP_FRAGMENT_SHADER -> {
                val next = propValue.toString()
                if (next != _fragmentShader) {
                    _fragmentShader = next
                    _glView.queueEvent {
                        _renderer.setShaders(_scene, _fragmentShader, _vertexShader)
                    }
                }
                true
            }
            HolmSurfaceAttr.PROP_VERTEX_SHADER -> {
                val next = propValue.toString()
                if (next != _vertexShader) {
                    _vertexShader = next
                    _glView.queueEvent {
                        _renderer.setShaders(_scene, _fragmentShader, _vertexShader)
                    }
                }
                true
            }
            HolmSurfaceAttr.PROP_PLAYING -> {
                _playing = propValue.toBool()
                applyPlayingRenderMode()
                true
            }
            HolmSurfaceAttr.PROP_LIQUID_BOOST -> {
                val next = propValue.toHolmFloat(HolmSurfaceAttr.DEFAULT_LIQUID_BOOST)
                if (next != _liquidBoost) {
                    _liquidBoost = next
                    _glView.queueEvent {
                        _renderer.setLiquidBoost(next)
                    }
                }
                true
            }
            HolmSurfaceAttr.PROP_LIQUID_BEND -> {
                val next = propValue.toHolmFloat(HolmSurfaceAttr.DEFAULT_LIQUID_BEND)
                if (next != _liquidBend) {
                    _liquidBend = next
                    _glView.queueEvent {
                        _renderer.setLiquidBend(next)
                    }
                }
                true
            }
            HolmSurfaceAttr.PROP_WARP_AMOUNT -> {
                val next = propValue.toHolmFloat(HolmSurfaceAttr.DEFAULT_WARP_AMOUNT)
                if (next != _warpAmount) {
                    _warpAmount = next
                    _glView.queueEvent {
                        _renderer.setWarpAmount(next)
                    }
                }
                true
            }
            HolmSurfaceAttr.PROP_SCROLL_SPEED -> {
                val next = propValue.toHolmFloat(HolmSurfaceAttr.DEFAULT_SCROLL_SPEED)
                if (next != _scrollSpeed) {
                    _scrollSpeed = next
                    _glView.queueEvent {
                        _renderer.setScrollSpeed(next)
                    }
                }
                true
            }
            HolmSurfaceAttr.PROP_MAX_FPS -> {
                val next = propValue.toHolmInt(HolmSurfaceAttr.DEFAULT_MAX_FPS).coerceAtLeast(0)
                if (next != _maxFps) {
                    _maxFps = next
                    // 见 applySurfaceFrameRate KDoc：此处不跳画限帧（会闪），只转发系统帧率提示。
                    applySurfaceFrameRate()
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
                // Attr 可能先于 Event 触发 BLOCKED；与 onReady 对称重放
                replayPendingError()
                true
            }
            HolmSurfaceEvent.ON_PERF -> {
                _onPerf = propValue as? KuiklyRenderCallback
                true
            }
            else -> super.setProp(propKey, propValue)
        }
    }

    override fun onDetachedFromWindow() {
        // GL 删除必须在 GL 线程；先 queueEvent 再 onPause
        _glView.queueEvent {
            _renderer.release()
        }
        _glView.onPause()
        super.onDetachedFromWindow()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        _glView.onResume()
        applySurfaceFrameRate()
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

    private fun applyPlayingRenderMode() {
        if (HolmExecutor.isRuntime(_executor)) {
            // runtime 旁路不走 Lite GLES 连续泵，避免阻塞态仍 CONTINUOUSLY
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
        if (HolmExecutor.isRuntime(_executor)) {
            val available = HolmRuntimeNative.isAvailable()
            _glView.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
            _glView.visibility = if (available) VISIBLE else INVISIBLE
            if (!available) {
                setBackgroundColor(0xFF1E2A4A.toInt())
                if (!_runtimeBlockedNotified) {
                    _runtimeBlockedNotified = true
                    fireError(HolmExecutor.ERROR_BLOCKED, HolmRuntimeNative.blockedMessage())
                }
                if (!_readyFired) {
                    _readyFired = true
                    fireReady()
                }
            } else {
                background = null
                _glView.visibility = VISIBLE
                clearPendingError()
                if (!_readyFired) {
                    _readyFired = true
                    fireReady()
                }
            }
        } else {
            background = null
            _glView.visibility = VISIBLE
            clearPendingError()
            applyPlayingRenderMode()
        }
    }

    private fun fireReady() {
        val backend = when {
            HolmExecutor.isRuntime(_executor) && HolmRuntimeNative.isAvailable() ->
                HolmExecutor.BACKEND_WGPU_M0
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

    companion object {
        const val VIEW_NAME: String = HolmSurfaceView.VIEW_NAME
        private const val UNLIMITED_SURFACE_FPS: Float = 120f
    }
}

private class HolmGlesRenderer : GLSurfaceView.Renderer {
    var onReady: (() -> Unit)? = null
    var onError: ((String, String) -> Unit)? = null
    var onPerf: ((Float, Float) -> Unit)? = null

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
    private var _scene: String = HolmSceneShaders.SCENE_FROST
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
            // 失败且无成功程序：深蓝底（Demo 红字叠在岛上）
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

    private fun rebuildProgram() {
        val vertSrc = HolmShaderResolve.vertex(_vertexSrc)
        val fragSrc = try {
            HolmShaderResolve.fragment(_scene, _fragmentBody)
        } catch (t: Throwable) {
            _shaderFailed = true
            onError?.invoke("SHADER_WRAP", t.message ?: "wrap failed")
            return
        }
        val vert = compile(GLES20.GL_VERTEX_SHADER, vertSrc)
        val frag = compile(GLES20.GL_FRAGMENT_SHADER, fragSrc)
        if (vert == 0 || frag == 0) {
            _shaderFailed = true
            // 保留上一成功 program；首次失败则 _program 仍为 0 → 深蓝底
            return
        }
        val program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vert)
        GLES20.glAttachShader(program, frag)
        GLES20.glLinkProgram(program)
        val link = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, link, 0)
        if (link[0] == 0) {
            _shaderFailed = true
            onError?.invoke("SHADER_LINK", GLES20.glGetProgramInfoLog(program) ?: "link failed")
            GLES20.glDeleteProgram(program)
            return
        }
        if (_program != 0) {
            GLES20.glDeleteProgram(_program)
        }
        _program = program
        _shaderFailed = false
        _posLoc = GLES20.glGetAttribLocation(_program, "a_position")
        _timeLoc = GLES20.glGetUniformLocation(_program, "u_time")
        _resolutionLoc = GLES20.glGetUniformLocation(_program, "u_resolution")
        _liquidBoostLoc = GLES20.glGetUniformLocation(_program, "u_liquidBoost")
        _liquidBendLoc = GLES20.glGetUniformLocation(_program, "u_liquidBend")
        _warpAmountLoc = GLES20.glGetUniformLocation(_program, "u_warpAmount")
        _scrollSpeedLoc = GLES20.glGetUniformLocation(_program, "u_scrollSpeed")
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
        /** 单帧动画步进上限，避免掉帧时墙钟猛跳造成拖影感 */
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
