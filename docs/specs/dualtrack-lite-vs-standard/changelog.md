# dualtrack-lite-vs-standard · changelog

## 2026-11-17（iOS ADD · kmp-wgpu Compute 回读）

- [ADD] iOS：`holm_runtime_headless_*` + `ComputeReadback.ios`；Demo 页不再 stub FAIL
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：速速改｜待批 0

## 2026-11-17（Ohos ADD · kmp-wgpu Compute 回读）

- [ADD] Ohos：`holm_runtime_headless_*` C ABI + `ComputeReadback.ohos`；Demo 页不再 stub FAIL
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：速速改｜待批 0

## 2026-11-17（Ohos FIX · bridge dlopen libshared）

- [FIX] Lite/Runtime 同挂 `holm_ohos_bridge unresolved`：壳改显式 dlopen `libshared.so` + 可重试 resolve
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：速速改｜待批 0

## 2026-11-16（Ohos 对齐审查整改 · 可修 P2 口径）

- [NOTE] setters / Provider 负向 / Lite+bridge / worktree：与 `kuikly-dev-ohos` 同日条；无行为回归改动
- [NOTE] 真机 DualTrack 仍挂 `P1-device-verify`（签名装机）
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：清审查可修口径｜待批 0

## 2026-11-16（Ohos 三包对齐 · Present 经 KN Bridge）

- [ADD] Ohos：`kmp-wgpu*` / `holm-renderer-*` ohosArm64；`HolmRuntimeOhosBridge` + `holm_ohos_bridge_*`；壳停直调 C
- [MODIFY] ADR 0008/0010/0013 + D13：四端 Present 对齐；Maven/拆仓仍另单
- 自测：`ohos_build.sh` + `assembleHap`；真机 DualTrack 待签名装机勾表
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：按确认 Plan｜待批 0

## 2026-11-16（Duo Sim Lite 冷启 + Runtime 出画）

- [FIX] iOS Lite 冷启：空片元 defer + 晚到 shader 补 `onReady`（修 FRAGMENT_REQUIRED 粘住）
- [NOTE] Duo：`_holm-ios-duo-lite.png` / `_holm-ios-duo-runtime.png`；热切未自动化；真机表 #5–#8 仍待
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：速速改 DualTrack 出画｜待批 0

## 2026-11-16（iOS 对齐审查整改 · Holm set* 分层）

- [FIX] P2：`HolmIosNative` 独立 cinterop set*；kmp-wgpu ios 仅 Present 粗块
- [NOTE] ohos 签名材料已从工作区还原；Sim Duo DualTrack 待勾真机表
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：速速改审查项｜待批 0

## 2026-11-16（iOS 架构对齐 Android · 三包 + kmp-wgpu Present）

- [ADD] `:kmp-wgpu` / `:kmp-wgpu-surface` ios 目标：cinterop `holm_runtime.h`；`IosWgpuPlatform` / `IosSurfaces`
- [ADD] `:holm-renderer-wgpu` ios：`HolmFullscreenPass` / `HolmIosNative` / `HolmRuntimeNative`；`:holm-renderer-gl` ios 目标
- [ADD] `KuiklyHolmCore` ios：`api` 两 renderer；`HolmUmbrellaBootstrap`；`HolmRuntimeIosBridge`（ObjC：`HolmDemoHolmRuntimeIosBridge`）
- [MODIFY] `KRHolmSurface.m`：Present 停直调 `holm_runtime_session_*`；经 bridge；切轨/start `requireProvider`；`HolmIosBridge.configure` 显式 register
- [MODIFY] ADR 0008/0009 / CONTEXT / 00 D13·D14 / AGENTS / file-map / ios runbook
- [NOTE] 泵仍 ObjC CADisplayLink（ADR 0009）；App 仍链 xcframework；iOS Compute 仍 stub；Ohos 另单
- 自测：`:kmp-wgpu`…`:holmDemo:linkPodDebugFrameworkIosSimulatorArm64` 通过；`iosApp` 无 `holm_runtime_session_*`
- 真机验证表：本单编译门禁已过；Sim/真机 DualTrack DoD 待审核员表 `docs/reviews/device-check-list/holm-ios-android-align.md`
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：按确认 Plan 对齐 iOS｜待批 0

## 2026-11-16（三包拆 · 清审查 P1/P2 可修项）

- [FIX] P1-doc-drift：`docs/product/README` / ADR 0008 / D11 对齐 `holm-renderer-*`；写明 umbrella 自动注册为例外
- [FIX] P1-plan-soft-drift（Demo 半）：`HolmDemoBootstrap` + `HomePager` / `KRApplication` / H5 `Main` 显式 `HolmProviders.register`
- [FIX] P2-provider-ready-backend：Android Provider 失败设 `_providerBlocked`，`onReady.backend=wgpu-blocked`（对齐 H5）
- [NOTE] 未做：Provider 真正分发 View 半部；H5 Worker 双路径收敛；worktree noise
- 自测：`:KuiklyHolmCore` / `:holmDemo` / `:androidApp` Android + `:KuiklyHolmCore`/`:holmDemo` JS compile 通过
- 真机验证表：`docs/reviews/device-check-list/holm-three-package-split.md`（仍挂 P1-device-verify）
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：审查打回项整改｜待批 0

## 2026-11-16（Holm 三包物理拆 · Android+H5 + kmp-wgpu 抽芯收尾）

- [ADD] Gradle：`:holm-core` / `:holm-renderer-gl` / `:holm-renderer-wgpu`；`:KuiklyHolmCore` 改 umbrella（`api` 三包；iOS/Ohos 源暂留）
- [ADD] `HolmBackendProvider` + `HolmProviders`（必设 / 缺则 BLOCKED）；`GlBackendProvider` / `WgpuBackendProvider`；umbrella `HolmUmbrellaBootstrap` 自动注册
- [MODIFY] ADR 0008 / CONTEXT / 00 / glossary / AGENTS / file-map：包名 `holm-renderer-gl` / `holm-renderer-wgpu`（取代草案 `holm-gl` / `holm-wgpu`）
- [MODIFY] Android Present 唯一入口：`KRHolmSurfaceView` → `HolmFullscreenPass`；Holm 语义 JNI → `HolmAndroidJni`（迁出 `AndroidWgpuPlatform`）
- [MODIFY] H5 Runtime 主路径经 `createFullscreenPass`；Worker 仍经 `HolmRuntimeNative.sessionSet*`（符号名未改）
- [NOTE] 本波不做 crate/so/wasm/`HolmRuntimeSession` 改名；不做 iOS/Ohos 物理拆；不做 Maven 独立坐标
- 自测：`:holm-core` / `:holm-renderer-gl` / `:holm-renderer-wgpu` / `:KuiklyHolmCore` / `:holmDemo` / `:kmp-wgpu` Android+JS compile 通过；so 含 `HolmAndroidJni_nativeSet*`
- 真机验证表：本单编译门禁已过；真机 DualTrack DoD 待审核员表 `docs/reviews/device-check-list/holm-three-package-split.md`
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：按确认 Plan 物理拆｜待批 0

## 2026-11-16（kmp-wgpu v1.1 · Headless Compute + 回读）

- [ADD] `holm-runtime`：`compute_readback` / `HeadlessCompute`；Android JNI `nativeCreateHeadless`/`nativeComputeReadback`；wasm `HeadlessComputeSession`
- [ADD] `:kmp-wgpu`：`createHeadlessDevice*` / `computeReadback*` / `ComputeReadbackSample`（方案 B 粗块；与 Present 分表）
- [ADD] Demo：`kmp_wgpu_compute` 页（无 HolmSurface）；Home 入口；H5 `__holmRuntime.createHeadless`
- [NOTE] 范围 Android+H5；iOS/Ohos stub「TBD」；未拆 Holm 三包
- 自测：`:kmp-wgpu` / `:holmDemo` Android+JS compile；so 含 `nativeCreateHeadless`；wasm glue 含 `HeadlessComputeSession`
- 真机：H5 Compute `ok size=8 sample=2…10` + Present `fps=60 wgpu-m0`；vivo V2545A Compute 同结果 + Present `fps=60 compile ok backend=wgpu-m0`
- 真机验证表：见审查 `docs/reviews/review-kmp-wgpu-compute-v1.1.md` / 可执行档待审核员补 `device-check-list/kmp-wgpu-compute-v1.1.md`
- 审查跟进：README 标短同步 / WGSL 双份同步义务
- [FIX] review P1-main-thread-io：Android `*Async` 真下沉到 `kmp-wgpu-headless` 线程；Demo 回主线程再改 UI
- [FIX] review P2-destroy-not-idempotent：`HeadlessDevice.destroy` 幂等
- [FIX] review P2-wgsl-dual-copy：Rust/Kotlin 双侧交叉引用注释
- [NOTE] P2-worktree-noise：本单提交勿带 `iosApp`/`ohosApp`/`static_server` 无关改动
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：审查打回项整改｜待批 0

## 2026-11-16（H5 · 清 P2-h5-error-sticky-ui）

- [FIX] H5 Runtime：成功 `setCustomFs` / 成功 `tick` 只 discard drain，不再把上一档 pending/`uncaptured` 打到 `onError`；成功装载后按 Attr 恢复泵
- [FIX] H5 Worker `scene` 成功路径：若 `playing` 仍为 true 则 `start()` 自愈
- [FIX] H5/Android `ON_ERROR` 重绑：只 replay `BLOCKED`，禁止把旧 `SHADER_*` 在 Compose 重绑时打回
- [FIX] Demo：`shaderSeq`/`compiledOkSeq` 忽略迟到 SHADER_*；`onShaderCompiled(ok)` 清红字并恢复泵；`onPerf` 本代 ok 出帧清 sticky
- 自测：`:holmDemo:packLocalJsBundleDebug` + H5「故意失败→隧道」→ `compile ok` / `fps≈60` / 无红字
- 真机验证表：本单无新建表（复用 `docs/reviews/device-check-list/kmp-wgpu-extract-v1.md` #7）
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：速速改清审查 P2｜待批 0

## 2026-11-16（kmp-wgpu v1 · 修审查 P0/P1）

- [FIX] H5 `Device.sessionTick` / 装载路径：成败只信 wasm throw/`Result`，不再用 sticky `lastErrorCode` 否决成功帧（review P0-silent-fail）
- [MODIFY] `sessionSetCustomFs`/`setScene`/`liquid`/`playing` 迁出 `kmp-wgpu` DeviceSession → Holm 适配（review P1-plan-soft-drift）
- [FIX] Android `abiVersion()` 调 `nativeAbiVersion`
- 自测：`:kmp-wgpu` / `:KuiklyHolmCore` Android+JS compile；本单无真机验证
- 真机验证表：见 `docs/reviews/device-check-list/kmp-wgpu-extract-v1.md`（修后待勾）
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：审查打回项整改｜待批 0

## 2026-11-16（kmp-wgpu 抽芯 v1 · 方案 B）

- [ADD] `:kmp-wgpu` / `:kmp-wgpu-surface`（Kotlin 近 wgpu 类型壳；粗粒度 Session 能力块；无逐 draw FFI）
- [MODIFY] `holm-runtime`：`session_core` → `holm_fullscreen`；增 `primitives`；Cargo 叙事 `kmp_wgpu_native`；产物名仍 `holm_runtime`
- [MODIFY] Android JNI 类 → `com.jlj.kmp.wgpu.AndroidWgpuPlatform`；增 `build-android.sh`（macOS）
- [MODIFY] `HolmRuntimeNative`（Android/H5）门面改走 surface + Device；增 `HolmFullscreenPass`
- [MODIFY] 已重编 `androidApp/.../libholm_runtime.so`（arm64 · ~4.0 MiB · 含新 JNI）
- [NOTE] iOS / Ohos C ABI 未迁；Compute+回读 → v1.1；未物理拆 holm-core/gl/wgpu；H5 wasm 符号未改（仍 `HolmRuntimeSession`）
- 自测：`:kmp-wgpu` / `:kmp-wgpu-surface` / `:KuiklyHolmCore` Android+JS compile 通过；`cargo ndk` 重编 so 成功；本单无真机验证
- 真机验证表：本单无真机验证（装包验收另单）
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：按确认 Plan ADR 0013 抽芯｜待批 0
- carried-over: DeviceSession 仍暴露 sessionSetCustomFs 等过渡粗块｜不修理由：v1 Holm 适配必需；README 已标明非长期公共面

## 2026-11-16（renderScale 可配置缓冲）

- [ADD] `HolmSurfaceAttr.renderScale` / Compose 参数：相对**平台基线**，默认 `1`；夹紧 `(0,1]`；与 `maxFps` 正交
- [MODIFY] 四端 Lite+Runtime：Ohos `SET_BUFFER_GEOMETRY`；Android `setFixedSize`；iOS Lite `contentScaleFactor` / Runtime `drawableSize×scale`；H5 canvas = CSS×scale
- [MODIFY] Demo `holm_runtime_m0`：`scale` 档 `1.0 / 0.5 / 0.25`
- [MODIFY] ADR 0010/0011 Consequences：降采样改为可配置，不强制 1×
- 自测：本单无真机验证（码农编译面；验收见 Plan）
- 真机验证表：本单无真机验证
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：按确认 Plan 落地 Attr｜待批 0

## 2026-11-16（Demo · Lite 电缆巢 WebGL1 + 红字）

- [MODIFY] `CableNestShaders.LITE_BODY`：`rayMarch` 改为 `for (int i = 0; …)`（修 H5 WebGL1 `Invalid init declaration`）
- [MODIFY] `HolmRuntimeM0Pager`：正样本 `SHADER_COMPILE`/`SHADER_CUSTOM` 也写红字；`onShaderCompiled(ok)` 清残留
- [NOTE] H5 须 `:holmDemo:packLocalJsBundleDebug` 后硬刷新；只改源不 pack 则 8083 仍旧 `nativevue2.js`
- 自测：已 pack；Lite「电缆巢」`compile ok` 出画；「故意失败」红字 `SHADER_COMPILE: undefined_holm_demo_error`
- 真机验证表：本单无真机验证
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：口令修显示+GLES 循环｜待批 0

## 2026-11-16（holm-runtime release · opt-level=s）

- [MODIFY] `Cargo.toml` `[profile.release]`：追加 `opt-level = "s"`（在 lto/strip/cg1 真机通过后）
- 自测：`.so` **4682072 → 4157912 B**（≈4.47→3.97 MiB；相对原始 8332752 ≈ −50%）
- 真机验证：**通过**（V2545A）→「电缆巢」Runtime≈70fps → Lite → Runtime；无 BLOCKED / SHADER_*
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：包体继续压体积｜待批 0

## 2026-11-16（holm-runtime release 包体瘦身）

- [MODIFY] `holm-runtime/Cargo.toml`：`[profile.release] lto=true` + `codegen-units=1` + `strip=true`（暂不加 `opt-level="s"`）
- 自测：`cargo ndk -t arm64-v8a … --release` → `libholm_runtime.so` **8332752 → 4682072 B**（≈7.95→4.47 MiB，约 −44%）
- 真机验证：**通过**（V2545A · installDebug）→「电缆巢」Runtime `wgpu-m0` ≈60fps → Lite `gles2` → Runtime；无 BLOCKED / SHADER_*；出画正常
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：包体符号/LTO 个案｜待批 0

## 2026-11-16（Demo · 电缆巢 CC0）

- [ADD] `DemoSample.CABLE_NEST`（「电缆巢」）+ `CableNestShaders` Lite/Runtime；作者 mrange · shadertoy.com · CC0
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：口令加 CC0 样本｜待批 0
- 真机验证表：本单无真机验证表（码农自测编译 + 可选装包）
- 自测：`holm_runtime_m0` →「电缆巢」Lite/Runtime 出画；可见出处小字
- carried-over: cam path warp「忘了哪份 shader」｜不修理由：作者整份 CC0 且未点名 NC 源

## 2026-11-16（onShaderCompiled 双耗时 · 四端）

- [ADD] 双轨装载结束事件 `onShaderCompiled`（ADR 0012）；Lite wrap/compile 分计；Runtime `wrapMs=0`
- [MODIFY] Demo 状态行展示最近一次 `wrapMs`/`compileMs`
- 本单无真机验证（编译门禁）
- 轻量治理：规范自检见会话实现回报；gate 跳过（纯业务）

## 2026-11-16（Android · Runtime→Lite GLES 句柄失效蓝屏）

- [MODIFY] `KRHolmSurfaceView` / `HolmGlesRenderer`：`markContextLost()`；切 Runtime pause 前与切回 Lite 前作废 `_program`/`_surfaceReady`（关 same-check 误跳过 → `_program==0` 深蓝清屏；换样本才恢复）
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：EGL pause 后句柄过期｜待批 0
- 真机验证表：本单无真机验证表（码农自测 Runtime 海洋→Lite 截屏）
- 自测：程序化海洋 Runtime→Lite 立即出浪；Lite→Runtime→Lite 不粘蓝屏

## 2026-11-15（Android · Runtime→Lite 切轨蓝屏）

- [MODIFY] `KRHolmSurfaceView`（Android）：片元只喂当前轨方言（错方言只存）；切 Lite 时 `queueEvent setShaders` 强制重编（关 Runtime 海洋→Lite 岛蓝屏）
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：双轨热切方言竞态｜待批 0
- 真机验证表：`docs/reviews/device-check-list/demo-procedural-ocean.md`（续跑 #4）
- 自测：Runtime 程序化海洋 → Lite → 岛立即出画；互切 2 次

## 2026-11-15（程序化海洋 · Lite 对齐 Runtime 迭代）

- [MODIFY] `ProceduralOceanShaders` Lite：`ITERATIONS_RAYMARCH/NORMAL/STEPS` → 12/24/64（与 Runtime 同档；关审查 `P2-lite-runtime-wave-iters`）
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：审查回执行对齐双轨｜待批 0
- 真机验证表：`docs/reviews/device-check-list/demo-procedural-ocean.md`
- 自测：installDebug →「程序化海洋」Lite/Runtime 出画对比

## 2026-11-15（Demo · 程序化海洋 MIT）

- [ADD] `DemoSample.PROCEDURAL_OCEAN`（「程序化海洋」）+ `ProceduralOceanShaders` Lite/Runtime；作者 afl_ext · shadertoy.com/view/MdXyzX · MIT
- [MODIFY] `HolmRuntimeM0Pager`：`uiCredit()` 通用出处行（IQ / 海洋）
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：口令加 MIT 样本｜待批 0
- 本单无真机验证
- 自测：`holm_runtime_m0` →「程序化海洋」Lite/Runtime 出画；可见出处小字；故意失败仍可用

## 2026-11-15（Android Runtime · DisplayHandle）

- [MODIFY] `android_jni`：wgpu 30 `create_surface` 补 `AndroidDisplayHandle`——`InstanceDescriptor::new_with_display_handle` + `SurfaceTargetUnsafe::from_display_and_window`（关真机 `MissingDisplayHandle`）
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：速速改 Runtime 创建卡点｜待批 0
- 真机验证表：`docs/reviews/device-check-list/unify-wgpu-30.md`（本单续跑 Runtime 轨）
- 自测：`build-android.ps1` → installDebug → `holm_runtime_m0` 非 BLOCKED

## 2026-11-15（审查回执行 · unify-wgpu-30 P2）

- [MODIFY] `session_core::build_pipe`：`device.poll(Wait)` 失败 `map_err` 返回 `WGPU_INTERNAL`（关 `P2-poll-error-swallowed`）
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：审查回执行 P2｜待批 0
- 本单无真机验证（H5 联调见 runbook；可执行档仍 `device-check-list/unify-wgpu-30.md`）
- 审查回执行：`docs/reviews/review-unify-wgpu-30.md`
- 自测：wasm/android release 重编；H5 Runtime 隧道 `wgpu-m0`≈60fps；故意失败 `SHADER_CUSTOM`；切回隧道恢复

## 2026-11-15（全端统一 wgpu 30.0.2）

- [MODIFY] `holm-runtime`：wasm / Android / iOS / Ohos 统一 `wgpu = "30.0.2"` + `naga = "30.0.2"`；`Cargo.lock` 去掉 23.x
- [MODIFY] 合并 `session_core_ohos.rs` → 单一 `session_core.rs`（wgpu 30 API + wasm error-scope 守卫）；修 `present_clear_only` 为 `CurrentSurfaceTexture` + `queue.present`
- [MODIFY] 入口：`InstanceDescriptor::new_without_display_handle`；`on_uncaptured_error(Arc)`；`ErrorScopeGuard::pop` + `PollType::wait_indefinitely`
- [MODIFY] Android features 显式 `vulkan`+`gles`；Ohos 显式 `vulkan`
- 自测：`build-wasm.ps1` OK（`holm_runtime_bg.wasm` ≈ 4.02 MiB）；`cargo ndk` arm64 `.so` OK（≈ 7.89 MiB）；iOS/Ohos 本机无 apple-ios / ohos target → **BLOCKED**（共享 session_core，API 与 Android 同源）
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：依赖统一升档｜待批 0
- 本单无真机验证

## 2026-11-15（样本展示名：Raymarching - 基础图形）

- [MODIFY] `DemoSample.IQ_PRIMITIVES.label`：`IQ原语` → `Raymarching - 基础图形`（id 不变）
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：对齐 Shadertoy 场景名｜待批 0
- 本单无真机验证
- 自测：示例 chip 可见「Raymarching - 基础图形」

## 2026-11-15（IQ原语 MIT 出处标注）

- [ADD] Demo 选中「IQ原语」时一行小字：作者 Inigo Quilez · 出处 shadertoy.com · MIT
- [MODIFY] `IqPrimitivesShaders`：KDoc + LITE/RUNTIME 注释头保留 MIT notice 与 Shadertoy/iquilezles 链接
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：口令加版权标注｜待批 0
- 本单无真机验证
- 自测：Runtime→IQ原语 → 可见出处小字；切走隧道小字消失

## 2026-11-15（失败停泵：勿 ClearTri 假特效）

- [MODIFY] `set_custom_fs` 失败：`present_clear_only` 暗色清屏 + `playing=false`（H5/Ohos session_core）
- [MODIFY] H5 Worker / 主线程 / Android：失败对齐停泵；Demo `surfacePlaying` 仅对 `SHADER_CUSTOM`/`SHADER_COMPILE`/WGPU_/TICK/SURFACE 置 false（勿吃 SHADER_WRAP）
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：浏览器复现 bad_compile 仍 60fps ClearTri｜待批 0
- 本单无真机验证（H5 浏览器自测已过：红字 + 暗屏无黄三角；切回隧道出画）
- 自测：隧道→故意失败 → 红字 + 停画/无黄三角；切回隧道恢复

## 2026-11-15（切换样本失败卸掉上一档特效）

- [MODIFY] Runtime `set_custom_fs` 失败 → ClearTri（卸 `pipe_custom`）；宿主先 `setCustomFs("")` 再装新
- [MODIFY] Lite H5/Android：compile/link 失败删除上一 program，深蓝清屏
- [NOTE] H5 宿主改动在 `h5App`/`KuiklyHolmCore` jsMain，**须重编/重启 8080**；只 pack Demo 不够
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：切换示例/轨应停旧特效｜待批 0
- 本单无真机验证
- 自测：隧道→故意失败 → 红字 + **不再**播隧道；切回隧道出画

## 2026-11-15（Demo 故意失败样本）

- [ADD] `DemoSample.BAD_COMPILE` / `BadCompileShaders`：Runtime `let ref`、Lite 未定义标识符；chip「故意失败」
- [MODIFY] 换样本时清 `errorText`，避免 sticky 挡住下一档
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：口令加负向 Demo chip｜待批 0
- 本单无真机验证
- 自测：Runtime→故意失败 → 红字 `SHADER_CUSTOM` 含 reserved；切回隧道出画

## 2026-11-15（审查回执行 · wgpu-error-to-business P1/P2）

- [MODIFY] H5 主线程：`sessionTick` 透传 message + 每帧/失败 `drainErrors`；Worker catch 仅 drain（空才 fallback fire）
- [MODIFY] Android：`setCustomFs`/`tick` 失败只 drain 一次，避免 lastError+drain 双发
- [MODIFY] Ohos tick（帧泵 + create 首帧）读 `last_error_*`；wasm `setCustomFs` 错误 JsValue 去 `SHADER_CUSTOM:` 前缀
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：审查回执行 P1/P2｜待批 0
- 本单无真机验证（H5 联调见 runbook；可执行档仍 `device-check-list/wgpu-error-to-business.md`）
- 审查回执行：`docs/reviews/review-wgpu-error-to-business.md`

## 2026-11-15（wgpu 错误全量 onError）

- [ADD] `holm-runtime`：`error_bridge` + naga 同步 WGSL 校验；`on_uncaptured_error` / pending drain；`set_custom_fs` 失败不覆盖成功管线
- [ADD] H5 `drainErrors` / Android `lastError*`+`drainErrors` / iOS·Ohos `last_error_*`；宿主透传完整 message
- [MODIFY] Demo：`SHADER_*` / `WGPU_*` / `TICK` / `SURFACE` 不被 onPerf 清空
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：按确认 Plan wgpu onError｜待批 0
- 本单无真机验证
- 自测：坏 WGSL（如 `let ref`）→ Runtime `onError SHADER_CUSTOM` 含 reserved keyword；合法 IQ 出画

## 2026-11-15（修 IQ Runtime 黑屏）

- [MODIFY] `IqPrimitivesShaders.RUNTIME_FS`：WGSL 保留字 `ref` → `refl`（编译失败会留下清屏≈黑屏）
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：速速改 Runtime 编译｜待批 0
- 本单无真机验证

## 2026-11-15（Demo 换 IQ SDF 原语陈列）

- [ADD] `IqPrimitivesShaders`：移植 Inigo Quilez Raymarching primitives（MIT；Lite+WGSL；GLES2 去重载/位运算，桩 iMouse/iFrame，AA=1，去 gamma）
- [MODIFY] `DemoSampleCatalog`：仅 隧道 + IQ原语；删除豆包 Phong/棋盘/大理石/菲涅尔/SDF 与血浆/水波/火焰
- [REMOVE] `DemoEffectShaders.kt`
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：口令换样本｜待批 0
- 本单无真机验证
- 自测：H5 Lite 出 IQ 场景；弱端可能掉帧属预期

## 2026-11-15（Demo 样本改用豆包 Shader 合集）

- [MODIFY] `DemoEffectShaders` / `DemoSampleCatalog`：去掉血浆/水波/火焰；接入 Phong / 棋盘 / 大理石 / 菲涅尔 / SDF（Lite+WGSL；合集 `u_scale`/`u_intensity` 写死常量）
- [MODIFY] `holm_runtime_m0` 示例 chip 按每行 3 个分行（6 样本）
- 来源：豆包线程「Shader代码合集」（噪声 3D 哈希按可编译修正）
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：口令换样本、无库契约变更｜待批 0
- 本单无真机验证
- 自测：H5 六 chip × Lite/Runtime 出画

## 2026-11-15（Demo ACD 特效观感重做）

- [MODIFY] `DemoEffectShaders`：血浆 / 水波 / 火焰 Lite+WGSL 成对重写（高对比域扭曲、多源涟漪+高光、上升湍流+热核/火花）
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：口令改观感、无契约变更｜待批 0
- 本单无真机验证
- 自测：H5 `holm_runtime_m0` 四 chip × Lite/Runtime 出画且主题可辨

## 2026-11-15（审查整改 demo-acd-samples P2）

- [MODIFY] 切样本不再清 `backend`/`waiting onReady`（仅切 executor 清）
- [MODIFY] 示例 chip 两行排布 + compact padding（窄屏不挤出）
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：速速改关 P2｜待批 0
- 本单无真机验证

## 2026-11-15（Demo 多样本 A/C/D chip）

- [ADD] `DemoSampleCatalog` + `DemoEffectShaders`：血浆 / 水波 / 火焰（Lite body + Runtime WGSL 成对）
- [MODIFY] `holm_runtime_m0`：示例 chip 切 隧道/血浆/水波/火焰；切样本清 backend；JSONL 带 `sample`
- [MODIFY] Home 入口文案「示例 / 双轨」
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：按确认 Plan Demo ACD｜待批 0
- 本单无真机验证
- 自测：四 chip × Lite/Runtime 出画；切样本不崩

## 2026-11-15（审查整改 ohos-lite-adr0011）

- [MODIFY] README / eglTerminate / 切轨方言瞬态（见 ohos changelog）
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：速速改关 P1-doc-drift + P2｜待批 0
- 真机验证表：仍待勾 `device-check-list/ohos-lite-adr0011.md`

## 2026-11-15（鸿蒙 Lite ADR 0011）

- [ADD] 同 XComponent EGL/GLES Lite：`gles2-ohos`；去 `LITE_PENDING`
- [ADD] C++ wrap/vert 对齐 `HolmShadertoyShell` / `HolmBuiltinShaders`（改 SoT 须同步 `holm_xc_shadertoy.h`）
- [MODIFY] Runtime↔Lite 互切急拆；共用 OnFrame + maxFps
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：按确认 Plan｜待批 0
- 真机验证表：`docs/reviews/device-check-list/ohos-lite-adr0011.md`（交审核员）
- 自测：Lite 隧道 + 互切；空 FS / WGSL 负向

## 2026-11-14（鸿蒙 Runtime ADR 0010）

- [ADD] `holm_runtime_session_create_ohos` + `build-ohos.sh` → `libholm_runtime.so`（wgpu 30 / ohos-only）
- [ADD] `libholm_xc.so` XComponent OnFrame 外泵 + `KRHolmSurface.ets`；Lite=`LITE_PENDING`
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：按确认 Plan 执行 ADR 0010｜待批 0
- 自测：`./build-ohos.sh` 出 so；`holm_xc_plugin.cpp` 对 SDK 头可编；真机隧道待勾

## 2026-11-14（关 iOS P2-objc-shadertoy-drift）

- [MODIFY] iOS Lite：Shadertoy wrap/vert 单源走 holmDemo Kotlin API
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：速速改关 P2｜待批 0
- 自测：Lite 隧道出画

## 2026-11-14（修 iOS Lite ~1fps）

- [MODIFY] iOS Lite GLES 缓冲 1x（对齐不乘 DPR）；避免隧道堵主线程导致 fps=0
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：速速改｜待批 0
- 自测：Lite fps>0；Runtime 无改

## 2026-11-14（iOS Lite GLES）

- [ADD] iOS `KRHolmGlesHostView` + `KRHolmSurface` Lite 轨：Shadertoy 隧道；`onReady.backend=gles2-ios`
- [MODIFY] 去 `LITE_PENDING`；Runtime↔Lite 互斥子 View；链 OpenGLES
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：按确认 Plan 补 ADR 0009 后单 Lite｜待批 0
- 自测：Duo/真机 Runtime↔Lite 隧道；backend 不粘 wgpu

## 2026-11-14（关 iOS review P2 DisplayLink/resize）

- [MODIFY] iOS `KRHolmSurface`：DisplayLink weak proxy；像素未变跳过 resize
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：速速改关 P2｜待批 0
- 自测：Runtime 通面；旋转不崩

## 2026-11-14（修 iOS Runtime→Lite 崩溃）

- [MODIFY] iOS：切轨先到 GLSL 时跳过 `set_custom_fs`；C ABI `catch_unwind`
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：热修切轨 abort｜待批 0
- 自测：Duo 模拟器 Runtime↔Lite

## 2026-11-14（iOS Runtime · ADR 0009）

- [ADD] `holm-runtime`：`ios_c_api` C Session（`holm_runtime_session_*`）+ iOS `wgpu`/`metal` 依赖 + `include/holm_runtime.h`
- [ADD] `build-ios.sh` → `dist/ios/HolmRuntime.xcframework`（device + sim；dist gitignore）
- [MODIFY] `KRHolmSurface`：容器 + `KRHolmMetalHostView`（CAMetalLayer）；主线程 DisplayLink 外泵；`maxFps`；Lite=`LITE_PENDING`
- [MODIFY] `iosApp.xcodeproj`：链 HolmRuntime.xcframework + Metal/QuartzCore
- [MODIFY] iOS runbook / CONTEXT 状态 / holm-runtime README
- 已知遗留：Lite GLES 片元后单｜不修理由：ADR 0009 顺序
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：按确认 Plan 落地 iOS Runtime｜待批 0
- 真机验证表：交审核员（模拟器/真机隧道 + Lite 负向）
- 自测：`./holm-runtime/build-ios.sh` → `pod install` → Run → `holm_runtime_m0`

## 2026-11-14（审查 android-runtime-dualtrack 整改）

- [MODIFY] `KRHolmSurfaceView`：切 Lite 主动 `fireReady(gles2)`（防 backend 粘 wgpu）；`_runtimeSurfaceValid`→`AtomicBoolean`；急拆超时再投幂等 destroy；卸岛 `quitSafely` + 再挂重建线程；非 WGSL custom → `onError SHADER_CUSTOM`
- [MODIFY] Demo `HolmRuntimeM0Pager`：切 executor 清 `backendId`/`backendText`
- 已知遗留：`HolmRuntimeNative._presentable` 仍进程级｜不修理由：Demo 单岛；多岛另单
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：关审查 P1/P2 可改项｜待批 0
- 真机验证表：`docs/reviews/device-check-list/android-runtime-dualtrack.md`（审核员已出；本单装包供勾）
- 自测：`:androidApp:installDebug` → `holm_runtime_m0` 切轨看 backend；进出页 3 次

## 2026-11-14（Lite LITE_BODY 去 pow 对齐 Runtime）

- [MODIFY] Demo `NeonTunnelShaders.LITE_BODY`：去掉片元 `pow(col, vec3(0.4545))`，与 `RUNTIME_FS` 一致，缓解 Lite custom 发白
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：补齐上一单 Lite 遗留双伽马｜待批 0
- 本单无真机验证
- 自测：`:androidApp:installDebug` → `holm_runtime_m0` Lite custom，对比 Runtime 发白是否缓解

## 2026-11-14（真机画质：Lite highp + Runtime 非 sRGB）

- [MODIFY] `HolmShadertoyShell`：优先 `highp`（修 Lite custom 隧道花屏噪点）
- [MODIFY] `session_core`：Surface format **优先非 sRGB**（修 Android Runtime 全席发白发雾）
- [MODIFY] Demo `NeonTunnelShaders.RUNTIME_FS`：去掉片元内 `pow(0.4545)` 双伽马
- [MODIFY] `misuse` 错误 18/19；`kuikly-dev-android/runbook` Runtime / 镜像 / 画质坑
- 治理收尾：门禁 跳过｜三问 是/是/否｜小结级：misuse 18/19｜待批 0
- 真机验证表：`docs/reviews/device-check-list/android-runtime-dualtrack.md`（审核员产出；码农只登记）
- 自测：rebuild `.so` + `:androidApp:installDebug` → `holm_runtime_m0`

## 2026-11-13（升 AGP 8.13.2 + compileSdk 37）

- [MODIFY] 根 `build.gradle.kts`：AGP `8.3.2`→`8.13.2`；去掉强制 R8 覆盖（交还 AGP 自带）
- [MODIFY] Gradle Wrapper `8.5`→`8.13`；各模块 `compileSdk=37`；`android.suppressUnsupportedCompileSdk=37`
- [MODIFY] 本机 `sdk.dir`→`D:\Android_SDK`（`platforms/android-37` 联到 `android-37.0`）
- [MODIFY] Wrapper / Maven **仅**腾讯云：`gradle` + `maven-public` + `gradle-plugins`（去掉 google/central 直连）
- [MODIFY] `compileSdk { release(37){ minorApiLevel=0 } }` + `buildToolsVersion=36.0.0`；`android.builder.sdkDownload=false`
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：工具链对齐本机仅有 SDK 37｜待批 0
- 本单无真机验证
- 自测：`:androidApp:compileDebugKotlin` **BUILD SUCCESSFUL**（Gradle 8.13 + AGP 8.13.2）

## 2026-11-13（Android Runtime DualTrack：SurfaceView + 外泵 + arm64 .so）

- [MODIFY] `holm-runtime`：共享 `session_core`；`android_jni` 从 `Surface`→`ANativeWindow` 建 Session；`build-android.ps1` → `androidApp/.../jniLibs/arm64-v8a`
- [MODIFY] `HolmRuntimeNative`（既有）+ `KRHolmSurfaceView`：Runtime 换 `SurfaceView`；`HandlerThread`+`Choreographer` 外泵 `tick`；急拆；Lite 还原 GLES
- [MODIFY] Demo `holm_runtime_m0`：去掉「Android Runtime 下一单」文案
- [MODIFY] CONTEXT / glossary / ADR 0006 / `holm-runtime/README`
- 已知遗留：真机 frost/glow/liquid/custom + logcat `[holm-perf]` 未跑｜不修理由：本单编译门禁；真机表交审核员
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：按确认 Plan 接线 Android Runtime｜待批 0
- 真机验证表：`docs/reviews/device-check-list/android-runtime-dualtrack.md`（审核员产出；码农只登记）

## 2026-11-13（审查 r3 整改：executor 对称拆泵 / Worker 失败回退 / 竞态）

- [MODIFY] H5：`runtime→lite` 走 `destroyPump()` 再 `startPump()`（修 P0 Worker 残留）
- [MODIFY] H5：Worker `CREATE_SESSION` 等异步失败 → 拆泵 + 主线程回退一次，再失败 BLOCKED+ready（修 P0 卡死）
- [MODIFY] H5：`_attached` + wasm `setInterval` 可取消；`createSession` 卸岛守卫（P1-concurrency）
- [MODIFY] `backendId`：WebGPU=`wgpu-m0`，GL 后端=`wgpu-m0-gl`（P1 能力诚实）
- [MODIFY] uncapped 改回 rAF（降 P2 busy-spin）；Worker `reportPerf` 门控；Demo ready 清 errorText
- 已知遗留：主线程预载 + Worker 再载 wasm（双载）未解｜不修理由：探针与泵生命周期拆分，改动面大
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：r3 打回整改｜待批 0
- 本单无真机验证（表仍挂 P1-device-verify）

## 2026-11-13（Runtime maxFps=0 对齐高刷：uncapped timer + AutoNoVsync）

- [MODIFY] Runtime 泵：`maxFps<=0` 也走 `setTimeout(0)` 紧调度（Worker 上纯 rAF+WebGPU 常吃不满 165Hz，Lite WebGL 可以）
- [MODIFY] Session present：`AutoNoVsync` 优先；`desired_maximum_frame_latency=1`
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：uncapped 对齐 Lite 高刷｜待批 0
- 本单无真机验证（H5 Runtime 点 0 对比 Lite onPerf）

## 2026-11-13（maxFps>60：timer 泵 + Mailbox/Immediate present）

- [MODIFY] Runtime/Lite H5 泵：`maxFps>60` 用 `setTimeout` 调度（破 rAF 屏刷封顶）；改档重启调度
- [MODIFY] `holm-runtime` Session：present 优先 `Mailbox`/`Immediate`，避免 Fifo 卡 tick
- [MODIFY] 主线程 Runtime 回退环同样 timer/rAF 分流
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：120 档可达提交率｜待批 0
- 本单无真机验证（H5 点 120 看 onPerf；60Hz 屏观感仍可能合帧）

## 2026-11-13（Runtime Demo 可调 maxFps）

- [MODIFY] `HolmRuntimeM0Pager`：`maxFps` 档位 0/30/60/120 传入 `HolmSurface`（泵侧本已支持；Worker/主线程限提交）
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：Demo 旋钮｜待批 0
- 本单无真机验证（H5 点 30 看 onPerf≈30）

## 2026-11-13（Runtime Demo 接 onPerf 显示真实提交 FPS）

- [MODIFY] `HolmRuntimeM0Pager`：接 `onPerf`，状态行显示 `fps=N (Xms)`（与 Lite 同语义：约 1s 窗真实 tick 提交率）
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：Demo 接线｜待批 0
- 本单无真机验证（H5 硬刷 `holm_runtime_m0` 看状态行）

## 2026-11-13（Runtime 性能对齐 Lite：Worker + 缓冲 + shader）

- [MODIFY] H5 Runtime：**优先 Worker + OffscreenCanvas** 泵帧（`HolmRuntimePumpSource`）；主线程回退保留
- [MODIFY] 缓冲尺寸与 Lite **同策**：CSS 像素，**不默认乘 DPR**（根因：同屏像素曾翻 4~9 倍）
- [MODIFY] 默认 **maxFps 仍 60**；perf 仅 `onPerf` 时约 1s 上报；`playing=false` 停 rAF / tick 跳过 GPU
- [MODIFY] WGSL：frost 抖动 UV 单采样（去 13-tap）；liquid 去 soft_blur，chroma 条件化
- [MODIFY] `HolmRuntimeSession.create` 支持 `HtmlCanvasElement | OffscreenCanvas`
- 包体：`holm_runtime_bg.wasm` ≈ **3.85 MiB**（4032600 B）
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：Runtime≥Lite 性能硬约束｜待批 0
- 本单无真机验证（浏览器对比 Lite/Runtime CPU；Android 未开）

## 2026-11-13（H5 Runtime Lite3：帧循环 + frost/glow/liquid 观感近似）

- [MODIFY] `holm-runtime`：`HolmRuntimeSession`（create/tick/resize/destroy + setScene/setLiquid）；WGSL 三席 + `clear_tri` fallback
- [MODIFY] H5：`HolmRuntimeNative` Session API；`KRHolmSurfaceView` Runtime rAF 泵帧；Demo 旁路页三席 chip + liquid 旋钮
- [MODIFY] `index.html`：`createSession` → `HolmRuntimeSession.create`
- 包体：`holm_runtime_bg.wasm` ≈ **3.84 MiB**（4029243 B）
- 同源说明：与 Lite GLSL **非逐字**，观感近似；对照形态 C（对 Lite 五席页肉眼比）
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：计划 M-H5-0..4｜待批 0
- 本单无真机验证（浏览器目视三席；Android 未开）

## 2026-11-13（M0 可见三角：清屏同色误判「没内容」）

- [MODIFY] `holm-runtime`：`m0_present` 清屏 + WGSL 琥珀黄三角（对比深蓝底）
- [MODIFY] H5：present 成功后去掉 canvas CSS `background`，避免与 GPU 同色混淆
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：M0 通面可见性｜待批 0
- 本单无真机验证（浏览器硬刷 Runtime 页目视三角）

## 2026-11-12（装 rustc 打通 H5 wasm M0）

- [MODIFY] `holm-runtime`：wasm-bindgen `holm_runtime_m0_present`（wgpu WebGPU|WebGL2 清屏）；`build-wasm.ps1`
- [ADD] `h5App/.../resources/holm-runtime/{holm_runtime.js,holm_runtime_bg.wasm}` + `index.html` 预载 `window.__holmRuntime`
- [MODIFY] H5 `HolmRuntimeNative`：探测 ready/hasWgpu + `presentM0`；`KRHolmSurfaceView` runtime 异步 present，失败仍 `wgpu-blocked`
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：工具链解锁 + DualTrack B 旁路通面｜待批 0（已达标 0）
- 本单无真机验证（浏览器目视清屏待你开 8080/8083）

## 2026-11-12（审查整改：onError 重放 + Runtime 假通面）


- [MODIFY] Android/H5：`onError` 缓存并在回调注册时重放（修 P1-concurrency）
- [MODIFY] Android：`playing` 在 `executor=runtime` 时不切 Lite CONTINUOUSLY（P2-playing）
- [MODIFY] `HolmRuntimeNative`：`isAvailable` = library ∧ `isM0Presentable`（本波 presentable=false，防空 .so 假通面）
- [MODIFY] iOS：`onError` 同样 pending 重放；文档改「frost 逐字 / liquid 骨架同源」
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：审查必修+可改 P2｜待批 0（已达标 0）
- 本单无真机验证（P1-device-verify 仍待表）

## 2026-11-12（DualTrack 尖刺落地）

- [ADD] Track A：iOS `KRHolmSurface.{h,m}`（GLES2 `frost`/`liquid_approx`）；pbxproj 替换 Placeholder；`docs/specs/kuikly-dev-ios/runbook.md` 起服
- [ADD] Track B：`holm-runtime/` Rust+wgpu crate 骨架；本机无 rustc → **BLOCKED**（不静默换栈）
- [ADD] Attr `executor`（`lite`|`runtime`，默认 `lite`）；Android/H5 旁路探测 `HolmRuntimeNative`
- [ADD] Demo `holm_runtime_m0` 第二页；Home 默认仍进 Lite 五席
- [ADD] 对比表填表 + ADR 0004 **Keep Lite**（O0 不改）
- 治理收尾：门禁 跳过｜三问 否/否/否｜理由：按 DualTrack 计划尖刺收口｜待批 0（已达标 0）
- 本单无真机验证（iOS 需 Mac；Runtime 需 rustc）
