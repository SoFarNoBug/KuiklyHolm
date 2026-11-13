doc_version: 20261112_1
layer: L1

# Rust 语言规范（L1）

> **范围**：Rust 语言惯用、默认格式、库 API 呈现与 `unsafe` 底线；**不含** wgpu / WebGPU 平台约束（→ L2 [`platform_wgpu.md`](platform_wgpu.md)），不含单仓 crate 布局 / JNI / 业务 ABI（→ **各项目 L3**）。本文件属 **可移植 L1**。  
> **来源（官方背书；冲突时 Reference/Style 优先于建议性指南）**：  
> 1. [Rust Style Guide](https://doc.rust-lang.org/stable/style-guide/)（`rustfmt` 默认格式；[Style Edition](https://doc.rust-lang.org/edition-guide/rust-2024/rustfmt-style-edition.html)）  
> 2. [Rust API Guidelines](https://rust-lang.github.io/api-guidelines/)（[Checklist](https://rust-lang.github.io/api-guidelines/checklist.html)；library team）  
> 3. [The Rust Reference · Behavior considered undefined](https://doc.rust-lang.org/reference/behavior-considered-undefined.html)  
> 4. [The Rustonomicon](https://doc.rust-lang.org/stable/nomicon/)（写 `unsafe` 前必读 companion）  
> 未列条目默认跟上述来源；冲突序 **L3 > L2 > L1**。

## 硬约束

### 1. 格式（Style Guide）

来源：[Rust Style Guide](https://doc.rust-lang.org/stable/style-guide/)

- 缩进用 **空格**，每级 **4**；行宽默认 **100**。
- 逗号分隔列表在换行时使用 **trailing comma**。
- 不以手工替代格式工具：库 / 应用代码以 **`cargo fmt`**（及项目约定的 Style Edition）为准；编辑器 format-on-save 须与 CI 同一 `style_edition`（见 [Edition Guide · rustfmt Style Edition](https://doc.rust-lang.org/edition-guide/rust-2024/rustfmt-style-edition.html)）。
- 本文件**不**复述全部排版细则；与 `rustfmt` 冲突时以官方 Style Guide / 工具链 bug 流程为准。

### 2. 命名与转换（API Guidelines · Naming）

来源：[API Guidelines Checklist · Naming](https://rust-lang.github.io/api-guidelines/checklist.html)（C-CASE / C-CONV / C-GETTER / C-ITER 等）

- 大小写遵循 **RFC 430**（C-CASE）：类型 `UpperCamelCase`，函数/方法/变量 `snake_case`，常量 `SCREAMING_SNAKE_CASE` 等。
- 转换命名遵循 `as_` / `to_` / `into_` 约定（C-CONV）；getter 遵循 Rust 惯例（C-GETTER）。
- 集合上产生迭代器的方法用 `iter` / `iter_mut` / `into_iter`（C-ITER）；迭代器类型名与方法对应（C-ITER-TY）。
- feature 名避免占位词（C-FEATURE）；多词命名保持一致词序（C-WORD-ORDER）。

### 3. 互操作与可预期 API（API Guidelines · Interop / Predictability）

来源：同 Checklist · Interoperability / Predictability

- 公开类型在合理处实现常见 trait（C-COMMON-TRAITS）：至少优先考虑 `Debug`；能 `Copy`/`Clone`/`Eq` 等则实现（C-DEBUG）。
- 转换优先标准 trait：`From` / `AsRef` / `AsMut`（C-CONV-TRAITS）。
- 错误类型有意义且行为良好（C-GOOD-ERR）；函数文档须覆盖 **Errors / Panics / Safety**（C-FAILURE）。
- 构造器用静态固有方法（C-CTOR）；清晰接收者做成方法（C-METHOD）；避免 out-parameter（C-NO-OUT）。
- 仅智能指针实现 `Deref`/`DerefMut`（C-DEREF）。

### 4. 文档与示例（API Guidelines · Documentation）

来源：[Documentation](https://rust-lang.github.io/api-guidelines/documentation.html)

- 公开项应有 rustdoc；示例优先用 **`?`**，不要示范性 `unwrap` / 废弃的 `try!`（C-QUESTION-MARK）。
- `unsafe` 函数必须有 **Safety** 节，写清调用方不变量（C-FAILURE）。
- `Cargo.toml` `[package]` 含常见元数据（C-METADATA）：至少 `description`、`license`、`repository`（及项目要求的其余字段）。

### 5. `unsafe` 与未定义行为（Reference + Nomicon）

来源：[Behavior considered undefined](https://doc.rust-lang.org/reference/behavior-considered-undefined.html)、[Rustonomicon · Working with Unsafe](https://doc.rust-lang.org/nomicon/working-with-unsafe.html)

- Rust 程序**不得**表现出 Reference 所列 UB（含 `unsafe` 块内）。`unsafe` 只表示「避免 UB 的责任在程序员」，不改变「禁止 UB」本身。
- 写 `unsafe` 前阅读 Nomicon；安全抽象须 **sound**：任意安全客户端不能通过该 API 触发 UB。
- 用 **模块隐私** 限制依赖不变量的 `unsafe` 作用域（Nomicon：unsafe 污染可扩散到模块）。
- FFI：错误调用约定 / 跨不允许 unwind 的帧展开等属 UB（Reference）；`extern "C"` 边界不得让 panic 无约定地穿透（细节 → 各项目 L3 互操作政策）。

### 6. 错误处理取向（API Guidelines 推导）

- 库代码对可恢复失败优先 **`Result` / `?`**；示例与公共路径避免教用户无脑 `unwrap`（C-QUESTION-MARK）。
- 仅在文档标明的不变式下、或进程不可继续时允许 panic，并写 **Panics** 节（C-FAILURE）。

## 判据速查

| 场景 | 结论 |
|---|---|
| 手改缩进/行宽与 `cargo fmt` 对抗 | **FAIL** → 跟 Style Guide / rustfmt |
| 公开类型无 `Debug` 且无理由 | **FAIL**（C-DEBUG） |
| `unsafe fn` 无 Safety 文档 | **FAIL**（C-FAILURE） |
| 安全 API 可被误用成 UB | **FAIL**（Reference soundness） |
| 示例教人 `unwrap` 吞错 | **FAIL**（C-QUESTION-MARK） |
| wgpu Device/Queue/每帧 submit | **L2** [`platform_wgpu.md`](platform_wgpu.md) |
| crate 名、cdylib ABI、JNI 符号 | **各项目 L3** |

## 非本文件

- WebGPU / wgpu 资源与帧循环 → L2 [`platform_wgpu.md`](platform_wgpu.md)
- Clippy 全套 warn 升 error、edition 钉死策略 → **各项目 L3 / CI**
- 排版机械细节全文 → [Style Guide](https://doc.rust-lang.org/stable/style-guide/)；勿在本文件双写
