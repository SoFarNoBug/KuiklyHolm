# ADR 0010: 鸿蒙 Runtime · XComponent + 外泵 Session

- Status: Accepted
- Date: 2026-11-14
- Note: Lite 已由 [ADR 0011](./0011-ohos-lite-egl-xcomponent.md) 落地；下文「Lite=`LITE_PENDING`」为史实，不再作现行行为。

## Context

H5 / Android / iOS Runtime（wgpu Session + 宿主外泵 + WGSL）已通；鸿蒙仅有 `ohosApp` 空壳与 `HolmOhos` 目录 init，无 `KRHolmSurface` / XComponent 岛，`kuikly-dev-ohos` runbook 仍占位。审讯锁定：**先通鸿蒙 Runtime，再补 Lite**；形态对齐 ADR 0006/0009，并符合鸿蒙自绘惯例（壳拥有 XComponent/帧回调，引擎吃 `OHNativeWindow*`）。

## Decision

1. **顺序**：本波只验收 Runtime 隧道；`executor=lite` → 深蓝 + 诚实 `onError`（如 `LITE_PENDING`），**禁止** GLES 冒充 wgpu；Lite 片元后置（**现行**：见 ADR 0011）。
2. **呈现面**：ETS `KRHolmSurface`（`KuiklyRenderBaseView`，`viewName=KRHolmSurface`）内嵌 **`XComponent`（surface）**；Attr 对齐双轨（`executor` / `fragmentShader` / `maxFps` / `onReady` / `onError` / `onPerf`…）。
3. **泵与胶水**：**XComponent NAPI 回调**拥有生命周期 + `OnFrame` → 同步 C `tick`；Rust **无内泵**。不把泵放到 Kotlin/Native。`maxFps`：`SetExpectedFrameRateRange` + 必要时跳帧；`0`=不限。
4. **ABI**：新增 `holm_runtime_session_create_ohos(window: *mut c_void, w, h)`；其余 `destroy` / `resize` / `tick` / `set_custom_fs` / `set_scene` / … 与 iOS 共用 `holm_runtime_session_*`。ohos target 拉 **wgpu + Vulkan**。Android JNI / iOS Metal create **本波不改**。
5. **产物（双 so）**：
   - 脚本编 `libholm_runtime.so`（aarch64-ohos）；改 Rust 先跑脚本，**Hvigor 不自动编 Rust**。
   - 独立 NAPI 模块 `libholm_xc.so`（`XComponent.libraryname = 'holm_xc'`）：注册 Surface/OnFrame 回调并调 Runtime C ABI；**不**并入 `libentry.so`。
6. **分辨率**：create/resize 以 `OH_NativeXComponent_GetXComponentSize` 的 Surface **像素**为准；本波不额外降 DPR。
7. **失败**：缺 so / `has_wgpu=0` / create·tick 失败 → `onError(BLOCKED)`，深蓝底；**禁止**静默 GLES 冒充。
8. **生命周期**：切 Lite / 卸岛 / SurfaceDestroyed → **UnregisterOnFrame** + Session `destroy`；新 Surface 再 `create`。

## Consequences

- 产品 DualTrack B 曾不含鸿蒙；本 ADR 开通鸿蒙 Runtime 尖刺/通面路径，鸿蒙仍可为 PARTIAL（真机 Vulkan 能力依赖设备）。
- 与 ADR 0009 同构：壳外泵 + C Session；差异在 XComponent/Vulkan/双 so，而非 Metal/xcframework。
- Demo `holm_runtime_m0` 可共用；鸿蒙上 Lite 本波曾为负向诚实失败。
- **已落地（ADR 0011）**：同 XComponent + EGL/GLES Shadertoy，`backend=gles2-ohos`；与 Runtime 互斥切轨。
- 可选后单：Android JNI 收拢到同一 C Session；Lite 降采样。
