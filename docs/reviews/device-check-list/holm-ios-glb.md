# 真机可执行档 · holm-ios-glb

- 关联报告：`docs/reviews/review-holm-ios-glb.md`
- 包名 / 入口：`iosApp` · Home →「Model / glTF」(`holm_model`)
- 前置条件：
  - iPhone 真机或 arm64 模拟器；Xcode 编过 `iosApp`
  - 仓根存在 symlink：`kmp-wgpu-native` → `../kmp-wgpu/kmp-wgpu-native`，`kmp-draco-native` → `../kmp-draco/kmp-draco-native`
  - `KmpWgpuNative.xcframework` 已含 `kmp_wgpu_model_*`（本波已 `./build-ios.sh`）
  - Bundle 含 `models/*.glb`（含 `Duck` / `Flamingo` / `LittlestTokyo`）

## 结果

- 机型：iPhone Duo Simulator（`B5558DE3-CCC8-4C8B-96F7-F8F198DFDE21` · iOS 27.1）
- 日期：2026-11-19
- 结论：**OK**（用户确认验证通过；`P1-device-verify` 关）

## 步骤

| # | 操作 | 类型 | 期望 | 结果 |
|---|---|---|---|---|
| 1 | 冷启 → Home → `holm_model` | navigate | 进入页；有资产 chip + gl/wgpu chip；无编译红屏 | 过 |
| 2 | 默认 `renderer-gl`，资产 Duck，等 ≥2s | assert | 可见整模（非纯深蓝空岛）；`onReady.backend`≈`gles2-model`；无 `MODEL_UNSUPPORTED` | 过 |
| 3 | 单指拖岛内 | tap | 模型绕转（yaw/pitch 与指滑同向）；松手停 | 过 |
| 4 | 切 Flamingo / 其它非 Tokyo 资产 | tap | 换模可见；orbit **不重置**；无明显整屏闪黑 | 过 |
| 5 | 切 `LittlestTokyo`（Draco），等 decode | wait | 最终可见 Tokyo；允许数秒 decode；失败须 `onError MODEL_LOAD`（禁静默空白） | 过 |
| 6 | 打开灯光 sheet：Lit on，拧方位/强度 | tap | GLES 下明暗/方向可见变化 | 过 |
| 7 | 切 `executor=renderer-wgpu`，等 ≥2s | tap | 仍见当前资产；`onReady.backend` 为 `wgpu-m0*`（**不得**静默 GLES）；可再拖 | 过 |
| 8 | wgpu 下再拧灯光 | tap | 明暗变化（强度乘 albedo 近似可接受） | 过 |
| 9 | wgpu↔gl 来回切 2 次 | tap | 每次均可出画；orbit 大致保留；无崩 | 过 |
| 10 | （负向）临时改 Demo 资产为不存在路径重装 | assert | `onError MODEL_LOAD`；不崩 | 跳过（负向未强制） |
| 11 | 反复进出 `holm_model` 2 次 | navigate | 无崩；第三次仍可出画 | 过 |

类型合法值：`tap` `assert` `scroll` `input` `wait` `screenshot` `track_logcat` `navigate`

## 覆盖对照

| 审查缺口 | 本表步骤 |
|---|---|
| GLES 真画 + orbit | #2–#3 |
| 切资产不闪 / 保 orbit | #4 |
| Tokyo / Draco | #5 |
| 灯光双轨 | #6 #8 |
| wgpu ModelPass 真画 | #7 |
| 切 executor | #9 |
| 失败路径 | #10 |
| 进出稳定 | #11 |
