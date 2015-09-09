package routeplan;

import java.util.List;

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
   * The resource. If resource is node, then it contains one point. If resource
   * is edge, it contains two points: start-end
   */
  private List<Point> resource;
  
  /**
   * Instantiates a new check point.
   *
   * @param point the point
   * @param expectedTime the expected time
   * @param resourceType the resource type
   */
  public CheckPoint(Point point, long expectedTime, List<Point> resource) {
    this.point = point;
    this.expectedTime = expectedTime;
    this.resource = resource;
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

  public List<Point> getResource() {
    return resource;
  }
}
