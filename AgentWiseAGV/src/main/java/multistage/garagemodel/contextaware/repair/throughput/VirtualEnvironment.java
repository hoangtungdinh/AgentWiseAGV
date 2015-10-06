package multistage.garagemodel.contextaware.repair.throughput;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.math3.random.RandomGenerator;

import com.github.rinde.rinsim.core.model.road.CollisionGraphRoadModel;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Graphs;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.collect.Range;

import heuristic.ShortestPathLengths;
import resourceagents.EdgeAgent;
import resourceagents.EdgeAgentList;
import resourceagents.FreeTimeWindow;
import resourceagents.NodeAgent;
import resourceagents.NodeAgentList;
import routeplan.CheckPoint;
import routeplan.Plan;
import routeplan.contextaware.PlanFTW;
import routeplan.contextaware.PlanStep;
import routeplan.contextaware.SingleStep;
import routeplan.contextaware.SwapRequest;
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
  
  /** The road model. */
  private CollisionGraphRoadModel roadModel;
  
  /** The setting. */
  private Setting setting;
  
  /** The agv list. */
  private List<VehicleAgent> agvList;
  
  private int numOfPlannedAGVs;
  
  private boolean createdOrderList;
  
  /**
   * shortest path lengths from all nodes to a destination the keys are
   * destinations, the values are the map of points and shortest path lengths
   */
  final Map<Point, ShortestPathLengths> shortestLengthToDest;
  
  /**
   * Instantiates a new virtual environment.
   *
   * @param roadModel the road model
   * @param randomGenerator the random generator
   * @param setting the setting
   */
  public VirtualEnvironment(CollisionGraphRoadModel roadModel,
      RandomGenerator randomGenerator, Setting setting, List<VehicleAgent> agvList) {
    this.setting = setting;
    nodeAgentList = new NodeAgentList(roadModel, setting);
    edgeAgentList = new EdgeAgentList(roadModel, setting);
    this.roadModel = roadModel;
    this.shortestLengthToDest = new HashMap<>();
    this.agvList = agvList;
    this.numOfPlannedAGVs = 0;
    this.createdOrderList = false;
  }
  
  /**
   * Explore route.
   *
   * @param agvID the agv id
   * @param startTime the start time
   * @param origin the origin
   * @param destinations the destinations
   * @param stationExits the station exits
   * @return the plan
   */
  public Plan exploreRoute(int agvID, Range<Long> startTime, Point origin,
      List<Point> destinations, List<Point> garageList) {
    
    // shortest path lengths from all nodes to a destination
    // the keys are destinations, the values are the map of points and shortest path lengths
    for (Point node : destinations) {
      if (!shortestLengthToDest.containsKey(node)) {
        shortestLengthToDest.put(node,
            new ShortestPathLengths(shortestPathLengthsTo(node)));
      }
    }

    // free time window of the start node
    final List<FreeTimeWindow> firstFreeTimeWindows = nodeAgentList
        .getNodeAgent(origin).getFreeTimeWindows(startTime, agvID);
    
    final SortedMap<Long, PlanFTW> planQueue = new TreeMap<>();
    final Set<PlanStep> closedSet = new LinkedHashSet<>();
    
    if (firstFreeTimeWindows.size() > 1) {
      throw new IllegalStateException("More than one first free time window");
    }
    
    for (FreeTimeWindow startFTW : firstFreeTimeWindows) {
      final List<Point> firstPath = new ArrayList<>();
      firstPath.add(origin);
      final LinkedList<FreeTimeWindow> firstFTW = new LinkedList<>();
      firstFTW.addLast(startFTW);

      final PlanFTW firstPlanFTW = new PlanFTW(firstFTW, firstPath, 0,
          destinations);

      planQueue.put(
          computeCost(firstPlanFTW, shortestLengthToDest, destinations),
          firstPlanFTW);

      final PlanStep planStep = new PlanStep(firstPlanFTW.getStage(),
          nodeAgentList.getNodeAgent(origin), startFTW);
      closedSet.add(planStep);
    }

    PlanFTW finalPlan = null;

    while (!planQueue.isEmpty()) {
      // select and remove the first plan in the queue
      final PlanFTW planFTW = planQueue.remove(planQueue.firstKey());
      
      final List<Point> path = planFTW.getPath();
      final List<FreeTimeWindow> ftwList = planFTW.getFreeTimeWindows();
      
      if ((planFTW.getStage() == destinations.size())
          && path.get(path.size() - 1)
              .equals(destinations.get(destinations.size() - 1))
          && ftwList.size() % 2 == 1) {
        // if it is a complete plan then break
        // The first condition says that the AGV is at the last stage (only one
        // more destination to reach). The second condition says that the AGV
        // has reached the last destination. The last condition say that the
        // plan finish at a node (which is the last destination)
        finalPlan = planFTW;
        break;
      }
      
      if (ftwList.size() % 2 == 1) {
        // if the last plan step is for a node
        // get all possible next node
        final List<Point> nextNodes = new ArrayList<>();
        nextNodes.addAll(roadModel.getGraph()
            .getOutgoingConnections(path.get(path.size() - 1)));
        for (Point nextNode : nextNodes) {
          // for each possible next node
          if (!planFTW.isValid(nextNode) || (garageList.contains(nextNode)
              && !nextNode.equals(destinations.get(destinations.size() - 1)))) {
            // we do not allow cyclic plans and plans contain the garages of other agv
            continue;
          }
          
          // check the new stage
          int newStage = -1;
          if (nextNode.equals(destinations.get(planFTW.getStage()))) {
            newStage = planFTW.getStage() + 1;
          } else {
            newStage = planFTW.getStage();
          }
          
          // now we get the free time window of the edge
          // call the edge agent
          final EdgeAgent edgeAgent = edgeAgentList
              .getEdgeAgent(path.get(path.size() - 1), nextNode);
          List<FreeTimeWindow> nextFTWs = edgeAgent.getFreeTimeWindows(
              path.get(path.size() - 1), nextNode,
              ftwList.get(ftwList.size() - 1).getExitWindow(), agvID);
          final List<Point> newPath = new ArrayList<>(path);
          newPath.add(nextNode);
          if (nextFTWs == null) {
            continue;
          }
          for (FreeTimeWindow newFTW : nextFTWs) {
            // add investigated plan step to the closedSet
            final PlanStep planStep = new PlanStep(newStage, edgeAgent, newFTW);
            if (closedSet.contains(planStep)) {
              continue;
            } else {
              closedSet.add(planStep);
            }
            
            LinkedList<FreeTimeWindow> newListOfFTWs = new LinkedList<>(
                ftwList);
            newListOfFTWs.addLast(newFTW);
            PlanFTW newPlanFTW = new PlanFTW(newListOfFTWs, newPath, newStage, destinations);
            // add next plan step to the queue
            // solve problem when have the same cost
            long estimatedCost = computeCost(newPlanFTW, shortestLengthToDest, destinations);
            while (planQueue.containsKey(estimatedCost)) {
              estimatedCost++;
            }
            planQueue.put(estimatedCost, newPlanFTW);
          }
        }
      } else {
        // if the last plan step is for an edge
        // call the node agent
        final NodeAgent nodeAgent = nodeAgentList
            .getNodeAgent(path.get(path.size() - 1));
        List<FreeTimeWindow> nextFTWs = nodeAgent.getFreeTimeWindows(
            ftwList.get(ftwList.size() - 1).getExitWindow(), agvID);
        for (FreeTimeWindow newFTW : nextFTWs) {
          final PlanStep planStep = new PlanStep(planFTW.getStage(), nodeAgent, newFTW);
          if (closedSet.contains(planStep)) {
            continue;
          } else {
            closedSet.add(planStep);
          }
          
          LinkedList<FreeTimeWindow> newListOfFTWs = new LinkedList<>(ftwList);
          newListOfFTWs.addLast(newFTW);
          getClass();
          PlanFTW newPlanFTW = new PlanFTW(newListOfFTWs, path, planFTW.getStage(), destinations);
          long estimatedCost = computeCost(newPlanFTW, shortestLengthToDest, destinations);
          while (planQueue.containsKey(estimatedCost)) {
            estimatedCost++;
          }
          planQueue.put(estimatedCost, newPlanFTW);
        }
      }
    }

    // generate actual plan from the time window plan
    final LinkedList<Range<Long>> intervals = new LinkedList<>();
    final List<FreeTimeWindow> freeTimeWindows = finalPlan.getFreeTimeWindows();
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

    Plan plan = new Plan(finalPlan.getPath(), intervals, false);
    
    return plan;
  }
  
  /**
   * Compute cost.
   *
   * @param planFTW the plan
   * @param shortestLengths the shortest lengths
   * @param destinations the destinations
   * @return the cost
   */
  public long computeCost(PlanFTW planFTW,
      Map<Point, ShortestPathLengths> shortestLengths, List<Point> destinations) {
    
    final List<Point> path = planFTW.getPath();
    final List<FreeTimeWindow> ftwList = planFTW.getFreeTimeWindows();
    
    long estimatedCost = -1;
    
    // calculate the length of the shortest path
    double lengthShortestPath = 0;
    for (int i = planFTW.getStage(); i < destinations.size(); i++) {
      if (i == planFTW.getStage()) {
        lengthShortestPath += shortestLengths.get(destinations.get(i))
            .getLength(path.get(path.size() - 1));
      } else {
        lengthShortestPath += shortestLengths.get(destinations.get(i))
            .getLength(destinations.get(i - 1));
      }
    }
    
    final long earliestExitTime = planFTW.getEarliestExitTime();
    
    if (ftwList.size() % 2 == 1) {
      // if the plan stop at a node (note that the exit time is the time when
      // the vehicle is completely out of the node
      estimatedCost = earliestExitTime
          + ((long) (lengthShortestPath * 1000 / setting.getVehicleSpeed()))
          - ((long) (setting.getVehicleLength() * 1000
              / setting.getVehicleSpeed()));
    } else {
      // if the plan stop at an edge, the exit time is the time when the vehicle
      // is exactly at the central of the next node
      estimatedCost = earliestExitTime
          + ((long) (lengthShortestPath * 1000 / setting.getVehicleSpeed()));
    }
    
    return estimatedCost;
  }
  
  public List<Point> getShortestPath(Point origin, Point destination) {
    return Graphs.shortestPathEuclideanDistance(roadModel.getGraph(), origin,
        destination);
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
    LinkedList<Range<Long>> intervals = new LinkedList<>(plan.getIntervals());
    
    if (intervals.size() % 2 == 1) {
      // if the plan start from a node
      if (intervals.getFirst().upperEndpoint() > currentTime) {
        // if the interval is not out-dated
        final NodeAgent firstNodeAgent = nodeAgentList
            .getNodeAgent(path.get(0));
        firstNodeAgent.addReservation(agvID, lifeTime, intervals.get(0));
      }
      intervals.removeFirst();
    }
    
    for (int i = 0; i < path.size() - 1; i++) {
      if (intervals.get(1).upperEndpoint() < currentTime) {
        continue;
      }
      
      final EdgeAgent edgeAgent = edgeAgentList.getEdgeAgent(path.get(i),
          path.get(i + 1));
      edgeAgent.addReservation(path.get(i), intervals.getFirst(), lifeTime,
          agvID);
      intervals.removeFirst();
      
      final NodeAgent nodeAgent = nodeAgentList.getNodeAgent(path.get(i + 1));
      nodeAgent.addReservation(agvID, lifeTime, intervals.getFirst());
      intervals.removeFirst();
    }
  }
  
  /**
   * Find the lengths of the shortest paths from all nodes to dest
   *
   * @param dest the dest
   * @return the map
   */
  public Map<Point, Double> shortestPathLengthsTo(Point dest) {
    final Map<Point, Double> initialDist = new HashMap<>();
    final Map<Point, Double> resultDist = new HashMap<>();

    // node set
    final Set<Point> nodeSet = roadModel.getGraph().getNodes();
    for (Point node : nodeSet) {
      if (node.equals(dest)) {
        initialDist.put(node, 0d);
      } else {
        initialDist.put(node, Double.MAX_VALUE);
      }
    }

    while (!initialDist.isEmpty()) {
      final Point node = getKeyOfMinValue(initialDist);
      final double dist = initialDist.remove(node);
      resultDist.put(node, dist);

      final List<Point> neighboringNodes = new ArrayList<>();
      neighboringNodes
          .addAll(roadModel.getGraph().getIncomingConnections(node));

      for (Point neighbor : neighboringNodes) {
        final double alt = dist
            + roadModel.getGraph().getConnection(neighbor, node).getLength();
        if (initialDist.containsKey(neighbor)
            && alt < initialDist.get(neighbor)) {
          initialDist.put(neighbor, alt);
        }
      }
    }

    return resultDist;
  }
  
  /**
   * Gets the key with minimum value.
   *
   * @param map the map
   * @return the key with minimum value
   */
  public Point getKeyOfMinValue(Map<Point, Double> map) {
    double minVal = Double.MAX_VALUE;
    Point key = null;
    
    for (Entry<Point, Double> entry : map.entrySet()) {
      if (minVal > entry.getValue()) {
        minVal = entry.getValue();
        key = entry.getKey();
      }
    }
    
    return key;
  }
  
  @Override
  public void tick(TimeLapse timeLapse) {
    
  }

  @Override
  public void afterTick(TimeLapse timeLapse) {
//    nodeAgentList.removeOutDatedReservation(timeLapse.getEndTime());
//    edgeAgentList.removeOutdatedReservations(timeLapse.getEndTime());
    if (!createdOrderList && numOfPlannedAGVs == setting.getNumOfAGVs()) {
      for (NodeAgent nodeAgent : nodeAgentList.getAllNodeAgents()) {
        nodeAgent.createOrderList();
      }
      for (EdgeAgent edgeAgent : edgeAgentList.getAllEdgeAgents()) {
        edgeAgent.createOrderList();
      }
      createdOrderList = true;
    }
  }
 
  /**
   * Checks if the agv is the first in the order list
   *
   * @param agvID the agv id
   * @param resource the resource
   * @return true, if is allowed to move
   */
  public boolean isAllowedToMove(int agvID, List<Point> resource) {
    if (resource.size() == 1) {
      // this is a node
      final NodeAgent nodeAgent = nodeAgentList.getNodeAgent(resource.get(0));
      if (nodeAgent.getNextAGV() == agvID) {
        nodeAgent.setNonSwappable();
        return true;
      } else {
        return false;
      }
    } else {
      // this is an edge
      final EdgeAgent edgeAgent = edgeAgentList.getEdgeAgent(resource.get(0), resource.get(1));
      if (edgeAgent.getNextAGV() == agvID) {
        edgeAgent.setNonSwappable();
        return true;
      } else {
        return false;
      }
    }
  }

  public void removeFirstAGV(List<Point> resource) {
    if (resource.size() == 1) {
      // if it is a node
      final NodeAgent nodeAgent = nodeAgentList.getNodeAgent(resource.get(0));
      nodeAgent.removeFirstAGV();
    } else {
      // if it is an edge
      final EdgeAgent edgeAgent = edgeAgentList.getEdgeAgent(resource.get(0), resource.get(1));
      edgeAgent.removeFirstAGV();
    }
  }
  
  public void setFirstOrderVisited(List<Point> resource) {
    if (resource.size() == 1) {
      // if it is a node
      final NodeAgent nodeAgent = nodeAgentList.getNodeAgent(resource.get(0));
      nodeAgent.setFirstOrderVisited();;
    } else {
      // if it is an edge
      final EdgeAgent edgeAgent = edgeAgentList.getEdgeAgent(resource.get(0), resource.get(1));
      edgeAgent.setFirstOrderVisited();
    }
  }
  
  public void notifyPlanned() {
    this.numOfPlannedAGVs++;
  }
  
  public boolean finishPlanningPhase() {
    return createdOrderList;
  }

  public void swapOrder(LinkedList<CheckPoint> checkPoints, int agvID) {
    // the first checkpoint is the resource where the agv is staying at. Thus, we start from the second checkpoint, which is the next resource
    int k = 1;
    final SingleStep firstPlanStep = convertCheckPointToSingleStep(checkPoints.get(k), agvID);
    Set<Integer> delayedAGVs = new HashSet<>();
    delayedAGVs.addAll(getPrecedingAGVs(firstPlanStep, checkPoints.get(k).getResource()));
    final Map<List<Point>, SwapRequest> delayedAgentMap = new HashMap<>();
    boolean deadlock = false;
    while (!deadlock && !delayedAGVs.isEmpty()) {
      final List<Point> resource = checkPoints.get(k).getResource();
      final SingleStep currentPlanStep = convertCheckPointToSingleStep(checkPoints.get(k), agvID);
      final Set<SingleStep> delayedSteps = getDelayedSteps(currentPlanStep, resource, delayedAGVs);
      if (delayedSteps.isEmpty()) {
        break;
      }
      for (SingleStep singleStep : delayedSteps) {
        if (!singleStep.isSwappable() || singleStep.isVisited()) {
          return;
        }
      }
      final Set<Integer> newDelayedAGVs = getDelayedAGVsFromDelayedSteps(delayedSteps);
      delayedAGVs = newDelayedAGVs;
      for (int delayedAGVID : delayedAGVs) {
        final VehicleAgent delayedAGV = agvList.get(delayedAGVID);
        if (resource.size() == 1) {
          // node
          if (roadModel.isOccupiedBy(resource.get(0), delayedAGV)) {
            return;
          }
        } else {
          // edge
          final Set<RoadUser> allAGVsOnEdge = roadModel.getRoadUsersOn(resource.get(0), resource.get(1));
          for (RoadUser roadUser : allAGVsOnEdge) {
            final VehicleAgent agvOnEdge = (VehicleAgent) roadUser;
            if (agvOnEdge.getID() == delayedAGVID) {
              return;
            }
          }
        }
      }
      
      delayedAgentMap.put(resource, new SwapRequest(currentPlanStep, delayedSteps));
      k++;
    }
    
    // create the backup of current order before changing
    createBackUp();
    
    for (List<Point> resource : delayedAgentMap.keySet()) {
      final SwapRequest swapRequest = delayedAgentMap.get(resource);
      if (resource.size() == 1) {
        // node
        final NodeAgent nodeAgent = nodeAgentList.getNodeAgent(resource.get(0));
        nodeAgent.swapOrder(swapRequest);
      } else {
        // edge
        final EdgeAgent edgeAgent = edgeAgentList.getEdgeAgent(resource.get(0), resource.get(1));
        edgeAgent.swapOrder(swapRequest);
      }
    }
    
    // create plan step priority graph and check for deadlock
    final PlanStepPriorityGraph planStepPriorityGraph = new PlanStepPriorityGraph(nodeAgentList, edgeAgentList, agvList);
    planStepPriorityGraph.createGraph();
    if (!planStepPriorityGraph.isAcyclic()) {
      rollback();
//      System.out.println("Just rolled back");
//    } else {
//      System.out.println("Swap successfully " + agvID);
    }
  }
  
  public Set<Integer> getDelayedAGVsFromDelayedSteps(Set<SingleStep> delayedSteps) {
    final Set<Integer> delayedAGVs = new HashSet<>();
    
    for (SingleStep singleStep : delayedSteps) {
      delayedAGVs.add(singleStep.getAgvID());
    }
    
    return delayedAGVs;
  }
  
  public Set<SingleStep> getDelayedSteps(SingleStep currentStep,
      List<Point> resource, Set<Integer> currentDelayedAGVs) {
    final Set<SingleStep> delayedAGVs = new HashSet<>();
    if (resource.size() == 1) {
      // node
      final NodeAgent nodeAgent = nodeAgentList.getNodeAgent(resource.get(0));
      delayedAGVs.addAll(nodeAgent.getDelayedSteps(currentStep, currentDelayedAGVs));
    } else {
      // edge
      final EdgeAgent edgeAgent = edgeAgentList.getEdgeAgent(resource.get(0), resource.get(1));
      delayedAGVs.addAll(edgeAgent.getDelayedSteps(currentStep, currentDelayedAGVs));
    }
    return delayedAGVs;
  }

  public Set<Integer> getPrecedingAGVs(SingleStep currentStep, List<Point> resource) {
    final Set<Integer> delayedAGVs = new HashSet<>();
    if (resource.size() == 1) {
      // node
      final NodeAgent nodeAgent = nodeAgentList.getNodeAgent(resource.get(0));
      delayedAGVs.addAll(nodeAgent.getPrecedingAGVs(currentStep));
    } else {
      // edge
      final EdgeAgent edgeAgent = edgeAgentList.getEdgeAgent(resource.get(0), resource.get(1));
      delayedAGVs.addAll(edgeAgent.getPrecedingAGVs(currentStep));
    }
    return delayedAGVs;
  }

  public SingleStep convertCheckPointToSingleStep(CheckPoint checkPoint, int agvID) {
    final SingleStep singleStep = new SingleStep(agvID, checkPoint.getID());
    return singleStep;
  }

  public void createBackUp() {
    for (NodeAgent nodeAgent : nodeAgentList.getAllNodeAgents()) {
      nodeAgent.createBackUp();
    }
    
    for (EdgeAgent edgeAgent : edgeAgentList.getAllEdgeAgents()) {
      edgeAgent.createBackUp();
    }
  }
  
  public void rollback() {
    for (NodeAgent nodeAgent : nodeAgentList.getAllNodeAgents()) {
      nodeAgent.rollback();
    }
    
    for (EdgeAgent edgeAgent : edgeAgentList.getAllEdgeAgents()) {
      edgeAgent.rollback();
    }
  }
}
