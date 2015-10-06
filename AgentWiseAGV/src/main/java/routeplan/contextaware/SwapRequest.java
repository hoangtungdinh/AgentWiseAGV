package routeplan.contextaware;

import java.util.Set;

public class SwapRequest {
  
  private final SingleStep stepToBeSwapped;
  
  private final Set<SingleStep> delayedSteps;

  public SwapRequest(SingleStep stepToBeSwapped,
      Set<SingleStep> delayedSteps) {
    this.stepToBeSwapped = stepToBeSwapped;
    this.delayedSteps = delayedSteps;
  }

  public SingleStep getStepToBeSwapped() {
    return stepToBeSwapped;
  }

  public Set<SingleStep> getDelayedSteps() {
    return delayedSteps;
  }
}
