package routeplan;

import com.github.rinde.rinsim.geom.Point;

/**
 * The Class CheckPoint.
 *
 * @author Tung
 */
public class CheckPoint {
  
  /** The point. */
  private Point point;
  
  /** The expected time. */
  private long expectedTime;
  
  /**
   * Instantiates a new check point.
   *
   * @param point the point
   * @param expectedTime the expected time
   */
  public CheckPoint(Point point, long expectedTime) {
    this.point = point;
    this.expectedTime = expectedTime;
  }
  
  /**
   * Gets the point.
   *
   * @return the point
   */
  public Point getPoint() {
    return point;
  }
  
  /**
   * Gets the expected time.
   *
   * @return the expected time
   */
  public long getExpectedTime() {
    return expectedTime;
  }
}
