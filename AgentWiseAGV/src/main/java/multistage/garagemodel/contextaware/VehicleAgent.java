package multistage.garagemodel.contextaware;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import com.github.rinde.rinsim.core.model.road.CollisionGraphRoadModel;
import com.github.rinde.rinsim.core.model.road.MovingRoadUser;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;

import multistage.Destinations;
import multistage.State;
import multistage.result.Result;
import routeplan.CheckPoint;
import routeplan.ExecutablePlan;
import routeplan.Plan;
import routeplan.ResourceType;
import setting.Setting;

public class VehicleAgent implements TickListener, MovingRoadUser {
  
  /** The road model. */
  private Optional<CollisionGraphRoadModel> roadModel;
  
  /** The planned path. */
  private Queue<Point> path;
  
  /** The destination list. */
  private Destinations destinationList;
  
  /** The virtual environment. */
  private VirtualEnvironment virtualEnvironment;
  
  /** The agv id. */
  private int agvID;
  
  /** The executable plan. */
  private ExecutablePlan executablePlan;
  
  /** The check points. */
  private LinkedList<CheckPoint> checkPoints;
  
  /** The initial pos. */
  private Point initialPos;
  
  /** The state. */
  private State state;
  
  /** The setting. */
  private Setting setting;
  
  private LinkedList<Point> destinations;
  
  private Result result;
  
  private int reachedDestinations = 0;
  
  private List<Point> garageList;
  
  private Point garage;

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
        roadModel.get().getPosition(this), destinations, garageList);
    executablePlan = new ExecutablePlan(plan, setting);
    path = new LinkedList<>(executablePlan.getPath());
    checkPoints = new LinkedList<>(executablePlan.getCheckPoints());
    virtualEnvironment.makeReservation(agvID, plan, startTime, plan.getArrivalTime());
//    System.out.println(plan.getArrivalTime());
  }

  @Override
  public void tick(TimeLapse timeLapse) {
    
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
    
    final Point currentPos = roadModel.get().getPosition(this);
    final Point roundedPos = new Point(round(currentPos.x), round(currentPos.y));
    
    if (agvID == 5 && timeLapse.getStartTime() < 100000
        && timeLapse.getStartTime() > 5000
        && checkPoints.getFirst().getPoint().equals(roundedPos)) {
      return;
    }
    
    if (agvID == 7 && timeLapse.getStartTime() < 110000
        && timeLapse.getStartTime() > 20000
        && checkPoints.getFirst().getPoint().equals(roundedPos)) {
      return;
    }
    
    if (state == State.ACTIVE) {
      // IDEA: check the position. If right before it leaves an edge or a node,
      // it still have to wait, then consume time
      if (!checkPoints.isEmpty()
          && roundedPos.equals(checkPoints.getFirst().getPoint())) {
        // if the AGV is at exactly the check point

        // if the check point is a node then announce to both the node and the
        // next edge that it has visited
        if (checkPoints.getFirst().getResourceType() == ResourceType.NODE) {
          virtualEnvironment.setVisited(agvID, checkPoints.getFirst());
          virtualEnvironment.setVisited(agvID, checkPoints.get(1));
        }

     // check if it is time to move according to the plan
        if (timeLapse.getStartTime() < checkPoints.getFirst().getExpectedTime()) {
          // if it hasn't been the start time yet
          // the amount of time left to the start time
          final long timeDifference = checkPoints.getFirst().getExpectedTime()
              - timeLapse.getStartTime();
          // if the amount of time left to the start time is smaller than or equal to the time left of this tick
          if (timeDifference <= timeLapse.getTimeLeft()) {
            // consume the amount of time left to start time
            timeLapse.consume(timeDifference);
            if (isSafeToMove(false)) {
              // if there is no higher priority delayed AGVs
              checkPoints.removeFirst();
            } else {
              timeLapse.consumeAll();
            }
          } else {
            // time difference is larger than time left
            timeLapse.consumeAll();
          }
        } else {
          // if the start time passed
          if (isSafeToMove(false)) {
            // if it is safe to move, mean that there is no delayed agv left
            if (checkPoints.getFirst().getResourceType() == ResourceType.EDGE) {
              // if the check point is an edge then move when next node is free
              if (!roadModel.get().isOccupied(checkPoints.getFirst().getResource().get(1))) {
                checkPoints.removeFirst();
              } else {
                timeLapse.consumeAll();
              }
            } else {
              // if the check point is a node then move when the capacity of the next edge is not full
              final CheckPoint checkPoint = checkPoints.get(1);
              if (getNumOfAGVsOnEdge(checkPoint.getResource().get(0), checkPoint.getResource().get(1)) < 2) {
                checkPoints.removeFirst();
              } else {
                timeLapse.consumeAll();
              }
            }
            
          } else {
            // if it is not safe, then wait
            timeLapse.consumeAll();
          }
        }
      }

      if (timeLapse.hasTimeLeft()) {
        roadModel.get().followPath(this, path, timeLapse);
      }
    }

    // count the number of reached destinations
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
   * Checks if is safe to move to the next resource.
   *
   * @return true, if there is no delayed agv that hasn't entered the resource
   */
  public boolean isSafeToMove(boolean isFirstCheckPoint) {
    return virtualEnvironment.getListOfDelayedAGVs(agvID,
        checkPoints.getFirst().getExpectedTime(), checkPoints.get(1)).isEmpty();
  }
  
  /**
   * Gets the number of agvs on an edge, exclusive
   *
   * @param from the from
   * @param to the to
   * @return the num of ag vs on edge
   */
  public int getNumOfAGVsOnEdge(Point from, Point to) {
    final Set<RoadUser> agvsOnEdge = roadModel.get().getRoadUsersOn(from, to);
    
    int numAGVsOnEdge = agvsOnEdge.size();
    
    if (roadModel.get().getRoadUsersOnNode(from).size() != 0) {
      numAGVsOnEdge--;
    }
    
    if (roadModel.get().getRoadUsersOnNode(to).size() != 0) {
      numAGVsOnEdge--;
    }
    
    return numAGVsOnEdge;
  }
  
  @Override
  public void afterTick(TimeLapse timeLapse) {}

}
