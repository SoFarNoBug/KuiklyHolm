# 真机可执行档 · on-shader-compiled

- 关联报告：`docs/reviews/review-on-shader-compiled.md`
- 包名 / 入口：`com.jlj.kuiklyholmui` · Home →「示例 / 双轨」（`holm_runtime_m0`）
- 前置条件：
  - 含 ADR 0012 / `onShaderCompiled` 的构建产物
  - Android：`installDebug` 可起；H5：按 `docs/specs/kuikly-dev-h5/runbook.md`
  - iOS / 鸿蒙：有对应 Debug 包时勾可选步

## 步骤

| # | 操作 | 类型 | 期望 |
|---|---|---|---|
| 1 | 冷启 → Home →「示例 / 双轨」 | navigate | 进入页；状态行有 `compile=` 字段（初始可为 `compile=—`） |
| 2 | Runtime + 隧道，等 ≥2s | assert | 有画面；`onReady`；状态行出现 `compile ok … compile=` 且 **compileMs>0**；**wrap=0ms**（Runtime） |
| 3 | 切样本 IQ / 程序化海洋 | tap | 每次切换后 `compile` 行更新；仍为 `ok`；Runtime `wrap=0` |
| 4 | 切轨 Lite（同隧道） | tap | 新 `onReady`/`backend`；状态行 `compile ok`；**wrapMs 可为 >0**；`compileMs` 合理 |
| 5 | Lite 下再切样本 | tap | 每次更新耗时；`executor` 显示含 lite |
| 6 | 点「故意失败」 | tap | 先红字 `onError`（SHADER_COMPILE 或等价）；状态行 `compile fail`；失败步耗时非空或显式 0 |
| 7 | 切回隧道可恢复 | tap | 红字可清或可再出画；`compile ok` 再出现 |
| 8 | （可选）Android：切 Runtime→后台→回前台或旋转触发 Surface 恢复 | assert | 恢复后再次出现 `compile ok`（系统重编也回调） |
| 9 | （可选）iOS / 鸿蒙：重复 #2–#6 | assert | 同契约；鸿蒙 Lite 错方言可见 `SHADER_DIALECT` 后再 `fail` 零耗时 |

类型合法值：`tap` `assert` `scroll` `input` `wait` `screenshot` `track_logcat` `navigate`

## H5 勾表结果（2026-11-16 · localhost:8080 `holm_runtime_m0`）

| # | 结果 | 实况摘录 |
|---|---|---|
| 1 | 通过 | 进页；初始可有瞬态 `SHADER_WRAP` 空片元噪声，随后出画 |
| 2 | 通过 | Runtime 隧道：`compile ok wrap=0ms compile=2.5ms (runtime)` · `backend=wgpu-m0` |
| 3 | 通过 | IQ：`wrap=0ms compile=8.5ms`；海洋：`wrap=0ms compile=2ms` |
| 4 | 通过 | Lite 海洋：`compile ok wrap=2.4ms compile=138.4ms (lite)` · `webgl-worker` |
| 5 | 通过 | Lite 隧道：`wrap=0.3ms compile=3.6ms (lite)` |
| 6 | 通过 | `onError SHADER_COMPILE` + `compile fail wrap=0ms compile=1.1ms (lite)` |
| 7 | 通过 | 回隧道：`compile ok wrap=0.3ms compile=3ms (lite)`；红字清 |
| 8–9 | 未跑 | 见下节 Android；iOS / 鸿蒙仍开 |

## Android 勾表结果（2026-11-16 · V2545A · `installDebug` → `holm_runtime_m0`）

| # | 结果 | 实况摘录 |
|---|---|---|
| 1–7 | 通过 | 人工确认 ok（装包冷启双轨页；契约目视与 H5 同档） |
| 8 | 未勾 | Surface 恢复重编未单独跑 |
| 9 | 未跑 | iOS / 鸿蒙 |

## 覆盖对照

| 审查缺口 | 本表步骤 |
|---|---|
| Runtime wrapMs=0 + compileMs>0 | #2 #3 |
| Lite 双耗时 | #4 #5 |
| 失败先 onError 再 fail 耗时 | #6 |
| 热换样本回调 | #3 #5 |
| Surface 恢复重编 | #8 |
| iOS/鸿蒙 | #9 |
