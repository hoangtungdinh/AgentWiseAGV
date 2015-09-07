package test;

import multistage.garagemodel.delegatemas.AGVSystem;
import setting.Setting;

public class Test {

  public static void main(String[] args) {
    final Setting setting = new Setting.SettingBuilder()
        .setNumOfAGVs(15).setSeed(2490143).build();
    final multistage.garagemodel.delegatemas.AGVSystem agvSystem = new AGVSystem(setting, true);
    agvSystem.run();
  }
  
}
