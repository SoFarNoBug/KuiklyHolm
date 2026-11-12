/* Holm frost fragment — 毛玻璃观感（策略 A：岛内模拟底图 + 邻域模糊，无真 backdrop） */
#ifdef HOLM_WEBGL2
#version 300 es
precision mediump float;
in vec2 v_uv;
uniform float u_time;
out vec4 fragColor;
#else
precision mediump float;
varying vec2 v_uv;
uniform float u_time;
#endif

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

/* 高对比可辨场景：条纹 + 圆，随时间缓慢平移 */
vec3 sceneColor(vec2 uv) {
    vec2 p = uv + vec2(u_time * 0.04, u_time * 0.02);
    float stripe = step(0.5, fract(p.x * 6.0 + p.y * 1.5));
    vec3 a = vec3(0.15, 0.45, 0.95);
    vec3 b = vec3(0.95, 0.35, 0.25);
    vec3 col = mix(a, b, stripe);
    float d = length(p - vec2(0.35 + 0.1 * sin(u_time * 0.7), 0.55));
    col = mix(col, vec3(0.2, 0.9, 0.55), 1.0 - smoothstep(0.12, 0.22, d));
    float d2 = length(p - vec2(0.72, 0.32 + 0.08 * cos(u_time * 0.5)));
    col = mix(col, vec3(0.95, 0.85, 0.2), 1.0 - smoothstep(0.08, 0.16, d2));
    return col;
}

vec3 blurScene(vec2 uv) {
    float r = 0.018;
    vec3 sum = vec3(0.0);
    sum += sceneColor(uv);
    sum += sceneColor(uv + vec2(r, 0.0));
    sum += sceneColor(uv + vec2(-r, 0.0));
    sum += sceneColor(uv + vec2(0.0, r));
    sum += sceneColor(uv + vec2(0.0, -r));
    sum += sceneColor(uv + vec2(r, r));
    sum += sceneColor(uv + vec2(r, -r));
    sum += sceneColor(uv + vec2(-r, r));
    sum += sceneColor(uv + vec2(-r, -r));
    sum += sceneColor(uv + vec2(1.5 * r, 0.5 * r));
    sum += sceneColor(uv + vec2(-1.5 * r, -0.5 * r));
    sum += sceneColor(uv + vec2(0.5 * r, 1.5 * r));
    sum += sceneColor(uv + vec2(-0.5 * r, -1.5 * r));
    return sum / 13.0;
}

void main() {
    vec2 uv = v_uv;
    vec3 blurred = blurScene(uv);
    float n = hash(uv * 40.0 + u_time * 0.1) * 0.06;
    vec3 frosted = mix(blurred, vec3(0.92, 0.95, 1.0), 0.32 + n);
    float edge = smoothstep(1.15, 0.35, length(uv - 0.5));
    frosted *= 0.82 + 0.22 * edge;
#ifdef HOLM_WEBGL2
    fragColor = vec4(frosted, 1.0);
#else
    gl_FragColor = vec4(frosted, 1.0);
#endif
}
