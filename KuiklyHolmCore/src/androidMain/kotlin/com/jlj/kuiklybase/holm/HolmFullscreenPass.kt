package com.jlj.kuiklybase.holm

import com.jlj.kmp.wgpu.Device
import com.jlj.kmp.wgpu.androidPlatform
import com.jlj.kmp.wgpu.sessionBackendId
import com.jlj.kmp.wgpu.sessionDrainErrors
import com.jlj.kmp.wgpu.sessionLastErrorCode
import com.jlj.kmp.wgpu.sessionLastErrorMessage
import com.jlj.kmp.wgpu.sessionTick
import com.jlj.kmp.wgpu.surface.GpuSurface
import com.jlj.kmp.wgpu.surface.SurfaceConfiguration

/**
 * @CreateDate: 2026/11/16
 * @Author: jlj
 * @Description: Holm 全屏片元适配（Android）；Holm 语义经 AndroidWgpuPlatform，不进 kmp-wgpu 产品面。
 */
class HolmFullscreenPass(
    val gpuSurface: GpuSurface,
) {
    val device: Device get() = gpuSurface.device

    private val _platform get() = androidPlatform()

    fun resize(width: Int, height: Int) {
        gpuSurface.configure(
            SurfaceConfiguration(
                width = width.coerceAtLeast(1),
                height = height.coerceAtLeast(1),
            ),
        )
    }

    fun setCustomFs(wgslFs: String): Boolean =
        _platform.setCustomFs(device.handle, wgslFs)

    fun setScene(scene: String) {
        _platform.setScene(device.handle, scene)
    }

    fun setLiquid(boost: Float, bend: Float) {
        _platform.setLiquid(device.handle, boost, bend)
    }

    fun setPlaying(playing: Boolean) {
        _platform.setPlaying(device.handle, playing)
    }

    fun tick(dtSecs: Float): Boolean = device.sessionTick(dtSecs)

    fun backendId(): String = device.sessionBackendId()

    fun lastErrorCode(): String = device.sessionLastErrorCode()

    fun lastErrorMessage(): String = device.sessionLastErrorMessage()

    fun drainErrors(): List<Pair<String, String>> = device.sessionDrainErrors()

    fun destroy() {
        device.destroy()
    }
}
