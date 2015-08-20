package resourceAgents;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.github.rinde.rinsim.geom.Point;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;

import dmasForRouting.AGVSystem;

/**
 * The Class EdgeAgent.
 *
 * @author Tung
 */
public class EdgeAgent {
  
  /**
   * The reservation map. There are always two reservation lists in the map,
   * corresponding to two entrances.
   */
  private Map<Point, List<Reservation>> reservationMap;
  
  /** The length of the associated edge. */
  private double length;
  
  /**
   * Instantiates a new edge agent.
   * The point p1 and p2 can be at any order
   * 
   * @param p1 the first point of the edge
   * @param p2 the second point of the edge
   * @param length the length of the edge
   */
  public EdgeAgent(Point p1, Point p2, double length) {
    this.length = length;
    
    reservationMap = new HashMap<>();
    
    // reservation list of AGVs coming from point 1
    List<Reservation> reservationFromP1 = new ArrayList<>();
    reservationMap.put(p1, reservationFromP1);
    // reservation list of AGVs coming from point 2
    List<Reservation> reservationFromP2 = new ArrayList<>();
    reservationMap.put(p2, reservationFromP2);
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
   * @param possibleEntryWindow the possible entry window
   * @param agvID the agv id
   * @return the free time windows
   */
  public List<FreeTimeWindow> getFreeTimeWindows(Point startPoint,
      Point endPoint, Range<Long> possibleEntryWindow, int agvID) {
    
    // actual entry window (take into account the length of vehicles)
    Range<Long> realPossibleEntryWindow;
    final long lowerEndPoint = possibleEntryWindow.lowerEndpoint()
        - ((long) (AGVSystem.VEHICLE_LENGTH*1000 / AGVSystem.VEHICLE_SPEED));
    if (possibleEntryWindow.hasUpperBound()) {
      final long upperEndPoint = possibleEntryWindow.upperEndpoint()
          - ((long) (AGVSystem.VEHICLE_LENGTH / AGVSystem.VEHICLE_SPEED));
      realPossibleEntryWindow = Range.open(lowerEndPoint, upperEndPoint);
    } else {
      realPossibleEntryWindow = Range.greaterThan(lowerEndPoint);
    }
    
    // get all reservations from other direction
    RangeSet<Long> reservationsFromOtherDirection = TreeRangeSet.create();
    List<Reservation> resvList = reservationMap.get(endPoint);
    for (Reservation reservation : resvList) {
      if (reservation.getAgvID() != agvID) {
        reservationsFromOtherDirection.add(reservation.getInterval());
      }
    }
    
    // get all possible free ranges that do not conflict with AGVs from other direction
    RangeSet<Long> freeRanges = reservationsFromOtherDirection.complement()
        .subRangeSet(Range.atLeast(realPossibleEntryWindow.lowerEndpoint()));
 
    // get all entryWindows based on the reservations of opposite direction AGVs
    RangeSet<Long> entryWindows = freeRanges.subRangeSet(realPossibleEntryWindow);
    
    if (entryWindows.isEmpty()) {
      // no possible time window
      return null;
    } else if (entryWindows.asRanges().size() > 1) {
      throw new Error("More than one entry window for edge!");
    }
    
    Range<Long> optimisticEntryWindow = entryWindows.span();
    
    // list of reservations from the same direction
    List<Reservation> reservationsWithSameDirection = reservationMap.get(startPoint);
    
    // minimum travel time
    long minTravelTime = (long) (((length + AGVSystem.VEHICLE_LENGTH) * 1000) / AGVSystem.VEHICLE_SPEED);
    
    long lowerEndExitWindow = -1;
    long upperEndExitWindow = Long.MAX_VALUE;
    
    // compute the exit window
    final long optimisticStartTime = optimisticEntryWindow.lowerEndpoint();
    for (Reservation reservation : reservationsWithSameDirection) {
      final long reservedEntryTime = reservation.getInterval().lowerEndpoint();
      final long reservedExitTime = reservation.getInterval().upperEndpoint();
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
    long upperEndEntryWindow;
    if (optimisticEntryWindow.hasUpperBound()) {
      upperEndEntryWindow = optimisticEntryWindow.upperEndpoint();
    } else {
      upperEndEntryWindow = Long.MAX_VALUE;
    }
    
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
  
  /**
   * Adds the reservation.
   *
   * @param startPoint the start point
   * @param interval the interval
   * @param lifeTime the life time
   * @param agvID the agv id
   */
  public void addReservation(Point startPoint, Range<Long> interval,
      long lifeTime, int agvID) {
    reservationMap.get(startPoint)
        .add(new Reservation(agvID, lifeTime, interval));
  }
  
  /**
   * Refresh reservation.
   * Basically it just adds new reservation
   *
   * @param startPoint the start point
   * @param interval the interval
   * @param lifeTime the life time
   * @param agvID the agv id
   */
  public void refreshReservation(Point startPoint, Range<Long> interval,
      long lifeTime, int agvID) {
    addReservation(startPoint, interval, lifeTime, agvID);
  }
  
  /**
   * Removes the out-dated reservations.
   *
   * @param currentTime the current time
   */
  public void removeOutdatedReservations(long currentTime) {
    for (Map.Entry<Point, List<Reservation>> entry : reservationMap.entrySet()) {
      List<Reservation> reservationList = entry.getValue();
      Iterator<Reservation> iter = reservationList.iterator();
      while (iter.hasNext()) {
        Reservation reservation = iter.next();
        if (reservation.getLifeTime() < currentTime) {
          iter.remove();
        }
      }
    }
  }
}
