package multistage.delegatemas;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import com.github.rinde.rinsim.core.model.road.CollisionGraphRoadModel;
import com.github.rinde.rinsim.core.model.road.MovingRoadUser;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;
import com.google.common.collect.Range;

import multistage.State;
import multistage.destinationgenerator.Destinations;
import multistage.result.Result;
import routeplan.CheckPoint;
import routeplan.ExecutablePlan;
import routeplan.Plan;
import setting.Setting;


public class VehicleAgent implements TickListener, MovingRoadUser {
  
  /** The road model. */
  private Optional<CollisionGraphRoadModel> roadModel;
  
//  /** The next destination. */
//  private Optional<Point> destination;
  
  /** The planned path. */
  private Queue<Point> path;
  
  /** The destination list. */
  private Destinations destinationList;
  
  /** The virtual environment. */
  private VirtualEnvironment virtualEnvironment;
  
  /** The agv id. */
  private int agvID;
  
  /** The current plan. */
  private Plan currentPlan;
  
  /** The executable plan. */
  private ExecutablePlan executablePlan;
  
  /** The check points. */
  private LinkedList<CheckPoint> checkPoints;
  
  /** The expected arrival time. */
  private long expectedArrivalTime;
  
  /** The next refresh time. */
  private long nextRefreshTime;
  
  /** The next exploration time. */
  private long nextExplorationTime;
  
  /** The initial pos. */
  private Point initialPos;
  
  /** The state. */
  private State state;
  
  /** The station entrance. */
  private Point stationEntrance;
  
  /** The station exit. */
  private Point stationExit;
  
  private LinkedList<Point> destinations;
  
  private Setting setting;
  
  private Result result;
  
  private List<Point> centralStation;
  
  private int reachedDestinations = 0;

  public VehicleAgent(Destinations destinationList, VirtualEnvironment virtualEnvironment,
      int agvID, List<Point> centralStation, Setting setting, Result result) {
    roadModel = Optional.absent();
    path = new LinkedList<>();
    this.destinationList = destinationList;
    this.virtualEnvironment = virtualEnvironment;
    this.agvID = agvID;
    this.initialPos = centralStation.get(centralStation.size() - 1 - agvID);
    this.stationExit = centralStation.get(centralStation.size() - 1);
    this.stationEntrance = centralStation.get(0);
    this.centralStation = new ArrayList<>(centralStation);
    this.centralStation.remove(stationEntrance);
    state = State.IDLE;
    this.setting = setting;
    this.destinations = new LinkedList<>();
    this.result = result;
  }

  @Override
  public void initRoadUser(RoadModel model) {
    roadModel = Optional.of((CollisionGraphRoadModel) model);
    roadModel.get().addObjectAt(this, initialPos);
    path = new LinkedList<>(
        roadModel.get().getShortestPathTo(this, stationExit));
  }

  @Override
  public double getSpeed() {
    return setting.getVehicleSpeed();
  }

  void nextDestination(long startTime) {
    
    destinations.addLast(destinationList.getDestination());
    destinations.addLast(destinationList.getDestination());
    destinations.addLast(destinationList.getDestination());
    destinations.addLast(stationEntrance);
    
    Plan plan = virtualEnvironment.exploreRoute(agvID, startTime,
        roadModel.get().getPosition(this), destinations,
        setting.getNumOfAlterRoutes(), centralStation);
    
    executablePlan = new ExecutablePlan(plan, setting);
    currentPlan = plan;
    path = new LinkedList<>(executablePlan.getPath());
    checkPoints = new LinkedList<>(executablePlan.getCheckPoints());
    virtualEnvironment.makeReservation(agvID, plan, startTime, startTime + setting.getEvaporationDuration());
    expectedArrivalTime = plan.getArrivalTime();
    nextExplorationTime = startTime + setting.getExplorationDuration();
    nextRefreshTime = startTime + setting.getRefreshDuration();
  }

  @Override
  public void tick(TimeLapse timeLapse) {
    
    if (state == State.ACTIVE
        && roadModel.get().getPosition(this).equals(stationEntrance)
        && path.size() == 1) {
      // if the agv reaches the entrance of the station, then it becomes idle
      // and will try to move to the exit of the station
      state = State.IDLE;
      path = new LinkedList<>(
          roadModel.get().getShortestPathTo(this, stationExit));
    } else if (state == State.IDLE
        && roadModel.get().getPosition(this).equals(stationExit)) {
      // of the agv reaches the exit of the station, then it becomes active
      state = State.ACTIVE;
      nextDestination(timeLapse.getEndTime());
    }
    
    if (currentPlan != null && currentPlan.getIntervals().size() > 1
        && currentPlan.getIntervals().get(1).upperEndpoint() < timeLapse
            .getStartTime()) {
      currentPlan.removeOldSteps();
    }
    
    if (state == State.ACTIVE) {
      // if not idle
      // if explore
      if (timeLapse.getStartTime() == nextExplorationTime) {
        // get the next check point
        final CheckPoint nextCheckPoint = checkPoints.getFirst();
        // get the next point of the path, we will explore from this point
        final Point startPoint = path.peek();
        if (startPoint.equals(nextCheckPoint.getPoint())) {
          // if the next check point is a node, then just explore
          final long startTime = nextCheckPoint.getExpectedTime();
          boolean changePlan = explore(startTime, startPoint, setting.getNumOfAlterRoutes());
          if (changePlan) {
            virtualEnvironment.makeReservation(agvID, currentPlan, startTime, startTime + setting.getEvaporationDuration());
            nextRefreshTime = startTime + setting.getRefreshDuration();
          }
        } else {
          if (!startPoint.equals(checkPoints.get(1).getPoint())) {
            // this one cannot happen
            throw new Error("Some problems here!");
          }
          // if the check point is on an edge, then start explore from the end
          // node of that edge
          final Point lastNode = currentPlan.getPath().get(0);
          final Range<Long> nodeInterval = currentPlan.getIntervals().get(0);
          final Range<Long> edgeInterval = currentPlan.getIntervals().get(1);
          final long startTime = checkPoints.get(1).getExpectedTime();
          boolean changePlan = explore(startTime, checkPoints.get(1).getPoint(),
              setting.getNumOfAlterRoutes());
          if (changePlan) {
            // if the AGV change the plan, add the check point on the edge to
            // the checkpoint list
            checkPoints.addFirst(nextCheckPoint);
            currentPlan.addLastNode(lastNode, nodeInterval, edgeInterval);
            virtualEnvironment.makeReservation(agvID, currentPlan, startTime, startTime + setting.getEvaporationDuration());
            nextRefreshTime = startTime + setting.getRefreshDuration();
          }
        }
      }

      if (timeLapse.getStartTime() == nextRefreshTime) {
        refresh(timeLapse.getStartTime());
      }

      // IDEA: check the position. If right before it leaves an edge or a node,
      // it still have to wait, then consume time
      Point currentPos = roadModel.get().getPosition(this);
      Point roundedPos = new Point(round(currentPos.x), round(currentPos.y));
      if (!checkPoints.isEmpty()
          && roundedPos.equals(checkPoints.getFirst().getPoint())) {
        // System.out.println(roadModel.get().getPosition(this));
        // sim.stop();
        if (timeLapse.getStartTime() < checkPoints.getFirst()
            .getExpectedTime()) {
          final long timeDifference = checkPoints.getFirst().getExpectedTime()
              - timeLapse.getStartTime();
          if (timeDifference <= timeLapse.getTimeLeft()) {
            timeLapse.consume(timeDifference);
            checkPoints.removeFirst();
          } else {
            // time difference is larger than time left
            timeLapse.consumeAll();
          }
        } else {
          checkPoints.removeFirst();
        }
      }

      if (timeLapse.hasTimeLeft()) {
        roadModel.get().followPath(this, path, timeLapse);
      }
    } else {
      roadModel.get().followPath(this, path, timeLapse);
    }
    
    if (!destinations.isEmpty()
        && roadModel.get().getPosition(this).equals(destinations.getFirst())) {
      reachedDestinations++;
      destinations.removeFirst();
    }
    
    if (timeLapse.getEndTime() == setting.getEndTime()) {
      result.updateResult(reachedDestinations);
    }
  }
  
  public double round(double input) {
    return (Math.round(input * 10) / 10d);
  }
  
  /**
   * Explore new routes.
   *
   * @param startTime the start time
   * @param startNode the start node
   * @param numberOfRoutes the number of routes
   * @return true, if change the current plan
   */
  public boolean explore(long startTime, Point startNode, int numberOfRoutes) {
    nextExplorationTime = startTime + setting.getExplorationDuration();
    Plan plan = virtualEnvironment.exploreRoute(agvID, startTime, startNode, destinations, numberOfRoutes, centralStation);
    
    if (plan == null) {
      return false;
    }
    
    if (expectedArrivalTime - plan.getArrivalTime() > setting.getSwitchingThreshold()) {
      executablePlan = new ExecutablePlan(plan, setting);
      currentPlan = plan;
      path = new LinkedList<>(executablePlan.getPath());
      checkPoints = new LinkedList<>(executablePlan.getCheckPoints());
      expectedArrivalTime = plan.getArrivalTime();
//      virtualEnvironment.makeReservation(agvID, plan, startTime, startTime + setting.getEvaporationDuration());
//      nextRefreshTime = startTime + setting.getRefreshDuration();
      return true;
    } else {
      return false;
    }
  }
  
  /**
   * Refresh the reservations.
   *
   * @param currentTime the current time
   */
  public void refresh(long currentTime) {
    virtualEnvironment.makeReservation(agvID, currentPlan, currentTime,
        currentTime + setting.getEvaporationDuration());
    nextRefreshTime = currentTime + setting.getRefreshDuration();
  }

  @Override
  public void afterTick(TimeLapse timeLapse) {}

}