# Review · holm-h5-glb · r3

- 结论：有条件通过
- 依据：对照 [`review-holm-h5-glb-r2.md`](review-holm-h5-glb-r2.md)；本轮速速改（Registry NVI 桥 / Draco U8 / 灯光热更 / 轨道保留 / wgpu 泵帧）；changelog 已 append
- 日期：2026-11-20
- diff 核对：`KRHolmSurfaceView` js · `HolmSurface`/`HolmModelNviCodec` · `HolmModelPager` · `kmp-draco` `DracoPlatform.js.kt`

## 对照上轮

| 桶 ID | 状态 | 说明 |
|---|---|---|
| `P0-silent-fail`（Offscreen 争用） | 已关 | Model 禁 Lite Offscreen + 换元重试；Chrome Flamingo/Tokyo `webgl-model` 出画 |
| `P1-main-thread-io` | 未关 | Tokyo 仍主线程 parse（`setTimeout(0)`） |
| `P1-device-verify` | 未关 | Chrome 主路径已绿；Safari / 全表未齐 |
| `P1-concurrency`（Draco ready） | 已关（上轮） | 保持 |
| 新：双包 Registry miss | 已关（本轮） | `modelAssetPath` 等 NVI 桥；否则永 waiting |
| 新：Draco `toUint8Array` | 已关（本轮） | Tokyo `KHR_draco` 可解 |
| 新：灯光/视角 | 已关（本轮） | state 重建 Content + 热更不盖 `_modelYaw` |
| 新：wgpu 半帧 | 已关（本轮） | `maxFps≥60/0` 每 rAF 出画 |

## P0

无。

## P1

### P1-main-thread-io Tokyo/大 GLB 仍主线程 parse
- 证据：`HolmJsModelPresenter.yieldThenParse` 仅 `setTimeout(0)`；Tokyo 冷切仍可卡主线程
- 影响：大资产卡顿；功能已可达
- 处置：建议 — Worker 另单

### P1-device-verify 浏览器全表未齐
- 证据：本轮 Chrome 已验 Flamingo/Tokyo/`webgl-model`/灯光 Unlit/拖转保留；Safari、错误资产、进出页、wgpu↔gl 多次切轨未系统勾完
- 影响：放行条件
- 处置：必修 — 可执行档 [`device-check-list/holm-h5-glb.md`](device-check-list/holm-h5-glb.md)

## P2

### P2-bridge-bytes-solid
- 证据：H5 NVI 桥仅 `asset`/`fileUri`/lights/orbit；`fromBytes` 与 `solidColor` 同源热更未走完整 bridge（solid 改走 `applyModelFromBridgeOrRegistry` 后被同源灯路径吞掉）
- 影响：Demo 现只用 assets；非主路径
- 处置：可选

### P2-model-maxfps-1-59-acc
- 证据：`startModelWgpuLoop` 对 `maxFps in 1..59` 仍用累加器清零，极端抖动仍可能偏慢
- 影响：默认 Demo `maxFps=60` 已对齐；低档节流边角
- 处置：可选

## 规范候选观察

- H5 双包（nativevue2 ≠ h5App）时 `HolmContentRegistry` 不可共享；Model 须像 Shader 一样过 NVI bridge 载荷，否则静默无 Present｜可抄码农 h5 池

## 真机

缺口：Safari；错误资产 `MODEL_LOAD`；进出页稳定性；wgpu↔gl 多次切轨系统勾。  
代码审不了：显示器刷新/体感 fps 数字、Safari WebGPU BLOCKED 文案。  
可执行档：[`device-check-list/holm-h5-glb.md`](device-check-list/holm-h5-glb.md)（已按 r3 更新 Chrome 结果）。

## changelog

码农已 append `docs/specs/kuikly-dev-h5/changelog.md`（速速改）。审核员不代写。

## 待补桶

无。
