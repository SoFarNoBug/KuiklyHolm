package com.jlj.kuiklybase.holm

import com.jlj.kmp.wgpu.Device
import com.jlj.kmp.wgpu.WgpuRuntime
import com.jlj.kmp.wgpu.isLoadPending as kmpIsLoadPending
import com.jlj.kmp.wgpu.surface.JsSurfaces
import org.w3c.dom.HTMLCanvasElement

/**
 * @CreateDate: 2026/11/12
 * @Author: jlj
 * @Description: H5 Runtime 门面 → kmp-wgpu-surface；直调 wasm（成败只信 throw）。
 * Present 走 FullscreenPass / 动态 session；不再依赖 Device.sessionTick*。
 */
object HolmWgpuNative {
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
                val backend = backendIdOf(session)
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

    /** ADR 0020 · ModelPass（与 Fullscreen 分型；无 WebGPU → onError）。 */
    fun createModelPass(
        canvas: HTMLCanvasElement,
        width: Int,
        height: Int,
        onSuccess: (HolmModelPass) -> Unit,
        onError: (String) -> Unit,
    ) {
        HolmModelPass.create(canvas, width, height, onSuccess = onSuccess, onError = onError)
    }

    private fun deviceOf(session: dynamic): Device? {
        if (session == null) return null
        return Device.wrap(session as Any, com.jlj.kmp.wgpu.jsPlatform())
    }

    private fun backendIdOf(session: dynamic): String {
        return try {
            js(
                """
                (function(s){
                  if (s && typeof s.backendId === 'function') return String(s.backendId() || 'wgpu-m0');
                  return 'wgpu-m0';
                })
                """,
            ).unsafeCast<(dynamic) -> String>()(session)
        } catch (_: Throwable) {
            "wgpu-m0"
        }
    }

    /**
     * @return null 成功；非空为错误文案（须 fireError SHADER_CUSTOM）。
     * 成败只信 wasm throw，不用 sticky lastError。
     */
    fun sessionSetCustomFs(session: dynamic, wgslFs: String): String? {
        if (session == null) return "KmpWgpuSession missing"
        return try {
            js(
                """
                (function(s, fs) {
                  if (!s || typeof s.setFragmentShader !== 'function') {
                    throw new Error('setFragmentShader missing');
                  }
                  s.setFragmentShader(fs || "");
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

    fun sessionSetPlaying(session: dynamic, playing: Boolean) {
        if (session == null) return
        js(
            "(function(s, p){ if (s && typeof s.setPlaying === 'function') s.setPlaying(!!p); })",
        ).unsafeCast<(dynamic, Boolean) -> Unit>()(session, playing)
    }

    fun sessionResize(session: dynamic, width: Int, height: Int) {
        if (session == null) return
        val w = width.coerceAtLeast(1)
        val h = height.coerceAtLeast(1)
        js(
            "(function(s,w,h){ if (s && typeof s.resize === 'function') s.resize(w,h); })",
        ).unsafeCast<(dynamic, Int, Int) -> Unit>()(session, w, h)
    }

    /**
     * @return null 成功；非空为错误文案。成败只信 throw。
     */
    fun sessionTick(session: dynamic, dtSecs: Float): String? {
        if (session == null) return "KmpWgpuSession missing"
        return try {
            js(
                """
                (function(s, dt){
                  if (!s || typeof s.tick !== 'function') throw new Error('tick missing');
                  s.tick(dt);
                  return null;
                })
                """,
            ).unsafeCast<(dynamic, Float) -> String?>()(session, dtSecs)
        } catch (t: Throwable) {
            t.message?.takeIf { it.isNotBlank() } ?: t.toString()
        }
    }

    fun sessionDestroy(session: dynamic) {
        deviceOf(session)?.destroy()
    }
}
