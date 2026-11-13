# 真机可执行档 · dualtrack-lite-vs-standard

- 关联报告：`docs/reviews/review-dualtrack-lite-vs-standard-r4.md`（亦覆盖基线 / r2 / r3）
- 包名 / 入口：`com.jlj.kuiklyholmui` · Home
- 前置条件：
  - Android Debug 安装成功；设备 GLES2
  - H5：按 `docs/specs/kuikly-dev-h5/runbook.md` 双端口起服；`holm-runtime` wasm 已拷入 resources；本轮 webpack `8080` 已热更整改包
  - iOS：Mac + Simulator/真机；按 `docs/specs/kuikly-dev-ios/runbook.md`（Windows 本机可跳过 iOS 步并记 PARTIAL）

## 步骤

| # | 操作 | 类型 | 期望 |
|---|---|---|---|
| 1 | 冷启动 → Home | navigate | 可见「Lite 五席」与「Runtime M0 旁路」；默认未自动进 Runtime |
| 2 | 点「Lite 五席 (holm_spike)」 | tap | 五席页；默认霜化有画面；`backend=gles2` 或 `webgl*`（非假 wgpu） |
| 3 | 返回 Home → 点「Runtime M0 旁路」 | navigate | `holm_runtime_m0`；`executor=runtime` |
| 4a | （无 wasm/.so）观察 Runtime 2s | assert | `backend=wgpu-blocked` + `onError BLOCKED`；**无**假 GLES 霜化 |
| 4b | （H5 有 wasm）观察 Runtime 2s | assert | `onReady backend=wgpu-m0` 或 `wgpu-m0-gl`；霜化有画面；状态行出现 `fps=`（约 1s）；无残留黄字 error（若曾闪 WORKER_INIT 后成功回退） |
| 5 | H5：切 柔光 / 液态 | tap | 画面可辨不同；无崩；液态旋钮可改 |
| 6 | H5：maxFps 点 30 → 等 ≥1s | assert | `onPerf` 贴近 ~30 |
| 7 | H5：maxFps 点 120 → 等 ≥1s | assert | 提交率可明显高于 60（机差允许；非卡死在 60） |
| 8 | H5：maxFps 点 0 → 等 ≥1s | assert | 高刷机可接近屏刷（如 ~160+）；调度为 rAF+AutoNoVsync（非 setTimeout(0) 空转）；CPU 不应长期打满 |
| 9 | （回归 P0）同页若可切换 executor 至 lite，或出页再进 Lite | navigate | Lite WebGL **重新**有画面；非空白/卡死 Worker 画布 |
| 10 | （回归 P0）H5：DevTools 禁用 WebGPU 或阻断 wasm 后再进 Runtime | assert | 主线程回退或最终 `BLOCKED` + `onReady`；**不**长期 `waiting onReady…` |
| 11 | 旋转或拉大窗口后 Lite/Runtime | assert | 仍可画；无崩溃 |
| 12 | （Mac）iOS `holm_spike` 霜化/液态 | tap | 两 scene 不同；`backend=gles2-ios`；进出不崩 |
| 13 | Android Runtime（无 .so） | assert | 与 #4a 同口径 BLOCKED，非假通面 |

类型合法值：`tap` `assert` `scroll` `input` `wait` `screenshot` `track_logcat` `navigate`

## 覆盖对照

| 审查缺口 | 本表步骤 |
|---|---|
| Lite 默认入口未被 Runtime 冲掉 | #1–#2 |
| Runtime BLOCKED（无 native/wasm） | #4a / #13 |
| H5 Runtime 通面 + onPerf + backend 诚实 | #4b–#5 |
| maxFps 30/120/0（0=rAF） | #6–#8 |
| executor 热切 / 出页后 Lite 起泵（P0） | #9 |
| Worker/WebGPU 失败不卡死（P0） | #10 |
| resize 稳定 | #11 |
| iOS Track A 目视 | #12 |
