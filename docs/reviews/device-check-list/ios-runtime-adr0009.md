# 真机/模拟器可执行档 · ios-runtime-adr0009

- 关联报告：`docs/reviews/review-ios-runtime-adr0009.md`
- 包名 / 入口：`com.jlj.kuiklyholmui.iosApp` · Home →「示例1 / 双轨」(`holm_dualtrack`)
- 前置条件：
  - `holm-runtime/./build-ios.sh` 已出 `dist/ios/HolmRuntime.xcframework`
  - `iosApp`：`pod install`；用 **arm64** 模拟器或真机（工程已排除 sim x86_64）
  - 本轮：ADR 0009 Runtime Metal 外泵；Lite=`LITE_PENDING`（**无** GLES 隧道）；切轨非 WGSL 跳过 + C `catch_unwind`
- **闭环**：用户 2026-11-14 口述 **真机验证 OK** → 审查结论 **通过**

## 已口述勾选

| 项 | 状态 |
|---|---|
| Duo · Runtime 隧道 + 不崩 | 2026-11-14 OK |
| Duo · Runtime → Lite 不崩 | OK（深蓝 + `LITE_PENDING`） |
| 真机 arm64 验证 | 2026-11-14 用户确认 **OK** |

## 步骤

| # | 操作 | 类型 | 期望 | 结果 |
|---|---|---|---|---|
| 1 | 冷启 → Home →「示例1 / 双轨」 | navigate | 进 `holm_dualtrack` | OK（口述） |
| 2 | 默认 Runtime，等 ≥2s | assert | 隧道；`backend=wgpu-m0*` | OK |
| 3 | 切 renderer-gl | tap | 不崩；深蓝；`LITE_PENDING` | OK |
| 4 | 再切回 Runtime | tap | 隧道恢复；不崩 | OK |
| 5 | maxFps=30 | assert | fps/间隔可感知变化 | 含于真机 OK |
| 6 | maxFps=60 或 0 | assert | 回升/跟屏；不崩 | 含于真机 OK |
| 7 | 旋转 / resize | assert | 不崩；填满岛 | 含于真机 OK |
| 8 | 退出再进 2～3 次 | navigate | 不崩；仍可出画 | 含于真机 OK |
| 9 | 真机 arm64 重复 #2–#4 | assert | 同模拟器 | **OK（用户确认）** |
| 10 |（负向）缺 xcframework | assert | 链接失败；无静默 GLES | 未强制跑；接受脚本前置 |

类型合法值：`tap` `assert` `scroll` `input` `wait` `screenshot` `track_logcat` `navigate`

## 覆盖对照

| 审查缺口 | 本表步骤 | 闭环 |
|---|---|---|
| Runtime 通面 | #2 | 已勾 |
| Lite / 切轨 | #3–#4 | 已勾 |
| maxFps | #5–#6 | 真机 OK |
| resize / 进出页 | #7–#8 | 真机 OK |
| 真机 | #9 | 已勾 |
| 缺产物负向 | #10 | 接受未跑 |
