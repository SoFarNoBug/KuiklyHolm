# changelog · HolmContent + 最小 glTF（ADR 0016）

## 2026-11-18

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
- 须本机重编并装入 `libkmp_wgpu_native.so` 后 Model+wgpu 才能真跑（未重编会 BLOCKED）
