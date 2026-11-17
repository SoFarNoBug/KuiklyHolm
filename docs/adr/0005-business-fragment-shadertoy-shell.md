# ADR 0005: 业务片元 Attr + Shadertoy 薄壳

## Status

Accepted（2026-11-13）

## Context

业务（以 Demo 为代表）需要把一段上色代码交给岛渲染，并在 Lite（GLES/WebGL）与 Runtime（wgpu/WGSL）之间对比性能。官方五席（ADR 0003）已锁，不能把「自定义」算作第六官方席。Shadertoy 习语（`mainImage` / `iTime` / `iResolution`）与岛内 `u_time` + UV 契约不一致。

## Decision

1. **可选 Attr** `fragmentShader` / `vertexShader`（空=走原 `scene` / 内置全屏 vert）。
2. **Lite**：`fragmentShader` 为 Shadertoy **正文**；库 `HolmShadertoyShell` 补 precision、`u_time`、`u_resolution`、入口 `main`。非空覆盖上色；**失败 `onError`，不静默回 frost**；保留上一成功程序，无成功程序时深蓝清屏。
3. **Runtime**：同 Attr 传 **WGSL 片元段**（含 `@fragment fn fs_main`）；`KmpWgpuSession.setCustomFs` 重建 pipeline。GLSL 不可直喂 Runtime。
4. **custom 不是官方五席**：Demo 可用 `scene` 状态机档位 `custom`，真正喂岛的是 Attr 源码；切回三席时清空 `fragmentShader`。
5. **本波验收端**：H5 双轨；Android Lite 可接自定义；**Android Runtime 打通为下一单**。

## Consequences

- 业务可热换片元做性能对比与特效实验；须维护 GLSL 正文 + WGSL 近似两份（方言不同）。
- 未知官方 `scene` 仍回 frost（ADR 0003）；与「自定义编译失败不回霜」正交。
- Shadertoy 子集仅 time + resolution + mainImage；纹理 / 多 Pass 不在本 ADR。
