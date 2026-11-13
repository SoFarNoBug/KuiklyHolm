package com.jlj.kuiklyholmui

/**
 * @CreateDate: 2026/11/13
 * @Author: jlj
 * @Description: Demo 写死霓虹隧道样本 — Lite=Shadertoy 正文；Runtime=WGSL 片元（观感近似）。
 */
object NeonTunnelShaders {
    const val SCENE_CUSTOM: String = "custom"

    /** Shadertoy 正文（库薄壳补 iTime / iResolution / main）。 */
    val LITE_BODY: String = """
const int MAX_STEPS = 60;
const float MAX_DIST = 50.0;
const float SURF_DIST = 0.002;

float tunnelSDF(vec3 p) {
    float twist = sin(p.z * 0.3 + iTime) * 0.5;
    float c = cos(twist);
    float s = sin(twist);
    vec2 q = mat2(c, -s, s, c) * p.xy;
    vec2 aq = abs(q);
    float d = max(aq.x * 0.866025 + aq.y * 0.5, aq.y) - 2.0;
    float rings = sin(p.z * 2.0 + iTime * 2.0) * 0.1;
    d += rings;
    return d;
}

float getDist(vec3 p) {
    return tunnelSDF(p);
}

float rayMarch(vec3 ro, vec3 rd) {
    float dO = 0.0;
    for (int i = 0; i < MAX_STEPS; i++) {
        vec3 p = ro + rd * dO;
        float dS = getDist(p);
        dO += dS;
        if (dO > MAX_DIST || abs(dS) < SURF_DIST) break;
    }
    return dO;
}

vec3 getNormal(vec3 p) {
    float d = getDist(p);
    vec2 e = vec2(0.001, 0.0);
    vec3 n = d - vec3(
        getDist(p - e.xyy),
        getDist(p - e.yxy),
        getDist(p - e.yyx)
    );
    return normalize(n);
}

void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    vec2 uv = (fragCoord - 0.5 * iResolution.xy) / iResolution.y;
    vec3 ro = vec3(0.0, 0.0, -iTime * 5.0);
    vec3 rd = normalize(vec3(uv, 1.0));
    float d = rayMarch(ro, rd);
    vec3 col = vec3(0.0);
    if (d < MAX_DIST) {
        vec3 p = ro + rd * d;
        vec3 n = getNormal(p);
        float stripe = sin(p.z * 3.0 + iTime * 3.0);
        stripe = smoothstep(0.9, 1.0, stripe);
        vec3 neon1 = vec3(1.0, 0.0, 0.8);
        vec3 neon2 = vec3(0.0, 0.8, 1.0);
        float angle = atan(n.y, n.x);
        vec3 glowCol = mix(neon1, neon2, sin(angle * 3.0 + iTime) * 0.5 + 0.5);
        col = vec3(0.05);
        col += glowCol * stripe * 2.0;
        float fresnel = pow(1.0 - abs(dot(n, rd)), 3.0);
        col += glowCol * fresnel * 0.5;
        float fog = 1.0 - exp(-d * 0.08);
        col = mix(col, vec3(0.02, 0.0, 0.05), fog);
    }
    float vignette = 1.0 - length(uv) * 0.5;
    col *= vignette;
    col = col / (1.0 + col);
    // 不在此做 pow(0.4545)：对齐 RUNTIME_FS；GLES/默认 sRGB 再伽马易发白
    fragColor = vec4(col, 1.0);
}
""".trimIndent()

    /**
     * Runtime WGSL 片元（接 FULLSCREEN_VS + FrameUniforms；入口 fs_main）。
     * 同隧道 raymarch / 霓虹意图，非 GLSL 逐字。
     */
    val RUNTIME_FS: String = """
const MAX_STEPS: i32 = 60;
const MAX_DIST: f32 = 50.0;
const SURF_DIST: f32 = 0.002;

fn tunnel_sdf(p: vec3f) -> f32 {
    let twist = sin(p.z * 0.3 + u.time) * 0.5;
    let c = cos(twist);
    let s = sin(twist);
    let q = vec2f(c * p.x - s * p.y, s * p.x + c * p.y);
    let aq = abs(q);
    var d = max(aq.x * 0.866025 + aq.y * 0.5, aq.y) - 2.0;
    d = d + sin(p.z * 2.0 + u.time * 2.0) * 0.1;
    return d;
}

fn ray_march(ro: vec3f, rd: vec3f) -> f32 {
    var d_o = 0.0;
    for (var i = 0; i < MAX_STEPS; i = i + 1) {
        let p = ro + rd * d_o;
        let d_s = tunnel_sdf(p);
        d_o = d_o + d_s;
        if (d_o > MAX_DIST || abs(d_s) < SURF_DIST) {
            break;
        }
    }
    return d_o;
}

fn get_normal(p: vec3f) -> vec3f {
    let d = tunnel_sdf(p);
    let e = vec2f(0.001, 0.0);
    let n = vec3f(
        d - tunnel_sdf(p - vec3f(e.x, e.y, e.y)),
        d - tunnel_sdf(p - vec3f(e.y, e.x, e.y)),
        d - tunnel_sdf(p - vec3f(e.y, e.y, e.x)),
    );
    return normalize(n);
}

@fragment
fn fs_main(in: VsOut) -> @location(0) vec4f {
    let res = max(u.resolution, vec2f(1.0, 1.0));
    let frag = in.uv * res;
    let uv = (frag - 0.5 * res) / res.y;
    let ro = vec3f(0.0, 0.0, -u.time * 5.0);
    let rd = normalize(vec3f(uv, 1.0));
    let d = ray_march(ro, rd);
    var col = vec3f(0.0);
    if (d < MAX_DIST) {
        let p = ro + rd * d;
        let n = get_normal(p);
        var stripe = sin(p.z * 3.0 + u.time * 3.0);
        stripe = smoothstep(0.9, 1.0, stripe);
        let neon1 = vec3f(1.0, 0.0, 0.8);
        let neon2 = vec3f(0.0, 0.8, 1.0);
        let angle = atan2(n.y, n.x);
        let glow_col = mix(neon1, neon2, sin(angle * 3.0 + u.time) * 0.5 + 0.5);
        col = vec3f(0.05);
        col = col + glow_col * stripe * 2.0;
        let fresnel = pow(1.0 - abs(dot(n, rd)), 3.0);
        col = col + glow_col * fresnel * 0.5;
        let fog = 1.0 - exp(-d * 0.08);
        col = mix(col, vec3f(0.02, 0.0, 0.05), fog);
    }
    let vignette = 1.0 - length(uv) * 0.5;
    col = col * vignette;
    col = col / (1.0 + col);
    // 不在此做 pow(0.4545)：Session 优先非 sRGB Surface；再伽马会与 frost/glow 不一致且易发白
    return vec4f(col, 1.0);
}
""".trimIndent()
}
