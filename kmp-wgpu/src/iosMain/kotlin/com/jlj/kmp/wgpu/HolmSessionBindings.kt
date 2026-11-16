@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.jlj.kmp.wgpu

import holm.runtime.cinterop.holm_runtime_session_set_custom_fs
import holm.runtime.cinterop.holm_runtime_session_set_liquid
import holm.runtime.cinterop.holm_runtime_session_set_playing
import holm.runtime.cinterop.holm_runtime_session_set_scene

/**
 * @CreateDate: 2026/11/16
 * @Author: jlj
 * @Description: Holm Session 语义 FFI（非 [WgpuPlatform]/[IosWgpuPlatform] 产品面）。
 * 仅供 holm-renderer-wgpu [HolmIosNative] 调用；对标 Android HolmAndroidJni。
 */
object HolmSessionBindings {
    fun setCustomFs(handle: WgpuHandle, fs: String): Boolean {
        if (handle == 0L || !WgpuRuntime.isLibraryLoaded()) return false
        return runCatching {
            holm_runtime_session_set_custom_fs(handle.toULong(), fs) == 1u
        }.getOrDefault(false)
    }

    fun setScene(handle: WgpuHandle, scene: String) {
        if (handle == 0L || !WgpuRuntime.isLibraryLoaded()) return
        runCatching {
            holm_runtime_session_set_scene(handle.toULong(), scene)
        }
    }

    fun setLiquid(handle: WgpuHandle, boost: Float, bend: Float) {
        if (handle == 0L || !WgpuRuntime.isLibraryLoaded()) return
        runCatching {
            holm_runtime_session_set_liquid(handle.toULong(), boost, bend)
        }
    }

    fun setPlaying(handle: WgpuHandle, playing: Boolean) {
        if (handle == 0L || !WgpuRuntime.isLibraryLoaded()) return
        runCatching {
            holm_runtime_session_set_playing(handle.toULong(), if (playing) 1u else 0u)
        }
    }
}
