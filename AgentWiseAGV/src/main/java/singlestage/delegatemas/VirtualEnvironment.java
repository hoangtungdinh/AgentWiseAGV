package singlestage.delegatemas;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.math3.random.RandomGenerator;

import com.github.rinde.rinsim.core.model.road.CollisionGraphRoadModel;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Graphs;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.collect.Range;

import resourceagents.EdgeAgent;
import resourceagents.EdgeAgentList;
import resourceagents.FreeTimeWindow;
import resourceagents.NodeAgent;
import resourceagents.NodeAgentList;
import routeplan.Plan;
import routeplan.delegatemas.PlanFTW;
import routeplan.delegatemas.PlanStep;
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
   * @param started the started
   * @return the plan
   */
  public Plan exploreRoute(int agvID, Range<Long> startTime, Point origin,
      List<Point> destinations, int numOfPaths, boolean started) {
    
    // sampling the environment to get several feasible paths
    final List<Path> feasiblePaths = pathSampling.getFeasiblePaths(origin,
        destinations, numOfPaths);
    
    final SortedMap<Long, PlanFTW> planQueue = new TreeMap<>();
    final Set<PlanStep> closedSet = new LinkedHashSet<>();
    
    for (Path path : feasiblePaths) {
      final List<Point> candPath = path.getPath();
      
      // free time window of the start node
      final List<FreeTimeWindow> firstFreeTimeWindows = nodeAgentList
          .getNodeAgent(candPath.get(0)).getFreeTimeWindows(startTime, agvID);

      for (FreeTimeWindow startFTW : firstFreeTimeWindows) {
        // for each possible start free time window, create a plan step and
        // add it to the queue
        final LinkedList<FreeTimeWindow> firstFTW = new LinkedList<>();
        firstFTW.addLast(startFTW);
        
        final PlanStep planStep = new PlanStep(candPath, 0, startFTW);
        closedSet.add(planStep);

        final PlanFTW firstPlanFTW = new PlanFTW(firstFTW, candPath);
        
        long estimatedCost = computeCost(firstPlanFTW);
        while (planQueue.containsKey(estimatedCost)) {
          estimatedCost++;
        }
        planQueue.put(estimatedCost, firstPlanFTW);
      }
    }
    
    PlanFTW bestPlan = null;
    
    while (!planQueue.isEmpty()) {
      final PlanFTW plan = planQueue.remove(planQueue.firstKey());
      final List<Point> candPath = plan.getPath();
      final LinkedList<FreeTimeWindow> currentFTWs = plan.getFreeTimeWindows();
      final int planLength = currentFTWs.size();

      // if all the resources have been planned
      if (planLength == (2 * candPath.size() - 1)) {
        bestPlan = plan;
        break;
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
        final PlanStep planStep = new PlanStep(candPath, planLength, ftw);
        if (closedSet.contains(planStep)) {
          continue;
        } else {
          closedSet.add(planStep);
        }
        final LinkedList<FreeTimeWindow> newFtwList = new LinkedList<>(
            currentFTWs);
        newFtwList.addLast(ftw);
        final PlanFTW newPlanFTW = new PlanFTW(newFtwList, candPath);
        long estimatedCost = computeCost(newPlanFTW);
        while (planQueue.containsKey(estimatedCost)) {
          estimatedCost++;
        }
        planQueue.put(estimatedCost, newPlanFTW);
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
    
    Plan plan = new Plan(bestPlan.getPath(), intervals, false);
    
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
  
  /**
   * Refresh reservation.
   *
   * @param agvID the agv id
   * @param plan the plan
   * @param currentTime the current time
   * @param lifeTime the life time
   * @return true, if successful
   */
  public boolean refreshReservation(int agvID, Plan plan, long currentTime, long lifeTime) {
    List<Point> path = plan.getPath();
    List<Range<Long>> intervals = plan.getIntervals();
    for (int i = 0; i < path.size() - 1; i++) {
      if (intervals.get(i * 2 + 1).upperEndpoint() < currentTime) {
        continue;
      }
      
      final NodeAgent nodeAgent = nodeAgentList.getNodeAgent(path.get(i));
      final boolean successNode = nodeAgent.refreshReservation(agvID, lifeTime, intervals.get(i * 2));
      if (!successNode) {
        return false;
      }
      
      final EdgeAgent edgeAgent = edgeAgentList.getEdgeAgent(path.get(i),
          path.get(i + 1));
      final boolean successEdge = edgeAgent.refreshReservation(path.get(i), path.get(i + 1), intervals.get(i * 2 + 1), lifeTime,
          agvID);
      if (!successEdge) {
        return false;
      }
    }
    
    final NodeAgent lastNodeAgent = nodeAgentList
        .getNodeAgent(path.get(path.size() - 1));
    final boolean success = lastNodeAgent.refreshReservation(agvID, lifeTime,
        intervals.get(intervals.size() - 1));
    return success;
  }
  
  public long computeCost(PlanFTW plan) {
    final LinkedList<FreeTimeWindow> currentFTWs = plan.getFreeTimeWindows();
    final List<Point> path = plan.getPath();
    final int planLength = currentFTWs.size();
    final long earliestExitTime = plan.getEarliestExitTime();
    final int index = planLength / 2;
    
    final List<Point> remainingPath;
    final double remainingPathLength;
    final long estimatedCost;
    
    if (planLength % 2 == 1) {
      // the last plan step is for a node.
      remainingPath = path.subList(index, path.size());
      remainingPathLength = Graphs.pathLength(remainingPath);
      // if the plan stop at a node (note that the exit time is the time when
      // the vehicle is completely out of the node
      estimatedCost = earliestExitTime
          + ((long) (remainingPathLength * 1000 / setting.getVehicleSpeed()))
          - ((long) (setting.getVehicleLength() * 1000
              / setting.getVehicleSpeed()));
    } else {
      // the last plan step is for an edge. Now we plan for the next node
      // example: plan at edge with planLength = 2, then index = 1
      remainingPath = path.subList(index, path.size());
      remainingPathLength = Graphs.pathLength(remainingPath);
      // if the plan stop at an edge, the exit time is the time when the vehicle
      // is exactly at the central of the next node
      estimatedCost = earliestExitTime
          + ((long) (remainingPathLength * 1000 / setting.getVehicleSpeed()));
    }
    
    return estimatedCost;
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
