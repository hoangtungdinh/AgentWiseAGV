package test;

import setting.Setting;

public class Test {

  public static void main(String[] args) {
    final Setting setting = new Setting.SettingBuilder()
        .setNumOfAGVs(20).setSeed(1).setEndTime(100000000).build();
    // 20 9776449
//    final singlestage.contextaware.AGVSystem agvSystem = new singlestage.contextaware.AGVSystem(setting, true);
//    final singlestage.delegatemas.AGVSystem agvSystem = new singlestage.delegatemas.AGVSystem(setting, true);
//    final multistage.garagemodel.contextaware.waiting.makespanandplancost.AGVSystem agvSystem = new multistage.garagemodel.contextaware.waiting.makespanandplancost.AGVSystem(setting, true);
//    final multistage.garagemodel.delegatemas.AGVSystem agvSystem = new multistage.garagemodel.delegatemas.AGVSystem(setting, true);
//    final multistage.garagemodel.contextaware.repair.makespanandplancost.AGVSystem agvSystem = new multistage.garagemodel.contextaware.repair.makespanandplancost.AGVSystem(setting, true);
    final multistage.garagemodel.contextaware.repair.throughput.AGVSystem agvSystem = new multistage.garagemodel.contextaware.repair.throughput.AGVSystem(setting, true);
    agvSystem.run();
  }
  
}
