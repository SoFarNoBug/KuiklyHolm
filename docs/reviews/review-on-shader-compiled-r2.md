# Review · on-shader-compiled · r2

- 结论：有条件通过
- 依据：上轮 `review-on-shader-compiled.md`；口令「修P2，复审」
- 日期：2026-11-16
- diff：H5 Runtime Worker 失败 `compileMs`；Ohos Lite `!ready` 回调；四端 Runtime 空片元 `ok=false`；changelog / ADR 0012 补丁

## 对照上轮

| 桶 ID | 状态 |
|---|---|
| P1-device-verify | 部分关 — H5 #1–#7、Android #1–#7 已勾；Android #8 / iOS / 鸿蒙仍开 |
| P2-h5-runtime-fail-compileMs-zero | 已关 — Worker init/scene `catch` 用 `compileStarted` 保留墙钟 |
| P2-ohos-lite-not-ready-silent | 已关 — `!liteGl.ready` 发 `FireShaderCompiled(ok=false)` |
| P2-runtime-empty-ok-true | 已关 — Android/H5/iOS/Ohos Runtime 空串均 `ok=false` |

## P0

无。

## P1

### P1-device-verify 四端装载回调与 Demo 耗时展示
- 证据：H5 #1–#7 已跑通；Android V2545A `installDebug` 后人工确认 #1–#7 ok（见可执行档）。Android #8（Surface 恢复）与 iOS / 鸿蒙未勾
- 影响：放行条件（跨端）
- 处置：必修 → 可执行档 [`device-check-list/on-shader-compiled.md`](device-check-list/on-shader-compiled.md)（H5 / Android 主路径已填；可选步仍开）
- 代码审不了：同基线报告

## P2

无。

## 规范候选观察

无。

## 真机

`P1-device-verify`：H5 + Android 主路径已勾；Android #8 / iOS / 鸿蒙仍开。可执行档：[`device-check-list/on-shader-compiled.md`](device-check-list/on-shader-compiled.md)。

## changelog

请确认已 append「审查整改 · onShaderCompiled P2」（审核员不代写）。

## 待补桶

无。
