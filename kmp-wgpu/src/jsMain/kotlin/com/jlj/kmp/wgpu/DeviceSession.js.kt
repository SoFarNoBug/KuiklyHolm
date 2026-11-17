package com.jlj.kmp.wgpu

/**
 * @CreateDate: 2026/11/16
 * @Author: jlj
 * @Description: H5 Device Session 能力块 → wasm。成败只信 Result/throw，不用 sticky lastError 否决成功。
 */

actual fun Device.sessionTick(dtSecs: Float): Boolean {
    return try {
        js(
            """
            (function(s, dt){
              if (!s || typeof s.tick !== 'function') return false;
              s.tick(dt);
              return true;
            })
            """,
        ).unsafeCast<(Any?, Float) -> Boolean>()(handle, dtSecs)
    } catch (_: Throwable) {
        false
    }
}

actual fun Device.sessionResize(width: Int, height: Int) {
    js(
        "(function(s,w,h){ if (s && typeof s.resize === 'function') s.resize(w,h); })",
    ).unsafeCast<(Any?, Int, Int) -> Unit>()(handle, width.coerceAtLeast(1), height.coerceAtLeast(1))
}

actual fun Device.sessionBackendId(): String {
    return try {
        js(
            """
            (function(s){
              if (s && typeof s.backendId === 'function') return String(s.backendId() || 'wgpu-m0');
              return 'wgpu-m0';
            })
            """,
        ).unsafeCast<(Any?) -> String>()(handle)
    } catch (_: Throwable) {
        "wgpu-m0"
    }
}

actual fun Device.sessionLastErrorCode(): String {
    return try {
        js(
            "(function(s){ return (s && typeof s.lastErrorCode === 'function') ? String(s.lastErrorCode()||'') : ''; })",
        ).unsafeCast<(Any?) -> String>()(handle)
    } catch (_: Throwable) {
        ""
    }
}

actual fun Device.sessionLastErrorMessage(): String {
    return try {
        js(
            "(function(s){ return (s && typeof s.lastErrorMessage === 'function') ? String(s.lastErrorMessage()||'') : ''; })",
        ).unsafeCast<(Any?) -> String>()(handle)
    } catch (_: Throwable) {
        ""
    }
}

actual fun Device.sessionDrainErrors(): List<Pair<String, String>> {
    return try {
        val flat = js(
            """
            (function(s){
              if (!s || typeof s.drainErrors !== 'function') return [];
              var arr = s.drainErrors();
              if (!arr) return [];
              var out = [];
              for (var i = 0; i < arr.length; i++) {
                var item = arr[i];
                if (item && typeof item === 'object') {
                  out.push(String(item.code || ''));
                  out.push(String(item.message || ''));
                } else {
                  out.push(String(item));
                }
              }
              return out;
            })
            """,
        ).unsafeCast<(Any?) -> Array<String>>()(handle)
        val out = ArrayList<Pair<String, String>>(flat.size / 2)
        var i = 0
        while (i + 1 < flat.size) {
            out.add(flat[i] to flat[i + 1])
            i += 2
        }
        out
    } catch (_: Throwable) {
        emptyList()
    }
}
