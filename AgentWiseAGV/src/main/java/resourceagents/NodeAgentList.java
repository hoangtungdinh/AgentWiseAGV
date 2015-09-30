package resourceagents;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.rinde.rinsim.core.model.road.CollisionGraphRoadModel;
import com.github.rinde.rinsim.geom.Point;

import setting.Setting;

/**
 * The Class NodeAgentList.
 *
 * @author Tung
 */
public class NodeAgentList {
  
  /** The node agent map. */
  private Map<Point, NodeAgent> nodeAgentMap;
  
  /** The node agent list. */
  private List<NodeAgent> nodeAgentList;
  
  /**
   * Instantiates a new node agent list.
   *
   * @param roadModel the road model
   * @param setting the setting
   */
  public NodeAgentList(CollisionGraphRoadModel roadModel, Setting setting) {
    nodeAgentMap = new HashMap<>();
    nodeAgentList = new ArrayList<>();
    
    Set<Point> allNodes = roadModel.getGraph().getNodes();
    for (Point node : allNodes) {
      NodeAgent nodeAgent = new NodeAgent(node, setting);
      nodeAgentMap.put(node, nodeAgent);
      nodeAgentList.add(nodeAgent);
    }
  }
  
  /**
   * Gets the node agent.
   *
   * @param node the node
   * @return the node agent
   */
  public NodeAgent getNodeAgent(Point node) {
    return nodeAgentMap.get(node);
  }
  
  /**
   * Removes the out dated reservation.
   *
   * @param currentTime the current time
   */
  public void removeOutDatedReservation(long currentTime) {
    for (NodeAgent nodeAgent : nodeAgentList) {
      nodeAgent.removeOutDatedReservation(currentTime);
    }
  }
  
  public void removeAllReservations() {
    for (NodeAgent nodeAgent : nodeAgentList) {
      nodeAgent.removeAllReservations();
    }
  }
  
  public void removeReservationsOf(List<Integer> agvList) {
    for (NodeAgent nodeAgent : nodeAgentList) {
      nodeAgent.removeReservationsOf(agvList);
    }
  }
  
  public void createOrderList() {
    for (NodeAgent nodeAgent : nodeAgentList) {
      nodeAgent.createOrderList();
    }
  }
  
  public List<NodeAgent> getAllNodeAgents() {
    return nodeAgentList;
  }
}
