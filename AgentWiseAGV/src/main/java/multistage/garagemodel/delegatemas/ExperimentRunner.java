package multistage.garagemodel.delegatemas;

import java.util.concurrent.Callable;

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
    final Result result = agvSystem.run();
    System.out.println("num of AGVs: " + (setting.getNumOfAGVs()) + "\tSeed: "
        + setting.getSeed() + "\tDONE");
    return result;
  }
}