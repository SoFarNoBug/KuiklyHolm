package com.jlj.kuiklybase.holm

/**
 * @CreateDate: 2026/11/20
 * @Author: jlj
 * @Description: H5 Model GLB 解析 Worker 源码（Blob Worker；Draco 经 importScripts；ADR 0020 P1）。
 */
internal object HolmJsModelParseWorkerSource {

    /**
     * 经典 Worker 脚本：onmessage 收 ArrayBuffer → 回传 meshes + baseColor（可 transfer）。
     * Draco wasm 路径相对页面根（webpack-dev-server / 静态资源）。
     */
    val SCRIPT: String = """
self.onmessage = function (ev) {
  var msg = ev.data || {};
  var ab = msg.ab;
  var base = msg.base || '';
  if (!ab) {
    self.postMessage({ ok: false, err: 'missing ab' });
    return;
  }
  ensureDraco(base).then(function () {
    try {
      var u8 = new Uint8Array(ab);
      var doc = parseGlb(u8);
      var transfer = [];
      var meshes = [];
      for (var i = 0; i < doc.meshes.length; i++) {
        var m = doc.meshes[i];
        transfer.push(m.positions.buffer, m.normals.buffer, m.uvs.buffer, m.indices.buffer);
        meshes.push({
          name: m.name,
          positions: m.positions,
          normals: m.normals,
          uvs: m.uvs,
          indices: m.indices
        });
      }
      self.postMessage({ ok: true, baseColor: doc.baseColor, meshes: meshes }, transfer);
    } catch (e) {
      self.postMessage({ ok: false, err: (e && e.message) ? e.message : String(e) });
    }
  }).catch(function (e) {
    self.postMessage({ ok: false, err: (e && e.message) ? e.message : String(e) });
  });
};

var _dracoPromise = null;
function ensureDraco(base) {
  if (self.__kmpDraco && self.__kmpDraco.ready === true) {
    return Promise.resolve();
  }
  if (_dracoPromise) return _dracoPromise;
  var root = base || '';
  if (root && root.charAt(root.length - 1) !== '/') root += '/';
  _dracoPromise = new Promise(function (resolve, reject) {
    try {
      // Blob Worker 相对 importScripts 不可靠；须绝对 URL（主线程传 location.origin）
      importScripts(root + 'kmp-draco-native/kmp_draco_native.js');
      importScripts(root + 'kmp-draco-native/kmp_draco_glue.js');
    } catch (e) {
      reject(e);
      return;
    }
    if (typeof __kmp_draco_bindgen !== 'function') {
      reject(new Error('kmp-draco bindgen missing in worker'));
      return;
    }
    __kmp_draco_bindgen(root + 'kmp-draco-native/kmp_draco_native_bg.wasm').then(function () {
      if (self.__kmpDraco) self.__kmpDraco.ready = true;
      resolve();
    }).catch(reject);
  });
  return _dracoPromise;
}

function u32(u8, off) {
  return u8[off] | (u8[off + 1] << 8) | (u8[off + 2] << 16) | (u8[off + 3] << 24);
}

function parseGlb(u8) {
  if (u8.length < 12) throw new Error('GLB too short');
  if (u32(u8, 0) !== 0x46546C67) throw new Error('Not a GLB');
  if (u32(u8, 4) !== 2) throw new Error('Unsupported glTF version');
  var length = u32(u8, 8);
  if (length > u8.length) throw new Error('GLB length exceeds buffer');
  var off = 12;
  var jsonText = null;
  var bin = new Uint8Array(0);
  while (off + 8 <= length) {
    var chunkLen = u32(u8, off);
    var chunkType = u32(u8, off + 4);
    off += 8;
    if (off + chunkLen > length) throw new Error('Chunk overruns GLB');
    if (chunkType === 0x4E4F534A) {
      jsonText = utf8Decode(u8.subarray(off, off + chunkLen)).replace(/\0+$/, '');
    } else if (chunkType === 0x004E4942) {
      bin = u8.subarray(off, off + chunkLen);
    }
    off += chunkLen;
  }
  if (!jsonText) throw new Error('Missing JSON chunk');
  var root = JSON.parse(jsonText);
  return assemble(root, bin);
}

function utf8Decode(u8) {
  if (typeof TextDecoder !== 'undefined') {
    return new TextDecoder('utf-8').decode(u8);
  }
  var s = '';
  for (var i = 0; i < u8.length; i++) s += String.fromCharCode(u8[i]);
  try { return decodeURIComponent(escape(s)); } catch (e) { return s; }
}

function assemble(root, bin) {
  var accessors = root.accessors || [];
  var bufferViews = root.bufferViews || [];
  var meshesJson = root.meshes || [];
  var nodes = root.nodes || [];
  var scenes = root.scenes || [];
  var materials = root.materials || [];
  var baseColor = [1, 1, 1, 1];
  if (materials.length > 0) {
    var pbr = materials[0].pbrMetallicRoughness || {};
    var f = pbr.baseColorFactor;
    if (f && f.length >= 3) {
      baseColor = [f[0], f[1], f[2], f.length >= 4 ? f[3] : 1];
    }
  }
  var sceneIndex = typeof root.scene === 'number' ? root.scene : 0;
  var rootNodes;
  if (scenes.length > 0 && sceneIndex < scenes.length && scenes[sceneIndex].nodes) {
    rootNodes = scenes[sceneIndex].nodes;
  } else {
    rootNodes = [];
    for (var ni = 0; ni < nodes.length; ni++) rootNodes.push(ni);
  }
  var out = [];
  var identity = mat4Identity();
  for (var r = 0; r < rootNodes.length; r++) {
    walk(rootNodes[r], identity, nodes, meshesJson, accessors, bufferViews, bin, out);
  }
  if (out.length === 0) throw new Error('No mesh primitives found');
  return { meshes: out, baseColor: baseColor };
}

function walk(nodeIndex, parent, nodes, meshesJson, accessors, bufferViews, bin, out) {
  if (nodeIndex < 0 || nodeIndex >= nodes.length) return;
  var node = nodes[nodeIndex];
  var local = nodeMatrix(node);
  var world = mat4Mul(parent, local);
  var meshIndex = typeof node.mesh === 'number' ? node.mesh : -1;
  if (meshIndex >= 0 && meshIndex < meshesJson.length) {
    var mesh = meshesJson[meshIndex];
    var name = mesh.name || ('mesh' + meshIndex);
    var primitives = mesh.primitives || [];
    for (var pi = 0; pi < primitives.length; pi++) {
      var prim = primitives[pi];
      var attrs = prim.attributes || {};
      var posAcc = typeof attrs.POSITION === 'number' ? attrs.POSITION : -1;
      if (posAcc < 0) continue;
      var positions, normals, uvs, indices, hadNormals;
      var dracoExt = prim.extensions && prim.extensions.KHR_draco_mesh_compression;
      if (dracoExt) {
        var decoded = decodeDracoPrimitive(dracoExt, bufferViews, bin);
        var attrMap = dracoExt.attributes || {};
        positions = floatAttrFromDraco(decoded, attrMap, 'POSITION', 3);
        if (!positions) throw new Error('KHR_draco missing POSITION');
        var nFrom = floatAttrFromDraco(decoded, attrMap, 'NORMAL', 3);
        hadNormals = !!nFrom;
        normals = nFrom || new Float32Array(positions.length);
        uvs = floatAttrFromDraco(decoded, attrMap, 'TEXCOORD_0', 2) ||
          new Float32Array((positions.length / 3) * 2);
        if (decoded.indices && decoded.indices.length) {
          indices = decoded.indices;
        } else {
          indices = new Uint32Array(positions.length / 3);
          for (var ii = 0; ii < indices.length; ii++) indices[ii] = ii;
        }
      } else {
        positions = readFloat3Accessor(accessors, bufferViews, bin, posAcc);
        var nAcc = typeof attrs.NORMAL === 'number' ? attrs.NORMAL : -1;
        hadNormals = nAcc >= 0;
        normals = nAcc >= 0
          ? readFloat3Accessor(accessors, bufferViews, bin, nAcc)
          : new Float32Array(positions.length);
        var uvAcc = typeof attrs.TEXCOORD_0 === 'number' ? attrs.TEXCOORD_0 : -1;
        uvs = uvAcc >= 0
          ? readFloat2Accessor(accessors, bufferViews, bin, uvAcc)
          : new Float32Array((positions.length / 3) * 2);
        var indAcc = typeof prim.indices === 'number' ? prim.indices : -1;
        if (indAcc >= 0) {
          indices = readIndices(accessors, bufferViews, bin, indAcc);
        } else {
          indices = new Uint32Array(positions.length / 3);
          for (var jj = 0; jj < indices.length; jj++) indices[jj] = jj;
        }
      }
      if (!hadNormals) computeFlatNormals(positions, indices, normals);
      transformPositions(positions, world);
      transformNormals(normals, world);
      out.push({
        name: name + '#' + pi,
        positions: positions,
        normals: normals,
        uvs: uvs,
        indices: indices
      });
    }
  }
  var children = node.children || [];
  for (var c = 0; c < children.length; c++) {
    walk(children[c], world, nodes, meshesJson, accessors, bufferViews, bin, out);
  }
}

function decodeDracoPrimitive(dracoExt, views, bin) {
  var viewIndex = typeof dracoExt.bufferView === 'number' ? dracoExt.bufferView : -1;
  if (viewIndex < 0 || viewIndex >= views.length) {
    throw new Error('KHR_draco bufferView missing or OOB: ' + viewIndex);
  }
  var view = views[viewIndex];
  var offset = view.byteOffset || 0;
  var length = typeof view.byteLength === 'number' ? view.byteLength : -1;
  if (length < 0 || offset < 0 || offset + length > bin.length) {
    throw new Error('KHR_draco bufferView slice OOB');
  }
  var slice = bin.subarray(offset, offset + length);
  if (!self.__kmpDraco || typeof self.__kmpDraco.decode !== 'function') {
    throw new Error('kmp-draco decode unavailable in worker');
  }
  var raw = self.__kmpDraco.decode(slice);
  var idx = raw.indices || [];
  var indices = new Uint32Array(idx.length);
  for (var i = 0; i < idx.length; i++) indices[i] = idx[i] >>> 0;
  var attrs = raw.attributes || [];
  var byId = {};
  for (var a = 0; a < attrs.length; a++) {
    byId[attrs[a].uniqueId] = attrs[a];
  }
  return { numPoints: raw.numPoints | 0, indices: indices, byId: byId };
}

function floatAttrFromDraco(decoded, attrMap, semantic, components) {
  var uid = typeof attrMap[semantic] === 'number' ? attrMap[semantic] : -1;
  if (uid < 0) return null;
  var attr = decoded.byId[uid];
  if (!attr) return null;
  var src = attr.values;
  var srcComps = attr.numComponents | 0;
  var n = decoded.numPoints;
  if (srcComps < components) {
    throw new Error('KHR_draco ' + semantic + ' comps too small');
  }
  var out = new Float32Array(n * components);
  for (var i = 0; i < n; i++) {
    for (var c = 0; c < components; c++) {
      out[i * components + c] = src[i * srcComps + c];
    }
  }
  return out;
}

function accessorByteOffset(acc, views) {
  var viewIndex = typeof acc.bufferView === 'number' ? acc.bufferView : -1;
  if (viewIndex < 0 || viewIndex >= views.length) return null;
  var view = views[viewIndex];
  var viewOff = view.byteOffset || 0;
  var accOff = acc.byteOffset || 0;
  return { view: view, offset: viewOff + accOff };
}

function readFloat3Accessor(accessors, views, bin, accIndex) {
  var acc = accessors[accIndex];
  var loc = accessorByteOffset(acc, views);
  if (!loc) throw new Error('POSITION accessor has no bufferView');
  var count = acc.count | 0;
  var stride = loc.view.byteStride || 12;
  var out = new Float32Array(count * 3);
  var dv = new DataView(bin.buffer, bin.byteOffset, bin.byteLength);
  for (var i = 0; i < count; i++) {
    var o = loc.offset + i * stride;
    out[i * 3] = dv.getFloat32(o, true);
    out[i * 3 + 1] = dv.getFloat32(o + 4, true);
    out[i * 3 + 2] = dv.getFloat32(o + 8, true);
  }
  return out;
}

function readFloat2Accessor(accessors, views, bin, accIndex) {
  var acc = accessors[accIndex];
  var loc = accessorByteOffset(acc, views);
  if (!loc) throw new Error('UV accessor has no bufferView');
  var count = acc.count | 0;
  var stride = loc.view.byteStride || 8;
  var out = new Float32Array(count * 2);
  var dv = new DataView(bin.buffer, bin.byteOffset, bin.byteLength);
  for (var i = 0; i < count; i++) {
    var o = loc.offset + i * stride;
    out[i * 2] = dv.getFloat32(o, true);
    out[i * 2 + 1] = dv.getFloat32(o + 4, true);
  }
  return out;
}

function readIndices(accessors, views, bin, accIndex) {
  var acc = accessors[accIndex];
  var loc = accessorByteOffset(acc, views);
  if (!loc) throw new Error('indices accessor has no bufferView');
  var count = acc.count | 0;
  var compType = acc.componentType | 0;
  var out = new Uint32Array(count);
  var dv = new DataView(bin.buffer, bin.byteOffset, bin.byteLength);
  var stride = loc.view.byteStride || (compType === 5123 ? 2 : compType === 5121 ? 1 : 4);
  for (var i = 0; i < count; i++) {
    var o = loc.offset + i * stride;
    if (compType === 5123) out[i] = dv.getUint16(o, true);
    else if (compType === 5121) out[i] = dv.getUint8(o);
    else out[i] = dv.getUint32(o, true);
  }
  return out;
}

function computeFlatNormals(positions, indices, normals) {
  for (var i = 0; i < normals.length; i++) normals[i] = 0;
  for (var t = 0; t + 2 < indices.length; t += 3) {
    var i0 = indices[t], i1 = indices[t + 1], i2 = indices[t + 2];
    var ax = positions[i0 * 3], ay = positions[i0 * 3 + 1], az = positions[i0 * 3 + 2];
    var bx = positions[i1 * 3], by = positions[i1 * 3 + 1], bz = positions[i1 * 3 + 2];
    var cx = positions[i2 * 3], cy = positions[i2 * 3 + 1], cz = positions[i2 * 3 + 2];
    var ux = bx - ax, uy = by - ay, uz = bz - az;
    var vx = cx - ax, vy = cy - ay, vz = cz - az;
    var nx = uy * vz - uz * vy;
    var ny = uz * vx - ux * vz;
    var nz = ux * vy - uy * vx;
    normals[i0 * 3] += nx; normals[i0 * 3 + 1] += ny; normals[i0 * 3 + 2] += nz;
    normals[i1 * 3] += nx; normals[i1 * 3 + 1] += ny; normals[i1 * 3 + 2] += nz;
    normals[i2 * 3] += nx; normals[i2 * 3 + 1] += ny; normals[i2 * 3 + 2] += nz;
  }
  for (var v = 0; v < normals.length; v += 3) {
    var x = normals[v], y = normals[v + 1], z = normals[v + 2];
    var len = Math.sqrt(x * x + y * y + z * z) || 1;
    normals[v] = x / len; normals[v + 1] = y / len; normals[v + 2] = z / len;
  }
}

function nodeMatrix(node) {
  if (node.matrix && node.matrix.length === 16) {
    return node.matrix.slice();
  }
  var t = node.translation || [0, 0, 0];
  var r = node.rotation || [0, 0, 0, 1];
  var s = node.scale || [1, 1, 1];
  return mat4FromTrs(t, r, s);
}

function mat4Identity() {
  return [1,0,0,0, 0,1,0,0, 0,0,1,0, 0,0,0,1];
}

function mat4FromTrs(t, r, s) {
  // Column-major，对齐 GltfGlbParser.Mat4.fromTrs
  var x = r[0], y = r[1], z = r[2], w = r[3];
  var x2 = x + x, y2 = y + y, z2 = z + z;
  var xx = x * x2, xy = x * y2, xz = x * z2;
  var yy = y * y2, yz = y * z2, zz = z * z2;
  var wx = w * x2, wy = w * y2, wz = w * z2;
  var sx = s[0], sy = s[1], sz = s[2];
  return [
    (1 - (yy + zz)) * sx, (xy + wz) * sx, (xz - wy) * sx, 0,
    (xy - wz) * sy, (1 - (xx + zz)) * sy, (yz + wx) * sy, 0,
    (xz + wy) * sz, (yz - wx) * sz, (1 - (xx + yy)) * sz, 0,
    t[0], t[1], t[2], 1
  ];
}

function mat4Mul(a, b) {
  var o = new Array(16);
  for (var c = 0; c < 4; c++) {
    for (var r = 0; r < 4; r++) {
      o[c * 4 + r] =
        a[r] * b[c * 4] +
        a[4 + r] * b[c * 4 + 1] +
        a[8 + r] * b[c * 4 + 2] +
        a[12 + r] * b[c * 4 + 3];
    }
  }
  return o;
}

function transformPositions(pos, m) {
  for (var i = 0; i < pos.length; i += 3) {
    var x = pos[i], y = pos[i + 1], z = pos[i + 2];
    pos[i] = m[0] * x + m[4] * y + m[8] * z + m[12];
    pos[i + 1] = m[1] * x + m[5] * y + m[9] * z + m[13];
    pos[i + 2] = m[2] * x + m[6] * y + m[10] * z + m[14];
  }
}

function transformNormals(n, m) {
  for (var i = 0; i < n.length; i += 3) {
    var x = n[i], y = n[i + 1], z = n[i + 2];
    var nx = m[0] * x + m[4] * y + m[8] * z;
    var ny = m[1] * x + m[5] * y + m[9] * z;
    var nz = m[2] * x + m[6] * y + m[10] * z;
    var len = Math.sqrt(nx * nx + ny * ny + nz * nz) || 1;
    n[i] = nx / len; n[i + 1] = ny / len; n[i + 2] = nz / len;
  }
}
"""
}
