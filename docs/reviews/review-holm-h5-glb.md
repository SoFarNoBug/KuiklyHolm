# Review · holm-h5-glb

- 结论：打回
- 依据：ADR 0020；Plan `.cursor/plans/2026-11-19-h5-model-dual-track.md`（验收含 gl↔wgpu 可切）；changelog `docs/specs/holm-content-gltf/changelog.md` + `docs/specs/kuikly-dev-h5/changelog.md`；`compileKotlinJs` 已过
- 日期：2026-11-19
- diff：`KRHolmSurfaceView` js Model 分轨；`HolmWebGlModelRenderer` / `HolmJsModelPresenter`；`HolmModelPass` js + `createModelPass`；`index.html` Draco/bindgen；`models/*`；Demo footer；ADR/guide/`holm_api`

## P0

### P0-plan-hard-drift H5 Model gl↔wgpu 切轨未换 canvas，WebGL/WebGPU 上下文互斥
- 证据：`KRHolmSurfaceView.kt` `enterModelPresentMode` ~1430–1434（gl 路：`destroyModelWgpu`+`destroyPump` 后直接 `startModelWebGl`）；~1375–1422（wgpu 路对称）；`destroyPump` 仅 `_controlTransferred` 时 `replaceCanvasInPlace`（~1132–1133）；Model 主线程 Present **不**置 transfer；`HolmWebGlModelRenderer.obtainWebGl` ~285–289；浏览器规范：同一 canvas 一旦取得 WebGPU（或 WebGL）上下文，另一 API `getContext` 永久失败
- 影响：Plan / ADR 0020 验收「gl↔wgpu 可切」主路径不可达——Demo 热切 executor 后 WebGL attach 报 `WEBGL_CONTEXT` 或 ModelPass `BLOCKED`，岛停画；冷开默认 gl 尚可，但双轨切换宣称落空（硬漂移）
- 处置：必修 — 在 WebGL↔WebGPU（及 Shader wgpu→Model gl）切换时强制 `replaceCanvasInPlace`（或等价换元），并 `detachModelPointer` 后绑新 canvas；复测切轨 2 次

## P1

### P1-main-thread-io Tokyo/大 GLB 在 Promise.then 主线程同步 parse（含 Draco）
- 证据：`HolmJsModelPresenter.kt:24–29`（`then` 内 `GltfDocument.parse`）；对比 Android `HolmAndroidModelPresenter` IO 线程
- 影响：切 Tokyo 可长时间卡死 UI 帧；非 ANR 语义但 H5 主线程等价卡顿
- 处置：必修（可与 P0 同单）— Worker / 拆片异步 parse + gen 回填；或 changelog/ADR 明示接受边界（仍建议修）

### P1-concurrency Draco wasm 无 ready 门闸；Presenter `_gen` 全局单例
- 证据：`index.html` Draco `__kmp_draco_bindgen(...)` fire-and-forget（无 `__kmpDraco.ready` 轮询）；`HolmJsModelPresenter.kt:17–18` 进程级 `_gen`；`DracoPlatform.js.kt` 未 ready 即 `DracoDecodeException`
- 影响：冷开即切 Tokyo 可能 `MODEL_LOAD`；多岛（若有）互取消 load
- 处置：建议 — Draco 对齐 wgpu 短轮询；`_gen` 按 View 实例

### P1-device-verify 浏览器双轨出画 / Tokyo / 切轨 / Safari
- 证据：代码审无法证明 Chrome WebGPU ModelPass、Safari WebGL、orbit、Draco、失败码诚实
- 影响：放行条件；`compileKotlinJs` ≠ Present 闭环
- 处置：必修（浏览器勾表）— 可执行档 [`device-check-list/holm-h5-glb.md`](device-check-list/holm-h5-glb.md)；**建议修 P0 后再跑 #7/#9**

## P2

### P2-webgl-mvp-alloc-per-frame
- 证据：`HolmWebGlModelRenderer.kt:212–215` 每帧 `Float32Array(16)`
- 影响：轻量 GC；对齐 Ohos「热路径少分配」教训但量级较小
- 处置：建议 — 复用成员缓冲

### P2-ready-on-load-fail
- 证据：`onModelLoadFailed` ~1465 只 `fireError`，不 `fireReady`；Demo `backendText` 可停在 `waiting…`
- 影响：UX；错误文案若已展示则次要
- 处置：可选 — 失败也 ready（backend 诚实）对齐其它端 surface 就绪语义

## 规范候选观察

- H5/Web 上 WebGL 与 WebGPU 不得复用同一 canvas 节点切换 Present；切轨须换元（`replaceCanvasInPlace`）并重绑手势｜可抄码农 android/h5 池
- H5 Model Draco/wasm 依赖须有 ready 门闸（对齐 `__kmpWgpu.ready`），禁止竞态 `MODEL_LOAD`｜可抄 h5 池

## 真机

缺口：Chrome gl/wgpu 真画、orbit、Tokyo/Draco、灯光、executor 热切、Safari WebGL、无 WebGPU 时 `BLOCKED`、进出页 — 代码审不了。  
可执行档：[`device-check-list/holm-h5-glb.md`](device-check-list/holm-h5-glb.md)。

## changelog

已见码农 append（holm-content-gltf + kuikly-dev-h5）。打回后修复须再 append；审核员不代写。

## 待补桶

无。
