# Review · holm-h5-glb · r2

- 结论：打回
- 依据：对照 [`review-holm-h5-glb.md`](review-holm-h5-glb.md)；changelog 已 append；本轮 Chrome `http://localhost:8080/?page_name=holm_model` 冒烟
- 日期：2026-11-19
- diff 核对：上轮 P0 换元 / Draco ready / View gen / MVP 复用 / 失败 ready — 代码侧已落；**浏览器主路径仍黑屏 waiting**

## 对照上轮

| 桶 ID | 状态 | 说明 |
|---|---|---|
| `P0-plan-hard-drift`（切轨未换 canvas） | 已关（代码） | `resetCanvasForModelApi` 一律 `replaceCanvasInPlace` |
| `P1-concurrency` | 已关 | `__kmpDraco.ready` + View gen；冒烟 `dracoReady/wgpuReady=true` |
| `P1-main-thread-io` | 未关 | 仅让帧；仍主线程 parse |
| `P1-device-verify` | 未关 | Chrome 冒烟已跑；主路径失败见新 P0 |
| `P2-webgl-mvp-alloc-per-frame` | 已关 | `_mvpJs` |
| `P2-ready-on-load-fail` | 已关（代码） | 但本轮未观察到 ready（卡在 attach 前） |
| `P0-silent-fail`（Offscreen 争用） | **新开** | 见下 |

## P0

### P0-silent-fail Lite Worker Offscreen 残留 → Model WebGL 永不 attach
- 证据（Chrome 冒烟 2026-11-19）：
  - UI 常驻 `waiting…` / `fps=—`；Duck/Flamingo 黑屏（截图）
  - CDP：`canvas.getContext('webgl')` → `InvalidStateError: … transferred its control to offscreen`
  - `models/Duck.glb` / `Flamingo.glb` HTTP 200；`__kmpDraco.ready` / `__kmpWgpu.ready` 均为 true
  - 代码：`onAddToParent` 在非 wgpu 且 `!_started` 时无条件 `startPump()`（Worker+`transferControlToOffscreen`）；Model 进入虽 `destroyPump`+换元，但**现网 canvas 仍为 transferred** → `HolmWebGlModelRenderer.obtainWebGl` 失败且未走到成功 `fireReady`
- 影响：H5 Model gl 主路径整条不生效（Plan 验收 #1 不可达）；用户只见 waiting
- 处置：必修 —
  1. `onAddToParent`：已挂 / 即将挂 `HolmModelContent`（或 `CONTENT_KIND_MODEL`）时**禁止** Lite Worker/Offscreen
  2. `enterModelPresentMode` / `startModelWebGl`：若检测 Offscreen 或 `getContext` 抛 transfer，强制再 `replaceCanvasInPlace` 后重试
  3. WebGL `attach` 失败须 `fireError` **且** `fireReady`（勿永停 waiting）

## P1

### P1-main-thread-io Tokyo/大 GLB 仍主线程 parse
- 证据：`yieldThenParse` 仅 `setTimeout(0)`
- 影响：大资产卡顿；本轮未测到 Tokyo（主路径已挂）
- 处置：建议 — Worker 另单

### P1-device-verify 浏览器全表
- 证据：本轮仅 Chrome；Safari / wgpu 切轨 / Tokyo 未闭环
- 影响：放行条件
- 处置：必修 — [`device-check-list/holm-h5-glb.md`](device-check-list/holm-h5-glb.md)；**先修新 P0 再勾**

## P2

无。

## 规范候选观察

- H5：Model Present 路径禁止先挂 Lite Worker/Offscreen；与 Model WebGL 争用同一 canvas 即 P0｜可抄码农 h5 池

## 真机

缺口：主路径出画（本轮失败根因 Offscreen）；wgpu 切轨；Safari；Tokyo。  
可执行档：[`device-check-list/holm-h5-glb.md`](device-check-list/holm-h5-glb.md)（已记 Chrome 失败）。

## changelog

上轮回执行已 append。本轮打回后再修须再 append；审核员不代写。

## 待补桶

无。
