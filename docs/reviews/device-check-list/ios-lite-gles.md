# 真机/模拟器可执行档 · ios-lite-gles

- 关联报告：`docs/reviews/review-ios-lite-gles.md`
- 包名 / 入口：`com.jlj.kuiklyholmui.iosApp` · Home →「示例1 / 双轨」(`holm_runtime_m0`)
- 前置条件：
  - `holm-runtime/./build-ios.sh` 已出 xcframework（Runtime 轨仍需）
  - arm64 模拟器或**真机**；本轮重点验证 Lite GLES + 切轨
  - 已知：Duo **模拟器** Lite raymarch 约数 fps 属弱 GLES 预期；**真机**才作 DualTrack 性能口径

## 步骤

| # | 操作 | 类型 | 期望 |
|---|---|---|---|
| 1 | 冷启 →「示例1 / 双轨」 | navigate | 默认 Runtime 隧道；`backend=wgpu-m0*` |
| 2 | 切 Lite，等 ≥2s | tap | 隧道动效；`onReady backend=gles2-ios`；**无** `LITE_PENDING` |
| 3 | 状态行看 fps | assert | fps>0；模拟器可个位数；真机应明显高于 Duo 模拟器约 6fps 档（目视流畅优先） |
| 4 | 切回 Runtime | tap | 隧道恢复；backend 回 wgpu（不长期粘 `gles2-ios`） |
| 5 | 再切 Lite | tap | 再 `gles2-ios`；不崩 |
| 6 | Lite 下 maxFps=30 再 60/0 | assert | fps/间隔可感知变化或不崩 |
| 7 | 旋转 / 进出页 2～3 次 | navigate | 不崩；Lite/Runtime 仍可出画 |
| 8 |（负向）若可空 fragmentShader | assert | 深蓝 + `FRAGMENT_REQUIRED` |
| 9 |（推荐）真机 arm64 重复 #2–#5 | assert | Lite 隧道可用；记录 fps 作 DualTrack 口径 |

类型合法值：`tap` `assert` `scroll` `input` `wait` `screenshot` `track_logcat` `navigate`

## 覆盖对照

| 审查缺口 | 本表步骤 |
|---|---|
| Lite 出画 / gles2-ios | #2 |
| 模拟器 vs 真机 fps 口径 | #3 #9 |
| 切轨 backend 不粘 | #4–#5 |
| maxFps / 生命周期 | #6–#7 |
| 空片元 | #8 |
