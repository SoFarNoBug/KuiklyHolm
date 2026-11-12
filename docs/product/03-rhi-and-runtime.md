# 03 · RHI 与 Holm Runtime

> **仅 Stage 0 选 Standard/Full 后必读全文。** Lite 以各端轻量执行器跑同源 shader 产物即可，不强制本章 Runtime。  
> 决策 D2：锚点是 Shader，不是强制 wgpu。

## 1. RHI 是什么

**RHI**：把设备/纹理/管线/提交/呈现与具体 GPU API 解耦的一层。

**结论**：Standard 档可用 RHI（如 wgpu）换「一份执行逻辑」；**观感一致仍首先靠 WGSL 同源 + 构建期编译**（见 `01`）。

## 2. 选型（Standard+）

### 主候选：Rust + wgpu

| 优点 | 风险 |
| --- | --- |
| 语义贴近 WebGPU | 鸿蒙弱；包体须实测 |
| 可嵌入 App | 需 Rust/FFI |

### 备选

C++ Dawn / 自研薄 RHI——须保持单一 shader 源。

### 不推荐

Three.js 当生产 Runtime；未选 Standard 却强行上 wgpu。

## 3. RHI 最小契约

逻辑上具备 Device / Surface / Buffer / Texture / ShaderModule / Pipeline / Encoder / Queue。  
若直接用 wgpu，则「RHI = wgpu」。

| 平台 | 句柄 |
| --- | --- |
| Android | ANativeWindow / Surface |
| iOS | CAMetalLayer |
| 鸿蒙 | XComponent → NativeWindow |
| Web | canvas / GPUCanvasContext |

## 4. Holm Runtime Core

| 模块 | 职责 |
| --- | --- |
| Caps | 能力探测 |
| AssetHub | 资源缓存 |
| Scene | 节点/相机/特效实体 |
| PassGraph | Pass + Quality 剪枝 |
| Clock | t / seek / seed |
| Renderer | tick → submit |
| ErrorBus | 上报 onError |

### C ABI（稳定面）

```text
holm_instance* holm_create(holm_surface_desc*)
void holm_destroy(holm_instance*)
void holm_resize(holm_instance*, u32 w, u32 h, f32 scale)
void holm_set_scene(holm_instance*, const char* scene_id)
void holm_set_props(holm_instance*, const char* json)
void holm_set_playing(holm_instance*, bool)
void holm_seek(holm_instance*, f64 t_seconds)
void holm_tick(holm_instance*, f64 media_time_seconds)
const char* holm_last_error(holm_instance*)
```

ABI 变更须 bump 版本；与 `02` prop 同步。

## 5. Shader 策略

1. WGSL 为唯一手写源（或 GLSL ES3 + 强制翻译，CI 编全后端）。  
2. 禁止长期三套手写算法（AGSL/MSL/GLSL 分叉）。  
3. 与 Runtime 同版本打包。

## 6. 色彩与坐标

| 项 | 约定 |
| --- | --- |
| 工作色空间 | 线性 |
| 输出 | sRGB |
| 纹理 | 标明 sRGB vs linear |
| Y / NDC | 集中转换，禁各端私改 |
| 单位 | 物理像素进 RHI；逻辑像素 Bridge 换算 |

## 7. Present / vsync

默认 Fifo；单一时钟源（Bridge vsync **或** 内部，勿双驱动）。  
M0：`seek(0)` 稳定出帧。

## 8. 包体

M0 尖刺记录 Android/iOS/Web 增量；超预算用 feature 裁剪，不改观感参数。

## 9. Runtime 验收

- [ ] 同 WGSL 清屏两端容差内一致  
- [ ] resize / destroy / recreate  
- [ ] Caps 可在 Demo 显示  
- [ ] 鸿蒙：wgpu 或 GLES，ABI 仍 `holm_*`  

---

**上一篇**：[02-kuikly-integration.md](./02-kuikly-integration.md)  
**下一篇**：[04-scene-effects-3d.md](./04-scene-effects-3d.md)
