package virtualEnvironment;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedMap;
import java.util.Stack;
import java.util.TreeMap;

import org.apache.commons.lang3.time.FastDatePrinter;
import org.apache.commons.math3.random.RandomGenerator;
import org.eclipse.swt.internal.theme.Theme;

import com.github.rinde.rinsim.core.model.road.CollisionGraphRoadModel;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Graphs;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.collect.Range;

import ch.qos.logback.core.pattern.parser.Node;
import dmasForRouting.AGVSystem;
import pathSampling.Path;
import pathSampling.PathSampling;
import resourceAgents.EdgeAgent;
import resourceAgents.EdgeAgentList;
import resourceAgents.FreeTimeWindow;
import resourceAgents.NodeAgent;
import resourceAgents.NodeAgentList;
import routePlan.Plan;
import routePlan.PlanFTW;
import vehicleAgent.State;

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
  
  /** The list of points in the central station. */
  private List<Point> centralStation;
  
  /** The road model. */
  private CollisionGraphRoadModel roadModel;
  
  /**
   * Instantiates a new virtual environment.
   *
   * @param roadModel the road model
   * @param randomGenerator the random generator
   * @param centralStation the central station
   */
  public VirtualEnvironment(CollisionGraphRoadModel roadModel,
      RandomGenerator randomGenerator, List<Point> centralStation) {
    nodeAgentList = new NodeAgentList(roadModel);
    edgeAgentList = new EdgeAgentList(roadModel);
    this.centralStation = centralStation;
    this.roadModel = roadModel;
  }
  
  /**
   * Explore route.
   *
   * @param agvID the agv id
   * @param startTime the start time
   * @param origin the origin
   * @param destination the destination
   * @return the plan
   */
  public Plan exploreRoute(int agvID, long startTime, Point origin,
      Point destination) {

    // free time window of the start node
    final List<FreeTimeWindow> firstFreeTimeWindows = nodeAgentList
        .getNodeAgent(origin)
        .getFreeTimeWindows(Range.atLeast(startTime), agvID);

    FreeTimeWindow startFTW = null;

    // there should be only one free time window that contains the startTime
    final long realStartTime = startTime
        - ((long) (AGVSystem.VEHICLE_LENGTH * 1000 / AGVSystem.VEHICLE_SPEED));
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

    PlanFTW firstPlanFTW = new PlanFTW(firstFTW, firstPath);

    final SortedMap<Long, PlanFTW> planQueue = new TreeMap<>();
    planQueue.put(computeCost(firstPlanFTW, destination), firstPlanFTW);

    PlanFTW finalPlan = null;

    // TODO we still need a closed list here

    while (!planQueue.isEmpty()) {
      // select and remove the first plan in the queue
      final PlanFTW planFTW = planQueue.remove(planQueue.firstKey());

      final List<Point> path = planFTW.getPath();
      final List<FreeTimeWindow> ftwList = planFTW.getFreeTimeWindows();

      if (path.get(path.size() - 1).equals(destination) && ftwList.size() % 2 == 1) {
        // if reached the destination then break
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
          if (path.contains(nextNode)) {
            // we do not allow cyclic plan
            continue;
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
          for (FreeTimeWindow newFTW : nextFTWs) {
            LinkedList<FreeTimeWindow> newListOfFTWs = new LinkedList<>(
                ftwList);
            newListOfFTWs.addLast(newFTW);
            PlanFTW newPlanFTW = new PlanFTW(newListOfFTWs, newPath);
            // add next plan step to the queue
            // solve problem when have the same cost
            long estimatedCost = computeCost(newPlanFTW, destination);
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
          LinkedList<FreeTimeWindow> newListOfFTWs = new LinkedList<>(ftwList);
          newListOfFTWs.addLast(newFTW);
          getClass();
          PlanFTW newPlanFTW = new PlanFTW(newListOfFTWs, path);
          long estimatedCost = computeCost(newPlanFTW, destination);
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
                  + ((long) (AGVSystem.VEHICLE_LENGTH * 1000
                      / AGVSystem.VEHICLE_SPEED))));
    }

    Plan plan = new Plan(finalPlan.getPath(), intervals);

    return plan;
  }
  
  /**
   * Compute cost.
   *
   * @param planFTW the plan ftw
   * @param destination the destination
   * @return the cost
   */
  public long computeCost(PlanFTW planFTW, Point destination) {
    final List<Point> path = planFTW.getPath();
    final List<FreeTimeWindow> ftwList = planFTW.getFreeTimeWindows();
    
    long estimatedCost = -1;
    
    // the shortest path from the last node of the plan
    final List<Point> nextShortestPath = getShortestPath(
        path.get(path.size() - 1), destination);
    // calculate the length of the shortest path
    final double lengthShortestPath = Graphs.pathLength(nextShortestPath);
    final long earliestExitTime = planFTW.getEarliestExitTime();
    
    if (ftwList.size() % 2 == 1) {
      // if the plan stop at a node (note that the exit time is the time when
      // the vehicle is completely out of the node
      estimatedCost = earliestExitTime
          + ((long) (lengthShortestPath * 1000 / AGVSystem.VEHICLE_SPEED))
          - ((long) (AGVSystem.VEHICLE_LENGTH * 1000
              / AGVSystem.VEHICLE_SPEED));
    } else {
      // if the plan stop at an edge, the exit time is the time when the vehicle
      // is exactly at the central of the next node
      estimatedCost = earliestExitTime
          + ((long) (lengthShortestPath * 1000 / AGVSystem.VEHICLE_SPEED));
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
  
  @Override
  public void tick(TimeLapse timeLapse) {
    
  }

  @Override
  public void afterTick(TimeLapse timeLapse) {
    nodeAgentList.removeOutDatedReservation(timeLapse.getEndTime());
    edgeAgentList.removeOutdatedReservations(timeLapse.getEndTime());
  }
}
