package pathSampling;

import java.util.ArrayList;
import java.util.List;

import com.github.rinde.rinsim.geom.Point;

/**
 * The Class Path.
 *
 * @author Tung
 */
public class Path {
  
  /** The path. */
  private List<Point> path;
  
  /**
   * Instantiates a new path.
   *
   * @param path the path
   */
  public Path(List<Point> path) {
    this.path = path;
  }
  
  /**
   * Instantiates a new path.
   */
  public Path() {
    path = new ArrayList<>();
  }
  
  /**
   * Gets the path.
   *
   * @return the path
   */
  public List<Point> getPath() {
    return path;
  }
}
