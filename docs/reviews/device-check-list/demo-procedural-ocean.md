# 真机可执行档 · demo-procedural-ocean

- 关联报告：`docs/reviews/review-demo-procedural-ocean.md`
- 包名 / 入口：`com.jlj.kuiklyholmui` · Home →「示例 / 双轨」(`holm_runtime_m0`)
- 前置条件：
  - 已装含本单 Demo 的包（优先 Android arm64；H5 亦可）
  - Runtime 轨需对应 `.so` / wasm；Lite 轨 GLES

## 步骤

| # | 操作 | 类型 | 期望 |
|---|---|---|---|
| 1 | 进 `holm_runtime_m0`，点「程序化海洋」，默认/切 Runtime | tap | 海面/天空可辨；`backend=wgpu-m0*`；有 `fps=`；无持续红字 |
| 2 | 同页看出处小字 | assert | 可见「作者 afl_ext · 出处 shadertoy.com · MIT」 |
| 3 | 切 Lite | tap | Lite 出画（可更糊/更低帧）；`backend=gles2*`；不崩 |
| 4 | Runtime ↔ Lite 互切 2 次 | tap | 均恢复出画；无黑屏卡死 |
| 5 | 切「故意失败」再切回「程序化海洋」 | tap | 失败有 `SHADER_*`；切回海洋可出画且红字清 |
| 6 | maxFps=30 下 Runtime 海洋 ≥3s | assert | 可感知降帧或不崩；岛仍有动画 |

类型合法值：`tap` `assert` `scroll` `input` `wait` `screenshot` `track_logcat` `navigate`

## 覆盖对照

| 审查缺口 | 本表步骤 |
|---|---|
| Runtime WGSL 出画 | #1 |
| MIT 出处 UI | #2 |
| Lite GLES 出画 | #3 |
| 双轨互切 | #4 |
| 失败样本不污染 | #5 |
| 弱端/限帧 | #6 |
