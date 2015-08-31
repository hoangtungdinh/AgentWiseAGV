package routeplan;

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
  private List<Point> path;
  
  /** The intervals. */
  private List<Range<Long>> intervals;

  /**
   * Instantiates a new plan.
   *
   * @param path the path
   * @param intervals the intervals
   */
  public Plan(List<Point> path, List<Range<Long>> intervals) {
    this.path = path;
    this.intervals = intervals;
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
}
