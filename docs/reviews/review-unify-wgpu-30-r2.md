# Review · unify-wgpu-30 · r2

- 结论：有条件通过
- 依据：上轮 [`review-unify-wgpu-30.md`](review-unify-wgpu-30.md)；码农速速改 changelog「审查回执行 · unify-wgpu-30 P2」
- 日期：2026-11-15
- diff：`session_core::build_pipe` poll `map_err`；wasm/android 产物重编

## 对照上轮

| 项 | 状态 |
|---|---|
| P1-plan-soft-drift iOS/Ohos rebuild BLOCKED | 未关 — 本机仍无 apple-ios / ohos 工具链 |
| P1-device-verify 通面/回退 | 未关 — H5 已冒烟（见下）；Android/iOS/Ohos 真机仍缺 |
| P2-poll-error-swallowed | 已关 — `poll(...).map_err(|e| format!("{CODE_WGPU_INTERNAL}: device poll: {e}"))`（`session_core.rs` build_pipe） |

## P0

无。

## P1

### P1-plan-soft-drift Plan「四端 rebuild」未齐：iOS / Ohos 本机 BLOCKED
- 证据：同 r1；本轮未补两脚本产物
- 影响：同 r1
- 处置：必修 — 有工具链机器补跑

### P1-device-verify wgpu 30 通面 / 回退 / color_space
- 证据：可执行档 [`device-check-list/unify-wgpu-30.md`](device-check-list/unify-wgpu-30.md)
- H5 已冒烟（2026-11-15）：Runtime 隧道 `backend=wgpu-m0` fps≈60 出画；「故意失败」→ `SHADER_CUSTOM` 含 reserved/`ref`；切回隧道恢复出画。未跑 WebGL2 强制回退；Android/iOS/Ohos 未跑
- 影响：放行条件（H5 主路径已过；余端仍缺）
- 处置：必修 → 可执行档剩余步 #2/#4/#5/#6

## P2

无。（上轮 P2 已关）

## 规范候选观察

无。

## 真机

有 `P1-device-verify`。可执行档：[`device-check-list/unify-wgpu-30.md`](device-check-list/unify-wgpu-30.md)。

## changelog

请确认已含「审查回执行 · unify-wgpu-30 P2」（审核员不代写）。

## 待补桶

无。
