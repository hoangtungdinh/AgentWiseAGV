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

import destinationGenerator.OriginDestination;
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
  
  /** The origin. */
  private Point origin;
  
  /** The destination. */
  private Point destination;
  
  /** The virtual environment. */
  private VirtualEnvironment virtualEnvironment;
  
  /** The agv id. */
  private int agvID;
  
  /** The executable plan. */
  private ExecutablePlan executablePlan;
  
  /** The check points. */
  private LinkedList<CheckPoint> checkPoints;
  
  private Simulator sim;
  
  public VehicleAgent(OriginDestination originDestination, VirtualEnvironment virtualEnvironment,
      int agvID, Simulator sim) {
    roadModel = Optional.absent();
    path = new LinkedList<>();
    this.origin = originDestination.getOrigin();
    this.destination = originDestination.getDestination();
    this.virtualEnvironment = virtualEnvironment;
    this.agvID = agvID;
    this.sim = sim;
  }

  @Override
  public void initRoadUser(RoadModel model) {
    roadModel = Optional.of((CollisionGraphRoadModel) model);
//    roadModel.get().addObjectAt(this, origin);
    planRoute();
  }

  @Override
  public double getSpeed() {
    return AGVSystem.VEHICLE_SPEED;
  }

  /**
   * Plan route.
   *
   * @param startTime the start time
   */
  void planRoute() {
    List<Point> dest = new ArrayList<>();
    dest.add(destination);
    Plan plan = virtualEnvironment.exploreRoute(agvID, origin, dest);
    executablePlan = new ExecutablePlan(plan);
    path = new LinkedList<>(executablePlan.getPath());
    checkPoints = new LinkedList<>(executablePlan.getCheckPoints());
    virtualEnvironment.makeReservation(agvID, plan, 0, plan.getArrivalTime());
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
    
    if (timeLapse.getStartTime() == checkPoints.getFirst().getExpectedTime()) {
      roadModel.get().addObjectAt(this, origin);
    }
    
    // IDEA: check the position. If right before it leaves an edge or a node,
    // it still have to wait, then consume time
    Point currentPos = roadModel.get().getPosition(this);
    Point roundedPos = new Point(round(currentPos.x), round(currentPos.y));
    if (!checkPoints.isEmpty()
        && roundedPos.equals(checkPoints.getFirst().getPoint())) {
      // System.out.println(roadModel.get().getPosition(this));
      // sim.stop();
      if (timeLapse.getStartTime() < checkPoints.getFirst().getExpectedTime()) {
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

