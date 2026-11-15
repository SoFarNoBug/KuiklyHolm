# Review · unify-wgpu-30

- 结论：有条件通过
- 依据：用户确认 Plan「统一 holm-runtime 到 wgpu 30.0.2」；changelog `docs/specs/dualtrack-lite-vs-standard/changelog.md`（2026-11-15 全端统一 wgpu 30.0.2）
- 日期：2026-11-15
- diff：`holm-runtime` Cargo.toml/lock + 单一 `session_core`（删 `session_core_ohos`）+ 各端 Instance/error-scope；产物 wasm/`.so`；ADR 0010 Consequences / README

## P0

无。

## P1

### P1-plan-soft-drift Plan「四端 rebuild」未齐：iOS / Ohos 本机 BLOCKED
- 证据：
  - Plan 验证节要求 `build-ios.sh` / `build-ohos.sh`
  - changelog 自测写明 iOS/Ohos「本机无 apple-ios / ohos target → **BLOCKED**」
  - 本机已证：`build-wasm.ps1` OK；`cargo ndk` arm64 OK；`Cargo.lock` 仅 `wgpu`/`naga` **30.0.2**（无 23.x）
- 影响：共享 `session_core` 降低分叉风险，但 iOS Metal / Ohos `VK_OHOS_surface` **无本单编译产物证据**
- 处置：必修 — 有工具链机器补跑两脚本；changelog 审查回执行补编译结果

### P1-device-verify wgpu 30 通面 / 回退 / color_space
- 证据：changelog「本单无真机验证」；大版本 API（`queue.present` / `CurrentSurfaceTexture` / `SurfaceColorSpace::Auto`）代码审不能代替出画
- 影响：放行条件
- 处置：必修 → 可执行档 [`device-check-list/unify-wgpu-30.md`](device-check-list/unify-wgpu-30.md)
- 代码审不了：H5 WebGPU 与 WebGL2 回退 backend 标签；Android 非 sRGB 无回退白雾；iOS Metal 观感；Ohos Vulkan surface 与升前一致

## P2

### P2-poll-error-swallowed
- 证据：`session_core.rs` `build_pipe`：`let _ = device.poll(wgpu::PollType::wait_indefinitely());` 后直接 `error_scope.pop()`（约 `:266-270`）
- 影响：`PollError` 被丢弃时可能漏抓本应在 Wait 后可见的校验错误（依赖 uncaptured / naga 兜底）
- 处置：建议 — `poll` 失败时 `map_err` 或至少 `emit` 后再 `pop`

## 规范候选观察

无。

## 真机

有 `P1-device-verify`。可执行档：[`device-check-list/unify-wgpu-30.md`](device-check-list/unify-wgpu-30.md)。

## changelog

请确认 `docs/specs/dualtrack-lite-vs-standard/changelog.md` 已含「全端统一 wgpu 30.0.2」条目（审核员不代写）。整改后请 append 审查回执行。

## 待补桶

无。
