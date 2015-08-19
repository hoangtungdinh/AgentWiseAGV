package virtualEnvironment;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import org.apache.commons.math3.random.RandomGenerator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.internal.cocoa.id;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Link;

import com.github.rinde.rinsim.core.model.road.CollisionGraphRoadModel;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.collect.Range;

import dmasForRouting.AGVSystem;
import others.PlanStep;
import others.RoutePlan;
import others.RoutePlan.ResourceElement;
import pathSampling.Path;
import pathSampling.PathSampling;
import resourceAgents.EdgeAgent;
import resourceAgents.EdgeAgentList;
import resourceAgents.FreeTimeWindow;
import resourceAgents.NodeAgent;
import resourceAgents.NodeAgentList;
import routePlan.PlanFTW;

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
    
    // sampling the environment to get several feasible paths
    final List<Path> feasiblePaths = PathSampling.getFeasiblePaths(origin, destination, numOfPaths);
    final List<PlanFTW> feasiblePlans = new ArrayList<>();
    
    for (Path path : feasiblePaths) {
      final List<Point> candPath = path.getPath();

      // free time window of the start node
      final List<FreeTimeWindow> firstFreeTimeWindows = nodeAgentList
          .getNodeAgent(candPath.get(0))
          .getFreeTimeWindows(Range.atLeast(startTime), agvID);
      
      FreeTimeWindow startFTW = null;
      
      // there should be only one free time window that contains the startTime
      for (FreeTimeWindow ftw : firstFreeTimeWindows) {
        if (ftw.getEntryWindow().contains(startTime)) {
          startFTW = ftw;
          break;
        }
      }
      
      // if no possible free time window then next candidate path
      if (startFTW == null) {
        continue;
      }
      
      LinkedList<FreeTimeWindow> firstFTW = new LinkedList<>();
      firstFTW.addLast(startFTW);
      
      Stack<PlanFTW> planStack = new Stack<>();
      planStack.push(new PlanFTW(firstFTW, candPath));
      
      while (!planStack.isEmpty()) {
        final PlanFTW plan = planStack.pop();
        final LinkedList<FreeTimeWindow> currentFTWs = plan.getFreeTimeWindows();
        final int planLength = currentFTWs.size();
        
        // if all the resources have been planned
        if (planLength == (2*candPath.size() - 1)) {
          feasiblePlans.add(plan);
          continue;
        }

        // list of next feasible free time windows
        List<FreeTimeWindow> nextFTWs;
        // check whether the last plan step is for a node or for an edge
        if (planLength % 2 == 1) {
          // the last plan step is for a node. Now we plan for the next edge
          final int index = planLength / 2;
          final EdgeAgent edgeAgent = edgeAgentList
              .getEdgeAgent(candPath.get(index), candPath.get(index + 1));
          nextFTWs = edgeAgent.getFreeTimeWindows(candPath.get(index),
              candPath.get(index + 1), currentFTWs.getLast().getExitWindow(),
              agvID);
        } else {
          // the last plan step is for an edge. Now we plan for the next node
          final int index = planLength / 2;
          final NodeAgent nodeAgent = nodeAgentList
              .getNodeAgent(candPath.get(index));
          nextFTWs = nodeAgent
              .getFreeTimeWindows(currentFTWs.getLast().getExitWindow(), agvID);
        }

        for (FreeTimeWindow ftw : nextFTWs) {
          final LinkedList<FreeTimeWindow> newFtwList = new LinkedList<>(
              currentFTWs);
          newFtwList.addLast(ftw);
          planStack.push(new PlanFTW(newFtwList, candPath));
        }
      }
    }
    
    // find the best plan (the one that the AGV arrives at destination earliest)
    PlanFTW bestPlan = null;
    for (PlanFTW planFTW : feasiblePlans) {
      if (bestPlan == null
          || bestPlan.getArrivalTime() > planFTW.getArrivalTime()) {
        bestPlan = planFTW;
      }
    }

    // TODO create route plan
      
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
