package com.jlj.kuiklybase.holm

import android.view.Surface
import com.jlj.kmp.wgpu.Device
import com.jlj.kmp.wgpu.WgpuRuntime
import com.jlj.kmp.wgpu.androidPlatform
import com.jlj.kmp.wgpu.sessionBackendId
import com.jlj.kmp.wgpu.sessionDrainErrors
import com.jlj.kmp.wgpu.sessionLastErrorCode
import com.jlj.kmp.wgpu.sessionLastErrorMessage
import com.jlj.kmp.wgpu.sessionResize
import com.jlj.kmp.wgpu.sessionTick
import com.jlj.kmp.wgpu.surface.AndroidSurfaces

/**
 * @CreateDate: 2026/11/12
 * @Author: jlj
 * @Description: Android Runtime 门面 → kmp-wgpu-surface；Holm 语义经 AndroidWgpuPlatform。
 */
object HolmRuntimeNative {
    @Volatile
    private var _presentable: Boolean = false

    fun isLibraryLoaded(): Boolean = WgpuRuntime.isLibraryLoaded()

    fun isM0Presentable(): Boolean = isLibraryLoaded() && _presentable

    fun isAvailable(): Boolean = isLibraryLoaded() && isM0Presentable()

    fun blockedMessage(): String {
        return if (!isLibraryLoaded()) {
            "DualTrack B BLOCKED: libholm_runtime.so missing (run holm-runtime/build-android.ps1). No silent non-wgpu stack."
        } else if (!_presentable) {
            val detail = lastCreateError().trim()
            if (detail.isNotEmpty()) {
                "DualTrack B BLOCKED: Session/Surface create failed: $detail. No silent GLES stand-in."
            } else {
                "DualTrack B BLOCKED: holm_runtime loaded but Session/Surface not ready. No silent GLES stand-in."
            }
        } else {
            "DualTrack B BLOCKED: holm_runtime unavailable."
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

    fun createSession(surface: Surface, width: Int, height: Int): Long {
        val gpu = AndroidSurfaces.create(surface, width, height) ?: return 0L
        return gpu.device.handle
    }

    fun createFullscreenPass(surface: Surface, width: Int, height: Int): HolmFullscreenPass? {
        val gpu = AndroidSurfaces.create(surface, width, height) ?: return null
        return HolmFullscreenPass(gpu)
    }

    fun destroySession(handle: Long) {
        if (handle == 0L) return
        Device.wrap(handle, androidPlatform()).destroy()
    }

    fun resize(handle: Long, width: Int, height: Int) {
        if (handle == 0L) return
        Device.wrap(handle, androidPlatform()).sessionResize(width, height)
    }

    fun setScene(handle: Long, scene: String) {
        if (handle == 0L) return
        androidPlatform().setScene(handle, scene)
    }

    fun setCustomFs(handle: Long, fs: String): Boolean {
        if (handle == 0L) return false
        return androidPlatform().setCustomFs(handle, fs)
    }

    fun lastErrorCode(handle: Long): String {
        if (handle == 0L) return ""
        return Device.wrap(handle, androidPlatform()).sessionLastErrorCode()
    }

    fun lastErrorMessage(handle: Long): String {
        if (handle == 0L) return ""
        return Device.wrap(handle, androidPlatform()).sessionLastErrorMessage()
    }

    fun drainErrors(handle: Long): List<Pair<String, String>> {
        if (handle == 0L) return emptyList()
        return Device.wrap(handle, androidPlatform()).sessionDrainErrors()
    }

    fun setLiquid(handle: Long, boost: Float, bend: Float) {
        if (handle == 0L) return
        androidPlatform().setLiquid(handle, boost, bend)
    }

    fun setPlaying(handle: Long, playing: Boolean) {
        if (handle == 0L) return
        androidPlatform().setPlaying(handle, playing)
    }

    fun tick(handle: Long, dtSecs: Float): Boolean {
        if (handle == 0L) return false
        return Device.wrap(handle, androidPlatform()).sessionTick(dtSecs)
    }

    fun backendId(handle: Long): String {
        if (handle == 0L) return HolmExecutor.BACKEND_WGPU_BLOCKED
        return Device.wrap(handle, androidPlatform()).sessionBackendId()
    }
}
