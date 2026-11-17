# 真机可执行档 · ohos-lite-adr0011

- 关联报告：`docs/reviews/review-ohos-lite-adr0011.md`
- 包名 / 入口：鸿蒙 `ohosApp` · Home →「示例1 / 双轨」(`holm_dualtrack`)
- 前置条件：
  - `holm-runtime/./build-ohos.sh` 已出 `libholm_runtime.so`（若 renderer-wgpu要测）
  - DevEco 编过 `libholm_xc.so`（含 EGL/GLESv2）
  - 真机 arm64（Vulkan/wgpu 能力依设备；Lite GLES 为本表重点）
  - 已知：同 XComponent 上 Runtime↔Lite 互斥占窗（ADR 0011）

## 步骤

| # | 操作 | 类型 | 期望 |
|---|---|---|---|
| 1 | 冷启 →「示例1 / 双轨」 | navigate | 进 `holm_dualtrack` |
| 2 | 默认 Runtime，等 ≥2s | assert | 隧道；`onReady.backend` 为 `wgpu-m0*`（弱端可 BLOCKED，记表） |
| 3 | 切 renderer-gl，等 ≥2s | tap | 霓虹隧道；`backend=gles2-ohos`；**无** `LITE_PENDING` |
| 4 | 状态行看 fps / 目视 | assert | fps>0 或目视动效；记录 fps |
| 5 | 切回 Runtime | tap | 隧道恢复或诚实 BLOCKED；backend **不**长期粘 `gles2-ohos` |
| 6 | 再切 renderer-gl | tap | 再 `gles2-ohos`；不崩；隧道恢复 |
| 7 | Lite 下 maxFps=30 再 60/0 | assert | fps/间隔可感知或不崩 |
| 8 | 旋转 / 进出页 2～3 次 | navigate | 不崩；renderer-gl / renderer-wgpu 仍可出画或诚实错误 |
| 9 |（负向）空 fragmentShader（若可） | assert | 深蓝 + `FRAGMENT_REQUIRED` |
| 10 |（负向）Lite 下误留 WGSL（若可） | assert | `SHADER_DIALECT` 或随后换 GLSL 出画；不静默假画 |

类型合法值：`tap` `assert` `scroll` `input` `wait` `screenshot` `track_logcat` `navigate`

## 覆盖对照

| 审查缺口 | 本表步骤 |
|---|---|
| Lite 出画 / gles2-ohos | #3 |
| Runtime↔Lite 切轨 / backend 不粘 | #5–#6 |
| maxFps / 生命周期 | #7–#8 |
| 空片元 / WGSL 负向 | #9–#10 |
| Runtime 基线 | #2 |
