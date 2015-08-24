package vehicleAgent;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.road.CollisionGraphRoadModel;
import com.github.rinde.rinsim.core.model.road.MovingRoadUser;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;

import destinationGenerator.Destinations;
import dmasForRouting.AGVSystem;
import routePlan.CheckPoint;
import routePlan.ExecutablePlan;
import routePlan.Plan;
import virtualEnvironment.VirtualEnvironment;

public class VehicleAgent implements TickListener, MovingRoadUser {
  
  /** The road model. */
  private Optional<CollisionGraphRoadModel> roadModel;
  
  /** The next destination. */
  private Optional<Point> destination;
  
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
  
  private int reachedDestinations = 0;

  public VehicleAgent(Destinations destinations, VirtualEnvironment virtualEnvironment,
      int agvID, Simulator sim, Point initialPos, List<Point> centralStation) {
    roadModel = Optional.absent();
    destination = Optional.absent();
    path = new LinkedList<>();
    this.destinationList = destinations;
    this.virtualEnvironment = virtualEnvironment;
    this.agvID = agvID;
    this.initialPos = initialPos;
    stationEntrance = centralStation.get(centralStation.size() - 1);
    stationExit = centralStation.get(0);
    state = State.IDLE;
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
    return AGVSystem.VEHICLE_SPEED;
  }

  void nextDestination(long startTime) {
    destination = Optional.of(destinationList.getDestination());
    
    Plan plan = virtualEnvironment.exploreRoute(agvID, startTime,
        roadModel.get().getPosition(this), destination.get(),
        AGVSystem.NUM_OF_ROUTES, state);
    executablePlan = new ExecutablePlan(plan);
    currentPlan = plan;
    path = new LinkedList<>(executablePlan.getPath());
    checkPoints = new LinkedList<>(executablePlan.getCheckPoints());
    virtualEnvironment.makeReservation(agvID, plan, startTime, startTime + AGVSystem.EVAPORATION_DURATION);
    expectedArrivalTime = plan.getArrivalTime();
    nextExplorationTime = startTime + AGVSystem.EXPLORATION_DURATION;
    nextRefreshTime = startTime + AGVSystem.REFRESH_DURATION;
  }

  @Override
  public void tick(TimeLapse timeLapse) {
    
    if (state == State.ACTIVE
        && roadModel.get().getPosition(this).equals(destination.get())) {
      state = State.GOING_HOME;
      System.out.println(agvID + ": Reached destination: " + ++reachedDestinations);
    } else if (state == State.GOING_HOME
        && roadModel.get().getPosition(this).equals(stationEntrance)) {
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
    
    if (state != State.IDLE) {
      // if explore
      if (timeLapse.getStartTime() == nextExplorationTime) {
        // get the next check point
        final CheckPoint nextCheckPoint = checkPoints.getFirst();
        // get the next point of the path, we will explore from this point
        final Point startPoint = path.peek();
        if (startPoint.equals(nextCheckPoint.getPoint())) {
          // if the next check point is a node, then just explore
          explore(nextCheckPoint.getExpectedTime(), startPoint,
              AGVSystem.NUM_OF_ROUTES);
        } else {
          if (!startPoint.equals(checkPoints.get(1).getPoint())) {
            // this one cannot happen
            throw new Error("Some problems here!");
          }
          // if the check point is on an edge, then start explore from the end
          // node of that edge
          boolean changePlan = explore(checkPoints.get(1).getExpectedTime(),
              checkPoints.get(1).getPoint(), AGVSystem.NUM_OF_ROUTES);
          if (changePlan) {
            // if the AGV change the plan, add the check point on the edge to
            // the checkpoint list
            checkPoints.addFirst(nextCheckPoint);
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

//    if (roadModel.get().getPosition(this).equals(destination.get())) {
////      System.out.println(agvID + ": Reached destination: " + ++reachedDestinations);
//      nextDestination(timeLapse.getEndTime());
//    }
    
    
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
    nextExplorationTime = startTime + AGVSystem.EXPLORATION_DURATION;
    Plan plan = virtualEnvironment.exploreRoute(agvID, startTime, startNode, destination.get(), numberOfRoutes, state);
    if (expectedArrivalTime - plan.getArrivalTime() > AGVSystem.SWITCHING_THRESHOLD) {
      executablePlan = new ExecutablePlan(plan);
      currentPlan = plan;
      path = new LinkedList<>(executablePlan.getPath());
      checkPoints = new LinkedList<>(executablePlan.getCheckPoints());
      virtualEnvironment.makeReservation(agvID, plan, startTime, startTime + AGVSystem.EVAPORATION_DURATION);
      expectedArrivalTime = plan.getArrivalTime();
      nextRefreshTime = startTime + AGVSystem.REFRESH_DURATION;
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
        currentTime + AGVSystem.EVAPORATION_DURATION);
    nextRefreshTime = currentTime + AGVSystem.REFRESH_DURATION;
  }

  @Override
  public void afterTick(TimeLapse timeLapse) {}

}

