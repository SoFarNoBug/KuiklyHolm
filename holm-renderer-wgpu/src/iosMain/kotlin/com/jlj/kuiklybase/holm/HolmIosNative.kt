package com.jlj.kuiklybase.holm

import com.jlj.kmp.wgpu.iosPlatform

/**
 * @CreateDate: 2026/11/16
 * @Author: jlj
 * @Description: iOS Present 语义门面 → IosWgpuPlatform（ADR 0015；无 scene/liquid）。
 */
object HolmIosNative {
    fun setCustomFs(handle: Long, fs: String): Boolean =
        iosPlatform().setFragmentShader(handle, fs)

    fun setPlaying(handle: Long, playing: Boolean) {
        iosPlatform().setPlaying(handle, playing)
    }
}
