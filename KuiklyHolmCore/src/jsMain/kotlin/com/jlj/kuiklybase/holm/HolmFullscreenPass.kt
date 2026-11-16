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
 * @Description: Holm 全屏片元适配（H5）；Holm 语义直调 wasm Session，不进 kmp-wgpu 产品面。
 */
class HolmFullscreenPass(
    val gpuSurface: GpuSurface,
) {
    val device: Device get() = gpuSurface.device

    fun resize(width: Int, height: Int) {
        gpuSurface.configure(
            SurfaceConfiguration(
                width = width.coerceAtLeast(1),
                height = height.coerceAtLeast(1),
            ),
        )
    }

    fun setCustomFs(wgslFs: String): Boolean {
        return try {
            js(
                """
                (function(s, fs){
                  if (!s || typeof s.setCustomFs !== 'function') throw new Error('setCustomFs missing');
                  s.setCustomFs(fs || '');
                  return true;
                })
                """,
            ).unsafeCast<(Any?, String) -> Boolean>()(device.handle, wgslFs)
        } catch (_: Throwable) {
            false
        }
    }

    fun setScene(scene: String) {
        js(
            "(function(s, scene){ if (s && typeof s.setScene === 'function') s.setScene(scene); })",
        ).unsafeCast<(Any?, String) -> Unit>()(device.handle, scene)
    }

    fun setLiquid(boost: Float, bend: Float) {
        js(
            "(function(s,b,d){ if (s && typeof s.setLiquid === 'function') s.setLiquid(b,d); })",
        ).unsafeCast<(Any?, Float, Float) -> Unit>()(device.handle, boost, bend)
    }

    fun setPlaying(playing: Boolean) {
        js(
            "(function(s,p){ if (s && typeof s.setPlaying === 'function') s.setPlaying(!!p); })",
        ).unsafeCast<(Any?, Boolean) -> Unit>()(device.handle, playing)
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
