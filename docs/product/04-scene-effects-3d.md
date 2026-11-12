# 04 · Scene、特效与简单 3D（Holm）

## 1. Scene

与平台无关的「画什么」：实体、相机、材质、时间轴。  
Bridge 只传 `scene` + `propsJson`；解析只在 Holm Runtime。

### JSON 示例（2D）

```json
{
  "id": "fx_refraction_panel",
  "version": 1,
  "camera": { "type": "ortho", "zNear": 0.1, "zFar": 100 },
  "entities": [
    {
      "id": "glass",
      "type": "effect_quad",
      "material": "liquid_approx_v1",
      "props": { "blurPx": 12, "ior": 1.2, "tint": [1, 1, 1, 0.12] }
    }
  ],
  "passes": ["backdrop_blur", "refraction", "tonemap"]
}
```

### JSON 示例（3D）

```json
{
  "id": "model_hero",
  "version": 1,
  "camera": { "type": "perspective", "yfov": 0.8, "zNear": 0.1, "zFar": 100 },
  "entities": [
    { "id": "root", "type": "gltf", "uri": "assets/models/hero.glb" }
  ],
  "passes": ["opaque", "tonemap"]
}
```

## 2. PassGraph

液体/毛玻璃是有序图像处理，不是设个 alpha。

证据：Flutter liquid_glass 双 pass；Prismal 分档；Web blur+rim+可选 displacement；Apple 系统玻璃（仅作概念，Holm 不调用）。

### 质量档剪枝

| Pass | High | Standard | Reduced |
| --- | --- | --- | --- |
| backdrop_blur | ✓ | ✓（降分辨率） | ✗ 或极弱 |
| refraction | ✓ | 可选弱 | ✗ |
| chromatic_aberration | ✓ | ✗ | ✗ |
| specular_sheen | ✓ | ✓ | 简化描边 |
| tonemap | ✓ | ✓ | ✓ |
| gltf opaque | ✓ | ✓ | ✓（可降分辨率） |

## 3. 特效范围（M1）

### 做

| 特效 | 说明 |
| --- | --- |
| `clear_color` | M0 |
| `gradient_bg` | 程序化背景 |
| `fullscreen_warp` | UV 扭曲 |
| `frost_panel` | 模糊+tint+rim |
| `liquid_approx_v1` | frost+边缘折射近似 |

### 不做（第一期）

完整 SDF 液滴融合；陀螺仪全局光；复刻 UIGlassContainer。

### Backdrop（详见 `07` H1）

| 策略 | 建议 |
| --- | --- |
| A. 程序化/静态图 | 第一期可默认（最稳） |
| B. 宿主提交局部纹理 + 半分辨率 + 静态冻结 | **真霜化推荐默认** |
| C. 自动截层 | 高风险；默认不做 |
| 列表滚动实时真折射 | **明确不支持** |

证据：见 `07` H1 / evidence-index。

## 4. 简单 3D（M2）

| 做 | 不做 |
| --- | --- |
| glTF 静态网格 + 基础 PBR/unlit | 完整蒙皮可二期 |
| 轨道相机 / 自转 | 编辑器 |
| 1–2 灯 | GI / 复杂阴影 |
| 单模型英雄区 | 开放世界 |

默认自研最小 glTF；Filament 仅在 O3 论证后引入。

## 5. 与上层「玻璃组件库」

| 层 | 职责 |
| --- | --- |
| 业务组件库 | API、布局、降级 |
| HolmSurface | High/Standard 走 `liquid_approx_*` |
| Reduced | 可不建岛，半透明 View / Blur |

## 6. 场景版本

`version` 强制；不识则 onError。

## 7. 验收

### M1

- [ ] frost 四端 Demo  
- [ ] liquid High vs Reduced 可见差  
- [ ] seek 黄金图稳定  

### M2

- [ ] 同一 glb 已接通端旋转一致  
- [ ] 缺模型不崩宿主  

---

**上一篇**：[03-rhi-and-runtime.md](./03-rhi-and-runtime.md)  
**下一篇**：[05-consistency-qa-perf.md](./05-consistency-qa-perf.md)
