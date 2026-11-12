/* Holm liquid_approx — Lite 单 Pass 液态玻璃近似（策略 A；约 7 次 sceneColor/像素） */
#ifdef HOLM_WEBGL2
#version 300 es
#ifdef GL_FRAGMENT_PRECISION_HIGH
precision highp float;
#else
precision mediump float;
#endif
in vec2 v_uv;
uniform float u_time;
uniform float u_liquidBoost;
uniform float u_liquidBend;
out vec4 fragColor;
#else
#ifdef GL_FRAGMENT_PRECISION_HIGH
precision highp float;
#else
precision mediump float;
#endif
varying vec2 v_uv;
uniform float u_time;
uniform float u_liquidBoost;
uniform float u_liquidBend;
#endif

float sdRoundBox(vec2 p, vec2 b, float r) {
    vec2 q = abs(p) - b + r;
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
}

vec3 sceneColor(vec2 uv) {
    vec2 p = uv + vec2(u_time * 0.035, u_time * 0.018);
    float stripe = smoothstep(0.35, 0.65, fract(p.x * 5.5 + p.y * 1.4));
    vec3 a = vec3(0.12, 0.42, 0.95);
    vec3 b = vec3(0.95, 0.32, 0.22);
    vec3 col = mix(a, b, stripe);
    float d = length(p - vec2(0.35 + 0.1 * sin(u_time * 0.55), 0.55));
    col = mix(col, vec3(0.15, 0.92, 0.55), 1.0 - smoothstep(0.12, 0.22, d));
    float d2 = length(p - vec2(0.72, 0.32 + 0.08 * cos(u_time * 0.4)));
    col = mix(col, vec3(0.95, 0.85, 0.18), 1.0 - smoothstep(0.08, 0.16, d2));
    return col;
}

/* 5-tap 轻模糊：宽画布用各向异性半径，减轻横向拉伸感 */
vec3 softBlur(vec2 uv) {
    vec2 r = vec2(0.010, 0.024);
    vec3 sum = sceneColor(uv);
    sum += sceneColor(uv + vec2(r.x, 0.0));
    sum += sceneColor(uv + vec2(-r.x, 0.0));
    sum += sceneColor(uv + vec2(0.0, r.y));
    sum += sceneColor(uv + vec2(0.0, -r.y));
    return sum * 0.2;
}

void main() {
    vec2 uv = v_uv;
    vec2 p = uv - 0.5;
    /* 近满幅玻璃板：去掉中间浮岛 + 外圈发黑的「画框」感 */
    float glass = sdRoundBox(p, vec2(0.492, 0.472), 0.055);
    float inside = 1.0 - smoothstep(-0.008, 0.012, glass);
    float rim = 1.0 - smoothstep(0.0, 0.055, abs(glass));
    float fresnel = pow(clamp(rim, 0.0, 1.0), 1.6);

    float boostK = max(u_liquidBoost - 1.0, 0.0);
    /* 折射只跟边缘 fresnel：中心近似不拧 */
    float bendAmt = u_liquidBend * fresnel * (0.85 + 0.65 * boostK);
    vec2 nDir = vec2(p.x * 0.42, p.y);
    float nLen = length(nDir);
    vec2 n = nLen > 0.001 ? (nDir / nLen) : vec2(0.0);
    float wave = sin(u_time * 0.85 + p.x * 7.0 + p.y * 9.0) * 0.3
               + sin(u_time * 0.45 + p.y * 10.0) * 0.22;
    vec2 offset = n * bendAmt * (0.5 + 0.5 * wave);
    offset.x *= 0.45;

    vec2 sampleUv = uv + offset * inside;
    vec3 blurred = softBlur(sampleUv);

    /* 色散：轻 mix，避免 R 锐/G 糊错位成彩虹锯齿 */
    float chroma = boostK * 0.022 * fresnel * inside;
    vec2 cOff = vec2(n.x * 0.45, n.y) * chroma;
    vec3 col = blurred;
    col.r = mix(col.r, sceneColor(sampleUv + cOff).r, 0.35);
    col.b = mix(col.b, sceneColor(sampleUv - cOff).b, 0.35);

    col = mix(col, vec3(0.88, 0.92, 0.98), 0.08 * inside + 0.05 * fresnel);
    float sheen = pow(fresnel, 2.4) * (0.18 + 0.22 * boostK);
    col += vec3(0.92, 0.96, 1.0) * sheen;
    float spec = pow(max(1.0 - abs(p.x * 0.9 + p.y * 0.55 - 0.05), 0.0), 10.0) * inside * 0.16;
    col += vec3(1.0) * spec;

    vec3 outside = sceneColor(uv) * 0.88;
    col = mix(outside, col, inside);

#ifdef HOLM_WEBGL2
    fragColor = vec4(col, 1.0);
#else
    gl_FragColor = vec4(col, 1.0);
#endif
}
