# Review · demo-acd-samples

- 结论：有条件通过
- 依据：Plan「Demo 复杂样本 A/C/D（chip 切换）」；changelog「Demo 多样本 A/C/D chip」
- 日期：2026-11-15
- 范围：Demo 样本 catalog + 血浆/水波/火焰 shader + `holm_runtime_m0` chip。不含 Core/native 壳

## P0

无。

## P1

### P1-device-verify（未关）四样本出画 / 双轨观感
- 证据：Plan 验收要求四 chip 出画、Lite/Runtime 可跑、观感可辨；changelog 写「本单无真机验证」
- 影响：出画与 WGSL/GLES 编译只能装机闭环
- 处置：必修 → 可执行档 [`device-check-list/demo-acd-samples.md`](device-check-list/demo-acd-samples.md)
- 代码审不了：四样本 × 两轨是否编译出画；血浆/水波/火焰是否可辨；弱端是否卡死；切样本后岛是否黑屏

## P2

### P2-sample-switch-clears-backend
- 证据：`HolmRuntimeM0Pager` 切样本时清空 `backendId`/`backendText=waiting onReady…`；Android `setShaders` 改片元通常**不**再 `onReady`（仅切 executor / Surface 会）
- 影响：状态行可能长期停在 waiting；采集 JSONL `backend` 变空；非静默无画
- 处置：建议 — 切样本勿清 backend；仅切 executor 时清

### P2-chip-row-nowrap
- 证据：四 chip 单行 `Row` 无换行
- 影响：窄屏可能挤出/截断
- 处置：可选 — 允许换行或缩小 padding

## 规范候选观察

1. Demo 仅换 `fragmentShader` 时不应清空 onReady/backend 展示；切轨（executor）与切片元语义不同。

（已入本 skill `_backlog`）

## 真机

有 `P1-device-verify`。可执行档：[`device-check-list/demo-acd-samples.md`](device-check-list/demo-acd-samples.md)。

## changelog

请确认已 append：`docs/specs/dualtrack-lite-vs-standard/changelog.md`「Demo 多样本 A/C/D chip」（审核员不代写）。

## 待补桶

无。
