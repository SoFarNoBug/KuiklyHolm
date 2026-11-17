package com.jlj.kuiklybase.holm

import com.jlj.kmp.wgpu.Device
import com.jlj.kmp.wgpu.surface.FullscreenPass
import com.jlj.kmp.wgpu.surface.GpuSurface

/**
 * @CreateDate: 2026/11/16
 * @Author: jlj
 * @Description: Holm 全屏片元适配（H5）；薄包 [FullscreenPass]（ADR 0015）。
 */
class HolmFullscreenPass(
    val gpuSurface: GpuSurface,
) {
    private val _pass = FullscreenPass(gpuSurface)

    val device: Device get() = _pass.device

    fun resize(width: Int, height: Int) {
        _pass.resize(width, height)
    }

    fun setCustomFs(wgslFs: String): Boolean =
        _pass.setFragmentShader(wgslFs)

    fun setPlaying(playing: Boolean) {
        _pass.setPlaying(playing)
    }

    fun tick(dtSecs: Float): Boolean = _pass.tick(dtSecs)

    fun backendId(): String = _pass.backendId()

    fun lastErrorCode(): String = _pass.lastErrorCode()

    fun lastErrorMessage(): String = _pass.lastErrorMessage()

    fun drainErrors(): List<Pair<String, String>> = _pass.drainErrors()

    fun destroy() {
        _pass.destroy()
    }
}
