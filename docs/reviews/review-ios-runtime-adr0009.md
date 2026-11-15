# Review · ios-runtime-adr0009

- 结论：通过
- 依据：ADR 0009；Plan「iOS Runtime ADR0009」；changelog（落地 + 切 Lite 热修 + P2 DisplayLink/resize）；用户口述 Duo + **真机验证 OK**（2026-11-14）
- 日期：2026-11-14
- 范围：iOS Runtime（C Session + xcframework + `KRHolmSurface`/`KRHolmMetalHostView` 主线程外泵）；Lite 诚实 `LITE_PENDING`；不含 Android/H5、不含 iOS Lite GLES 出画

## 对照上轮

无。（新 task-id）

## P0

无。

## P1

### P1-device-verify（已关）
- 证据：用户 2026-11-14 确认 **真机验证 OK**；此前 Duo 已勾 Runtime 通面 + Runtime↔Lite 不崩；可执行档 [`device-check-list/ios-runtime-adr0009.md`](device-check-list/ios-runtime-adr0009.md)
- 影响：—
- 处置：已关
- 残余说明：#10 缺 xcframework 负向未强制口述（本机脚本前置；链接失败非静默假栈，接受）

## P2

### P2-cadisplaylink-retain（已关）
- 证据：后单 `KRHolmDisplayLinkProxy` 弱持 Surface；`invalidate` 后清 proxy
- 处置：已关

### P2-layout-resize-churn（已关）
- 证据：`lastSessionW/H` 像素未变跳过 `session_resize`
- 处置：已关

### P2-main-thread-session-create（已关 · 接受）
- 证据：主线程 `block_on` 创 Session（ADR 0009 锁定）
- 处置：已关（接受；卡顿另单，本波不修）

## 规范候选观察

1. DualTrack / expand View：Compose 若先写 `fragmentShader` 再写 `executor`，宿主在仍持 Runtime Session 时必须拒绝非本轨方言（GLSL≠WGSL），禁止喂错后端；跨 FFI 的 native 入口须 `catch_unwind`（或等价）避免 panic abort 进程。

（已入本 skill `_backlog`）

## 真机

`P1-device-verify` 已关。可执行档：[`device-check-list/ios-runtime-adr0009.md`](device-check-list/ios-runtime-adr0009.md)。

## changelog

请确认已 append：
- `docs/specs/dualtrack-lite-vs-standard/changelog.md`（iOS Runtime ADR 0009 + 切 Lite 热修 + P2）
- `docs/specs/kuikly-dev-ios/changelog.md`  
（审核员不代写。）

## 待补桶

无。
