# Review · renderscale-buffer-config

- 结论：有条件通过
- 依据：Plan「DualTrack 可配置 renderScale」；`docs/specs/dualtrack-lite-vs-standard/changelog.md`「renderScale 可配置缓冲」；ADR 0010/0011 Consequences
- 日期：2026-11-16
- 范围：`HolmSurface.renderScale` 四端 Lite+Runtime；Demo 档位。不含 march 砍步、不含强制默认 1×

## P0

无。（编装期曾缺 `libholm_xc` `.d.ts` `setRenderScale` → ArkTS `P0-build-break`；本轮已补 `types/libholm_xc/index.d.ts` 后重编）

## P1

### P1-device-verify（未关）四端 scale 热切与鸿蒙降采样实效
- 证据：Plan 验收要求 `scale=1` 观感不变、`0.5` IQ/电缆巢 fps 上升、热切不崩；代码无法证明 `SET_BUFFER_GEOMETRY` / `setFixedSize` 在真机上的填率收益
- 影响：放行条件未闭环
- 处置：必修 → 可执行档 [`device-check-list/renderscale-buffer-config.md`](device-check-list/renderscale-buffer-config.md)
- 代码审不了：鸿蒙 `SET_BUFFER_GEOMETRY` 失败时「仅逻辑尺寸」是否仍糊且省 GPU；Android fixedSize 与 compositor 缩放；iOS Lite 再降是否糊；H5 canvas 缩后是否清晰度/fps 符合预期

### P1-concurrency（未关 · 建议观察）热改 scale 与帧泵交错
- 证据：Ohos `NapiSetRenderScale`→`ApplyDrawSizeNow` 持 `HolmXcMutex`；Android `setFixedSize` 可能触发异步 `surfaceChanged` 与当前轨泵交错
- 影响：极端热切可能一帧错尺寸或闪一下；未论证必崩/必错画
- 处置：建议 — 真机表热切 1.0↔0.5↔0.25；若闪屏再收紧

## P2

### P2-napi-dts-sync
- 证据：NAPI `ExportFn(setRenderScale)` 与 `types/libholm_xc/index.d.ts` 曾不同步导致编不过
- 影响：以后加 NAPI 易再漏 `.d.ts`
- 处置：建议 — 改 `holm_xc` 导出必同改 `index.d.ts`

### P2-geometry-fail-soft
- 证据：`GetSurfaceSize` 几何失败只 WARN，仍用 scaled 逻辑尺寸喂 Lite/Runtime（`holm_xc_plugin.cpp`）
- 影响：若窗口缓冲未缩、viewport 已缩，可能黑边/拉伸；changelog 已记
- 处置：可选 — 真机确认失败码；必要时升级为可见 `onError`

## 规范候选观察

1. 鸿蒙 NAPI 新导出必须同步 `entry/src/main/cpp/types/lib*/index.d.ts`，否则 ArkTS 编译失败（本单已踩）。
2. DualTrack 性能对比须标注 `renderScale` 与端基线（物理 / 点 / CSS），避免跨端数字误读。

（已入本 skill `_backlog`）

## 真机

有 `P1-device-verify`。可执行档：[`device-check-list/renderscale-buffer-config.md`](device-check-list/renderscale-buffer-config.md)。

## Plan 对齐

- Attr 默认 1、`(0,1]`、相对基线 — 已覆盖
- 四端 Lite+Runtime — 已覆盖（Ohos SET_BUFFER_GEOMETRY；Android setFixedSize；iOS contentScale/drawable；H5 CSS×scale）
- Demo `1.0/0.5/0.25` — 已覆盖
- 非目标：未强制默认降采样、未砍 march — 符合
- changelog：dualtrack + 各端已 append（审核员不代写）

## changelog

请确认 `docs/specs/dualtrack-lite-vs-standard/changelog.md`「renderScale 可配置缓冲」及各端 changelog 条目仍在。

## 待补桶

无。
