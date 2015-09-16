package resourceagents;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.github.rinde.rinsim.geom.Point;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;

import setting.Setting;

/**
 * The Class ResourceAgent.
 *
 * @author Tung
 */
public class NodeAgent implements ResourceAgent {
  
 /** The reservations. */
 private List<Reservation> reservations;
 
 /** The node that the agent associated. */
 private Point node;

  /** The setting. */
  private Setting setting;
  
  /**
   * Instantiates a new node agent.
   *
   * @param node the node
   * @param setting the setting
   */
  public NodeAgent(Point node, Setting setting) {
    reservations = new ArrayList<>();
    this.node = node;
    this.setting = setting;
  }
  
  /**
   * Gets the free time windows.
   *
   * @param possibleEntryWindow the possible entry window
   * @param agvID the agv id
   * @return the free time windows
   */
  public List<FreeTimeWindow> getFreeTimeWindows(Range<Long> possibleEntryWindow, int agvID) {
    
    // actual entry window (take into account the length of vehicles)
    Range<Long> realPossibleEntryWindow;
    final long lowerEndPoint = possibleEntryWindow.lowerEndpoint()
        - ((long) (setting.getVehicleLength()*1000 / setting.getVehicleSpeed()));
    if (possibleEntryWindow.hasUpperBound()) {
      final long upperEndPoint = possibleEntryWindow.upperEndpoint()
          - ((long) (setting.getVehicleLength() / setting.getVehicleSpeed()));
      realPossibleEntryWindow = Range.closed(lowerEndPoint, upperEndPoint);
    } else {
      realPossibleEntryWindow = Range.atLeast(lowerEndPoint);
    }
    
    // create the timeline of existing reservations
    RangeSet<Long> existingReservations = TreeRangeSet.create();
    for (Reservation reservation : reservations) {
      if (reservation.getAgvID() != agvID) {
        existingReservations.add(reservation.getInterval());
      }
    }
    
    // all possible free range
    RangeSet<Long> freeRanges = existingReservations.complement()
        .subRangeSet(Range.atLeast(realPossibleEntryWindow.lowerEndpoint()));
  
    // mininum travel time required for AGVs to traverse a node
    final long minTravelTime = (long) ((setting.getVehicleLength()*2*1000) / setting.getVehicleSpeed());
    
    // list of free time windows
    List<FreeTimeWindow> freeTimeWindows = new ArrayList<>();
    
    // to be a free time window, a range must be equal or longer than the minimum travel time
    for (Range<Long> range : freeRanges.asRanges()) {
      // if the free range is not connected to the possible entry window
      if (!range.isConnected(realPossibleEntryWindow)) {
        continue;
      }
      
      if (!range.hasUpperBound()) {
        // if there is no upper bound
        final Range<Long> entryWindow = range.intersection(realPossibleEntryWindow);
        final Range<Long> exitWindow = Range
            .atLeast(entryWindow.lowerEndpoint() + minTravelTime);
        final Range<Long> timeWindow = Range
            .atLeast(entryWindow.lowerEndpoint());
        freeTimeWindows
            .add(new FreeTimeWindow(timeWindow, entryWindow, exitWindow));
      } else
        if (range.upperEndpoint() - range.lowerEndpoint() >= minTravelTime) {
        // if there is upper bound but the range is still longer than the
        // minimum travel time
        final Range<Long> optimisticEntryWindow = Range
            .closed(range.lowerEndpoint(), range.upperEndpoint() - minTravelTime);

        if (!optimisticEntryWindow.isConnected(realPossibleEntryWindow)) {
          continue;
        }

        final Range<Long> entryWindow = optimisticEntryWindow
            .intersection(realPossibleEntryWindow);
        final Range<Long> exitWindow = Range.closed(
            entryWindow.lowerEndpoint() + minTravelTime,
            range.upperEndpoint());
        final Range<Long> timeWindow = Range.closed(entryWindow.lowerEndpoint(),
            exitWindow.upperEndpoint());
        freeTimeWindows
            .add(new FreeTimeWindow(timeWindow, entryWindow, exitWindow));
      }
    }
    
    return freeTimeWindows;
  }
  
  /**
   * Adds the reservation.
   *
   * @param agvID the agv id
   * @param lifeTime the life time
   * @param interval the interval
   */
  public void addReservation(int agvID, long lifeTime, Range<Long> interval) {
    // remove all old reservation of the same AGV
    Iterator<Reservation> iter = reservations.iterator();
    while (iter.hasNext()) {
      Reservation reservation = iter.next();
      if (reservation.getAgvID() == agvID
          && reservation.getLifeTime() != lifeTime) {
        iter.remove();
      }
    }
    
    final long lowerEndPoint = interval.lowerEndpoint();
    final long upperEndPoint = interval.upperEndpoint();
    reservations.add(new Reservation(agvID, lifeTime, Range.open(lowerEndPoint, upperEndPoint)));
  }
  
  /**
   * Refresh reservation.
   *
   * @param agvID the agv id
   * @param lifeTime the life time
   * @param interval the interval
   * @return true, if successful refreshing
   */
  public boolean refreshReservation(int agvID, long lifeTime, Range<Long> interval) {
    final long lowerEndPoint = interval.lowerEndpoint();
    final long upperEndPoint = interval.upperEndpoint();
    final Range<Long> newInterval = Range.open(lowerEndPoint, upperEndPoint);
    
    Iterator<Reservation> iter = reservations.iterator();
    // go through all existing reservations
    while (iter.hasNext()) {
      Reservation reservation = iter.next();
      
      if (reservation.getAgvID() == agvID
          && reservation.getLifeTime() != lifeTime) {
        // remove the reservations of the 'agvID'
        iter.remove();
      } else if (reservation.getInterval().isConnected(newInterval)) {
        // if there is another reservation that is overlapping with the current
        // plan, then refreshing fails, return false
        return false;
      }
    }
    
    // if there is no overlapping reservations, then refresh by adding new reservation
    reservations.add(new Reservation(agvID, lifeTime, newInterval));
    return true;
  }
  
  /**
   * Removes the out dated reservation.
   *
   * @param currentTime the current time
   */
  public void removeOutDatedReservation(long currentTime) {
    Iterator<Reservation> iter = reservations.iterator();
    while (iter.hasNext()) {
      Reservation reservation = iter.next();
      if (reservation.getLifeTime() < currentTime) {
        iter.remove();
      }
    }
  }
  
  public Point getNode() {
    return node;
  }
  
  /**
   * Sets the reservation of agvID that contains time as visited.
   *
   * @param agvID the agv id
   * @param time the time
   */
  public void setVisited(int agvID, long time) {
    for (Reservation resv : reservations) {
      if (resv.getAgvID() == agvID && resv.getInterval().contains(time)) {
        resv.setVisited();
        return;
      }
    }
  }
  
  /**
   * Gets the list of delayed agvs at the startTime
   *
   * @param agvID the agv id
   * @param startTime the start time according to the plan of the 'agvID'
   * @return the list of delayed agvs
   */
  public List<Integer> getListOfDelayedAGVs(int agvID, long startTime) {
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
    reservations.clear();
  }
}
