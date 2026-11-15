# Review · wgpu-error-to-business · r2

- 结论：有条件通过
- 依据：上轮 [`review-wgpu-error-to-business.md`](review-wgpu-error-to-business.md)；码农速速改 changelog「审查回执行 · wgpu-error-to-business P1/P2」
- 日期：2026-11-15
- 范围：复审上轮开放项；抽检 H5 主线程 / Worker / Android drain / Ohos tick / wasm 前缀。未新开业务深潜

## 对照上轮

| 桶 ID | 状态 | 说明 |
|---|---|---|
| `P1-plan-soft-drift` | 已关 | H5 `sessionTick`→`String?` + 主线程 `flushRuntimeSessionErrors`；Worker catch 仅 drain；Ohos 帧泵/create 首帧读 `last_error_*` |
| `P1-device-verify` | 未关 | 仍无浏览器勾表；可执行档沿用基线 |
| `P2-duplicate-onerror-setCustomFs` | 已关 | Android/H5 失败路径 drain 优先，空才 fallback 单发 |
| `P2-shader-custom-message-prefix` | 已关 | `session.rs` map_err 只带 `e` |
| （新开） | 无 | — |

## P0

无。

## P1

### P1-device-verify（未关）H5 Runtime 负向 onError / sticky
- 证据：整改 changelog 仍「本单无真机验证」；代码审无法代替硬刷后红字/出画
- 影响：唯一未关必修 P1
- 处置：必修 → 可执行档 [`device-check-list/wgpu-error-to-business.md`](device-check-list/wgpu-error-to-business.md)
- 代码审不了：坏 WGSL 单条 `SHADER_CUSTOM` 含 reserved；合法 IQ 出画；sticky；新 wasm 生效

## P2

无。

## 规范候选观察

无。（上轮两条已在本 skill `_backlog`，本轮不重复入池；实现已对齐「drain 单发 / 主线程同契约」）

## 真机

同基线；可执行档未改。优先勾表 #2–#6。

## changelog

请确认已含「审查回执行 · wgpu-error-to-business P1/P2」（审核员不代写）。

## 待补桶

无。
