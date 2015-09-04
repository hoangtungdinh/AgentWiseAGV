package test;

import setting.Setting;
import singlestage.delegatemas.AGVSystem;
import singlestage.result.Result;

public class Test {

  public static void main(String[] args) {
    final Setting setting = new Setting.SettingBuilder()
        .setNumOfAGVs(40).setSeed(9842557).build();
    final singlestage.delegatemas.AGVSystem agvSystem = new AGVSystem(setting, false);
    final Result result = agvSystem.run();
  }
  
}
