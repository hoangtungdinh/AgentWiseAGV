package test;

import setting.Setting;
import singlestage.delegatemas.AGVSystem;

public class Test {

  public static void main(String[] args) {
    System.out.println("start");
    final Setting setting = new Setting.SettingBuilder()
        .setNumOfAGVs(10).setSeed(1737944).build();
    final singlestage.delegatemas.AGVSystem agvSystem = new AGVSystem(setting, true);
    agvSystem.run();
    System.out.println("done");
  }
  
}
