# Review · dualtrack-lite-vs-standard

- 结论：有条件通过
- 依据：Plan「Lite / Standard 双轨对比」；`docs/specs/dualtrack-lite-vs-standard/changelog.md`
- 日期：2026-11-12

## P0

无。

## P1

### P1-concurrency Attr `executor=runtime` 时 `onError(BLOCKED)` 可能早于回调注册而丢失
- 证据：
  - `KuiklyHolmCore/.../androidMain/.../KRHolmSurfaceView.kt`：`PROP_EXECUTOR` → `applyExecutorMode()` → `fireError`；`ON_ERROR` 仅赋值、**无重放**
  - 同文件 `ON_READY` 有 `_readyFired` 重放；`ON_ERROR` 不对称
  - `jsMain/.../KRHolmSurfaceView.kt` 同构；`onAddToParent` 再调 `applyExecutorMode` 时 `_runtimeBlockedNotified` 已 true，仍不重发
- 影响：`holm_runtime_m0` 首屏可能只见 `backend=wgpu-blocked`，`errorText` 空；与 DualTrack「缺 native 须 onError(BLOCKED)」不完全一致（ready 仍可达）
- 处置：必修（重放最近 BLOCKED，或延迟到回调已挂再 fire）

### P1-device-verify iOS Lite 目视 + Android/H5 Runtime 旁路态未在代码审闭环
- 证据：DualTrack §4 Track A Mac 未勾；Track B 清屏/三角 BLOCKED；changelog「本单无真机验证」
- 影响：对齐证据与 Runtime 旁路 UX 须设备/模拟器确认
- 处置：必修放行条件 → 可执行档 [`device-check-list/dualtrack-lite-vs-standard.md`](device-check-list/dualtrack-lite-vs-standard.md)
- 代码审不了：iOS GLES 热换观感；Runtime 页 BLOCKED 文案是否可见；Lite 默认入口未被旁路冲掉

### P1-doc-drift iOS `liquid_approx` 标注「简化版」与「同源」口径不完全一致
- 证据：`KRHolmSurface.m` 注释「简化版」；common `LiquidApproxShaderSource` 含更新 fresnel/中心安全等；DualTrack / runbook 写「同源 GLSL」
- 影响：不挡 frost 对齐主路径；液态跨端像素级一致性可能偏
- 处置：建议（同步串或文档改为「同源骨架 / 允许端侧精简」）

## P2

### P2-runtime-ready-without-draw
- 证据：Android `HolmRuntimeNative.isAvailable()` 仅 `loadLibrary`；`available==true` 分支直接 `backend=wgpu-m0` 且无清屏/三角绘制
- 影响：将来误放空 .so 会假通面；本机无 .so 时不可达
- 处置：可选（通面里程碑接 ABI/`has_wgpu` + 真绘制）

### P2-playing-ignores-executor
- 证据：Android `PROP_PLAYING` 不判断 `_executor`，runtime 阻塞态仍可改 `renderMode`
- 影响：微浪费；非功能静默失效
- 处置：可选

## 规范候选观察

1. 原生桥 `onError`/`onReady`：Attr 可能先于回调 fire 时须支持重放（与本单 P1-concurrency 同源）— 已入本 skill `_backlog`

## 真机

有 `P1-device-verify`。可执行档：[`device-check-list/dualtrack-lite-vs-standard.md`](device-check-list/dualtrack-lite-vs-standard.md)。

## Plan 对齐

- DualTrack 文档 + CONTEXT/Stage0：已覆盖；O0 未改选 — 符合
- Track A iOS `KRHolmSurface` frost/liquid + runbook + pbxproj：代码已覆盖；Mac 目视 → 真机桶
- Track B `holm-runtime` + `executor` 旁路 + Demo 第二页默认 Lite：已覆盖；清屏/三角按 Plan 允许记 **BLOCKED** — 非 `P0-plan-hard-drift`
- ADR 0004 Keep Lite：与对比表证据一致；O0 未翻转 — 符合「仅收口改 O0」
- changelog 已 append `docs/specs/dualtrack-lite-vs-standard/changelog.md`
- 非目标：未上 glTF / 未删五席 / 未静默换非 wgpu 栈 — 符合

## changelog

请确认 `docs/specs/dualtrack-lite-vs-standard/changelog.md` 与 `docs/specs/kuikly-dev-ios/changelog.md` DualTrack 条目仍在（审核员不代写）。

## 待补桶

无。
