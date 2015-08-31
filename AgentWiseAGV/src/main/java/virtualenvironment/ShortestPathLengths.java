package virtualenvironment;

import java.util.Map;

import com.github.rinde.rinsim.geom.Point;

/**
 * The Class ShortestPathLengths.
 */
public class ShortestPathLengths {

  /** The shortest path lengths. */
  private Map<Point, Double> shortestPathLengths;
  
  /**
   * Instantiates a new shortest path lengths.
   *
   * @param shortestPathLengths the shortest path lengths
   */
  public ShortestPathLengths(Map<Point, Double> shortestPathLengths) {
    this.shortestPathLengths = shortestPathLengths;
  }
  
  /**
   * Gets the length.
   *
   * @param node the node
   * @return the length
   */
  public double getLength(Point node) {
    return shortestPathLengths.get(node);
  }
}
