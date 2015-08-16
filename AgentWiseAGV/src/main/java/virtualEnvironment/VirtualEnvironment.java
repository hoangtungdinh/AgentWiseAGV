package virtualEnvironment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.commons.math3.random.RandomGenerator;

import com.github.rinde.rinsim.core.model.road.CollisionGraphRoadModel;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.collect.Range;

import dmasForRouting.AGVSystem;
import resourceAgents.EdgeAgentList;
import resourceAgents.FreeTimeWindow;
import resourceAgents.NodeAgentList;
import routePlan.RoutePlan;

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
  
  /** The random generator. */
  private RandomGenerator randomGenerator;
  
  /**
   * Instantiates a new virtual environment.
   *
   * @param roadModel the road model
   * @param randomGenerator the random generator
   */
  public VirtualEnvironment(CollisionGraphRoadModel roadModel,
      RandomGenerator randomGenerator) {
    this.roadModel = roadModel;
    nodeAgentList = new NodeAgentList(roadModel);
    edgeAgentList = new EdgeAgentList(roadModel);
    this.randomGenerator = randomGenerator;
  }
  
//  public RoutePlan exploreRoute(int agvID, long startTime, Point startNode,
//      Point destination) {
//    // get all free time windows of the start node
//    List<FreeTimeWindow> freeTimeWindowsOnCurrentNode = nodeAgentList
//        .getNodeAgent(startNode)
//        .getFreeTimeWindows(Range.atLeast(startTime), agvID);
//
//    // minimum travel time for the first node
//    // TODO is that calculation correct?
//    final long minTravelTime = (long) (AGVSystem.VEHICLE_LENGTH
//        / AGVSystem.VEHICLE_SPEED);
//
//    // list of free time windows that contain start time
//    List<FreeTimeWindow> availableFreeTimeWindows = new ArrayList<>();
//    for (FreeTimeWindow ftw : freeTimeWindowsOnCurrentNode) {
//      if (ftw.getInterval().contains(startTime + 1)) {
//        if (!ftw.getExitWindow().hasUpperBound() || ftw.getEntryWindow()
//            .upperEndpoint() >= startTime + minTravelTime) {
//          availableFreeTimeWindows.add(ftw);
//        }
//      }
//    }
//
//    // get all directly connected nodes
//    Collection<Point> connectedNodes = roadModel.getGraph()
//        .getOutgoingConnections(startNode);
//    
//    
//
//  }

  @Override
  public void tick(TimeLapse timeLapse) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void afterTick(TimeLapse timeLapse) {
    // TODO Auto-generated method stub
    
  }
}
