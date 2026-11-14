# 真机可执行档 · android-runtime-dualtrack

- 关联报告：`docs/reviews/review-android-runtime-dualtrack.md`
- 包名 / 入口：`com.jlj.kuiklyholmui` · Home →「示例1 / 双轨」(`holm_runtime_m0`)
- 前置条件：
  - arm64 真机；`:androidApp:installDebug`；`libholm_runtime.so` 已由 `holm-runtime/build-android.ps1` 打进 `jniLibs/arm64-v8a`
  - 本轮含：SurfaceView Runtime、Lite highp 壳、Session 非 sRGB、LITE_BODY/RUNTIME_FS 无片元 `pow(0.4545)`；**官方五席已删**
  - logcat 可滤：`adb logcat -s System.out:I *:S` 或搜 `[holm-perf]`

## 步骤

| # | 操作 | 类型 | 期望 |
|---|---|---|---|
| 1 | 冷启 → Home → 点「示例1 / 双轨」 | navigate | 进入 `holm_runtime_m0`；可见 Lite/Runtime、示例1、maxFps、采集钮；**无**霜化/柔光/液态 chip |
| 2 | 默认 Runtime + 示例1，等 ≥2s | assert | `onReady backend=wgpu-m0`（或 `wgpu-m0-gl`）；霓虹隧道画面与 `fps=`；**非**深蓝 BLOCKED；**非**霜化 |
| 3 | Runtime + 示例1：开始采集 ≥8s，maxFps=60 | track_logcat | `[holm-perf]` JSONL：`executor=runtime`，`fps`≈60 档；`backend` 非假 gles |
| 4 | 同条件 maxFps=120 再采 ≥8s | track_logcat | `fps` 明显高于 60（高刷机接近 120）；观感可与 60 相近 |
| 5 | 切 executor=lite（保持示例1） | tap | **有隧道动画**；状态/`onReady` 最终为 `backend=gles2`（**不得**长期停在 `wgpu-m0`） |
| 6 | Lite 示例1：开始采集 ≥5s | track_logcat | JSONL：`executor=lite` 且 `backend=gles2`；无花屏噪点；发白较改前缓解 |
| 7 | 再切回 Runtime | tap | 隧道再起；无崩；可再 fireReady wgpu |
| 8 | 反复进出本页 3 次 | navigate | 无崩；第三次仍可 Runtime 通面 |
| 9 | （负向）临时移走/重命名 arm64 `.so` 重装，进 Runtime | assert | `onError BLOCKED` + 深蓝底；**无**假 GLES 霜化冒充 wgpu |
| 10 | （可选）非 arm64 或模拟器 | assert | Runtime BLOCKED 口径同 #9；Lite 仍可画则记 PARTIAL |

类型合法值：`tap` `assert` `scroll` `input` `wait` `screenshot` `track_logcat` `navigate`

## 覆盖对照

| 审查缺口 | 本表步骤 |
|---|---|
| Runtime / Lite 隧道通面 | #2–#7 |
| 非 sRGB / 去 pow 发白 | #2 #6 |
| Lite highp 无花屏 | #6 |
| maxFps 计量 | #3–#4 |
| executor 热切 + backend 诚实 | #5–#7 |
| 进出页稳定 | #8 |
| 缺 .so 不静默 GLES（ADR 0006） | #9 |
| 非 arm64 | #10 |
| 无五席入口 / 无 frost 回退 | #1 #2 #9 |
