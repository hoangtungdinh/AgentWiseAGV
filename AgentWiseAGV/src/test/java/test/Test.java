package test;

import setting.Setting;

public class Test {

  public static void main(String[] args) {
    final Setting setting = new Setting.SettingBuilder()
        .setNumOfAGVs(20).setSeed(7472239).build();
    // 20 9776449
    final singlestage.delegatemas.AGVSystem agvSystem = new singlestage.delegatemas.AGVSystem(setting, true);
//    final singlestage.contextaware.AGVSystem agvSystem = new singlestage.contextaware.AGVSystem(setting, true);
    agvSystem.run();
  }
  
}
