package test;

import setting.Setting;

public class Test {

  public static void main(String[] args) {
    final Setting setting = new Setting.SettingBuilder()
        .setNumOfAGVs(10).setSeed(9757293).build();
    // 20 9776449
//    final multistage.garagemodel.contextaware.AGVSystem agvSystem = new multistage.garagemodel.contextaware.AGVSystem(setting, true);
    final multistage.garagemodel.delegatemas.AGVSystem agvSystem = new multistage.garagemodel.delegatemas.AGVSystem(setting, true);
    agvSystem.run();
  }
  
}
