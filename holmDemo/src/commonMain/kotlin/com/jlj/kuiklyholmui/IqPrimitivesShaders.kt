package com.jlj.kuiklyholmui

/**
 * @CreateDate: 2026/11/15
 * @Author: jlj
 * @Description: Inigo Quilez「Raymarching primitives」Demo 移植（MIT）。
 * Lite=Shadertoy body；Runtime=WGSL 同构近似。
 *
 * 版权（须随副本保留）：
 * The MIT License — Copyright © 2013 Inigo Quilez
 * 作者：Inigo Quilez；出处：shadertoy.com（view/Xds3zN）
 * 另见：https://www.shadertoy.com/playlist/43cXRl · https://iquilezles.org/articles/distfunctions
 * 全文 permission notice 见 [LITE_BODY] / [RUNTIME_FS] 注释头。
 *
 * 相对原版改动（GLES2/WebGL1 + Holm 壳）：
 * - 仅 iTime/iResolution；iMouse/iFrame 桩掉；AA 固定 1
 * - 去掉函数重载 / 位运算；dot2→dot2/dot3；重载 SDF 改后缀 2
 * - 去掉片元 gamma pow（对齐 NeonTunnel / Holm 输出约定）
 */
object IqPrimitivesShaders {
    /** Demo 一行小字：作者 + 出处（MIT 全文在 shader 注释头）。 */
    const val UI_CREDIT: String = "作者 Inigo Quilez · 出处 shadertoy.com · MIT"

    /**
     * Shadertoy 正文。体积大、步进多，弱端可能掉帧；Demo 观感优先。
     */
    val LITE_BODY: String = """
// The MIT License
// Copyright © 2013 Inigo Quilez
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
// Author: Inigo Quilez · Source: shadertoy.com (https://www.shadertoy.com/view/Xds3zN)
// Also: https://www.shadertoy.com/playlist/43cXRl · https://iquilezles.org/articles/distfunctions

#define iMouse vec4(0.0)
#define iFrame 0

float dot2(vec2 v) { return dot(v, v); }
float dot3(vec3 v) { return dot(v, v); }
float ndot(vec2 a, vec2 b) { return a.x * b.x - a.y * b.y; }

float sdPlane(vec3 p) { return p.y; }

float sdSphere(vec3 p, float s) { return length(p) - s; }

float sdBox(vec3 p, vec3 b) {
    vec3 d = abs(p) - b;
    return min(max(d.x, max(d.y, d.z)), 0.0) + length(max(d, 0.0));
}

float sdBoxFrame(vec3 p, vec3 b, float e) {
    p = abs(p) - b;
    vec3 q = abs(p + e) - e;
    return min(min(
        length(max(vec3(p.x, q.y, q.z), 0.0)) + min(max(p.x, max(q.y, q.z)), 0.0),
        length(max(vec3(q.x, p.y, q.z), 0.0)) + min(max(q.x, max(p.y, q.z)), 0.0)),
        length(max(vec3(q.x, q.y, p.z), 0.0)) + min(max(q.x, max(q.y, p.z)), 0.0));
}

float sdEllipsoid(vec3 p, vec3 r) {
    float k0 = length(p / r);
    float k1 = length(p / (r * r));
    return k0 * (k0 - 1.0) / k1;
}

float sdTorus(vec3 p, vec2 t) {
    return length(vec2(length(p.xz) - t.x, p.y)) - t.y;
}

float sdCappedTorus(vec3 p, vec2 sc, float ra, float rb) {
    p.x = abs(p.x);
    float k = (sc.y * p.x > sc.x * p.y) ? dot(p.xy, sc) : length(p.xy);
    return sqrt(dot(p, p) + ra * ra - 2.0 * ra * k) - rb;
}

float sdHexPrism(vec3 p, vec2 h) {
    const vec3 k = vec3(-0.8660254, 0.5, 0.57735);
    p = abs(p);
    p.xy -= 2.0 * min(dot(k.xy, p.xy), 0.0) * k.xy;
    vec2 d = vec2(
        length(p.xy - vec2(clamp(p.x, -k.z * h.x, k.z * h.x), h.x)) * sign(p.y - h.x),
        p.z - h.y);
    return min(max(d.x, d.y), 0.0) + length(max(d, 0.0));
}

float sdOctogonPrism(vec3 p, float r, float h) {
    const vec3 k = vec3(-0.9238795325, 0.3826834323, 0.4142135623);
    p = abs(p);
    p.xy -= 2.0 * min(dot(vec2(k.x, k.y), p.xy), 0.0) * vec2(k.x, k.y);
    p.xy -= 2.0 * min(dot(vec2(-k.x, k.y), p.xy), 0.0) * vec2(-k.x, k.y);
    p.xy -= vec2(clamp(p.x, -k.z * r, k.z * r), r);
    vec2 d = vec2(length(p.xy) * sign(p.y), p.z - h);
    return min(max(d.x, d.y), 0.0) + length(max(d, 0.0));
}

float sdCapsule(vec3 p, vec3 a, vec3 b, float r) {
    vec3 pa = p - a;
    vec3 ba = b - a;
    float h = clamp(dot(pa, ba) / dot(ba, ba), 0.0, 1.0);
    return length(pa - ba * h) - r;
}

float sdRoundCone(vec3 p, float r1, float r2, float h) {
    vec2 q = vec2(length(p.xz), p.y);
    float b = (r1 - r2) / h;
    float a = sqrt(1.0 - b * b);
    float k = dot(q, vec2(-b, a));
    if (k < 0.0) return length(q) - r1;
    if (k > a * h) return length(q - vec2(0.0, h)) - r2;
    return dot(q, vec2(a, b)) - r1;
}

float sdRoundCone2(vec3 p, vec3 a, vec3 b, float r1, float r2) {
    vec3 ba = b - a;
    float l2 = dot(ba, ba);
    float rr = r1 - r2;
    float a2 = l2 - rr * rr;
    float il2 = 1.0 / l2;
    vec3 pa = p - a;
    float y = dot(pa, ba);
    float z = y - l2;
    float x2 = dot3(pa * l2 - ba * y);
    float y2 = y * y * l2;
    float z2 = z * z * l2;
    float k = sign(rr) * rr * rr * x2;
    if (sign(z) * a2 * z2 > k) return sqrt(x2 + z2) * il2 - r2;
    if (sign(y) * a2 * y2 < k) return sqrt(x2 + y2) * il2 - r1;
    return (sqrt(x2 * a2 * il2) + y * rr) * il2 - r1;
}

float sdTriPrism(vec3 p, vec2 h) {
    float k = sqrt(3.0);
    h.x *= 0.5 * k;
    p.xy /= h.x;
    p.x = abs(p.x) - 1.0;
    p.y = p.y + 1.0 / k;
    if (p.x + k * p.y > 0.0) p.xy = vec2(p.x - k * p.y, -k * p.x - p.y) / 2.0;
    p.x -= clamp(p.x, -2.0, 0.0);
    float d1 = length(p.xy) * sign(-p.y) * h.x;
    float d2 = abs(p.z) - h.y;
    return length(max(vec2(d1, d2), 0.0)) + min(max(d1, d2), 0.0);
}

float sdCylinder(vec3 p, vec2 h) {
    vec2 d = abs(vec2(length(p.xz), p.y)) - h;
    return min(max(d.x, d.y), 0.0) + length(max(d, 0.0));
}

float sdCylinder2(vec3 p, vec3 a, vec3 b, float r) {
    vec3 pa = p - a;
    vec3 ba = b - a;
    float baba = dot(ba, ba);
    float paba = dot(pa, ba);
    float x = length(pa * baba - ba * paba) - r * baba;
    float y = abs(paba - baba * 0.5) - baba * 0.5;
    float x2 = x * x;
    float y2 = y * y * baba;
    float d = (max(x, y) < 0.0) ? -min(x2, y2) : (((x > 0.0) ? x2 : 0.0) + ((y > 0.0) ? y2 : 0.0));
    return sign(d) * sqrt(abs(d)) / baba;
}

float sdCone(vec3 p, vec2 c, float h) {
    vec2 q = h * vec2(c.x, -c.y) / c.y;
    vec2 w = vec2(length(p.xz), p.y);
    vec2 a = w - q * clamp(dot(w, q) / dot(q, q), 0.0, 1.0);
    vec2 b = w - q * vec2(clamp(w.x / q.x, 0.0, 1.0), 1.0);
    float k = sign(q.y);
    float d = min(dot(a, a), dot(b, b));
    float s = max(k * (w.x * q.y - w.y * q.x), k * (w.y - q.y));
    return sqrt(d) * sign(s);
}

float sdCappedCone(vec3 p, float h, float r1, float r2) {
    vec2 q = vec2(length(p.xz), p.y);
    vec2 k1 = vec2(r2, h);
    vec2 k2 = vec2(r2 - r1, 2.0 * h);
    vec2 ca = vec2(q.x - min(q.x, (q.y < 0.0) ? r1 : r2), abs(q.y) - h);
    vec2 cb = q - k1 + k2 * clamp(dot(k1 - q, k2) / dot2(k2), 0.0, 1.0);
    float s = (cb.x < 0.0 && ca.y < 0.0) ? -1.0 : 1.0;
    return s * sqrt(min(dot2(ca), dot2(cb)));
}

float sdCappedCone2(vec3 p, vec3 a, vec3 b, float ra, float rb) {
    float rba = rb - ra;
    float baba = dot(b - a, b - a);
    float papa = dot(p - a, p - a);
    float paba = dot(p - a, b - a) / baba;
    float x = sqrt(papa - paba * paba * baba);
    float cax = max(0.0, x - ((paba < 0.5) ? ra : rb));
    float cay = abs(paba - 0.5) - 0.5;
    float k = rba * rba + baba;
    float f = clamp((rba * (x - ra) + paba * baba) / k, 0.0, 1.0);
    float cbx = x - ra - f * rba;
    float cby = paba - f;
    float s = (cbx < 0.0 && cay < 0.0) ? -1.0 : 1.0;
    return s * sqrt(min(cax * cax + cay * cay * baba, cbx * cbx + cby * cby * baba));
}

float sdSolidAngle(vec3 pos, vec2 c, float ra) {
    vec2 p = vec2(length(pos.xz), pos.y);
    float l = length(p) - ra;
    float m = length(p - c * clamp(dot(p, c), 0.0, ra));
    return max(l, m * sign(c.y * p.x - c.x * p.y));
}

float sdOctahedron(vec3 p, float s) {
    p = abs(p);
    float m = p.x + p.y + p.z - s;
    vec3 q;
    if (3.0 * p.x < m) q = p.xyz;
    else if (3.0 * p.y < m) q = p.yzx;
    else if (3.0 * p.z < m) q = p.zxy;
    else return m * 0.57735027;
    float k = clamp(0.5 * (q.z - q.y + s), 0.0, s);
    return length(vec3(q.x, q.y - s + k, q.z - k));
}

float sdPyramid(vec3 p, float h) {
    float m2 = h * h + 0.25;
    p.xz = abs(p.xz);
    p.xz = (p.z > p.x) ? p.zx : p.xz;
    p.xz -= 0.5;
    vec3 q = vec3(p.z, h * p.y - 0.5 * p.x, h * p.x + 0.5 * p.y);
    float s = max(-q.x, 0.0);
    float t = clamp((q.y - 0.5 * p.z) / (m2 + 0.25), 0.0, 1.0);
    float a = m2 * (q.x + s) * (q.x + s) + q.y * q.y;
    float b = m2 * (q.x + 0.5 * t) * (q.x + 0.5 * t) + (q.y - m2 * t) * (q.y - m2 * t);
    float d2 = min(q.y, -q.x * m2 - q.y * 0.5) > 0.0 ? 0.0 : min(a, b);
    return sqrt((d2 + q.z * q.z) / m2) * sign(max(q.z, -p.y));
}

float sdRhombus(vec3 p, float la, float lb, float h, float ra) {
    p = abs(p);
    vec2 b = vec2(la, lb);
    float f = clamp((ndot(b, b - 2.0 * p.xz)) / dot(b, b), -1.0, 1.0);
    vec2 q = vec2(length(p.xz - 0.5 * b * vec2(1.0 - f, 1.0 + f)) * sign(p.x * b.y + p.z * b.x - b.x * b.y) - ra, p.y - h);
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0));
}

float sdHorseshoe(vec3 p, vec2 c, float r, float le, vec2 w) {
    p.x = abs(p.x);
    float l = length(p.xy);
    p.xy = mat2(-c.x, c.y, c.y, c.x) * p.xy;
    p.xy = vec2((p.y > 0.0 || p.x > 0.0) ? p.x : l * sign(-c.x), (p.x > 0.0) ? p.y : l);
    p.xy = vec2(p.x, abs(p.y - r)) - vec2(le, 0.0);
    vec2 q = vec2(length(max(p.xy, 0.0)) + min(0.0, max(p.x, p.y)), p.z);
    vec2 d = abs(q) - w;
    return min(max(d.x, d.y), 0.0) + length(max(d, 0.0));
}

float sdU(vec3 p, float r, float le, vec2 w) {
    p.x = (p.y > 0.0) ? abs(p.x) : length(p.xy);
    p.x = abs(p.x - r);
    p.y = p.y - le;
    float k = max(p.x, p.y);
    vec2 q = vec2((k < 0.0) ? -k : length(max(p.xy, 0.0)), abs(p.z)) - w;
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0);
}

vec2 opU(vec2 d1, vec2 d2) {
    return (d1.x < d2.x) ? d1 : d2;
}

vec2 map(vec3 pos) {
    vec2 res = vec2(pos.y, 0.0);

    if (sdBox(pos - vec3(-2.0, 0.3, 0.25), vec3(0.3, 0.3, 1.0)) < res.x) {
        res = opU(res, vec2(sdSphere(pos - vec3(-2.0, 0.25, 0.0), 0.25), 26.9));
        res = opU(res, vec2(sdRhombus((pos - vec3(-2.0, 0.25, 1.0)).xzy, 0.15, 0.25, 0.04, 0.08), 17.0));
    }

    if (sdBox(pos - vec3(0.0, 0.3, -1.0), vec3(0.35, 0.3, 2.5)) < res.x) {
        res = opU(res, vec2(sdCappedTorus((pos - vec3(0.0, 0.30, 1.0)) * vec3(1.0, -1.0, 1.0), vec2(0.866025, -0.5), 0.25, 0.05), 25.0));
        res = opU(res, vec2(sdBoxFrame(pos - vec3(0.0, 0.25, 0.0), vec3(0.3, 0.25, 0.2), 0.025), 16.9));
        res = opU(res, vec2(sdCone(pos - vec3(0.0, 0.45, -1.0), vec2(0.6, 0.8), 0.45), 55.0));
        res = opU(res, vec2(sdCappedCone(pos - vec3(0.0, 0.25, -2.0), 0.25, 0.25, 0.1), 13.67));
        res = opU(res, vec2(sdSolidAngle(pos - vec3(0.0, 0.00, -3.0), vec2(3.0, 4.0) / 5.0, 0.4), 49.13));
    }

    if (sdBox(pos - vec3(1.0, 0.3, -1.0), vec3(0.35, 0.3, 2.5)) < res.x) {
        res = opU(res, vec2(sdTorus((pos - vec3(1.0, 0.30, 1.0)).xzy, vec2(0.25, 0.05)), 7.1));
        res = opU(res, vec2(sdBox(pos - vec3(1.0, 0.25, 0.0), vec3(0.3, 0.25, 0.1)), 3.0));
        res = opU(res, vec2(sdCapsule(pos - vec3(1.0, 0.00, -1.0), vec3(-0.1, 0.1, -0.1), vec3(0.2, 0.4, 0.2), 0.1), 31.9));
        res = opU(res, vec2(sdCylinder(pos - vec3(1.0, 0.25, -2.0), vec2(0.15, 0.25)), 8.0));
        res = opU(res, vec2(sdHexPrism(pos - vec3(1.0, 0.2, -3.0), vec2(0.2, 0.05)), 18.4));
    }

    if (sdBox(pos - vec3(-1.0, 0.35, -1.0), vec3(0.35, 0.35, 2.5)) < res.x) {
        res = opU(res, vec2(sdPyramid(pos - vec3(-1.0, -0.6, -3.0), 1.0), 13.56));
        res = opU(res, vec2(sdOctahedron(pos - vec3(-1.0, 0.15, -2.0), 0.35), 23.56));
        res = opU(res, vec2(sdTriPrism(pos - vec3(-1.0, 0.15, -1.0), vec2(0.3, 0.05)), 43.5));
        res = opU(res, vec2(sdEllipsoid(pos - vec3(-1.0, 0.25, 0.0), vec3(0.2, 0.25, 0.05)), 43.17));
        res = opU(res, vec2(sdHorseshoe(pos - vec3(-1.0, 0.25, 1.0), vec2(cos(1.3), sin(1.3)), 0.2, 0.3, vec2(0.03, 0.08)), 11.5));
    }

    if (sdBox(pos - vec3(2.0, 0.3, -1.0), vec3(0.35, 0.3, 2.5)) < res.x) {
        res = opU(res, vec2(sdOctogonPrism(pos - vec3(2.0, 0.2, -3.0), 0.2, 0.05), 51.8));
        res = opU(res, vec2(sdCylinder2(pos - vec3(2.0, 0.14, -2.0), vec3(0.1, -0.1, 0.0), vec3(-0.2, 0.35, 0.1), 0.08), 31.2));
        res = opU(res, vec2(sdCappedCone2(pos - vec3(2.0, 0.09, -1.0), vec3(0.1, 0.0, 0.0), vec3(-0.2, 0.40, 0.1), 0.15, 0.05), 46.1));
        res = opU(res, vec2(sdRoundCone2(pos - vec3(2.0, 0.15, 0.0), vec3(0.1, 0.0, 0.0), vec3(-0.1, 0.35, 0.1), 0.15, 0.05), 51.7));
        res = opU(res, vec2(sdRoundCone(pos - vec3(2.0, 0.20, 1.0), 0.2, 0.1, 0.3), 37.0));
    }

    return res;
}

vec2 iBox(vec3 ro, vec3 rd, vec3 rad) {
    vec3 m = 1.0 / rd;
    vec3 n = m * ro;
    vec3 k = abs(m) * rad;
    vec3 t1 = -n - k;
    vec3 t2 = -n + k;
    return vec2(max(max(t1.x, t1.y), t1.z), min(min(t2.x, t2.y), t2.z));
}

vec2 raycast(vec3 ro, vec3 rd) {
    vec2 res = vec2(-1.0, -1.0);
    float tmin = 1.0;
    float tmax = 20.0;
    float tp1 = (0.0 - ro.y) / rd.y;
    if (tp1 > 0.0) {
        tmax = min(tmax, tp1);
        res = vec2(tp1, 1.0);
    }
    vec2 tb = iBox(ro - vec3(0.0, 0.4, -0.5), rd, vec3(2.5, 0.41, 3.0));
    if (tb.x < tb.y && tb.y > 0.0 && tb.x < tmax) {
        tmin = max(tb.x, tmin);
        tmax = min(tb.y, tmax);
        float t = tmin;
        for (int i = 0; i < 70; i++) {
            if (t >= tmax) break;
            vec2 h = map(ro + rd * t);
            if (abs(h.x) < (0.0001 * t)) {
                res = vec2(t, h.y);
                break;
            }
            t += h.x;
        }
    }
    return res;
}

float calcSoftshadow(vec3 ro, vec3 rd, float mint, float tmax) {
    float tp = (0.8 - ro.y) / rd.y;
    if (tp > 0.0) tmax = min(tmax, tp);
    float res = 1.0;
    float t = mint;
    for (int i = 0; i < 24; i++) {
        float h = map(ro + rd * t).x;
        float s = clamp(8.0 * h / t, 0.0, 1.0);
        res = min(res, s);
        t += clamp(h, 0.01, 0.2);
        if (res < 0.004 || t > tmax) break;
    }
    res = clamp(res, 0.0, 1.0);
    return res * res * (3.0 - 2.0 * res);
}

vec3 calcNormal(vec3 pos) {
    vec2 e = vec2(1.0, -1.0) * 0.5773 * 0.0005;
    return normalize(
        e.xyy * map(pos + e.xyy).x +
        e.yyx * map(pos + e.yyx).x +
        e.yxy * map(pos + e.yxy).x +
        e.xxx * map(pos + e.xxx).x);
}

float calcAO(vec3 pos, vec3 nor) {
    float occ = 0.0;
    float sca = 1.0;
    for (int i = 0; i < 5; i++) {
        float h = 0.01 + 0.12 * float(i) / 4.0;
        float d = map(pos + h * nor).x;
        occ += (h - d) * sca;
        sca *= 0.95;
        if (occ > 0.35) break;
    }
    return clamp(1.0 - 3.0 * occ, 0.0, 1.0) * (0.5 + 0.5 * nor.y);
}

float checkersGradBox(vec2 p, vec2 dpdx, vec2 dpdy) {
    vec2 w = abs(dpdx) + abs(dpdy) + 0.001;
    vec2 i = 2.0 * (abs(fract((p - 0.5 * w) * 0.5) - 0.5) - abs(fract((p + 0.5 * w) * 0.5) - 0.5)) / w;
    return 0.5 - 0.5 * i.x * i.y;
}

vec3 render(vec3 ro, vec3 rd, vec3 rdx, vec3 rdy) {
    vec3 col = vec3(0.7, 0.7, 0.9) - max(rd.y, 0.0) * 0.3;
    vec2 res = raycast(ro, rd);
    float t = res.x;
    float m = res.y;
    if (m > -0.5) {
        vec3 pos = ro + t * rd;
        vec3 nor = (m < 1.5) ? vec3(0.0, 1.0, 0.0) : calcNormal(pos);
        vec3 ref = reflect(rd, nor);
        col = 0.2 + 0.2 * sin(m * 2.0 + vec3(0.0, 1.0, 2.0));
        float ks = 1.0;
        if (m < 1.5) {
            vec3 dpdx = ro.y * (rd / rd.y - rdx / rdx.y);
            vec3 dpdy = ro.y * (rd / rd.y - rdy / rdy.y);
            float f = checkersGradBox(3.0 * pos.xz, 3.0 * dpdx.xz, 3.0 * dpdy.xz);
            col = 0.15 + f * vec3(0.05);
            ks = 0.4;
        }
        float occ = calcAO(pos, nor);
        vec3 lin = vec3(0.0);
        {
            vec3 lig = normalize(vec3(-0.5, 0.4, -0.6));
            vec3 hal = normalize(lig - rd);
            float dif = clamp(dot(nor, lig), 0.0, 1.0);
            dif *= calcSoftshadow(pos, lig, 0.02, 2.5);
            float spe = pow(clamp(dot(nor, hal), 0.0, 1.0), 16.0);
            spe *= dif;
            spe *= 0.04 + 0.96 * pow(clamp(1.0 - dot(hal, lig), 0.0, 1.0), 5.0);
            lin += col * 2.20 * dif * vec3(1.30, 1.00, 0.70);
            lin += 5.00 * spe * vec3(1.30, 1.00, 0.70) * ks;
        }
        {
            float dif = sqrt(clamp(0.5 + 0.5 * nor.y, 0.0, 1.0));
            dif *= occ;
            float spe = smoothstep(-0.2, 0.2, ref.y);
            spe *= dif;
            spe *= 0.04 + 0.96 * pow(clamp(1.0 + dot(nor, rd), 0.0, 1.0), 5.0);
            spe *= calcSoftshadow(pos, ref, 0.02, 2.5);
            lin += col * 0.60 * dif * vec3(0.40, 0.60, 1.15);
            lin += 2.00 * spe * vec3(0.40, 0.60, 1.30) * ks;
        }
        {
            float dif = clamp(dot(nor, normalize(vec3(0.5, 0.0, 0.6))), 0.0, 1.0) * clamp(1.0 - pos.y, 0.0, 1.0);
            dif *= occ;
            lin += col * 0.55 * dif * vec3(0.25, 0.25, 0.25);
        }
        {
            float dif = pow(clamp(1.0 + dot(nor, rd), 0.0, 1.0), 2.0);
            dif *= occ;
            lin += col * 0.25 * dif * vec3(1.00, 1.00, 1.00);
        }
        col = lin;
        col = mix(col, vec3(0.7, 0.7, 0.9), 1.0 - exp(-0.0001 * t * t * t));
    }
    return clamp(col, 0.0, 1.0);
}

mat3 setCamera(vec3 ro, vec3 ta, float cr) {
    vec3 cw = normalize(ta - ro);
    vec3 cp = vec3(sin(cr), cos(cr), 0.0);
    vec3 cu = normalize(cross(cw, cp));
    vec3 cv = cross(cu, cw);
    return mat3(cu, cv, cw);
}

void mainImage(out vec4 fragColor, in vec2 fragCoord) {
    vec2 mo = iMouse.xy / iResolution.xy;
    float time = 32.0 + iTime * 1.5;
    vec3 ta = vec3(0.25, -0.75, -0.75);
    vec3 ro = ta + vec3(4.5 * cos(0.1 * time + 7.0 * mo.x), 2.2, 4.5 * sin(0.1 * time + 7.0 * mo.x));
    mat3 ca = setCamera(ro, ta, 0.0);
    float sres = min(iResolution.x, iResolution.y);
    vec2 p = (2.0 * fragCoord - iResolution.xy) / sres;
    float fl = 2.5;
    vec3 rd = ca * normalize(vec3(p, fl));
    vec2 px = (2.0 * (fragCoord + vec2(1.0, 0.0)) - iResolution.xy) / sres;
    vec2 py = (2.0 * (fragCoord + vec2(0.0, 1.0)) - iResolution.xy) / sres;
    vec3 rdx = ca * normalize(vec3(px, fl));
    vec3 rdy = ca * normalize(vec3(py, fl));
    vec3 col = render(ro, rd, rdx, rdy);
    fragColor = vec4(col, 1.0);
}
""".trimIndent()

    /**
     * WGSL 同构（精简注释）；弱端同样重。MIT 同 LITE。
     * 为控制体积：软阴影步进 16、raymarch 56，观感接近。
     */
    val RUNTIME_FS: String = """
// The MIT License
// Copyright © 2013 Inigo Quilez
// Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions: The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software. THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
// Author: Inigo Quilez · Source: shadertoy.com (https://www.shadertoy.com/view/Xds3zN)
// Also: https://www.shadertoy.com/playlist/43cXRl · https://iquilezles.org/articles/distfunctions
// Holm Runtime WGSL port of the above SDF demo.

fn dot2(v: vec2f) -> f32 { return dot(v, v); }
fn dot3(v: vec3f) -> f32 { return dot(v, v); }
fn ndot(a: vec2f, b: vec2f) -> f32 { return a.x * b.x - a.y * b.y; }

fn sdSphere(p: vec3f, s: f32) -> f32 { return length(p) - s; }

fn sdBox(p: vec3f, b: vec3f) -> f32 {
    let d = abs(p) - b;
    return min(max(d.x, max(d.y, d.z)), 0.0) + length(max(d, vec3f(0.0)));
}

fn sdBoxFrame(p0: vec3f, b: vec3f, e: f32) -> f32 {
    var p = abs(p0) - b;
    let q = abs(p + e) - e;
    return min(min(
        length(max(vec3f(p.x, q.y, q.z), vec3f(0.0))) + min(max(p.x, max(q.y, q.z)), 0.0),
        length(max(vec3f(q.x, p.y, q.z), vec3f(0.0))) + min(max(q.x, max(p.y, q.z)), 0.0)),
        length(max(vec3f(q.x, q.y, p.z), vec3f(0.0))) + min(max(q.x, max(q.y, p.z)), 0.0));
}

fn sdEllipsoid(p: vec3f, r: vec3f) -> f32 {
    let k0 = length(p / r);
    let k1 = length(p / (r * r));
    return k0 * (k0 - 1.0) / k1;
}

fn sdTorus(p: vec3f, t: vec2f) -> f32 {
    return length(vec2f(length(p.xz) - t.x, p.y)) - t.y;
}

fn sdCappedTorus(p0: vec3f, sc: vec2f, ra: f32, rb: f32) -> f32 {
    var p = p0;
    p.x = abs(p.x);
    var k = length(p.xy);
    if (sc.y * p.x > sc.x * p.y) {
        k = dot(p.xy, sc);
    }
    return sqrt(dot(p, p) + ra * ra - 2.0 * ra * k) - rb;
}

fn sdHexPrism(p0: vec3f, h: vec2f) -> f32 {
    let k = vec3f(-0.8660254, 0.5, 0.57735);
    var p = abs(p0);
    p = vec3f(p.xy - 2.0 * min(dot(k.xy, p.xy), 0.0) * k.xy, p.z);
    let d = vec2f(
        length(p.xy - vec2f(clamp(p.x, -k.z * h.x, k.z * h.x), h.x)) * sign(p.y - h.x),
        p.z - h.y);
    return min(max(d.x, d.y), 0.0) + length(max(d, vec2f(0.0)));
}

fn sdOctogonPrism(p0: vec3f, r: f32, h: f32) -> f32 {
    let k = vec3f(-0.9238795325, 0.3826834323, 0.4142135623);
    var p = abs(p0);
    p = vec3f(p.xy - 2.0 * min(dot(vec2f(k.x, k.y), p.xy), 0.0) * vec2f(k.x, k.y), p.z);
    p = vec3f(p.xy - 2.0 * min(dot(vec2f(-k.x, k.y), p.xy), 0.0) * vec2f(-k.x, k.y), p.z);
    p = vec3f(p.xy - vec2f(clamp(p.x, -k.z * r, k.z * r), r), p.z);
    let d = vec2f(length(p.xy) * sign(p.y), p.z - h);
    return min(max(d.x, d.y), 0.0) + length(max(d, vec2f(0.0)));
}

fn sdCapsule(p: vec3f, a: vec3f, b: vec3f, r: f32) -> f32 {
    let pa = p - a;
    let ba = b - a;
    let h = clamp(dot(pa, ba) / dot(ba, ba), 0.0, 1.0);
    return length(pa - ba * h) - r;
}

fn sdRoundCone(p: vec3f, r1: f32, r2: f32, h: f32) -> f32 {
    let q = vec2f(length(p.xz), p.y);
    let b = (r1 - r2) / h;
    let a = sqrt(1.0 - b * b);
    let k = dot(q, vec2f(-b, a));
    if (k < 0.0) { return length(q) - r1; }
    if (k > a * h) { return length(q - vec2f(0.0, h)) - r2; }
    return dot(q, vec2f(a, b)) - r1;
}

fn sdRoundCone2(p: vec3f, a: vec3f, b: vec3f, r1: f32, r2: f32) -> f32 {
    let ba = b - a;
    let l2 = dot(ba, ba);
    let rr = r1 - r2;
    let a2 = l2 - rr * rr;
    let il2 = 1.0 / l2;
    let pa = p - a;
    let y = dot(pa, ba);
    let z = y - l2;
    let x2 = dot3(pa * l2 - ba * y);
    let y2 = y * y * l2;
    let z2 = z * z * l2;
    let k = sign(rr) * rr * rr * x2;
    if (sign(z) * a2 * z2 > k) { return sqrt(x2 + z2) * il2 - r2; }
    if (sign(y) * a2 * y2 < k) { return sqrt(x2 + y2) * il2 - r1; }
    return (sqrt(x2 * a2 * il2) + y * rr) * il2 - r1;
}

fn sdTriPrism(p0: vec3f, h0: vec2f) -> f32 {
    let k = sqrt(3.0);
    var h = h0;
    h.x = h.x * 0.5 * k;
    var p = p0;
    p = vec3f(p.xy / h.x, p.z);
    p.x = abs(p.x) - 1.0;
    p.y = p.y + 1.0 / k;
    if (p.x + k * p.y > 0.0) {
        p = vec3f(vec2f(p.x - k * p.y, -k * p.x - p.y) / 2.0, p.z);
    }
    p.x = p.x - clamp(p.x, -2.0, 0.0);
    let d1 = length(p.xy) * sign(-p.y) * h.x;
    let d2 = abs(p.z) - h.y;
    return length(max(vec2f(d1, d2), vec2f(0.0))) + min(max(d1, d2), 0.0);
}

fn sdCylinder(p: vec3f, h: vec2f) -> f32 {
    let d = abs(vec2f(length(p.xz), p.y)) - h;
    return min(max(d.x, d.y), 0.0) + length(max(d, vec2f(0.0)));
}

fn sdCylinder2(p: vec3f, a: vec3f, b: vec3f, r: f32) -> f32 {
    let pa = p - a;
    let ba = b - a;
    let baba = dot(ba, ba);
    let paba = dot(pa, ba);
    let x = length(pa * baba - ba * paba) - r * baba;
    let y = abs(paba - baba * 0.5) - baba * 0.5;
    let x2 = x * x;
    let y2 = y * y * baba;
    var d = 0.0;
    if (max(x, y) < 0.0) {
        d = -min(x2, y2);
    } else {
        var xx = 0.0;
        var yy = 0.0;
        if (x > 0.0) { xx = x2; }
        if (y > 0.0) { yy = y2; }
        d = xx + yy;
    }
    return sign(d) * sqrt(abs(d)) / baba;
}

fn sdCone(p: vec3f, c: vec2f, h: f32) -> f32 {
    let q = h * vec2f(c.x, -c.y) / c.y;
    let w = vec2f(length(p.xz), p.y);
    let a = w - q * clamp(dot(w, q) / dot(q, q), 0.0, 1.0);
    let b = w - q * vec2f(clamp(w.x / q.x, 0.0, 1.0), 1.0);
    let k = sign(q.y);
    let d = min(dot(a, a), dot(b, b));
    let s = max(k * (w.x * q.y - w.y * q.x), k * (w.y - q.y));
    return sqrt(d) * sign(s);
}

fn sdCappedCone(p: vec3f, h: f32, r1: f32, r2: f32) -> f32 {
    let q = vec2f(length(p.xz), p.y);
    let k1 = vec2f(r2, h);
    let k2 = vec2f(r2 - r1, 2.0 * h);
    var rpick = r2;
    if (q.y < 0.0) { rpick = r1; }
    let ca = vec2f(q.x - min(q.x, rpick), abs(q.y) - h);
    let cb = q - k1 + k2 * clamp(dot(k1 - q, k2) / dot2(k2), 0.0, 1.0);
    var s = 1.0;
    if (cb.x < 0.0 && ca.y < 0.0) { s = -1.0; }
    return s * sqrt(min(dot2(ca), dot2(cb)));
}

fn sdCappedCone2(p: vec3f, a: vec3f, b: vec3f, ra: f32, rb: f32) -> f32 {
    let rba = rb - ra;
    let baba = dot(b - a, b - a);
    let papa = dot(p - a, p - a);
    let paba = dot(p - a, b - a) / baba;
    let x = sqrt(papa - paba * paba * baba);
    var rax = rb;
    if (paba < 0.5) { rax = ra; }
    let cax = max(0.0, x - rax);
    let cay = abs(paba - 0.5) - 0.5;
    let k = rba * rba + baba;
    let f = clamp((rba * (x - ra) + paba * baba) / k, 0.0, 1.0);
    let cbx = x - ra - f * rba;
    let cby = paba - f;
    var s = 1.0;
    if (cbx < 0.0 && cay < 0.0) { s = -1.0; }
    return s * sqrt(min(cax * cax + cay * cay * baba, cbx * cbx + cby * cby * baba));
}

fn sdSolidAngle(pos: vec3f, c: vec2f, ra: f32) -> f32 {
    let p = vec2f(length(pos.xz), pos.y);
    let l = length(p) - ra;
    let m = length(p - c * clamp(dot(p, c), 0.0, ra));
    return max(l, m * sign(c.y * p.x - c.x * p.y));
}

fn sdOctahedron(p0: vec3f, s: f32) -> f32 {
    var p = abs(p0);
    let m = p.x + p.y + p.z - s;
    var q = p;
    if (3.0 * p.x < m) { q = p.xyz; }
    else if (3.0 * p.y < m) { q = p.yzx; }
    else if (3.0 * p.z < m) { q = p.zxy; }
    else { return m * 0.57735027; }
    let k = clamp(0.5 * (q.z - q.y + s), 0.0, s);
    return length(vec3f(q.x, q.y - s + k, q.z - k));
}

fn sdPyramid(p0: vec3f, h: f32) -> f32 {
    let m2 = h * h + 0.25;
    var xz = abs(p0.xz);
    if (xz.y > xz.x) { xz = xz.yx; }
    xz = xz - vec2f(0.5);
    let q = vec3f(xz.y, h * p0.y - 0.5 * xz.x, h * xz.x + 0.5 * p0.y);
    let s = max(-q.x, 0.0);
    let t = clamp((q.y - 0.5 * xz.y) / (m2 + 0.25), 0.0, 1.0);
    let a = m2 * (q.x + s) * (q.x + s) + q.y * q.y;
    let b = m2 * (q.x + 0.5 * t) * (q.x + 0.5 * t) + (q.y - m2 * t) * (q.y - m2 * t);
    var d2 = min(a, b);
    if (min(q.y, -q.x * m2 - q.y * 0.5) > 0.0) { d2 = 0.0; }
    return sqrt((d2 + q.z * q.z) / m2) * sign(max(q.z, -p0.y));
}

fn sdRhombus(p0: vec3f, la: f32, lb: f32, h: f32, ra: f32) -> f32 {
    var p = abs(p0);
    let b = vec2f(la, lb);
    let f = clamp((ndot(b, b - 2.0 * p.xz)) / dot(b, b), -1.0, 1.0);
    let q = vec2f(length(p.xz - 0.5 * b * vec2f(1.0 - f, 1.0 + f)) * sign(p.x * b.y + p.z * b.x - b.x * b.y) - ra, p.y - h);
    return min(max(q.x, q.y), 0.0) + length(max(q, vec2f(0.0)));
}

fn sdHorseshoe(p0: vec3f, c: vec2f, r: f32, le: f32, w: vec2f) -> f32 {
    var p = p0;
    p.x = abs(p.x);
    let l = length(p.xy);
    let m = mat2x2f(-c.x, c.y, c.y, c.x);
    p = vec3f(m * p.xy, p.z);
    var px = p.x;
    var py = p.y;
    if (!(p.y > 0.0 || p.x > 0.0)) { px = l * sign(-c.x); }
    if (!(p.x > 0.0)) { py = l; }
    p = vec3f(px, abs(py - r) - 0.0, p.z);
    p = vec3f(p.x - le, p.y, p.z);
    let q = vec2f(length(max(p.xy, vec2f(0.0))) + min(0.0, max(p.x, p.y)), p.z);
    let d = abs(q) - w;
    return min(max(d.x, d.y), 0.0) + length(max(d, vec2f(0.0)));
}

fn opU(d1: vec2f, d2: vec2f) -> vec2f {
    if (d1.x < d2.x) { return d1; }
    return d2;
}

fn map(pos: vec3f) -> vec2f {
    var res = vec2f(pos.y, 0.0);
    if (sdBox(pos - vec3f(-2.0, 0.3, 0.25), vec3f(0.3, 0.3, 1.0)) < res.x) {
        res = opU(res, vec2f(sdSphere(pos - vec3f(-2.0, 0.25, 0.0), 0.25), 26.9));
        res = opU(res, vec2f(sdRhombus((pos - vec3f(-2.0, 0.25, 1.0)).xzy, 0.15, 0.25, 0.04, 0.08), 17.0));
    }
    if (sdBox(pos - vec3f(0.0, 0.3, -1.0), vec3f(0.35, 0.3, 2.5)) < res.x) {
        res = opU(res, vec2f(sdCappedTorus((pos - vec3f(0.0, 0.30, 1.0)) * vec3f(1.0, -1.0, 1.0), vec2f(0.866025, -0.5), 0.25, 0.05), 25.0));
        res = opU(res, vec2f(sdBoxFrame(pos - vec3f(0.0, 0.25, 0.0), vec3f(0.3, 0.25, 0.2), 0.025), 16.9));
        res = opU(res, vec2f(sdCone(pos - vec3f(0.0, 0.45, -1.0), vec2f(0.6, 0.8), 0.45), 55.0));
        res = opU(res, vec2f(sdCappedCone(pos - vec3f(0.0, 0.25, -2.0), 0.25, 0.25, 0.1), 13.67));
        res = opU(res, vec2f(sdSolidAngle(pos - vec3f(0.0, 0.00, -3.0), vec2f(3.0, 4.0) / 5.0, 0.4), 49.13));
    }
    if (sdBox(pos - vec3f(1.0, 0.3, -1.0), vec3f(0.35, 0.3, 2.5)) < res.x) {
        res = opU(res, vec2f(sdTorus((pos - vec3f(1.0, 0.30, 1.0)).xzy, vec2f(0.25, 0.05)), 7.1));
        res = opU(res, vec2f(sdBox(pos - vec3f(1.0, 0.25, 0.0), vec3f(0.3, 0.25, 0.1)), 3.0));
        res = opU(res, vec2f(sdCapsule(pos - vec3f(1.0, 0.00, -1.0), vec3f(-0.1, 0.1, -0.1), vec3f(0.2, 0.4, 0.2), 0.1), 31.9));
        res = opU(res, vec2f(sdCylinder(pos - vec3f(1.0, 0.25, -2.0), vec2f(0.15, 0.25)), 8.0));
        res = opU(res, vec2f(sdHexPrism(pos - vec3f(1.0, 0.2, -3.0), vec2f(0.2, 0.05)), 18.4));
    }
    if (sdBox(pos - vec3f(-1.0, 0.35, -1.0), vec3f(0.35, 0.35, 2.5)) < res.x) {
        res = opU(res, vec2f(sdPyramid(pos - vec3f(-1.0, -0.6, -3.0), 1.0), 13.56));
        res = opU(res, vec2f(sdOctahedron(pos - vec3f(-1.0, 0.15, -2.0), 0.35), 23.56));
        res = opU(res, vec2f(sdTriPrism(pos - vec3f(-1.0, 0.15, -1.0), vec2f(0.3, 0.05)), 43.5));
        res = opU(res, vec2f(sdEllipsoid(pos - vec3f(-1.0, 0.25, 0.0), vec3f(0.2, 0.25, 0.05)), 43.17));
        res = opU(res, vec2f(sdHorseshoe(pos - vec3f(-1.0, 0.25, 1.0), vec2f(cos(1.3), sin(1.3)), 0.2, 0.3, vec2f(0.03, 0.08)), 11.5));
    }
    if (sdBox(pos - vec3f(2.0, 0.3, -1.0), vec3f(0.35, 0.3, 2.5)) < res.x) {
        res = opU(res, vec2f(sdOctogonPrism(pos - vec3f(2.0, 0.2, -3.0), 0.2, 0.05), 51.8));
        res = opU(res, vec2f(sdCylinder2(pos - vec3f(2.0, 0.14, -2.0), vec3f(0.1, -0.1, 0.0), vec3f(-0.2, 0.35, 0.1), 0.08), 31.2));
        res = opU(res, vec2f(sdCappedCone2(pos - vec3f(2.0, 0.09, -1.0), vec3f(0.1, 0.0, 0.0), vec3f(-0.2, 0.40, 0.1), 0.15, 0.05), 46.1));
        res = opU(res, vec2f(sdRoundCone2(pos - vec3f(2.0, 0.15, 0.0), vec3f(0.1, 0.0, 0.0), vec3f(-0.1, 0.35, 0.1), 0.15, 0.05), 51.7));
        res = opU(res, vec2f(sdRoundCone(pos - vec3f(2.0, 0.20, 1.0), 0.2, 0.1, 0.3), 37.0));
    }
    return res;
}

fn iBoxHit(ro: vec3f, rd: vec3f, rad: vec3f) -> vec2f {
    let m = 1.0 / rd;
    let n = m * ro;
    let k = abs(m) * rad;
    let t1 = -n - k;
    let t2 = -n + k;
    return vec2f(max(max(t1.x, t1.y), t1.z), min(min(t2.x, t2.y), t2.z));
}

fn raycast(ro: vec3f, rd: vec3f) -> vec2f {
    var res = vec2f(-1.0, -1.0);
    var tmin = 1.0;
    var tmax = 20.0;
    let tp1 = (0.0 - ro.y) / rd.y;
    if (tp1 > 0.0) {
        tmax = min(tmax, tp1);
        res = vec2f(tp1, 1.0);
    }
    let tb = iBoxHit(ro - vec3f(0.0, 0.4, -0.5), rd, vec3f(2.5, 0.41, 3.0));
    if (tb.x < tb.y && tb.y > 0.0 && tb.x < tmax) {
        tmin = max(tb.x, tmin);
        tmax = min(tb.y, tmax);
        var t = tmin;
        for (var i = 0; i < 56; i = i + 1) {
            if (t >= tmax) { break; }
            let h = map(ro + rd * t);
            if (abs(h.x) < (0.0001 * t)) {
                res = vec2f(t, h.y);
                break;
            }
            t = t + h.x;
        }
    }
    return res;
}

fn calcSoftshadow(ro: vec3f, rd: vec3f, mint: f32, tmax0: f32) -> f32 {
    var tmax = tmax0;
    let tp = (0.8 - ro.y) / rd.y;
    if (tp > 0.0) { tmax = min(tmax, tp); }
    var res = 1.0;
    var t = mint;
    for (var i = 0; i < 16; i = i + 1) {
        let h = map(ro + rd * t).x;
        let s = clamp(8.0 * h / t, 0.0, 1.0);
        res = min(res, s);
        t = t + clamp(h, 0.01, 0.2);
        if (res < 0.004 || t > tmax) { break; }
    }
    res = clamp(res, 0.0, 1.0);
    return res * res * (3.0 - 2.0 * res);
}

fn calcNormal(pos: vec3f) -> vec3f {
    let e = vec2f(1.0, -1.0) * 0.5773 * 0.0005;
    return normalize(
        e.xyy * map(pos + e.xyy).x +
        e.yyx * map(pos + e.yyx).x +
        e.yxy * map(pos + e.yxy).x +
        e.xxx * map(pos + e.xxx).x);
}

fn calcAO(pos: vec3f, nor: vec3f) -> f32 {
    var occ = 0.0;
    var sca = 1.0;
    for (var i = 0; i < 5; i = i + 1) {
        let h = 0.01 + 0.12 * f32(i) / 4.0;
        let d = map(pos + h * nor).x;
        occ = occ + (h - d) * sca;
        sca = sca * 0.95;
        if (occ > 0.35) { break; }
    }
    return clamp(1.0 - 3.0 * occ, 0.0, 1.0) * (0.5 + 0.5 * nor.y);
}

fn checkersGradBox(p: vec2f, dpdx: vec2f, dpdy: vec2f) -> f32 {
    let w = abs(dpdx) + abs(dpdy) + 0.001;
    let i = 2.0 * (abs(fract((p - 0.5 * w) * 0.5) - 0.5) - abs(fract((p + 0.5 * w) * 0.5) - 0.5)) / w;
    return 0.5 - 0.5 * i.x * i.y;
}

fn render(ro: vec3f, rd: vec3f, rdx: vec3f, rdy: vec3f) -> vec3f {
    var col = vec3f(0.7, 0.7, 0.9) - max(rd.y, 0.0) * 0.3;
    let res = raycast(ro, rd);
    let t = res.x;
    let m = res.y;
    if (m > -0.5) {
        let pos = ro + t * rd;
        var nor = vec3f(0.0, 1.0, 0.0);
        if (m >= 1.5) { nor = calcNormal(pos); }
        // WGSL: `ref` 保留字，勿作标识符
        let refl = reflect(rd, nor);
        col = 0.2 + 0.2 * sin(m * 2.0 + vec3f(0.0, 1.0, 2.0));
        var ks = 1.0;
        if (m < 1.5) {
            let dpdx = ro.y * (rd / rd.y - rdx / rdx.y);
            let dpdy = ro.y * (rd / rd.y - rdy / rdy.y);
            let f = checkersGradBox(3.0 * pos.xz, 3.0 * dpdx.xz, 3.0 * dpdy.xz);
            col = 0.15 + f * vec3f(0.05);
            ks = 0.4;
        }
        let occ = calcAO(pos, nor);
        var lin = vec3f(0.0);
        {
            let lig = normalize(vec3f(-0.5, 0.4, -0.6));
            let hal = normalize(lig - rd);
            var dif = clamp(dot(nor, lig), 0.0, 1.0);
            dif = dif * calcSoftshadow(pos, lig, 0.02, 2.5);
            var spe = pow(clamp(dot(nor, hal), 0.0, 1.0), 16.0);
            spe = spe * dif;
            spe = spe * (0.04 + 0.96 * pow(clamp(1.0 - dot(hal, lig), 0.0, 1.0), 5.0));
            lin = lin + col * 2.20 * dif * vec3f(1.30, 1.00, 0.70);
            lin = lin + 5.00 * spe * vec3f(1.30, 1.00, 0.70) * ks;
        }
        {
            var dif = sqrt(clamp(0.5 + 0.5 * nor.y, 0.0, 1.0));
            dif = dif * occ;
            var spe = smoothstep(-0.2, 0.2, refl.y);
            spe = spe * dif;
            spe = spe * (0.04 + 0.96 * pow(clamp(1.0 + dot(nor, rd), 0.0, 1.0), 5.0));
            spe = spe * calcSoftshadow(pos, refl, 0.02, 2.5);
            lin = lin + col * 0.60 * dif * vec3f(0.40, 0.60, 1.15);
            lin = lin + 2.00 * spe * vec3f(0.40, 0.60, 1.30) * ks;
        }
        {
            var dif = clamp(dot(nor, normalize(vec3f(0.5, 0.0, 0.6))), 0.0, 1.0) * clamp(1.0 - pos.y, 0.0, 1.0);
            dif = dif * occ;
            lin = lin + col * 0.55 * dif * vec3f(0.25, 0.25, 0.25);
        }
        {
            var dif = pow(clamp(1.0 + dot(nor, rd), 0.0, 1.0), 2.0);
            dif = dif * occ;
            lin = lin + col * 0.25 * dif * vec3f(1.00, 1.00, 1.00);
        }
        col = lin;
        col = mix(col, vec3f(0.7, 0.7, 0.9), 1.0 - exp(-0.0001 * t * t * t));
    }
    return clamp(col, vec3f(0.0), vec3f(1.0));
}

fn setCamera(ro: vec3f, ta: vec3f, cr: f32) -> mat3x3f {
    let cw = normalize(ta - ro);
    let cp = vec3f(sin(cr), cos(cr), 0.0);
    let cu = normalize(cross(cw, cp));
    let cv = cross(cu, cw);
    return mat3x3f(cu, cv, cw);
}

@fragment
fn fs_main(in: VsOut) -> @location(0) vec4f {
    let res = max(u.resolution, vec2f(1.0, 1.0));
    let fragCoord = in.uv * res;
    let time = 32.0 + u.time * 1.5;
    let ta = vec3f(0.25, -0.75, -0.75);
    let ro = ta + vec3f(4.5 * cos(0.1 * time), 2.2, 4.5 * sin(0.1 * time));
    let ca = setCamera(ro, ta, 0.0);
    let sres = min(res.x, res.y);
    let p = (2.0 * fragCoord - res) / sres;
    let fl = 2.5;
    let rd = ca * normalize(vec3f(p, fl));
    let px = (2.0 * (fragCoord + vec2f(1.0, 0.0)) - res) / sres;
    let py = (2.0 * (fragCoord + vec2f(0.0, 1.0)) - res) / sres;
    let rdx = ca * normalize(vec3f(px, fl));
    let rdy = ca * normalize(vec3f(py, fl));
    let col = render(ro, rd, rdx, rdy);
    return vec4f(col, 1.0);
}
""".trimIndent()
}
