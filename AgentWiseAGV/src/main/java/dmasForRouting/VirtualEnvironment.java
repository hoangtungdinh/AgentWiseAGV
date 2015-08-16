package dmasForRouting;

import com.github.rinde.rinsim.core.model.road.CollisionGraphRoadModel;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;

import resourceAgents.EdgeAgentList;
import resourceAgents.NodeAgentList;

/**
 * The Class VirtualEnvironment.
 *
 * @author Tung
 */
public class VirtualEnvironment implements TickListener {
  
  /** The road model. */
  private CollisionGraphRoadModel roadModel;
  
  /** The node agent list. */
  private NodeAgentList nodeAgentList;
  
  /** The edge agent list. */
  private EdgeAgentList edgeAgentList;
  
  /**
   * Instantiates a new virtual environment.
   *
   * @param roadModel the road model
   */
  public VirtualEnvironment(CollisionGraphRoadModel roadModel) {
    this.roadModel = roadModel;
    nodeAgentList = new NodeAgentList(roadModel);
    edgeAgentList = new EdgeAgentList(roadModel);
  }
  
  

  @Override
  public void tick(TimeLapse timeLapse) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void afterTick(TimeLapse timeLapse) {
    // TODO Auto-generated method stub
    
  }
}
