package com.jlj.kuiklybase.holm

import android.view.Surface
import com.jlj.kmp.wgpu.WgpuRuntime
import com.jlj.kmp.wgpu.androidPlatform
import com.jlj.kmp.wgpu.surface.AndroidSurfaces

/**
 * @CreateDate: 2026/11/12
 * @Author: jlj
 * @Description: Android Runtime 门面；Present 唯一入口为 [createFullscreenPass] → [HolmFullscreenPass]。
 */
object HolmWgpuNative {
    @Volatile
    private var _presentable: Boolean = false

    fun isLibraryLoaded(): Boolean = WgpuRuntime.isLibraryLoaded()

    fun isM0Presentable(): Boolean = isLibraryLoaded() && _presentable

    fun isAvailable(): Boolean = isLibraryLoaded() && isM0Presentable()

    fun blockedMessage(): String {
        return if (!isLibraryLoaded()) {
            "DualTrack B BLOCKED: libkmp_wgpu_native.so missing (run kmp-wgpu-native/build-android.ps1). No silent non-wgpu stack."
        } else if (!_presentable) {
            val detail = lastCreateError().trim()
            if (detail.isNotEmpty()) {
                "DualTrack B BLOCKED: Session/Surface create failed: $detail. No silent GLES stand-in."
            } else {
                "DualTrack B BLOCKED: kmp_wgpu_native loaded but Session/Surface not ready. No silent GLES stand-in."
            }
        } else {
            "DualTrack B BLOCKED: kmp_wgpu_native unavailable."
        }
    }

    fun lastCreateError(): String = AndroidSurfaces.lastCreateError()

    fun markPresentable(ok: Boolean) {
        _presentable = ok
    }

    fun abiVersion(): Int {
        if (!isLibraryLoaded()) return 0
        return runCatching { androidPlatform().abiVersion() }.getOrDefault(0)
    }

    fun hasWgpu(): Int = if (WgpuRuntime.isAvailable()) 1 else 0

    fun createFullscreenPass(surface: Surface, width: Int, height: Int): HolmFullscreenPass? {
        val gpu = AndroidSurfaces.create(surface, width, height) ?: return null
        return HolmFullscreenPass(gpu)
    }

    fun createModelPass(surface: Surface, width: Int, height: Int): HolmModelPass? =
        HolmModelPass.create(surface, width, height)
}
