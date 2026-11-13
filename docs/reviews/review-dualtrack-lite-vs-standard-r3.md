# Review · dualtrack-lite-vs-standard · r3

- 结论：打回
- 依据：基线 r2；本轮 diff = commit `7859372`（H5 Runtime Session/Worker/FPS 泵 + Demo onPerf/maxFps + wasm）
- 日期：2026-11-13

## 对照上轮

| 桶 / 项 | 状态 | 说明 |
|---|---|---|
| P0 | **新开 2** | r2 无 P0；本轮 H5 Runtime Worker 路径引入 |
| P1-concurrency（onError 重放） | 已关（沿 r2） | 本轮未回退 |
| P1-doc-drift（iOS liquid 口径） | 已关（沿 r2） | 本轮未回退 |
| P1-device-verify | 未关 | 真机/浏览器表扩 H5 Runtime 通面；见可执行档 |
| P2（r2 两项） | 已关 | 沿 r2 |
| 新开 P1/P2 | 见下 | Worker 失败态 / uncapped / 双载 wasm 等 |

## P0

### P0-silent-fail `executor` runtime→lite 不拆 Worker，Lite 永不挂泵
- 证据：
  - `KRHolmSurfaceView.kt:859-870`：切到非 Runtime 只 `destroyRuntimeSession()`，**不** `destroyPump()`/`terminate` Worker
  - 同文件 `:821-822` Worker 成功后 `_started=true`；else 支走 `_started` 已 true → 只 `postPlaying`，**不** `startPump()`
  - `destroyPump()`（`:696-719`）才是唯一 terminate Worker 的路径；Lite→Runtime（`:770-771`）会调，反向不会
- 影响：同一 `HolmSurface` 上把 `executor` 从 `runtime` 改回默认 Lite 时，Offscreen Worker 继续占画布，WebGL Lite 静默不起；Demo 虽分两页，Attr 契约仍可达
- 处置：必修（切非 Runtime 一律 `destroyPump()`，或对称清理 `_worker` + `_started` 后再 `startPump()`）

### P0-silent-fail Worker `CREATE_SESSION` 异步失败后卡死（无 BLOCKED / 无 ready / 无回退）
- 证据：
  - `startRuntimeWorkerPath` 同步 transfer 成功即 `_backend=wgpu-m0`、`_runtimeSessionStarted=true`、早退（`:815-826`）
  - `HolmRuntimePumpSource.kt` create 失败只 `emitError("CREATE_SESSION")`；`onPumpMessage` error 支仅 `fireError`（`:443-447`），**不**置 `BACKEND_WGPU_BLOCKED`、不 `fireReady`、不重置 `_runtimeSessionStarted`、不主线程回退
  - 主线程 `createSession` onError 则三者齐全（`:847-856`）
- 影响：已 transfer 的空画布 + Demo 长期 `waiting onReady…`；同页重试被 `_runtimeSessionStarted` 挡住
- 处置：必修（Worker error → BLOCKED + ready + 可选 replaceCanvas 后主线程回退；或超时看门狗）

## P1

### P1-device-verify H5 Runtime 通面 / 高刷档 / Lite 对照仍须设备或浏览器勾选
- 证据：changelog 多条「本单无真机验证」；本轮目视曾见 `wgpu-m0`/`fps≈163`，但审查会话不替代放行勾表
- 影响：放行条件；尤其 P0 修复后须回归 executor 切换与 Worker 失败态
- 处置：必修放行条件 → 可执行档 [`device-check-list/dualtrack-lite-vs-standard.md`](device-check-list/dualtrack-lite-vs-standard.md)（本轮已扩 H5 Runtime 通面步）
- 代码审不了：165Hz 机上 uncapped 稳定性；executor 热切后 Lite 是否真起；CREATE_SESSION 失败 UX

### P1-concurrency 卸岛 / wasm 轮询与异步 create 竞态
- 证据：`createSession` onSuccess 只判 `isRuntime`（`:830-845`），卸岛后仍可能 `startRuntimeLoop`；wasm `setInterval` 轮询（约 `:781-800`）无 handle，`destroyPump` 不清 `_runtimeLoadWaiting` 时可导致重挂死等
- 影响：短生命周期岛或快速切页可能泄漏泵或卡死再进
- 处置：必修（attached 守卫；保存 interval id 并在 destroy 清除；`_runtimeLoadWaiting` 与 destroy 对称）

### P1-doc-drift / 能力诚实 `backends` 含 GL 且 `backendId` 恒 `wgpu-m0`
- 证据：`holm-runtime/src/session.rs:98-99` `BROWSER_WEBGPU | GL`；`backend_id` 固定 `wgpu-m0`
- 影响：弱端可能静默走 wgpu-over-GL，与 DualTrack「禁止静默非 wgpu 充数」口径易混（有 adapter 时仍报 m0）
- 处置：建议（探测实际 backend 写入 ready，或文档明确「允许 WebGL 后端的 wgpu」）

## P2

### P2-uncapped-busy-spin
- 证据：Runtime `maxFps<=0` → `setTimeout(0)`（`HolmRuntimePumpSource` / 主线程回退）；Lite 同档仍 rAF
- 影响：冲高刷时 CPU 易打满；属有意换吞吐，须产品知情
- 处置：可选（rAF 优先 + 落后补偿；或 Demo 标注耗电）

### P2-double-wasm-load
- 证据：`index.html` 主线程预载 + Worker `importScripts`/`bindgen` 再载
- 影响：约双倍 wasm 内存/冷启
- 处置：可选（探针与泵共用一份或 Worker-only 探测）

### P2-sticky-error-on-worker-fallback
- 证据：transfer 同步失败路径会 `WORKER_INIT` error；Demo `errorText` 不因后续 `onReady` 清空
- 影响：主线程回退成功仍可能黄字残留
- 处置：建议（ready 清 error；或仅最终失败 fireError）

### P2-worker-perf-ungated
- 证据：Worker `tickPerf` 无条件 postMessage；主线程环才判 `_onPerf`
- 影响：无 onPerf 时多余跨线程流量
- 处置：可选

## 规范候选观察

1. Kuikly expand View：`executor`（或等价模式）切换时须对称 teardown（含 Worker/Offscreen），禁止只清「半套」Session 标志 — 已入本 skill `_backlog`
2. Offscreen Worker 异步 init 失败须与主线程失败同契约（BLOCKED + ready + 可回退）— 已入本 skill `_backlog`

## 真机

有 `P1-device-verify`。可执行档：[`device-check-list/dualtrack-lite-vs-standard.md`](device-check-list/dualtrack-lite-vs-standard.md)（r3 扩 H5 Runtime 通面与 executor 热切）。

## changelog

请确认 `docs/specs/dualtrack-lite-vs-standard/changelog.md` 已含 Runtime Lite3 / 性能 / maxFps / uncapped 等条目（审核员不代写）。

## 待补桶

无。
