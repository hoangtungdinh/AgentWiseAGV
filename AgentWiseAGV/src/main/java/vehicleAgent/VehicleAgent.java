package vehicleAgent;

import java.util.ArrayList;
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
  
  private Simulator sim;
  
  private Point destination;
  
  public VehicleAgent(Destinations destinations, VirtualEnvironment virtualEnvironment,
      int agvID, Simulator sim, Point initialPos, List<Point> centralStation) {
    roadModel = Optional.absent();
    path = new LinkedList<>();
    this.destinationList = destinations;
    this.virtualEnvironment = virtualEnvironment;
    this.agvID = agvID;
    this.initialPos = initialPos;
    stationEntrance = centralStation.get(centralStation.size() - 1);
    stationExit = centralStation.get(0);
    state = State.IDLE;
    this.sim = sim;
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
    
    destination = destinationList.getDestination();
    List<Point> dest = new ArrayList<>();
    dest.add(destination);
    
    List<Point> stationExits = new ArrayList<>();
    stationExits.add(stationExit);
    Plan plan = virtualEnvironment.exploreRoute(agvID, startTime,
        roadModel.get().getPosition(this), dest, stationExits);
    executablePlan = new ExecutablePlan(plan);
    path = new LinkedList<>(executablePlan.getPath());
    checkPoints = new LinkedList<>(executablePlan.getCheckPoints());
    virtualEnvironment.makeReservation(agvID, plan, startTime, plan.getArrivalTime());
  }

  @Override
  public void tick(TimeLapse timeLapse) {
    
//    if (currentPlan != null) {
//      System.out.println();
//      System.out.println(agvID + " current plan");
//      System.out.println(currentPlan.getPath());
//      System.out.println(currentPlan.getIntervals());
//    }
    
//    if (destination.isPresent()) {
//      System.out.println(agvID + " " + destination.get());
//    }
    
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

    if (roadModel.get().getPosition(this).equals(destination)) {
      sim.unregister(this);
    }
    
    
  }
  
  public double round(double input) {
    return (Math.round(input * 10) / 10d);
  }
  
  @Override
  public void afterTick(TimeLapse timeLapse) {}

}

