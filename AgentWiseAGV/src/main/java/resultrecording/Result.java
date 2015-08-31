package resultrecording;

import com.github.rinde.rinsim.core.Simulator;

import dmasForRouting.Setting;

public class Result {
  
  private long jointPlanCost;
  
  private long earliestStartingTime;
  
  private long latestFinishTime;
  
  private long makeSpan;
  
  private int numOfAGVs;
  
  private Setting setting;
  
  private Simulator sim;
  
  public Result(Setting setting, Simulator sim) {
    jointPlanCost = 0;
    earliestStartingTime = Long.MAX_VALUE;
    latestFinishTime = 0;
    makeSpan = -1;
    numOfAGVs = 0;
    this.setting = setting;
    this.sim = sim;
  }
  
  public void updateResult(long startTime, long finishTime) {
    numOfAGVs++;
    
    if (earliestStartingTime > startTime) {
      earliestStartingTime = startTime;
    }
    
    if (latestFinishTime < finishTime) {
      latestFinishTime = finishTime;
    }
    
    jointPlanCost += finishTime - startTime;
    
    if (numOfAGVs == setting.getNumOfAGVs()) {
      sim.stop();
      makeSpan = latestFinishTime - earliestStartingTime;
    }
  }
  
  public Setting getSetting() {
    return setting;
  }

  public long getJointPlanCost() {
    return jointPlanCost;
  }

  public long getMakeSpan() {
    return makeSpan;
  }
}
