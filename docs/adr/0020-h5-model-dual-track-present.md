# ADR 0020: H5 Model 真画双轨（WebGL + wgpu ModelPass）

- Status: Accepted
- Date: 2026-11-19

## Context

ADR 0016–0019 将 `HolmModelContent` 真画钉在 Android / iOS / Ohos；H5 固定 `MODEL_UNSUPPORTED` + 清屏。Shader DualTrack 与 kmp-wgpu Fullscreen wasm、kmp-draco H5 wasm 已齐；缺：wasm ModelPass、WebGL 网格宿主、Core jsMain 接入 `:holm-gltf`、静态 GLB。

## Decision

1. **真画范围**：`HolmModelContent` 在 **Android + iOS + Ohos + H5** 双轨出画。
   - `renderer-gl` → WebGL 网格宿主，`onReady.backend=webgl-model`
   - `renderer-wgpu` → kmp-wgpu wasm **ModelPass**（canvas），`backend≈wgpu-m0*`
2. **无 WebGPU**：wgpu 轨 **`BLOCKED` 诚实失败**；**禁止**用 WebGL 冒充 wgpu（对齐 D8 / `holm_api`）。
3. **解析 / Draco**：Core **jsMain 依赖 `:holm-gltf`**；`DracoBitstream` 走 `window.__kmpDraco`（kmp-draco wasm）。
4. **资源**：Demo GLB 进 `h5App` 静态 `models/`（或构建拷贝）；路径约定与三端一致（如 `models/Duck.glb`）；**禁止** Content API 裸 http。
5. **桥**：`KRHolmSurfaceView` jsMain 按 executor 分轨；异步 parse + pending 上传；切轨对称 teardown；热路径勿每帧整模重灌。
6. **非目标**：多材质/纹理/蒙皮产品化；Three.js / 系统玻璃冒充；改 Shadertoy Lite 语义；本 ADR 强制 Maven 发版。

## Consequences

- 修订 guide / `holm_api` / ADR 0016·0018·0019 文首「H5 BLOCKED」；浏览器冒烟表 `device-check-list/holm-h5-glb.md`。
- kmp-wgpu 须增 wasm ModelPass API 并重编 glue 入 Holm `h5App` resources。
