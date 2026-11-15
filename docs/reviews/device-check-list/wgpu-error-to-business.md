# 真机可执行档 · wgpu-error-to-business

- 关联报告：`docs/reviews/review-wgpu-error-to-business.md`
- 包名 / 入口：`com.jlj.kuiklyholmui` · Home →「示例 / 双轨」(`holm_runtime_m0`)
- 前置条件：
  - 已跑 `holm-runtime/build-wasm.ps1` 且 `h5App/.../holm-runtime/` 时间戳新于改码
  - 已 `:holmDemo:packLocalJsBundleDebug`；按 `docs/specs/kuikly-dev-h5/runbook.md` 起 **8080 + 8083**；**硬刷新** 8080
  - 负向：页内 chip「故意失败」（`BAD_COMPILE`）；勿再手改源码

## 步骤

| # | 操作 | 类型 | 期望 |
|---|---|---|---|
| 1 | 冷启 8080/8083 → Home →「示例 / 双轨」 | navigate | 进 `holm_runtime_m0`；可见 Lite/Runtime、样本 chip（含**故意失败**）、maxFps |
| 2 | Runtime + 隧道（合法 WGSL），等 ≥2s | assert | `onReady`；有画面；`fps=`；**无**持续红字 |
| 3 | 切「IQ原语」（合法），等 ≥2s | assert | 场景可辨；无 `SHADER_CUSTOM` 红字 |
| 4 | Runtime + 点「故意失败」 | tap | `onError SHADER_CUSTOM` 文案含 **reserved** / `ref`；岛可深蓝/清屏；**不**当成功特效 |
| 5 | 负向仍显示红字时，等 fps 行出现且 >0（若泵仍转）或观察 ≥3s | assert | 红字**不被** onPerf 清空（sticky） |
| 6 | 点回「隧道」或「IQ原语」 | tap | 出画；红字被清掉（换样本清 sticky） |
| 7 | （可选）Lite +「故意失败」 | tap | `onError SHADER_COMPILE`（或等价）；再切回隧道可恢复 |
| 8 | （可选）Android arm64 Runtime +「故意失败」 | assert | `onError` 带完整 message；非仅 `Runtime setCustomFs failed` |

类型合法值：`tap` `assert` `scroll` `input` `wait` `screenshot` `track_logcat` `navigate`

## 覆盖对照

| 审查缺口 | 本表步骤 |
|---|---|
| 合法 Runtime 通面 | #2 #3 #6 |
| 坏 WGSL → onError 完整文案 | #4 |
| sticky 不被 fps 冲掉 | #5 |
| 换回合法样本可恢复 | #6 |
| Lite 负向可选 | #7 |
| Android 可选对齐 | #8 |
