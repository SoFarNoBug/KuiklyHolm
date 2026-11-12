package com.jlj.kuiklybase.holm

/**
 * @CreateDate: 2026/11/12
 * @Author: jlj
 * @Description: H5 WebGL 纯 JS 泵帧源码（Blob Worker / 主线程回退共用；不经 Kotlin RAF）。
 */
object HolmGlPumpSource {
    /**
     * 自执行脚本：Worker 内自动挂 onmessage；主线程 eval 后暴露 createHolmGlPump。
     */
    val SCRIPT: String = """
(function (root) {
  var CTX = {
    alpha: true,
    antialias: false,
    depth: false,
    stencil: false,
    powerPreference: "high-performance",
    failIfMajorPerformanceCaveat: false,
    preserveDrawingBuffer: false
  };

  function createHolmGlPump(send, backendLabel) {
    var canvas = null;
    var gl = null;
    var program = null;
    var buffer = null;
    var timeLoc = null;
    var iorLoc = null;
    var warpLoc = null;
    var posLoc = -1;
    var vertSrc = "";
    var fragSrc = "";
    var scene = "frost";
    var ior = 1.35;
    var warp = 0.05;
    var maxFps = 60;
    var intervalMs = 1000 / 60;
    var nextDue = 0;
    var playing = true;
    var dirtyIor = true;
    var dirtyWarp = true;
    var rafId = 0;
    var animTime = 0;
    var lastNow = 0;
    var fpsWindowMs = 0;
    var frameCount = 0;
    var viewW = 1;
    var viewH = 1;

    function applyMaxFps(v) {
      var n = typeof v === "number" ? (v | 0) : 60;
      if (n < 0) n = 0;
      maxFps = n;
      intervalMs = maxFps <= 0 ? 0 : 1000 / maxFps;
      nextDue = 0;
    }

    function emitError(code, message) {
      send({ type: "error", code: code, message: message });
    }

    function compile(type, source) {
      var sh = gl.createShader(type);
      if (!sh) return null;
      gl.shaderSource(sh, source);
      gl.compileShader(sh);
      if (!gl.getShaderParameter(sh, gl.COMPILE_STATUS)) {
        var info = gl.getShaderInfoLog(sh) || "compile failed";
        gl.deleteShader(sh);
        emitError("SHADER_COMPILE", info);
        return null;
      }
      return sh;
    }

    function ensureBuffer() {
      if (buffer) return;
      buffer = gl.createBuffer();
      gl.bindBuffer(gl.ARRAY_BUFFER, buffer);
      gl.bufferData(
        gl.ARRAY_BUFFER,
        new Float32Array([-1, -1, 1, -1, -1, 1, 1, 1]),
        gl.STATIC_DRAW
      );
    }

    function rebuild() {
      if (!gl || !vertSrc || !fragSrc) return false;
      var vs = compile(gl.VERTEX_SHADER, vertSrc);
      var fs = compile(gl.FRAGMENT_SHADER, fragSrc);
      if (!vs || !fs) return false;
      var prog = gl.createProgram();
      if (!prog) return false;
      gl.attachShader(prog, vs);
      gl.attachShader(prog, fs);
      gl.linkProgram(prog);
      if (!gl.getProgramParameter(prog, gl.LINK_STATUS)) {
        emitError("SHADER_LINK", gl.getProgramInfoLog(prog) || "link failed");
        return false;
      }
      if (program) gl.deleteProgram(program);
      program = prog;
      timeLoc = gl.getUniformLocation(program, "u_time");
      iorLoc = gl.getUniformLocation(program, "u_ior");
      warpLoc = gl.getUniformLocation(program, "u_warp");
      posLoc = gl.getAttribLocation(program, "a_position");
      gl.bindBuffer(gl.ARRAY_BUFFER, buffer);
      gl.enableVertexAttribArray(posLoc);
      gl.vertexAttribPointer(posLoc, 2, gl.FLOAT, false, 0, 0);
      dirtyIor = true;
      dirtyWarp = true;
      return true;
    }

    function applySize(w, h) {
      w = w | 0;
      h = h | 0;
      if (w < 1) w = 1;
      if (h < 1) h = 1;
      viewW = w;
      viewH = h;
      if (!canvas) return;
      if (canvas.width !== w || canvas.height !== h) {
        canvas.width = w;
        canvas.height = h;
      }
      if (gl) gl.viewport(0, 0, w, h);
    }

    function tickPerf(nowMs) {
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

    function draw(now) {
      rafId = 0;
      if (!gl || !program || !playing) return;
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
        animTime += dt;
        gl.clearColor(0.05, 0.07, 0.1, 1);
        gl.clear(gl.COLOR_BUFFER_BIT);
        gl.useProgram(program);
        if (timeLoc) {
          gl.uniform1f(timeLoc, animTime);
        }
        if (dirtyIor && iorLoc) {
          gl.uniform1f(iorLoc, ior);
          dirtyIor = false;
        }
        if (dirtyWarp && warpLoc) {
          gl.uniform1f(warpLoc, warp);
          dirtyWarp = false;
        }
        gl.drawArrays(gl.TRIANGLE_STRIP, 0, 4);
        tickPerf(now);
      }
      if (playing) {
        rafId = requestAnimationFrame(draw);
      }
    }

    function start() {
      if (rafId || !playing || !gl || !program) return;
      rafId = requestAnimationFrame(draw);
    }

    function stop() {
      if (rafId) {
        cancelAnimationFrame(rafId);
        rafId = 0;
      }
    }

    return {
      init: function (msg) {
        canvas = msg.canvas;
        vertSrc = msg.vert || "";
        fragSrc = msg.frag || "";
        scene = msg.scene || "frost";
        ior = typeof msg.ior === "number" ? msg.ior : 1.35;
        warp = typeof msg.warp === "number" ? msg.warp : 0.05;
        applyMaxFps(typeof msg.maxFps === "number" ? msg.maxFps : 60);
        playing = msg.playing !== false;
        dirtyIor = true;
        dirtyWarp = true;
        gl = canvas.getContext("webgl", CTX) || canvas.getContext("experimental-webgl", CTX);
        if (!gl) {
          emitError("NO_WEBGL", "WebGL unavailable");
          return;
        }
        ensureBuffer();
        applySize(msg.w, msg.h);
        if (!rebuild()) return;
        animTime = 0;
        lastNow = 0;
        fpsWindowMs = 0;
        frameCount = 0;
        send({
          type: "ready",
          backend: backendLabel,
          quality: "auto",
          scene: scene
        });
        if (playing) start();
      },
      set: function (msg) {
        if (typeof msg.ior === "number" && msg.ior !== ior) {
          ior = msg.ior;
          dirtyIor = true;
        }
        if (typeof msg.warp === "number" && msg.warp !== warp) {
          warp = msg.warp;
          dirtyWarp = true;
        }
        if (typeof msg.maxFps === "number") {
          var next = msg.maxFps | 0;
          if (next < 0) next = 0;
          if (next !== maxFps) applyMaxFps(next);
        }
      },
      resize: function (msg) {
        applySize(msg.w, msg.h);
      },
      scene: function (msg) {
        if (msg.frag) fragSrc = msg.frag;
        if (msg.vert) vertSrc = msg.vert;
        if (msg.scene) scene = msg.scene;
        rebuild();
      },
      playing: function (msg) {
        var next = msg.playing !== false;
        if (next === playing) return;
        playing = next;
        if (playing) start();
        else stop();
      },
      destroy: function () {
        stop();
        if (gl && program) {
          gl.deleteProgram(program);
        }
        program = null;
        gl = null;
        buffer = null;
        canvas = null;
      }
    };
  }

  root.createHolmGlPump = createHolmGlPump;

  if (typeof document === "undefined") {
    var pump = null;
    root.onmessage = function (ev) {
      var msg = ev.data || {};
      if (msg.type === "init") {
        pump = createHolmGlPump(function (m) {
          root.postMessage(m);
        }, "webgl-worker");
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
