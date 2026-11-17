package com.jlj.kuiklybase.holm

import com.jlj.kmp.wgpu.Device
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
 * @Description: Holm 全屏片元适配（Ohos）；Present 唯一入口；Holm 语义经 HolmOhosNative。
 */
class HolmFullscreenPass(
    val gpuSurface: GpuSurface,
) {
    val device: Device get() = gpuSurface.device

    val handle: Long get() = device.handle

    fun resize(width: Int, height: Int) {
        gpuSurface.configure(
            SurfaceConfiguration(
                width = width.coerceAtLeast(1),
                height = height.coerceAtLeast(1),
            ),
        )
    }

    fun setCustomFs(wgslFs: String): Boolean =
        HolmOhosNative.setCustomFs(handle, wgslFs)

    fun setScene(scene: String) {
        HolmOhosNative.setScene(handle, scene)
    }

    fun setLiquid(boost: Float, bend: Float) {
        HolmOhosNative.setLiquid(handle, boost, bend)
    }

    fun setPlaying(playing: Boolean) {
        HolmOhosNative.setPlaying(handle, playing)
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
