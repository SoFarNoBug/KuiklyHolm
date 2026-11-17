# 真机可执行档 · kmp-wgpu-independent-repo

- 关联报告：`docs/reviews/review-kmp-wgpu-independent-repo.md` · 复审 `review-kmp-wgpu-independent-repo-r2.md` · `review-kmp-wgpu-independent-repo-r3.md`
- 包名 / 入口：`com.jlj.kuiklyholmui` · Home →「示例 / 双渲染后端」(`holm_dualtrack`)；Compute → `kmp_wgpu_compute`
- 前置条件：
  - P0（wasm `setFragmentShader`）**r2 已关**；H5 仍须硬刷新加载新 glue
  - 独立仓 `/Users/aimo/Studio/kmp/kmp-wgpu`；Android so 含 `nativeSetFragmentShader`（`nm -D` 可验）
  - 联调：`local.properties` `kmp.wgpu.local=true`；Maven 验收：去掉该行并 `publish-maven.sh local` 或 Central `0.1.0`

## 步骤

| # | 操作 | 类型 | 期望 |
|---|---|---|---|
| 1 | Android 冷启 →「示例 / 双渲染后端」 | navigate | 进 `holm_dualtrack` |
| 2 | includeBuild 联调：Runtime + 隧道 ≥2s | assert | `backend=wgpu-m0*`；隧道动效；非只清屏 |
| 3 | Runtime 热换 IQ / 电缆巢再回隧道 | tap | 出画或诚实 `SHADER_*`；可恢复隧道 |
| 4 | 空/清片元再装隧道 | tap | 空时只清屏（无琥珀 ClearTri）；再装隧道成功 |
| 5 | Compute 页回读 | assert | ×2 样本通过 |
| 6 | 关 `kmp.wgpu.local`，mavenLocal/Central `0.1.0` 重编装包 → 同 #2 | assert | 仍出画 |
| 7 | H5：硬刷新 → Runtime + 隧道 | assert | glue 有 `setFragmentShader`；隧道出画（非仅清屏） |
| 8 | H5：坏片元 → 再切有效隧道 | tap | 先失败；再成功须恢复出画 |
| 9 | iOS：Runtime 隧道 | assert | xcframework 链上；出画 |
| 10 | iOS：Compute 回读 | assert | 非 stub FAIL |
| 11 | Ohos：XComponent Runtime 隧道 | assert | so 可解析；出画 |
| 12 | Ohos：旧壳若仍调 setLiquid CName | assert | 空桩无崩溃（API 已删） |

类型合法值：`tap` `assert` `scroll` `input` `wait` `screenshot` `track_logcat` `navigate`

## 本轮勾选（2026-11-17 · 用户确认四端通面）

| # | 结果 | 备注 |
|---|---|---|
| 1–2 | 过 | SM-A505F · `installDebug` + `holm_dualtrack`；用户确认 |
| 3–5 | 过 | 用户确认 Android 通面（含热换/空片元/Compute 口径） |
| 6 | 过 | r3：关 `kmp.wgpu.local`；mavenLocal `0.1.0`；`dependencyInsight` → `io.github.sofarnobug:kmp-wgpu-android:0.1.0`；`installDebug` → 隧道 · `backend=wgpu-m0` · compile ok |
| 7–8 | 过 | 8080/8083；glue `setFragmentShader`；用户确认 H5 |
| 9–10 | 过 | iPhone Duo 模拟器；用户确认 |
| 11–12 | 过 | `FMR0223927006189` · 新 ABI so + HAP；用户确认 |

## 覆盖对照

| 审查缺口 | 本表步骤 |
|---|---|
| Android 新 ABI so + FullscreenPass | #1–#5 |
| Maven / includeBuild 切换 | #2 vs #6（#6 已过 · r3） |
| H5 wasm `setFragmentShader`（P0 修复后） | #7–#8 |
| iOS / Ohos 通面 | #9–#12 |
