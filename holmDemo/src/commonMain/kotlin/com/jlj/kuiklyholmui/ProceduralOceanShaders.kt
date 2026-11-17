package com.jlj.kuiklyholmui

/**
 * @CreateDate: 2026/11/15
 * @Author: jlj
 * @Description: afl_ext「程序化海洋」Demo 移植（MIT）。
 * Lite=Shadertoy body；Runtime=WGSL 同构。
 *
 * 版权（须随副本保留）：
 * The MIT License — Copyright © 2017-2024 afl_ext
 * 作者：afl_ext；出处：shadertoy.com（view/MdXyzX）
 * 全文 permission notice 见 [LITE_BODY] / [RUNTIME_FS] 注释头。
 *
 * 相对原版改动（GLES2/WebGL1 + Holm 壳）：
 * - 仅 iTime/iResolution；iMouse 桩为 0（走默认视角）
 * - renderer-gl / renderer-wgpu 波次对齐：raymarch 12、normal 24（原版 normal 36，双渲染后端同降一档）
 * - ACES 去掉末尾 pow(1/2.2) 伽马（对齐 NeonTunnel / Holm 输出约定）
 */
object ProceduralOceanShaders {
    /** Demo 一行小字：作者 + 出处（MIT 全文在 shader 注释头）。 */
    const val UI_CREDIT: String = "作者 afl_ext · 出处 shadertoy.com · MIT"

    /**
     * Shadertoy 正文。与 Runtime 同档迭代；弱端可能掉帧，真机再定是否另降 Lite。
     */
    val GL_BODY: String = """
// The MIT License
// Copyright © 2017-2024 afl_ext
// Permission is hereby granted, free of charge, to any person obtaining a copy of
// this software and associated documentation files (the "Software"), to deal in
// the Software without restriction, including without limitation the rights to
// use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
// of the Software, and to permit persons to whom the Software is furnished to do
// so, subject to the following conditions: The above copyright notice and this
// permission notice shall be included in all copies or substantial portions of
// the Software. THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
// EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO
// EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES
// OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
// ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
// DEALINGS IN THE SOFTWARE.
// Author: afl_ext · Source: shadertoy.com (https://www.shadertoy.com/view/MdXyzX)
// Holm Lite port: same iters as Runtime; iMouse stubbed; ACES without display gamma.

#define DRAG_MULT 0.38
#define WATER_DEPTH 1.0
#define CAMERA_HEIGHT 1.5
#define ITERATIONS_RAYMARCH 12
#define ITERATIONS_NORMAL 24
#define RAYMARCH_STEPS 64
#define iMouse vec4(0.0)
#define NormalizedMouse (iMouse.xy / iResolution.xy)

vec2 wavedx(vec2 position, vec2 direction, float frequency, float timeshift) {
    float x = dot(direction, position) * frequency + timeshift;
    float wave = exp(sin(x) - 1.0);
    float dx = wave * cos(x);
    return vec2(wave, -dx);
}

// GLES2：循环上界须常量；分 raymarch / normal 两档，勿传 int 参数当界。
float getwaves_rm(vec2 position) {
    float wavePhaseShift = length(position) * 0.1;
    float iter = 0.0;
    float frequency = 1.0;
    float timeMultiplier = 2.0;
    float weight = 1.0;
    float sumOfValues = 0.0;
    float sumOfWeights = 0.0;
    for (int i = 0; i < ITERATIONS_RAYMARCH; i++) {
        vec2 p = vec2(sin(iter), cos(iter));
        vec2 res = wavedx(position, p, frequency, iTime * timeMultiplier + wavePhaseShift);
        position += p * res.y * weight * DRAG_MULT;
        sumOfValues += res.x * weight;
        sumOfWeights += weight;
        weight = mix(weight, 0.0, 0.2);
        frequency *= 1.18;
        timeMultiplier *= 1.07;
        iter += 1232.399963;
    }
    return sumOfValues / sumOfWeights;
}

float getwaves_n(vec2 position) {
    float wavePhaseShift = length(position) * 0.1;
    float iter = 0.0;
    float frequency = 1.0;
    float timeMultiplier = 2.0;
    float weight = 1.0;
    float sumOfValues = 0.0;
    float sumOfWeights = 0.0;
    for (int i = 0; i < ITERATIONS_NORMAL; i++) {
        vec2 p = vec2(sin(iter), cos(iter));
        vec2 res = wavedx(position, p, frequency, iTime * timeMultiplier + wavePhaseShift);
        position += p * res.y * weight * DRAG_MULT;
        sumOfValues += res.x * weight;
        sumOfWeights += weight;
        weight = mix(weight, 0.0, 0.2);
        frequency *= 1.18;
        timeMultiplier *= 1.07;
        iter += 1232.399963;
    }
    return sumOfValues / sumOfWeights;
}

float raymarchwater(vec3 camera, vec3 start, vec3 end, float depth) {
    vec3 pos = start;
    vec3 dir = normalize(end - start);
    for (int i = 0; i < RAYMARCH_STEPS; i++) {
        float height = getwaves_rm(pos.xz) * depth - depth;
        if (height + 0.01 > pos.y) {
            return distance(pos, camera);
        }
        pos += dir * (pos.y - height);
    }
    return distance(start, camera);
}

vec3 normal(vec2 pos, float e, float depth) {
    vec2 ex = vec2(e, 0.0);
    float H = getwaves_n(pos.xy) * depth;
    vec3 a = vec3(pos.x, H, pos.y);
    return normalize(
        cross(
            a - vec3(pos.x - e, getwaves_n(pos.xy - ex.xy) * depth, pos.y),
            a - vec3(pos.x, getwaves_n(pos.xy + ex.yx) * depth, pos.y + e)
        )
    );
}

mat3 createRotationMatrixAxisAngle(vec3 axis, float angle) {
    float s = sin(angle);
    float c = cos(angle);
    float oc = 1.0 - c;
    return mat3(
        oc * axis.x * axis.x + c, oc * axis.x * axis.y - axis.z * s, oc * axis.z * axis.x + axis.y * s,
        oc * axis.x * axis.y + axis.z * s, oc * axis.y * axis.y + c, oc * axis.y * axis.z - axis.x * s,
        oc * axis.z * axis.x - axis.y * s, oc * axis.y * axis.z + axis.x * s, oc * axis.z * axis.z + c
    );
}

vec3 getRay(vec2 fragCoord) {
    vec2 uv = ((fragCoord.xy / iResolution.xy) * 2.0 - 1.0) * vec2(iResolution.x / iResolution.y, 1.0);
    vec3 proj = normalize(vec3(uv.x, uv.y, 1.5));
    if (iResolution.x < 600.0) {
        return proj;
    }
    float mx = (NormalizedMouse.x + 0.5) * 2.0 - 1.0;
    float my = NormalizedMouse.y == 0.0 ? 0.27 : NormalizedMouse.y;
    return createRotationMatrixAxisAngle(vec3(0.0, -1.0, 0.0), 3.0 * mx)
        * createRotationMatrixAxisAngle(vec3(1.0, 0.0, 0.0), 0.5 + 1.5 * (my * 2.0 - 1.0))
        * proj;
}

float intersectPlane(vec3 origin, vec3 direction, vec3 point, vec3 normal) {
    return clamp(dot(point - origin, normal) / dot(direction, normal), -1.0, 9991999.0);
}

vec3 extra_cheap_atmosphere(vec3 raydir, vec3 sundir) {
    float special_trick = 1.0 / (raydir.y * 1.0 + 0.1);
    float special_trick2 = 1.0 / (sundir.y * 11.0 + 1.0);
    float raysundt = pow(abs(dot(sundir, raydir)), 2.0);
    vec3 suncolor = mix(vec3(1.0), max(vec3(0.0), vec3(1.0) - vec3(5.5, 13.0, 22.4) / 22.4), special_trick2);
    vec3 bluesky = vec3(5.5, 13.0, 22.4) / 22.4 * suncolor;
    vec3 bluesky2 = max(vec3(0.0), bluesky - vec3(5.5, 13.0, 22.4) * 0.002 * (special_trick + -6.0 * sundir.y * sundir.y));
    bluesky2 *= special_trick * (0.24 + raysundt * 0.24);
    return bluesky2 * (1.0 + 1.0 * pow(1.0 - raydir.y, 3.0));
}

vec3 getSunDirection() {
    return normalize(vec3(-0.0773502691896258, 0.5 + sin(iTime * 0.2 + 2.6) * 0.45, 0.5773502691896258));
}

vec3 getAtmosphere(vec3 dir) {
    return extra_cheap_atmosphere(dir, getSunDirection()) * 0.5;
}

float getSun(vec3 dir) {
    return pow(max(0.0, dot(dir, getSunDirection())), 720.0) * 210.0;
}

vec3 aces_tonemap(vec3 color) {
    mat3 m1 = mat3(
        0.59719, 0.07600, 0.02840,
        0.35458, 0.90834, 0.13383,
        0.04823, 0.01566, 0.83777
    );
    mat3 m2 = mat3(
        1.60475, -0.10208, -0.00327,
        -0.53108, 1.10813, -0.07276,
        -0.07367, -0.00605, 1.07602
    );
    vec3 v = m1 * color;
    vec3 a = v * (v + 0.0245786) - 0.000090537;
    vec3 b = v * (0.983729 * v + 0.4329510) + 0.238081;
    // 无 display gamma：Holm Surface 再伽马易发白
    return clamp(m2 * (a / b), 0.0, 1.0);
}

void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    vec3 ray = getRay(fragCoord);
    if (ray.y >= 0.0) {
        vec3 C = getAtmosphere(ray) + getSun(ray);
        fragColor = vec4(aces_tonemap(C * 2.0), 1.0);
        return;
    }
    vec3 waterPlaneHigh = vec3(0.0, 0.0, 0.0);
    vec3 waterPlaneLow = vec3(0.0, -WATER_DEPTH, 0.0);
    vec3 origin = vec3(iTime * 0.2, CAMERA_HEIGHT, 1.0);
    float highPlaneHit = intersectPlane(origin, ray, waterPlaneHigh, vec3(0.0, 1.0, 0.0));
    float lowPlaneHit = intersectPlane(origin, ray, waterPlaneLow, vec3(0.0, 1.0, 0.0));
    vec3 highHitPos = origin + ray * highPlaneHit;
    vec3 lowHitPos = origin + ray * lowPlaneHit;
    float dist = raymarchwater(origin, highHitPos, lowHitPos, WATER_DEPTH);
    vec3 waterHitPos = origin + ray * dist;
    vec3 N = normal(waterHitPos.xz, 0.01, WATER_DEPTH);
    N = mix(N, vec3(0.0, 1.0, 0.0), 0.8 * min(1.0, sqrt(dist * 0.01) * 1.1));
    float fresnel = (0.04 + (1.0 - 0.04) * (pow(1.0 - max(0.0, dot(-N, ray)), 5.0)));
    vec3 R = normalize(reflect(ray, N));
    R.y = abs(R.y);
    vec3 reflection = getAtmosphere(R) + getSun(R);
    vec3 scattering = vec3(0.0293, 0.0698, 0.1717) * 0.1 * (0.2 + (waterHitPos.y + WATER_DEPTH) / WATER_DEPTH);
    vec3 C = fresnel * reflection + scattering;
    fragColor = vec4(aces_tonemap(C * 2.0), 1.0);
}
""".trimIndent()

    /**
     * WGSL 同构；与 Lite 同档（raymarch 12 / normal 24；原版 normal 36）。
     */
    val WGPU_FS: String = """
// The MIT License
// Copyright © 2017-2024 afl_ext
// Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions: The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software. THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
// Author: afl_ext · Source: shadertoy.com (https://www.shadertoy.com/view/MdXyzX)
// Holm Runtime WGSL port; ACES without display gamma; fixed default look (no mouse).

const DRAG_MULT: f32 = 0.38;
const WATER_DEPTH: f32 = 1.0;
const CAMERA_HEIGHT: f32 = 1.5;
const ITERATIONS_RAYMARCH: i32 = 12;
const ITERATIONS_NORMAL: i32 = 24;
const RAYMARCH_STEPS: i32 = 64;

fn wavedx(position: vec2f, direction: vec2f, frequency: f32, timeshift: f32) -> vec2f {
    let x = dot(direction, position) * frequency + timeshift;
    let wave = exp(sin(x) - 1.0);
    let dx = wave * cos(x);
    return vec2f(wave, -dx);
}

fn getwaves(position0: vec2f, iterations: i32) -> f32 {
    var position = position0;
    let wave_phase_shift = length(position) * 0.1;
    var iter = 0.0;
    var frequency = 1.0;
    var time_multiplier = 2.0;
    var weight = 1.0;
    var sum_of_values = 0.0;
    var sum_of_weights = 0.0;
    for (var i = 0; i < iterations; i = i + 1) {
        let p = vec2f(sin(iter), cos(iter));
        let res = wavedx(position, p, frequency, u.time * time_multiplier + wave_phase_shift);
        position = position + p * res.y * weight * DRAG_MULT;
        sum_of_values = sum_of_values + res.x * weight;
        sum_of_weights = sum_of_weights + weight;
        weight = mix(weight, 0.0, 0.2);
        frequency = frequency * 1.18;
        time_multiplier = time_multiplier * 1.07;
        iter = iter + 1232.399963;
    }
    return sum_of_values / sum_of_weights;
}

fn raymarchwater(camera: vec3f, start: vec3f, end: vec3f, depth: f32) -> f32 {
    var pos = start;
    let dir = normalize(end - start);
    for (var i = 0; i < RAYMARCH_STEPS; i = i + 1) {
        let height = getwaves(pos.xz, ITERATIONS_RAYMARCH) * depth - depth;
        if (height + 0.01 > pos.y) {
            return distance(pos, camera);
        }
        pos = pos + dir * (pos.y - height);
    }
    return distance(start, camera);
}

fn water_normal(pos: vec2f, e: f32, depth: f32) -> vec3f {
    let ex = vec2f(e, 0.0);
    let h = getwaves(pos.xy, ITERATIONS_NORMAL) * depth;
    let a = vec3f(pos.x, h, pos.y);
    return normalize(
        cross(
            a - vec3f(pos.x - e, getwaves(pos.xy - ex.xy, ITERATIONS_NORMAL) * depth, pos.y),
            a - vec3f(pos.x, getwaves(pos.xy + ex.yx, ITERATIONS_NORMAL) * depth, pos.y + e),
        )
    );
}

fn create_rotation_matrix_axis_angle(axis: vec3f, angle: f32) -> mat3x3f {
    let s = sin(angle);
    let c = cos(angle);
    let oc = 1.0 - c;
    return mat3x3f(
        vec3f(oc * axis.x * axis.x + c, oc * axis.x * axis.y - axis.z * s, oc * axis.z * axis.x + axis.y * s),
        vec3f(oc * axis.x * axis.y + axis.z * s, oc * axis.y * axis.y + c, oc * axis.y * axis.z - axis.x * s),
        vec3f(oc * axis.z * axis.x - axis.y * s, oc * axis.y * axis.z + axis.x * s, oc * axis.z * axis.z + c),
    );
}

fn get_ray(frag_coord: vec2f, res: vec2f) -> vec3f {
    let uv = ((frag_coord / res) * 2.0 - 1.0) * vec2f(res.x / res.y, 1.0);
    let proj = normalize(vec3f(uv.x, uv.y, 1.5));
    if (res.x < 600.0) {
        return proj;
    }
    // 固定默认视角（原版 NormalizedMouse.y==0 → 0.27）
    let mx = 0.0;
    let my = 0.27;
    return create_rotation_matrix_axis_angle(vec3f(0.0, -1.0, 0.0), 3.0 * mx)
        * create_rotation_matrix_axis_angle(vec3f(1.0, 0.0, 0.0), 0.5 + 1.5 * (my * 2.0 - 1.0))
        * proj;
}

fn intersect_plane(origin: vec3f, direction: vec3f, point: vec3f, normal: vec3f) -> f32 {
    return clamp(dot(point - origin, normal) / dot(direction, normal), -1.0, 9991999.0);
}

fn extra_cheap_atmosphere(raydir: vec3f, sundir: vec3f) -> vec3f {
    let special_trick = 1.0 / (raydir.y * 1.0 + 0.1);
    let special_trick2 = 1.0 / (sundir.y * 11.0 + 1.0);
    let raysundt = pow(abs(dot(sundir, raydir)), 2.0);
    let suncolor = mix(vec3f(1.0), max(vec3f(0.0), vec3f(1.0) - vec3f(5.5, 13.0, 22.4) / 22.4), special_trick2);
    let bluesky = vec3f(5.5, 13.0, 22.4) / 22.4 * suncolor;
    var bluesky2 = max(vec3f(0.0), bluesky - vec3f(5.5, 13.0, 22.4) * 0.002 * (special_trick + -6.0 * sundir.y * sundir.y));
    bluesky2 = bluesky2 * special_trick * (0.24 + raysundt * 0.24);
    return bluesky2 * (1.0 + 1.0 * pow(1.0 - raydir.y, 3.0));
}

fn get_sun_direction() -> vec3f {
    return normalize(vec3f(-0.0773502691896258, 0.5 + sin(u.time * 0.2 + 2.6) * 0.45, 0.5773502691896258));
}

fn get_atmosphere(dir: vec3f) -> vec3f {
    return extra_cheap_atmosphere(dir, get_sun_direction()) * 0.5;
}

fn get_sun(dir: vec3f) -> f32 {
    return pow(max(0.0, dot(dir, get_sun_direction())), 720.0) * 210.0;
}

fn aces_tonemap(color: vec3f) -> vec3f {
    let m1 = mat3x3f(
        vec3f(0.59719, 0.07600, 0.02840),
        vec3f(0.35458, 0.90834, 0.13383),
        vec3f(0.04823, 0.01566, 0.83777),
    );
    let m2 = mat3x3f(
        vec3f(1.60475, -0.10208, -0.00327),
        vec3f(-0.53108, 1.10813, -0.07276),
        vec3f(-0.07367, -0.00605, 1.07602),
    );
    let v = m1 * color;
    let a = v * (v + 0.0245786) - 0.000090537;
    let b = v * (0.983729 * v + 0.4329510) + 0.238081;
    return clamp(m2 * (a / b), vec3f(0.0), vec3f(1.0));
}

@fragment
fn fs_main(in: VsOut) -> @location(0) vec4f {
    let res = max(u.resolution, vec2f(1.0, 1.0));
    let frag = in.uv * res;
    let ray = get_ray(frag, res);
    if (ray.y >= 0.0) {
        let c = get_atmosphere(ray) + get_sun(ray);
        return vec4f(aces_tonemap(c * 2.0), 1.0);
    }
    let water_plane_high = vec3f(0.0, 0.0, 0.0);
    let water_plane_low = vec3f(0.0, -WATER_DEPTH, 0.0);
    let origin = vec3f(u.time * 0.2, CAMERA_HEIGHT, 1.0);
    let high_plane_hit = intersect_plane(origin, ray, water_plane_high, vec3f(0.0, 1.0, 0.0));
    let low_plane_hit = intersect_plane(origin, ray, water_plane_low, vec3f(0.0, 1.0, 0.0));
    let high_hit_pos = origin + ray * high_plane_hit;
    let low_hit_pos = origin + ray * low_plane_hit;
    let dist = raymarchwater(origin, high_hit_pos, low_hit_pos, WATER_DEPTH);
    let water_hit_pos = origin + ray * dist;
    var n = water_normal(water_hit_pos.xz, 0.01, WATER_DEPTH);
    n = mix(n, vec3f(0.0, 1.0, 0.0), 0.8 * min(1.0, sqrt(dist * 0.01) * 1.1));
    let fresnel = 0.04 + (1.0 - 0.04) * pow(1.0 - max(0.0, dot(-n, ray)), 5.0);
    var r = normalize(reflect(ray, n));
    r = vec3f(r.x, abs(r.y), r.z);
    let reflection = get_atmosphere(r) + get_sun(r);
    let scattering = vec3f(0.0293, 0.0698, 0.1717) * 0.1 * (0.2 + (water_hit_pos.y + WATER_DEPTH) / WATER_DEPTH);
    let c = fresnel * reflection + scattering;
    return vec4f(aces_tonemap(c * 2.0), 1.0);
}
""".trimIndent()
}
