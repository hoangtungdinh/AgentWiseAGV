package routeplan;

import java.util.LinkedList;
import java.util.List;

import com.github.rinde.rinsim.geom.Point;
import com.google.common.collect.Range;

/**
 * The Class Plan.
 *
 * @author Tung
 */
public class Plan {
  
  /** The path. */
  private LinkedList<Point> path;
  
  /** The intervals. */
  private LinkedList<Range<Long>> intervals;

  /**
   * Instantiates a new plan.
   *
   * @param path the path
   * @param intervals the intervals
   */
  public Plan(List<Point> path, List<Range<Long>> intervals) {
    this.path = new LinkedList<>(path);
    this.intervals = new LinkedList<>(intervals);
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
   * Gets the intervals.
   *
   * @return the intervals
   */
  public List<Range<Long>> getIntervals() {
    return intervals;
  }
  
  /**
   * Gets the arrival time.
   *
   * @return the arrival time
   */
  public long getArrivalTime() {
    return intervals.get(intervals.size() - 1).lowerEndpoint();
  }
  
  /**
   * Removes the out-dated steps
   */
  public void removeOldSteps() {
    path.removeFirst();
    intervals.removeFirst();
    intervals.removeFirst();
  }
  
  /**
   * Adds the last node.
   * This method is useful when exploration.
   *
   * @param lastNode the last node
   * @param currentTime the current time
   * @param timeToLeaveEdge the time to leave edge
   */
  public void addLastNode(Point lastNode, Range<Long> nodeInterval, Range<Long> edgeInterval) {
    path.addFirst(lastNode);
    intervals.addFirst(edgeInterval);
    intervals.addFirst(nodeInterval);
  }
}
