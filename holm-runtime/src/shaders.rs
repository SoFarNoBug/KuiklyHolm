//! Lite 前三席 WGSL（观感近似，非 GLSL 逐字）。

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

pub const FROST_FS: &str = r#"
fn hash(p: vec2f) -> f32 {
    return fract(sin(dot(p, vec2f(127.1, 311.7))) * 43758.5453);
}

fn scene_color(uv: vec2f) -> vec3f {
    let p = uv + vec2f(u.time * 0.04, u.time * 0.02);
    let stripe = step(0.5, fract(p.x * 6.0 + p.y * 1.5));
    let a = vec3f(0.15, 0.45, 0.95);
    let b = vec3f(0.95, 0.35, 0.25);
    var col = mix(a, b, stripe);
    let d = length(p - vec2f(0.35 + 0.1 * sin(u.time * 0.7), 0.55));
    col = mix(col, vec3f(0.2, 0.9, 0.55), 1.0 - smoothstep(0.12, 0.22, d));
    let d2 = length(p - vec2f(0.72, 0.32 + 0.08 * cos(u.time * 0.5)));
    col = mix(col, vec3f(0.95, 0.85, 0.2), 1.0 - smoothstep(0.08, 0.16, d2));
    return col;
}

// 抖动 UV + 单次 scene（替代 13-tap 模糊，观感近似、GPU 成本对齐 Lite 量级）
fn frost_uv(uv: vec2f) -> vec2f {
    let n = hash(uv * 28.0 + u.time * 0.05) - 0.5;
    let n2 = hash(uv.yx * 33.0 - u.time * 0.03) - 0.5;
    return uv + vec2f(n, n2) * 0.022;
}

@fragment
fn fs_main(in: VsOut) -> @location(0) vec4f {
    let uv = in.uv;
    var frosted = scene_color(frost_uv(uv));
    let n = hash(uv * 40.0 + u.time * 0.1) * 0.06;
    frosted = mix(frosted, vec3f(0.92, 0.95, 1.0), 0.34 + n);
    let edge = smoothstep(1.15, 0.35, length(uv - vec2f(0.5, 0.5)));
    frosted *= 0.82 + 0.22 * edge;
    return vec4f(frosted, 1.0);
}
"#;

pub const GLOW_FS: &str = r#"
@fragment
fn fs_main(in: VsOut) -> @location(0) vec4f {
    let uv = in.uv;
    var col = vec3f(0.04, 0.05, 0.10);

    let c1 = vec2f(0.32 + 0.14 * sin(u.time * 0.55), 0.48 + 0.12 * cos(u.time * 0.40));
    var b1 = 1.0 - smoothstep(0.0, 0.48, length(uv - c1));
    b1 *= 0.82 + 0.18 * sin(u.time * 1.1);
    col += vec3f(0.18, 0.72, 1.0) * (b1 * b1);

    let c2 = vec2f(0.68 + 0.10 * cos(u.time * 0.48), 0.38 + 0.14 * sin(u.time * 0.62));
    var b2 = 1.0 - smoothstep(0.0, 0.42, length(uv - c2));
    b2 *= 0.80 + 0.20 * cos(u.time * 0.95);
    col += vec3f(0.95, 0.28, 0.72) * (b2 * b2);

    let c3 = vec2f(0.52 + 0.08 * sin(u.time * 0.35), 0.72 + 0.08 * cos(u.time * 0.70));
    var b3 = 1.0 - smoothstep(0.0, 0.36, length(uv - c3));
    b3 *= 0.75 + 0.25 * sin(u.time * 0.8 + 1.2);
    col += vec3f(0.95, 0.78, 0.28) * (b3 * b3);

    let edge = smoothstep(1.2, 0.35, length(uv - vec2f(0.5, 0.5)));
    col *= 0.72 + 0.38 * edge;
    return vec4f(col, 1.0);
}
"#;

pub const LIQUID_FS: &str = r#"
fn sd_round_box(p: vec2f, b: vec2f, r: f32) -> f32 {
    let q = abs(p) - b + vec2f(r, r);
    return length(max(q, vec2f(0.0))) + min(max(q.x, q.y), 0.0) - r;
}

fn scene_color(uv: vec2f) -> vec3f {
    let p = uv + vec2f(u.time * 0.035, u.time * 0.018);
    let stripe = smoothstep(0.35, 0.65, fract(p.x * 5.5 + p.y * 1.4));
    let a = vec3f(0.12, 0.42, 0.95);
    let b = vec3f(0.95, 0.32, 0.22);
    var col = mix(a, b, stripe);
    let d = length(p - vec2f(0.35 + 0.1 * sin(u.time * 0.55), 0.55));
    col = mix(col, vec3f(0.15, 0.92, 0.55), 1.0 - smoothstep(0.12, 0.22, d));
    let d2 = length(p - vec2f(0.72, 0.32 + 0.08 * cos(u.time * 0.4)));
    col = mix(col, vec3f(0.95, 0.85, 0.18), 1.0 - smoothstep(0.08, 0.16, d2));
    return col;
}

@fragment
fn fs_main(in: VsOut) -> @location(0) vec4f {
    let uv = in.uv;
    let p = uv - vec2f(0.5, 0.5);
    let glass = sd_round_box(p, vec2f(0.492, 0.472), 0.055);
    let inside = 1.0 - smoothstep(-0.008, 0.012, glass);
    let rim = 1.0 - smoothstep(0.0, 0.055, abs(glass));
    let fresnel = pow(clamp(rim, 0.0, 1.0), 1.6);

    let boost_k = max(u.liquid_boost - 1.0, 0.0);
    let bend_amt = u.liquid_bend * fresnel * (0.85 + 0.65 * boost_k);
    let n_dir = vec2f(p.x * 0.42, p.y);
    let n_len = length(n_dir);
    let n = select(vec2f(0.0), n_dir / n_len, n_len > 0.001);
    let wave = sin(u.time * 0.85 + p.x * 7.0 + p.y * 9.0) * 0.3
        + sin(u.time * 0.45 + p.y * 10.0) * 0.22;
    var offset = n * bend_amt * (0.5 + 0.5 * wave);
    offset.x *= 0.45;

    let sample_uv = uv + offset * inside;
    // 单次 scene + 条件 chroma（去掉 5-tap soft_blur）
    var col = scene_color(sample_uv);
    if (boost_k > 0.05) {
        let chroma = boost_k * 0.022 * fresnel * inside;
        let c_off = vec2f(n.x * 0.45, n.y) * chroma;
        col.r = mix(col.r, scene_color(sample_uv + c_off).r, 0.35);
        col.b = mix(col.b, scene_color(sample_uv - c_off).b, 0.35);
    }

    col = mix(col, vec3f(0.88, 0.92, 0.98), 0.08 * inside + 0.05 * fresnel);
    let sheen = pow(fresnel, 2.4) * (0.18 + 0.22 * boost_k);
    col += vec3f(0.92, 0.96, 1.0) * sheen;
    let spec = pow(max(1.0 - abs(p.x * 0.9 + p.y * 0.55 - 0.05), 0.0), 10.0) * inside * 0.16;
    col += vec3f(1.0) * spec;

    let outside = scene_color(uv) * 0.88;
    col = mix(outside, col, inside);
    return vec4f(col, 1.0);
}
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
