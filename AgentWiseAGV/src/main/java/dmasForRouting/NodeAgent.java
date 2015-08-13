package dmasForRouting;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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
  private Map<Range<Long>, ReservationID> reservationInfo;

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
    reservationInfo = new HashMap<>();
    shortestPathLength = new HashMap<>();
  }
  
  /**
   * Gets the free time windows.
   *
   * @return the free time windows
   */
  public RangeSet<Long> getFreeTimeWindows() {
    return reservationList.complement();
  }
  
  /**
   * Adds the requested time window.
   *
   * @param requestedTimeWindow the requested time window
   * @param lifeTime the life time
   * @param id the id
   * @return true, if successfully added
   */
  public boolean addRequestedTimeWindow(Range<Long> requestedTimeWindow, int lifeTime, int id) {
    // get all overlapping range
    RangeSet<Long> overlappingRanges = this.reservationList.subRangeSet(requestedTimeWindow);
    
    if (overlappingRanges.isEmpty()) {
      // if there is no overlapping range
      // add time window to the reservation list
      this.reservationList.add(requestedTimeWindow);
      // create reservation ID
      this.reservationInfo.put(requestedTimeWindow, new ReservationID(id, lifeTime));
      return true;
    } else {
      // if there exists overlapping range(s)
      Set<Range<Long>> setOfOverlappingRanges = overlappingRanges.asRanges();
      for (Range<Long> range : setOfOverlappingRanges) {
        // get a time point inside the overlapping range
        final long arbitraryTimePoint = range.lowerEndpoint() + 1;
        // get the existing range in the reservation list that that overlaps with the requested range
        final Range<Long> existedTimeWindow = reservationList.rangeContaining(arbitraryTimePoint);
        // TODO handle when existedTimeWindow is null
        // get the info of the existing range
        final ReservationID reservationID = reservationInfo.get(existedTimeWindow);
        if (reservationID.getID() != id) {
          // if the existing range was booked by another AGV, then not add the new reservation
          return false;
        } else {
          // if the existing range was booked by the requesting AGV, then remove the old range and add the new range
          reservationList.remove(existedTimeWindow);
          reservationInfo.remove(existedTimeWindow);
        }
      }
      // this line can only be reached if all overlapping time window were booked by the requesting AGV
      reservationList.add(requestedTimeWindow);
      reservationInfo.put(requestedTimeWindow, new ReservationID(id, lifeTime));
      return true;
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
