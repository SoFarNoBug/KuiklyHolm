//! Android JNIÃ¯Â¼ÂÃ¤Â»Â `android.view.Surface` Ã¥Â»?SessionÃ¯Â¼ÂÃ¤Â¾Â Kotlin Ã¥Â¤ÂÃ¦Â³Âµ tickÃ£Â?

use crate::session_core::HolmRuntimeSessionCore;
use jni::objects::{JClass, JObject, JString};
use jni::sys::{jboolean, jfloat, jint, jlong, jstring, JNI_FALSE, JNI_TRUE};
use jni::JNIEnv;
use raw_window_handle::{
    AndroidDisplayHandle, AndroidNdkWindowHandle, DisplayHandle, HandleError, HasDisplayHandle,
    HasWindowHandle, RawDisplayHandle, RawWindowHandle, WindowHandle,
};
use std::ptr::NonNull;

struct AndroidWindowPtr(*mut ndk_sys::ANativeWindow);

// Safety: ANativeWindow Ã§Â?create/destroy Ã©ÂÂÃ¥Â¯Â¹Ã¯Â¼ÂSession Ã¤Â»ÂÃ¥ÂÂ¨ Holm Ã¦Â¸Â²Ã¦ÂÂÃ§ÂºÂ¿Ã§Â¨ÂÃ¤Â½Â¿Ã§ÂÂ¨Ã£Â?
unsafe impl Send for AndroidWindowPtr {}
unsafe impl Sync for AndroidWindowPtr {}

struct AndroidSurfaceTarget {
    window: AndroidWindowPtr,
}

impl HasWindowHandle for AndroidSurfaceTarget {
    fn window_handle(&self) -> Result<WindowHandle<'_>, HandleError> {
        let ptr = NonNull::new(self.window.0 as *mut _).ok_or(HandleError::Unavailable)?;
        let handle = AndroidNdkWindowHandle::new(ptr);
        Ok(unsafe { WindowHandle::borrow_raw(RawWindowHandle::AndroidNdk(handle)) })
    }
}

impl HasDisplayHandle for AndroidSurfaceTarget {
    fn display_handle(&self) -> Result<DisplayHandle<'_>, HandleError> {
        let handle = AndroidDisplayHandle::new();
        Ok(unsafe { DisplayHandle::borrow_raw(RawDisplayHandle::Android(handle)) })
    }
}

struct NativeSession {
    core: HolmRuntimeSessionCore,
    window: AndroidWindowPtr,
}

fn box_session(session: NativeSession) -> jlong {
    Box::into_raw(Box::new(session)) as jlong
}

unsafe fn from_handle<'a>(handle: jlong) -> Option<&'a mut NativeSession> {
    if handle == 0 {
        return None;
    }
    Some(&mut *(handle as *mut NativeSession))
}

fn release_window(ptr: *mut ndk_sys::ANativeWindow) {
    if !ptr.is_null() {
        unsafe { ndk_sys::ANativeWindow_release(ptr) };
    }
}

#[no_mangle]
pub extern "system" fn Java_com_jlj_kuiklybase_holm_HolmRuntimeNative_nativeAbiVersion(
    _env: JNIEnv,
    _class: JClass,
) -> jint {
    crate::abi_version_inner() as jint
}

#[no_mangle]
pub extern "system" fn Java_com_jlj_kuiklybase_holm_HolmRuntimeNative_nativeHasWgpu(
    _env: JNIEnv,
    _class: JClass,
) -> jint {
    crate::has_wgpu_inner() as jint
}

#[no_mangle]
pub extern "system" fn Java_com_jlj_kuiklybase_holm_HolmRuntimeNative_nativeCreate(
    env: JNIEnv,
    _class: JClass,
    surface: JObject,
    width: jint,
    height: jint,
) -> jlong {
    if surface.is_null() {
        return 0;
    }
    // fromSurface already acquires; release once in destroy
    let window_ptr =
        unsafe { ndk_sys::ANativeWindow_fromSurface(env.get_raw(), surface.as_raw()) };
    if window_ptr.is_null() {
        return 0;
    }

    let w = width.max(1) as u32;
    let h = height.max(1) as u32;
    let target = AndroidSurfaceTarget {
        window: AndroidWindowPtr(window_ptr),
    };

    let created = (|| {
        let instance = wgpu::Instance::new(wgpu::InstanceDescriptor {
            backends: wgpu::Backends::VULKAN | wgpu::Backends::GL,
            ..Default::default()
        });
        let target_unsafe = unsafe { wgpu::SurfaceTargetUnsafe::from_window(&target) }
            .map_err(|e| format!("surface_target: {e}"))?;
        let surface = unsafe { instance.create_surface_unsafe(target_unsafe) }
            .map_err(|e| format!("create_surface: {e}"))?;
        let core = pollster::block_on(HolmRuntimeSessionCore::create_with_surface(
            instance, surface, w, h, false,
        ))?;
        Ok::<HolmRuntimeSessionCore, String>(core)
    })();

    match created {
        Ok(core) => box_session(NativeSession {
            core,
            window: AndroidWindowPtr(window_ptr),
        }),
        Err(_) => {
            release_window(window_ptr);
            0
        }
    }
}

#[no_mangle]
pub extern "system" fn Java_com_jlj_kuiklybase_holm_HolmRuntimeNative_nativeDestroy(
    _env: JNIEnv,
    _class: JClass,
    handle: jlong,
) {
    if handle == 0 {
        return;
    }
    let boxed = unsafe { Box::from_raw(handle as *mut NativeSession) };
    let ptr = boxed.window.0;
    drop(boxed);
    release_window(ptr);
}

#[no_mangle]
pub extern "system" fn Java_com_jlj_kuiklybase_holm_HolmRuntimeNative_nativeResize(
    _env: JNIEnv,
    _class: JClass,
    handle: jlong,
    width: jint,
    height: jint,
) {
    if let Some(s) = unsafe { from_handle(handle) } {
        s.core.resize(width.max(1) as u32, height.max(1) as u32);
    }
}

#[no_mangle]
pub extern "system" fn Java_com_jlj_kuiklybase_holm_HolmRuntimeNative_nativeSetScene(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    scene: JString,
) {
    let Some(s) = (unsafe { from_handle(handle) }) else {
        return;
    };
    let Ok(name) = env.get_string(&scene) else {
        return;
    };
    s.core.set_scene(&name.to_string_lossy());
}

#[no_mangle]
pub extern "system" fn Java_com_jlj_kuiklybase_holm_HolmRuntimeNative_nativeSetCustomFs(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    fs: JString,
) -> jboolean {
    let Some(s) = (unsafe { from_handle(handle) }) else {
        return JNI_FALSE;
    };
    let Ok(src) = env.get_string(&fs) else {
        return JNI_FALSE;
    };
    match s.core.set_custom_fs(&src.to_string_lossy()) {
        Ok(()) => JNI_TRUE,
        Err(_) => JNI_FALSE,
    }
}

#[no_mangle]
pub extern "system" fn Java_com_jlj_kuiklybase_holm_HolmRuntimeNative_nativeSetLiquid(
    _env: JNIEnv,
    _class: JClass,
    handle: jlong,
    boost: jfloat,
    bend: jfloat,
) {
    if let Some(s) = unsafe { from_handle(handle) } {
        s.core.set_liquid(boost, bend);
    }
}

#[no_mangle]
pub extern "system" fn Java_com_jlj_kuiklybase_holm_HolmRuntimeNative_nativeSetPlaying(
    _env: JNIEnv,
    _class: JClass,
    handle: jlong,
    playing: jboolean,
) {
    if let Some(s) = unsafe { from_handle(handle) } {
        s.core.set_playing(playing != JNI_FALSE);
    }
}

#[no_mangle]
pub extern "system" fn Java_com_jlj_kuiklybase_holm_HolmRuntimeNative_nativeTick(
    _env: JNIEnv,
    _class: JClass,
    handle: jlong,
    dt_secs: jfloat,
) -> jboolean {
    let Some(s) = (unsafe { from_handle(handle) }) else {
        return JNI_FALSE;
    };
    match s.core.tick(dt_secs) {
        Ok(()) => JNI_TRUE,
        Err(_) => JNI_FALSE,
    }
}

#[no_mangle]
pub extern "system" fn Java_com_jlj_kuiklybase_holm_HolmRuntimeNative_nativeBackendId(
    env: JNIEnv,
    _class: JClass,
    handle: jlong,
) -> jstring {
    let id = unsafe { from_handle(handle) }
        .map(|s| s.core.backend_id)
        .unwrap_or("wgpu-blocked");
    env.new_string(id)
        .map(|s| s.into_raw())
        .unwrap_or(std::ptr::null_mut())
}

#[no_mangle]
pub extern "system" fn Java_com_jlj_kuiklybase_holm_HolmRuntimeNative_nativeLastErrorCode(
    env: JNIEnv,
    _class: JClass,
    handle: jlong,
) -> jstring {
    let (code, _) = unsafe { from_handle(handle) }
        .map(|s| s.core.last_error())
        .unwrap_or_default();
    env.new_string(code)
        .map(|s| s.into_raw())
        .unwrap_or(std::ptr::null_mut())
}

#[no_mangle]
pub extern "system" fn Java_com_jlj_kuiklybase_holm_HolmRuntimeNative_nativeLastErrorMessage(
    env: JNIEnv,
    _class: JClass,
    handle: jlong,
) -> jstring {
    let (_, msg) = unsafe { from_handle(handle) }
        .map(|s| s.core.last_error())
        .unwrap_or_default();
    env.new_string(msg)
        .map(|s| s.into_raw())
        .unwrap_or(std::ptr::null_mut())
}

/// 返回 String[]：code, message, code, message, …
#[no_mangle]
pub extern "system" fn Java_com_jlj_kuiklybase_holm_HolmRuntimeNative_nativeDrainErrors<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    handle: jlong,
) -> jni::sys::jobjectArray {
    let empty = || {
        env.new_object_array(0, "java/lang/String", JObject::null())
            .map(|a| a.into_raw())
            .unwrap_or(std::ptr::null_mut())
    };
    let Some(s) = (unsafe { from_handle(handle) }) else {
        return empty();
    };
    let errs = s.core.drain_errors();
    let len = (errs.len() * 2) as i32;
    let Ok(arr) = env.new_object_array(len, "java/lang/String", JObject::null()) else {
        return empty();
    };
    for (i, (code, msg)) in errs.into_iter().enumerate() {
        let Ok(jc) = env.new_string(&code) else {
            continue;
        };
        let Ok(jm) = env.new_string(&msg) else {
            continue;
        };
        let _ = env.set_object_array_element(&arr, (i * 2) as i32, jc);
        let _ = env.set_object_array_element(&arr, (i * 2 + 1) as i32, jm);
    }
    arr.into_raw()
}
