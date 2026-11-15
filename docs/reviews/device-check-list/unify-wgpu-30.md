# 真机可执行档 · unify-wgpu-30

- 关联报告：`docs/reviews/review-unify-wgpu-30.md`
- 包名 / 入口：`com.jlj.kuiklyholmui` · Home →「示例 / 双轨」(`holm_runtime_m0`)
- 前置条件：
  - 本单产物：`holm-runtime/build-wasm.ps1`、`build-android.ps1` 已跑；`h5App/.../holm-runtime/` 与 `androidApp/.../libholm_runtime.so` 时间戳新于改码
  - iOS：有 Mac/Xcode 时先 `./build-ios.sh` 再装真机/模拟器
  - Ohos：有 OHOS native SDK 时先 `./build-ohos.sh` 再装真机
  - H5：按 `docs/specs/kuikly-dev-h5/runbook.md` 起 **8080 + 8083**；**硬刷新** 8080

## 步骤

| # | 操作 | 类型 | 期望 |
|---|---|---|---|
| 1 | H5 冷启 → Home →「示例 / 双轨」→ Runtime + 隧道，等 ≥2s | navigate | `onReady`；backend=`wgpu-m0` 或 `wgpu-m0-gl`；有画面；`fps=`；无持续红字 |
| 2 | （无原生 WebGPU 环境）强制 WebGL2 回退路径再开 Runtime | assert | backend=`wgpu-m0-gl`；仍出画；不静默假成功 |
| 3 | Runtime 切「IQ原语」/「Raymarching - 基础图形」，等 ≥2s | assert | 场景可辨；无 `SHADER_CUSTOM` |
| 4 | Android arm64 装本单 `.so` → 同页 Runtime 隧道 ≥2s | assert | Vulkan 通面；**无**发白发雾回退观感；`onReady` backend 合理 |
| 5 | （有工具链）iOS 重编 xcframework → Runtime 隧道 ≥2s | assert | Metal 出画；`color_space: Auto` 无明显过饱和/偏色相对升前 |
| 6 | （有工具链）Ohos 重编 `libholm_runtime.so` → Runtime 隧道 ≥2s | assert | Vulkan / `VK_OHOS_surface` 出画；失败诚实 `onError(BLOCKED)`，不 GLES 冒充 |
| 7 | 任一端 Runtime →「故意失败」→ 再切回隧道 | tap | `SHADER_CUSTOM` 红字 + 暗屏停泵；切回可出画 |

类型合法值：`tap` `assert` `scroll` `input` `wait` `screenshot` `track_logcat` `navigate`

## 覆盖对照

| 审查缺口 | 本表步骤 |
|---|---|
| H5 WebGPU / WebGL2 回退 | #1 #2 |
| 合法样本通面 | #3 |
| Android 非 sRGB / 白雾 | #4 |
| iOS Metal + color_space | #5 |
| Ohos Vulkan surface | #6 |
| 失败清屏停泵未回归 | #7 |
