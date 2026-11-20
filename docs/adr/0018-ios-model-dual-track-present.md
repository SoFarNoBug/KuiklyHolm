# ADR 0018: iOS Model 真画双轨（GLES + wgpu ModelPass）

- Status: Accepted
- Date: 2026-11-19

## Context

ADR 0016 将 Model 真画先钉在 Android；ADR 0017 接通 `KHR_draco`（kmp-draco）。iOS 壳此前对 `contentKind=model` 固定 `MODEL_UNSUPPORTED`。kmp-draco M1 与 kmp-wgpu `kmp_wgpu_model_*` Metal C ABI 已齐，具备双轨 Present 条件。

## Decision

1. **真画范围**：`HolmModelContent` 在 **Android + iOS** 双轨出画（`renderer-gl` = GLES 网格宿主；`renderer-wgpu` = Metal `ModelPass`）。**Ohos 见 [ADR 0019](0019-ohos-model-dual-track-present.md)**；**H5 见 [ADR 0020](0020-h5-model-dual-track-present.md)**。
2. **解析 / Draco**：`holm-gltf` iosMain 硬依赖 `kmp-draco`；iosApp 链 `KmpDracoNative.xcframework`；Tokyo 可解。
3. **资源**：Demo GLB 进 iosApp Bundle `models/`；路径约定与 Android assets 一致（如 `models/Duck.glb`）。
4. **桥**：`HolmIosModelBridge` 负责 Registry → 后台读+parse → snapshot；`KRHolmGlesModelHostView` / `HolmWgpuIosBridge` ModelPass 分轨 Present；切资产保留 orbit、待新 mesh 再换（对齐 Android）。
5. **非目标**：多材质/纹理；Pod 发版流程变更。（Ohos / H5 Model 已移出本 ADR 非目标，见 0019 / 0020。）

## Consequences

- 修订 guide / `holm_api` / Demo 文案；真机冒烟表留给 review。
- ADR 0016「其它端可 BLOCKED」在 0018 时收窄为 Ohos/H5；0019 后再收窄为仅 H5。
