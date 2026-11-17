# ADR 0010: 鸿蒙 Runtime · XComponent + 外泵 Session

- Status: Accepted
- Date: 2026-11-14
- Note: Lite 已由 [ADR 0011](./0011-ohos-lite-egl-xcomponent.md) 落地；下文「Lite=`LITE_PENDING`」为史实，不再作现行行为。

## Context

H5 / Android / iOS Runtime（wgpu Session + 宿主外泵 + WGSL）已通；鸿蒙仅有 `ohosApp` 空壳与 `HolmOhos` 目录 init，无 `KRHolmSurface` / XComponent 岛，`kuikly-dev-ohos` runbook 仍占位。审讯锁定：**先通鸿蒙 Runtime，再补 Lite**；形态对齐 ADR 0006/0009，并符合鸿蒙自绘惯例（壳拥有 XComponent/帧回调，引擎吃 `OHNativeWindow*`）。

## Decision

1. **顺序**：本波只验收 Runtime 隧道；`executor=renderer-gl` → 深蓝 + 诚实 `onError`（如 `LITE_PENDING`），**禁止** GLES 冒充 wgpu；Lite 片元后置（**现行**：见 ADR 0011）。
2. **呈现面**：ETS `KRHolmSurface`（`KuiklyRenderBaseView`，`viewName=KRHolmSurface`）内嵌 **`XComponent`（surface）**；Attr 对齐双轨（`executor` / `fragmentShader` / `maxFps` / `onReady` / `onError` / `onPerf`…）。
3. **泵与胶水**：**XComponent NAPI 回调**拥有生命周期 + `OnFrame` → 同步 `tick`；Rust **无内泵**。不把泵放到 Kotlin/Native。`maxFps`：`SetExpectedFrameRateRange` + 必要时跳帧；`0`=不限。
4. **ABI**：`kmp_wgpu_session_create_ohos(window: *mut c_void, w, h)`；其余 `destroy` / `resize` / `tick` / `set_*` 与 iOS 共用。ohos target 拉 **wgpu + Vulkan**。
5. **产物与 Present 路径（对齐 ADR 0008）**：
   - 脚本编 `libkmp_wgpu_native.so`（aarch64-ohos）；改 Rust 先跑脚本，**Hvigor 不自动编 Rust**；由 **`libshared.so`（KN / kmp-wgpu cinterop）** 持有。
   - 独立 NAPI 模块 `libholm_xc.so`：注册 Surface/OnFrame + **Lite EGL**；Runtime Present **经** `dlsym(holm_ohos_bridge_*)` → KN `HolmWgpuOhosBridge` → `HolmFullscreenPass` → `OhosWgpuPlatform`；**禁止**壳直调 `kmp_wgpu_session_*`。
6. **分辨率**：create/resize 以 `OH_NativeXComponent_GetXComponentSize` 的 Surface **像素**为基线；**现行**由 Attr `renderScale`（默认 `1`）相对基线缩放（见 2026-11-16 Consequences）。
7. **失败**：缺 so / `has_wgpu=0` / create·tick 失败 → `onError(BLOCKED)`，深蓝底；**禁止**静默 GLES 冒充。
8. **生命周期**：切 Lite / 卸岛 / SurfaceDestroyed → **UnregisterOnFrame** + Session `destroy`；新 Surface 再 `create`。

## Consequences

- 产品 DualTrack B 曾不含鸿蒙；本 ADR 开通鸿蒙 Runtime 尖刺/通面路径，鸿蒙仍可为 PARTIAL（真机 Vulkan 能力依赖设备）。
- 与 ADR 0009 同构：壳外泵 + C Session；差异在 XComponent/Vulkan/双 so，而非 Metal/xcframework。
- Demo `holm_dualtrack` 可共用；鸿蒙上 Lite 本波曾为负向诚实失败。
- **已落地（ADR 0011）**：同 XComponent + EGL/GLES Shadertoy，`backend=gles2-ohos`；与 Runtime 互斥切轨。
- **2026-11-15**：`kmp-wgpu-native` 全端统一 **wgpu 30.0.2**（不再 Ohos 独占）；`session_core_ohos` 双核心退役，单一 `session_core`；Ohos 仍依赖 Vulkan / `VK_OHOS_surface`。
- 可选后单：Android JNI 收拢到同一 C Session。
- **2026-11-16**：降采样改为可配置 `HolmSurface.renderScale`（默认 `1`=基线满像素；`(0,1]`；四端 Lite+Runtime）；库不强制 1×。
- **2026-11-16（三包对齐）**：Present 经 KN Bridge；切轨/start `requireProvider`；Lite 空片元首包 defer（对齐 iOS）。
