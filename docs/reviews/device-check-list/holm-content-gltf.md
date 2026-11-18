# 真机可执行档 · holm-content-gltf

- 关联报告：`docs/reviews/review-holm-content-gltf-r3.md`（上轮 r1/r2 可对照）
- 包名 / 入口：`com.jlj.kuiklyholmui` · Home →「示例 / 双渲染后端」(`holm_dualtrack`)；「Model / glTF」(`holm_model`)
- 前置条件：
  - arm64 Android 真机；`:androidApp:installDebug`
  - `kmp.wgpu.local=true` 且已装含 ModelPass 的 `libkmp_wgpu_native`（或等价本地联调）
  - `assets/models/hero.glb` 已打进包（或确认缺文件时程序立方体回落）
  - 本轮：Content API + Android Model **GLES + 真 wgpu ModelPass**

## 结果

- 机型：Vivo `V2545A`（`10AG720SDK002K5`）
- 日期：2026-11-18
- 结论：**OK**（用户确认闭环；`P1-device-verify` 关）

## 步骤

| # | 操作 | 类型 | 期望 | 结果 |
|---|---|---|---|---|
| 1 | 冷启 → Home → `holm_dualtrack` | navigate | 进入页；有 renderer-gl / renderer-wgpu chip；**无**编译错误红屏 | OK |
| 2 | 默认轨等 ≥2s | assert | 隧道（或当前样本）出画；`onReady` 有 backend；状态行无 sticky 无意义红字 | OK |
| 3 | 切样本 chip（隧道↔IQ 等） | tap | 画面切换；**不**清空合理 backend 展示；故意失败样本有红字，切回正样本红字清除 | OK |
| 4 | 切 executor gl↔wgpu（正样本） | tap | 两轨均可出画；方言匹配；无崩 | OK |
| 5 | 返回 Home → `holm_model` | navigate | 进入 Model 页 | OK |
| 6 | 默认 renderer-gl，等 ≥2s | assert | 可见立方体/模型（非纯深蓝空岛）；`onReady.backend`≈`gles2-model` | OK |
| 7 | 单指拖岛内 | tap | 模型绕转（yaw/pitch）；松手停 | OK |
| 8 | 切 executor=renderer-wgpu，等 ≥2s | tap | 仍可见整模（真 ModelPass，非 GLES 层）；`onReady.backend` 为 `wgpu-m0`/`wgpu-m0-gl`（**不得** `gles2-model`）；无崩；可再拖 | OK |
| 8b | 再切回 gl → 再切 wgpu，各等 ≥2s | tap | **两次**切入 wgpu 均仍见模（覆盖 r2 P0：eagerDestroy 后 recreate）；backend 仍为 wgpu-m0*；无崩 | OK |
| 9 | （负向）改 Demo 为不存在的 assets 路径重装（或临时改名 glb） | assert | `onError MODEL_LOAD` 或程序立方体回落（hero 约定）；**不崩** | OK（hero 回落约定） |
| 10 | 反复进出 `holm_model` / `holm_dualtrack` 各 2 次 | navigate | 无崩；第三次仍可出画 | OK |
| 11 | （可选）H5/iOS/Ohos 挂 Model 或打开 Model 页 | assert | `MODEL_UNSUPPORTED` + 清屏；不崩 | 跳过（本机仅 Android） |

类型合法值：`tap` `assert` `scroll` `input` `wait` `screenshot` `track_logcat` `navigate`

## 覆盖对照

| 审查缺口 | 本表步骤 |
|---|---|
| DualTrack Content 迁移观感 | #1–#4 |
| Model GL 真画 + orbit | #5–#7 |
| Model + wgpu 首进真画 | #8 |
| Model + wgpu 二次切入（r2 P0） | #8b |
| 缺资源 / 失败路径 | #9 |
| 进出稳定 | #10 |
| 非 Android BLOCKED | #11 |
| onReady.backend 诚实性 | #6 #8 #8b |
