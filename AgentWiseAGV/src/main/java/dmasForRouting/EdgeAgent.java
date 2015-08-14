package dmasForRouting;

import java.util.ArrayList;
import java.util.HashMap;
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
  public List<FreeTimeWindow> getFreeTimeWindows(Point startPoint,
      Point endPoint, Range<Long> possibleEntryWindow,
      Range<Long> oldReservation) {
    // set of ranges that do not conflict with AGVs from other direction
    RangeSet<Long> freeRanges;
    // get the reservation lists of AGVs starting from the other direction
    RangeSet<Long> reservationsFromOtherDirection = reservationMap.get(endPoint);
    
    // get all possible free ranges that do not conflict with AGVs from other direction
    if (oldReservation == null) {
      freeRanges = reservationsFromOtherDirection.complement()
          .subRangeSet(Range.atLeast(possibleEntryWindow.lowerEndpoint()));
    } else {
      reservationsFromOtherDirection.remove(oldReservation);
      freeRanges = reservationsFromOtherDirection.complement()
          .subRangeSet(Range.atLeast(possibleEntryWindow.lowerEndpoint()));
      reservationsFromOtherDirection.add(oldReservation);
    }
    
    // get all entryWindows based on the reservations of opposite direction AGVs
    RangeSet<Long> entryWindows = freeRanges.subRangeSet(possibleEntryWindow);
    
    if (entryWindows.isEmpty()) {
      // no possible time window
      return null;
    } else if (entryWindows.asRanges().size() > 1) {
      throw new Error("More than one entry window for edge!");
    }
    
    Range<Long> optimisticEntryWindow = entryWindows.span();
    
    Map<Range<Long>, Long> lifeTimeOfReservationsWithSameDirection = reservationLifeTime.row(startPoint);
    // set of reservations of AGVs coming from the same direction
    Set<Range<Long>> reservationsWithSameDirection = lifeTimeOfReservationsWithSameDirection.keySet();
    
    // minimum travel time
    long minTravelTime = (long) ((this.length + AGVSystem.VEHICLE_LENGTH) / AGVSystem.VEHICLE_SPEED);
    
    long lowerEndExitWindow = -1;
    long upperEndExitWindow = Long.MAX_VALUE;
    
    // compute the exit window
    final long optimisticStartTime = optimisticEntryWindow.lowerEndpoint();
    for (Range<Long> range : reservationsWithSameDirection) {
      final long reservedEntryTime = range.lowerEndpoint();
      final long reservedExitTime = range.upperEndpoint();
      if (reservedEntryTime < optimisticStartTime && reservedExitTime > lowerEndExitWindow) {
        // if there is  an AGV that enters the edge before but exit the edge after the current AGV, then update the lower bound exit time
        lowerEndExitWindow = reservedExitTime;
      } else if (reservedEntryTime > optimisticStartTime && reservedExitTime < upperEndExitWindow) {
        // if there is an AGV that enters the edge after but exit the edge before the current AGV, then update the upper bound of exit time
        upperEndExitWindow = reservedExitTime;
      }
    }
    
    // if no valid exit window at this time then it is an error
    if (lowerEndExitWindow > upperEndExitWindow) {
      throw new Error("Invalid exit window");
    }
    
    long lowerEndEntryWindow = optimisticEntryWindow.lowerEndpoint();
    long upperEndEntryWindow = optimisticEntryWindow.upperEndpoint();
    
    Range<Long> optimisticTimeWindow = Range.open(lowerEndEntryWindow, upperEndExitWindow);
    RangeSet<Long> allNonConflictTimeWindow = freeRanges.subRangeSet(optimisticTimeWindow);
    Range<Long> feasibleTimeWindow = allNonConflictTimeWindow.rangeContaining(lowerEndEntryWindow + 1);
    
    if (feasibleTimeWindow == null) {
      throw new Error("Time window cannot be null");
    }
    
    upperEndExitWindow = feasibleTimeWindow.upperEndpoint();
    
    if (lowerEndExitWindow < lowerEndEntryWindow + minTravelTime) {
      lowerEndExitWindow = lowerEndEntryWindow + minTravelTime;
    }
    
    if (upperEndExitWindow < lowerEndExitWindow) {
      return null;
    }
    
    if (upperEndEntryWindow + minTravelTime > upperEndExitWindow) {
      upperEndEntryWindow = upperEndExitWindow - minTravelTime;
    }
    
    Range<Long> entryWindow = Range.open(lowerEndEntryWindow, upperEndEntryWindow);
    Range<Long> exitWindow = Range.open(lowerEndExitWindow, upperEndExitWindow);
    Range<Long> timeWindow = Range.open(lowerEndEntryWindow, upperEndExitWindow);
    
    List<FreeTimeWindow> freeTimeWindows = new ArrayList<>();
    freeTimeWindows.add(new FreeTimeWindow(timeWindow, entryWindow, exitWindow));
    
    return freeTimeWindows;
  }
  
  public void addReservation(Range<Long> newReservation, Point startPoint, Point endPoint, long lifeTime, Range<Long> oldReservation) {
    // TODO control the difference between two direction if remove old reservation
    if (oldReservation != null) {
      
    }
  }
  
  // TODO control the difference between two direction when remove outdated reservations
}
