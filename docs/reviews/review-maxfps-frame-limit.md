# Review · maxfps-frame-limit

- 结论：通过
- 依据：Plan「HolmSurface maxFps 帧率限制」；`docs/specs/stage0-lite-spike/changelog.md`「HolmSurface maxFps 限帧」
- 日期：2026-11-12

## P0

无。

## P1

无。

## P2

无。

## 规范候选观察

无。

## 真机

无。（Plan / changelog 写明本单无真机验证；验收为 Demo 目视 fps + 编译门禁，代码侧 Attr→泵接线已闭环）

## Plan 对齐

- Attr/`HolmSurface`：`maxFps` 默认 60、`0` 不限、`<0`→0 — 已覆盖
- H5：`HolmGlPumpSource` nextDue 累加器 + 跳过仍挂 rAF；`init`/`set` 带 maxFps；`onPerf` 仅真实 draw — 已覆盖
- Android：`PROP_MAX_FPS`→`queueEvent`/`setMaxFps`；`onDrawFrame` 同语义早退；playing 仍切 CONTINUOUSLY/WHEN_DIRTY — 已覆盖
- Demo：`0/60/120` + 状态行 `maxFps=` — 已覆盖
- 非目标：未改用 setTimeout 主时钟、未改 shader、未引入独立渲染定时器 — 符合
- changelog 已 append

## changelog

请确认 `docs/specs/stage0-lite-spike/changelog.md`「HolmSurface maxFps 限帧」条目仍在（审核员不代写）。

## 待补桶

无。
