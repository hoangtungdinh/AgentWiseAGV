package routeplan.delegatemas;

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

  /**
   * Instantiates a new plan ftw.
   *
   * @param freeTimeWindows the free time windows
   * @param path the path
   * @param stage the stage
   */
  public PlanFTW(LinkedList<FreeTimeWindow> freeTimeWindows, List<Point> path) {
    this.freeTimeWindows = freeTimeWindows;
    this.path = path;
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
}
