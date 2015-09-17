package multistage.garagemodel.delegatemas;

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
  
  @Override
  public boolean equals(Object other) {
    if (other == null) {
      return false;
    }
    if (other == this) {
      return true;
    }
    // allows comparison with subclasses
    if (!(other instanceof Path)) {
      return false;
    }
    final Path otherPath = (Path) other;
    return this.path.equals(otherPath.getPath());
  }
}
