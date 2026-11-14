# 真机可执行档 · delete-five-scenes

- 关联报告：`docs/reviews/review-delete-five-scenes-r2.md`（基线 r1：`review-delete-five-scenes.md`）
- 包名 / 入口：`com.jlj.kuiklyholmui` · Home →「示例1 / 双轨」(`holm_runtime_m0`)
- 前置条件：
  - Runtime 产物已于 2026-11-14 重编入仓（`.so` / wasm 无 `holm-frost` 等）；装包用当前树即可。若本地又改了 `holm-runtime` 源，须再跑 `build-android.ps1` / `build-wasm.ps1`
  - arm64 真机：`:androidApp:installDebug`
  - logcat：`adb logcat | findstr holm-perf`（可选采集）

## 步骤

| # | 操作 | 类型 | 期望 |
|---|---|---|---|
| 1 | 冷启 → Home | assert | **仅**「示例1 / 双轨」主按钮；**无** `holm_spike` / 五席 / 霜化入口 |
| 2 | 进 `holm_runtime_m0` | navigate | 可见 Lite/Runtime、**示例1** chip、maxFps、采集；**无**霜化/柔光/液态 chip |
| 3 | 默认 Runtime + 示例1，等 ≥2s | assert | 霓虹隧道；`onReady backend=wgpu-m0`（或 `wgpu-m0-gl`）；**非**霜化/柔光/液态；**非**深蓝 BLOCKED |
| 4 | 切 Lite，等 ≥2s | tap | 仍为隧道；`backend=gles2`；无花屏 |
| 5 | （负向·空片元）临时清空 Demo `fragmentShader` 或等价，重进 Lite | assert | `onError`（如 SHADER_WRAP / fragment required）+ 深蓝清屏；**绝不**出现霜化画面 |
| 6 | （负向·Runtime 无 custom）清空 custom FS 且 `setScene` 传 `frost`（或旧席名） | assert | ClearTri/清屏类占位，**不**出霜化（验证新 so） |
| 7 | 恢复示例1；Runtime 采集 ≥5s maxFps=60 | track_logcat | `[holm-perf]` 有 `executor=runtime` 与 fps |
| 8 | （可选）移走 so 重装进 Runtime | assert | `BLOCKED` + 深蓝；无假 GLES 霜化冒充 |
| 9 | （可选）iOS 模拟器开岛 | assert | 清屏 + `FRAGMENT_REQUIRED`（本波未接宿主片元） |

类型合法值：`tap` `assert` `scroll` `input` `wait` `screenshot` `track_logcat` `navigate`

## 覆盖对照

| 审查缺口 | 本表步骤 |
|---|---|
| 无五席入口 | #1 #2 |
| 隧道 Lite/Runtime 通面 | #3 #4 |
| 空片元无 frost 回退 | #5 |
| 重编 so 后 scene=frost 不再出席 | #6 |
| 采集 | #7 |
| 缺 so BLOCKED | #8 |
| iOS 诚实失败 | #9 |
