package multistage.contextaware;

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
  
  /** The station entrance. */
  private Point stationEntrance;
  
  /** The station exit. */
  private Point stationExit;
  
  /** The setting. */
  private Setting setting;
  
  private LinkedList<Point> destinations;
  
  private Result result;
  
  private int reachedDestinations = 0;

  public VehicleAgent(Destinations destinationList, VirtualEnvironment virtualEnvironment,
      int agvID, List<Point> centralStation, Setting setting, Result result) {
    roadModel = Optional.absent();
    path = new LinkedList<>();
    this.destinationList = destinationList;
    this.virtualEnvironment = virtualEnvironment;
    this.agvID = agvID;
    this.initialPos = centralStation.get(agvID);
    stationExit = centralStation.get(centralStation.size() - 1);
    stationEntrance = centralStation.get(0);
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
    
    destinations = new LinkedList<>();
    destinations.addLast(destinationList.getDestination());
    destinations.addLast(destinationList.getDestination());
    destinations.addLast(stationEntrance);
    
    List<Point> stationExits = new ArrayList<>();
    stationExits.add(stationExit);
    Plan plan = virtualEnvironment.exploreRoute(agvID, startTime,
        roadModel.get().getPosition(this), destinations, stationExits);
    executablePlan = new ExecutablePlan(plan, setting);
    path = new LinkedList<>(executablePlan.getPath());
    checkPoints = new LinkedList<>(executablePlan.getCheckPoints());
    virtualEnvironment.makeReservation(agvID, plan, startTime, plan.getArrivalTime());
  }

  @Override
  public void tick(TimeLapse timeLapse) {
    
    if (agvID == 9) {
      System.out.println("hello");
    }
    
    if (state == State.ACTIVE
        && roadModel.get().getPosition(this).equals(stationEntrance)
        && path.size() == 1) {
      // if the agv reaches the entrance of the station, then it becomes idle
      // and will try to move to the exit of the station
      state = State.IDLE;
      path = new LinkedList<>(
          roadModel.get().getShortestPathTo(this, stationExit));
    } else if (state == State.IDLE && roadModel.get().getPosition(this).equals(stationExit)) {
      // of the agv reaches the exit of the station, then it becomes active
      state = State.ACTIVE;
      nextDestination(timeLapse.getEndTime());
    }
    
    if (state == State.ACTIVE) {
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
  
  @Override
  public void afterTick(TimeLapse timeLapse) {}

}
