# Review · wgpu-error-to-business

- 结论：有条件通过
- 依据：用户确认 Plan「wgpu 错误全量 onError」（选项 2 → Implement）；changelog `docs/specs/dualtrack-lite-vs-standard/changelog.md`（2026-11-15 wgpu 错误全量 onError）
- 日期：2026-11-15
- diff：`holm-runtime` error_bridge/naga/sink + H5/Android/iOS/Ohos 宿主透传 + Demo sticky；wasm 已重编

## P0

无。

## P1

### P1-plan-soft-drift 四端「完整 message」未齐：H5 主线程回退 + Ohos tick
- 证据：
  - H5 Worker 路径：`HolmRuntimePumpSource` `flushSessionErrors` + `setCustomFs` throw（约 `:72-84,:230-294`）— 主路径已接线
  - H5 主线程回退：`KRHolmSurfaceView.startRuntimeLoop` tick 失败仍 `fireError(BLOCKED, "holm-runtime tick failed")`（约 `:689-695`）；整段**未**调 `sessionDrainErrors`；`HolmRuntimeNative.sessionTick` catch 只返 `false`、丢弃异常文案（约 `:171-185`）
  - Ohos：`ApplyRuntimeShaders` 已读 `last_error_*`（`holm_xc_plugin.cpp` 约 `:243-250`）；`OnFramePump` tick 失败仍 `FireError(..., "BLOCKED", "holm_runtime tick failed")`（约 `:334-338`），未读 last_error
- 影响：Worker/Android setCustomFs 主路径可达；主线程回退与 Ohos tick 失败仍泛化文案，未达 Plan「全量可捕获 + 完整 message」
- 处置：必修 — 与 Android/iOS 对齐：tick 失败读 last_error；主线程泵每帧/失败路径 drain；sessionTick 透传 throw message

### P1-device-verify H5 Runtime 负向 onError / sticky
- 证据：changelog「本单无真机验证」；naga smoke 不能代替浏览器 `onError` 与 sticky
- 影响：放行条件
- 处置：必修 → 可执行档 [`device-check-list/wgpu-error-to-business.md`](device-check-list/wgpu-error-to-business.md)
- 代码审不了：坏 WGSL 是否出红字含 reserved keyword；合法 IQ 是否出画；fps>0 是否冲掉 SHADER_/WGPU_；硬刷 8080 是否吃到新 wasm

## P2

### P2-duplicate-onerror-setCustomFs
- 证据：`set_custom_fs` 失败既 `error_sink.emit` 又 `Err`（`session_core.rs` 约 `:307-310`）；Android `!ok` 先 `fireError(lastError*)` 再 `flushRuntimeErrors`/`drain`（`KRHolmSurfaceView.kt` 约 `:538-548`）；H5 Worker catch 先 `flushSessionErrors` 再 `emitError("SHADER_CUSTOM", …)`（`HolmRuntimePumpSource.kt` 约 `:235-237`）
- 影响：同一次失败可双发 onError（Demo 多覆盖同文案；业务若计数会翻倍）
- 处置：建议 — 宿主只走 drain **或** 只走 Result/lastError，不要两条都 fire

### P2-shader-custom-message-prefix
- 证据：wasm `set_custom_fs` map_err 前缀 `SHADER_CUSTOM: {e}`（`session.rs` 约 `:76`），宿主再以 code=`SHADER_CUSTOM` fire → 展示易成 `onError SHADER_CUSTOM: SHADER_CUSTOM: WGSL:…`
- 影响：可读性
- 处置：可选 — JsValue 只带 `e`，code 由宿主填

## 规范候选观察

- Runtime 错误桥：`emit`+`Result`/`throw` 与宿主 `drain` 只能选一侧 fire，避免双发｜可抄码农池
- H5 Runtime 主线程回退须与 Worker 同契约（lastError / drain）｜与本单 P1-plan-soft-drift 同源

## 真机

有 `P1-device-verify`。可执行档：[`device-check-list/wgpu-error-to-business.md`](device-check-list/wgpu-error-to-business.md)。

## changelog

请确认 `docs/specs/dualtrack-lite-vs-standard/changelog.md` 已含「wgpu 错误全量 onError」条目（审核员不代写）。整改后请 append 审查回执行。

## 待补桶

无。
