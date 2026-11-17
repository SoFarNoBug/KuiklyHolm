@file:OptIn(
    kotlinx.cinterop.ExperimentalForeignApi::class,
    kotlin.experimental.ExperimentalNativeApi::class,
)

package com.jlj.kuiklyholmui

import com.jlj.kuiklybase.holm.HolmWgpuOhosBridge
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CPointer
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

@CName("holm_ohos_bridge_set_liquid")
fun holmOhosBridgeSetLiquid(handle: Long, boost: Float, bend: Float) {
    HolmWgpuOhosBridge.setLiquid(handle, boost, bend)
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
