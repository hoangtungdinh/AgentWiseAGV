package singlestage.delegatemas;

import java.util.ArrayList;
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
import resourceagents.Reservation;
import routeplan.CheckPoint;
import routeplan.Plan;
import routeplan.ResourceType;
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
  
  /** The agv list. */
  private List<VehicleAgent> agvList;
  
  /**
   * Instantiates a new virtual environment.
   *
   * @param roadModel the road model
   * @param randomGenerator the random generator
   * @param setting the setting
   */
  public VirtualEnvironment(CollisionGraphRoadModel roadModel,
      RandomGenerator randomGenerator, Setting setting,
      List<VehicleAgent> agvList) {
    this.setting = setting;
    nodeAgentList = new NodeAgentList(roadModel, setting);
    edgeAgentList = new EdgeAgentList(roadModel, setting);
    this.pathSampling = new PathSampling(setting, randomGenerator);
    this.agvList = agvList;
  }
  
  /**
   * Explore route.
   *
   * @param agvID the agv id
   * @param startTime the possible start time
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
  
  /**
   * Propagate delay.
   * The currentPlan must start from the node that the AGV is freezing.
   *
   * @param agvID the agv id
   * @param currentPlan the current plan
   */
  public void propagateDelay(int agvID, long currentTime) {
    
    final List<Integer> affectedAGVs = getAffectedAGVs(agvID, currentTime);
    nodeAgentList.removeReservationsOf(affectedAGVs);
    edgeAgentList.removeReservationsOf(affectedAGVs);
    
    // we will modify the plan of all agvs when one is delayed
    for (Integer affectedAGV : affectedAGVs) {
      final VehicleAgent agv = agvList.get(affectedAGV);
      
      // if the agv has reached the destination then ignore
      if (agv.hasCompleted()) {
        continue;
      }
      
      // first we detect the current plan step
      final Plan currentPlan = agv.getCurrentPlan();
      final List<Range<Long>> reservedIntervals = currentPlan.getIntervals();
      final int idxOfCurrentResv = getIndexOfCurrentResv(reservedIntervals, currentTime);
      final LinkedList<Range<Long>> newReservations = new LinkedList<>();
      
      // add all reservations until the reservation at current time
      for (int i = 0; i < idxOfCurrentResv; i++) {
        newReservations.addLast(reservedIntervals.get(i));
      }
      
      // modify the reservation at the current time then add it to the new reservation
      final Range<Long> currentReservation = reservedIntervals.get(idxOfCurrentResv);
      final long newLowerEndPoint = currentReservation.lowerEndpoint();
      final long newUpperEndPoint = currentReservation.upperEndpoint() + setting.getExpectedFreezingDuration();
      newReservations.addLast(Range.closed(newLowerEndPoint, newUpperEndPoint));
      
      // modify the reservation of all future reservations
      for (int i = idxOfCurrentResv + 1; i < reservedIntervals.size(); i++) {
        final Range<Long> oldReservation = reservedIntervals.get(i);
        final long updatedLowerEndPoint = oldReservation.lowerEndpoint() + setting.getExpectedFreezingDuration();
        final long updatedUpperEndPoint = oldReservation.upperEndpoint() + setting.getExpectedFreezingDuration();
        newReservations.addLast(Range.closed(updatedLowerEndPoint, updatedUpperEndPoint));
      }
      
      // create new plan for the agv
      final Plan newPlan = new Plan(currentPlan.getPath(), newReservations, false);
      
      // notify the agv about the new plan
      agv.notifyDelay(newPlan, currentTime);
    }
  }
  
  public int getIndexOfCurrentResv(List<Range<Long>> reservedIntervals, long currentTime) {
    int index = 0;
    // for each reservation
    for (Range<Long> interval : reservedIntervals) {
      // detect the first reservation that contains currentTime
      if (interval.contains(currentTime)) {
        if (reservedIntervals.get(index + 1).contains(currentTime)) {
          // when currentTime is in two consecutive intervals, mean that the agv
          // is on two resources (edges, node or node, edges).
          
          // first, we consider the case that the agv is at exactly a node and
          // is going to move to edge, then the current time is of the interval
          // at a node and is also the start time of the next interval
          if (index % 2 == 0 && reservedIntervals.get(index + 1).lowerEndpoint() == currentTime) {
            // the first condition says that the index is of a node, the second
            // condition is about the start time of edge
            // in this case, we return index, mean that the interval of the node
            return index;
          }
          
          // for all other case, we only consider the interval of the second resource
          return index + 1;
        } else {
          return index;
        }
      } else {
        index++;
      }
    }
    
    // this line can happen when the agvs have not entered the map yet. In multi
    // stage, it should be removed and we should throw an exception here.
    return 0;
  }
  
  /**
   * Gets all the affected agvs.
   *
   * @param agvID the agv id
   * @param currentTime the current time
   * @return the affected ag vs
   */
  public List<Integer> getAffectedAGVs(int agvID, long currentTime) {
    final LinkedList<Integer> affectedAGVs = new LinkedList<>();
    final List<Integer> investigatedAGVs = new ArrayList<>();
    
    affectedAGVs.add(agvID);
    
    while (!affectedAGVs.isEmpty()) {

      final int currentAGV = affectedAGVs.removeFirst();
      
      if (investigatedAGVs.contains(currentAGV)) {
        continue;
      } else {
        investigatedAGVs.add(currentAGV);
      }
      
      final List<Point> path = agvList.get(currentAGV).getCurrentPlan().getPath();
      final List<Range<Long>> intervals = agvList.get(currentAGV).getCurrentPlan()
          .getIntervals();

      final int currentIndex = getIndexOfCurrentResv(intervals, currentTime);

      for (int i = currentIndex; i < intervals.size(); i++) {
        final List<Reservation> reservations;
        
        if (i % 2 == 0) {
          // node
          final NodeAgent nodeAgent = nodeAgentList
              .getNodeAgent(path.get(i / 2));
          reservations = nodeAgent.getReservations();
        } else {
          // edge
          final EdgeAgent edgeAgent = edgeAgentList
              .getEdgeAgent(path.get(i / 2), path.get(i / 2 + 1));
          reservations = edgeAgent.getReservations(path.get(i / 2));
        }
        
        final Range<Long> delayedInterval = intervals.get(i);
        final long startTimeOfDelayedInterval = delayedInterval.lowerEndpoint();

        for (Reservation resv : reservations) {
          if (resv.getInterval().lowerEndpoint() > startTimeOfDelayedInterval) {
            affectedAGVs.add(resv.getAgvID());
          }
        }
      }
    }
    
    return investigatedAGVs;
  }
  
  /**
   * set a plan step (a reservation) as visited.
   *
   * @param agvID the agv id
   * @param nextCheckPoint the next check point
   */
  public void setVisited(int agvID, CheckPoint nextCheckPoint) {
    final List<Point> resource = nextCheckPoint.getResource();
    if (nextCheckPoint.getResourceType() == ResourceType.NODE) {
      // if the resource is a node
      final NodeAgent nodeAgent = nodeAgentList.getNodeAgent(resource.get(0));
      nodeAgent.setVisited(agvID, nextCheckPoint.getExpectedTime());
    } else {
      // if the resource is an edge
      final EdgeAgent edgeAgent = edgeAgentList.getEdgeAgent(resource.get(0), resource.get(1));
      edgeAgent.setVisited(agvID, nextCheckPoint.getExpectedTime(), resource.get(0));
    }
  }
  
  /**
   * Gets the list of higher priority agvs that have not entered the resource (count from the startTime)
   *
   * @param agvID the agv id
   * @param startTime the start time according to the plan of the 'agvID'
   * @param resource the resource
   * @return the list of delayed agvs
   */
  public List<Integer> getListOfHigherPriorityAGVs(int agvID, long startTime, List<Point> resource) {
    if (resource.size() == 1) {
      // if the resource is a node 
      final NodeAgent nodeAgent = nodeAgentList.getNodeAgent(resource.get(0));
      return nodeAgent.getListOfDelayedAGVs(agvID, startTime);
    } else {
      // if the resource is an edge
      final EdgeAgent edgeAgent = edgeAgentList.getEdgeAgent(resource.get(0), resource.get(1));
      return edgeAgent.getListOfDelayedAGVs(agvID, startTime, resource.get(0));
    }
  }
  
  public void modifyReservation(int agvID, List<Point> resource, Range<Long> interval) {
    if (resource.size() == 1) {
      // if the resource is a node
      final NodeAgent nodeAgent = nodeAgentList.getNodeAgent(resource.get(0));
      nodeAgent.modifyReservation(agvID, interval);
    } else {
      // if the resource is an edge
      final EdgeAgent edgeAgent = edgeAgentList.getEdgeAgent(resource.get(0), resource.get(1));
      edgeAgent.modifyReservation(agvID, resource.get(0), interval);
    }
  }
  
  @Override
  public void tick(TimeLapse timeLapse) {
    
  }

  @Override
  public void afterTick(TimeLapse timeLapse) {
    nodeAgentList.removeOutDatedReservation(timeLapse.getEndTime());
    edgeAgentList.removeOutdatedReservations(timeLapse.getEndTime());
    if (timeLapse.getEndTime() > 500000) {
      throw new IllegalStateException("Gridlock happens!!!");
    }
  }
}