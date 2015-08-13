package dmasForRouting;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.github.rinde.rinsim.geom.Point;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;

/**
 * The Class ResourceAgent.
 *
 * @author Tung
 */
public class NodeAgent {
  
  /** The reservation list. */
  private RangeSet<Long> reservationList;

  /** The reservation life time. */
  private Map<Range<Long>, Long> reservationLifeTime;

  /**
   * The shortest path length. It stores the shortest path length between the
   * associated node and other nodes.
   */
  private Map<Point, Double> shortestPathLength;
  
  /**
   * Instantiates a new node agent.
   */
  public NodeAgent() {
    reservationList = TreeRangeSet.create();
    reservationLifeTime = new HashMap<>();
    shortestPathLength = new HashMap<>();
  }
  
  /**
   * Gets the free time windows from the startTime.
   *
   * @param startTime the start time
   * @param oldReservation the old reservation of this AGV in this node
   * @return the free time windows
   */
  public RangeSet<Long> getFreeTimeWindows(long startTime, Range<Long> oldReservation) {
    RangeSet<Long> freeTimeWindows;
    
    if (oldReservation == null) {
      freeTimeWindows = reservationList.complement().subRangeSet(Range.atLeast(startTime));
    } else {
      reservationList.remove(oldReservation);
      freeTimeWindows = reservationList.complement().subRangeSet(Range.atLeast(startTime));
      reservationList.add(oldReservation);
    }
    
    return freeTimeWindows;
  }
  
  /**
   * Adds the new reservation
   *
   * @param newReservation the new reservation
   * @param lifeTime the life time
   * @param oldReservation the old reservation of this AGV in this node. If there is no old reservation, then it is null
   * @return true, if successfully added
   */
  public void addReservation(Range<Long> newReservation, long lifeTime, Range<Long> oldReservation) {
    
    // if there exists old reservation from the previous plan of the AGV, then
    // remove it.
    if (oldReservation != null) {
      reservationList.remove(oldReservation);
      reservationLifeTime.remove(oldReservation);
    }
    
    // get all overlapping range
    RangeSet<Long> overlappingRanges = this.reservationList.subRangeSet(newReservation);
    // check if there is any overlapping range
    if (!overlappingRanges.isEmpty()) {
      throw new Error("Overlapping reservation!");
    }
    
    // add new reservation
    reservationList.add(newReservation);
    reservationLifeTime.put(newReservation, lifeTime);
  }
  
  /**
   * Refresh reservation.
   *
   * @param reservation the reservation
   * @param lifeTime the life time
   */
  public void refreshReservation(Range<Long> reservation, long lifeTime) {
    if (reservationLifeTime.containsKey(reservation)) {
      // if there exists the reservation then update it
      reservationLifeTime.put(reservation, lifeTime);
    } else {
      // otherwise throw an error
      throw new Error("No reservation for refreshing");
    }
  }
  
  /**
   * Removes the out dated reservation.
   *
   * @param currentTime the current time
   */
  public void removeOutDatedReservation(long currentTime) {
    for (Iterator<Map.Entry<Range<Long>, Long>> iter = reservationLifeTime.entrySet().iterator(); iter.hasNext(); ) {
      Map.Entry<Range<Long>, Long> entry = iter.next();
      if (entry.getValue() <= currentTime) {
        // if life time has passed
        // remove from the reservation list
        reservationList.remove(entry.getKey());
        // remove from the reservation life time list
        iter.remove();
      }
    }
  }
  
  /**
   * Sets the path length.
   *
   * @param point the point
   * @param length the length from the current node to the "point"
   */
  public void setPathLength(Point point, double length) {
    shortestPathLength.put(point, length);
  }
  
  /**
   * Gets the path length.
   *
   * @param point the point
   * @return the path length from the current node to the "point"
   */
  public double getPathLength(Point point) {
    return shortestPathLength.get(point);
  }
}
