# 真机可执行档 · holm-ohos-android-align

- 关联报告：[`docs/reviews/review-holm-ohos-android-align.md`](../review-holm-ohos-android-align.md)
- 包名 / 入口：`com.jlj.kuiklyholm.demo` · Home →「示例1 / 双轨」(`holm_runtime_m0`)
- 前置条件：
  - `holm-runtime/./build-ohos.sh` → `ohosApp/entry/libs/arm64-v8a/libholm_runtime.so`
  - `./ohos_build.sh` → `libshared.so`（须含 `holm_ohos_bridge_*` 导出）
  - DevEco 自动签名后 `./ohosApp/runOhosApp.sh`
  - 本波：Present 经 `holm_ohos_bridge_*` → KN；壳无 `holm_runtime_session_*`；Provider 由 `HolmOhosNative.configure` / umbrella 注册

## 步骤

| # | 操作 | 类型 | 期望 |
|---|---|---|---|
| 1 | 冷启 → Home →「示例1 / 双轨」 | navigate | 进 `holm_runtime_m0` |
| 2 | 默认 Runtime，等 ≥2s | assert | 隧道；`onReady.backend=wgpu-m0*`；非深蓝 BLOCKED |
| 3 | 切 Lite | tap | `backend=gles2-ohos`；隧道/出画；**无** `LITE_PENDING` |
| 4 | 再切回 Runtime | tap | 隧道恢复；`backend=wgpu-m0*`；不崩 |
| 5 | Runtime 下 maxFps 30→60/0 | assert | fps/间隔可感知或合理；不崩 |
| 6 | renderScale 1.0→0.5→1.0 | assert | 可糊再清；不崩 |
| 7 | 旋转 / resize / 二次进页 | assert | 不崩；岛仍填满 |
| 8 |（可选负向）特制未 register 宿主 | assert | 切轨/start → `BLOCKED` 含 HolmProvider；**默认 Demo 跳过**（P2-provider-negative-hard 已关口径） |
| 9 |（负向）缺 `libholm_runtime.so` / create 失败 | assert | `BLOCKED`；无 GLES 冒充 Runtime |
| 10 | ripgrep 壳源 | assert | `holm_xc` / ets 无 Present 直调 `holm_runtime_session_*` |

类型合法值：`tap` `assert` `scroll` `input` `wait` `screenshot` `track_logcat` `navigate`

## 本机结果（2026-11-16）

| # | 结果 | 证据 |
|---|---|---|
| 编译门禁 | **通过** | `./ohos_build.sh`；HAP 内 `libshared.so` 导出全部 `holm_ohos_bridge_*`；`NEEDED libholm_runtime.so` + `RUNPATH [$ORIGIN]`；`assembleHap` SUCCESS |
| 装机 | **通过** | 本机材料 `default_ohosApp_2fKXy…` + `hap-sign-tool sign-app` → `entry-default-signed.hap`；`hdc install -r` SUCCESS（`FMR0223927006189`） |
| 启动 | **通过** | 已可 `aa start`；装机 signed HAP |
| 10 | **通过** | `rg holm_runtime_session_` 仅注释 |
| 1–2 | **通过** | 2026-11-17：进双轨；Runtime 隧道；`backend=wgpu-m0-gl`；fps≈61 |
| 3 | **通过** | 切 Lite：隧道出画；fps≈60；无 `holm_ohos_bridge unresolved` |
| 4–9 | **未跑** | 热切回 Runtime / maxFps / scale / 负向待补 |
| Compute 回读 | **装机待启** | 2026-11-17：`holm_runtime_headless_*` 已入 HAP；signed 已装 `FMR0223927006189`；锁屏 `10106102` 未开到 `kmp_wgpu_compute` — 解锁后 Home→Compute→Run 期望 `ok size=8 sample=2,2,2,2,4,6,8,10` |

## 覆盖对照

| 审查缺口 | 本表步骤 |
|---|---|
| FullscreenPass / bridge Present | #2 #4 |
| Lite↔Runtime 对称 teardown | #3 #4 |
| maxFps / renderScale | #5 #6 |
| 生命周期 | #7 |
| Provider / 缺库负向 | #8 #9 |
| 壳停直调 C | #10 |
