package multistage.result;

import java.util.ArrayList;
import java.util.List;

public class Sample {
  
  private List<Result> results;
  
  private int numOfAGVs;
  
  public Sample(int numOfAGVs) {
    results = new ArrayList<>();
    this.numOfAGVs = numOfAGVs;
  }
  
  public void addResult(Result result) {
    results.add(result);
  }
  
  public List<Result> getResults() {
    return results;
  }
  
  public int getNumOfAGVs() {
    return numOfAGVs;
  }
}
