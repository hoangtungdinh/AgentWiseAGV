package destinationGenerator;

import java.util.List;

import com.github.rinde.rinsim.geom.Point;

/**
 * The Class DestinationLists.
 *
 * @author Tung
 */
public class DestinationList {
  
  /** The destination lists. */
  private List<Point> destinationList;

  /**
   * Instantiates a new destination lists.
   *
   * @param destinationLists the destination lists
   */
  public DestinationList(List<Point> destinationList) {
    this.destinationList = destinationList;
  }

  /**
   * Gets the destination list.
   *
   * @return the destination list
   */
  public List<Point> getDestinationList() {
    return destinationList;
  }
  
}
