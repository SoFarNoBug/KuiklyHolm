import AbilityConstant from '@ohos.app.ability.AbilityConstant';
import hilog from '@ohos.hilog';
import UIAbility from '@ohos.app.ability.UIAbility';
import Want from '@ohos.app.ability.Want';
import window from '@ohos.window';
import fs from '@ohos.file.fs';
import { KuiklyRenderAdapterManager } from '@kuikly-open/render';
import { LogAdapter } from '../kuikly/adapter/LogAdapter';
import { RouterAdapter } from '../kuikly/adapter/RouterAdapter';
import { ResfileHolder } from '../kuikly/ResfileHolder';

const MODEL_ASSETS: string[] = [
  'models/Duck.glb',
  'models/Flamingo.glb',
  'models/Horse.glb',
  'models/LittlestTokyo.glb',
  'models/Parrot.glb',
  'models/PrimaryIonDrive.glb',
  'models/SittingBox.glb',
  'models/Soldier.glb',
  'models/Stork.glb'
];

export default class EntryAbility extends UIAbility {
  onCreate(want: Want, launchParam: AbilityConstant.LaunchParam): void {
    hilog.info(0x0000, 'testTag', '%{public}s', 'Ability onCreate');
    // 早于 Stage；实际 NAPI 注入在 MyNativeManager.loadNative（so 已加载）
    ResfileHolder.dir = this.context.resourceDir;
    ResfileHolder.cacheDir = this.context.cacheDir;
    this.copyModelRawfilesToCache();
  }

  private copyModelRawfilesToCache(): void {
    const cacheRoot = `${this.context.cacheDir}/models`;
    try {
      if (!fs.accessSync(cacheRoot)) {
        fs.mkdirSync(cacheRoot);
      }
    } catch (_e) {
      try {
        fs.mkdirSync(cacheRoot);
      } catch (_e2) {
        hilog.error(0x0000, 'testTag', 'mkdir models cache failed');
        return;
      }
    }
    for (let i = 0; i < MODEL_ASSETS.length; i++) {
      const asset = MODEL_ASSETS[i];
      const name = asset.substring(asset.lastIndexOf('/') + 1);
      const dest = `${cacheRoot}/${name}`;
      try {
        if (fs.accessSync(dest)) {
          continue;
        }
      } catch (_e) {
        // not exists
      }
      try {
        const bytes = this.context.resourceManager.getRawFileContentSync(asset);
        const file = fs.openSync(dest, fs.OpenMode.CREATE | fs.OpenMode.WRITE_ONLY);
        fs.writeSync(file.fd, bytes.buffer);
        fs.closeSync(file);
      } catch (e) {
        hilog.error(0x0000, 'testTag', 'copy %{public}s failed: %{public}s', asset, String(e));
      }
    }
  }

  onDestroy(): void {
    hilog.info(0x0000, 'testTag', '%{public}s', 'Ability onDestroy');
  }

  onWindowStageCreate(windowStage: window.WindowStage): void {
    // Main window is created, set main page for this ability
    hilog.info(0x0000, 'testTag', '%{public}s', 'Ability onWindowStageCreate');
    windowStage.getMainWindowSync().setWindowLayoutFullScreen(true);

    KuiklyRenderAdapterManager.krLogAdapter = new LogAdapter();
    KuiklyRenderAdapterManager.krRouterAdapter = new RouterAdapter();

    windowStage.loadContent('pages/Index', (err, data) => {
      if (err.code) {
        hilog.error(0x0000, 'testTag', 'Failed to load the content. Cause: %{public}s', JSON.stringify(err) ?? '');
        return;
      }
      hilog.info(0x0000, 'testTag', 'Succeeded in loading the content. Data: %{public}s', JSON.stringify(data) ?? '');
    });
  }

  onWindowStageDestroy(): void {
    // Main window is destroyed, release UI related resources
    hilog.info(0x0000, 'testTag', '%{public}s', 'Ability onWindowStageDestroy');
  }

  onForeground(): void {
    // Ability has brought to foreground
    hilog.info(0x0000, 'testTag', '%{public}s', 'Ability onForeground');
  }

  onBackground(): void {
    // Ability has back to background
    hilog.info(0x0000, 'testTag', '%{public}s', 'Ability onBackground');
  }
}