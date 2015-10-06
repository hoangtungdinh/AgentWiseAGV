package resourceagents;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.rinde.rinsim.geom.Point;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;

import routeplan.RangeEndPoint;
import routeplan.contextaware.SingleStep;
import routeplan.contextaware.SwapRequest;
import setting.Setting;

/**
 * The Class EdgeAgent.
 *
 * @author Tung
 */
public class EdgeAgent implements ResourceAgent {
  
  /**
   * The reservation map. There are always two reservation lists in the map,
   * corresponding to two entrances. They key is the start point of the edge.
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
  
  private LinkedList<SingleStep> backupList;
  
  private LinkedList<SingleStep> orderedList;
  
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
    this.orderedList = null;
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
      // the AGV can understand that a reservation is still valid if two
      // conditions are satisfied
      // 1. the reservation is of another AGV
      // 2. the reservation does not totally lie in the possible entry window,
      // so the entry window is not separated into two ranges
      // if the 2nd condition is not satisfied, it means that an AGV removed its
      // reservation at node, but not at the edge
      if (reservation.getAgvID() != agvID
          && !realPossibleEntryWindow.encloses(reservation.getInterval())) {
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
      throw new IllegalStateException("More than one entry window for edge!");
    }
    
    Range<Long> optimisticEntryWindow = entryWindows.span();
    
    // list of reservations from the same direction
    final List<Reservation> reservationsFromSameDirection = reservationMap
        .get(startPoint);
    
    // the range that there cannot be any reservation start inside. If there is
    // a reservation having start time in this range, then this reservation is
    // unused (due to changing intention)
    final long rangeWithoutReservationLowerEndPoint = (long) (realPossibleEntryWindow
        .lowerEndpoint()
        - setting.getVehicleLength() * 1000 / setting.getVehicleSpeed());
    final Range<Long> rangeWithoutReservation;
    if (realPossibleEntryWindow.hasUpperBound()) {
      final long rangeWithoutReservationUpperEndpoint = (long) (realPossibleEntryWindow
          .upperEndpoint()
          + setting.getVehicleLength() * 1000 / setting.getVehicleSpeed());
      rangeWithoutReservation = Range.open(rangeWithoutReservationLowerEndPoint,
          rangeWithoutReservationUpperEndpoint);
    } else {
      rangeWithoutReservation = Range.greaterThan(rangeWithoutReservationLowerEndPoint);
    }
    
    // compute the capacity at the start time
    long startTime = optimisticEntryWindow.lowerEndpoint();
    List<Reservation> overlappingReservations = new ArrayList<>();
    
    Iterator<Reservation> iter = reservationsFromSameDirection.iterator();
    while (iter.hasNext()) {
      final Reservation resv = iter.next();
      final long resvStartTime = resv.getInterval().lowerEndpoint();
      if (rangeWithoutReservation.contains(resvStartTime) && resv.getAgvID() != agvID) {
        iter.remove();
      } else if (resv.getInterval().contains(startTime) && resv.getAgvID() != agvID) {
        overlappingReservations.add(resv);
      }
    }
    
    // if the edge is already full, we have to calculate the start time
    // note that, because the capacity of the node is only 1, it means that
    // during the possible entry window, the capacity can only decrease. It
    // cannot increase because no vehicle can enter the edge while the current
    // AGV is occupying the start node.
    if (overlappingReservations.size() > capacity) {
      throw new IllegalStateException("More AGVs than capacity");
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
      // ignore reservation of the same agv and unused reservation
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
    
    // example: (728300‥756300) (744300‥758300)
    if (lowerEndExitWindow > upperEndExitWindow) {
      return null;
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
    checkNotNull(feasibleTimeWindow, "cannot be null %s", feasibleTimeWindow);
    
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
      if (resv.getInterval().contains(upperEndExitWindow)
          && resv.getAgvID() != agvID) {
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
      if (resv.getInterval().contains(upperEndExitWindow)
          && resv.getAgvID() != agvID) {
        if (resv.getInterval().lowerEndpoint() > latestEntryTimeOfOtherAGVs) {
          latestEntryTimeOfOtherAGVs = resv.getInterval().lowerEndpoint();
        }
      }
    }
    
    // check capacity at the entry time of the latest AGV
    List<Reservation> overlappingAtLatestEntryTime = new ArrayList<>();
    for (Reservation resv : reservationsFromSameDirection) {
      if (resv.getInterval().contains(latestEntryTimeOfOtherAGVs)
          && resv.getAgvID() != agvID) {
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
    for (Map.Entry<Point, List<Reservation>> entry : reservationMap
        .entrySet()) {
      List<Reservation> reservationList = entry.getValue();
      Iterator<Reservation> iter = reservationList.iterator();
      while (iter.hasNext()) {
        Reservation reservation = iter.next();
        if (reservation.getAgvID() == agvID
            && reservation.getLifeTime() != lifeTime) {
          // The condition mean that we remove the old reservation of the
          // agvID at this resource
          iter.remove();
        } else {
          final long currentStartTime = interval.lowerEndpoint();
          final long existingStartTime = reservation.getInterval().lowerEndpoint();
          if (currentStartTime == existingStartTime) {
            // the condition mean that we detect an invalid reservation
            // (due to intention changing) of another agv and remove that
            // reservation
            iter.remove();
          }
        }
      }
    }
    final long lowerEndPoint = interval.lowerEndpoint();
    final long upperEndPoint = interval.upperEndpoint();
    reservationMap.get(startPoint)
        .add(new Reservation(agvID, lifeTime, Range.open(lowerEndPoint, upperEndPoint)));
  }
  
  /**
   * Modify a reservation of agvID. Note that we only change the startTime of
   * the reservation, so the endTime is always still the same
   *
   * @param agvID the agv id
   * @param startPoint the start point
   * @param interval the interval
   * @param modifiedEndPoint the modified end point
   */
  public void modifyReservation(int agvID, Point startPoint, Range<Long> interval, RangeEndPoint modifiedEndPoint) {
    final long startTime = interval.lowerEndpoint();
    final long endTime = interval.upperEndpoint();
    
    final List<Reservation> reservations = reservationMap.get(startPoint);
    
    for (Reservation reservation : reservations) {
      if (modifiedEndPoint == RangeEndPoint.LOWER) {
        // if we modify the lower end point, then we check if the upper end points are similar
        final long existingEndTime = reservation.getInterval().upperEndpoint();
        if (reservation.getAgvID() == agvID
            && existingEndTime == endTime) {
          final Range<Long> newInterval = Range.open(startTime, endTime);
          reservation.setNewInterval(newInterval);
          return;
        }
      } else {
        // if we modify the upper end point, then we check if the lower end points are similar
        final long existingStartTime = reservation.getInterval().lowerEndpoint();
        if (reservation.getAgvID() == agvID
            && existingStartTime == startTime) {
          final Range<Long> newInterval = Range.open(startTime, endTime);
          reservation.setNewInterval(newInterval);
          return;
        }
      }
    }
    
    throw new IllegalStateException("No reservation to modify");
  }
  
  /**
   * Refresh reservation.
   *
   * @param startPoint the start point
   * @param endPoint the end point
   * @param interval the interval
   * @param lifeTime the life time
   * @param agvID the agv id
   * @return true, if successful
   */
  public boolean refreshReservation(Point startPoint, Point endPoint,
      Range<Long> interval, long lifeTime, int agvID) {
    
    final long lowerEndPoint = interval.lowerEndpoint();
    final long upperEndPoint = interval.upperEndpoint();
    final Range<Long> newInterval = Range.open(lowerEndPoint, upperEndPoint);
    
    // check if the reservation is overlapped with any reservation from the
    // opposite direction
    final List<Reservation> resvFromOppositeDirection = reservationMap
        .get(endPoint);
    for (Reservation resv : resvFromOppositeDirection) {
      if (resv.getAgvID() != agvID
          && resv.getInterval().isConnected(newInterval)) {
        return false;
      }
    }

    // check if the reservation is overlapped with any reservation from the same
    // direction
    final List<Reservation> resvFromTheSameDirection = reservationMap
        .get(startPoint);
    for (Reservation resv : resvFromTheSameDirection) {
      if (resv.getAgvID() != agvID
          && isOverlapping(resv.getInterval(), newInterval)) {
        return false;
      }
    }

    // if there is no overlap then add reservation
    addReservation(startPoint, newInterval, lifeTime, agvID);
    return true;
  }
  
  /**
   * Checks if two reservations of two agvs from the same direction is overlapping
   *
   * @param firstRange the range of the first agv
   * @param secondRange the range of the second agv
   * @return true, if is overlapping
   */
  public boolean isOverlapping(Range<Long> firstRange, Range<Long> secondRange) {
    final long minDifferentTime = (long) (setting.getVehicleLength() * 1000
        / setting.getVehicleSpeed());
    
    final long start1 = firstRange.lowerEndpoint();
    final long start2 = secondRange.lowerEndpoint();
    final long end1 = firstRange.upperEndpoint();
    final long end2 = secondRange.upperEndpoint();
    
    if (start1 <= start2 - minDifferentTime
        && end1 <= end2 - minDifferentTime) {
      return false;
    } else if (start1 >= start2 + minDifferentTime
        && end1 >= end2 + minDifferentTime) {
      return false;
    } else {
      return true;
    }
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
  
  /**
   * Sets the reservation of 'agvID' that contains 'time' as visited.
   *
   * @param agvID the agv id
   * @param endTime the end time of the interval
   * @param startNode the start node
   */
  public void setVisited(int agvID, long endTime, Point startNode) {
    // get reservations of AGVs coming from startNode
    final List<Reservation> reservations = reservationMap.get(startNode);
    
    for (Reservation resv : reservations) {
      final long existingEndTime = resv.getInterval().upperEndpoint();
      if (resv.getAgvID() == agvID && existingEndTime == endTime) {
        resv.setVisited();
        return;
      }
    }
    
//    throw new IllegalStateException("No reservation to set visited");
  }
  
  /**
   * Sets the first order visited. Only for context aware
   */
  public void setFirstOrderVisited() {
    orderedList.getFirst().setVisited();
  }
  
  /**
   * Gets the list of delayed agvs at the startTime
   *
   * @param agvID the agv id
   * @param startTime the start time according to the plan of the 'agvID'
   * @return the list of delayed agvs
   */
  public List<Integer> getListOfDelayedAGVs(int agvID, long startTime, Point startNode) {
    // get the reservations of AGVs coming from the startNode
    final List<Reservation> reservations = reservationMap.get(startNode);
    
    List<Integer> delayedAGV = new ArrayList<>();
    
    for (Reservation resv : reservations) {
      // if the start point of the resv is before the start time and the resv
      // hasn't marked as visited and the resv is of another agv
      if (resv.getInterval().lowerEndpoint() < startTime && !resv.hasVisited()
          && resv.getAgvID() != agvID) {
        delayedAGV.add(resv.getAgvID());
      }
    }
    return delayedAGV;
  }
  
  public void removeAllReservations() {
    reservationMap.get(node1).clear();
    reservationMap.get(node2).clear();
  }
  
  public List<Reservation> getReservations(Point startPoint) {
    return reservationMap.get(startPoint);
  }
  
  /**
   * Removes the reservations of several AGVs.
   *
   * @param agvList the agv list
   */
  public void removeReservationsOf(List<Integer> agvList) {
    for (Map.Entry<Point, List<Reservation>> entry : reservationMap.entrySet()) {
      List<Reservation> reservationList = entry.getValue();
      Iterator<Reservation> iter = reservationList.iterator();
      while (iter.hasNext()) {
        Reservation reservation = iter.next();
        if (agvList.contains(reservation.getAgvID())) {
          iter.remove();
        }
      }
    }
  }
  
  public void removeReservationOfOneAGV(int agvID) {
    for (Map.Entry<Point, List<Reservation>> entry : reservationMap.entrySet()) {
      List<Reservation> reservationList = entry.getValue();
      Iterator<Reservation> iter = reservationList.iterator();
      while (iter.hasNext()) {
        Reservation reservation = iter.next();
        if (reservation.getAgvID() == agvID) {
          iter.remove();
        }
      }
    }
  }
  
  public void createOrderList() {
    if (orderedList != null) {
      throw new IllegalStateException("This method should be called only once!");
    }
    
    // first create a reservation list with all reservations from node1
    final List<Reservation> resvList = new ArrayList<>(reservationMap.get(node1));
    // add all reservations from node2 to that list
    resvList.addAll(reservationMap.get(node2));
    
    Collections.sort(resvList);
    
    orderedList = new LinkedList<>();
    for (Reservation resv : resvList) {
      final SingleStep singleStep = new SingleStep(resv.getAgvID(), resv.getInterval().lowerEndpoint());
      orderedList.add(singleStep);
    }
  }
  
  public int getNextAGV() {
    for (SingleStep step : orderedList) {
      if (!step.isVisited()) {
        return step.getAgvID();
      }
    }
    checkState(false, "Cannot find the next agv");
    return -1;
  }
  
  public void setNonSwappable() {
    for (SingleStep step : orderedList) {
      if (!step.isVisited()) {
        step.setNonSwappable();
        return;
      }
    }
    checkState(false, "Cannot find the next agv");
  }
  
  public void removeFirstAGV() {
    orderedList.removeFirst();
  }

  @Override
  public String toString() {
    return ("Edge: " + node1 + " to " + node2);
  }
  
  public List<SingleStep> getOrderedList() {
    return orderedList;
  }

  public void createBackUp() {
    backupList = new LinkedList<>(orderedList);
  }
  
  public void rollback() {
    orderedList = new LinkedList<>(backupList);
  }

  public Set<Integer> getPrecedingAGVs(SingleStep currentStep) {
    final Set<Integer> delayedAGVs = new HashSet<>();
    for (SingleStep singleStep : orderedList) {
      if (singleStep.equals(currentStep)) {
        return delayedAGVs;
      } else {
        if (singleStep.getAgvID() != currentStep.getAgvID()) {
          delayedAGVs.add(singleStep.getAgvID());
        }
      }
    }
    checkState(false, "cannot find the current step");
    return null;
  }

  public Set<SingleStep> getDelayedSteps(SingleStep currentStep,
      Set<Integer> currentDelayedAGVs) {
    boolean startCount = false;
    final Set<SingleStep> delayedSteps = new HashSet<>();
    for (SingleStep singleStep : orderedList) {
      if (singleStep.equals(currentStep)) {
        return delayedSteps;
      } else {
        if (currentDelayedAGVs.contains(singleStep.getAgvID())) {
          startCount = true;
        }
        
        if (startCount) {
          if (singleStep.getAgvID() != currentStep.getAgvID()) {
            delayedSteps.add(singleStep);
          }
        }
      }
    }
    checkState(false, "cannot find the current step");
    return null;
  }

  public void swapOrder(SwapRequest swapRequest) {
    final SingleStep stepToBeSwapped = swapRequest.getStepToBeSwapped();
    final Set<SingleStep> delayedSteps = swapRequest.getDelayedSteps();
    final boolean removeSuccess = orderedList.remove(stepToBeSwapped);
    
    checkState(removeSuccess, "Cannot find the step to be swapped");
    
    int index = -1;
    for (int i = 0; i < orderedList.size(); i++) {
      if (delayedSteps.contains(orderedList.get(i))) {
        index = i;
        break;
      }
    }
    
    checkState(index != -1, "Cannot find any delayed step");
    
    orderedList.add(index, stepToBeSwapped);
  }
}
