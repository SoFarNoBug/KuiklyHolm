# ADR 0016: HolmContent 契约 + 自研最小 glTF（关闭 O3）

- Status: Accepted
- Date: 2026-11-18
- Branch: `feat/holm-model-content-android`

## Context

底座双轨与 kmp-wgpu 分发已齐。产品要英雄区加载 `.glb` 并拖转观看，同时保留片元特效。若把 `modelUri` 与 `fragmentShader` 塞进同一 Attr 袋会双模式冲突；若引入 Filament 则与 ADR 0007/0013（底座 + kmp-wgpu）叠床且多端故事重。`00` O3「3D / Filament」待关。

## Decision

1. **交付物**：`HolmSurface` **只接受** `HolmContent`（实现：`HolmShaderContent` / `HolmModelContent`）。废除对外 `fragmentShader` Attr（故意 break；四端 Demo 同步迁）。
2. **岛 vs 内容**：岛保留 `executor` / `playing` / `maxFps` / `renderScale` / 回调；Content 只描述画什么。允许热换 Content（卸旧 GPU 资源）。无 Content → 清屏、不报错。
3. **相机 / 手势**：仅 `HolmModelContent` 有真相机；默认库内单指轨道，可关并由业务回写角度。不内置自转；业务用角度 API 自己转。Shader 不装假相机。
4. **模型栈（关 O3）**：**不引入 Filament**。自研最小 glTF：库读网格并画（第一版画文件内全部静态 mesh）；业务可配基础色 override、透视相机、N 盏灯（默认 **unlit**，库不自动配灯；上限见 Caps/接入文档）。解析在可选 Kotlin 模块 **`holm-gltf`**；GL / wgpu 两条 Present 都要，业务 API 统一，仍显式 `executor`。wgpu 网格 Present 走 **kmp-wgpu `ModelPass`**（独立于 FullscreenPass；ADR 0013 原语层），**禁止**用 GLES 层冒充 wgpu。不用模型的宿主可不引 `holm-gltf`。
5. **资源**：支持 assets / `file://` / 字节；**排除 http**。先验 Android assets；读+解析在后台，回渲染线程上传。
6. **范围**：本波 common Content 化；**Model 真画**经 [ADR 0018](0018-ios-model-dual-track-present.md) / [ADR 0019](0019-ohos-model-dual-track-present.md) / [ADR 0020](0020-h5-model-dual-track-present.md) 覆盖 **Android + iOS + Ohos + H5** 双轨。自动 framing（包围盒）保证打开看得见。材质默认用 glb；显式 albedo/solid 才覆盖。

## Considered Options

- 同一 `HolmSurface` 上 fragmentShader 与 modelUri 互斥 → 拒绝（API 双灵魂）
- Filament 作 Model 后端 → 拒绝（O3 关闭为自研；包体/多端/与 kmp-wgpu 叠床）
- Model 仅 wgpu → 拒绝（业务要求 GL 与 wgpu 都能画，统一 API）
- 解析进 kmp-wgpu-native → 拒绝（GL 轨与「kmp-wgpu 不懂 glTF」边界）

## Consequences

- 须新增/修订：`holm-core` Content API、可选 `:holm-gltf`、两 renderer 的 Model Present；`holm_api` / guide / Demo 全迁。
- ADR 0005 的「Attr 喂片元」被本决策在**对外契约**上取代为 Content；实现可暂 internally 复用薄壳。
- `docs/product/00` O3 → 关闭（自研最小 glTF，非 Filament）。
- M2 验收对齐：英雄 glb + 拖转；缺资源 onError；Android 优先。
