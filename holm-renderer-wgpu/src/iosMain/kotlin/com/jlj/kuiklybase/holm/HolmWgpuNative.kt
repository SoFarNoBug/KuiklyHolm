@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.jlj.kuiklybase.holm

import com.jlj.kmp.wgpu.WgpuRuntime
import com.jlj.kmp.wgpu.iosPlatform
import com.jlj.kmp.wgpu.surface.IosSurfaces
import kotlinx.cinterop.COpaquePointer
import kotlin.concurrent.Volatile

/**
 * @CreateDate: 2026/11/16
 * @Author: jlj
 * @Description: iOS Runtime 门面；Present 唯一入口为 [createFullscreenPass] → [HolmFullscreenPass]。
 */
object HolmWgpuNative {
    @Volatile
    private var _presentable: Boolean = false

    fun isLibraryLoaded(): Boolean = WgpuRuntime.isLibraryLoaded()

    fun isM0Presentable(): Boolean = isLibraryLoaded() && _presentable

    fun isAvailable(): Boolean = isLibraryLoaded() && isM0Presentable()

    fun blockedMessage(): String {
        return if (!isLibraryLoaded()) {
            "DualTrack B BLOCKED: KmpWgpuNative.xcframework missing (run kmp-wgpu-native/build-ios.sh). No silent non-wgpu stack."
        } else if (!_presentable) {
            val detail = lastCreateError().trim()
            if (detail.isNotEmpty()) {
                "DualTrack B BLOCKED: Session/Metal create failed: $detail. No silent GLES stand-in."
            } else {
                "DualTrack B BLOCKED: kmp_wgpu_native linked but Session/Metal not ready. No silent GLES stand-in."
            }
        } else {
            "DualTrack B BLOCKED: kmp_wgpu_native unavailable."
        }
    }

    fun lastCreateError(): String = IosSurfaces.lastCreateError()

    fun markPresentable(ok: Boolean) {
        _presentable = ok
    }

    fun abiVersion(): Int {
        if (!isLibraryLoaded()) return 0
        return runCatching { iosPlatform().abiVersion() }.getOrDefault(0)
    }

    fun hasWgpu(): Int = if (WgpuRuntime.isAvailable()) 1 else 0

    fun createFullscreenPass(
        metalLayer: COpaquePointer?,
        width: Int,
        height: Int,
    ): HolmFullscreenPass? {
        val gpu = IosSurfaces.create(metalLayer, width, height) ?: return null
        return HolmFullscreenPass(gpu)
    }
}
