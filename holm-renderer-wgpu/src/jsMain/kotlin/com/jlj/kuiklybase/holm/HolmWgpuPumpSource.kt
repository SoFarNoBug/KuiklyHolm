package com.jlj.kuiklybase.holm

/**
 * @CreateDate: 2026/11/13
 * @Author: jlj
 * @Description: H5 Runtime wgpu 泵帧（Blob Worker / OffscreenCanvas）；主线程只转发 prop。
 */
object HolmWgpuPumpSource {
    /**
     * Worker 脚本：importScripts 加载 kmp_wgpu_native 胶水，OffscreenCanvas 上跑 Session tick。
     * 缓冲尺寸与 Lite 一致（CSS 像素，不乘 DPR），默认 maxFps=60，perf 1s 上报一次。
     */
    val SCRIPT: String = """
(function (root) {
  function createHolmWgpuPump(send) {
    var canvas = null;
    var session = null;
    var scene = "";
    var customFs = "";
    var maxFps = 60;
    var intervalMs = 1000 / 60;
    var nextDue = 0;
    var playing = true;
    var rafId = 0;
    var timerId = 0;
    var lastNow = 0;
    var fpsWindowMs = 0;
    var frameCount = 0;
    var destroyed = false;

    function applyMaxFps(v) {
      var n = typeof v === "number" ? (v | 0) : 60;
      if (n < 0) n = 0;
      maxFps = n;
      intervalMs = maxFps <= 0 ? 0 : 1000 / maxFps;
      nextDue = 0;
    }

    var reportPerf = false;

    // maxFps>60：timer；0=不限走 rAF（对齐 Lite，配合 AutoNoVsync 吃高刷，避免 setTimeout(0) 空转）
    function useTimerPump() {
      return maxFps > 60;
    }

    function scheduleNext(now) {
      if (!playing || destroyed) return;
      if (useTimerPump()) {
        var t = typeof now === "number" ? now : performance.now();
        var delay = 0;
        if (maxFps > 0) {
          if (!nextDue) nextDue = t + intervalMs;
          delay = nextDue - t;
          if (delay < 0) delay = 0;
          if (delay > intervalMs) delay = intervalMs;
        }
        timerId = setTimeout(function () {
          timerId = 0;
          draw(performance.now());
        }, delay);
      } else {
        rafId = requestAnimationFrame(draw);
      }
    }

    function emitError(code, message) {
      send({ type: "error", code: code, message: String(message || "") });
    }

    function emitShaderCompiled(ok, compileMs) {
      send({
        type: "shaderCompiled",
        ok: !!ok,
        wrapMs: 0,
        compileMs: typeof compileMs === "number" ? compileMs : 0,
        executor: "renderer-wgpu"
      });
    }

    function nowMs() {
      return (typeof performance !== "undefined" && performance.now) ? performance.now() : Date.now();
    }

    /** @return 已投递条数；失败路径只走 drain，避免与 throw 双发 */
    function flushSessionErrors() {
      if (!session || typeof session.drainErrors !== "function") return 0;
      try {
        var pending = session.drainErrors();
        if (!pending || !pending.length) return 0;
        for (var i = 0; i < pending.length; i++) {
          var item = pending[i] || {};
          var code = item.code || "WGPU_VALIDATION";
          var message = item.message || "";
          emitError(code, message);
        }
        return pending.length;
      } catch (_) {
        return 0;
      }
    }

    function tickPerf(nowMs) {
      if (!reportPerf) return;
      if (!fpsWindowMs) {
        fpsWindowMs = nowMs;
        frameCount = 0;
        return;
      }
      frameCount += 1;
      var elapsed = nowMs - fpsWindowMs;
      if (elapsed >= 1000) {
        var fps = (frameCount * 1000) / elapsed;
        var frameMs = fps > 0.01 ? 1000 / fps : 0;
        send({ type: "perf", fps: fps, frameMs: frameMs });
        fpsWindowMs = nowMs;
        frameCount = 0;
      }
    }

    function applySize(w, h) {
      w = w | 0;
      h = h | 0;
      if (w < 1) w = 1;
      if (h < 1) h = 1;
      if (!canvas) return;
      if (canvas.width !== w || canvas.height !== h) {
        canvas.width = w;
        canvas.height = h;
      }
      if (session && typeof session.resize === "function") {
        session.resize(w >>> 0, h >>> 0);
      }
    }

    function draw(now) {
      rafId = 0;
      timerId = 0;
      if (destroyed || !session || !playing) return;
      var shouldSubmit = true;
      if (maxFps > 0) {
        if (!nextDue) nextDue = now;
        if (now < nextDue) {
          shouldSubmit = false;
        } else {
          nextDue += intervalMs;
          if (now - nextDue > intervalMs) nextDue = now;
        }
      }
      if (shouldSubmit) {
        if (!lastNow) lastNow = now;
        var dt = (now - lastNow) / 1000;
        if (dt < 0) dt = 0;
        if (dt > 0.05) dt = 0.05;
        lastNow = now;
        try {
          session.tick(dt);
          flushSessionErrors();
          tickPerf(now);
        } catch (e) {
          if (!flushSessionErrors()) {
            emitError("TICK", (e && e.message) ? e.message : String(e));
          }
          stop();
          return;
        }
      }
      if (playing && !destroyed) {
        scheduleNext(now);
      }
    }

    function start() {
      if (rafId || timerId || !playing || !session || destroyed) return;
      lastNow = 0;
      nextDue = 0;
      scheduleNext(performance.now());
    }

    function stop() {
      if (rafId) {
        cancelAnimationFrame(rafId);
        rafId = 0;
      }
      if (timerId) {
        clearTimeout(timerId);
        timerId = 0;
      }
    }

    return {
      init: function (msg) {
        canvas = msg.canvas;
        scene = msg.scene || "";
        customFs = typeof msg.customFs === "string" ? msg.customFs : "";
        applyMaxFps(typeof msg.maxFps === "number" ? msg.maxFps : 60);
        playing = msg.playing !== false;
        reportPerf = msg.reportPerf === true;
        var glueUrl = msg.glueUrl;
        var wasmUrl = msg.wasmUrl;
        if (!canvas) {
          emitError("NO_CANVAS", "OffscreenCanvas missing");
          return;
        }
        if (!glueUrl || !wasmUrl) {
          emitError("NO_WASM_URL", "glueUrl/wasmUrl missing");
          return;
        }
        try {
          importScripts(glueUrl);
        } catch (e) {
          emitError("IMPORT_SCRIPTS", (e && e.message) ? e.message : String(e));
          return;
        }
        var bindgen = (typeof wasm_bindgen === "function")
          ? wasm_bindgen
          : (typeof self !== "undefined" && typeof self.wasm_bindgen === "function" ? self.wasm_bindgen : null);
        if (!bindgen) {
          emitError("NO_BINDGEN", "wasm_bindgen missing after importScripts");
          return;
        }
        var w = typeof msg.w === "number" ? msg.w : 1;
        var h = typeof msg.h === "number" ? msg.h : 1;
        if (w < 1) w = 1;
        if (h < 1) h = 1;
        canvas.width = w;
        canvas.height = h;
        bindgen(wasmUrl)
          .then(function () {
            if (destroyed) return null;
            if (!bindgen.KmpWgpuSession || typeof bindgen.KmpWgpuSession.create !== "function") {
              throw new Error("KmpWgpuSession.create missing");
            }
            return bindgen.KmpWgpuSession.create(canvas);
          })
          .then(function (s) {
            if (destroyed) {
              try { if (s && s.destroy) s.destroy(); } catch (_) {}
              return;
            }
            if (!s) return;
            session = s;
            var compileStarted = 0;
            try {
              if (typeof session.setFragmentShader === "function") session.setFragmentShader("");
              if (customFs && typeof session.setFragmentShader === "function") {
                compileStarted = nowMs();
                session.setFragmentShader(customFs);
                emitShaderCompiled(true, nowMs() - compileStarted);
              } else {
                // 无片元：只清屏（ADR 0015）
                emitShaderCompiled(false, 0);
              }
              if (session && typeof session.drainErrors === "function") {
                try { session.drainErrors(); } catch (_) {}
              }
            } catch (e) {
              if (!flushSessionErrors()) {
                emitError("SHADER_CUSTOM", (e && e.message) ? e.message : String(e));
              }
              emitShaderCompiled(false, compileStarted ? (nowMs() - compileStarted) : 0);
              // setFragmentShader 失败：wasm 已停 playing；泵侧同步停
              playing = false;
              stop();
              try {
                if (session && typeof session.setPlaying === "function") session.setPlaying(false);
              } catch (_) {}
            }
            if (typeof session.setPlaying === "function") session.setPlaying(!!playing);
            var backend = "wgpu-m0";
            try {
              if (typeof session.backendId === "function") {
                backend = String(session.backendId() || backend);
              }
            } catch (_) {}
            fpsWindowMs = 0;
            frameCount = 0;
            send({ type: "ready", backend: backend, quality: "auto", scene: scene });
            if (playing) start();
          })
          .catch(function (e) {
            emitError("CREATE_SESSION", (e && e.message) ? e.message : String(e));
          });
      },
      set: function (msg) {
        if (typeof msg.reportPerf === "boolean") {
          reportPerf = msg.reportPerf;
        }
        if (typeof msg.maxFps === "number") {
          var next = msg.maxFps | 0;
          if (next < 0) next = 0;
          if (next !== maxFps) {
            applyMaxFps(next);
            // 切换 rAF/timer 泵模式须重启调度
            if (playing) {
              stop();
              start();
            }
          }
        }
      },
      resize: function (msg) {
        applySize(msg.w, msg.h);
      },
      scene: function (msg) {
        if (msg.scene) scene = msg.scene;
        if (typeof msg.customFs === "string") customFs = msg.customFs;
        if (!session) return;
        var compileStarted = 0;
        try {
          // 切换样本：先卸上一档，再装新；失败不残留旧特效
          if (typeof session.setFragmentShader === "function") session.setFragmentShader("");
          if (customFs && typeof session.setFragmentShader === "function") {
            compileStarted = nowMs();
            session.setFragmentShader(customFs);
            emitShaderCompiled(true, nowMs() - compileStarted);
          } else {
            emitShaderCompiled(false, 0);
          }
          // 成功换档：丢掉上一失败 pending，避免迟到 error 盖住新特效
          if (session && typeof session.drainErrors === "function") {
            try { session.drainErrors(); } catch (_) {}
          }
          // 上一档失败曾 stop；父级 playing=true 若已早于 scene 到达则此处自愈开泵
          if (playing) {
            try {
              if (typeof session.setPlaying === "function") session.setPlaying(true);
            } catch (_) {}
            start();
          }
        } catch (e) {
          if (!flushSessionErrors()) {
            emitError("SHADER_CUSTOM", (e && e.message) ? e.message : String(e));
          }
          emitShaderCompiled(false, compileStarted ? (nowMs() - compileStarted) : 0);
          playing = false;
          stop();
          try {
            if (session && typeof session.setPlaying === "function") session.setPlaying(false);
          } catch (_) {}
        }
      },
      playing: function (msg) {
        var next = msg.playing !== false;
        if (next === playing) return;
        playing = next;
        if (session && typeof session.setPlaying === "function") {
          session.setPlaying(!!playing);
        }
        if (playing) start();
        else stop();
      },
      destroy: function () {
        destroyed = true;
        stop();
        var s = session;
        session = null;
        canvas = null;
        try {
          if (s && typeof s.destroy === "function") s.destroy();
        } catch (_) {}
      }
    };
  }

  if (typeof document === "undefined") {
    var pump = null;
    root.onmessage = function (ev) {
      var msg = ev.data || {};
      if (msg.type === "init") {
        pump = createHolmWgpuPump(function (m) {
          root.postMessage(m);
        });
        pump.init(msg);
        return;
      }
      if (!pump) return;
      if (msg.type === "set") pump.set(msg);
      else if (msg.type === "resize") pump.resize(msg);
      else if (msg.type === "scene") pump.scene(msg);
      else if (msg.type === "playing") pump.playing(msg);
      else if (msg.type === "destroy") {
        pump.destroy();
        pump = null;
      }
    };
  }
})(typeof self !== "undefined" ? self : this);
""".trimIndent()
}
