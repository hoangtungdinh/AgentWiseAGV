package test;

import setting.Setting;
import singlestage.delegatemas.AGVSystem;

public class Test {

  public static void main(String[] args) {
    final Setting setting = new Setting.SettingBuilder()
        .setNumOfAGVs(50).setSeed(6779059).build();
    final singlestage.delegatemas.AGVSystem agvSystem = new AGVSystem(setting, true);
    agvSystem.run();
  }
  
}
