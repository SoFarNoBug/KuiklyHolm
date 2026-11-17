# ADR 0009: iOS Runtime · CAMetalLayer + ObjC 外泵 Session

- Status: Accepted
- Date: 2026-11-14
- Updated: 2026-11-16（Present 改经 kmp-wgpu；泵仍 ObjC）

## Context

H5 / Android Runtime（wgpu Session + 外泵 + 宿主 WGSL）已通；iOS 仅有 GLES2 `KRHolmSurface` 壳，五席删除后未接 `fragmentShader`，`kmp-wgpu-native` README 曾写「本波不做 iOS Runtime」。审讯锁定：**先通 iOS Runtime，再补 Lite**；形态对齐 ADR 0006，并符合 iOS 嵌入式 GPU 惯例（壳拥有 View/Layer/vsync，引擎吃 layer 指针）。

## Decision

1. **顺序**：本波只验收 Runtime 隧道；`executor=renderer-gl` → 深蓝 + 诚实 `onError`（不冒充出画）；Lite `fragmentShader` 后置。
2. **呈现面**：`KRHolmSurface` 为普通 `UIView` **容器**；`executor=renderer-wgpu` 挂带 **`CAMetalLayer` 的子 View**（非根 `CAEAGLLayer` 热换；非 `MTKView` 抢 drawable）。
3. **泵与胶水**：**iosApp ObjC** 拥有子 View + 主线程 **`CADisplayLink`** → 同步 `tick`；Rust **无内泵**。不把泵放到 Kotlin/Native。
4. **ABI**：完整 C Session API（`kmp_wgpu_session_*`，含 Metal layer 指针 create / resize / tick / set_custom_fs / destroy…）仍为底层事实源；与 `kmp_wgpu_abi_version` / `has_wgpu` 并列。
5. **Present 路径（2026-11-16）**：壳 **禁止直调** `kmp_wgpu_session_*`；经 `HolmWgpuIosBridge` → `HolmFullscreenPass` → `IosWgpuPlatform`（cinterop）→ 同一 C ABI。`iosApp` 仍链 `KmpWgpuNative.xcframework` 供符号解析。
6. **产物**：脚本打 **device + simulator** xcframework，落入固定目录由 `iosApp` 直链；改 Rust 先跑脚本，Gradle/Xcode **不**自动编 Rust。缺库 / create 失败 → `onError(BLOCKED)`，**禁止** GLES 冒充 wgpu。
7. **帧率**：落实 Attr `maxFps`（DisplayLink `preferredFrameRateRange` / 跳帧；`0`=不限）。
8. **生命周期**：切 Lite / 卸岛 / 失窗 → **立刻** Session `destroy`；新 layer 再 `create`。`drawableSize` / `contentScaleFactor` 跟 `nativeScale` 与 layout。
9. **Provider**：切轨 / start 前经 bridge `requireProvider`；与 ADR 0008 对齐。

## Consequences

- 推翻 `kmp-wgpu-native/README`「本波不做 iOS Runtime」；实现前以本 ADR 为准。
- 与 ADR 0006 同构但泵线程不同：Android=渲染线程；iOS=主线程（UIKit/wgpu-in-app 惯例）。
- Demo `holm_dualtrack` 双轨 UI 可保持；Lite 轨曾为本波负向诚实失败。
- **已落地（后单）**：Lite GLES 子 View（`CAEAGLLayer` + Shadertoy wrap，`backend=gles2-ios`）；与 Runtime Metal 子树互斥。
- **已落地（2026-11-16）**：三包 ios 目标 + kmp-wgpu Present；壳停直调 C；泵仍 ObjC。
