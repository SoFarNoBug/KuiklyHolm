# 真机可执行档 · dualtrack-lite-vs-standard

- 关联报告：`docs/reviews/review-dualtrack-lite-vs-standard.md`
- 包名 / 入口：`com.jlj.kuiklyholmui` · Home
- 前置条件：
  - Android Debug 安装成功；设备 GLES2
  - H5：按 `docs/specs/kuikly-dev-h5/runbook.md` 起服（若本轮验 H5）
  - iOS：Mac + Simulator/真机；按 `docs/specs/kuikly-dev-ios/runbook.md`（Windows 本机可跳过 iOS 步并记 PARTIAL）

## 步骤

| # | 操作 | 类型 | 期望 |
|---|---|---|---|
| 1 | 冷启动 → Home | navigate | 可见「Lite 五席」与「Runtime M0 旁路」两按钮；默认未自动进 Runtime |
| 2 | 点「Lite 五席 (holm_spike)」 | tap | 进入五席页；默认霜化有画面；`onReady` 行含 `backend=gles2` 或 `webgl*`（非 wgpu-blocked） |
| 3 | 返回 Home → 点「Runtime M0 旁路」 | navigate | 进入 `holm_runtime_m0`；状态行 `executor=runtime` |
| 4 | 观察 Runtime 页 2s | assert | `onReady backend=wgpu-blocked`（无 rustc/.so 时）；**不应**出现假 GLES 霜化充数 |
| 5 | （期望）同页 `onError BLOCKED` 文案可见 | assert | 有 `BLOCKED` 字样；若仅有 backend 无 error 行 → 记开 `P1-concurrency` 复现 |
| 6 | 旋转或拉大窗口（Android）后返回 Lite 页 | assert | Lite 仍可画；无崩溃 |
| 7 | （Mac）iOS 打开 `holm_spike`，切霜化/液态 | tap | 两 scene 画面不同；`backend=gles2-ios`；resize/进出页不崩 |
| 8 | （可选）H5 重复 #1–#5 | assert | 与 Android 同口径；Worker/主线程均可 |

类型合法值：`tap` `assert` `scroll` `input` `wait` `screenshot` `track_logcat` `navigate`

## 覆盖对照

| 审查缺口 | 本表步骤 |
|---|---|
| Lite 默认入口未被 Runtime 冲掉 | #1–#2 |
| Runtime BLOCKED 旁路（非静默 GLES） | #3–#4 |
| onError 回调是否到达 UI（P1-concurrency） | #5 |
| 进出页 / resize 稳定 | #6 |
| iOS Track A 目视 | #7 |
| H5 旁路 | #8 |
