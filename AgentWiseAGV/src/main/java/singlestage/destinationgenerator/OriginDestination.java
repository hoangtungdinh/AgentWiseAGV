package singlestage.destinationgenerator;

import com.github.rinde.rinsim.geom.Point;

/**
 * The Class Destinations.
 *
 * @author Tung
 */
public class OriginDestination {
  
  /** The origin. */
  private Point origin;
  
  /** The destination. */
  private Point destination;

  /**
   * Instantiates a new origin destination.
   *
   * @param origin the origin
   * @param destination the destination
   */
  public OriginDestination(Point origin, Point destination) {
    this.origin = origin;
    this.destination = destination;
  }

  /**
   * Gets the origin.
   *
   * @return the origin
   */
  public Point getOrigin() {
    return origin;
  }

  /**
   * Gets the destination.
   *
   * @return the destination
   */
  public Point getDestination() {
    return destination;
  }
}
