package com.jlj.kuiklybase.holm

import org.w3c.dom.HTMLCanvasElement

/**
 * @CreateDate: 2026/11/12
 * @Author: jlj
 * @Description: H5 holm-runtime wasm 探测与 Session 泵帧；index.html 预载 window.__holmRuntime。
 */
object HolmRuntimeNative {
    fun isLibraryLoaded(): Boolean {
        return js(
            "typeof window !== 'undefined' && !!window.__holmRuntime && window.__holmRuntime.ready === true",
        ).unsafeCast<Boolean>()
    }

    fun isLoadPending(): Boolean {
        return js(
            """
            (typeof window !== 'undefined' && !!window.__holmRuntime
              && window.__holmRuntime.ready !== true
              && !window.__holmRuntime.error)
            """,
        ).unsafeCast<Boolean>()
    }

    fun isM0Presentable(): Boolean {
        if (!isLibraryLoaded()) return false
        return js(
            "typeof window.__holmRuntime.hasWgpu === 'function' && window.__holmRuntime.hasWgpu() === 1",
        ).unsafeCast<Boolean>()
    }

    fun isAvailable(): Boolean = isLibraryLoaded() && isM0Presentable()

    fun blockedMessage(): String {
        val loadErr = js(
            "typeof window !== 'undefined' && window.__holmRuntime && window.__holmRuntime.error",
        ).unsafeCast<String?>()
        return if (!loadErr.isNullOrBlank()) {
            "DualTrack B BLOCKED: holm-runtime wasm load failed: $loadErr"
        } else if (!isLibraryLoaded()) {
            "DualTrack B BLOCKED: holm-runtime wasm not loaded (build holm-runtime/build-wasm.ps1). No silent non-wgpu stack."
        } else {
            "DualTrack B BLOCKED: holm-runtime wasm loaded but has_wgpu=0. No silent GLES stand-in."
        }
    }

    /**
     * 创建泵帧 Session；成功回调 (session, backendId)。
     */
    fun createSession(
        canvas: HTMLCanvasElement,
        onSuccess: (session: dynamic, backend: String) -> Unit,
        onError: (message: String) -> Unit,
    ) {
        if (!isAvailable()) {
            onError(blockedMessage())
            return
        }
        val ok = { session: dynamic, backend: String -> onSuccess(session, backend) }
        val err = { message: String -> onError(message) }
        js(
            """
            (function(canvas, ok, err) {
              var rt = window.__holmRuntime;
              if (!rt || typeof rt.createSession !== 'function') {
                err('holm-runtime createSession missing');
                return;
              }
              // 与 Lite 一致：backing store = CSS 像素（不乘 DPR），避免 Runtime 默认可视像素数翻 4~9 倍
              var cssW = Math.max(1, canvas.clientWidth | 0);
              var cssH = Math.max(1, canvas.clientHeight | 0);
              canvas.width = cssW;
              canvas.height = cssH;
              Promise.resolve(rt.createSession(canvas)).then(
                function(session) {
                  var backend = 'wgpu-m0';
                  try {
                    if (session && typeof session.backendId === 'function') {
                      backend = String(session.backendId() || backend);
                    }
                  } catch (e) {}
                  ok(session, backend);
                },
                function(e) {
                  var msg = (e && e.message) ? e.message : String(e);
                  err(msg);
                }
              );
            })
            """,
        ).unsafeCast<
            (
                HTMLCanvasElement,
                (dynamic, String) -> Unit,
                (String) -> Unit,
            ) -> Unit,
            >()(canvas, ok, err)
    }

    fun sessionSetScene(session: dynamic, scene: String) {
        if (session == null) return
        js(
            "(function(s, scene){ if (s && typeof s.setScene === 'function') s.setScene(scene); })",
        ).unsafeCast<(dynamic, String) -> Unit>()(session, scene)
    }

    /**
     * 设置自定义 WGSL 片元（空串清除）。返回 false 表示调用抛错或 API 缺失。
     */
    fun sessionSetCustomFs(session: dynamic, wgslFs: String): Boolean {
        if (session == null) return false
        return try {
            js(
                """
                (function(s, fs) {
                  if (!s || typeof s.setCustomFs !== 'function') return false;
                  s.setCustomFs(fs || "");
                  return true;
                })
                """,
            ).unsafeCast<(dynamic, String) -> Boolean>()(session, wgslFs)
        } catch (t: Throwable) {
            false
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
        if (session == null) return
        js(
            "(function(s, w, h){ if (s && typeof s.resize === 'function') s.resize(w >>> 0, h >>> 0); })",
        ).unsafeCast<(dynamic, Int, Int) -> Unit>()(session, width, height)
    }

    fun sessionTick(session: dynamic, dtSecs: Float): Boolean {
        if (session == null) return false
        return try {
            js(
                """
                (function(s, dt) {
                  if (!s || typeof s.tick !== 'function') return false;
                  s.tick(dt);
                  return true;
                })
                """,
            ).unsafeCast<(dynamic, Float) -> Boolean>()(session, dtSecs)
        } catch (t: Throwable) {
            false
        }
    }

    fun sessionDestroy(session: dynamic) {
        if (session == null) return
        js(
            """
            (function(s) {
              try {
                if (s && typeof s.destroy === 'function') s.destroy();
              } catch (e) {}
            })
            """,
        ).unsafeCast<(dynamic) -> Unit>()(session)
    }
}
