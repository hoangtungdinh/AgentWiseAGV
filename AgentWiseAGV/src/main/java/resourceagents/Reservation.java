package resourceagents;

import com.google.common.collect.Range;

/**
 * The Class ReservationID. The class contains the ID of the AGV and the life
 * time of the reservation
 *
 * @author Tung
 */
public class Reservation {
  
  /** The id of the AGV. */
  private int agvID;
  
  /** The life time of the reservation. */
  private long lifeTime;
  
  /** The reservation interval. */
  private Range<Long> interval;

  /**
   * Instantiates a new reservation.
   *
   * @param agvID the agv id
   * @param lifeTime the life time
   * @param interval the interval
   */
  public Reservation(int agvID, long lifeTime, Range<Long> interval) {
    this.agvID = agvID;
    this.lifeTime = lifeTime;
    this.interval = interval;
  }

  /**
   * Gets the agv id.
   *
   * @return the agv id
   */
  public int getAgvID() {
    return agvID;
  }

  /**
   * Gets the life time.
   *
   * @return the life time
   */
  public long getLifeTime() {
    return lifeTime;
  }

  /**
   * Gets the interval.
   *
   * @return the interval
   */
  public Range<Long> getInterval() {
    return interval;
  }
  
  /**
   * Update life time.
   *
   * @param newLifeTime the new life time
   */
  public void updateLifeTime(long newLifeTime) {
    this.lifeTime = newLifeTime;
  }
}
