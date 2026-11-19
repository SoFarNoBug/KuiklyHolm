@file:OptIn(
    kotlinx.cinterop.ExperimentalForeignApi::class,
    kotlin.native.concurrent.ObsoleteWorkersApi::class,
)

package com.jlj.kuiklybase.holm

import com.jlj.kuiklybase.holm.gltf.GltfDocument
import com.jlj.kuiklybase.holm.gltf.GltfMinimalGlb
import com.jlj.kuiklybase.holm.gltf.GltfParseException
import kotlin.native.concurrent.Worker
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.posix.SEEK_END
import platform.posix.SEEK_SET
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fread
import platform.posix.fseek
import platform.posix.ftell
import kotlin.concurrent.AtomicInt
import kotlin.concurrent.AtomicLong
import kotlin.concurrent.AtomicReference
import kotlin.concurrent.Volatile
import kotlin.math.sqrt

/**
 * @CreateDate: 2026/11/19
 * @Author: jlj
 * @Description: Ohos Model Present 桥 — 按岛 session；异步 parse（ADR 0019 / 审查回执行）。
 */
class HolmOhosModelSnapshot(
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

object HolmOhosModelBridge {
    private val _nextSessionId = AtomicLong(1L)
    /** 不可变快照 + CAS；K/N 无 synchronized。 */
    private val _sessions = AtomicReference<Map<Long, Session>>(emptyMap())
    private val _fallbackMvp = FloatArray(16)
    private val _fallbackColor = floatArrayOf(0.85f, 0.55f, 0.25f, 1f)
    private val _fallbackColorLit = FloatArray(4)
    private val _fallbackLight = floatArrayOf(0f, 0.3f, 0.8f, 0.4f, 1f)
    private val _parseWorker = Worker.start(name = "holm-ohos-model-parse")

    fun createSession(): Long {
        val id = _nextSessionId.getAndAdd(1L)
        val created = Session()
        while (true) {
            val cur = _sessions.value
            val next = cur + (id to created)
            if (_sessions.compareAndSet(cur, next)) {
                break
            }
        }
        return id
    }

    fun destroySession(sessionId: Long) {
        if (sessionId == 0L) return
        var removed: Session? = null
        while (true) {
            val cur = _sessions.value
            removed = cur[sessionId]
            if (removed == null) {
                return
            }
            val next = cur - sessionId
            if (_sessions.compareAndSet(cur, next)) {
                break
            }
        }
        removed?.cancel()
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

    fun consumeSnapshot(sessionId: Long): HolmOhosModelSnapshot? =
        session(sessionId)?.consumeSnapshot()

    fun requeueSnapshot(sessionId: Long, snapshot: HolmOhosModelSnapshot) {
        session(sessionId)?.requeueSnapshot(snapshot)
    }

    fun currentPacked(sessionId: Long): HolmOhosModelSnapshot? =
        session(sessionId)?.currentPacked()

    /** 返回会话内 scratch；调用方须在同帧内读完（setFrame 已拷贝进 native）。 */
    fun fillMvp(sessionId: Long, aspect: Float): FloatArray =
        session(sessionId)?.fillMvp(aspect) ?: identityMvp(_fallbackMvp)

    /** 返回会话内 scratch；勿跨帧持有。 */
    fun colorRgba(sessionId: Long): FloatArray =
        session(sessionId)?.colorRgba() ?: _fallbackColor

    /** 返回会话内 scratch；勿跨帧持有。 */
    fun lightDirIntensity(sessionId: Long): FloatArray =
        session(sessionId)?.lightDirIntensity() ?: _fallbackLight

    /** intensity≠1 时写入独立 lit scratch，避免与 [colorRgba] 同缓冲互踩。 */
    fun colorWithIntensity(sessionId: Long, intensity: Float): FloatArray =
        session(sessionId)?.colorWithIntensity(intensity) ?: run {
            _fallbackColorLit[0] = 0.85f * intensity
            _fallbackColorLit[1] = 0.55f * intensity
            _fallbackColorLit[2] = 0.25f * intensity
            _fallbackColorLit[3] = 1f
            _fallbackColorLit
        }

    /** [0]=vertFloats [1]=indexCount；仅 pending snapshot（无 pending → 全 0，避免每帧重灌）。 */
    fun peekMeshSizes(sessionId: Long): IntArray =
        session(sessionId)?.peekMeshSizes() ?: intArrayOf(0, 0)

    /**
     * 拷贝 **pending** mesh 到调用方缓冲；无 pending 返回 0（不回落 currentPacked）。
     * @return 1 成功；0 无 pending；-1 缓冲不足（仍写入所需 sizes）。
     */
    fun copyMesh(
        sessionId: Long,
        verts: FloatArray?,
        indices: IntArray?,
        outSizes: IntArray,
        center3: FloatArray?,
        radiusOut: FloatArray?,
        color4: FloatArray?,
        yawPitch: FloatArray?,
    ): Int = session(sessionId)?.copyMesh(verts, indices, outSizes, center3, radiusOut, color4, yawPitch) ?: 0

    private fun session(sessionId: Long): Session? {
        if (sessionId == 0L) return null
        return _sessions.value[sessionId]
    }

    private fun identityMvp(out: FloatArray): FloatArray {
        for (i in out.indices) {
            out[i] = if (i % 5 == 0) 1f else 0f
        }
        return out
    }

    private class ParseHandoff(
        val gen: Int,
        val ok: Boolean,
        val packed: HolmModelMeshUpload.Packed?,
        val errorMessage: String,
    )

    private class Session {
        private val _gen = AtomicInt(0)
        private var _contentId: String = ""
        private var _content: HolmModelContent? = null
        private var _executor: String = HolmExecutor.GL
        private var _yaw: Float = 0.4f
        private var _pitch: Float = 0.3f
        private var _packed: HolmModelMeshUpload.Packed? = null
        private var _pendingSnapshot: HolmOhosModelSnapshot? = null
        private var _lastErrorCode: String = ""
        private var _lastErrorMessage: String = ""
        private var _ready: Boolean = false
        @Volatile
        private var _handoff: ParseHandoff? = null
        private val _mvpScratch = FloatArray(16)
        private val _colorScratch = FloatArray(4)
        private val _colorLitScratch = FloatArray(4)
        private val _lightScratch = FloatArray(5)

        fun cancel() {
            _gen.addAndGet(1)
            detach()
        }

        fun attach(contentId: String, executor: String) {
            drainHandoff()
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
            // 加载中：清旧 mesh，避免失败路径仍 isReady + 旧模（审查 P0-silent-fail）
            _ready = false
            _packed = null
            _pendingSnapshot = null
            _lastErrorCode = ""
            _lastErrorMessage = ""
            _handoff = null
            _parseWorker.executeAfter(0L) {
                try {
                    val bytes = loadBytes(content)
                    val doc = GltfDocument.parse(bytes)
                    val packed = HolmModelMeshUpload.pack(doc, content.solidColorRgba)
                    _handoff = ParseHandoff(gen = gen, ok = true, packed = packed, errorMessage = "")
                } catch (t: Throwable) {
                    val msg = when (t) {
                        is GltfParseException -> t.message ?: "parse failed"
                        else -> t.message ?: t::class.simpleName ?: "load failed"
                    }
                    _handoff = ParseHandoff(gen = gen, ok = false, packed = null, errorMessage = msg)
                }
            }
        }

        fun detach() {
            _gen.addAndGet(1)
            _handoff = null
            _contentId = ""
            _content = null
            _packed = null
            _pendingSnapshot = null
            _ready = false
            _lastErrorCode = ""
            _lastErrorMessage = ""
        }

        fun setExecutor(executor: String) {
            drainHandoff()
            val next = HolmExecutor.normalize(executor)
            if (next == _executor) return
            _executor = next
            val packed = _packed ?: return
            _pendingSnapshot = snapshotFromPacked(packed)
        }

        fun nudgeOrbit(dx: Float, dy: Float) {
            drainHandoff()
            val content = _content ?: return
            if (!content.orbitEnabled) return
            _yaw -= dx * 0.01f
            _pitch = (_pitch - dy * 0.01f).coerceIn(-1.2f, 1.2f)
            content.setYaw(_yaw)
            content.setPitch(_pitch)
        }

        fun yaw(): Float {
            drainHandoff()
            return _yaw
        }

        fun pitch(): Float {
            drainHandoff()
            return _pitch
        }

        fun orbitEnabled(): Boolean {
            drainHandoff()
            return _content?.orbitEnabled ?: true
        }

        fun isReady(): Boolean {
            drainHandoff()
            return _ready && _packed != null
        }

        fun lastErrorCode(): String {
            drainHandoff()
            return _lastErrorCode
        }

        fun lastErrorMessage(): String {
            drainHandoff()
            return _lastErrorMessage
        }

        fun clearError() {
            drainHandoff()
            _lastErrorCode = ""
            _lastErrorMessage = ""
        }

        fun consumeSnapshot(): HolmOhosModelSnapshot? {
            drainHandoff()
            val snap = _pendingSnapshot ?: return null
            _pendingSnapshot = null
            return snap
        }

        fun requeueSnapshot(snapshot: HolmOhosModelSnapshot) {
            drainHandoff()
            if (_pendingSnapshot == null) {
                _pendingSnapshot = snapshot
            }
        }

        fun currentPacked(): HolmOhosModelSnapshot? {
            drainHandoff()
            val packed = _packed ?: return null
            return snapshotFromPacked(packed)
        }

        fun fillMvp(aspect: Float): FloatArray {
            drainHandoff()
            val packed = _packed
            if (packed == null) {
                for (i in _mvpScratch.indices) {
                    _mvpScratch[i] = if (i % 5 == 0) 1f else 0f
                }
                return _mvpScratch
            }
            HolmModelMeshUpload.mvp(_mvpScratch, packed.center, packed.radius, _yaw, _pitch, aspect)
            return _mvpScratch
        }

        fun colorRgba(): FloatArray {
            drainHandoff()
            val packed = _packed
            if (packed == null) {
                _colorScratch[0] = 0.85f
                _colorScratch[1] = 0.55f
                _colorScratch[2] = 0.25f
                _colorScratch[3] = 1f
            } else {
                packed.color.copyInto(_colorScratch, destinationOffset = 0, startIndex = 0, endIndex = 4)
            }
            return _colorScratch
        }

        fun colorWithIntensity(intensity: Float): FloatArray {
            val base = colorRgba()
            _colorLitScratch[0] = base[0] * intensity
            _colorLitScratch[1] = base[1] * intensity
            _colorLitScratch[2] = base[2] * intensity
            _colorLitScratch[3] = base[3]
            return _colorLitScratch
        }

        fun lightDirIntensity(): FloatArray {
            drainHandoff()
            val lights = _content?.lights.orEmpty()
            if (lights.isEmpty()) {
                _lightScratch[0] = 0f
                _lightScratch[1] = 0.3f
                _lightScratch[2] = 0.8f
                _lightScratch[3] = 0.4f
                _lightScratch[4] = 1f
                return _lightScratch
            }
            val L = lights[0]
            val len = sqrt(
                L.directionX * L.directionX + L.directionY * L.directionY + L.directionZ * L.directionZ,
            ).coerceAtLeast(1e-5f)
            _lightScratch[0] = 1f
            _lightScratch[1] = L.directionX / len
            _lightScratch[2] = L.directionY / len
            _lightScratch[3] = L.directionZ / len
            _lightScratch[4] = L.intensity.coerceAtLeast(0f)
            return _lightScratch
        }

        fun peekMeshSizes(): IntArray {
            drainHandoff()
            val snap = _pendingSnapshot ?: return intArrayOf(0, 0)
            return intArrayOf(snap.verts.size, snap.indices.size)
        }

        fun copyMesh(
            verts: FloatArray?,
            indices: IntArray?,
            outSizes: IntArray,
            center3: FloatArray?,
            radiusOut: FloatArray?,
            color4: FloatArray?,
            yawPitch: FloatArray?,
        ): Int {
            drainHandoff()
            val snap = _pendingSnapshot ?: run {
                if (outSizes.size >= 2) {
                    outSizes[0] = 0
                    outSizes[1] = 0
                }
                return 0
            }
            if (outSizes.size >= 2) {
                outSizes[0] = snap.verts.size
                outSizes[1] = snap.indices.size
            }
            if (verts == null || indices == null ||
                verts.size < snap.verts.size || indices.size < snap.indices.size
            ) {
                return -1
            }
            snap.verts.copyInto(verts)
            snap.indices.copyInto(indices)
            if (center3 != null && center3.size >= 3) {
                center3[0] = snap.centerX
                center3[1] = snap.centerY
                center3[2] = snap.centerZ
            }
            if (radiusOut != null && radiusOut.isNotEmpty()) {
                radiusOut[0] = snap.radius
            }
            if (color4 != null && color4.size >= 4) {
                color4[0] = snap.colorR
                color4[1] = snap.colorG
                color4[2] = snap.colorB
                color4[3] = snap.colorA
            }
            if (yawPitch != null && yawPitch.size >= 2) {
                yawPitch[0] = snap.yaw
                yawPitch[1] = snap.pitch
            }
            if (_pendingSnapshot === snap) {
                _pendingSnapshot = null
            }
            return 1
        }

        private fun drainHandoff() {
            val handoff = _handoff ?: return
            _handoff = null
            if (handoff.gen != _gen.value) return
            if (handoff.ok) {
                val packed = handoff.packed ?: return
                _packed = packed
                _pendingSnapshot = snapshotFromPacked(packed)
                _ready = true
                _lastErrorCode = ""
                _lastErrorMessage = ""
            } else {
                _packed = null
                _pendingSnapshot = null
                _ready = false
                _lastErrorCode = "MODEL_LOAD"
                _lastErrorMessage = handoff.errorMessage
            }
        }

        private fun snapshotFromPacked(packed: HolmModelMeshUpload.Packed): HolmOhosModelSnapshot =
            HolmOhosModelSnapshot(
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

        private fun loadBytes(content: HolmModelContent): ByteArray {
            content.bytes?.let { return it }
            val asset = content.assetPath
            if (!asset.isNullOrBlank()) {
                return try {
                    readCachedAsset(asset)
                } catch (_: Exception) {
                    if (asset.contains("hero.glb")) {
                        GltfMinimalGlb.buildHeroCubeGlb()
                    } else {
                        throw GltfParseException("Ohos asset missing (copy rawfile→cache): $asset")
                    }
                }
            }
            val uri = content.fileUri
            if (!uri.isNullOrBlank()) {
                throw GltfParseException("Ohos fileUri not supported yet: $uri")
            }
            throw GltfParseException("HolmModelContent has no bytes/asset/fileUri")
        }

        private fun readCachedAsset(assetPath: String): ByteArray {
            val cleaned = assetPath.removePrefix("/")
            val cache = HolmOhos.cacheDir().trimEnd('/')
            val res = HolmOhos.resfileDir().trimEnd('/')
            val candidates = listOf(
                "$cache/$cleaned",
                "$cache/rawfile/$cleaned",
                "$res/$cleaned",
                "$res/rawfile/$cleaned",
            )
            for (path in candidates) {
                readFile(path)?.let { return it }
            }
            throw GltfParseException("fopen failed for $cleaned (tried cache/resfile)")
        }

        private fun readFile(path: String): ByteArray? {
            val file = fopen(path, "rb") ?: return null
            try {
                if (fseek(file, 0, SEEK_END) != 0) return null
                val lenLong = ftell(file)
                if (lenLong < 0) return null
                if (fseek(file, 0, SEEK_SET) != 0) return null
                val len = lenLong.toInt()
                if (len <= 0) return ByteArray(0)
                val out = ByteArray(len)
                out.usePinned { pinned ->
                    val n = fread(pinned.addressOf(0), 1u, len.toULong(), file)
                    if (n.toInt() != len) return null
                }
                return out
            } finally {
                fclose(file)
            }
        }
    }
}
