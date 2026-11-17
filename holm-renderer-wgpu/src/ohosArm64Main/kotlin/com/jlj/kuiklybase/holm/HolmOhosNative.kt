package com.jlj.kuiklybase.holm

import com.jlj.kmp.wgpu.ohosPlatform

/**
 * @CreateDate: 2026/11/16
 * @Author: jlj
 * @Description: Ohos Present 语义门面 → OhosWgpuPlatform（ADR 0015；无 scene/liquid）。
 */
object HolmOhosNative {
    fun setCustomFs(handle: Long, fs: String): Boolean =
        ohosPlatform().setFragmentShader(handle, fs)

    fun setPlaying(handle: Long, playing: Boolean) {
        ohosPlatform().setPlaying(handle, playing)
    }
}
