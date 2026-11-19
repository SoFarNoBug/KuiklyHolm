package com.jlj.kuiklybase.holm

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.jlj.kuiklybase.holm.gltf.GltfDocument
import com.jlj.kuiklybase.holm.gltf.GltfMesh
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * @CreateDate: 2026/11/18
 * @Author: jlj
 * @Description: Android GLES 最小网格 Present（unlit / Lambert + orbit framing）。
 */
internal class HolmGlesModelRenderer : GLSurfaceView.Renderer {
    private var _program: Int = 0
    private var _uMvp: Int = -1
    private var _uColor: Int = -1
    private var _uLightDir: Int = -1
    private var _uLit: Int = -1
    private var _uLightIntensity: Int = -1
    private var _aPos: Int = -1
    private var _aNrm: Int = -1

    private var _width: Int = 1
    private var _height: Int = 1
    private var _yaw: Float = 0.4f
    private var _pitch: Float = 0.3f
    private var _orbitEnabled: Boolean = true
    private var _color: FloatArray = floatArrayOf(0.85f, 0.55f, 0.25f, 1f)
    private var _lights: List<HolmModelLight> = emptyList()
    private var _gpuMeshes: List<GpuMesh> = emptyList()
    private var _center: FloatArray = floatArrayOf(0f, 0f, 0f)
    private var _radius: Float = 1f
    private var _ready: Boolean = false
    /** surface 未就绪时暂存，onSurfaceCreated 再灌。 */
    private var _pendingDoc: GltfDocument? = null
    private var _pendingApplyBaseColor: Boolean = true
    private var _fpsWindowNs: Long = 0L
    private var _frameCount: Int = 0

    var onReady: (() -> Unit)? = null
    var onError: ((String, String) -> Unit)? = null
    var onPerf: ((Float, Float) -> Unit)? = null
    /** 每帧拉取最新灯光（Content.setLights 热更新，不重载 mesh）。 */
    var lightsProvider: (() -> List<HolmModelLight>)? = null

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

    /**
     * @param applyBaseColorFactor 仅当业务未显式设 solidColor 时用 glTF factor；避免同色启发式误覆盖。
     */
    fun upload(doc: GltfDocument, applyBaseColorFactor: Boolean = true) {
        if (_program == 0) {
            _pendingDoc = doc
            _pendingApplyBaseColor = applyBaseColorFactor
            return
        }
        _pendingDoc = null
        applyUpload(doc, applyBaseColorFactor)
    }

    fun clearGpu() {
        _pendingDoc = null
        releaseGpu()
        _ready = false
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.02f, 0.08f, 0.18f, 1f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthFunc(GLES20.GL_LEQUAL)
        GLES20.glDepthMask(true)
        // 与 wgpu ModelPass cull_mode=Back 对齐（CCW 正面）
        GLES20.glEnable(GLES20.GL_CULL_FACE)
        GLES20.glCullFace(GLES20.GL_BACK)
        GLES20.glFrontFace(GLES20.GL_CCW)
        _program = link(
            VERT,
            FRAG,
        )
        _uMvp = GLES20.glGetUniformLocation(_program, "u_mvp")
        _uColor = GLES20.glGetUniformLocation(_program, "u_color")
        _uLightDir = GLES20.glGetUniformLocation(_program, "u_lightDir")
        _uLit = GLES20.glGetUniformLocation(_program, "u_lit")
        _uLightIntensity = GLES20.glGetUniformLocation(_program, "u_lightIntensity")
        _aPos = GLES20.glGetAttribLocation(_program, "a_pos")
        _aNrm = GLES20.glGetAttribLocation(_program, "a_nrm")
        val pending = _pendingDoc
        if (pending != null) {
            _pendingDoc = null
            applyUpload(pending, _pendingApplyBaseColor)
        }
        onReady?.invoke()
    }

    private fun applyUpload(doc: GltfDocument, applyBaseColorFactor: Boolean) {
        releaseGpu()
        if (applyBaseColorFactor) {
            val factor = doc.baseColorFactor
            _color = floatArrayOf(factor[0], factor[1], factor[2], factor.getOrElse(3) { 1f })
        }
        var minX = Float.POSITIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY
        var minZ = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY
        var maxZ = Float.NEGATIVE_INFINITY
        for (m in doc.meshes) {
            var i = 0
            while (i + 2 < m.positions.size) {
                val x = m.positions[i]
                val y = m.positions[i + 1]
                val z = m.positions[i + 2]
                if (x < minX) minX = x
                if (y < minY) minY = y
                if (z < minZ) minZ = z
                if (x > maxX) maxX = x
                if (y > maxY) maxY = y
                if (z > maxZ) maxZ = z
                i += 3
            }
        }
        _center[0] = (minX + maxX) * 0.5f
        _center[1] = (minY + maxY) * 0.5f
        _center[2] = (minZ + maxZ) * 0.5f
        val dx = maxX - minX
        val dy = maxY - minY
        val dz = maxZ - minZ
        _radius = max(dx, max(dy, dz)).coerceAtLeast(0.001f) * 0.6f
        _gpuMeshes = doc.meshes.map { uploadMesh(it) }
        _ready = true
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        _width = width.coerceAtLeast(1)
        _height = height.coerceAtLeast(1)
        GLES20.glViewport(0, 0, _width, _height)
    }

    override fun onDrawFrame(gl: GL10?) {
        val nowNs = System.nanoTime()
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        if (!_ready || _program == 0 || _gpuMeshes.isEmpty()) {
            tickPerf(nowNs)
            return
        }
        lightsProvider?.let { provider ->
            _lights = provider()
        }
        GLES20.glUseProgram(_program)
        val mvp = FloatArray(16)
        val view = FloatArray(16)
        val proj = FloatArray(16)
        val eyeDist = _radius * 3.2f
        val cy = cos(_pitch.toDouble()).toFloat()
        val eyeX = _center[0] + eyeDist * cy * sin(_yaw.toDouble()).toFloat()
        val eyeY = _center[1] + eyeDist * sin(_pitch.toDouble()).toFloat()
        val eyeZ = _center[2] + eyeDist * cy * cos(_yaw.toDouble()).toFloat()
        Matrix.setLookAtM(view, 0, eyeX, eyeY, eyeZ, _center[0], _center[1], _center[2], 0f, 1f, 0f)
        val aspect = _width.toFloat() / _height.toFloat()
        // near/far 跟 eyeDist 成比例：远近比可控（减 z-fighting），且不裁到主体
        val zNear = (eyeDist * 0.05f).coerceAtLeast(0.01f)
        val zFar = (eyeDist * 10f).coerceAtLeast(zNear * 100f)
        Matrix.perspectiveM(proj, 0, 45f, aspect, zNear, zFar)
        Matrix.multiplyMM(mvp, 0, proj, 0, view, 0)
        GLES20.glUniformMatrix4fv(_uMvp, 1, false, mvp, 0)
        GLES20.glUniform4f(_uColor, _color[0], _color[1], _color[2], _color[3])
        val lit = if (_lights.isEmpty()) 0 else 1
        GLES20.glUniform1i(_uLit, lit)
        if (lit != 0) {
            val L = _lights[0]
            val len = kotlin.math.sqrt(
                L.directionX * L.directionX + L.directionY * L.directionY + L.directionZ * L.directionZ,
            ).coerceAtLeast(1e-5f)
            GLES20.glUniform3f(
                _uLightDir,
                L.directionX / len,
                L.directionY / len,
                L.directionZ / len,
            )
            GLES20.glUniform1f(_uLightIntensity, L.intensity.coerceAtLeast(0f))
        } else {
            GLES20.glUniform3f(_uLightDir, 0.3f, 0.8f, 0.4f)
            GLES20.glUniform1f(_uLightIntensity, 1f)
        }
        for (mesh in _gpuMeshes) {
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mesh.vbo)
            GLES20.glEnableVertexAttribArray(_aPos)
            GLES20.glVertexAttribPointer(_aPos, 3, GLES20.GL_FLOAT, false, 24, 0)
            GLES20.glEnableVertexAttribArray(_aNrm)
            GLES20.glVertexAttribPointer(_aNrm, 3, GLES20.GL_FLOAT, false, 24, 12)
            GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, mesh.ibo)
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, mesh.indexCount, GLES20.GL_UNSIGNED_INT, 0)
        }
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0)
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

    private fun uploadMesh(mesh: GltfMesh): GpuMesh {
        val vertCount = mesh.positions.size / 3
        val interleaved = FloatArray(vertCount * 6)
        for (i in 0 until vertCount) {
            interleaved[i * 6] = mesh.positions[i * 3]
            interleaved[i * 6 + 1] = mesh.positions[i * 3 + 1]
            interleaved[i * 6 + 2] = mesh.positions[i * 3 + 2]
            interleaved[i * 6 + 3] = mesh.normals.getOrElse(i * 3) { 0f }
            interleaved[i * 6 + 4] = mesh.normals.getOrElse(i * 3 + 1) { 1f }
            interleaved[i * 6 + 5] = mesh.normals.getOrElse(i * 3 + 2) { 0f }
        }
        val vbo = IntArray(1)
        val ibo = IntArray(1)
        GLES20.glGenBuffers(1, vbo, 0)
        GLES20.glGenBuffers(1, ibo, 0)
        val vb: FloatBuffer = ByteBuffer.allocateDirect(interleaved.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(interleaved)
        vb.position(0)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo[0])
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, interleaved.size * 4, vb, GLES20.GL_STATIC_DRAW)
        val ib: IntBuffer = ByteBuffer.allocateDirect(mesh.indices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asIntBuffer()
            .put(mesh.indices)
        ib.position(0)
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, ibo[0])
        GLES20.glBufferData(
            GLES20.GL_ELEMENT_ARRAY_BUFFER,
            mesh.indices.size * 4,
            ib,
            GLES20.GL_STATIC_DRAW,
        )
        return GpuMesh(vbo[0], ibo[0], mesh.indices.size)
    }

    private fun releaseGpu() {
        for (m in _gpuMeshes) {
            val bufs = intArrayOf(m.vbo, m.ibo)
            GLES20.glDeleteBuffers(2, bufs, 0)
        }
        _gpuMeshes = emptyList()
    }

    private fun link(vsSrc: String, fsSrc: String): Int {
        val vs = compile(GLES20.GL_VERTEX_SHADER, vsSrc)
        val fs = compile(GLES20.GL_FRAGMENT_SHADER, fsSrc)
        val p = GLES20.glCreateProgram()
        GLES20.glAttachShader(p, vs)
        GLES20.glAttachShader(p, fs)
        GLES20.glLinkProgram(p)
        return p
    }

    private fun compile(type: Int, src: String): Int {
        val s = GLES20.glCreateShader(type)
        GLES20.glShaderSource(s, src)
        GLES20.glCompileShader(s)
        return s
    }

    private data class GpuMesh(val vbo: Int, val ibo: Int, val indexCount: Int)

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
