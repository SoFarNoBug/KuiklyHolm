# Review · dualtrack-lite-vs-standard · r4

- 结论：有条件通过
- 依据：上轮 r3 打回；本轮 diff = 未提交整改（`KRHolmSurfaceView` / `HolmRuntimePumpSource` / `session.rs` / Demo / changelog / wasm）
- 日期：2026-11-13

## 对照上轮

| 桶 / 项 | 状态 | 说明 |
|---|---|---|
| P0-silent-fail（runtime→lite 不拆泵） | **已关** | else 支 `destroyPump()` + `startPump()`（`KRHolmSurfaceView.kt:911-922`） |
| P0-silent-fail（Worker CREATE_SESSION 卡死） | **已关** | `handleRuntimeWorkerFatal` → 拆泵 + 主线程回退一次 → BLOCKED+ready（`:555-588`；error 路由 `:454-459`） |
| P1-concurrency（卸岛 / wasm poll） | **已关** | `_attached` + `_runtimeLoadPollId`/`clearWasmLoadPoll`；createSession 双守卫（`:140-154,:536-539,:874-879,:936,:953`） |
| P1-doc-drift（backendId 恒 wgpu-m0） | **已关** | `backend_label`：WebGPU=`wgpu-m0`，GL=`wgpu-m0-gl`（`session.rs:9-14,122,279-280`） |
| P1-device-verify | **未关** | 仍须浏览器/真机勾表；本轮扩 r4 回归步 |
| P2-uncapped-busy-spin | **已关** | `maxFps<=0` 回 rAF（Worker + 主线程环） |
| P2-sticky-error-on-worker-fallback | **已关** | Demo `onReady` 清 `errorText`；Worker ready 亦 `clearPendingError` |
| P2-worker-perf-ungated | **已关** | `reportPerf` 门控 + Attr `ON_PERF` 时 post interest |
| P2-double-wasm-load | **未关** | changelog 已知遗留；本轮不修 |
| 新开 | 见 P2 | 回退旗标与 destroy 顺序 |

## P0

无。

## P1

### P1-device-verify（未关）H5 Runtime 通面 / 高刷 / executor 热切 / Worker 失败 UX
- 证据：changelog「本单无真机验证」；代码审无法代替 8080 勾表
- 影响：放行条件
- 处置：必修 → 可执行档 [`device-check-list/dualtrack-lite-vs-standard.md`](device-check-list/dualtrack-lite-vs-standard.md)（r4 已对齐 uncapped=rAF、backend 可能 `wgpu-m0-gl`、P0 回归步）
- 代码审不了：高刷机 `maxFps=0` 实际 fps；executor 热切后 Lite 是否真画；人为断 WebGPU/Worker 后 BLOCKED/回退文案

## P2

### P2-double-wasm-load（未关）
- 证据：changelog 已知遗留；主线程预载 + Worker 再载
- 影响：冷启内存/带宽约双倍
- 处置：可选（沿上轮）

### P2-fallback-flag-reset
- 证据：`destroyRuntimeSession` 将 `_runtimeMainFallbackTried=false`（约 `:530`）；`handleRuntimeWorkerFatal` 在 `destroyPump()` **之后**才读该旗标（`:570-587`）→「已回退过则 BLOCKED」支路对本函数内二次进入基本不可达
- 影响：最终失败仍靠 `startRuntimeMainSession` onError 的 BLOCKED+ready，主路径不静默；旗标语义弱于注释
- 处置：建议（回退旗标移出 destroy 清理，或先读后清）

## 规范候选观察

无。（上轮两条仍在本 skill `_backlog`；本轮将自评改为已达标，供人批晋升；未新开候选）

## 真机

有 `P1-device-verify`。可执行档：[`device-check-list/dualtrack-lite-vs-standard.md`](device-check-list/dualtrack-lite-vs-standard.md)。

## changelog

请确认 `docs/specs/dualtrack-lite-vs-standard/changelog.md` 已含「审查 r3 整改」条目（审核员不代写）。

## 待补桶

无。
