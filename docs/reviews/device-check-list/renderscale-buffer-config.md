# 真机可执行档 · renderscale-buffer-config

- 关联报告：`docs/reviews/review-renderscale-buffer-config.md`
- 包名 / 入口：鸿蒙优先 `ohosApp` · Home →「示例1 / 双轨」(`holm_dualtrack`)；对照可扫 Android / iOS / H5
- 前置条件：
  - 已装含 `renderScale` 的 HAP / APK / iOS / H5 包
  - 状态行可见 `scale=` 与 fps
  - 重点样本：IQ、电缆巢（填率敏感）

## 步骤

| # | 操作 | 类型 | 期望 |
|---|---|---|---|
| 1 | 冷启 →「示例1 / 双轨」 | navigate | 进页；默认 `scale=1.0` |
| 2 | Runtime + 隧道，`scale=1.0`，记 fps ≥3s | assert | 出画；fps 与改前同档（基线） |
| 3 | 同轨切 `scale=0.5`，记 fps ≥3s | tap | 不崩；画面可变糊；fps **不低于** #2（鸿蒙 IQ/电缆宜明显升） |
| 4 | 切 `scale=0.25`，再回 `1.0` | tap | 不崩；回 1.0 清晰度恢复 |
| 5 | Lite + IQ：`1.0` → `0.5`，各记 fps | assert | 同 #3；backend=`gles2-ohos`（或端对应 Lite） |
| 6 | Lite + 电缆巢：同上 | assert | fps 可升或不崩；记数 |
| 7 | Runtime↔Lite 互切，保持 `scale=0.5` | tap | 切轨不崩；scale 语义仍生效 |
| 8 | `maxFps=30` + `scale=0.5` | assert | 两属性正交；不互相把帧率掐死到异常低 |
| 9 |（可选）Android/iOS/H5 各扫 #3–#4 | assert | `scale=1` 观感不变；`0.5` 可糊/可升 fps |

类型合法值：`tap` `assert` `scroll` `input` `wait` `screenshot` `track_logcat` `navigate`

## 覆盖对照

| 审查缺口 | 本表步骤 |
|---|---|
| scale=1 基线不变 | #2 |
| 0.5 填率/糊感 | #3 #5 #6 |
| 热切 / 回 1.0 | #4 |
| 双轨 × scale | #7 |
| 与 maxFps 正交 | #8 |
| 跨端抽检 | #9 |
