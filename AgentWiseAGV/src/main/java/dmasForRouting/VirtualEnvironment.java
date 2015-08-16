package dmasForRouting;

import com.github.rinde.rinsim.core.model.road.CollisionGraphRoadModel;

/**
 * The Class VirtualEnvironment.
 *
 * @author Tung
 */
public class VirtualEnvironment {
  
  /** The road model. */
  private CollisionGraphRoadModel roadModel;
  
  /**
   * Instantiates a new virtual environment.
   *
   * @param roadModel the road model
   */
  public VirtualEnvironment(CollisionGraphRoadModel roadModel) {
    this.roadModel = roadModel;
  }
}
