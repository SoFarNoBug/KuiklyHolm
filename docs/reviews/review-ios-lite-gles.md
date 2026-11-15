# Review · ios-lite-gles

- 结论：有条件通过
- 依据：Plan「iOS Lite GLES」；changelog（iOS Lite GLES + 修 ~1fps 1x 缓冲）；ADR 0009 Consequences 后单注记；用户口述 Duo Lite 已出画（约 6fps）
- 日期：2026-11-14
- 范围：iOS Lite `CAEAGLLayer` 子 View + Shadertoy；与 Runtime Metal 互斥；1x 缓冲热修。不含 Android/H5、不含 Metal Lite

## 对照上轮

| 上轮（ios-runtime-adr0009） | 本轮 |
|---|---|
| Lite=`LITE_PENDING` | 已关：GLES 出画 + `gles2-ios` |
| P1-device-verify Runtime 已关 | 新开：Lite / 切轨 / 真机 fps 口径 |
| P2 DisplayLink/resize 已关 | 保留主线程泵（ADR） |

## P0

无。

## P1

### P1-device-verify（未关）Lite 通面与 DualTrack 口径
- 证据：Plan 验收含 Lite 隧道 / `gles2-ios` / 切回 Runtime / maxFps / 空片元；对话仅 Duo Lite 出画且约 6fps（模拟器 GLES+raymarch 预期偏弱，真机未勾）
- 影响：放行条件未全部闭环；**禁止**用 Duo 模拟器 Lite fps 代表真机 GLES
- 处置：必修 → 可执行档 [`device-check-list/ios-lite-gles.md`](device-check-list/ios-lite-gles.md)
- 代码审不了：真机 Lite 隧道流畅度与 fps；Runtime↔Lite backend 是否粘 wgpu；空 `fragmentShader` 深蓝+`FRAGMENT_REQUIRED`；maxFps 可感知

## P2

### P2-objc-shadertoy-drift
- 证据：`KRHolmGlesHostView` 内嵌 wrap，与 `HolmShadertoyShell.kt` 双文；注释已交叉引用
- 影响：改壳不同步则 Lite 编译/观感漂移
- 处置：建议 — 已入 ios 码农 `_backlog`；改壳须双端同单

### P2-sim-gles-raymarch-fps
- 证据：`LITE_BODY` `MAX_STEPS=60` + normal；Duo 全宽×280 岛；用户约 6fps
- 影响：模拟器 DualTrack 数字不可对标真机；非静默无画
- 处置：可选 — 真机采数；或半分辨率 / 降 steps（产品另单）

## 规范候选观察

1. DualTrack 性能对比须标注端与分辨率策略（iOS Lite 1x 点像素；模拟器 GLES raymarch fps 不作真机结论）。

（已入本 skill `_backlog`）

## 真机

有 `P1-device-verify`。可执行档：[`device-check-list/ios-lite-gles.md`](device-check-list/ios-lite-gles.md)。

## changelog

请确认已 append：
- `docs/specs/dualtrack-lite-vs-standard/changelog.md`（iOS Lite GLES + 修 ~1fps）
- `docs/specs/kuikly-dev-ios/changelog.md`  
（审核员不代写。）

## 待补桶

无。
