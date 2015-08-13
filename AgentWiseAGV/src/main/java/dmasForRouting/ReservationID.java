package dmasForRouting;

/**
 * The Class ReservationID. The class contains the ID of the AGV and the life
 * time of the reservation
 *
 * @author Tung
 */
public class ReservationID {
  
  /** The id of the AGV. */
  private int id;
  
  /** The life time of the reservation. */
  private int lifeTime;
  
  public ReservationID(int id, int lifeTime) {
    this.id = id;
    this.lifeTime = lifeTime;
  }

  /**
   * Gets the id of the AGV.
   *
   * @return the id of the AGV
   */
  public int getID() {
    return id;
  }

  /**
   * Gets the life time of the reservation.
   *
   * @return the life time of the reservation
   */
  public int getLifeTime() {
    return lifeTime;
  }
  
  /**
   * Decrease life time by one unit
   *
   * @return true, if successful
   */
  public boolean decreaseLifeTime() {
    if (lifeTime == 0) {
      return false;
    } else {
      lifeTime--;
      return true;
    }
  }

  /**
   * Sets the life time.
   *
   * @param lifeTime the new life time
   */
  public void setLifeTime(int lifeTime) {
    this.lifeTime = lifeTime;
  }
}
