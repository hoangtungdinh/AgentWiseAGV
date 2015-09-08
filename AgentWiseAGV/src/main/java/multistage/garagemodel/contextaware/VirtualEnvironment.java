package multistage.garagemodel.contextaware;

import java.util.ArrayList;
import java.util.HashMap;
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
import resourceagents.PlanStep;
import routeplan.Plan;
import routeplan.contextaware.PlanFTW;
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
      RandomGenerator randomGenerator, Setting setting) {
    this.setting = setting;
    nodeAgentList = new NodeAgentList(roadModel, setting);
    edgeAgentList = new EdgeAgentList(roadModel, setting);
    this.roadModel = roadModel;
    this.shortestLengthToDest = new HashMap<>();
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
  public Plan exploreRoute(int agvID, long startTime, Point origin,
      List<Point> destinations, List<Point> garageList) {
    // TODO remove startTime, all start at 0 
    
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
        .getNodeAgent(origin)
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

    // if no possible free time window then it is an error
    if (startFTW == null) {
      throw new Error("No free time window for the first node!");
    }

    List<Point> firstPath = new ArrayList<>();
    firstPath.add(origin);
    LinkedList<FreeTimeWindow> firstFTW = new LinkedList<>();
    firstFTW.addLast(startFTW);

    PlanFTW firstPlanFTW = new PlanFTW(firstFTW, firstPath, 0, destinations);

    final SortedMap<Long, PlanFTW> planQueue = new TreeMap<>();
    planQueue.put(computeCost(firstPlanFTW, shortestLengthToDest, destinations),
        firstPlanFTW);

    PlanFTW finalPlan = null;

    final Set<PlanStep> closedSet = new LinkedHashSet<>();

    while (!planQueue.isEmpty()) {
//      if (planQueue.size() % 10000 == 0) {
//        System.out.println(planQueue.size());
//      }
      // select and remove the first plan in the queue
      final PlanFTW planFTW = planQueue.remove(planQueue.firstKey());
      
      final List<Point> path = planFTW.getPath();
      final List<FreeTimeWindow> ftwList = planFTW.getFreeTimeWindows();
      
//      System.out.println(planQueue.size());
      
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

    Plan plan = new Plan(finalPlan.getPath(), intervals);
    
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
    nodeAgentList.removeOutDatedReservation(timeLapse.getEndTime());
    edgeAgentList.removeOutdatedReservations(timeLapse.getEndTime());
  }
}
