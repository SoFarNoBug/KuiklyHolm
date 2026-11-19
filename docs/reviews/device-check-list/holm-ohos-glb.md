# 真机可执行档 · holm-ohos-glb

- 关联报告：[`docs/reviews/review-holm-ohos-glb-r4.md`](../review-holm-ohos-glb-r4.md)（通过；r3→r4 关 map/scratch P2）
- 包名 / 入口：`ohosApp` · Home →「Model / glTF」(`holm_model`)
- 前置条件：
  - 鸿蒙 arm64 真机；`ohos_build` / DevEco 编过 entry
  - `entry/libs/arm64-v8a` 含 `libkmp_wgpu_native.so`（含 `kmp_wgpu_model_*`）、`libkmp_draco_native.so`、`libshared.so`
  - rawfile `models/*.glb`；Ability 启动拷到 `cacheDir/models/`

## 结果

- 机型：`FMR0223927006189`（hdc）
- 日期：2026-11-19
- 结论：**通过**（用户确认验证 OK；含 FPS 回修：GLES Tokyo 不再 ~3fps、wgpu 不再钉 ~45）

## 步骤

| # | 操作 | 类型 | 期望 | 结果 |
|---|---|---|---|---|
| 1 | 冷启 → Home → `holm_model` | navigate | 进入页；有资产 chip + gl/wgpu chip；无编译红屏 | OK |
| 2 | 默认 `renderer-gl`，资产 Duck，等 ≥2s | assert | 可见整模；`onReady.backend`≈`gles2-model`；无 `MODEL_UNSUPPORTED` | OK |
| 3 | 单指拖岛内 | tap | 模型绕转；松手停 | OK |
| 4 | 切 Flamingo / 其它非 Tokyo 资产 | tap | 换模可见；orbit **不重置**；无明显整屏闪黑 | OK |
| 5 | 切 `LittlestTokyo`（Draco），等 decode | wait | 最终可见 Tokyo；失败须 `onError MODEL_LOAD`；出画后 FPS 明显 >3 | OK |
| 6 | 打开灯光 sheet：Lit on，拧方位/强度 | tap | GLES 下明暗/方向可见变化 | OK |
| 7 | 切 `executor=renderer-wgpu`，等 ≥2s | tap | 仍见当前资产；`onReady.backend` 为 `wgpu-m0*`（**不得**静默 GLES）；FPS 不长期钉 ~45 | OK |
| 8 | wgpu 下再拧灯光 | tap | 明暗变化可接受 | OK |
| 9 | wgpu↔gl 来回切 2 次 | tap | 每次均可出画；orbit 大致保留；无崩 | OK |
| 10 | （负向）临时改 Demo 资产为不存在路径重装 | assert | `onError MODEL_LOAD`；不崩 | OK（用户总验） |
| 11 | 反复进出 `holm_model` 2 次 | navigate | 无崩；第三次仍可出画 | OK |

类型合法值：`tap` `assert` `scroll` `input` `wait` `screenshot` `track_logcat` `navigate`

## 覆盖对照

| 审查缺口 | 本表步骤 |
|---|---|
| GLES 真画 + orbit | #2–#3 |
| 切资产不闪 / 保 orbit | #4 |
| Tokyo / Draco + FPS | #5 |
| 灯光双轨 | #6 #8 |
| wgpu ModelPass 真画 + 脱 45 | #7 |
| 切 executor | #9 |
| 失败路径 | #10 |
| 进出稳定 | #11 |
