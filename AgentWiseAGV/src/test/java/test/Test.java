package test;

import multistage.delegatemas.AGVSystem;
import multistage.result.Result;
import setting.Setting;

public class Test {

  public static void main(String[] args) {
    final Setting setting = new Setting.SettingBuilder()
        .setNumOfAGVs(90).setSeed(2777159).build();
    final multistage.delegatemas.AGVSystem agvSystem = new AGVSystem(setting);
    final Result result = agvSystem.run();
  }
  
}
