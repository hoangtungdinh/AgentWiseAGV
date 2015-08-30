package resourceAgents;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.github.rinde.rinsim.core.model.road.CollisionGraphRoadModel;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import dmasForRouting.Setting;

/**
 * The Class EdgeAgentList.
 *
 * @author Tung
 */
public class EdgeAgentList {
  
  /** The table containing edge agents. */
  private Table<Point, Point, EdgeAgent> edgeAgentTable;
  
  private List<EdgeAgent> edgeAgentList;
  
  /**
   * Instantiates a new edge agent list.
   *
   * @param roadModel the road model
   * @param setting the setting
   */
  public EdgeAgentList(CollisionGraphRoadModel roadModel, Setting setting) {
    edgeAgentTable = HashBasedTable.create();
    edgeAgentList = new ArrayList<>();

    Set<Point> allNodes = roadModel.getGraph().getNodes();
    for (Point node : allNodes) {
      Collection<Point> connectedNodes = roadModel.getGraph()
          .getOutgoingConnections(node);
      for (Point outgoingNode : connectedNodes) {
        if (!edgeAgentTable.contains(node, outgoingNode)) {
          EdgeAgent edgeAgent = new EdgeAgent(node, outgoingNode, roadModel
              .getGraph().getConnection(node, outgoingNode).getLength(),
              setting);
          edgeAgentTable.put(node, outgoingNode, edgeAgent);
          edgeAgentTable.put(outgoingNode, node, edgeAgent);
          edgeAgentList.add(edgeAgent);
        }
      }
    }
  }
  
  /**
   * Gets the edge agent.
   *
   * @param p1 the first node
   * @param p2 the second node
   * @return the edge agent
   */
  public EdgeAgent getEdgeAgent(Point p1, Point p2) {
    return edgeAgentTable.get(p1, p2);
  }
  
  /**
   * Removes the outdated reservations.
   *
   * @param currentTime the current time
   */
  public void removeOutdatedReservations(long currentTime) {
    for (EdgeAgent edgeAgent : edgeAgentList) {
      edgeAgent.removeOutdatedReservations(currentTime);
    }
  }
}
