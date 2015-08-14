package dmasForRouting;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
   * @param startPoint the start point
   * @param endPoint the end point of the edge
   * @param startTime the start time
   * @param oldReservation the old reservation of this AGV in this node. Note that the oldReservation of
   * the AGVs must be started from the different startPoint. Otherwise, it is null.
   * @return the free time windows
   */
  public List<FreeTimeWindow> getFreeTimeWindows(Point startPoint, Point endPoint, long startTime, Range<Long> oldReservation) {
    // set of ranges that do not conflict with AGVs from other direction
    RangeSet<Long> freeRanges;
    // get the reservation lists of AGVs starting from the other direction
    RangeSet<Long> reservationsFromOtherDirection = reservationMap.get(endPoint);
    
    // get all possible free ranges that do not conflict with AGVs from other direction
    if (oldReservation == null) {
      freeRanges = reservationsFromOtherDirection.complement()
          .subRangeSet(Range.atLeast(startTime));
    } else {
      reservationsFromOtherDirection.remove(oldReservation);
      freeRanges = reservationsFromOtherDirection.complement()
          .subRangeSet(Range.atLeast(startTime));
      reservationsFromOtherDirection.add(oldReservation);
    }

    Map<Range<Long>, Long> lifeTimeOfReservationsWithSameDirection = reservationLifeTime.row(startPoint);
    // set of reservations of AGVs coming from the same direction
    Set<Range<Long>> reservationsWithSameDirection = lifeTimeOfReservationsWithSameDirection.keySet();
    // construct set of reserved entry time windows of AGVs coming from the same direction
    RangeSet<Long> reservedEntryTimes = TreeRangeSet.create();
    final long entryInterval = (long) (AGVSystem.VEHICLE_LENGTH / AGVSystem.VEHICLE_SPEED);
    for (Range<Long> range : reservationsWithSameDirection) {
      reservedEntryTimes.add(Range.open(range.lowerEndpoint(), range.lowerEndpoint() + entryInterval));
    }
    
    // set of possible entry times
    RangeSet<Long> possibleEntryTimes = TreeRangeSet.create();
    // set of free time windows according to the entry time of AGVs coming from the same direction
    RangeSet<Long> optimisticTimeWindows = reservedEntryTimes.complement();
    for (Range<Long> range : freeRanges.asRanges()) {
      // set of actual free time window for the "range"
      final RangeSet<Long> times = optimisticTimeWindows.subRangeSet(range);
      possibleEntryTimes.addAll(times);
    }
    
    // minimum travel time
    long minTravelTime = (long) ((this.length + AGVSystem.VEHICLE_LENGTH) / AGVSystem.VEHICLE_SPEED);
    
    // consider each possible entry interval
    for (Range<Long> range : possibleEntryTimes.asRanges()) {
      // here we get the exit intervals corresponding to the entry interval
      // "range". If the entry interval is between two other entry intervals,
      // start time of the exit interval must be greater than the end time of
      // the exit interval of the previous entry interval and less than the exit
      // time of the after entry interval.
      
      // check whether the exit time of the current AGV is larger than the exit time of all previous AGV
      long startExitTime = range.lowerEndpoint() + minTravelTime;
      long endExitTime;
      // find the minimum time to exit (all previous AGVs have to exit first)
      for (Range<Long> currentReservation : reservationsWithSameDirection) {
        if (currentReservation.contains(range.lowerEndpoint())) {
          if (currentReservation.upperEndpoint() > startExitTime) {
            startExitTime = currentReservation.upperEndpoint();
          }
        }
      }
      // TODO compare the entry + exit with available time windows freeRanges
    }
    
  }
  
  public void addReservation(Range<Long> newReservation, Point startPoint, Point endPoint, long lifeTime, Range<Long> oldReservation) {
    // TODO control the difference between two direction if remove old reservation
    if (oldReservation != null) {
      
    }
  }
  
  // TODO control the difference between two direction when remove outdated reservations
}
