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
 * The Class ResourceAgent.
 *
 * @author Tung
 */
public class NodeAgent {
  
 /** The reservations. */
 private List<Reservation> reservations;
 
 /** The node that the agent associated. */
 private Point node;

  /**
   * The shortest path length. It stores the shortest path length between the
   * associated node and other nodes.
   */
  private Map<Point, Double> shortestPathLength;
  
  /**
   * Instantiates a new node agent.
   */
  public NodeAgent(Point node) {
    reservations = new ArrayList<>();
    shortestPathLength = new HashMap<>();
    this.node = node;
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
        - ((long) (AGVSystem.VEHICLE_LENGTH*1000 / AGVSystem.VEHICLE_SPEED));
    if (possibleEntryWindow.hasUpperBound()) {
      final long upperEndPoint = possibleEntryWindow.upperEndpoint()
          - ((long) (AGVSystem.VEHICLE_LENGTH / AGVSystem.VEHICLE_SPEED));
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
    final long minTravelTime = (long) ((AGVSystem.VEHICLE_LENGTH*2*1000) / AGVSystem.VEHICLE_SPEED);
    
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
    final long lowerEndPoint = interval.lowerEndpoint();
    final long upperEndPoint = interval.upperEndpoint();
    reservations.add(new Reservation(agvID, lifeTime, Range.open(lowerEndPoint, upperEndPoint)));
  }
  
  /**
   * Refresh reservation.
   * Basically, it just adds new reservation
   *
   * @param agvID the agv id
   * @param lifeTime the life time
   * @param interval the interval
   */
  public void refreshReservation(int agvID, long lifeTime, Range<Long> interval) {
    addReservation(agvID, lifeTime, interval);;
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

  public Point getNode() {
    return node;
  }
  
  
}
