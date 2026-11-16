package com.jlj.kuiklybase.holm

import com.jlj.kmp.wgpu.HolmSessionBindings

/**
 * @CreateDate: 2026/11/16
 * @Author: jlj
 * @Description: Holm Session 语义门面；转发 [HolmSessionBindings]（对标 Android HolmAndroidJni / iOS HolmIosNative）。
 */
object HolmOhosNative {
    fun setCustomFs(handle: Long, fs: String): Boolean =
        HolmSessionBindings.setCustomFs(handle, fs)

    fun setScene(handle: Long, scene: String) {
        HolmSessionBindings.setScene(handle, scene)
    }

    fun setLiquid(handle: Long, boost: Float, bend: Float) {
        HolmSessionBindings.setLiquid(handle, boost, bend)
    }

    fun setPlaying(handle: Long, playing: Boolean) {
        HolmSessionBindings.setPlaying(handle, playing)
    }
}
