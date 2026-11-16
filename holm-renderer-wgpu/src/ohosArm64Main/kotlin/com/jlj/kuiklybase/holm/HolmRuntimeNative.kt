@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.jlj.kuiklybase.holm

import com.jlj.kmp.wgpu.WgpuRuntime
import com.jlj.kmp.wgpu.ohosPlatform
import com.jlj.kmp.wgpu.surface.OhosSurfaces
import kotlinx.cinterop.COpaquePointer
import kotlin.concurrent.Volatile

/**
 * @CreateDate: 2026/11/16
 * @Author: jlj
 * @Description: Ohos Runtime 门面；Present 唯一入口为 [createFullscreenPass] → [HolmFullscreenPass]。
 */
object HolmRuntimeNative {
    @Volatile
    private var _presentable: Boolean = false

    fun isLibraryLoaded(): Boolean = WgpuRuntime.isLibraryLoaded()

    fun isM0Presentable(): Boolean = isLibraryLoaded() && _presentable

    fun isAvailable(): Boolean = isLibraryLoaded() && isM0Presentable()

    fun blockedMessage(): String {
        return if (!isLibraryLoaded()) {
            "DualTrack B BLOCKED: libholm_runtime.so missing (run holm-runtime/build-ohos.sh). No silent non-wgpu stack."
        } else if (!_presentable) {
            val detail = lastCreateError().trim()
            if (detail.isNotEmpty()) {
                "DualTrack B BLOCKED: Session/create_ohos failed: $detail. No silent GLES stand-in."
            } else {
                "DualTrack B BLOCKED: holm_runtime loaded but Session/create_ohos not ready. No silent GLES stand-in."
            }
        } else {
            "DualTrack B BLOCKED: holm_runtime unavailable."
        }
    }

    fun lastCreateError(): String = OhosSurfaces.lastCreateError()

    fun markPresentable(ok: Boolean) {
        _presentable = ok
    }

    fun abiVersion(): Int {
        if (!isLibraryLoaded()) return 0
        return runCatching { ohosPlatform().abiVersion() }.getOrDefault(0)
    }

    fun hasWgpu(): Int = if (WgpuRuntime.isAvailable()) 1 else 0

    fun createFullscreenPass(
        nativeWindow: COpaquePointer?,
        width: Int,
        height: Int,
    ): HolmFullscreenPass? {
        val gpu = OhosSurfaces.create(nativeWindow, width, height) ?: return null
        return HolmFullscreenPass(gpu)
    }
}
