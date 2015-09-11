package test;

import setting.Setting;
import singlestage.contextaware.AGVSystem;

public class Test {

  public static void main(String[] args) {
    System.out.println("start");
    final Setting setting = new Setting.SettingBuilder()
        .setNumOfAGVs(100).setSeed(1249843).build();
    final singlestage.contextaware.AGVSystem agvSystem = new AGVSystem(setting, true);
    agvSystem.run();
    System.out.println("done");
  }
  
}
