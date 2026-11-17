# 真机可执行档 · business-custom-shader

- 关联报告：`docs/reviews/review-business-custom-shader.md`
- 包名 / 入口：`com.jlj.kuiklyholmui` · Home →「自定义/双轨对比」(`holm_dualtrack`)
- 前置条件：
  - H5：按 `docs/specs/kuikly-dev-h5/runbook.md` 起服；`holm-runtime` wasm 已更新（含 `setCustomFs`）
  - 本轮 P0 整改后的包（`scene` 成功须能 start/ready）
  - Android：可选验 Lite custom；Runtime 本单不承诺

## 步骤

| # | 操作 | 类型 | 期望 |
|---|---|---|---|
| 1 | 冷启 → Home → 点「自定义/双轨对比」 | navigate | 进入 `holm_dualtrack`；可见 Lite/renderer-wgpu与 custom 档 |
| 2 | 默认 Runtime + 霜化 | assert | `onReady`；`backend=wgpu-m0` 或 `wgpu-m0-gl`；有画面与 `fps=` |
| 3 | 点 custom | tap | 霓虹隧道类动效（非仍霜化）；无持续红字 |
| 4 | 开始采集 → 等 ≥3s → 停止 | assert | 页内 JSONL 含 `executor/backend/custom/fps/frameMs/width/height`；`custom=true` |
| 5 | 切轨 Lite（保持 custom） | tap | **有动画**；`onReady`/`backend=webgl*`；非长期 `waiting onReady`；非冻帧 |
| 6 | 开始采集 ≥3s | assert | JSONL 中 `executor=renderer-gl` 且 `custom=true`；fps 可抄 |
| 7 | 再切回 Runtime + custom | tap | 隧道近似再起；无卡死；可采一组 Runtime 样本 |
| 8 | 切回霜化/柔光/液态 | tap | 内置席恢复；`fragmentShader` 清空后观感回到五席逻辑 |
| 9 | （失败 UX）临时改坏 Demo 常量或断网级坏串后点 custom | assert | `onError`；岛区深蓝底 + 红字；**不**静默当霜化成功 |
| 10 | Android（可选）Lite + custom | assert | 隧道可画；Runtime 仍可为 BLOCKED（下一单） |

类型合法值：`tap` `assert` `scroll` `input` `wait` `screenshot` `track_logcat` `navigate`

## 覆盖对照

| 审查缺口 | 本表步骤 |
|---|---|
| custom renderer-gl / renderer-wgpu 通面 | #3 #5 #7 |
| 切轨后 ready/泵帧（P0） | #5 |
| JSONL 采集字段 | #4 #6 |
| 失败深蓝+红字 | #9 |
| 回官方席 | #8 |
| Android Lite 可选 | #10 |
