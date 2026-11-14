//! 共享 Runtime Session 核心（H5 wasm / Android native 共用管线与 tick）。

use crate::shaders;

#[repr(C)]
#[derive(Clone, Copy, Debug)]
pub struct FrameUniforms {
    pub time: f32,
    pub liquid_boost: f32,
    pub liquid_bend: f32,
    pub _pad0: f32,
    pub resolution: [f32; 2],
    pub _pad1: [f32; 2],
}

unsafe impl bytemuck::Pod for FrameUniforms {}
unsafe impl bytemuck::Zeroable for FrameUniforms {}

#[derive(Clone, Copy, PartialEq, Eq)]
pub enum SceneKind {
    ClearTri,
    Custom,
}

impl SceneKind {
    pub fn from_name(name: &str) -> Self {
        match name {
            "custom" => Self::Custom,
            _ => Self::ClearTri,
        }
    }
}

pub fn backend_label(backend: wgpu::Backend) -> &'static str {
    match backend {
        wgpu::Backend::BrowserWebGpu => "wgpu-m0",
        wgpu::Backend::Gl => "wgpu-m0-gl",
        wgpu::Backend::Vulkan => "wgpu-m0",
        _ => "wgpu-m0",
    }
}

pub struct HolmRuntimeSessionCore {
    pub device: wgpu::Device,
    pub queue: wgpu::Queue,
    pub surface: wgpu::Surface<'static>,
    pub config: wgpu::SurfaceConfiguration,
    pub uniform_buf: wgpu::Buffer,
    pub bind_group: wgpu::BindGroup,
    pub pipe_layout: wgpu::PipelineLayout,
    pub pipe_clear: wgpu::RenderPipeline,
    pub pipe_custom: Option<wgpu::RenderPipeline>,
    pub scene: SceneKind,
    pub liquid_boost: f32,
    pub liquid_bend: f32,
    pub playing: bool,
    pub time: f32,
    pub backend_id: &'static str,
}

impl HolmRuntimeSessionCore {
    pub async fn create_with_surface(
        instance: wgpu::Instance,
        surface: wgpu::Surface<'static>,
        width: u32,
        height: u32,
        downlevel_limits: bool,
    ) -> Result<Self, String> {
        let adapter = instance
            .request_adapter(&wgpu::RequestAdapterOptions {
                power_preference: wgpu::PowerPreference::HighPerformance,
                compatible_surface: Some(&surface),
                force_fallback_adapter: false,
            })
            .await
            .ok_or_else(|| "no wgpu adapter".to_string())?;

        let backend_id = backend_label(adapter.get_info().backend);

        let required_limits = if downlevel_limits {
            wgpu::Limits::downlevel_webgl2_defaults().using_resolution(adapter.limits())
        } else {
            wgpu::Limits::default().using_resolution(adapter.limits())
        };

        let (device, queue) = adapter
            .request_device(
                &wgpu::DeviceDescriptor {
                    label: Some("holm-runtime-session"),
                    required_features: wgpu::Features::empty(),
                    required_limits,
                    memory_hints: Default::default(),
                },
                None,
            )
            .await
            .map_err(|e| format!("request_device: {e}"))?;

        let caps = surface.get_capabilities(&adapter);
        // 片元按「直接出屏」写色（含 Demo 隧道）；优先非 sRGB，避免 Android Vulkan
        // 再做一次 OETF → 发白发雾。无非 sRGB 时再回落。
        let format = caps
            .formats
            .iter()
            .copied()
            .find(|f| !f.is_srgb())
            .or_else(|| caps.formats.iter().copied().find(|f| f.is_srgb()))
            .or_else(|| caps.formats.first().copied())
            .ok_or_else(|| "no surface formats".to_string())?;

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
            pipe_clear,
            pipe_custom: None,
            scene: SceneKind::ClearTri,
            liquid_boost: 1.35,
            liquid_bend: 0.05,
            playing: true,
            time: 0.0,
            backend_id,
        })
    }

    pub fn build_pipe(
        device: &wgpu::Device,
        pipe_layout: &wgpu::PipelineLayout,
        format: wgpu::TextureFormat,
        label: &str,
        fs: &str,
    ) -> Result<wgpu::RenderPipeline, String> {
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

    pub fn set_scene(&mut self, scene: &str) {
        if self.pipe_custom.is_some() && self.scene == SceneKind::Custom {
            return;
        }
        self.scene = SceneKind::from_name(scene);
        if self.scene == SceneKind::Custom && self.pipe_custom.is_none() {
            self.scene = SceneKind::ClearTri;
        }
    }

    pub fn set_custom_fs(&mut self, fs: &str) -> Result<(), String> {
        let trimmed = fs.trim();
        if trimmed.is_empty() {
            self.pipe_custom = None;
            if self.scene == SceneKind::Custom {
                self.scene = SceneKind::ClearTri;
            }
            return Ok(());
        }
        let pipe = Self::build_pipe(
            &self.device,
            &self.pipe_layout,
            self.config.format,
            "holm-custom",
            trimmed,
        )?;
        self.pipe_custom = Some(pipe);
        self.scene = SceneKind::Custom;
        Ok(())
    }

    pub fn set_liquid(&mut self, boost: f32, bend: f32) {
        self.liquid_boost = boost;
        self.liquid_bend = bend;
    }

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

    pub fn tick(&mut self, dt_secs: f32) -> Result<(), String> {
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
            .map_err(|e| format!("get_current_texture: {e}"))?;
        let view = frame
            .texture
            .create_view(&wgpu::TextureViewDescriptor::default());

        let pipeline = match self.scene {
            SceneKind::ClearTri => &self.pipe_clear,
            SceneKind::Custom => self.pipe_custom.as_ref().unwrap_or(&self.pipe_clear),
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
}
