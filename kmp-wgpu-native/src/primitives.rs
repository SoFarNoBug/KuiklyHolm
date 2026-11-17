//! kmp-wgpu 原语边界（ADR 0013 · 方案 B）。
//!
//! 本模块标明 **Device / Queue / Surface** 与 Holm 全屏 Pass 的切割线：
//! - 原语：wgpu `Device` / `Queue` / `Surface` / pipeline / buffer（由 [`crate::holm_fullscreen`] 持有）
//! - Holm：`set_custom_fs` / ClearTri / liquid / playing / tick 全屏片元（非 kmp-wgpu 产品 API）
//!
//! v1 热路径仍是粗粒度 Session 能力块（一次 tick = encode+submit+present），
//! 禁止碎 JNI。Kotlin `:kmp-wgpu` 以类型壳暴露概念；跨桥走能力块。

#![allow(unused_imports)]

#[cfg(feature = "wgpu_backend")]
pub use wgpu::{Device, Queue, Surface, SurfaceConfiguration};
