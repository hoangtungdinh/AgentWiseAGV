package result.throughput;

import com.github.rinde.rinsim.core.Simulator;

import setting.Setting;

public class Result {
  
  private int numOfReachedDestinations;
  
  private int numOfAGVs;
  
  private Setting setting;
  
  private Simulator sim;
  
  public Result(Setting setting, Simulator sim) {
    numOfAGVs = 0;
    this.setting = setting;
    this.sim = sim;
    this.numOfReachedDestinations = 0;
  }
  
  public void updateResult(int numOfReachedDestinations) {
    numOfAGVs++;
    
    this.numOfReachedDestinations += numOfReachedDestinations;
    
    if (numOfAGVs == setting.getNumOfAGVs()) {
      sim.stop();
    }
  }
  
  public Setting getSetting() {
    return setting;
  }

  public long getNumOfReachedDestinations() {
    return numOfReachedDestinations;
  }
}
