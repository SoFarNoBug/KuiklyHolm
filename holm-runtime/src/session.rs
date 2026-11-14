//! H5 wasm 泵帧 Session：包装 [`crate::session_core`]；HtmlCanvas / OffscreenCanvas。

use crate::session_core::HolmRuntimeSessionCore;
use wasm_bindgen::prelude::*;
use wasm_bindgen::JsCast;
use web_sys::{HtmlCanvasElement, OffscreenCanvas};

#[wasm_bindgen]
pub struct HolmRuntimeSession {
    inner: HolmRuntimeSessionCore,
}

#[wasm_bindgen]
impl HolmRuntimeSession {
    /// 异步创建 Session；`target` 为 `HtmlCanvasElement` 或 `OffscreenCanvas`。
    pub async fn create(target: JsValue) -> Result<HolmRuntimeSession, JsValue> {
        console_error_panic_hook::set_once();
        if let Ok(canvas) = target.clone().dyn_into::<HtmlCanvasElement>() {
            let width = canvas.width().max(1);
            let height = canvas.height().max(1);
            return Self::create_with_surface(
                wgpu::SurfaceTarget::Canvas(canvas),
                width,
                height,
            )
            .await;
        }
        if let Ok(canvas) = target.dyn_into::<OffscreenCanvas>() {
            let width = canvas.width().max(1);
            let height = canvas.height().max(1);
            return Self::create_with_surface(
                wgpu::SurfaceTarget::OffscreenCanvas(canvas),
                width,
                height,
            )
            .await;
        }
        Err(JsValue::from_str(
            "create: target must be HtmlCanvasElement or OffscreenCanvas",
        ))
    }

    async fn create_with_surface(
        target: wgpu::SurfaceTarget<'static>,
        width: u32,
        height: u32,
    ) -> Result<HolmRuntimeSession, JsValue> {
        let instance = wgpu::Instance::new(wgpu::InstanceDescriptor {
            backends: wgpu::Backends::BROWSER_WEBGPU | wgpu::Backends::GL,
            ..Default::default()
        });
        let surface = instance
            .create_surface(target)
            .map_err(|e| JsValue::from_str(&format!("create_surface: {e}")))?;
        let inner = HolmRuntimeSessionCore::create_with_surface(instance, surface, width, height, true)
            .await
            .map_err(|e| JsValue::from_str(&e))?;
        Ok(Self { inner })
    }

    #[wasm_bindgen(js_name = backendId)]
    pub fn backend_id(&self) -> String {
        self.inner.backend_id.to_string()
    }

    #[wasm_bindgen(js_name = setScene)]
    pub fn set_scene(&mut self, scene: &str) {
        self.inner.set_scene(scene);
    }

    /// 业务 WGSL 片元正文（须含 `@fragment fn fs_main`）；空串清除并回 ClearTri。
    #[wasm_bindgen(js_name = setCustomFs)]
    pub fn set_custom_fs(&mut self, fs: &str) -> Result<(), JsValue> {
        self.inner
            .set_custom_fs(fs)
            .map_err(|e| JsValue::from_str(&format!("setCustomFs: {e}")))
    }

    #[wasm_bindgen(js_name = setLiquid)]
    pub fn set_liquid(&mut self, boost: f32, bend: f32) {
        self.inner.set_liquid(boost, bend);
    }

    #[wasm_bindgen(js_name = setPlaying)]
    pub fn set_playing(&mut self, playing: bool) {
        self.inner.set_playing(playing);
    }

    pub fn resize(&mut self, width: u32, height: u32) {
        self.inner.resize(width, height);
    }

    /// 推进一帧；`dt_secs` 为秒。未 playing 时跳过 GPU（保留上一帧）。
    pub fn tick(&mut self, dt_secs: f32) -> Result<(), JsValue> {
        self.inner
            .tick(dt_secs)
            .map_err(|e| JsValue::from_str(&e))
    }

    /// 释放 Session（宿主卸岛时调用）。
    pub fn destroy(self) {
        // drop inner
    }
}
