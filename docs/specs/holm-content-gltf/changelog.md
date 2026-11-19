# changelog · HolmContent + 最小 glTF（ADR 0016）

## 2026-11-19

- [NOTE] 审查 holm-ohos-glb r4 · 通过（关 map/scratch P2；见 kuikly-dev-ohos changelog）
- [FIX] Ohos Model P2：session map 加锁 + 帧路径 FloatArray scratch（见 kuikly-dev-ohos changelog）
- 轻量治理：路径=速速改｜sediment=否｜gate=已解锁｜真机表=holm-ohos-glb
- [FIX] Ohos Model FPS：GLES 仅 pending 上传 mesh；wgpu 去每帧 snapshot；ExpectedRate 90Hz 不再钉 45（见 kuikly-dev-ohos changelog）
- 轻量治理：路径=速速改｜sediment=否｜gate=已解锁｜真机表=device-check-list/holm-ohos-glb.md
- [FIX] 审查回执行 holm-ohos-glb：Ohos Model 异步 parse + 失败清 ready；壳帧轮询完成 Present（见 kuikly-dev-ohos changelog）
- 轻量治理：路径=速速改｜sediment=否｜gate=已解锁｜真机表=device-check-list/holm-ohos-glb.md｜审查回执行=P0/P1/P2
- [ADD] ADR 0019：鸿蒙 Model 双轨 Present（GLES `gles2-model` + wgpu ModelPass）；H5 仍 BLOCKED
- [NOTE] Ohos 编冒烟：`./ohos_build.sh` SUCCESS；`settings.ohos` 双 ohosInclude 命名消歧；真机表待勾
- 轻量治理：路径=按计划行事｜sediment=否｜gate=已解锁｜真机表=device-check-list/holm-ohos-glb.md
- [NOTE] 审查回执行 holm-ios-glb：用户确认 iPhone Duo Simulator OK → 关 `P1-device-verify`（见 kuikly-dev-ios changelog + `device-check-list/holm-ios-glb.md`）
- 轻量治理：路径=速速改｜sediment=否｜gate=跳过｜真机表=device-check-list/holm-ios-glb.md（已关）
- [NOTE] 审查回执行 holm-ios-glb：关 P1-concurrency + 全部 P2（见 kuikly-dev-ios changelog）；`P1-device-verify` 仍开放
- 轻量治理：路径=速速改｜sediment=否｜gate=已解锁｜真机表=device-check-list/holm-ios-glb.md
- [ADD] ADR 0018：iOS Model 真画双轨（GLES + Metal ModelPass）；Draco ios + Bundle 模型；Demo/guide/`holm_api` 同步
- 轻量治理：路径=按计划行事｜sediment=否｜gate=已解锁｜真机表=待审核员
- [FIX] GLES Model 开启背面剔除（`GL_CULL_FACE` / Back / CCW），与 wgpu ModelPass 对齐，减轻 Tokyo 双轨明暗不一致
- 轻量治理：路径=速速改｜sediment=否｜gate=跳过｜真机表=无
- [ADD] `HolmModelContent.setLights` 热更新（不重载 glTF）；GLES `u_lightIntensity`；wgpu 强度乘 albedo 近似；Demo 可展开灯光 sheet（方位/仰角/强度滑杆）
- 轻量治理：路径=速速改｜sediment=否｜gate=跳过｜真机表=无
- [FIX] 同轨换 Model 资产：保留 orbit；不拆 Pass / 不重灌旧 mesh（避免闪一下 + 视角重置；Tokyo 仍需异步 decode 耗时）
- 轻量治理：路径=速速改｜sediment=否｜gate=跳过｜真机表=无
- [FIX] Model 透视 near/far 改为 `0.05×/10× eyeDist`（纠正先前 `eyeDist-1.5R` 近裁过狠导致双轨透视坏）
- 轻量治理：路径=速速改｜sediment=否｜gate=跳过｜真机表=无
- [FIX] GLES/wgpu Model 轨道：按 eyeDist 动态 near/far；Model GLSurfaceView 要 24-bit depth + `GL_LEQUAL`（Tokyo 拖转表面闪烁 / z-fighting）
- 轻量治理：路径=速速改｜sediment=否｜gate=跳过｜真机表=无本单新表

## 2026-11-18

- [FIX] 审查 kmp-draco r1：P0 — 旁路仓在则默认 includeBuild（`kmp.draco.local=false` 才走 Maven）+ `publish-maven.sh local`；P1 — 补 `connectedDebugAndroidTest` Tokyo 切片；P2 — `texCoords` KDoc / README 仅 arm64；蒙皮仍非本波
- 轻量治理：路径=速速改｜sediment=否｜gate=跳过｜真机表=device-check-list/kmp-draco.md（待勾）｜审查回执行=r1 P0/P1-plan-soft-drift/P2
- [ADD] ADR 0017：`:holm-gltf` 硬依赖 `kmp-draco`（`KHR_draco`）；独立仓 `~/Studio/kmp/kmp-draco`（Rust `draco-core` + Android JNI）；`kmp.draco.local` includeBuild；解析失败 fail-loud；`.so` ≈621 KiB（arm64）
- 轻量治理：路径=按计划行事｜sediment=见收尾｜gate=跳过（纯业务）｜真机表=device-check-list/holm-content-gltf.md（待审核员补 Tokyo/Draco 步）
- [MODIFY] Demo 去掉 Box/hero 立方体；接入 three.js 示例 GLB（Flamingo/Parrot/…）；保留 Duck；更新 ATTRIBUTION
- [ADD] `holm_model` 展示 onPerf；GLES Model 层补帧率统计（wgpu 沿用 tickRuntimePerf）
- [FIX] Model 轨道手势改转台：手指左右/上下均与模型同向（yaw+pitch 取反；GLES/wgpu 一致）
- 轻量治理：路径=速速改｜sediment=否｜gate=跳过｜真机表=无
- [ADD] Demo 接入 Khronos `Duck.glb` / `Box.glb` + 资产 chip；`assets/models/ATTRIBUTION.md`（CC BY / SCEA / 本仓 hero）
- 轻量治理：路径=速速改｜sediment=见收尾｜gate=跳过｜真机表=无本单新表
- [NOTE] 审查回执行 r3 · 通过（Vivo 真机 OK）
- [FIX] 审查 r2：`eagerDestroy` 后 Surface 仍活时 `rearmRuntimeSurfaceIfAlive` 再建 ModelPass/FullscreenPass（关 P0-silent-fail）；GLES `upload` 待 surface；H5 Model 双 fireError；detach unregister contentId；solidColor 显式时不再用 glTF factor 覆盖
- 轻量治理：路径=速速改｜sediment=见收尾｜gate=跳过｜真机表=device-check-list/holm-content-gltf.md（#8b）｜审查回执行=r2 P0/P1-concurrency/P2
- [ADD] `HolmContent` / `HolmShaderContent` / `HolmModelContent` + `HolmContentRegistry`；`HolmSurface` Compose 只收 `content`（Attr=`contentId`）
- [BREAK] 删除对外 `fragmentShader` / `vertexShader` / `scene` / warp·scroll 业务 Attr；Demo DualTrack 全迁 `HolmShaderContent`
- [ADD] 四端 View：`contentId` / `contentKind` 桥；非 Android `HolmModelContent` → `MODEL_UNSUPPORTED` + 清屏
- [ADD] 可选模块 `:holm-gltf`（`GltfDocument.parse` / `GltfMinimalGlb`）；不进 umbrella 必选
- [ADD] Android Model Present：异步读 assets|file|bytes → parse → GLES 网格（`executor=gl`）
- [ADD] **真 wgpu ModelPass**：`kmp-wgpu-native` `model_pass.rs` + JNI；`kmp-wgpu-surface` `ModelPass`；Holm `HolmModelPass`；View 在 Model+wgpu 走独立 Pass（非 Fullscreen、非 GLES 冒充）；`onReady.backend` 报 `wgpu-m0*` / `gles2-model`
- [ADD] Demo `holm_model` + `assets/models/hero.glb`（程序生成立方体；缺 assets 时 Present 回落 `GltfMinimalGlb`）
- [MODIFY] `docs/guide.md` / `holm_api` L3 + 三端 selfcheck H13；根 README 最小示例；ADR 0016 补 ModelPass 句
- [NOTE] 真机验证表：`docs/reviews/device-check-list/holm-content-gltf.md`（审核员产出；本单需重编 `libkmp_wgpu_native.so`）
- 轻量治理：路径=按计划行事｜sediment=见收尾三问｜gate=跳过（纯业务）｜真机表=device-check-list/holm-content-gltf.md

## 已知遗留

- 非 Android Model 真画、http 加载、蒙皮/动画、Filament：非本波目标
- carried-over: ModelPass 仅 Android JNI；iOS/H5/Ohos stub 未做｜本波范围仅 Android
- carried-over: kmp-draco 仅 Android JNI；iOS/JS expect 抛错｜ADR 0017 M0
- carried-over: Tokyo 等含 JOINTS/WEIGHTS 的蒙皮属性本波不消费｜非静默空白类
- 须本机重编并装入 `libkmp_wgpu_native.so` 后 Model+wgpu 才能真跑（未重编会 BLOCKED）
- Tokyo / `KHR_draco`：Rust + instrumented 夹具已备；真机 Demo 待勾 [`device-check-list/kmp-draco.md`](../../reviews/device-check-list/kmp-draco.md)
