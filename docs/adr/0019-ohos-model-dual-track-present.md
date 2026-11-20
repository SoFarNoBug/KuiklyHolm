# ADR 0019: 鸿蒙 Model 真画双轨（GLES + wgpu ModelPass）

- Status: Accepted
- Date: 2026-11-19

## Context

ADR 0016 将 Model 真画先钉在 Android；ADR 0018 扩至 iOS。鸿蒙壳对 `contentKind=model` 固定 `MODEL_UNSUPPORTED`。Shader DualTrack（ADR 0010/0011）与 kmp-draco Ohos `.so`、kmp-wgpu Fullscreen Ohos 已齐；缺 ModelPass Ohos ABI、GLES 网格宿主、`holm-gltf` ohosArm64 与 rawfile 资产。

## Decision

1. **真画范围**：`HolmModelContent` 在 **Android + iOS + Ohos** 双轨出画（`renderer-gl` = GLES 网格宿主，`onReady.backend=gles2-model`；`renderer-wgpu` = `kmp_wgpu_model_create_ohos` / ModelPass）。**H5 真画见 [ADR 0020](0020-h5-model-dual-track-present.md)**。
2. **分工**：对齐 ADR 0010/0011 — GLES mesh 在 `libholm_xc` C++；wgpu 经 `holm_ohos_bridge_*` → KN，**禁止**壳直调 `kmp_wgpu_*`。
3. **解析 / Draco**：`holm-gltf` ohosArm64 硬依赖 `kmp-draco`；`settings.ohos` `includeBuild` `kmp-draco/ohosInclude`；App 装 `libkmp_draco_native.so`；Tokyo 可解。
4. **资源**：Demo GLB 进 `ohosApp` rawfile `models/`；路径约定与 Android/iOS 一致。
5. **桥**：`HolmOhosModelBridge` 按岛 session；Registry → 异步 parse → snapshot；切资产保 orbit。
6. **非目标**：多材质/纹理；改 Shadertoy Lite 语义。（H5 Model 已移出本 ADR 非目标，见 0020。）

## Consequences

- 修订 guide / `holm_api` / ADR 0016·0018 文首；真机冒烟表 `device-check-list/holm-ohos-glb.md`。
- ADR 0018「Ohos BLOCKED」已关；H5 由 ADR 0020 承接。
