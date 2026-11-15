//! iOS C Session ABI（ADR 0009）：CAMetalLayer → wgpu Metal Surface；宿主外泵 tick。
//! 所有对外 C 入口 `catch_unwind`：非法 WGSL 等 panic 不得穿 FFI 把进程 abort。

use crate::session_core::HolmRuntimeSessionCore;
use std::ffi::{c_char, CStr};
use std::os::raw::c_void;
use std::panic::{catch_unwind, AssertUnwindSafe};
use std::ptr;

struct NativeSession {
    core: HolmRuntimeSessionCore,
}

fn box_session(session: NativeSession) -> u64 {
    Box::into_raw(Box::new(session)) as u64
}

unsafe fn from_handle<'a>(handle: u64) -> Option<&'a mut NativeSession> {
    if handle == 0 {
        return None;
    }
    Some(&mut *(handle as *mut NativeSession))
}

/// Create Session bound to a CAMetalLayer. Layer must outlive the session; Rust does not retain it.
#[no_mangle]
pub extern "C" fn holm_runtime_session_create_metal(
    metal_layer: *mut c_void,
    width: u32,
    height: u32,
) -> u64 {
    if metal_layer.is_null() {
        return 0;
    }
    let w = width.max(1);
    let h = height.max(1);

    let result = catch_unwind(AssertUnwindSafe(|| {
        let mut desc = wgpu::InstanceDescriptor::new_without_display_handle();
        desc.backends = wgpu::Backends::METAL;
        let instance = wgpu::Instance::new(desc);
        // SAFETY: caller guarantees a live CAMetalLayer* for the session lifetime.
        let target = wgpu::SurfaceTargetUnsafe::CoreAnimationLayer(metal_layer);
        let surface = unsafe { instance.create_surface_unsafe(target) }
            .map_err(|e| format!("create_surface: {e}"))?;
        let core = pollster::block_on(HolmRuntimeSessionCore::create_with_surface(
            instance, surface, w, h, false,
        ))?;
        Ok::<HolmRuntimeSessionCore, String>(core)
    }));

    match result {
        Ok(Ok(core)) => box_session(NativeSession { core }),
        _ => 0,
    }
}

#[no_mangle]
pub extern "C" fn holm_runtime_session_destroy(handle: u64) {
    if handle == 0 {
        return;
    }
    let _ = catch_unwind(AssertUnwindSafe(|| unsafe {
        drop(Box::from_raw(handle as *mut NativeSession));
    }));
}

#[no_mangle]
pub extern "C" fn holm_runtime_session_resize(handle: u64, width: u32, height: u32) {
    let _ = catch_unwind(AssertUnwindSafe(|| {
        if let Some(s) = unsafe { from_handle(handle) } {
            s.core.resize(width.max(1), height.max(1));
        }
    }));
}

#[no_mangle]
pub extern "C" fn holm_runtime_session_set_scene(handle: u64, scene: *const c_char) {
    if scene.is_null() {
        return;
    }
    let _ = catch_unwind(AssertUnwindSafe(|| {
        let Some(s) = (unsafe { from_handle(handle) }) else {
            return;
        };
        let Ok(name) = unsafe { CStr::from_ptr(scene) }.to_str() else {
            return;
        };
        s.core.set_scene(name);
    }));
}

#[no_mangle]
pub extern "C" fn holm_runtime_session_set_custom_fs(handle: u64, fs: *const c_char) -> u32 {
    let result = catch_unwind(AssertUnwindSafe(|| {
        let Some(s) = (unsafe { from_handle(handle) }) else {
            return 0u32;
        };
        let src = if fs.is_null() {
            ""
        } else {
            match unsafe { CStr::from_ptr(fs) }.to_str() {
                Ok(v) => v,
                Err(_) => return 0u32,
            }
        };
        match s.core.set_custom_fs(src) {
            Ok(()) => 1u32,
            Err(_) => 0u32,
        }
    }));
    match result {
        Ok(v) => v,
        Err(_) => 0,
    }
}

#[no_mangle]
pub extern "C" fn holm_runtime_session_set_liquid(handle: u64, boost: f32, bend: f32) {
    let _ = catch_unwind(AssertUnwindSafe(|| {
        if let Some(s) = unsafe { from_handle(handle) } {
            s.core.set_liquid(boost, bend);
        }
    }));
}

#[no_mangle]
pub extern "C" fn holm_runtime_session_set_playing(handle: u64, playing: u32) {
    let _ = catch_unwind(AssertUnwindSafe(|| {
        if let Some(s) = unsafe { from_handle(handle) } {
            s.core.set_playing(playing != 0);
        }
    }));
}

#[no_mangle]
pub extern "C" fn holm_runtime_session_tick(handle: u64, dt_secs: f32) -> u32 {
    let result = catch_unwind(AssertUnwindSafe(|| {
        let Some(s) = (unsafe { from_handle(handle) }) else {
            return 0u32;
        };
        match s.core.tick(dt_secs) {
            Ok(()) => 1u32,
            Err(_) => 0u32,
        }
    }));
    match result {
        Ok(v) => v,
        Err(_) => 0,
    }
}

#[no_mangle]
pub extern "C" fn holm_runtime_session_backend_id(
    handle: u64,
    out_buf: *mut c_char,
    out_len: u32,
) -> u32 {
    if out_buf.is_null() || out_len == 0 {
        return 0;
    }
    let result = catch_unwind(AssertUnwindSafe(|| {
        let id = unsafe { from_handle(handle) }
            .map(|s| s.core.backend_id)
            .unwrap_or("wgpu-blocked");
        let bytes = id.as_bytes();
        let max = (out_len as usize).saturating_sub(1);
        let n = bytes.len().min(max);
        unsafe {
            ptr::copy_nonoverlapping(bytes.as_ptr(), out_buf as *mut u8, n);
            *out_buf.add(n) = 0;
        }
        n as u32
    }));
    match result {
        Ok(v) => v,
        Err(_) => 0,
    }
}

fn copy_utf8(s: &str, out_buf: *mut c_char, out_len: u32) -> u32 {
    if out_buf.is_null() || out_len == 0 {
        return 0;
    }
    let bytes = s.as_bytes();
    let max = (out_len as usize).saturating_sub(1);
    let n = bytes.len().min(max);
    unsafe {
        ptr::copy_nonoverlapping(bytes.as_ptr(), out_buf as *mut u8, n);
        *out_buf.add(n) = 0;
    }
    n as u32
}

#[no_mangle]
pub extern "C" fn holm_runtime_session_last_error_code(
    handle: u64,
    out_buf: *mut c_char,
    out_len: u32,
) -> u32 {
    let result = catch_unwind(AssertUnwindSafe(|| {
        let (code, _) = unsafe { from_handle(handle) }
            .map(|s| s.core.last_error())
            .unwrap_or_default();
        copy_utf8(&code, out_buf, out_len)
    }));
    match result {
        Ok(v) => v,
        Err(_) => 0,
    }
}

#[no_mangle]
pub extern "C" fn holm_runtime_session_last_error_message(
    handle: u64,
    out_buf: *mut c_char,
    out_len: u32,
) -> u32 {
    let result = catch_unwind(AssertUnwindSafe(|| {
        let (_, msg) = unsafe { from_handle(handle) }
            .map(|s| s.core.last_error())
            .unwrap_or_default();
        copy_utf8(&msg, out_buf, out_len)
    }));
    match result {
        Ok(v) => v,
        Err(_) => 0,
    }
}
