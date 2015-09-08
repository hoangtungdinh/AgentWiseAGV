package test;

import multistage.garagemodel.contextaware.AGVSystem;
import setting.Setting;

public class Test {

  public static void main(String[] args) {
    System.out.println("start");
    final Setting setting = new Setting.SettingBuilder()
        .setNumOfAGVs(5).setSeed(8496955).build();
    final multistage.garagemodel.contextaware.AGVSystem agvSystem = new AGVSystem(setting, false);
    agvSystem.run();
    System.out.println("done");
  }
  
}
