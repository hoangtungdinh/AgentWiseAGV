package routePlan;

import java.util.ArrayList;
import java.util.List;

import com.github.rinde.rinsim.geom.Point;
import com.google.common.collect.Range;

import dmasForRouting.AGVSystem;

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
  
  /**
   * Instantiates a new executable plan.
   *
   * @param plan the plan
   */
  public ExecutablePlan(Plan plan) {
    this.path = plan.getPath();
    this.intervals = plan.getIntervals();
    this.checkPoints = new ArrayList<>();
    calculateCheckPoints();
  }
  
  /**
   * Calculate check points.
   */
  private void calculateCheckPoints() {
    final double safeDistance = AGVSystem.VEHICLE_LENGTH + 0.1;
    final long timeLeftToLeaveNode = (long) (AGVSystem.VEHICLE_LENGTH *1000 / AGVSystem.VEHICLE_SPEED);
    final long timeLeftToLeaveEdge = (long) (safeDistance*1000 / AGVSystem.VEHICLE_SPEED);
    
    checkPoints.add(new CheckPoint(path.get(0), intervals.get(0).upperEndpoint() - timeLeftToLeaveNode));
    
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
          newCheckPoint = new CheckPoint(p, intervals.get(i*2 + 1).upperEndpoint() - timeLeftToLeaveEdge);
        } else {
          final Point p = new Point(path.get(i + 1).x - safeDistance, path.get(i).y);
          newCheckPoint = new CheckPoint(p, intervals.get(i*2 + 1).upperEndpoint() - timeLeftToLeaveEdge);
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
          newCheckPoint = new CheckPoint(p, intervals.get(i*2 + 1).upperEndpoint() - timeLeftToLeaveEdge);
        } else {
          final Point p = new Point(path.get(i).x, path.get(i + 1).y - safeDistance);
          newCheckPoint = new CheckPoint(p, intervals.get(i*2 + 1).upperEndpoint() - timeLeftToLeaveEdge);
        }
      }
      
      // add the new check point of the edge
      checkPoints.add(newCheckPoint);
      
      // add the check point of the node, which is the node itself
      if (intervals.get(i*2 + 2).hasUpperBound() || i != path.size() - 2) {
        checkPoints.add(new CheckPoint(path.get(i + 1), intervals.get(i*2 + 2).upperEndpoint() - timeLeftToLeaveNode));
      }
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
