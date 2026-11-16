# 真机/模拟器可执行档 · holm-ios-android-align

- 关联报告：`docs/reviews/review-holm-ios-android-align.md`
- 包名 / 入口：`com.jlj.kuiklyholmui.iosApp` · Home →「示例1 / 双轨」(`holm_runtime_m0`)
- 前置条件：
  - `holm-runtime/./build-ios.sh` → `dist/ios/HolmRuntime.xcframework`
  - `./gradlew :holmDemo:linkPodDebugFrameworkIosSimulatorArm64`（真机用 `IosArm64`）
  - `iosApp`：`pod install`；选 **arm64** Sim 或真机（已排除 sim x86_64）
  - 本波：Present 经 `HolmDemoHolmRuntimeIosBridge`；壳无 `holm_runtime_session_*` 直调；Provider 已由 `HolmIosBridge.configure` 注册

## 步骤

| # | 操作 | 类型 | 期望 |
|---|---|---|---|
| 1 | 冷启 → Home →「示例1 / 双轨」 | navigate | 进 `holm_runtime_m0` |
| 2 | 默认 Runtime，等 ≥2s | assert | 隧道；`onReady.backend=wgpu-m0*`；非深蓝 BLOCKED |
| 3 | 切 Lite | tap | `backend=gles2-ios`；隧道/出画；**无** `LITE_PENDING` |
| 4 | 再切回 Runtime | tap | 隧道恢复；`backend=wgpu-m0*`；不崩 |
| 5 | Runtime 下 maxFps 30→60/0 | assert | fps/间隔可感知或合理；不崩 |
| 6 | renderScale 1.0→0.5→1.0 | assert | 可糊再清；不崩 |
| 7 | 旋转 / resize / 二次进页 | assert | 不崩；岛仍填满 |
| 8 |（尽量）真机 arm64 重复 #2–#4 | assert | 同 Sim |
| 9 |（可选负向）特制未 register 宿主 | assert | 切轨/start → `BLOCKED` 含 HolmProvider；默认 Demo 可跳过 |
| 10 |（负向）缺 xcframework / 未 build-ios | assert | 链接失败或 create → `BLOCKED`；无 GLES 冒充 Runtime |

类型合法值：`tap` `assert` `scroll` `input` `wait` `screenshot` `track_logcat` `navigate`

## 本机结果（iPhone Duo Sim · 2026-11-16）

| # | 结果 | 证据 |
|---|---|---|
| 1–2 | **通过** | ContentView 直进 `holm_runtime_m0`；`_holm-ios-duo-runtime.png`：`executor=runtime` · `backend=wgpu-m0` · `fps≈58` · 隧道出画 |
| 3 | **通过（冷启 Lite）** | `_holm-ios-duo-lite.png`：`executor=lite` · `backend=gles2` · 隧道出画 · **无** `FRAGMENT_REQUIRED` / `LITE_PENDING`（修：`didMoveToWindow` 早于 Compose attr 时 defer 空片元报错，晚到 `setCss_fragmentShader` 补 `onReady`） |
| 4 | **部分** | 冷启各轨已过；本机无 Simulator AX / `cliclick`，热切 Lite↔Runtime **未点到**；对称 teardown 仍靠代码审 |
| 5–7 | 未跑 | — |
| 8 | 未跑 | 真机 arm64 |
| 9–10 | 跳过 / 代码路径 | Demo 默认已 register；缺库负向仍为链接期 |

截图目录：`docs/reviews/device-check-list/_holm-ios-duo-*.png`

## 覆盖对照

| 审查缺口 | 本表步骤 |
|---|---|
| FullscreenPass / bridge Present | #2 #4 |
| Lite↔Runtime 对称 teardown | #3 #4 |
| maxFps / renderScale | #5 #6 |
| 生命周期 | #7 |
| 真机 | #8 |
| Provider / 缺库负向 | #9 #10 |
