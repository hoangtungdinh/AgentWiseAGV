package routeplan.contextaware;

import java.util.LinkedList;
import java.util.List;

import com.github.rinde.rinsim.geom.Point;

import resourceagents.FreeTimeWindow;

/**
 * The Class PlanFTW.
 * It contains a list of connected free time windows
 *
 * @author Tung
 */
public class PlanFTW {
  
  /** The free time windows (index as point, edge, point, edge, ...). */
  private LinkedList<FreeTimeWindow> freeTimeWindows;
  
  /** The path. */
  private List<Point> path;
  
  /** The stage. */
  private int stage;
  
  /** The destinations. */
  private List<Point> destinations;

  /**
   * Instantiates a new plan ftw.
   *
   * @param freeTimeWindows the free time windows
   * @param path the path
   * @param stage the stage
   */
  public PlanFTW(LinkedList<FreeTimeWindow> freeTimeWindows, List<Point> path,
      int stage, List<Point> destinations) {
    this.freeTimeWindows = freeTimeWindows;
    this.path = path;
    this.stage = stage;
    this.destinations = destinations;
  }

  /**
   * Gets the free time windows.
   *
   * @return the free time windows
   */
  public LinkedList<FreeTimeWindow> getFreeTimeWindows() {
    return freeTimeWindows;
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
   * Gets the arrival time.
   *
   * @return the arrival time
   */
  public long getArrivalTime() {
    return freeTimeWindows.getLast().getEntryWindow().lowerEndpoint();
  }
  
  /**
   * Gets the earliest exit time.
   *
   * @return the earliest exit time
   */
  public long getEarliestExitTime() {
    return freeTimeWindows.getLast().getExitWindow().lowerEndpoint();
  }
  
  /**
   * Gets the stage.
   *
   * @return the stage
   */
  public int getStage() {
    return stage;
  }
  
  /**
   * Checks if the next node is valid.
   * A next node is valid if it does not create loop in the same stage
   *
   * @param nextNode the next node
   * @return true, if is valid
   */
  public boolean isValid(Point nextNode) {
    if (stage == 0) {
      if (path.contains(nextNode)) {
        return false;
      } else {
        return true;
      }
    } else {
      int i = path.size() - 1;
      while (!path.get(i).equals(destinations.get(stage - 1))) {
        if (path.get(i).equals(nextNode)) {
          return false;
        }
        i--;
      }
      return true;
    }
  }
}
