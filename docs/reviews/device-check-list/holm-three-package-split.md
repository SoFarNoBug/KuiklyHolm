# 真机可执行档 · holm-three-package-split

- 关联报告：`docs/reviews/review-holm-three-package-split.md` · 复审 `review-holm-three-package-split-r2.md`
- 包名 / 入口：`com.jlj.kuiklyholmui` · Home →「示例 / 双轨」(`holm_dualtrack`)；可选「kmp-wgpu Compute 回读」回归
- 前置条件：
  - Android：当前树入仓 `jniLibs/arm64-v8a/libholm_runtime.so`（须含 `HolmAndroidJni_nativeSet*`）后 `installDebug`
  - H5：`:holmDemo:packLocalJsBundleDebug` + 8083 serve + `:h5App:jsBrowserDevelopmentRun`；硬刷新

## 步骤

| # | 操作 | 类型 | 期望 |
|---|---|---|---|
| 1 | Android 冷启 →「示例 / 双轨」 | navigate | 进页；可见 renderer-gl / renderer-wgpu |
| 2 | Runtime + 隧道 ≥2s | assert | `backend=wgpu-m0*`；隧道动效；非深蓝 BLOCKED |
| 3 | Runtime↔Lite 互切 2 次 | tap | 各轨 backend 诚实；隧道可恢复；不崩 |
| 4 | Runtime 热换 IQ / 电缆巢 → 回隧道 | tap | 出画或诚实 `SHADER_*`；回隧道成功 |
| 5 | H5 同页 Runtime + 隧道 | assert | 同 #2；`fps` 有读数 |
| 6 | H5 Runtime↔Lite 互切 | tap | 同 #3 |
| 7 |（可选）Compute 页冒烟 | navigate | `ok size=8 sample=2…10`（回归不回归） |

类型合法值：`tap` `assert` `scroll` `input` `wait` `screenshot` `track_logcat` `navigate`

## 本轮勾选（2026-11-16 · r2 H5 + Android SM-A505F）

| # | 结果 | 备注 |
|---|---|---|
| 1 | 过 | `installDebug` → `pageName=holm_dualtrack`；截图 `_holm-android-runtime-tunnel.png` |
| 2 | 过 | `executor=renderer-wgpu` `backend=wgpu-m0` `compile ok` `fps≈51` 隧道出画 |
| 3 | 过 | Lite=`gles2` `compile ok` `fps≈19` → 回 Runtime=`wgpu-m0`；截图 `_holm-android-lite.png` / `_holm-android-runtime-after-lite.png` |
| 4 | 过 | IQ `sample=iq_primitives` `backend=wgpu-m0`（重 compile）→ 回隧道 `fps≈56`；截图 `_holm-android-iq.png` / `_holm-android-tunnel-again.png` |
| 5 | 过 | H5 `backend=wgpu-m0` `fps=60` |
| 6 | 过 | H5 Lite=`webgl-worker` → Runtime=`wgpu-m0` |
| 7 | 过 | `ok size=8 sample=2.0,…,10.0`；截图 `_holm-android-compute.png` |

机型：Samsung SM-A505F · Android 10 · arm64；so 含 `HolmAndroidJni_nativeSet*`。

## 覆盖对照

| 审查缺口 | 本表步骤 |
|---|---|
| FullscreenPass + HolmAndroidJni Present | #1–#4 |
| H5 Present / 切轨 | #5–#6 |
| Compute 不回归 | #7 |
