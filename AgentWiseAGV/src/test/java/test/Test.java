package test;

import setting.Setting;

public class Test {

  public static void main(String[] args) {
    final Setting setting = new Setting.SettingBuilder()
        .setNumOfAGVs(1).setSeed(3).build();
    // 20 9776449
    final multistage.garagemodel.contextaware.waiting.AGVSystem agvSystem = new multistage.garagemodel.contextaware.waiting.AGVSystem(setting, true);
//    final multistage.garagemodel.delegatemas.AGVSystem agvSystem = new multistage.garagemodel.delegatemas.AGVSystem(setting, true);
    agvSystem.run();
  }
  
}
