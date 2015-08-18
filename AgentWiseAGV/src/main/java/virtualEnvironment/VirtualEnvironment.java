package virtualEnvironment;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.apache.commons.math3.random.RandomGenerator;

import com.github.rinde.rinsim.core.model.road.CollisionGraphRoadModel;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.collect.Range;

import dmasForRouting.AGVSystem;
import pathSampling.Path;
import pathSampling.PathSampling;
import resourceAgents.EdgeAgent;
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
  }
  
  public RoutePlan exploreRoute(int agvID, long startTime, Point origin,
      Point destination, int numOfPaths) {
    
    final List<Path> feasiblePaths = PathSampling.getFeasiblePaths(origin, destination, numOfPaths);
    
    for (Path path : feasiblePaths) {
      final List<Point> candPath = path.getPath();
      final Stack<RoutePlan> planStack = new Stack<>();
      
      // minimum travel time for a node
      final long minNodeTravelTime = (long) (2 * AGVSystem.VEHICLE_LENGTH / AGVSystem.VEHICLE_SPEED);
      
      // free time window of the start node
      final List<FreeTimeWindow> firstFreeTimeWindows = nodeAgentList
          .getNodeAgent(candPath.get(0))
          .getFreeTimeWindows(Range.atLeast(startTime), agvID);
      
      FreeTimeWindow startFTW = null;
      
      for (FreeTimeWindow ftw : firstFreeTimeWindows) {
        if (ftw.getEntryWindow().contains(startTime)) {
          startFTW = ftw;
          break;
        }
      }
      
      if (startFTW == null) {
        continue;
      }
      
      planStack.push(new RoutePlan(candPath, startFTW.getExitWindow()));

      while (!planStack.isEmpty()) {
        RoutePlan routePlan = planStack.pop();

        final int index = routePlan.getCurrentIndex();
        final Range<Long> exitWindow = routePlan.getCurrentExitWindow();
        final Point currentNode = candPath.get(index);
        final Point nextNode = candPath.get(index + 1);

        // select edge schedule
        // select edge agent
        final EdgeAgent edgeAgent = edgeAgentList
            .getEdgeAgent(currentNode, nextNode);
        
        // list of free time window corresponding to the edge
        final List<FreeTimeWindow> edgeFTWs = edgeAgent
            .getFreeTimeWindows(currentNode, nextNode, exitWindow, agvID);
        
        
      }
    }
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
