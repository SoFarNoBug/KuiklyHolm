@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.jlj.kuiklybase.holm

import com.jlj.kuiklybase.holm.gltf.GltfDocument
import com.jlj.kuiklybase.holm.gltf.GltfMinimalGlb
import com.jlj.kuiklybase.holm.gltf.GltfParseException
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSBundle
import platform.Foundation.NSData
import platform.Foundation.create
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_global_queue
import platform.darwin.dispatch_get_main_queue
import platform.posix.QOS_CLASS_USER_INITIATED
import platform.posix.memcpy
import kotlin.concurrent.AtomicInt
import kotlin.concurrent.AtomicLong

/**
 * @CreateDate: 2026/11/19
 * @Author: jlj
 * @Description: iOS Model Present 桥 — 按岛 session 分表（审查 P1-concurrency）。
 */
class HolmIosModelSnapshot(
    val verts: FloatArray,
    val indices: IntArray,
    val centerX: Float,
    val centerY: Float,
    val centerZ: Float,
    val radius: Float,
    val colorR: Float,
    val colorG: Float,
    val colorB: Float,
    val colorA: Float,
    val yaw: Float,
    val pitch: Float,
    val orbitEnabled: Boolean,
)

object HolmIosModelBridge {
    private val _nextSessionId = AtomicLong(1L)
    private val _sessions = mutableMapOf<Long, Session>()

    /** 每座 KRHolmSurface 进 Model 时 acquire；卸岛 destroy。 */
    fun createSession(): Long {
        val id = _nextSessionId.getAndAdd(1L)
        _sessions[id] = Session()
        return id
    }

    fun destroySession(sessionId: Long) {
        if (sessionId == 0L) return
        _sessions.remove(sessionId)?.cancel()
    }

    fun attach(sessionId: Long, contentId: String, executor: String) {
        session(sessionId)?.attach(contentId, executor)
    }

    fun detach(sessionId: Long) {
        session(sessionId)?.detach()
    }

    fun setExecutor(sessionId: Long, executor: String) {
        session(sessionId)?.setExecutor(executor)
    }

    fun nudgeOrbit(sessionId: Long, dx: Float, dy: Float) {
        session(sessionId)?.nudgeOrbit(dx, dy)
    }

    fun yaw(sessionId: Long): Float = session(sessionId)?.yaw() ?: 0.4f

    fun pitch(sessionId: Long): Float = session(sessionId)?.pitch() ?: 0.3f

    fun orbitEnabled(sessionId: Long): Boolean = session(sessionId)?.orbitEnabled() ?: true

    fun isReady(sessionId: Long): Boolean = session(sessionId)?.isReady() == true

    fun lastErrorCode(sessionId: Long): String = session(sessionId)?.lastErrorCode().orEmpty()

    fun lastErrorMessage(sessionId: Long): String = session(sessionId)?.lastErrorMessage().orEmpty()

    fun clearError(sessionId: Long) {
        session(sessionId)?.clearError()
    }

    fun consumeSnapshot(sessionId: Long): HolmIosModelSnapshot? =
        session(sessionId)?.consumeSnapshot()

    /** 无 Present 宿主时把已取出的 snapshot 还回 pending（P2-consume-snap-no-host）。 */
    fun requeueSnapshot(sessionId: Long, snapshot: HolmIosModelSnapshot) {
        session(sessionId)?.requeueSnapshot(snapshot)
    }

    fun currentPacked(sessionId: Long): HolmIosModelSnapshot? =
        session(sessionId)?.currentPacked()

    fun fillMvp(sessionId: Long, aspect: Float): FloatArray =
        session(sessionId)?.fillMvp(aspect)
            ?: FloatArray(16) { if (it % 5 == 0) 1f else 0f }

    fun colorRgba(sessionId: Long): FloatArray =
        session(sessionId)?.colorRgba() ?: floatArrayOf(0.85f, 0.55f, 0.25f, 1f)

    fun lightDirIntensity(sessionId: Long): FloatArray =
        session(sessionId)?.lightDirIntensity()
            ?: floatArrayOf(0f, 0.3f, 0.8f, 0.4f, 1f)

    private fun session(sessionId: Long): Session? {
        if (sessionId == 0L) return null
        return _sessions[sessionId]
    }

    private class Session {
        private val _gen = AtomicInt(0)
        private var _contentId: String = ""
        private var _content: HolmModelContent? = null
        private var _executor: String = HolmExecutor.GL
        private var _yaw: Float = 0.4f
        private var _pitch: Float = 0.3f
        private var _packed: HolmModelMeshUpload.Packed? = null
        private var _pendingSnapshot: HolmIosModelSnapshot? = null
        private var _lastErrorCode: String = ""
        private var _lastErrorMessage: String = ""
        private var _ready: Boolean = false

        fun cancel() {
            _gen.addAndGet(1)
            detach()
        }

        fun attach(contentId: String, executor: String) {
            val id = contentId.trim()
            if (id.isEmpty()) {
                detach()
                return
            }
            val content = HolmContentRegistry.get(id) as? HolmModelContent
            if (content == null) {
                _lastErrorCode = "MODEL_LOAD"
                _lastErrorMessage = "HolmModelContent missing for contentId=$id"
                _pendingSnapshot = null
                _packed = null
                _ready = false
                return
            }
            val sameId = id == _contentId
            val nextExec = HolmExecutor.normalize(executor)
            if (sameId && nextExec == _executor && _packed != null && _content === content) {
                return
            }
            if (sameId && nextExec != _executor && _packed != null) {
                _executor = nextExec
                _content = content
                _pendingSnapshot = snapshotFromPacked(_packed!!)
                return
            }
            val keepOrbit = _contentId.isNotEmpty() && _ready
            if (!keepOrbit) {
                _yaw = content.yaw
                _pitch = content.pitch
            }
            _contentId = id
            _content = content
            _executor = nextExec
            val gen = _gen.addAndGet(1)
            dispatch_async(dispatch_get_global_queue(QOS_CLASS_USER_INITIATED.toLong(), 0u)) {
                try {
                    val bytes = loadBytes(content)
                    val doc = GltfDocument.parse(bytes)
                    val packed = HolmModelMeshUpload.pack(doc, content.solidColorRgba)
                    dispatch_async(dispatch_get_main_queue()) {
                        if (gen != _gen.value) return@dispatch_async
                        _packed = packed
                        _pendingSnapshot = snapshotFromPacked(packed)
                        _ready = true
                        _lastErrorCode = ""
                        _lastErrorMessage = ""
                    }
                } catch (t: Throwable) {
                    val msg = when (t) {
                        is GltfParseException -> t.message ?: "parse failed"
                        else -> t.message ?: t::class.simpleName ?: "load failed"
                    }
                    dispatch_async(dispatch_get_main_queue()) {
                        if (gen != _gen.value) return@dispatch_async
                        _lastErrorCode = "MODEL_LOAD"
                        _lastErrorMessage = msg
                        _pendingSnapshot = null
                    }
                }
            }
        }

        fun detach() {
            _gen.addAndGet(1)
            _contentId = ""
            _content = null
            _packed = null
            _pendingSnapshot = null
            _ready = false
            _lastErrorCode = ""
            _lastErrorMessage = ""
        }

        fun setExecutor(executor: String) {
            val next = HolmExecutor.normalize(executor)
            if (next == _executor) return
            _executor = next
            val packed = _packed ?: return
            _pendingSnapshot = snapshotFromPacked(packed)
        }

        fun nudgeOrbit(dx: Float, dy: Float) {
            val content = _content ?: return
            if (!content.orbitEnabled) return
            _yaw -= dx * 0.01f
            _pitch = (_pitch - dy * 0.01f).coerceIn(-1.2f, 1.2f)
            content.setYaw(_yaw)
            content.setPitch(_pitch)
        }

        fun yaw(): Float = _yaw

        fun pitch(): Float = _pitch

        fun orbitEnabled(): Boolean = _content?.orbitEnabled ?: true

        fun isReady(): Boolean = _ready && _packed != null

        fun lastErrorCode(): String = _lastErrorCode

        fun lastErrorMessage(): String = _lastErrorMessage

        fun clearError() {
            _lastErrorCode = ""
            _lastErrorMessage = ""
        }

        fun consumeSnapshot(): HolmIosModelSnapshot? {
            val snap = _pendingSnapshot ?: return null
            _pendingSnapshot = null
            return snap
        }

        fun requeueSnapshot(snapshot: HolmIosModelSnapshot) {
            if (_pendingSnapshot == null) {
                _pendingSnapshot = snapshot
            }
        }

        fun currentPacked(): HolmIosModelSnapshot? {
            val packed = _packed ?: return null
            return snapshotFromPacked(packed)
        }

        fun fillMvp(aspect: Float): FloatArray {
            val packed = _packed ?: return FloatArray(16) { if (it % 5 == 0) 1f else 0f }
            val out = FloatArray(16)
            HolmModelMeshUpload.mvp(out, packed.center, packed.radius, _yaw, _pitch, aspect)
            return out
        }

        fun colorRgba(): FloatArray {
            val packed = _packed ?: return floatArrayOf(0.85f, 0.55f, 0.25f, 1f)
            return packed.color.copyOf(4)
        }

        fun lightDirIntensity(): FloatArray {
            val lights = _content?.lights.orEmpty()
            if (lights.isEmpty()) {
                return floatArrayOf(0f, 0.3f, 0.8f, 0.4f, 1f)
            }
            val L = lights[0]
            val len = kotlin.math.sqrt(
                L.directionX * L.directionX + L.directionY * L.directionY + L.directionZ * L.directionZ,
            ).coerceAtLeast(1e-5f)
            return floatArrayOf(
                1f,
                L.directionX / len,
                L.directionY / len,
                L.directionZ / len,
                L.intensity.coerceAtLeast(0f),
            )
        }

        private fun snapshotFromPacked(packed: HolmModelMeshUpload.Packed): HolmIosModelSnapshot =
            HolmIosModelSnapshot(
                verts = packed.verts,
                indices = packed.indices,
                centerX = packed.center[0],
                centerY = packed.center[1],
                centerZ = packed.center[2],
                radius = packed.radius,
                colorR = packed.color[0],
                colorG = packed.color[1],
                colorB = packed.color[2],
                colorA = packed.color[3],
                yaw = _yaw,
                pitch = _pitch,
                orbitEnabled = _content?.orbitEnabled ?: true,
            )

        @OptIn(BetaInteropApi::class)
        private fun loadBytes(content: HolmModelContent): ByteArray {
            content.bytes?.let { return it }
            val asset = content.assetPath
            if (!asset.isNullOrBlank()) {
                return try {
                    readBundleAsset(asset)
                } catch (_: Exception) {
                    if (asset.contains("hero.glb")) {
                        GltfMinimalGlb.buildHeroCubeGlb()
                    } else {
                        throw GltfParseException("bundle missing: $asset")
                    }
                }
            }
            val uri = content.fileUri
            if (!uri.isNullOrBlank()) {
                throw GltfParseException("iOS fileUri not supported yet: $uri")
            }
            throw GltfParseException("HolmModelContent has no bytes/asset/fileUri")
        }

        @OptIn(BetaInteropApi::class)
        private fun readBundleAsset(assetPath: String): ByteArray {
            val cleaned = assetPath.removePrefix("/")
            val slash = cleaned.lastIndexOf('/')
            val (dir, nameWithExt) = if (slash >= 0) {
                cleaned.substring(0, slash) to cleaned.substring(slash + 1)
            } else {
                null to cleaned
            }
            val dot = nameWithExt.lastIndexOf('.')
            val (name, ext) = if (dot > 0) {
                nameWithExt.substring(0, dot) to nameWithExt.substring(dot + 1)
            } else {
                nameWithExt to null
            }
            val path = NSBundle.mainBundle.pathForResource(name, ext, dir)
                ?: throw GltfParseException("NSBundle pathForResource failed: $assetPath")
            val data = NSData.create(contentsOfFile = path)
                ?: throw GltfParseException("NSData read failed: $path")
            val len = data.length.toInt()
            if (len <= 0) return ByteArray(0)
            val out = ByteArray(len)
            out.usePinned { pinned ->
                memcpy(pinned.addressOf(0), data.bytes, data.length)
            }
            return out
        }
    }
}
