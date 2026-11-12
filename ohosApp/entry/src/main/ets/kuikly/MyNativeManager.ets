import { KuiklyNativeManager } from '@kuikly-open/render';
import Napi from 'libentry.so';
import { ResfileHolder } from './ResfileHolder';

class MyNativeManager extends KuiklyNativeManager {
  protected loadNative(): number {
    // 空壳：仅注入目录占位（若 NAPI 导出存在）；无放大镜 HAR
    if (ResfileHolder.dir.length > 0 || ResfileHolder.cacheDir.length > 0) {
      if (typeof Napi.setHolmOhosDirs === 'function') {
        Napi.setHolmOhosDirs(ResfileHolder.dir, ResfileHolder.cacheDir);
      }
    }
    return Napi.initKuikly();
  }
}

const globalNativeManager = new MyNativeManager();

export default globalNativeManager;
