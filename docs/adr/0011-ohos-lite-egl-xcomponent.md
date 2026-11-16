# ADR 0011: 鸿蒙 Lite · 同 XComponent + EGL/GLES

- Status: Accepted
- Date: 2026-11-15

## Context

ADR 0010 已通鸿蒙 Runtime（XComponent + `libholm_xc` 外泵 + wgpu）；`executor=lite` 曾诚实 `LITE_PENDING`。审讯锁定补齐 Lite，与 Android/iOS DualTrack 对齐：同一岛可切 Lite GLES ↔ Runtime wgpu。

## Decision

1. **呈现面**：与 Runtime **共用同一 XComponent / `OHNativeWindow*`**；切轨时销毁 wgpu Session 或 EGL 上下文后再建另一轨（禁止两套同时占窗）。
2. **实现落点**：Lite EGL/GLES 在 **`libholm_xc.so`** 内与 Runtime 路径互斥；**不**并入 `libentry.so`；**不**新建独立 gles so。
3. **Shader 契约**：Attr `fragmentShader` 仍为 Shadertoy **body**；wrap + 全屏 vert 在 C++ 内嵌，与 `HolmShadertoyShell` / `HolmBuiltinShaders` **逐字对齐**（Kotlin 为 SoT；改 shell 须同步 C++，changelog 留痕）。
4. **帧泵**：复用 XComponent `OnFrame`；Lite → GLES draw + `eglSwapBuffers`；`maxFps` 跳帧语义与 Runtime 共用。
5. **分辨率**：`OH_NativeXComponent_GetXComponentSize` 像素为基线；**现行**由 Attr `renderScale`（默认 `1`）相对缩放（可 `SET_BUFFER_GEOMETRY`）。
6. **`onReady.backend`**：`gles2-ohos`。
7. **失败诚实**：空 body → `FRAGMENT_REQUIRED`；Lite 收到像 WGSL 的串 → `onError`（不静默编译）；EGL/编译失败 → 深蓝底 + `onError`；**禁止**用 GLES 冒充 wgpu，亦禁止 Runtime 失败时静默切 Lite。
8. **验收**：Demo `holm_runtime_m0` Lite 隧道出画 + Runtime↔Lite 互切；去掉 `LITE_PENDING`。

## Consequences

- 推翻 ADR 0010「本波 Lite=`LITE_PENDING`」；Runtime 决策仍以 0010 为准，Lite 以本 ADR 为准。
- C++ wrap 与 Kotlin SoT 存在同步义务（无 KN→NAPI 桥）。
- **2026-11-16**：降采样落地为可配置 `renderScale`（默认 `1`，不强制 1×）；包体优化仍可另单。
