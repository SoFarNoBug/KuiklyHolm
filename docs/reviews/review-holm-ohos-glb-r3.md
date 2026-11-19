# Review · holm-ohos-glb-r3

- 结论：通过
- 依据：对照 [`review-holm-ohos-glb-r2.md`](review-holm-ohos-glb-r2.md)；FPS 速速改 diff（`peek`/`copy` 仅 pending；`presentModelFrame` 去 `currentPacked`；`ExpectedRate` ≥55→120）；用户确认真机验证 OK（`FMR0223927006189`）
- 日期：2026-11-19
- diff：`HolmOhosModelBridge` · `HolmWgpuOhosBridge` · `holm_xc_plugin` `ApplyExpectedFps` / `PullModelMeshToGles`

## 对照上轮

| 桶 ID | 状态 |
|---|---|
| P1-device-verify | **已关** — 用户确认验证 OK；表见 [`device-check-list/holm-ohos-glb.md`](device-check-list/holm-ohos-glb.md) |
| P2-model-session-map-unsync | **仍建议**（可选）— 仍依赖 `HolmXcMutex` 串行；非放行阻断 |
| （本轮新开）P2-maxfps-ge55-peak | **新开（可选）** — `maxFps≥55` 不再钉 ExpectedRange；见下 |
| （本轮新开）P2-per-frame-float-alloc | **新开（可选）** — wgpu 帧仍 `fillMvp`/`colorRgba`/`light` 小数组分配 |

## P0

无。

## P1

无。

## P2

### P2-model-session-map-unsync
- 证据：同 r1/r2；壳 `HolmXcMutex` 串行 OnFrame / NAPI
- 处置：可选

### P2-maxfps-ge55-peak
- 证据：`holm_xc_plugin.cpp` `ApplyExpectedFps` ≥55 分支 `expected=120`（修 90Hz≈45）
- 影响：Demo `maxFps=60` Attr 不再软限到 60，跟面板峰值
- 处置：可选（接受为修 45 的代价；真要软限走 `<55`）

### P2-per-frame-float-alloc
- 证据：`HolmWgpuOhosBridge.presentModelFrame` 每帧仍 `fillMvp` / `colorRgba` / `lightDirIntensity` 分配小 `FloatArray`
- 影响：远小于整模 snapshot；未证掉帧
- 处置：可选

## 规范候选观察

- GLES Model mesh 上传须只消费 pending snapshot，禁止每帧 `currentPacked`/`glBufferData` 整模重灌（Tokyo 级资产必炸帧）｜peer：`kuikly-ohos-coding` 池可抄

## 真机

`P1-device-verify` 已关。可执行档：[`device-check-list/holm-ohos-glb.md`](device-check-list/holm-ohos-glb.md)。机型：`FMR0223927006189`；用户确认验证 OK（含 FPS 回修后 GLES Tokyo / wgpu 脱 ~45）。

## changelog

已见 FPS FIX append（kuikly-dev-ohos + holm-content-gltf）。可选：码农再 append「审查 r3 · 通过」。

## 待补桶

无。
