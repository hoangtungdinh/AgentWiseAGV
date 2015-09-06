package test;

import multistage.garagemodel.GraphCreator;
import setting.Setting;

public class Test {

  public static void main(String[] args) {
    GraphCreator graphCreator = new GraphCreator(new Setting.SettingBuilder().build());
    graphCreator.createGraph();
  }
  
}
