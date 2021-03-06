package multistage.garagemodel.delegatemas;

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

import multistage.Destinations;
import multistage.State;
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
  
  private LinkedList<Point> destinations;
  
  private Setting setting;
  
  private Result result;
  
  private List<Point> garageList;
  
  private Point garage;
  
  private int reachedDestinations = 0;
  
  public VehicleAgent(Destinations destinationList, VirtualEnvironment virtualEnvironment,
      int agvID, List<Point> garageList, Setting setting, Result result) {
    roadModel = Optional.absent();
    path = new LinkedList<>();
    this.destinationList = destinationList;
    this.virtualEnvironment = virtualEnvironment;
    this.agvID = agvID;
    this.garageList = garageList;
    this.garage = garageList.get(agvID);
    this.initialPos = garage;
    state = State.IDLE;
    this.setting = setting;
    this.destinations = new LinkedList<>();
    this.result = result;
  }

  @Override
  public void initRoadUser(RoadModel model) {
    roadModel = Optional.of((CollisionGraphRoadModel) model);
    roadModel.get().addObjectAt(this, initialPos);
    path = new LinkedList<>();
  }

  @Override
  public double getSpeed() {
    return setting.getVehicleSpeed();
  }

  void nextDestination(long startTime) {
    
    destinations = new LinkedList<>();
    for (int i = 0; i < setting.getNumOfDestsForEachAGV(); i++) {
      destinations.addLast(destinationList.getDestination());
    }
    destinations.addLast(garage);
    
    Plan plan = virtualEnvironment.exploreRoute(agvID, startTime,
        roadModel.get().getPosition(this), destinations,
        setting.getNumOfAlterRoutes(), garageList);
    
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
    
    final long currentTime = timeLapse.getStartTime();
    
    if (state == State.ACTIVE
        && roadModel.get().getPosition(this).equals(garage)
        && path.size() == 1) {
      // if the agv reaches the entrance of the station, then it becomes idle
      // and will try to move to the exit of the station
      state = State.IDLE;
    } else if (state == State.IDLE) {
      // of the agv reaches the exit of the station, then it becomes active
      state = State.ACTIVE;
      nextDestination(timeLapse.getEndTime());
    }
    
    if (currentPlan != null && currentPlan.getIntervals().size() > 1
        && currentPlan.getIntervals().get(1).upperEndpoint() < currentTime) {
      currentPlan.removeOldSteps();
    }
    
    if (state == State.ACTIVE) {
      // if not idle
      // if explore
      if (currentTime == nextExplorationTime) {
        // get the next check point
        final CheckPoint nextCheckPoint = checkPoints.getFirst();
        // get the next point of the path, we will explore from this point
        final Point startPoint = path.peek();
        if (startPoint.equals(nextCheckPoint.getPoint())) {
          // if the next check point is a node, then just explore
          final long startTime = nextCheckPoint.getExpectedTime();
          boolean changePlan = explore(startTime, startPoint, setting.getNumOfAlterRoutes());
          if (changePlan) {
            virtualEnvironment.makeReservation(agvID, currentPlan, currentTime, currentTime + setting.getEvaporationDuration());
            nextRefreshTime = currentTime + setting.getRefreshDuration();
          }
        } else {
          if (!startPoint.equals(checkPoints.get(1).getPoint())) {
            // this one cannot happen
            System.out.println(roadModel.get().getPosition(this));
            throw new Error("Some problems here!");
          }
          // if the check point is on an edge, then start explore from the end
          // node of that edge
          final Point lastNode = currentPlan.getPath().get(0);
          final Range<Long> nodeInterval = currentPlan.getIntervals().get(0);
          final Range<Long> edgeInterval = currentPlan.getIntervals().get(1);
          final long startTime = nextCheckPoint.getExpectedTime()
              + ((long) ((setting.getVehicleLength() + 0.1) * 1000
                  / setting.getVehicleSpeed()));
          boolean changePlan = explore(startTime, checkPoints.get(1).getPoint(),
              setting.getNumOfAlterRoutes());
          if (changePlan) {
            // if the AGV change the plan, add the check point on the edge to
            // the checkpoint list
            checkPoints.addFirst(nextCheckPoint);
            currentPlan.addLastNode(lastNode, nodeInterval, edgeInterval);
            virtualEnvironment.makeReservation(agvID, currentPlan, currentTime, currentTime + setting.getEvaporationDuration());
            nextRefreshTime = currentTime + setting.getRefreshDuration();
          }
        }
      }

      if (currentTime == nextRefreshTime) {
        refresh(currentTime);
      }

      // IDEA: check the position. If right before it leaves an edge or a node,
      // it still have to wait, then consume time
      Point currentPos = roadModel.get().getPosition(this);
      Point roundedPos = new Point(round(currentPos.x), round(currentPos.y));
      if (!checkPoints.isEmpty()
          && roundedPos.equals(checkPoints.getFirst().getPoint())) {
        // System.out.println(roadModel.get().getPosition(this));
        // sim.stop();
        if (currentTime < checkPoints.getFirst().getExpectedTime()) {
          final long timeDifference = checkPoints.getFirst().getExpectedTime()
              - currentTime;
          if (timeDifference < timeLapse.getTimeLeft()) {
            timeLapse.consume(timeDifference);
            checkPoints.removeFirst();
          } else if (timeDifference == timeLapse.getTimeLeft()) {
            timeLapse.consume(timeDifference);
            checkPoints.removeFirst();
            if (currentPos.x % 1 == 0 && currentPos.y % 1 == 0) {
              path.remove();
            }
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
    }
    
    if (destinations.size() > 1 && roadModel.get().getPosition(this).equals(destinations.getFirst())) {
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
    Plan plan = virtualEnvironment.exploreRoute(agvID, startTime, startNode, destinations, numberOfRoutes, garageList);
    
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