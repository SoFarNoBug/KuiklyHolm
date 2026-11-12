# Review · second-effect-glow

- 结论：有条件通过
- 依据：Plan `second_effect_glow_55db2772`（第二特效 glow · scene 热换）
- 日期：2026-11-12

## P0

无。

## P1

### P1-device-verify Android GLES scene 热换须真机确认
- 证据：`KuiklyHolmCore/.../androidMain/.../KRHolmSurfaceView.kt:55-59`（`queueEvent`→`setScene`→`rebuildProgram`）；代码审查无法证明真机 GL 线程换 program 后 frost↔glow 观感正确
- 影响：H5 路径可硬刷验收；Android 编译绿但运行时热换未闭环
- 处置：必修（放行条件）；可执行档 → [device-check-list/second-effect-glow.md](./device-check-list/second-effect-glow.md)

## P2

### P2-onready-stale-scene 状态行不随切换刷新
- 证据：`holmDemo/.../HolmSpikePager.kt:107-109`（`onReady` 仅首帧）；`KRHolmSurfaceView` `_readyFired` 只触发一次
- 影响：点「柔光」后画面可变，但 `backendText` 仍可能显示首次 `scene=frost`
- 处置：建议（Demo 文案体验，非主路径失效）

### P2-h5-ready-payload H5 onReady backend 常空
- 证据：JS `fireReady` 用 `kotlin.js.json`；`ReadyParams.decode` 期望 `JSONObject`（`HolmSurface.kt`）
- 影响：文案 `backend=` 空；与本单 glow 热换无关，属既有桥接缺口
- 处置：可选

### P2-android-release-thread detach 上直接 `glDeleteProgram`
- 证据：`KRHolmSurfaceView.kt:87-90` → `release()` 调 GLES，未 `queueEvent`
- 影响：偶发上下文竞态；本单未证必崩
- 处置：建议（后续改 GL 线程释放）

## 规范候选观察

无。

## 真机

- 缺口：Android 上 frost↔glow 热换观感与无黑屏/无编译失败回调
- 可执行档：[device-check-list/second-effect-glow.md](./device-check-list/second-effect-glow.md)
- H5：Plan 验收项（硬刷切换）；本审查不替代人工硬刷

## Plan 对齐

| Plan 项 | 状态 |
|---|---|
| glow shader + GlowShaderSource + HolmSceneShaders | 已落实 |
| Android/H5 scene 热换 | 已落实 |
| Demo 霜化/柔光切换 | 已落实 |
| changelog + ADR/Stage0 | 已 append |
| 编译绿 | 码农侧已报 BUILD SUCCESSFUL |

## changelog

`docs/specs/stage0-lite-spike/changelog.md`「第二特效 glow」已 append（审核员不代写）。

## 待补桶

无。
