package test;

import setting.Setting;

public class Test {

  public static void main(String[] args) {
    final Setting setting = new Setting.SettingBuilder()
        .setNumOfAGVs(12).setSeed(1).build();
    // 20 9776449
    final multistage.garagemodel.delegatemas.AGVSystem agvSystem = new multistage.garagemodel.delegatemas.AGVSystem(setting, true);
//    final singlestage.contextaware.AGVSystem agvSystem = new singlestage.contextaware.AGVSystem(setting, true);
    agvSystem.run();
  }
  
}
