# Review · h5-worker-gl-pump

- 结论：通过
- 依据：Plan「H5 OffscreenCanvas Worker 泵帧（1+2+3+5）」；`docs/specs/stage0-lite-spike/changelog.md` 对应条目
- 日期：2026-11-12

## P0

无。

## P1

无。

## P2

### P2-offscreen-remount 同实例二次挂载
- 证据：`KRHolmSurfaceView.kt:155-168`（`transferControlToOffscreen`）；`destroyPump`（约 360–394 行）terminate Worker 但不换新 `HTMLCanvasElement`
- 影响：若 Kuikly 复用同一 export 实例再 `onAddToParent`，二次 transfer 会失败，需依赖工厂每次 `KRHolmSurfaceView()` 新建；尖刺页主路径通常新建，故不升 P1
- 处置：建议（后续若见复用，destroy 时换 canvas 或强制主线程回退）

### P2-worker-init-no-fallback transfer 后失败不可回退主线程泵
- 证据：`startWorkerPath` catch 仅 `WORKER_INIT`（约 159–166 行）；`canUseWorkerPath` 为真后不再走 `startMainPath`
- 影响：init/`postMessage` 失败时画布已交出，无法同实例改走主线程泵；错误经 `onError` 上报
- 处置：可选

## 规范候选观察

- Kotlin/JS 禁止用 `js("self").asDynamic()` 赋全局：会编译成运行时 `self.asDynamic()`，导致泵分发未安装、特效静默不起。｜2026-11-12｜自评：已达标（本单已用 IIFE 赋值规避）

## 真机

无。（Plan 未要求真机验证；H5 浏览器已见 `backend=webgl-worker`、场景热换、fps 未限帧）

## Plan 对齐

- 1/2/3/5：纯 JS 泵 + Worker/Offscreen 主路径 + context + 脏更新/visibility + 主线程回退 — 已覆盖
- Android / 半分辨率 / 限帧 — 未改，符合非目标
- changelog 已 append

## changelog

请确认 `docs/specs/stage0-lite-spike/changelog.md`「H5 OffscreenCanvas Worker 泵帧」条目仍在（审核员不代写）。

## 待补桶

无。
