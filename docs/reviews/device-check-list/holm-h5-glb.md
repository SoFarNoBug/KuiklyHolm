# 浏览器可执行档 · holm-h5-glb

- 关联报告：[`docs/reviews/review-holm-h5-glb-r3.md`](../review-holm-h5-glb-r3.md)（有条件通过）
- 包名 / 入口：`h5App` · `http://localhost:8080/?page_name=holm_model`
- 前置条件：
  - `8083` nativevue2 + `8080` webpack-dev-server
  - 静态 `models/*.glb`、`kmp-wgpu-native/*`、`kmp-draco-native/*`
  - 含 r3 全修：NVI Model 桥、Draco U8、灯光/solid 热更、wgpu 每 rAF、Worker parse、bytes B64

## 结果

- 机型 / 浏览器：Cursor IDE Browser（Chromium）· localhost:8080
- 日期：2026-11-20（r3 全修复验）
- 结论：**Chrome 主路径 + wgpu 切轨通过**；Safari 未测；错误资产无 Demo chip

## 步骤

| # | 操作 | 类型 | 期望 | 结果 |
|---|---|---|---|---|
| 1 | 冷开 → `holm_model` | navigate | 进入页；资产 chip + gl/wgpu | OK |
| 2 | 默认 `renderer-gl`，Flamingo，等 ≥2s | assert | 可见整模；`backend=webgl-model`；fps>0 | OK |
| 3 | 岛内拖转 | tap | 模型绕转 | OK（r3） |
| 4 | 切 Duck / Flamingo | tap | 换模可见；再 `onReady` | OK（r3 抽样） |
| 5 | 切 `LittlestTokyo` | wait | 可见；无 `intermediate value` / `MODEL_LOAD` Draco；`onReady` | OK（Worker/回退后 `webgl-model`） |
| 6 | 灯光 sheet：Unlit↔Lit；拖转后再调灯 | tap | 明暗变；**视角不重置** | OK（r3；wgpu Unlit 抽样） |
| 7 | 切 `renderer-wgpu` | tap | `wgpu-m0*` 或诚实 BLOCKED；fps 接近 gl（非半帧） | OK（`wgpu-m0`，fps≈59–60） |
| 8 | wgpu 灯光/拖转 | tap | 可接受 | OK（灯光 sheet Unlit 抽样） |
| 9 | wgpu↔gl ≥2 次 | tap | 每次可出画 | OK（gl↔wgpu 往返；`webgl-model` / `wgpu-m0`） |
| 10 | Safari gl + wgpu BLOCKED | assert | 诚实 | 未测 |
| 11 | 错误资产 | assert | `MODEL_LOAD` | 未测 UI（Demo 无 chip；`models/__missing__` → HTTP 404） |
| 12 | 进出页 2 次 | navigate | 稳定 | OK（leave tunnel → reenter ×2；再 `onReady`） |

类型合法值：`tap` `assert` `scroll` `input` `wait` `screenshot` `track_logcat` `navigate`

## 覆盖对照

| 审查缺口 | 本表步骤 |
|---|---|
| WebGL 真画 + orbit | #2–#3 |
| Tokyo / Draco | #5 |
| 灯光 + 视角保留 | #6 |
| 切 executor / 半帧 | #7 #9 |
| Safari / BLOCKED | #10 |
| 进出页 | #12 |
