//! Holm Runtime / **kmp_wgpu_native**（ADR 0013）— DualTrack B · wgpu。
//!
//! 长期对外原语身份：`kmp_wgpu_native`；本 crate 产物文件名暂仍 `holm_runtime`
//!（`.so` / wasm / xcframework）以兼容既有脚本与 App。
//! 内部：[`primitives`] 原语边界 + [`holm_fullscreen`] Holm 全屏 Pass；
//! 旧 ABI（JNI / C Session / wasm Session）薄封装其上。

#![allow(dead_code)]

#[cfg(feature = "wgpu_backend")]
mod shaders;

#[cfg(feature = "wgpu_backend")]
mod error_bridge;

#[cfg(feature = "wgpu_backend")]
mod primitives;

#[cfg(feature = "wgpu_backend")]
mod holm_fullscreen;

#[cfg(all(target_arch = "wasm32", feature = "wgpu_backend"))]
mod session;

#[cfg(all(target_arch = "wasm32", feature = "wgpu_backend"))]
pub use session::HolmRuntimeSession;

#[cfg(all(target_os = "android", feature = "wgpu_backend"))]
mod android_jni;

#[cfg(all(target_os = "ios", feature = "wgpu_backend"))]
mod ios_c_api;

#[cfg(all(target_env = "ohos", feature = "wgpu_backend"))]
mod ohos_c_api;

pub const BACKEND_ID: &str = "wgpu-m0";

pub(crate) fn abi_version_inner() -> u32 {
    1
}

pub(crate) fn has_wgpu_inner() -> u32 {
    #[cfg(feature = "wgpu_backend")]
    {
        1
    }
    #[cfg(not(feature = "wgpu_backend"))]
    {
        0
    }
}

#[cfg(not(target_arch = "wasm32"))]
#[no_mangle]
pub extern "C" fn holm_runtime_abi_version() -> u32 {
    abi_version_inner()
}

#[cfg(not(target_arch = "wasm32"))]
#[no_mangle]
pub extern "C" fn holm_runtime_has_wgpu() -> u32 {
    has_wgpu_inner()
}

/// 遗留 stub：通面请走 Android JNI / iOS C Session / H5 HolmRuntimeSession。
#[cfg(not(target_arch = "wasm32"))]
#[no_mangle]
pub extern "C" fn holm_runtime_m0_clear(_width: u32, _height: u32) -> i32 {
    -1
}

#[cfg(not(target_arch = "wasm32"))]
#[no_mangle]
pub extern "C" fn holm_runtime_m0_triangle(_width: u32, _height: u32) -> i32 {
    -1
}

#[cfg(target_arch = "wasm32")]
mod wasm_exports {
    use super::{abi_version_inner, has_wgpu_inner};
    use wasm_bindgen::prelude::*;

    #[wasm_bindgen]
    pub fn holm_runtime_abi_version() -> u32 {
        abi_version_inner()
    }

    #[wasm_bindgen]
    pub fn holm_runtime_has_wgpu() -> u32 {
        has_wgpu_inner()
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn abi_version_is_one() {
        assert_eq!(abi_version_inner(), 1);
    }
}
