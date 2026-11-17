# Review · kmp-wgpu-extract-v1

- 结论：打回
- 依据：Plan「kmp-wgpu 抽芯 v1（方案 B）」；ADR 0013；`docs/specs/dualtrack-lite-vs-standard/changelog.md`「kmp-wgpu 抽芯 v1」
- 日期：2026-11-16
- 范围：`:kmp-wgpu` / `:kmp-wgpu-surface`；`holm_fullscreen`/`primitives`；Android JNI→`AndroidWgpuPlatform`；Android/H5 `HolmRuntimeNative`+`HolmFullscreenPass`。不含 iOS/Ohos 迁移、不含 Compute v1.1、不含 Holm 三包物理拆。

## P0

### P0-silent-fail H5 `sessionTick`/`sessionSetCustomFs` 用 sticky `lastErrorCode` 误判成败
- 证据：
  - [`DeviceSession.js.kt`](../../kmp-wgpu/src/jsMain/kotlin/com/jlj/kmp/wgpu/DeviceSession.js.kt) `sessionTick`：`tick` 成功后若 `lastErrorCode()` 非空仍 `return false`
  - 同文件 `sessionSetCustomFs`：wasm `setCustomFs` 成功后仍用 `lastErrorCode` 非空当失败
  - [`error_bridge.rs`](../../holm-runtime/src/error_bridge.rs)：`drain` 只清 `pending`，**不清** `last_code`/`last_message`；成功路径不 `emit` 清空
  - 旧 [`HolmRuntimeNative`](../../KuiklyHolmCore/src/jsMain/kotlin/com/jlj/kuiklybase/holm/HolmRuntimeNative.kt) 仅以 throw/`Result` 定成败，不把 sticky last 当 tick 失败
- 影响：一次 `SHADER_CUSTOM`/`TICK`/`SURFACE` 后，同 Session 上后续成功装载可被报失败；或 `tick` 被当成失败 → [`KRHolmSurfaceView`](../../KuiklyHolmCore/src/jsMain/kotlin/com/jlj/kuiklybase/holm/KRHolmSurfaceView.kt) 停泵 + BLOCKED，Runtime 轨整条不可用
- 处置：必修 — H5 成败只信 wasm `Result`/throw；或成功后清 last；禁止用未 drain 的 sticky last 否决成功帧

## P1

### P1-device-verify（未关）Android+H5 抽芯后 Runtime 对等通面
- 证据：Plan DoD 要求 Android/H5 隧道、热换片元、resize/destroy、缺库 BLOCKED 对等；代码审无法证真机/浏览器帧与切轨
- 影响：放行条件未闭环（且本轮因 P0 先打回）
- 处置：必修 → 可执行档 [`device-check-list/kmp-wgpu-extract-v1.md`](device-check-list/kmp-wgpu-extract-v1.md)
- 代码审不了：新 JNI so 装包后隧道/热换；H5 修 sticky 后失败样本再成功是否恢复；缺 so BLOCKED

### P1-plan-soft-drift（未关）`kmp-wgpu` 仍暴露 Holm Session 语义粗块
- 证据：Plan「禁止出现在 kmp-wgpu 包下：setCustomFs / setScene / liquid*」；[`DeviceSession.kt`](../../kmp-wgpu/src/commonMain/kotlin/com/jlj/kmp/wgpu/DeviceSession.kt) 仍有 `sessionSetCustomFs`/`sessionSetScene`/`sessionSetLiquid`；README 已标过渡
- 影响：分层叙事软漂移；未导致主路径不可达
- 处置：建议 — 复审前迁入 Holm 适配或 `holmcompat` 包名；或 ADR/Plan 回写允许过渡

## P2

### P2-worktree-noise
- 证据：工作树另有 `iosApp`/`ohosApp`/`h5App` wasm 体量变化等，与本 Plan 抽芯无直接对应
- 影响：审查范围噪音；勿误当成 v1 验收产物
- 处置：可选 — 提交前拆开无关 diff

### P2-abiVersion-hardcode
- 证据：Android `HolmRuntimeNative.abiVersion()` 固定返回 1，未调 native
- 影响：探测失真极低；非主路径
- 处置：可选

## 规范候选观察

1. 跨 FFI 包装 `Result`/throw 时，禁止再用 sticky `lastError*` 二次否决成功路径；`drain` 若不清 last，宿主不得把 last 当帧成败。

（已入本 skill `_backlog`）

## 真机

有 `P1-device-verify`。可执行档：[`device-check-list/kmp-wgpu-extract-v1.md`](device-check-list/kmp-wgpu-extract-v1.md)。

## Plan 对齐

- 方案 B 类型壳 + 粗 FFI — 已覆盖（Android JNI 能力块；无逐 draw）
- `:kmp-wgpu` / `:kmp-wgpu-surface` + Holm 适配 — 已覆盖
- Android+H5 迁移；iOS/Ohos 不迁 — 符合
- `holm_fullscreen` / `kmp_wgpu_native` 叙事 / 产物名兼容 — 符合
- H5 sticky last 误判 — **偏离对等行为**（打回点）
- changelog：dualtrack 已有「kmp-wgpu 抽芯 v1」条目（审核员不代写）

## changelog

请确认 `docs/specs/dualtrack-lite-vs-standard/changelog.md`「kmp-wgpu 抽芯 v1 · 方案 B」仍在；修 P0 后宜补一行 H5 lastError 修复说明。

## 待补桶

无。
