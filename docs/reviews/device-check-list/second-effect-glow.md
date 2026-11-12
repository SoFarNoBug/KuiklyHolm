# 真机可执行档 · second-effect-glow

- 关联报告：`docs/reviews/review-second-effect-glow.md`
- 包名 / 入口：`com.jlj.kuiklyholmui` · 页 `holm_spike`（Home → Holm 尖刺）
- 前置条件：Debug 安装成功；设备支持 GLES2；从冷启动进入页

## 步骤

| # | 操作 | 类型 | 期望 |
|---|---|---|---|
| 1 | 打开 `holm_spike` | navigate | 默认霜化：卡片内可见模糊蓝橙条纹+白雾，非纯白噪点 |
| 2 | 点「柔光」 | tap | 约 1s 内切到深色底 + 青/品红/琥珀色柔光团漂移；无整卡黑屏 |
| 3 | 停留观察 3s | assert | 光斑缓慢移动/呼吸；无持续闪烁黑帧 |
| 4 | 点「霜化」 | tap | 回到毛玻璃条纹雾面；切换过程无崩溃 |
| 5 | 快速连点霜化/柔光 5 次 | tap | 最终态与最后一次按钮一致；无崩溃、无永久黑屏 |
| 6 | （可选）logcat 过滤 Holm/SHADER | track_logcat | 无反复 `SHADER_COMPILE` / `SHADER_LINK` 失败刷屏 |

类型合法值：`tap` `assert` `scroll` `input` `wait` `screenshot` `track_logcat` `navigate`

## 覆盖对照

| 审查缺口 | 本表步骤 |
|---|---|
| Android `queueEvent` 热换 program 观感 | #1–#4 |
| 快速切换稳定性 | #5 |
| shader 编译失败是否静默 | #6 |
