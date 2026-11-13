//! H5 wasm 泵帧 Session：Device/Queue 各一份；按 scene 切 pipeline。
//! 支持 HtmlCanvasElement / OffscreenCanvas（Worker 路径）。

use crate::shaders;
use wasm_bindgen::prelude::*;
use wasm_bindgen::JsCast;
use web_sys::{HtmlCanvasElement, OffscreenCanvas};

fn backend_label(backend: wgpu::Backend) -> &'static str {
    match backend {
        wgpu::Backend::BrowserWebGpu => "wgpu-m0",
        wgpu::Backend::Gl => "wgpu-m0-gl",
        _ => "wgpu-m0",
    }
}

#[repr(C)]
#[derive(Clone, Copy, Debug)]
struct FrameUniforms {
    time: f32,
    liquid_boost: f32,
    liquid_bend: f32,
    _pad0: f32,
    resolution: [f32; 2],
    _pad1: [f32; 2],
}

unsafe impl bytemuck::Pod for FrameUniforms {}
unsafe impl bytemuck::Zeroable for FrameUniforms {}

#[derive(Clone, Copy, PartialEq, Eq)]
enum SceneKind {
    Frost,
    Glow,
    LiquidApprox,
    ClearTri,
    Custom,
}

impl SceneKind {
    fn from_name(name: &str) -> Self {
        match name {
            "glow" => Self::Glow,
            "liquid_approx" => Self::LiquidApprox,
            "clear_tri" => Self::ClearTri,
            "custom" => Self::Custom,
            _ => Self::Frost, // 未知回退 frost（与 Lite 路由一致）
        }
    }
}

#[wasm_bindgen]
pub struct HolmRuntimeSession {
    device: wgpu::Device,
    queue: wgpu::Queue,
    surface: wgpu::Surface<'static>,
    config: wgpu::SurfaceConfiguration,
    uniform_buf: wgpu::Buffer,
    bind_group: wgpu::BindGroup,
    pipe_layout: wgpu::PipelineLayout,
    pipe_frost: wgpu::RenderPipeline,
    pipe_glow: wgpu::RenderPipeline,
    pipe_liquid: wgpu::RenderPipeline,
    pipe_clear: wgpu::RenderPipeline,
    pipe_custom: Option<wgpu::RenderPipeline>,
    scene: SceneKind,
    liquid_boost: f32,
    liquid_bend: f32,
    playing: bool,
    time: f32,
    backend_id: &'static str,
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

        let adapter = instance
            .request_adapter(&wgpu::RequestAdapterOptions {
                power_preference: wgpu::PowerPreference::HighPerformance,
                compatible_surface: Some(&surface),
                force_fallback_adapter: false,
            })
            .await
            .ok_or_else(|| JsValue::from_str("no wgpu adapter"))?;

        let backend_id = backend_label(adapter.get_info().backend);

        let (device, queue) = adapter
            .request_device(
                &wgpu::DeviceDescriptor {
                    label: Some("holm-runtime-session"),
                    required_features: wgpu::Features::empty(),
                    required_limits: wgpu::Limits::downlevel_webgl2_defaults()
                        .using_resolution(adapter.limits()),
                    memory_hints: Default::default(),
                },
                None,
            )
            .await
            .map_err(|e| JsValue::from_str(&format!("request_device: {e}")))?;

        let caps = surface.get_capabilities(&adapter);
        let format = caps
            .formats
            .iter()
            .copied()
            .find(|f| f.is_srgb())
            .or_else(|| caps.formats.first().copied())
            .ok_or_else(|| JsValue::from_str("no surface formats"))?;

        // AutoNoVsync：Web 上按 Immediate→Mailbox→Fifo 回退，避免 Fifo 把 uncapped/高档卡在低屏刷
        let present_mode = if caps
            .present_modes
            .iter()
            .any(|m| *m == wgpu::PresentMode::AutoNoVsync)
        {
            wgpu::PresentMode::AutoNoVsync
        } else {
            caps.present_modes
                .iter()
                .copied()
                .find(|m| {
                    matches!(
                        *m,
                        wgpu::PresentMode::Immediate
                            | wgpu::PresentMode::Mailbox
                            | wgpu::PresentMode::FifoRelaxed
                    )
                })
                .or_else(|| caps.present_modes.first().copied())
                .unwrap_or(wgpu::PresentMode::Fifo)
        };

        let config = wgpu::SurfaceConfiguration {
            usage: wgpu::TextureUsages::RENDER_ATTACHMENT,
            format,
            width: width.max(1),
            height: height.max(1),
            present_mode,
            alpha_mode: caps
                .alpha_modes
                .first()
                .copied()
                .unwrap_or(wgpu::CompositeAlphaMode::Auto),
            view_formats: vec![],
            // 降低排队延迟，利于 uncapped / 高刷新
            desired_maximum_frame_latency: 1,
        };
        surface.configure(&device, &config);

        let uniform_buf = device.create_buffer(&wgpu::BufferDescriptor {
            label: Some("holm-frame-uniforms"),
            size: std::mem::size_of::<FrameUniforms>() as u64,
            usage: wgpu::BufferUsages::UNIFORM | wgpu::BufferUsages::COPY_DST,
            mapped_at_creation: false,
        });

        let bind_layout = device.create_bind_group_layout(&wgpu::BindGroupLayoutDescriptor {
            label: Some("holm-frame-bgl"),
            entries: &[wgpu::BindGroupLayoutEntry {
                binding: 0,
                visibility: wgpu::ShaderStages::FRAGMENT,
                ty: wgpu::BindingType::Buffer {
                    ty: wgpu::BufferBindingType::Uniform,
                    has_dynamic_offset: false,
                    min_binding_size: None,
                },
                count: None,
            }],
        });
        let bind_group = device.create_bind_group(&wgpu::BindGroupDescriptor {
            label: Some("holm-frame-bg"),
            layout: &bind_layout,
            entries: &[wgpu::BindGroupEntry {
                binding: 0,
                resource: uniform_buf.as_entire_binding(),
            }],
        });
        let pipe_layout = device.create_pipeline_layout(&wgpu::PipelineLayoutDescriptor {
            label: Some("holm-pipe-layout"),
            bind_group_layouts: &[&bind_layout],
            push_constant_ranges: &[],
        });

        let pipe_frost =
            Self::build_pipe(&device, &pipe_layout, format, "holm-frost", shaders::FROST_FS)?;
        let pipe_glow =
            Self::build_pipe(&device, &pipe_layout, format, "holm-glow", shaders::GLOW_FS)?;
        let pipe_liquid =
            Self::build_pipe(&device, &pipe_layout, format, "holm-liquid", shaders::LIQUID_FS)?;
        let pipe_clear = Self::build_pipe(
            &device,
            &pipe_layout,
            format,
            "holm-clear-tri",
            shaders::CLEAR_TRI_FS,
        )?;

        Ok(Self {
            device,
            queue,
            surface,
            config,
            uniform_buf,
            bind_group,
            pipe_layout,
            pipe_frost,
            pipe_glow,
            pipe_liquid,
            pipe_clear,
            pipe_custom: None,
            scene: SceneKind::Frost,
            liquid_boost: 1.35,
            liquid_bend: 0.05,
            playing: true,
            time: 0.0,
            backend_id,
        })
    }

    fn build_pipe(
        device: &wgpu::Device,
        pipe_layout: &wgpu::PipelineLayout,
        format: wgpu::TextureFormat,
        label: &str,
        fs: &str,
    ) -> Result<wgpu::RenderPipeline, JsValue> {
        let src = shaders::module_source(fs);
        let module = device.create_shader_module(wgpu::ShaderModuleDescriptor {
            label: Some(label),
            source: wgpu::ShaderSource::Wgsl(src.into()),
        });
        Ok(device.create_render_pipeline(&wgpu::RenderPipelineDescriptor {
            label: Some(label),
            layout: Some(pipe_layout),
            vertex: wgpu::VertexState {
                module: &module,
                entry_point: Some("vs_main"),
                compilation_options: Default::default(),
                buffers: &[],
            },
            fragment: Some(wgpu::FragmentState {
                module: &module,
                entry_point: Some("fs_main"),
                compilation_options: Default::default(),
                targets: &[Some(wgpu::ColorTargetState {
                    format,
                    blend: Some(wgpu::BlendState::REPLACE),
                    write_mask: wgpu::ColorWrites::ALL,
                })],
            }),
            primitive: wgpu::PrimitiveState::default(),
            depth_stencil: None,
            multisample: wgpu::MultisampleState::default(),
            multiview: None,
            cache: None,
        }))
    }

    #[wasm_bindgen(js_name = backendId)]
    pub fn backend_id(&self) -> String {
        self.backend_id.to_string()
    }

    #[wasm_bindgen(js_name = setScene)]
    pub fn set_scene(&mut self, scene: &str) {
        // 有自定义管线时 setScene 不拆 custom；清自定义请 setCustomFs("")
        if self.pipe_custom.is_some() && self.scene == SceneKind::Custom {
            return;
        }
        self.scene = SceneKind::from_name(scene);
        if self.scene == SceneKind::Custom && self.pipe_custom.is_none() {
            self.scene = SceneKind::Frost;
        }
    }

    /// 业务 WGSL 片元正文（须含 `@fragment fn fs_main`）；空串清除并回 frost。
    #[wasm_bindgen(js_name = setCustomFs)]
    pub fn set_custom_fs(&mut self, fs: &str) -> Result<(), JsValue> {
        let trimmed = fs.trim();
        if trimmed.is_empty() {
            self.pipe_custom = None;
            if self.scene == SceneKind::Custom {
                self.scene = SceneKind::Frost;
            }
            return Ok(());
        }
        let pipe = Self::build_pipe(
            &self.device,
            &self.pipe_layout,
            self.config.format,
            "holm-custom",
            trimmed,
        )
        .map_err(|e| JsValue::from_str(&format!("setCustomFs: {e:?}")))?;
        self.pipe_custom = Some(pipe);
        self.scene = SceneKind::Custom;
        Ok(())
    }

    #[wasm_bindgen(js_name = setLiquid)]
    pub fn set_liquid(&mut self, boost: f32, bend: f32) {
        self.liquid_boost = boost;
        self.liquid_bend = bend;
    }

    #[wasm_bindgen(js_name = setPlaying)]
    pub fn set_playing(&mut self, playing: bool) {
        self.playing = playing;
    }

    pub fn resize(&mut self, width: u32, height: u32) {
        let w = width.max(1);
        let h = height.max(1);
        if self.config.width == w && self.config.height == h {
            return;
        }
        self.config.width = w;
        self.config.height = h;
        self.surface.configure(&self.device, &self.config);
    }

    /// 推进一帧；`dt_secs` 为秒。未 playing 时跳过 GPU（保留上一帧）。
    pub fn tick(&mut self, dt_secs: f32) -> Result<(), JsValue> {
        if !self.playing {
            return Ok(());
        }
        self.time += dt_secs.max(0.0);
        let uniforms = FrameUniforms {
            time: self.time,
            liquid_boost: self.liquid_boost,
            liquid_bend: self.liquid_bend,
            _pad0: 0.0,
            resolution: [self.config.width as f32, self.config.height as f32],
            _pad1: [0.0, 0.0],
        };
        self.queue
            .write_buffer(&self.uniform_buf, 0, bytemuck::bytes_of(&uniforms));

        let frame = self
            .surface
            .get_current_texture()
            .map_err(|e| JsValue::from_str(&format!("get_current_texture: {e}")))?;
        let view = frame
            .texture
            .create_view(&wgpu::TextureViewDescriptor::default());

        let pipeline = match self.scene {
            SceneKind::Frost => &self.pipe_frost,
            SceneKind::Glow => &self.pipe_glow,
            SceneKind::LiquidApprox => &self.pipe_liquid,
            SceneKind::ClearTri => &self.pipe_clear,
            SceneKind::Custom => self
                .pipe_custom
                .as_ref()
                .unwrap_or(&self.pipe_clear),
        };

        let mut encoder = self
            .device
            .create_command_encoder(&wgpu::CommandEncoderDescriptor {
                label: Some("holm-tick"),
            });
        {
            let mut pass = encoder.begin_render_pass(&wgpu::RenderPassDescriptor {
                label: Some("holm-tick-pass"),
                color_attachments: &[Some(wgpu::RenderPassColorAttachment {
                    view: &view,
                    resolve_target: None,
                    ops: wgpu::Operations {
                        load: wgpu::LoadOp::Clear(wgpu::Color {
                            r: 0.05,
                            g: 0.06,
                            b: 0.1,
                            a: 1.0,
                        }),
                        store: wgpu::StoreOp::Store,
                    },
                })],
                depth_stencil_attachment: None,
                timestamp_writes: None,
                occlusion_query_set: None,
            });
            pass.set_pipeline(pipeline);
            pass.set_bind_group(0, &self.bind_group, &[]);
            pass.draw(0..3, 0..1);
        }
        self.queue.submit(Some(encoder.finish()));
        frame.present();
        Ok(())
    }

    /// 释放 Session（宿主卸岛时调用）。
    pub fn destroy(self) {
        // drop fields
    }
}
