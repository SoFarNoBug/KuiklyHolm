//! Holm Runtime — DualTrack B · wgpu session（H5 wasm）+ native C ABI stubs.
//! H5：`HolmRuntimeSession` 泵帧；席位 frost/glow/liquid_approx（观感近似）+ clear_tri。

#![allow(dead_code)]

#[cfg(all(target_arch = "wasm32", feature = "wgpu_backend"))]
mod shaders;
#[cfg(all(target_arch = "wasm32", feature = "wgpu_backend"))]
mod session;

#[cfg(all(target_arch = "wasm32", feature = "wgpu_backend"))]
pub use session::HolmRuntimeSession;

pub const BACKEND_ID: &str = "wgpu-m0";

fn abi_version_inner() -> u32 {
    1
}

fn has_wgpu_inner() -> u32 {
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
