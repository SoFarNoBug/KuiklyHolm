package com.jlj.kuiklybase.holm

import org.khronos.webgl.Float32Array
import org.khronos.webgl.Uint32Array
import org.khronos.webgl.WebGLBuffer
import org.khronos.webgl.WebGLProgram
import org.khronos.webgl.WebGLRenderingContext
import org.khronos.webgl.WebGLUniformLocation
import org.w3c.dom.HTMLCanvasElement

/**
 * @CreateDate: 2026/11/19
 * @Author: jlj
 * @Description: H5 WebGL1 最小网格 Present（Lambert + orbit；backend=webgl-model）。
 */
internal class HolmWebGlModelRenderer {
    private var _gl: WebGLRenderingContext? = null
    private var _program: WebGLProgram? = null
    private var _uMvp: WebGLUniformLocation? = null
    private var _uColor: WebGLUniformLocation? = null
    private var _uLightDir: WebGLUniformLocation? = null
    private var _uLit: WebGLUniformLocation? = null
    private var _uLightIntensity: WebGLUniformLocation? = null
    private var _aPos: Int = -1
    private var _aNrm: Int = -1
    private var _vbo: WebGLBuffer? = null
    private var _ibo: WebGLBuffer? = null
    private var _indexCount: Int = 0
    private var _width: Int = 1
    private var _height: Int = 1
    private var _yaw: Float = 0.4f
    private var _pitch: Float = 0.3f
    private var _orbitEnabled: Boolean = true
    private var _color: FloatArray = floatArrayOf(0.85f, 0.55f, 0.25f, 1f)
    private var _lights: List<HolmModelLight> = emptyList()
    private var _center: FloatArray = floatArrayOf(0f, 0f, 0f)
    private var _radius: Float = 1f
    private var _ready: Boolean = false
    private var _pending: HolmModelMeshUpload.Packed? = null
    private var _rafId: Int = 0
    private var _playing: Boolean = true
    private var _perfWindowMs: Double = 0.0
    private var _frameCount: Int = 0
    private val _mvp = FloatArray(16)
    private var _mvpJs: Float32Array? = null

    var onReady: (() -> Unit)? = null
    var onError: ((String, String) -> Unit)? = null
    var onPerf: ((Float, Float) -> Unit)? = null
    var lightsProvider: (() -> List<HolmModelLight>)? = null

    fun attach(canvas: HTMLCanvasElement, width: Int, height: Int): Boolean {
        destroyGl()
        val gl = obtainWebGl(canvas) ?: run {
            onError?.invoke("WEBGL_CONTEXT", "WebGL unavailable")
            return false
        }
        _gl = gl
        if (gl.getExtension("OES_element_index_uint") == null) {
            onError?.invoke("WEBGL_EXT", "OES_element_index_uint required")
            destroyGl()
            return false
        }
        gl.clearColor(0.02f, 0.08f, 0.18f, 1f)
        gl.enable(WebGLRenderingContext.DEPTH_TEST)
        gl.depthFunc(WebGLRenderingContext.LEQUAL)
        gl.depthMask(true)
        gl.enable(WebGLRenderingContext.CULL_FACE)
        gl.cullFace(WebGLRenderingContext.BACK)
        gl.frontFace(WebGLRenderingContext.CCW)
        val program = link(gl, VERT, FRAG) ?: run {
            onError?.invoke("WEBGL_SHADER", "link failed")
            destroyGl()
            return false
        }
        _program = program
        _uMvp = gl.getUniformLocation(program, "u_mvp")
        _uColor = gl.getUniformLocation(program, "u_color")
        _uLightDir = gl.getUniformLocation(program, "u_lightDir")
        _uLit = gl.getUniformLocation(program, "u_lit")
        _uLightIntensity = gl.getUniformLocation(program, "u_lightIntensity")
        _aPos = gl.getAttribLocation(program, "a_pos")
        _aNrm = gl.getAttribLocation(program, "a_nrm")
        resize(width, height)
        val pending = _pending
        if (pending != null) {
            _pending = null
            applyPacked(pending)
        }
        onReady?.invoke()
        startLoop()
        return true
    }

    fun resize(width: Int, height: Int) {
        _width = width.coerceAtLeast(1)
        _height = height.coerceAtLeast(1)
        val gl = _gl ?: return
        gl.viewport(0, 0, _width, _height)
    }

    fun setPlaying(playing: Boolean) {
        _playing = playing
        if (playing) startLoop() else stopLoop()
    }

    fun setOrbit(yaw: Float, pitch: Float, enabled: Boolean) {
        _yaw = yaw
        _pitch = pitch.coerceIn(-1.2f, 1.2f)
        _orbitEnabled = enabled
    }

    fun nudgeOrbit(dx: Float, dy: Float) {
        if (!_orbitEnabled) return
        _yaw += dx * 0.01f
        _pitch = (_pitch + dy * 0.01f).coerceIn(-1.2f, 1.2f)
    }

    fun setAppearance(color: FloatArray, lights: List<HolmModelLight>) {
        _color = color.copyOf(4)
        _lights = lights
    }

    fun uploadPacked(packed: HolmModelMeshUpload.Packed) {
        if (_gl == null || _program == null) {
            _pending = packed
            return
        }
        _pending = null
        applyPacked(packed)
    }

    fun clearGpu() {
        _pending = null
        releaseBuffers()
        _ready = false
        val gl = _gl ?: return
        gl.clear(WebGLRenderingContext.COLOR_BUFFER_BIT or WebGLRenderingContext.DEPTH_BUFFER_BIT)
    }

    fun destroy() {
        stopLoop()
        destroyGl()
        onReady = null
        onError = null
        onPerf = null
        lightsProvider = null
    }

    private fun applyPacked(packed: HolmModelMeshUpload.Packed) {
        val gl = _gl ?: return
        releaseBuffers()
        _center = packed.center.copyOf(3)
        _radius = packed.radius
        _color = packed.color.copyOf(4)
        val vbo = gl.createBuffer() ?: return
        val ibo = gl.createBuffer() ?: return
        val verts = Float32Array(packed.verts.size)
        val vertsDyn = verts.asDynamic()
        for (i in packed.verts.indices) vertsDyn[i] = packed.verts[i]
        val indices = Uint32Array(packed.indices.size)
        val idxDyn = indices.asDynamic()
        for (i in packed.indices.indices) idxDyn[i] = packed.indices[i]
        gl.bindBuffer(WebGLRenderingContext.ARRAY_BUFFER, vbo)
        gl.bufferData(WebGLRenderingContext.ARRAY_BUFFER, verts, WebGLRenderingContext.STATIC_DRAW)
        gl.bindBuffer(WebGLRenderingContext.ELEMENT_ARRAY_BUFFER, ibo)
        gl.bufferData(WebGLRenderingContext.ELEMENT_ARRAY_BUFFER, indices, WebGLRenderingContext.STATIC_DRAW)
        _vbo = vbo
        _ibo = ibo
        _indexCount = packed.indices.size
        _ready = _indexCount > 0
    }

    private fun startLoop() {
        if (_rafId != 0 || !_playing || _gl == null) return
        lateinit var onFrame: (Double) -> Unit
        onFrame = frame@{ now ->
            _rafId = 0
            if (!_playing || _gl == null) return@frame
            drawFrame(now)
            _rafId = js("requestAnimationFrame").unsafeCast<((Double) -> Unit) -> Int>()(onFrame)
        }
        _rafId = js("requestAnimationFrame").unsafeCast<((Double) -> Unit) -> Int>()(onFrame)
    }

    private fun stopLoop() {
        if (_rafId != 0) {
            js("cancelAnimationFrame").unsafeCast<(Int) -> Unit>()(_rafId)
            _rafId = 0
        }
        _perfWindowMs = 0.0
        _frameCount = 0
    }

    private fun drawFrame(now: Double) {
        val gl = _gl ?: return
        val program = _program ?: return
        gl.clear(WebGLRenderingContext.COLOR_BUFFER_BIT or WebGLRenderingContext.DEPTH_BUFFER_BIT)
        if (!_ready || _indexCount <= 0 || _vbo == null || _ibo == null) {
            tickPerf(now)
            return
        }
        lightsProvider?.let { provider -> _lights = provider() }
        gl.useProgram(program)
        HolmModelMeshUpload.mvp(
            _mvp,
            _center,
            _radius,
            _yaw,
            _pitch,
            _width.toFloat() / _height.toFloat(),
        )
        val mvpJs = _mvpJs ?: Float32Array(16).also { _mvpJs = it }
        val mvpDyn = mvpJs.asDynamic()
        for (i in 0 until 16) mvpDyn[i] = _mvp[i]
        gl.uniformMatrix4fv(_uMvp, false, mvpJs)
        gl.uniform4f(_uColor, _color[0], _color[1], _color[2], _color[3])
        val lit = if (_lights.isEmpty()) 0 else 1
        gl.uniform1i(_uLit, lit)
        if (lit != 0) {
            val L = _lights[0]
            val len = kotlin.math.sqrt(
                L.directionX * L.directionX + L.directionY * L.directionY + L.directionZ * L.directionZ,
            ).coerceAtLeast(1e-5f)
            gl.uniform3f(_uLightDir, L.directionX / len, L.directionY / len, L.directionZ / len)
            gl.uniform1f(_uLightIntensity, L.intensity.coerceAtLeast(0f))
        } else {
            gl.uniform3f(_uLightDir, 0.3f, 0.8f, 0.4f)
            gl.uniform1f(_uLightIntensity, 1f)
        }
        gl.bindBuffer(WebGLRenderingContext.ARRAY_BUFFER, _vbo)
        gl.enableVertexAttribArray(_aPos)
        gl.vertexAttribPointer(_aPos, 3, WebGLRenderingContext.FLOAT, false, 24, 0)
        gl.enableVertexAttribArray(_aNrm)
        gl.vertexAttribPointer(_aNrm, 3, WebGLRenderingContext.FLOAT, false, 24, 12)
        gl.bindBuffer(WebGLRenderingContext.ELEMENT_ARRAY_BUFFER, _ibo)
        gl.drawElements(
            WebGLRenderingContext.TRIANGLES,
            _indexCount,
            WebGLRenderingContext.UNSIGNED_INT,
            0,
        )
        tickPerf(now)
    }

    private fun tickPerf(now: Double) {
        val cb = onPerf ?: return
        if (_perfWindowMs == 0.0) {
            _perfWindowMs = now
            _frameCount = 0
            return
        }
        _frameCount += 1
        val elapsed = now - _perfWindowMs
        if (elapsed >= 1000.0) {
            val fps = (_frameCount * 1000.0 / elapsed).toFloat()
            val frameMs = if (fps > 0.01f) 1000f / fps else 0f
            cb(fps, frameMs)
            _perfWindowMs = now
            _frameCount = 0
        }
    }

    private fun releaseBuffers() {
        val gl = _gl ?: return
        _vbo?.let { gl.deleteBuffer(it) }
        _ibo?.let { gl.deleteBuffer(it) }
        _vbo = null
        _ibo = null
        _indexCount = 0
    }

    private fun destroyGl() {
        stopLoop()
        releaseBuffers()
        val gl = _gl
        val program = _program
        if (gl != null && program != null) {
            gl.deleteProgram(program)
        }
        _program = null
        _gl = null
        _mvpJs = null
        _ready = false
    }

    private fun obtainWebGl(canvas: HTMLCanvasElement): WebGLRenderingContext? {
        return try {
            val attrs = js("({ alpha: false, depth: true, antialias: true, preserveDrawingBuffer: false })")
            val gl = canvas.getContext("webgl", attrs)
                ?: canvas.getContext("experimental-webgl", attrs)
            gl?.unsafeCast<WebGLRenderingContext>()
        } catch (t: Throwable) {
            val msg = t.message ?: ""
            onError?.invoke(
                "WEBGL_CONTEXT",
                if (msg.contains("offscreen", ignoreCase = true)) {
                    "canvas transferred to Offscreen (Lite Worker); need fresh canvas"
                } else {
                    msg.ifBlank { "WebGL unavailable" }
                },
            )
            null
        }
    }

    private fun link(gl: WebGLRenderingContext, vsSrc: String, fsSrc: String): WebGLProgram? {
        val vs = compile(gl, WebGLRenderingContext.VERTEX_SHADER, vsSrc) ?: return null
        val fs = compile(gl, WebGLRenderingContext.FRAGMENT_SHADER, fsSrc) ?: return null
        val p = gl.createProgram() ?: return null
        gl.attachShader(p, vs)
        gl.attachShader(p, fs)
        gl.linkProgram(p)
        val ok = gl.getProgramParameter(p, WebGLRenderingContext.LINK_STATUS) == true
        gl.deleteShader(vs)
        gl.deleteShader(fs)
        if (!ok) {
            gl.deleteProgram(p)
            return null
        }
        return p
    }

    private fun compile(gl: WebGLRenderingContext, type: Int, src: String): dynamic {
        val s = gl.createShader(type) ?: return null
        gl.shaderSource(s, src)
        gl.compileShader(s)
        val ok = gl.getShaderParameter(s, WebGLRenderingContext.COMPILE_STATUS) == true
        if (!ok) {
            gl.deleteShader(s)
            return null
        }
        return s
    }

    companion object {
        private const val VERT = """
            attribute vec3 a_pos;
            attribute vec3 a_nrm;
            uniform mat4 u_mvp;
            varying vec3 v_nrm;
            void main() {
              v_nrm = a_nrm;
              gl_Position = u_mvp * vec4(a_pos, 1.0);
            }
        """
        private const val FRAG = """
            precision mediump float;
            varying vec3 v_nrm;
            uniform vec4 u_color;
            uniform vec3 u_lightDir;
            uniform int u_lit;
            uniform float u_lightIntensity;
            void main() {
              if (u_lit == 0) {
                gl_FragColor = u_color;
              } else {
                float ndl = max(dot(normalize(v_nrm), normalize(u_lightDir)), 0.15);
                gl_FragColor = vec4(u_color.rgb * ndl * u_lightIntensity, u_color.a);
              }
            }
        """
    }
}
