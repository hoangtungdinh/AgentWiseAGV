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
import resourceagents.Reservation;
import routeplan.CheckPoint;
import routeplan.Plan;
import routeplan.RangeEndPoint;
import routeplan.contextaware.PlanFTW;
import routeplan.contextaware.PlanStep;
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
  
  /**
   * set a plan step (a reservation) as visited.
   *
   * @param agvID the agv id
   * @param nextCheckPoint the next check point
   */
  public void setVisited(int agvID, List<Point> resource, long endTime) {
    if (resource.size() == 1) {
      // if the resource is a node
      final NodeAgent nodeAgent = nodeAgentList.getNodeAgent(resource.get(0));
      nodeAgent.setVisited(agvID, endTime);
    } else {
      // if the resource is an edge
      final EdgeAgent edgeAgent = edgeAgentList.getEdgeAgent(resource.get(0), resource.get(1));
      edgeAgent.setVisited(agvID, endTime, resource.get(0));
    }
    
    if (resource.size() < 1 && resource.size() > 2) {
      throw new IllegalStateException("Invalid resource list");
    }
  }
  
  /**
   * Gets the list of delayed agvs in comparison with the plan of 'agvID' at the
   * startTime in the resource of the next check point
   *
   * @param agvID the agv id
   * @param startTime the start time according to the plan of the 'agvID'
   * @return the list of delayed agvs
   */
  public List<Integer> getListOfDelayedAGVs(int agvID, long startTime, CheckPoint nextCheckPoint) {
    final List<Point> resource = nextCheckPoint.getResource();
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
  
  public void modifyReservation(int agvID, List<Point> resource, Range<Long> interval, RangeEndPoint modifiedEndPoint) {
    if (resource.size() == 1) {
      // if the resource is a node
      final NodeAgent nodeAgent = nodeAgentList.getNodeAgent(resource.get(0));
      nodeAgent.modifyReservation(agvID, interval, modifiedEndPoint);
    } else {
      // if the resource is an edge
      final EdgeAgent edgeAgent = edgeAgentList.getEdgeAgent(resource.get(0), resource.get(1));
      edgeAgent.modifyReservation(agvID, resource.get(0), interval, modifiedEndPoint);
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
        if (reservedIntervals.size() > index + 1 && reservedIntervals.get(index + 1).contains(currentTime)) {
          // when currentTime is in two consecutive intervals, mean that the agv
          // is on two resources (edges, node or node, edges).
          
          // first, we consider the case that the agv is at exactly a node and
          // is going to move to edge, then the current time is of the interval
          // at a node and is also the start time of the next interval
          if ((index + reservedIntervals.size()) % 2 == 1 && reservedIntervals
              .get(index + 1).lowerEndpoint() == currentTime) {
            // the first condition says that the index is of a node. If the
            // reservedIntervals starts from a node, then size is odd and index
            // is even. If the reservedIntervals start from an edge, then size
            // is even and index is odd. The second condition is about the start
            // time of edge
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
  
  @Override
  public void tick(TimeLapse timeLapse) {
    
  }

  @Override
  public void afterTick(TimeLapse timeLapse) {
    nodeAgentList.removeOutDatedReservation(timeLapse.getEndTime());
    edgeAgentList.removeOutdatedReservations(timeLapse.getEndTime());
  }
}
