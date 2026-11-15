package com.jlj.kuiklyholmui

/**
 * @CreateDate: 2026/11/16
 * @Author: jlj
 * @Description: mrange「电缆巢」Demo 移植（CC0）。
 * Lite=Shadertoy body；Runtime=WGSL 同构。
 *
 * 版权：CC0 1.0 Universal — 作者 mrange；出处 shadertoy.com
 * 片段保留：hsv2rgb（SO / WTFPL）；sphered（iquilezles articles）。
 *
 * 相对原版改动（GLES2 + Holm）：
 * - 去掉 df 重载（GLES ES 1.00 不支持）→ df_state / df_world
 * - postProcess 去掉 pow(1/2.2) 显示伽马（对齐 NeonTunnel / Holm）
 */
object CableNestShaders {
    const val UI_CREDIT: String = "作者 mrange · 出处 shadertoy.com · CC0"

    val LITE_BODY: String = """
// License CC0: Cable nest — Author: mrange · Source: shadertoy.com
// Holm Lite port: no df overload; no display gamma in postProcess.
// hsv2rgb: https://stackoverflow.com/a/17897228/418488 (WTFPL / sam hocevar)
// sphered: https://iquilezles.org/articles/spherefunctions
#define TOLERANCE       0.0001
#define NORMTOL         0.00125
#define MAX_RAY_LENGTH  20.0
#define MAX_RAY_MARCHES 90
#define TIME            iTime
#define RESOLUTION      iResolution
#define ROT(a)          mat2(cos(a), sin(a), -sin(a), cos(a))
#define PI              3.141592654
#define TAU             (2.0*PI)

const vec4 hsv2rgb_K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
vec3 hsv2rgb(vec3 c) {
  vec3 p = abs(fract(c.xxx + hsv2rgb_K.xyz) * 6.0 - hsv2rgb_K.www);
  return c.z * mix(hsv2rgb_K.xxx, clamp(p - hsv2rgb_K.xxx, 0.0, 1.0), c.y);
}
#define HSV2RGB(c)  (c.z * mix(hsv2rgb_K.xxx, clamp(abs(fract(c.xxx + hsv2rgb_K.xyz) * 6.0 - hsv2rgb_K.www) - hsv2rgb_K.xxx, 0.0, 1.0), c.y))

#define PATHA vec2(0.1147, 0.2093)
#define PATHB vec2(13.0, 3.0)

const float cam_amp = 1.0;

vec4 g_state = vec4(0.0);

float tanh_approx(float x) {
  float x2 = x*x;
  return clamp(x*(27.0 + x2)/(27.0+9.0*x2), -1.0, 1.0);
}

float sphered(vec3 ro, vec3 rd, vec4 sph, float dbuffer) {
    float ndbuffer = dbuffer/sph.w;
    vec3  rc = (ro - sph.xyz)/sph.w;
    float b = dot(rd,rc);
    float c = dot(rc,rc) - 1.0;
    float h = b*b - c;
    if( h<0.0 ) return 0.0;
    h = sqrt( h );
    float t1 = -b - h;
    float t2 = -b + h;
    if( t2<0.0 || t1>ndbuffer ) return 0.0;
    t1 = max( t1, 0.0 );
    t2 = min( t2, ndbuffer );
    float i1 = -(c*t1 + b*t1*t1 + t1*t1*t1/3.0);
    float i2 = -(c*t2 + b*t2*t2 + t2*t2*t2/3.0);
    return (i2-i1)*(3.0/4.0);
}

float hash(float co) {
  return fract(sin(co*12.9898) * 13758.5453);
}

vec3 cam_path(float z) {
  return vec3(cam_amp*sin(z*PATHA)*PATHB, z);
}

vec3 dcam_path(float z) {
  return vec3(cam_amp*PATHA*PATHB*cos(PATHA*z), 1.0);
}

vec3 ddcam_path(float z) {
  return cam_amp*vec3(cam_amp*-PATHA*PATHA*PATHB*sin(PATHA*z), 0.0);
}

float df_state(vec3 p3, out vec4 state) {
  float cylr = 0.2;
  vec2 p = p3.xy;
  float t = p3.z;
  const float ss = 1.5;
  mat2 pp = ss*ROT(1.0+0.5*p3.z);
  p *= ROT(-0.2*TIME);
  float s = 1.0;
  float d = 1E6;
  float tt = 0.0;
  for (int i = 0; i < 3; ++i) {
    tt += sqrt(2.0)*float(1+i);
    p *= pp;
    vec2 sp = sign(p);
    p = abs(p);
    tt += dot(sp, vec2(0.25, 0.5))*s;
    p -= 1.35*s;
    s *= 1.0/ss;
    float dd = (length(p-vec2(0.0))-cylr)*s;
    if (dd < d) {
      d = dd;
      state = vec4(p, t, hash(tt+123.4));
    }
  }
  return d;
}

float df_world(vec3 p) {
  vec3 cam = cam_path(p.z);
  vec3 dcam = normalize(dcam_path(p.z));
  p.xy -= cam.xy;
  p -= dcam*dot(vec3(p.xy, 0), dcam)*0.5*vec3(1,1,-1);
  vec4 state;
  float d = df_state(p, state);
  g_state = state;
  return d;
}

float rayMarch(in vec3 ro, in vec3 rd, out int iter) {
  float t = 0.0;
  int i = 0;
  for (i = 0; i < MAX_RAY_MARCHES; i++) {
    float distance = df_world(ro + rd*t);
    if (distance < TOLERANCE || t > MAX_RAY_LENGTH) break;
    t += distance;
  }
  iter = i;
  return t;
}

vec3 normal(in vec3 pos) {
  vec3  eps = vec3(NORMTOL,0.0,0.0);
  vec3 nor;
  nor.x = df_world(pos+eps.xyy) - df_world(pos-eps.xyy);
  nor.y = df_world(pos+eps.yxy) - df_world(pos-eps.yxy);
  nor.z = df_world(pos+eps.yyx) - df_world(pos-eps.yyx);
  return normalize(nor);
}

float softShadow(in vec3 pos, in vec3 ld, in float ll, float mint, float k) {
  const float minShadow = 0.25;
  float res = 1.0;
  float t = mint;
  for (int i=0; i<25; ++i) {
    float distance = df_world(pos + ld*t);
    res = min(res, k*distance/t);
    if (ll <= t) break;
    if(res <= minShadow) break;
    t += max(mint*0.2, distance);
  }
  return clamp(res,minShadow,1.0);
}

vec3 postProcess(in vec3 col, in vec2 q)  {
  col=clamp(col,0.0,1.0);
  col=col*0.6+0.4*col*col*(3.0-2.0*col);
  col=mix(col, vec3(dot(col, vec3(0.33))), -0.4);
  col*=0.5+0.5*pow(19.0*q.x*q.y*(1.0-q.x)*(1.0-q.y),0.7);
  return col;
}

vec3 render(vec3 ro, vec3 rd) {
  vec3 lightPos0  = cam_path(TIME-0.5);
  vec3 lightPos1  = cam_path(TIME+6.5);
  vec3 skyCol = vec3(0.0);
  int iter = 0;
  float t = rayMarch(ro, rd, iter);
  vec4 state = g_state;
  float tt = float(iter)/float(MAX_RAY_MARCHES);
  float bs = 1.0-tt*tt*tt*tt;
  vec3 pos = ro + t*rd;
  float lsd1  = sphered(ro, rd, vec4(lightPos1, 2.5), t);
  float beat  = smoothstep(0.25, 1.0, sin(TAU*TIME*120.0/60.0));
  vec3 bcol   = mix(1.5*vec3(2.25, 0.75, 0.5), 3.5*vec3(2.0, 1.0, 0.75), beat);
  vec3 gcol   = lsd1*bcol;
  if (t >= MAX_RAY_LENGTH) {
    return skyCol+gcol;
  }
  vec3 nor    = normal(pos);
  float sa    = atan(state.y, state.x)+4.0*state.z*(0.5+0.5*state.w);
  float v     = 0.9*smoothstep(-0.1, 0.1, sin(4.0*sa));
  vec3 color  = hsv2rgb(vec3(0.0+123.4*state.w, 0.66, 0.75*v));
  vec3 lv0    = lightPos0 - pos;
  float ll20  = dot(lv0, lv0);
  float ll0   = sqrt(ll20);
  vec3 ld0    = lv0 / ll0;
  float dm0   = 8.0/ll20;
  float sha0  = softShadow(pos, ld0, ll0, 0.125, 32.0);
  float dif0  = max(dot(nor,ld0),0.0)*dm0;
  vec3 lv1    = lightPos1 - pos;
  float ll21  = dot(lv1, lv1);
  float spe1  = pow(max(dot(reflect(normalize(lv1), nor), rd), 0.), 100.)*tanh_approx(3.0/ll21);
  vec3 col = vec3(0.0);
  col += dif0*sha0*color;
  col += spe1*bcol*bs;
  col += gcol;
  return col;
}

vec3 effect3d(vec2 p) {
  float tm = TIME;
  vec3 cam  = cam_path(tm);
  vec3 dcam = dcam_path(tm);
  vec3 ddcam= ddcam_path(tm);
  vec3 ro = cam;
  vec3 ww = normalize(dcam);
  vec3 uu = normalize(cross(vec3(0.0,1.0,0.0)+ddcam*-2.0, ww ));
  vec3 vv = normalize(cross(ww,uu));
  float rdd = (2.0+0.5*tanh_approx(length(p)));
  vec3 rd = normalize(p.x*uu + p.y*vv + rdd*ww);
  return render(ro, rd);
}

void mainImage(out vec4 fragColor, in vec2 fragCoord) {
  vec2 q=fragCoord.xy/RESOLUTION.xy;
  vec2 p = -1.0 + 2.0*q;
  p.x *= RESOLUTION.x/RESOLUTION.y;
  vec3 col = effect3d(p);
  fragColor = vec4(postProcess(col, q),1.0);
}
""".trimIndent()

    val RUNTIME_FS: String = """
const TOLERANCE: f32 = 0.0001;
const NORMTOL: f32 = 0.00125;
const MAX_RAY_LENGTH: f32 = 20.0;
const MAX_RAY_MARCHES: i32 = 90;
const PI: f32 = 3.141592654;
const TAU: f32 = 6.283185308;
const PATHA: vec2f = vec2f(0.1147, 0.2093);
const PATHB: vec2f = vec2f(13.0, 3.0);
const CAM_AMP: f32 = 1.0;
const HSV2RGB_K: vec4f = vec4f(1.0, 0.6666667, 0.3333333, 3.0);

var<private> g_state: vec4f = vec4f(0.0);

fn rot2(a: f32) -> mat2x2f {
  let c = cos(a);
  let s = sin(a);
  return mat2x2f(vec2f(c, s), vec2f(-s, c));
}

fn hsv2rgb(c: vec3f) -> vec3f {
  let p = abs(fract(c.xxx + HSV2RGB_K.xyz) * 6.0 - HSV2RGB_K.www);
  return c.z * mix(HSV2RGB_K.xxx, clamp(p - HSV2RGB_K.xxx, vec3f(0.0), vec3f(1.0)), c.y);
}

fn tanh_approx(x: f32) -> f32 {
  let x2 = x * x;
  return clamp(x * (27.0 + x2) / (27.0 + 9.0 * x2), -1.0, 1.0);
}

fn sphered(ro: vec3f, rd: vec3f, sph: vec4f, dbuffer: f32) -> f32 {
  let ndbuffer = dbuffer / sph.w;
  let rc = (ro - sph.xyz) / sph.w;
  let b = dot(rd, rc);
  let c = dot(rc, rc) - 1.0;
  var h = b * b - c;
  if (h < 0.0) {
    return 0.0;
  }
  h = sqrt(h);
  var t1 = -b - h;
  var t2 = -b + h;
  if (t2 < 0.0 || t1 > ndbuffer) {
    return 0.0;
  }
  t1 = max(t1, 0.0);
  t2 = min(t2, ndbuffer);
  let i1 = -(c * t1 + b * t1 * t1 + t1 * t1 * t1 / 3.0);
  let i2 = -(c * t2 + b * t2 * t2 + t2 * t2 * t2 / 3.0);
  return (i2 - i1) * (3.0 / 4.0);
}

fn hash1(co: f32) -> f32 {
  return fract(sin(co * 12.9898) * 13758.5453);
}

fn cam_path(z: f32) -> vec3f {
  return vec3f(CAM_AMP * sin(z * PATHA) * PATHB, z);
}

fn dcam_path(z: f32) -> vec3f {
  return vec3f(CAM_AMP * PATHA * PATHB * cos(PATHA * z), 1.0);
}

fn ddcam_path(z: f32) -> vec3f {
  return CAM_AMP * vec3f(CAM_AMP * (-PATHA * PATHA * PATHB) * sin(PATHA * z), 0.0);
}

fn df_state(p3: vec3f) -> vec2f {
  // returns (distance, unused); writes g_state
  var p = p3.xy;
  let t = p3.z;
  let ss = 1.5;
  let pp = ss * rot2(1.0 + 0.5 * p3.z);
  p = rot2(-0.2 * u.time) * p;
  var s = 1.0;
  var d = 1e6;
  var tt = 0.0;
  var state = vec4f(0.0);
  for (var i = 0; i < 3; i = i + 1) {
    tt = tt + sqrt(2.0) * f32(1 + i);
    p = pp * p;
    let sp = sign(p);
    p = abs(p);
    tt = tt + dot(sp, vec2f(0.25, 0.5)) * s;
    p = p - 1.35 * s;
    s = s * (1.0 / ss);
    let dd = (length(p - vec2f(0.0)) - 0.2) * s;
    if (dd < d) {
      d = dd;
      state = vec4f(p, t, hash1(tt + 123.4));
    }
  }
  g_state = state;
  return vec2f(d, 0.0);
}

fn df_world(p0: vec3f) -> f32 {
  var p = p0;
  let cam = cam_path(p.z);
  let dcam = normalize(dcam_path(p.z));
  p = vec3f(p.xy - cam.xy, p.z);
  p = p - dcam * dot(vec3f(p.xy, 0.0), dcam) * 0.5 * vec3f(1.0, 1.0, -1.0);
  return df_state(p).x;
}

fn ray_march(ro: vec3f, rd: vec3f) -> vec2f {
  var t = 0.0;
  var iter = 0;
  for (var i = 0; i < MAX_RAY_MARCHES; i = i + 1) {
    let distance = df_world(ro + rd * t);
    iter = i;
    if (distance < TOLERANCE || t > MAX_RAY_LENGTH) {
      break;
    }
    t = t + distance;
  }
  return vec2f(t, f32(iter));
}

fn normal_at(pos: vec3f) -> vec3f {
  let eps = vec3f(NORMTOL, 0.0, 0.0);
  return normalize(vec3f(
    df_world(pos + eps.xyy) - df_world(pos - eps.xyy),
    df_world(pos + eps.yxy) - df_world(pos - eps.yxy),
    df_world(pos + eps.yyx) - df_world(pos - eps.yyx),
  ));
}

fn soft_shadow(pos: vec3f, ld: vec3f, ll: f32, mint: f32, k: f32) -> f32 {
  let min_shadow = 0.25;
  var res = 1.0;
  var t = mint;
  for (var i = 0; i < 25; i = i + 1) {
    let distance = df_world(pos + ld * t);
    res = min(res, k * distance / t);
    if (ll <= t) {
      break;
    }
    if (res <= min_shadow) {
      break;
    }
    t = t + max(mint * 0.2, distance);
  }
  return clamp(res, min_shadow, 1.0);
}

fn post_process(col0: vec3f, q: vec2f) -> vec3f {
  var col = clamp(col0, vec3f(0.0), vec3f(1.0));
  col = col * 0.6 + 0.4 * col * col * (3.0 - 2.0 * col);
  col = mix(col, vec3f(dot(col, vec3f(0.33))), -0.4);
  col = col * (0.5 + 0.5 * pow(19.0 * q.x * q.y * (1.0 - q.x) * (1.0 - q.y), 0.7));
  return col;
}

fn render(ro: vec3f, rd: vec3f) -> vec3f {
  let light_pos0 = cam_path(u.time - 0.5);
  let light_pos1 = cam_path(u.time + 6.5);
  let sky_col = vec3f(0.0);
  let march = ray_march(ro, rd);
  let t = march.x;
  let iter = march.y;
  let state = g_state;
  let tt = iter / f32(MAX_RAY_MARCHES);
  let bs = 1.0 - tt * tt * tt * tt;
  let pos = ro + t * rd;
  let lsd1 = sphered(ro, rd, vec4f(light_pos1, 2.5), t);
  let beat = smoothstep(0.25, 1.0, sin(TAU * u.time * 2.0));
  let bcol = mix(1.5 * vec3f(2.25, 0.75, 0.5), 3.5 * vec3f(2.0, 1.0, 0.75), beat);
  let gcol = lsd1 * bcol;
  if (t >= MAX_RAY_LENGTH) {
    return sky_col + gcol;
  }
  let nor = normal_at(pos);
  let sa = atan2(state.y, state.x) + 4.0 * state.z * (0.5 + 0.5 * state.w);
  let v = 0.9 * smoothstep(-0.1, 0.1, sin(4.0 * sa));
  let color = hsv2rgb(vec3f(123.4 * state.w, 0.66, 0.75 * v));
  let lv0 = light_pos0 - pos;
  let ll20 = dot(lv0, lv0);
  let ll0 = sqrt(ll20);
  let ld0 = lv0 / ll0;
  let dm0 = 8.0 / ll20;
  let sha0 = soft_shadow(pos, ld0, ll0, 0.125, 32.0);
  let dif0 = max(dot(nor, ld0), 0.0) * dm0;
  let lv1 = light_pos1 - pos;
  let ll21 = dot(lv1, lv1);
  let spe1 = pow(max(dot(reflect(normalize(lv1), nor), rd), 0.0), 100.0) * tanh_approx(3.0 / ll21);
  var col = vec3f(0.0);
  col = col + dif0 * sha0 * color;
  col = col + spe1 * bcol * bs;
  col = col + gcol;
  return col;
}

fn effect3d(p: vec2f) -> vec3f {
  let tm = u.time;
  let cam = cam_path(tm);
  let dcam = dcam_path(tm);
  let ddcam = ddcam_path(tm);
  let ro = cam;
  let ww = normalize(dcam);
  let uu = normalize(cross(vec3f(0.0, 1.0, 0.0) + ddcam * (-2.0), ww));
  let vv = normalize(cross(ww, uu));
  let rdd = 2.0 + 0.5 * tanh_approx(length(p));
  let rd = normalize(p.x * uu + p.y * vv + rdd * ww);
  return render(ro, rd);
}

@fragment
fn fs_main(in: VsOut) -> @location(0) vec4f {
  let res = max(u.resolution, vec2f(1.0, 1.0));
  let frag = in.uv * res;
  let q = frag / res;
  var p = -1.0 + 2.0 * q;
  p.x = p.x * (res.x / res.y);
  let col = post_process(effect3d(p), q);
  return vec4f(col, 1.0);
}
""".trimIndent()
}
