package routeplan;

import java.util.ArrayList;
import java.util.List;

import com.github.rinde.rinsim.geom.Point;
import com.google.common.collect.Range;

import setting.Setting;

/**
 * The Class ExecutablePlan.
 *
 * @author Tung
 */
public class ExecutablePlan {
  
  /** The path. */
  private List<Point> path;
  
  /** The intervals. */
  private List<Range<Long>> intervals;
  
  /** The check points. */
  private List<CheckPoint> checkPoints;
  
  /** The setting. */
  private Setting setting;
  
  /**
   * Instantiates a new executable plan.
   *
   * @param plan the plan
   */
  public ExecutablePlan(Plan plan, Setting setting) {
    this.path = plan.getPath();
    this.intervals = plan.getIntervals();
    this.checkPoints = new ArrayList<>();
    this.setting = setting;
    calculateCheckPoints();
  }
  
  /**
   * Calculate check points.
   */
  private void calculateCheckPoints() {
    final double safeDistance = setting.getVehicleLength() + 0.1;
    final long timeLeftToLeaveNode = (long) (setting.getVehicleLength() *1000 / setting.getVehicleSpeed());
    final long timeLeftToLeaveEdge = (long) (safeDistance*1000 / setting.getVehicleSpeed());
    
    final List<Point> firstResource = new ArrayList<>();
    firstResource.add(path.get(0));
    checkPoints.add(new CheckPoint(path.get(0),
        intervals.get(0).upperEndpoint() - timeLeftToLeaveNode, firstResource,
        ResourceType.NODE));
    
    CheckPoint newCheckPoint;
    
    for (int i = 0; i < path.size() - 1; i++) {
      // check whether the edge is a horizontal edge or a vertical edge
      boolean horizontal;
      if (path.get(i).x == path.get(i + 1).x) {
        horizontal = false;
      } else if (path.get(i).y == path.get(i + 1).y) {
        horizontal = true;
      } else {
        throw new Error("Invalid map");
      }
      
      final List<Point> edgeResource = new ArrayList<>();
      edgeResource.add(path.get(i));
      edgeResource.add(path.get(i + 1));
      
      if (horizontal) {
        // if move horizontally check if move left or right
        boolean moveLeft;
        if (path.get(i + 1).x - path.get(i).x > 0) {
          moveLeft = false;
        } else {
          moveLeft = true;
        }
        
        if (moveLeft) {
          final Point p = new Point(path.get(i + 1).x + safeDistance, path.get(i).y);
          newCheckPoint = new CheckPoint(p, intervals.get(i*2 + 1).upperEndpoint() - timeLeftToLeaveEdge, edgeResource, ResourceType.EDGE);
        } else {
          final Point p = new Point(path.get(i + 1).x - safeDistance, path.get(i).y);
          newCheckPoint = new CheckPoint(p, intervals.get(i*2 + 1).upperEndpoint() - timeLeftToLeaveEdge, edgeResource, ResourceType.EDGE);
        }
      } else {
        // if move vertically check if move up or move down
        boolean moveUp;
        if (path.get(i + 1).y - path.get(i).y > 0) {
          moveUp = false;
        } else {
          moveUp = true;
        }
        
        if (moveUp) {
          final Point p = new Point(path.get(i).x, path.get(i + 1).y + safeDistance);
          newCheckPoint = new CheckPoint(p, intervals.get(i*2 + 1).upperEndpoint() - timeLeftToLeaveEdge, edgeResource, ResourceType.EDGE);
        } else {
          final Point p = new Point(path.get(i).x, path.get(i + 1).y - safeDistance);
          newCheckPoint = new CheckPoint(p, intervals.get(i*2 + 1).upperEndpoint() - timeLeftToLeaveEdge, edgeResource, ResourceType.EDGE);
        }
      }
      
      // add the new check point of the edge
      checkPoints.add(newCheckPoint);
      
      // add the check point of the node, which is the node itself
      final List<Point> nodeResource = new ArrayList<>();
      nodeResource.add(path.get(i + 1));
      checkPoints.add(new CheckPoint(path.get(i + 1),
          intervals.get(i * 2 + 2).upperEndpoint() - timeLeftToLeaveNode,
          nodeResource, ResourceType.NODE));
    }
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
   * Gets the check points.
   *
   * @return the check points
   */
  public List<CheckPoint> getCheckPoints() {
    return checkPoints;
  }
}
