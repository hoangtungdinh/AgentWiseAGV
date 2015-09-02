package multistage.delegatemas;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import org.apache.commons.math3.random.RandomGenerator;

import com.github.rinde.rinsim.core.model.road.CollisionGraphRoadModel;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.collect.Range;

import resourceagents.EdgeAgent;
import resourceagents.EdgeAgentList;
import resourceagents.FreeTimeWindow;
import resourceagents.NodeAgent;
import resourceagents.NodeAgentList;
import routeplan.Plan;
import routeplan.delegatemas.PlanFTW;
import setting.Setting;

/**
 * The Class VirtualEnvironment.
 *
 * @author Tung
 */
public class VirtualEnvironment implements TickListener {
  
  /** The node agent list. */
  private NodeAgentList nodeAgentList;
  
  /** The edge agent list. */
  private EdgeAgentList edgeAgentList;
  
  /** The setting. */
  private Setting setting;
  
  /** The path sampling. */
  private PathSampling pathSampling;
  
  /**
   * Instantiates a new virtual environment.
   *
   * @param roadModel the road model
   * @param randomGenerator the random generator
   * @param setting the setting
   */
  public VirtualEnvironment(CollisionGraphRoadModel roadModel,
      RandomGenerator randomGenerator, Setting setting) {
    this.setting = setting;
    nodeAgentList = new NodeAgentList(roadModel, setting);
    edgeAgentList = new EdgeAgentList(roadModel, setting);
    this.pathSampling = new PathSampling(setting);
  }
  
  /**
   * Explore route.
   *
   * @param agvID the agv id
   * @param startTime the start time
   * @param origin the origin
   * @param destinations the destinations
   * @param numOfPaths the num of paths
   * @param centralStation the central station
   * @return the plan
   */
  public Plan exploreRoute(int agvID, long startTime, Point origin,
      List<Point> destinations, int numOfPaths, List<Point> centralStation) {
    
    // sampling the environment to get several feasible paths
    final List<Path> feasiblePaths = pathSampling.getFeasiblePaths(origin,
        destinations, numOfPaths, centralStation);
    
    final List<PlanFTW> feasiblePlans = new ArrayList<>();
    
    for (Path path : feasiblePaths) {
      final List<Point> candPath = path.getPath();

      // free time window of the start node
      final List<FreeTimeWindow> firstFreeTimeWindows = nodeAgentList
          .getNodeAgent(candPath.get(0))
          .getFreeTimeWindows(Range.atLeast(startTime), agvID);
      
      FreeTimeWindow startFTW = null;

      // there should be only one free time window that contains the startTime
      final long realStartTime = startTime - ((long) (setting.getVehicleLength()
          * 1000 / setting.getVehicleSpeed()));
      for (FreeTimeWindow ftw : firstFreeTimeWindows) {
        if (ftw.getEntryWindow().contains(realStartTime)
            || ftw.getEntryWindow().lowerEndpoint() == realStartTime) {
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
        
        if (nextFTWs == null) {
          continue;
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
    
    if (bestPlan == null) {
      return null;
    }
    
    // generate actual plan from the time window plan
    final LinkedList<Range<Long>> intervals = new LinkedList<>();
    final List<FreeTimeWindow> freeTimeWindows = bestPlan.getFreeTimeWindows();
    final FreeTimeWindow lastFreeTimeWindow = freeTimeWindows
        .get(freeTimeWindows.size() - 1);
    
    intervals.addFirst(
        Range.closed(lastFreeTimeWindow.getEntryWindow().lowerEndpoint(),
            lastFreeTimeWindow.getExitWindow().lowerEndpoint()));
 
    for (int i = freeTimeWindows.size() - 2; i >= 0; i--) {
      intervals.addFirst(
          Range.closed(freeTimeWindows.get(i).getEntryWindow().lowerEndpoint(),
              intervals.getFirst().lowerEndpoint()
                  + ((long) (setting.getVehicleLength() * 1000
                      / setting.getVehicleSpeed()))));
    }
    
    Plan plan = new Plan(bestPlan.getPath(), intervals);
    
    return plan;
  }
  
  /**
   * Make reservation.
   *
   * @param agvID the agv id
   * @param plan the plan
   * @param lifeTime the life time
   */
  public void makeReservation(int agvID, Plan plan, long currentTime, long lifeTime) {
    List<Point> path = plan.getPath();
    List<Range<Long>> intervals = plan.getIntervals();
    for (int i = 0; i < path.size() - 1; i++) {
      if (intervals.get(i * 2 + 1).upperEndpoint() < currentTime) {
        continue;
      }
      
      final NodeAgent nodeAgent = nodeAgentList.getNodeAgent(path.get(i));
      nodeAgent.addReservation(agvID, lifeTime, intervals.get(i * 2));
      final EdgeAgent edgeAgent = edgeAgentList.getEdgeAgent(path.get(i),
          path.get(i + 1));
      edgeAgent.addReservation(path.get(i), intervals.get(i * 2 + 1), lifeTime,
          agvID);
    }
    
    final NodeAgent lastNodeAgent = nodeAgentList
        .getNodeAgent(path.get(path.size() - 1));
    lastNodeAgent.addReservation(agvID, lifeTime,
        intervals.get(intervals.size() - 1));
  }
  
  @Override
  public void tick(TimeLapse timeLapse) {
    
  }

  @Override
  public void afterTick(TimeLapse timeLapse) {
    nodeAgentList.removeOutDatedReservation(timeLapse.getEndTime());
    edgeAgentList.removeOutdatedReservations(timeLapse.getEndTime());
  }
}
