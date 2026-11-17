# 真机/模拟器可执行档 · demo-acd-samples

- 关联报告：`docs/reviews/review-demo-acd-samples.md`
- 包名 / 入口：任意端 Demo · Home →「示例 / 双轨」(`holm_dualtrack`)
- 前置条件：
  - 已装含本单 Demo 的包（Android / H5 / iOS / 鸿蒙任一即可；优先 Android）
  - renderer-wgpu需对应 native/wasm 产物；renderer-gl GLES

## 步骤

| # | 操作 | 类型 | 期望 |
|---|---|---|---|
| 1 | 进 `holm_dualtrack`，默认 Runtime + 隧道 | navigate | 隧道出画；`backend=wgpu*` 或诚实 BLOCKED |
| 2 | 依次点 血浆 / 水波 / 火焰 | tap | 各样本出画且观感可辨（非黑屏/非永久卡死） |
| 3 | 回到隧道，切 renderer-gl | tap | Lite 隧道；`backend=gles2*` |
| 4 | Lite 下再切 血浆→水波→火焰 | tap | 均出画；无崩 |
| 5 | 切回 Runtime，再点一非隧道样本 | tap | 出画；backend 不长期粘 gles |
| 6 | 切样本后看状态行 backend | assert | 不宜长期停在 `waiting onReady…`（若停住记缺陷） |
| 7 | maxFps=30 下一火焰样本 | assert | 可感知降帧或不崩 |

类型合法值：`tap` `assert` `scroll` `input` `wait` `screenshot` `track_logcat` `navigate`

## 覆盖对照

| 审查缺口 | 本表步骤 |
|---|---|
| 四样本出画 | #2 #4 |
| renderer-gl / renderer-wgpu 双轨 | #1 #3 #5 |
| 观感可辨 / 弱端 | #2 #7 |
| 切样本 backend 展示 | #6 |
