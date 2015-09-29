package test;

import setting.Setting;

public class Test {

  public static void main(String[] args) {
    final Setting setting = new Setting.SettingBuilder()
        .setNumOfAGVs(10).setSeed(8496955).build();
    // 20 9776449
//    final singlestage.contextaware.AGVSystem agvSystem = new singlestage.contextaware.AGVSystem(setting, true);
//    final singlestage.delegatemas.AGVSystem agvSystem = new singlestage.delegatemas.AGVSystem(setting, true);
//    final multistage.garagemodel.contextaware.waiting.AGVSystem agvSystem = new multistage.garagemodel.contextaware.waiting.AGVSystem(setting, true);
//    final multistage.garagemodel.delegatemas.AGVSystem agvSystem = new multistage.garagemodel.delegatemas.AGVSystem(setting, true);
    final multistage.garagemodel.contextaware.repair.AGVSystem agvSystem = new multistage.garagemodel.contextaware.repair.AGVSystem(setting, true);
    agvSystem.run();
  }
  
}
