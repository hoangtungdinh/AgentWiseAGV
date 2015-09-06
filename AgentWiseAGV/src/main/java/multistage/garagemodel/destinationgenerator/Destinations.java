package multistage.garagemodel.destinationgenerator;

import java.util.LinkedList;
import java.util.List;

import com.github.rinde.rinsim.geom.Point;

/**
 * The Class Destinations.
 *
 * @author Tung
 */
public class Destinations {
  
  /** The destinations. */
  private LinkedList<Point> destinations;

  /**
   * Instantiates a new destinations.
   *
   * @param destinations the destinations
   */
  public Destinations(List<Point> destinations) {
    this.destinations = new LinkedList<>(destinations);
  }

  /**
   * Gets the destination.
   *
   * @return the destination
   */
  public Point getDestination() {
    if (destinations.isEmpty()) {
      throw new IllegalStateException("There is no destination left!");
    }
    return destinations.removeFirst();
  }
}