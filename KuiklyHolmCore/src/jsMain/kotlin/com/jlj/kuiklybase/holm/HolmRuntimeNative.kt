package com.jlj.kuiklybase.holm

import com.jlj.kmp.wgpu.Device
import com.jlj.kmp.wgpu.WgpuRuntime
import com.jlj.kmp.wgpu.isLoadPending as kmpIsLoadPending
import com.jlj.kmp.wgpu.jsPlatform
import com.jlj.kmp.wgpu.sessionBackendId
import com.jlj.kmp.wgpu.sessionLastErrorCode
import com.jlj.kmp.wgpu.sessionLastErrorMessage
import com.jlj.kmp.wgpu.sessionResize
import com.jlj.kmp.wgpu.sessionTick
import com.jlj.kmp.wgpu.surface.JsSurfaces
import org.w3c.dom.HTMLCanvasElement

/**
 * @CreateDate: 2026/11/12
 * @Author: jlj
 * @Description: H5 Runtime 门面 → kmp-wgpu-surface；Holm 语义直调 wasm（成败只信 throw）。
 */
object HolmRuntimeNative {
    fun isLibraryLoaded(): Boolean = WgpuRuntime.isLibraryLoaded()

    fun isLoadPending(): Boolean = kmpIsLoadPending()

    fun isM0Presentable(): Boolean = WgpuRuntime.isAvailable()

    fun isAvailable(): Boolean = WgpuRuntime.isAvailable()

    fun blockedMessage(): String {
        val msg = WgpuRuntime.blockedMessage()
        return msg.replace("kmp-wgpu BLOCKED:", "DualTrack B BLOCKED:")
    }

    fun createSession(
        canvas: HTMLCanvasElement,
        width: Int,
        height: Int,
        onSuccess: (session: dynamic, backend: String) -> Unit,
        onError: (message: String) -> Unit,
    ) {
        JsSurfaces.create(
            canvas = canvas,
            width = width,
            height = height,
            onSuccess = { gpu ->
                val session = gpu.device.handle
                val backend = gpu.device.sessionBackendId()
                onSuccess(session.asDynamic(), backend)
            },
            onError = onError,
        )
    }

    fun createFullscreenPass(
        canvas: HTMLCanvasElement,
        width: Int,
        height: Int,
        onSuccess: (HolmFullscreenPass) -> Unit,
        onError: (String) -> Unit,
    ) {
        JsSurfaces.create(canvas, width, height, onSuccess = { gpu ->
            onSuccess(HolmFullscreenPass(gpu))
        }, onError = onError)
    }

    private fun deviceOf(session: dynamic): Device? {
        if (session == null) return null
        return Device.wrap(session as Any, jsPlatform())
    }

    fun sessionSetScene(session: dynamic, scene: String) {
        if (session == null) return
        js(
            "(function(s, scene){ if (s && typeof s.setScene === 'function') s.setScene(scene); })",
        ).unsafeCast<(dynamic, String) -> Unit>()(session, scene)
    }

    /**
     * @return null 成功；非空为错误文案（须 fireError SHADER_CUSTOM）。
     * 成败只信 wasm throw，不用 sticky lastError。
     */
    fun sessionSetCustomFs(session: dynamic, wgslFs: String): String? {
        if (session == null) return "HolmRuntimeSession missing"
        return try {
            js(
                """
                (function(s, fs) {
                  if (!s || typeof s.setCustomFs !== 'function') {
                    throw new Error('setCustomFs missing');
                  }
                  s.setCustomFs(fs || "");
                  return null;
                })
                """,
            ).unsafeCast<(dynamic, String) -> String?>()(session, wgslFs)
        } catch (t: Throwable) {
            t.message?.takeIf { it.isNotBlank() } ?: t.toString()
        }
    }

    fun sessionDrainErrors(session: dynamic): Array<dynamic> {
        if (session == null) return emptyArray()
        return try {
            js(
                """
                (function(s) {
                  if (!s || typeof s.drainErrors !== 'function') return [];
                  var arr = s.drainErrors();
                  return arr || [];
                })
                """,
            ).unsafeCast<(dynamic) -> Array<dynamic>>()(session)
        } catch (_: Throwable) {
            emptyArray()
        }
    }

    fun sessionSetLiquid(session: dynamic, boost: Float, bend: Float) {
        if (session == null) return
        js(
            "(function(s, boost, bend){ if (s && typeof s.setLiquid === 'function') s.setLiquid(boost, bend); })",
        ).unsafeCast<(dynamic, Float, Float) -> Unit>()(session, boost, bend)
    }

    fun sessionSetPlaying(session: dynamic, playing: Boolean) {
        if (session == null) return
        js(
            "(function(s, p){ if (s && typeof s.setPlaying === 'function') s.setPlaying(!!p); })",
        ).unsafeCast<(dynamic, Boolean) -> Unit>()(session, playing)
    }

    fun sessionResize(session: dynamic, width: Int, height: Int) {
        deviceOf(session)?.sessionResize(width, height)
    }

    /**
     * @return null 成功；非空为错误文案。成败只信 throw。
     */
    fun sessionTick(session: dynamic, dtSecs: Float): String? {
        val device = deviceOf(session) ?: return "HolmRuntimeSession missing"
        return if (device.sessionTick(dtSecs)) {
            null
        } else {
            // tick 失败（throw→false）；可读 last 作文案，但不因 sticky last 单独否决成功帧
            device.sessionLastErrorMessage()
                .ifBlank { device.sessionLastErrorCode() }
                .ifBlank { "tick failed" }
        }
    }

    fun sessionDestroy(session: dynamic) {
        deviceOf(session)?.destroy()
    }
}
