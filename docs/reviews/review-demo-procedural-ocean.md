# Review · demo-procedural-ocean

- 结论：有条件通过
- 依据：口令「程序化海洋 / afl_ext / shadertoy / 直接开始编码」；changelog「Demo · 程序化海洋 MIT」
- 日期：2026-11-15
- diff：`ProceduralOceanShaders.kt`（新）+ `DemoSampleCatalog` / `HolmRuntimeM0Pager` / `CONTEXT.md`

## P0

无。

## P1

### P1-device-verify 程序化海洋双轨出画
- 证据：changelog「本单无真机验证」；Lite/Runtime 均为重 raymarch 海面，代码审不能代替编译出画与弱端帧率
- 影响：放行条件
- 处置：必修 → 可执行档 [`device-check-list/demo-procedural-ocean.md`](device-check-list/demo-procedural-ocean.md)
- 代码审不了：Runtime WGSL / Lite GLES 是否出画；海面与天空是否可辨；弱端是否卡死；故意失败切回是否恢复

## P2

### P2-lite-runtime-wave-iters
- 证据：Lite `ITERATIONS_RAYMARCH/NORMAL=8/16`，Runtime `12/24`（原版 12/36）
- 影响：双轨观感与性能刻意不对齐；非静默失效
- 处置：建议 — Demo 文案或自测说明「Lite 降档」；真机表已覆盖双轨

### P2-json-example-gap
- 证据：`PROCEDURAL_OCEAN.jsonExample=4`，`BAD_COMPILE` 仍为 `3`
- 影响：JSONL `example=` 编号不连续；采集脚本若假定 1..N 连续会歧义
- 处置：可选 — 顺成 3=海洋、4=故意失败（或文档注明非连续）

## 规范候选观察

无。

## 真机

有 `P1-device-verify`。可执行档：[`device-check-list/demo-procedural-ocean.md`](device-check-list/demo-procedural-ocean.md)。

## changelog

已确认含「Demo · 程序化海洋 MIT」置顶条目（审核员不代写）。

## 待补桶

无。

## 发现面（路由）

- 卡 B：chip → `fragmentFor` → `HolmSurface.fragmentShader` 已接线；`uiCredit()` 在海洋/IQ 可见
- 卡 A/C/D/E：未命中
- 规范：新建 Kotlin + 头注释；MIT 头与 UI 署名齐全；Compose 包名未漂
