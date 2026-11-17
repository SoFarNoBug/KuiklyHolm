package com.jlj.kuiklybase.holm

import com.jlj.kmp.wgpu.WgpuRuntime

/**
 * @CreateDate: 2026/11/16
 * @Author: jlj
 * @Description: Holm Session 语义 JNI（setCustomFs/scene/liquid/playing）；不在 com.jlj.kmp.wgpu 产品面。
 */
object HolmAndroidJni {
    fun setCustomFs(handle: Long, fs: String): Boolean {
        if (handle == 0L || !WgpuRuntime.isLibraryLoaded()) return false
        return runCatching { nativeSetCustomFs(handle, fs) }.getOrDefault(false)
    }

    fun setScene(handle: Long, scene: String) {
        if (handle == 0L || !WgpuRuntime.isLibraryLoaded()) return
        runCatching { nativeSetScene(handle, scene) }
    }

    fun setLiquid(handle: Long, boost: Float, bend: Float) {
        if (handle == 0L || !WgpuRuntime.isLibraryLoaded()) return
        runCatching { nativeSetLiquid(handle, boost, bend) }
    }

    fun setPlaying(handle: Long, playing: Boolean) {
        if (handle == 0L || !WgpuRuntime.isLibraryLoaded()) return
        runCatching { nativeSetPlaying(handle, playing) }
    }

    private external fun nativeSetCustomFs(handle: Long, fs: String): Boolean
    private external fun nativeSetScene(handle: Long, scene: String)
    private external fun nativeSetLiquid(handle: Long, boost: Float, bend: Float)
    private external fun nativeSetPlaying(handle: Long, playing: Boolean)
}
