package experiment;

import java.util.concurrent.Callable;

import multistage.garagemodel.delegatemas.AGVSystem;
import multistage.result.Result;
import setting.Setting;

public class ExperimentRunner implements Callable<Result> {
  
  private final Setting setting;
  
  public ExperimentRunner(Setting setting) {
    this.setting = setting;
  }

  @Override
  public Result call() throws Exception {
    System.out.println("num of AGVs: " + (setting.getNumOfAGVs()) + "\tSeed: "
        + setting.getSeed());
    final AGVSystem agvSystem = new AGVSystem(setting,
        false);
    Result result = null;
    try {
      result = agvSystem.run();
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println("Error at seed: " + setting.getSeed() + " numOfAGVs: " + setting.getNumOfAGVs());
      System.exit(0);
    }
    System.out.println("num of AGVs: " + (setting.getNumOfAGVs()) + "\tSeed: "
        + setting.getSeed() + "\tDONE");
    return result;
  }
}
