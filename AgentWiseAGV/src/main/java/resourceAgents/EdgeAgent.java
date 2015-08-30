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

import dmasForRouting.Setting;

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
  
  /** Two nodes of the edge that the agent associated. */
  private Point node1;
  private Point node2;
  
  /** The length of the associated edge. */
  private double length;
  
  /** The capacity. */
  private int capacity;
  
  private Setting setting;
  
  /**
   * Instantiates a new edge agent.
   * The point p1 and p2 can be at any order
   *
   * @param p1 the first point of the edge
   * @param p2 the second point of the edge
   * @param length the length of the edge
   * @param setting the setting
   */
  public EdgeAgent(Point p1, Point p2, double length, Setting setting) {
    this.setting = setting;
    this.length = length - setting.getVehicleLength();
    node1 = p1;
    node2 = p2;

    reservationMap = new HashMap<>();

    // reservation list of AGVs coming from point 1
    List<Reservation> reservationFromP1 = new ArrayList<>();
    reservationMap.put(p1, reservationFromP1);
    // reservation list of AGVs coming from point 2
    List<Reservation> reservationFromP2 = new ArrayList<>();
    reservationMap.put(p2, reservationFromP2);

    capacity = (int) ((length - setting.getVehicleLength() - 0.2)
        / setting.getVehicleLength());
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
        - ((long) (setting.getVehicleLength()*1000 / setting.getVehicleSpeed()));
    if (possibleEntryWindow.hasUpperBound()) {
      final long upperEndPoint = possibleEntryWindow.upperEndpoint()
          - ((long) (setting.getVehicleLength()*1000 / setting.getVehicleSpeed()));
      realPossibleEntryWindow = Range.closed(lowerEndPoint, upperEndPoint);
    } else {
      realPossibleEntryWindow = Range.atLeast(lowerEndPoint);
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
      // The case that there are more than one entry window only happens if
      // another AGV come from the opposite direction on the edge, then it
      // leaves the edge and enter the node that the current AGV is staying. It
      // is impossible since if the current AGV can stay at the current node, no
      // other AGV can enter this node during that time.
      throw new Error("More than one entry window for edge!");
    }
    
    Range<Long> optimisticEntryWindow = entryWindows.span();
    
    // list of reservations from the same direction
    List<Reservation> reservationsFromSameDirection = reservationMap.get(startPoint);
    
    // compute the capacity at the start time
    long startTime = optimisticEntryWindow.lowerEndpoint();
    List<Reservation> overlappingReservations = new ArrayList<>();
    for (Reservation resv : reservationsFromSameDirection) {
      if (resv.getInterval().contains(startTime) && resv.getAgvID() != agvID) {
        overlappingReservations.add(resv);
      }
    }
    
    // if the edge is already full, we have to calculate the start time
    // note that, because the capacity of the node is only 1, it means that
    // during the possible entry window, the capacity can only decrease. It
    // cannot increase because no vehicle can enter the edge while the current
    // AGV is occupying the start node.
    if (overlappingReservations.size() > capacity) {
      throw new Error("It cannot happen");
    }
    
    if (overlappingReservations.size() == capacity) {
      long newStartTime = Long.MAX_VALUE;
      
      for (Reservation resv : overlappingReservations) {
        if (resv.getInterval().upperEndpoint() < newStartTime) {
          newStartTime = resv.getInterval().upperEndpoint();
        }
      }
      
      // update the optimistic entry window
      if (optimisticEntryWindow.hasUpperBound()
          && newStartTime > optimisticEntryWindow.upperEndpoint()) {
        return null;
      } else {
        if (optimisticEntryWindow.hasUpperBound()) {
          final long upperBound = optimisticEntryWindow.upperEndpoint();
          optimisticEntryWindow = Range.closed(newStartTime, upperBound);
        } else {
          optimisticEntryWindow = Range.atLeast(newStartTime);
        }
      }
    }
    
    // minimum travel time
    long minTravelTime = (long) (((length + setting.getVehicleLength()) * 1000)
        / setting.getVehicleSpeed());
    
    long lowerEndExitWindow = -1;
    long upperEndExitWindow = Long.MAX_VALUE;
    
    // compute the exit window
    final long optimisticStartTime = optimisticEntryWindow.lowerEndpoint();
    final long minDifferentTime = (long) (setting.getVehicleLength() * 1000
        / setting.getVehicleSpeed());
    for (Reservation reservation : reservationsFromSameDirection) {
      if (reservation.getAgvID() == agvID) {
        continue;
      }
      final long reservedEntryTime = reservation.getInterval().lowerEndpoint();
      final long reservedExitTime = reservation.getInterval().upperEndpoint();
      if (reservedEntryTime < optimisticStartTime && reservedExitTime + minDifferentTime > lowerEndExitWindow) {
        // if there is  an AGV that enters the edge before but exit the edge after the current AGV, then update the lower bound exit time
        lowerEndExitWindow = reservedExitTime + minDifferentTime;
      } else if (reservedEntryTime > optimisticStartTime && reservedExitTime - minDifferentTime < upperEndExitWindow) {
        // if there is an AGV that enters the edge after but exit the edge before the current AGV, then update the upper bound of exit time
        upperEndExitWindow = reservedExitTime - minDifferentTime;
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
    
    Range<Long> optimisticTimeWindow = Range.closed(lowerEndEntryWindow, upperEndExitWindow);
    RangeSet<Long> allNonConflictTimeWindow = freeRanges.subRangeSet(optimisticTimeWindow);
    Range<Long> feasibleTimeWindow = allNonConflictTimeWindow.rangeContaining(lowerEndEntryWindow);
    
    if (feasibleTimeWindow == null) {
      throw new Error("Time window cannot be null");
    }
    
    upperEndExitWindow = feasibleTimeWindow.upperEndpoint();
    
    // calculate the capacity of the edge at upperEndExitWindow
    // if the capacity is overloaded, mean that another AGV will come to the
    // edge after the entry time of the current AGV, then we have to decrease
    // the upperEndExitWindow
    // --------------------------
    // we     ----------------------|---
    //               ---------------|-------------
    //                              |--------------------     
    List<Reservation> overlappingAtUpperEndPoint = new ArrayList<>();
    for (Reservation resv : reservationsFromSameDirection) {
      if (resv.getInterval().contains(upperEndExitWindow) && resv.getAgvID() != agvID) {
        overlappingAtUpperEndPoint.add(resv);
      }
    }
    
    // check first case
    if (overlappingAtUpperEndPoint.size() == capacity) {
      long newExitTime = 0;

      for (Reservation resv : overlappingAtUpperEndPoint) {
        if (resv.getInterval().lowerEndpoint() > newExitTime) {
          newExitTime = resv.getInterval().lowerEndpoint();
        }
      }
      
      upperEndExitWindow = newExitTime;
    }
    
    // check second case, when the capacity is overloaded at some interval (not
    // end points)
    // --------------|------------   t
    // we     -------|---------------|---
    //               |---------------|-------------
    //                               |--------------------     
    // we check the latest reservation after us that overlap with the updated end point t
    long latestEntryTimeOfOtherAGVs = -1;
    for (Reservation resv : reservationsFromSameDirection) {
      if (resv.getInterval().contains(upperEndExitWindow) && resv.getAgvID() != agvID) {
        if (resv.getInterval().lowerEndpoint() > latestEntryTimeOfOtherAGVs) {
          latestEntryTimeOfOtherAGVs = resv.getInterval().lowerEndpoint();
        }
      }
    }
    
    // check capacity at the entry time of the latest AGV
    List<Reservation> overlappingAtLatestEntryTime = new ArrayList<>();
    for (Reservation resv : reservationsFromSameDirection) {
      if (resv.getInterval().contains(latestEntryTimeOfOtherAGVs) && resv.getAgvID() != agvID) {
        overlappingAtLatestEntryTime.add(resv);
      }
    }
    
    if (overlappingAtLatestEntryTime.size() == capacity - 1) {
      upperEndExitWindow = latestEntryTimeOfOtherAGVs;
    }
    
    // now the capacity condition is guaranteed. We start checking the feasibility
    if (lowerEndExitWindow < lowerEndEntryWindow + minTravelTime) {
      lowerEndExitWindow = lowerEndEntryWindow + minTravelTime;
    }
    
    if (upperEndExitWindow < lowerEndExitWindow) {
      return null;
    }
    
    if (upperEndEntryWindow + minTravelTime > upperEndExitWindow) {
      upperEndEntryWindow = upperEndExitWindow - minTravelTime;
    }
    
    Range<Long> entryWindow = Range.closed(lowerEndEntryWindow, upperEndEntryWindow);
    Range<Long> exitWindow = Range.closed(lowerEndExitWindow, upperEndExitWindow);
    Range<Long> timeWindow = Range.closed(lowerEndEntryWindow, upperEndExitWindow);
    
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
    // remove all old reservations
    for (Map.Entry<Point, List<Reservation>> entry : reservationMap.entrySet()) {
      List<Reservation> reservationList = entry.getValue();
      Iterator<Reservation> iter = reservationList.iterator();
      while (iter.hasNext()) {
        Reservation reservation = iter.next();
        if (reservation.getAgvID() == agvID
            && reservation.getLifeTime() != lifeTime) {
          iter.remove();
        }
      }
    }
    final long lowerEndPoint = interval.lowerEndpoint();
    final long upperEndPoint = interval.upperEndpoint();
    reservationMap.get(startPoint)
        .add(new Reservation(agvID, lifeTime, Range.open(lowerEndPoint, upperEndPoint)));
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

  public Point getNode1() {
    return node1;
  }

  public Point getNode2() {
    return node2;
  }
  
  
}
