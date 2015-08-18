package routePlan;

import java.util.LinkedList;
import java.util.List;

import com.github.rinde.rinsim.geom.Point;
import com.google.common.collect.Range;

/**
 * The Class Route.
 * Contains the path and the schedule along the path
 *
 * @author Tung
 */
public class RoutePlan {

  /** The plan. */
  private LinkedList<PlanStep> plan;
  
  /** The path. */
  private List<Point> path;
  
  /** The current index. */
  private int currentIndex;
  
  /** The current exit window. */
  private Range<Long> currentExitWindow;
  
  /**
   * Instantiates a new route plan.
   *
   * @param path the path
   * @param exitWindow the exit window
   */
  public RoutePlan(List<Point> path, Range<Long> exitWindow) {
    this.path = path;
    this.currentExitWindow = exitWindow;
    this.currentIndex = 0;
    this.plan = new LinkedList<>();
  }
  
  /**
   * Instantiates a new route plan based on an existing route plan.
   *
   * @param currentPlan the current plan
   * @param currentPath the current path
   * @param nextIndex the next index
   * @param nextExitWindow the next exit window
   * @param nextPlanStep the next plan step
   */
  public RoutePlan(LinkedList<PlanStep> currentPlan, List<Point> currentPath, int nextIndex,
      Range<Long> nextExitWindow, PlanStep nextPlanStep) {
    this.plan = new LinkedList<>(plan);
    this.path = currentPath;
    this.currentIndex = nextIndex;
    this.currentExitWindow = nextExitWindow;
    this.plan.add(nextPlanStep);
  }

  /**
   * Adds the next plan step.
   *
   * @param currentNode the current node
   * @param nextNode the next node
   * @param interval the interval
   */
  public void addNextStep(Point currentNode, Point nextNode,
      Range<Long> interval) {
    plan.addLast(new PlanStep(currentNode, nextNode, interval));
  }
  
  /**
   * View the next step.
   *
   * @return the next plan step
   */
  public PlanStep viewNextStep() {
    return plan.getFirst();
  }
  
  /**
   * Consume next step. Different from {@link #viewNextStep}, this method will
   * return and remove the next step from the plan
   *
   * @return the plan step
   */
  public PlanStep consumeNextStep() {
    return plan.removeFirst();
  }
  
  /**
   * Gets the destination arrival time.
   *
   * @return the destination arrival time
   */
  public long getDestinationArrivalTime() {
    return plan.getLast().getInterval().lowerEndpoint();
  }

  /**
   * Gets the plan.
   *
   * @return the plan
   */
  public LinkedList<PlanStep> getPlan() {
    return plan;
  }

  /**
   * Gets the path.
   *
   * @return the path
   */
  public List<Point> getPath() {
    return path;
  }

  /**
   * Gets the current index.
   *
   * @return the current index
   */
  public int getCurrentIndex() {
    return currentIndex;
  }

  /**
   * Gets the current exit window.
   *
   * @return the current exit window
   */
  public Range<Long> getCurrentExitWindow() {
    return currentExitWindow;
  }
  
  
}
