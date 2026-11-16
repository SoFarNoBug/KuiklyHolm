package com.jlj.kmp.wgpu.surface

import android.view.Surface
import com.jlj.kmp.wgpu.Device
import com.jlj.kmp.wgpu.WgpuRuntime
import com.jlj.kmp.wgpu.androidPlatform
import com.jlj.kmp.wgpu.sessionResize

/**
 * @CreateDate: 2026/11/16
 * @Author: jlj
 * @Description: Android Surface → Device + GpuSurface。
 */

object AndroidSurfaces {
    fun create(
        surface: Surface,
        width: Int,
        height: Int,
    ): GpuSurface? {
        WgpuRuntime.createInstance()
        val device = androidPlatform().createDeviceFromSurface(surface, width, height) ?: return null
        return GpuSurface(
            device = device,
            initialConfig = SurfaceConfiguration(width.coerceAtLeast(1), height.coerceAtLeast(1)),
        )
    }

    fun lastCreateError(): String = androidPlatform().lastCreateError()
}

internal actual fun resizePlatform(device: Device, width: Int, height: Int) {
    device.sessionResize(width, height)
}
