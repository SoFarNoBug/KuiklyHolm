# KuiklyHolm

在 Kuikly 页面里用局部 GPU 岛做高级视觉特效（及可选简单 3D）。业务优先；统一 Runtime 不是默认必上。

| 项 | 值 |
|---|---|
| 根工程 | `KuiklyHolmUI` |
| 三包 | `:holm-core` / `:holm-renderer-gl` / `:holm-renderer-wgpu` |
| Umbrella | `:KuiklyHolmCore` · `com.jlj.kuiklybase.holm`（兼容只引 Core） |
| Demo | `:holmDemo` · `com.jlj.kuiklyholmui` |
| 坐标 | `io.github.sofarnobug:KuiklyHolmCore` |

## 快速依赖（Maven Central）

```kotlin
implementation("io.github.sofarnobug:KuiklyHolmCore:1.0.0-2.31.0")
```

版本与 Kuikly / Compose 对齐策略见发布说明；未发版前请用源码模块依赖。

## 文档

- [CONTEXT.md](CONTEXT.md) — 术语与工程壳
- [docs/product/](docs/product/) — 产品决策与阶段
- [CONTRIBUTING.md](CONTRIBUTING.md) — 维护者：起 Demo / 发布 / 审查

## License

见 [LICENSE](LICENSE)。
