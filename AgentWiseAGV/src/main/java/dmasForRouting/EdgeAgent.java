package dmasForRouting;

import java.util.HashMap;
import java.util.Map;

import com.github.rinde.rinsim.geom.Point;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.Table;
import com.google.common.collect.TreeRangeSet;

/**
 * The Class EdgeAgent.
 *
 * @author Tung
 */
public class EdgeAgent {
  
  /**
   * The reservation amp. There are always two reservation lists in the map,
   * corresponding to two entrances.
   */
  private Map<Point, RangeSet<Long>> reservationMap;
  
  /**
   * The reservation info, which is a table with row is the entrance point,
   * column is the reserved range.
   */
  private Table<Point, Range<Long>, Long> reservationLifeTime;
  
  /** The length of the associated edge. */
  private double length;
  
  /**
   * Instantiates a new edge agent.
   *
   * @param p1 the first point of the edge
   * @param p2 the second point of the edge
   * @param length the length of the edge
   */
  public EdgeAgent(Point p1, Point p2, double length) {
    this.length = length;
    
    reservationMap = new HashMap<>();
    
    // reservation list of AGVs coming from point 1
    RangeSet<Long> rangeSetForP1 = TreeRangeSet.create();
    reservationMap.put(p1, rangeSetForP1);
    // reservation list of AGVs coming from point 2
    RangeSet<Long> rangeSetForP2 = TreeRangeSet.create();
    reservationMap.put(p2, rangeSetForP2);
  }
  
  /**
   * Gets the length.
   *
   * @return the length
   */
  public double getLength() {
    return length;
  }
  
  /**
   * Gets the free time windows if the AGV wants to move to the endPoint from the startTime.
   *
   * @param endPoint the end point of the edge
   * @param startTime the start time
   * @param oldReservation the old reservation of this AGV in this node. Note that the oldReservation of 
   * the AGVs must be started from the different startPoint. Otherwise, it is null.
   * @return the free time windows
   */
  public RangeSet<Long> getFreeTimeWindows(Point endPoint, long startTime, Range<Long> oldReservation) {
    RangeSet<Long> freeTimeWindows;
    // get the reservation lists of AGVs starting from the other direction
    RangeSet<Long> reservationList = reservationMap.get(endPoint);
    
    if (oldReservation == null) {
      freeTimeWindows = reservationList.complement().subRangeSet(Range.atLeast(startTime));
    } else {
      reservationList.remove(oldReservation);
      freeTimeWindows = reservationList.complement().subRangeSet(Range.atLeast(startTime));
      reservationList.add(oldReservation);
    }
    
    return freeTimeWindows;
  }
  
  public void addReservation(Range<Long> newReservation, Point startPoint, Point endPoint, long lifeTime, Range<Long> oldReservation) {
    // TODO control the difference between two direction if remove old reservation
    if (oldReservation != null) {
      
    }
  }
  
  // TODO control the difference between two direction when remove outdated reservations
}
