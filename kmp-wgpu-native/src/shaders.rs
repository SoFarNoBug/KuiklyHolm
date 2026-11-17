//! Runtime 内置 WGSL：全屏 VS + 清屏占位；业务片元由宿主 setCustomFs 提供。

pub const FULLSCREEN_VS: &str = r#"
struct VsOut {
    @builtin(position) pos: vec4f,
    @location(0) uv: vec2f,
}

@vertex
fn vs_main(@builtin(vertex_index) vi: u32) -> VsOut {
    var p = array<vec2f, 3>(
        vec2f(-1.0, -1.0),
        vec2f( 3.0, -1.0),
        vec2f(-1.0,  3.0),
    );
    var out: VsOut;
    out.pos = vec4f(p[vi], 0.0, 1.0);
    out.uv = p[vi] * 0.5 + vec2f(0.5, 0.5);
    return out;
}
"#;

/// u_time, liquid_boost, liquid_bend, _pad, resolution.xy, _pad2.xy
pub const FRAME_UNIFORMS_WGSL: &str = r#"
struct FrameUniforms {
    time: f32,
    liquid_boost: f32,
    liquid_bend: f32,
    _pad0: f32,
    resolution: vec2f,
    _pad1: vec2f,
}

@group(0) @binding(0) var<uniform> u: FrameUniforms;
"#;

pub const CLEAR_TRI_FS: &str = r#"
@fragment
fn fs_main(in: VsOut) -> @location(0) vec4f {
    let uv = in.uv;
    let p = uv * 2.0 - vec2f(1.0, 1.0);
    // 深蓝底 + 琥珀三角（debug / fallback）
    var col = vec3f(0.08, 0.12, 0.28);
    let d1 = (p.y - (-0.55)) / (0.65 - (-0.55));
    let left = mix(-0.7, 0.0, d1);
    let right = mix(0.7, 0.0, d1);
    let in_tri = select(0.0, 1.0, p.y >= -0.55 && p.y <= 0.65 && p.x >= left && p.x <= right);
    col = mix(col, vec3f(0.98, 0.78, 0.18), in_tri);
    return vec4f(col, 1.0);
}
"#;

pub fn module_source(fs: &str) -> String {
    format!("{}\n{}\n{}", FULLSCREEN_VS, FRAME_UNIFORMS_WGSL, fs)
}
