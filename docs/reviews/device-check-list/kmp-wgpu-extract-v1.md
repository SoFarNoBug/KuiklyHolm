# 真机可执行档 · kmp-wgpu-extract-v1

- 关联报告：`docs/reviews/review-kmp-wgpu-extract-v1.md` · 复审 `review-kmp-wgpu-extract-v1-r2.md`
- 包名 / 入口：`com.jlj.kuiklyholmui` · Home →「示例 / 双轨」(`holm_runtime_m0`)；H5 同页
- 前置条件：
  - **先关闭 P0**（H5 sticky `lastErrorCode`）再跑本表；否则 H5 #6–#8 可能假失败
  - Android：已用当前树 `./holm-runtime/build-android.sh`（或 ps1）重编 `jniLibs/arm64-v8a/libholm_runtime.so`（JNI=`AndroidWgpuPlatform`）后 `installDebug`
  - H5：既有 `holm_runtime` wasm 预载；硬刷新；`:holmDemo:packLocalJsBundleDebug` 若改了 Demo/Core JS

## 步骤

| # | 操作 | 类型 | 期望 |
|---|---|---|---|
| 1 | Android 冷启 →「示例 / 双轨」 | navigate | 进 `holm_runtime_m0`；可见 Lite/Runtime |
| 2 | 默认 Runtime + 隧道 ≥2s | assert | `backend=wgpu-m0*`；隧道动效；非深蓝 BLOCKED |
| 3 | Runtime 热换 IQ / 电缆巢 | tap | 出画或诚实 `SHADER_*`；不崩；可再切回隧道成功 |
| 4 | Runtime↔Lite 互切 2 次 | tap | 各轨 backend 诚实；隧道可恢复 |
| 5 |（负向）临时移走 arm64 `.so` 重装进 Runtime | assert | `BLOCKED`；无假 GLES 冒充 wgpu |
| 6 | H5 同页 Runtime + 隧道 | assert | 同 #2（浏览器） |
| 7 | H5：故意坏片元 → 再切有效隧道 | tap | 先红字/`SHADER_*`；再切有效样本须**恢复出画**（验 sticky last 已修） |
| 8 | H5：Runtime↔Lite 互切 | tap | 同 #4 |
| 9 | Android+H5：旋转或 resize 岛 | assert | 不崩；画面跟尺寸 |

类型合法值：`tap` `assert` `scroll` `input` `wait` `screenshot` `track_logcat` `navigate`

## 本轮勾选（2026-11-16 · r2 H5 + r3 Android）

| # | 结果 | 备注 |
|---|---|---|
| 1–4 | 过 | SM-A505F · `installDebug`；用户确认验证 ok |
| 5 | 未跑 | 负向缺 so（非本波硬拦） |
| 6 | 过 | `wgpu-m0` / `compile ok` / `fps≈60`；截图 `_holm-h5-runtime-tunnel-ok.png` |
| 7 | 部分→接受 | 出画恢复 ok；红字/`fps` 粘滞降 P2（切轨可清）；截图 `_holm-h5-tunnel-after-bad.png` |
| 8 | 过 | Lite=`webgl-worker`；再回 Runtime=`wgpu-m0` `fps≈59` |
| 9 | 未跑 | resize（非本波硬拦） |

## 覆盖对照

| 审查缺口 | 本表步骤 |
|---|---|
| Android 新 JNI so 通面 | #1–#4 |
| 缺 so BLOCKED | #5 |
| H5 对等 + sticky 修复 | #6–#8 |
| resize | #9 |
