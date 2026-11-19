# 真机可执行档 · kmp-draco

- 关联报告：`docs/reviews/review-kmp-draco.md`
- 包名 / 入口：`com.jlj.kuiklyholmui` · Home →「Model / glTF」(`holm_model`) · 资产 chip **Tokyo**（`models/LittlestTokyo.glb`）
- 前置条件：
  - arm64 Android 真机；旁路仓 `~/Studio/kmp/kmp-draco`；`local.properties`：`kmp.draco.local=true`（及既有 `kmp.wgpu.local` 若验 wgpu）
  - 已跑 `kmp-draco-native/build-android.sh`，AAR/jniLibs 含 `libkmp_draco_native.so`
  - **P0-build-break 关闭前**：勿用「关 local、只靠 Central」路径（坐标尚未发布）
  - `:androidApp:installDebug`

## 步骤

| # | 操作 | 类型 | 期望 |
|---|---|---|---|
| 1 | 冷启 → `holm_model` | navigate | 进页；资产 chip 含 Tokyo |
| 2 | 选 Tokyo + executor=renderer-gl，等 ≥3s | assert | **可见**东京场景几何（非纯空岛/非仅报错）；可单指拖转；无崩 |
| 3 | logcat / 页内错误区 | track_logcat | 无持续 `MODEL_LOAD` / `KHR_draco` / `UnsatisfiedLinkError` |
| 4 | 切 executor=renderer-wgpu，等 ≥3s | tap | 仍见模；`onReady.backend` 为 `wgpu-m0*`（非 `gles2-model` 冒充）；可拖 |
| 5 | 切回 Duck 或非 Draco 资产再回 Tokyo | tap | 非 Draco 仍正常；Tokyo 再次可解可拖 |
| 6 | （负向）临时改名/移走 `libkmp_draco_native.so` 或断 local 后重装（可选） | assert | Tokyo → `MODEL_LOAD` / 明确失败文案；**不**静默空白；不崩 |
| 7 | 反复进出 `holm_model` 2 次并再开 Tokyo | navigate | 无崩；仍可出画 |

类型合法值：`tap` `assert` `scroll` `input` `wait` `screenshot` `track_logcat` `navigate`

## 覆盖对照

| 审查缺口 | 本表步骤 |
|---|---|
| Tokyo KHR_draco 真画 | #1–#3 |
| Draco + wgpu ModelPass | #4 |
| 热换资产 / 再解 | #5 |
| 缺 native fail-loud | #6 |
| 进出稳定 | #7 |
