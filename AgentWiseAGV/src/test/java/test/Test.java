package test;

import multistage.garagemodel.delegatemas.AGVSystem;
import setting.Setting;

public class Test {

  public static void main(String[] args) {
    System.out.println("start");
    final Setting setting = new Setting.SettingBuilder()
        .setNumOfAGVs(100).setSeed(4010001).build();
    final AGVSystem agvSystem = new AGVSystem(setting, false);
    agvSystem.run();
    System.out.println("done");
  }
  
}
