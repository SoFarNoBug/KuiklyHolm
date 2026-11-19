@file:OptIn(
    kotlinx.cinterop.ExperimentalForeignApi::class,
    kotlin.experimental.ExperimentalNativeApi::class,
)

package com.jlj.kuiklyholmui

import com.jlj.kuiklybase.holm.HolmOhosModelBridge
import com.jlj.kuiklybase.holm.HolmWgpuOhosBridge
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.FloatVar
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.set
import kotlinx.cinterop.toKString
import kotlin.experimental.ExperimentalNativeApi

/**
 * @CreateDate: 2026/11/16
 * @Author: jlj
 * @Description: libshared.so 导出 @CName；供 libholm_xc dlsym（须落在 shared 产物模块）。
 */

private fun writeCString(dst: CPointer<ByteVar>?, cap: Int, value: String): UInt {
    if (dst == null || cap <= 0) return 0u
    val bytes = value.encodeToByteArray()
    val n = minOf(bytes.size, cap - 1)
    for (i in 0 until n) {
        dst[i] = bytes[i]
    }
    dst[n] = 0
    return n.toUInt()
}

@CName("holm_ohos_bridge_require_provider")
fun holmOhosBridgeRequireProvider(
    executor: CPointer<ByteVar>?,
    errBuf: CPointer<ByteVar>?,
    errCap: Int,
): Int {
    val key = executor?.toKString().orEmpty().ifBlank { "renderer-gl" }
    val err = HolmWgpuOhosBridge.requireProvider(key)
    return if (err == null) {
        0
    } else {
        writeCString(errBuf, errCap, err)
        1
    }
}

@CName("holm_ohos_bridge_has_wgpu")
fun holmOhosBridgeHasWgpu(): Int = if (HolmWgpuOhosBridge.hasWgpu()) 1 else 0

@CName("holm_ohos_bridge_create")
fun holmOhosBridgeCreate(window: COpaquePointer?, width: Int, height: Int): Long =
    HolmWgpuOhosBridge.create(window, width, height)

@CName("holm_ohos_bridge_destroy")
fun holmOhosBridgeDestroy(handle: Long) {
    HolmWgpuOhosBridge.destroy(handle)
}

@CName("holm_ohos_bridge_tick")
fun holmOhosBridgeTick(handle: Long, dtSecs: Float): Int =
    if (HolmWgpuOhosBridge.tick(handle, dtSecs)) 1 else 0

@CName("holm_ohos_bridge_resize")
fun holmOhosBridgeResize(handle: Long, width: Int, height: Int) {
    HolmWgpuOhosBridge.resize(handle, width, height)
}

@CName("holm_ohos_bridge_set_custom_fs")
fun holmOhosBridgeSetCustomFs(handle: Long, fs: CPointer<ByteVar>?): Int {
    val body = fs?.toKString().orEmpty()
    return if (HolmWgpuOhosBridge.setCustomFs(handle, body)) 1 else 0
}

@CName("holm_ohos_bridge_set_scene")
fun holmOhosBridgeSetScene(handle: Long, scene: CPointer<ByteVar>?) {
    HolmWgpuOhosBridge.setScene(handle, scene?.toKString().orEmpty())
}

/** ADR 0015：liquid 已废；保留 CName 空桩以免旧壳 dlsym 崩。 */
@CName("holm_ohos_bridge_set_liquid")
fun holmOhosBridgeSetLiquid(handle: Long, boost: Float, bend: Float) {
    // no-op
}

@CName("holm_ohos_bridge_set_playing")
fun holmOhosBridgeSetPlaying(handle: Long, playing: Int) {
    HolmWgpuOhosBridge.setPlaying(handle, playing != 0)
}

@CName("holm_ohos_bridge_backend_id")
fun holmOhosBridgeBackendId(handle: Long, buf: CPointer<ByteVar>?, cap: Int): UInt =
    writeCString(buf, cap, HolmWgpuOhosBridge.backendId(handle))

@CName("holm_ohos_bridge_last_error_code")
fun holmOhosBridgeLastErrorCode(handle: Long, buf: CPointer<ByteVar>?, cap: Int): UInt =
    writeCString(buf, cap, HolmWgpuOhosBridge.lastErrorCode(handle))

@CName("holm_ohos_bridge_last_error_message")
fun holmOhosBridgeLastErrorMessage(handle: Long, buf: CPointer<ByteVar>?, cap: Int): UInt =
    writeCString(buf, cap, HolmWgpuOhosBridge.lastErrorMessage(handle))

@CName("holm_ohos_bridge_last_create_error")
fun holmOhosBridgeLastCreateError(buf: CPointer<ByteVar>?, cap: Int): UInt =
    writeCString(buf, cap, HolmWgpuOhosBridge.lastCreateError())

// --- ADR 0019 ModelPass ---

@CName("holm_ohos_bridge_create_model")
fun holmOhosBridgeCreateModel(window: COpaquePointer?, width: Int, height: Int): Long =
    HolmWgpuOhosBridge.createModel(window, width, height)

@CName("holm_ohos_bridge_destroy_model")
fun holmOhosBridgeDestroyModel(handle: Long) {
    HolmWgpuOhosBridge.destroyModel(handle)
}

@CName("holm_ohos_bridge_resize_model")
fun holmOhosBridgeResizeModel(handle: Long, width: Int, height: Int) {
    HolmWgpuOhosBridge.resizeModel(handle, width, height)
}

@CName("holm_ohos_bridge_tick_model_frame")
fun holmOhosBridgeTickModelFrame(
    handle: Long,
    sessionId: Long,
    aspect: Float,
    dtSecs: Float,
    playing: Int,
): Int =
    if (HolmWgpuOhosBridge.presentModelFrame(handle, sessionId, aspect, dtSecs, playing != 0)) 1 else 0

@CName("holm_ohos_bridge_upload_model_meshes")
fun holmOhosBridgeUploadModelMeshes(handle: Long, sessionId: Long): Int =
    if (HolmWgpuOhosBridge.uploadModelMeshes(handle, sessionId)) 1 else 0

@CName("holm_ohos_bridge_backend_id_model")
fun holmOhosBridgeBackendIdModel(handle: Long, buf: CPointer<ByteVar>?, cap: Int): UInt =
    writeCString(buf, cap, HolmWgpuOhosBridge.backendIdModel(handle))

@CName("holm_ohos_bridge_last_error_code_model")
fun holmOhosBridgeLastErrorCodeModel(handle: Long, buf: CPointer<ByteVar>?, cap: Int): UInt =
    writeCString(buf, cap, HolmWgpuOhosBridge.lastErrorCodeModel(handle))

@CName("holm_ohos_bridge_last_error_message_model")
fun holmOhosBridgeLastErrorMessageModel(handle: Long, buf: CPointer<ByteVar>?, cap: Int): UInt =
    writeCString(buf, cap, HolmWgpuOhosBridge.lastErrorMessageModel(handle))

// --- ADR 0019 Model session ---

@CName("holm_ohos_model_session_create")
fun holmOhosModelSessionCreate(): Long = HolmOhosModelBridge.createSession()

@CName("holm_ohos_model_session_destroy")
fun holmOhosModelSessionDestroy(sessionId: Long) {
    HolmOhosModelBridge.destroySession(sessionId)
}

@CName("holm_ohos_model_attach")
fun holmOhosModelAttach(sessionId: Long, contentId: CPointer<ByteVar>?, executor: CPointer<ByteVar>?) {
    HolmOhosModelBridge.attach(
        sessionId,
        contentId?.toKString().orEmpty(),
        executor?.toKString().orEmpty().ifBlank { "renderer-gl" },
    )
}

@CName("holm_ohos_model_set_executor")
fun holmOhosModelSetExecutor(sessionId: Long, executor: CPointer<ByteVar>?) {
    HolmOhosModelBridge.setExecutor(sessionId, executor?.toKString().orEmpty().ifBlank { "renderer-gl" })
}

@CName("holm_ohos_model_nudge")
fun holmOhosModelNudge(sessionId: Long, dx: Float, dy: Float) {
    HolmOhosModelBridge.nudgeOrbit(sessionId, dx, dy)
}

@CName("holm_ohos_model_is_ready")
fun holmOhosModelIsReady(sessionId: Long): Int =
    if (HolmOhosModelBridge.isReady(sessionId)) 1 else 0

@CName("holm_ohos_model_clear_error")
fun holmOhosModelClearError(sessionId: Long) {
    HolmOhosModelBridge.clearError(sessionId)
}

@CName("holm_ohos_model_last_error_code")
fun holmOhosModelLastErrorCode(sessionId: Long, buf: CPointer<ByteVar>?, cap: Int): UInt =
    writeCString(buf, cap, HolmOhosModelBridge.lastErrorCode(sessionId))

@CName("holm_ohos_model_last_error_message")
fun holmOhosModelLastErrorMessage(sessionId: Long, buf: CPointer<ByteVar>?, cap: Int): UInt =
    writeCString(buf, cap, HolmOhosModelBridge.lastErrorMessage(sessionId))

@CName("holm_ohos_model_peek_mesh")
fun holmOhosModelPeekMesh(sessionId: Long, vertFloats: CPointer<IntVar>?, indexCount: CPointer<IntVar>?) {
    val sizes = HolmOhosModelBridge.peekMeshSizes(sessionId)
    vertFloats?.set(0, sizes[0])
    indexCount?.set(0, sizes[1])
}

@CName("holm_ohos_model_copy_mesh")
fun holmOhosModelCopyMesh(
    sessionId: Long,
    verts: CPointer<FloatVar>?,
    vertCap: Int,
    indices: CPointer<IntVar>?,
    idxCap: Int,
    outVertFloats: CPointer<IntVar>?,
    outIndexCount: CPointer<IntVar>?,
    center3: CPointer<FloatVar>?,
    radius: CPointer<FloatVar>?,
    color4: CPointer<FloatVar>?,
    yaw: CPointer<FloatVar>?,
    pitch: CPointer<FloatVar>?,
): Int {
    val outSizes = IntArray(2)
    val center = FloatArray(3)
    val radiusArr = FloatArray(1)
    val color = FloatArray(4)
    val yawPitch = FloatArray(2)
    val need = HolmOhosModelBridge.peekMeshSizes(sessionId)
    if (need[0] <= 0 || need[1] <= 0) {
        outVertFloats?.set(0, 0)
        outIndexCount?.set(0, 0)
        return 0
    }
    if (verts == null || indices == null || vertCap < need[0] || idxCap < need[1]) {
        outVertFloats?.set(0, need[0])
        outIndexCount?.set(0, need[1])
        return -1
    }
    val vertArr = FloatArray(need[0])
    val idxArr = IntArray(need[1])
    val rc = HolmOhosModelBridge.copyMesh(sessionId, vertArr, idxArr, outSizes, center, radiusArr, color, yawPitch)
    outVertFloats?.set(0, outSizes[0])
    outIndexCount?.set(0, outSizes[1])
    if (rc != 1) return rc
    for (i in vertArr.indices) {
        verts[i] = vertArr[i]
    }
    for (i in idxArr.indices) {
        indices[i] = idxArr[i]
    }
    if (center3 != null) {
        center3[0] = center[0]
        center3[1] = center[1]
        center3[2] = center[2]
    }
    radius?.set(0, radiusArr[0])
    if (color4 != null) {
        color4[0] = color[0]
        color4[1] = color[1]
        color4[2] = color[2]
        color4[3] = color[3]
    }
    yaw?.set(0, yawPitch[0])
    pitch?.set(0, yawPitch[1])
    return 1
}

@CName("holm_ohos_model_fill_light")
fun holmOhosModelFillLight(sessionId: Long, out5: CPointer<FloatVar>?) {
    if (out5 == null) return
    val light = HolmOhosModelBridge.lightDirIntensity(sessionId)
    for (i in 0 until 5) {
        out5[i] = light.getOrElse(i) { 0f }
    }
}
