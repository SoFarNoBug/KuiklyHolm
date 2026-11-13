# Review · android-runtime-dualtrack

- 结论：有条件通过
- 依据：ADR 0006；`docs/specs/dualtrack-lite-vs-standard/changelog.md`（2026-11-13 SurfaceView DualTrack + 2026-11-14 画质/去 pow）；未提交工作树 diff（Core / holm-runtime / Demo / jniLibs）
- 日期：2026-11-14
- 范围：Android Runtime（SurfaceView + HandlerThread/Choreographer 外泵 + arm64 JNI）与同页 Lite GLES 切换 / 画质修复；不含 H5 r4 已关项复审

## 对照上轮

无。（新 task-id；非 dualtrack H5 r* 复审）

## P0

无。

## P1

### P1-doc-drift（未关）runtime→lite 后 `onReady`/`backend` 可能粘在 wgpu
- 证据：
  - `KRHolmSurfaceView.applyExecutorMode` Lite 支：只 `onResume`，不主动 `fireReady`（约 `:452-465`）
  - `HolmGlesRenderer.onSurfaceCreated`：`_ready` 只升不降；`release()` 不重置 `_ready`（`:724-727,:797-803`）
  - 切 executor 时 `_readyFired=false`（`:327`），但 GLSurfaceView 已建过面时可能不再走 `onSurfaceCreated` → Lite 不重发 ready
  - Demo `backendId` 仅在 `onReady` 写入（`HolmRuntimeM0Pager.kt:215-218`）；采集 JSONL 曾出现 `executor=lite` + `backend=wgpu-m0`
- 影响：状态行 / `[holm-perf]` 能力标签说谎；画面仍可能是 GLES（非静默无画）
- 处置：必修 — 切 Lite 后按当前 executor 再 `fireReady(BACKEND_GLES2)`，或 renderer 在可见恢复时重置 `_ready` 并回调；Demo 切轨时清/改写 `backendId`

### P1-concurrency（未关）急拆 timeout 与跨线程 Surface 旗标
- 证据：
  - `eagerDestroyRuntimeSession`：`latch.await(800ms)` 超时后仍 `finally` 清 `_runtimeEagerDestroying`，未强制 `_sessionHandle=0` / 停泵（`:556-575`）
  - `_runtimeSurfaceValid` UI 写、渲染线程读，非原子（`:93,:120,:470`）
- 影响：慢 destroy / 竞态下可能对已毁 Surface 继续 tick（未证必崩，故不升 P0）
- 处置：建议 — 超时亦停泵并清空句柄；旗标改 `AtomicBoolean` 或仅渲染线程拥有

### P1-device-verify（未关）Android Runtime/Lite 通面与画质
- 证据：changelog 登记真机表待审核员；画质（highp / 非 sRGB / 去 pow）与四席观感代码审不闭环
- 影响：放行条件
- 处置：必修 → 可执行档 [`device-check-list/android-runtime-dualtrack.md`](device-check-list/android-runtime-dualtrack.md)
- 代码审不了：arm64 真机 frost/glow/liquid/custom 通面；Lite custom 发白是否缓解；缺 .so / 非 arm64 的 BLOCKED；executor 热切后 backend 文案；maxFps 60/120 的 onPerf 与观感

## P2

### P2-handler-thread-leak
- 证据：`HandlerThread("holm-runtime")` 在字段初始化 `start()`（`:67-68`），`onDetachedFromWindow` 不 `quit`/`quitSafely`
- 影响：反复进出页可积线程
- 处置：建议

### P2-global-presentable-flag
- 证据：`HolmRuntimeNative._presentable` 进程级单例（`:14-15,:47-48`）
- 影响：多岛时 ready/available 互相污染（Demo 单岛可忍）
- 处置：可选

### P2-silent-skip-non-wgsl-custom
- 证据：`applyRuntimeShaders` 非 WGSL 外形则 `return`，无 `onError`（`:515-517`）
- 影响：误塞 GLSL 到 Runtime 时可能留旧 custom / 无提示
- 处置：建议

## 规范候选观察

1. expand View / DualTrack：`executor` 热切后须按**当前**轨重发 `onReady`（含 backend），不得依赖「Surface 已创建过则不再 onSurfaceCreated」。
2. 持 native Surface 的急拆：await 超时也必须停泵并丢弃句柄，禁止超时后继续 tick。

（已入本 skill `_backlog`；可人工抄 `peer_pools` → kuikly-android-coding）

## 真机

有 `P1-device-verify`。可执行档：[`device-check-list/android-runtime-dualtrack.md`](device-check-list/android-runtime-dualtrack.md)。

## changelog

请确认 `docs/specs/dualtrack-lite-vs-standard/changelog.md` 已含：
- Android Runtime DualTrack（SurfaceView + 外泵 + arm64 .so）
- 画质：Lite highp + Runtime 非 sRGB + RUNTIME/LITE 去 pow  
（审核员不代写。）

## 待补桶

无。
