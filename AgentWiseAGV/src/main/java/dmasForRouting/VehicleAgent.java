package dmasForRouting;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.apache.commons.math3.random.RandomGenerator;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.road.CollisionGraphRoadModel;
import com.github.rinde.rinsim.core.model.road.MovingRoadUser;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;

import routePlan.CheckPoint;
import routePlan.ExecutablePlan;
import routePlan.Plan;
import virtualEnvironment.VirtualEnvironment;

class VehicleAgent implements TickListener, MovingRoadUser {
  private final RandomGenerator rng;
  private Optional<CollisionGraphRoadModel> roadModel;
  private Optional<Point> destination;
  private Queue<Point> path;
  private LinkedList<Point> destinationList;
  private VirtualEnvironment virtualEnvironment;
  private int agvID;
  private ExecutablePlan executablePlan;
  private LinkedList<CheckPoint> checkPoints;
  private Simulator sim;
  private int reachedDestinations = 0;

  VehicleAgent(RandomGenerator r, List<Point> destinationList,
      VirtualEnvironment virtualEnvironment, int agvID, Simulator sim) {
    rng = r;
    roadModel = Optional.absent();
    destination = Optional.absent();
    path = new LinkedList<>();
    this.destinationList = new LinkedList<Point>(destinationList);
    this.virtualEnvironment = virtualEnvironment;
    this.agvID = agvID;
    this.sim = sim;
  }

  @Override
  public void initRoadUser(RoadModel model) {
    roadModel = Optional.of((CollisionGraphRoadModel) model);
    Point p;
    do {
      p = model.getRandomPosition(rng);
    } while (roadModel.get().isOccupied(p));
    roadModel.get().addObjectAt(this, p);
  }

  @Override
  public double getSpeed() {
    return AGVSystem.VEHICLE_SPEED;
  }

  void nextDestination(long startTime) {
    if (destinationList.isEmpty()) {
      throw new IllegalStateException("There is no destination left!");
    }
    destination = Optional.of(destinationList.getFirst());
    destinationList.removeFirst();
//    System.out.println(agvID + ": Destination: " + destination.get());
    
    Plan plan = virtualEnvironment.exploreRoute(agvID, startTime, roadModel.get().getPosition(this), destination.get(), 5);
    executablePlan = new ExecutablePlan(plan);
    path = new LinkedList<>(executablePlan.getPath());
    checkPoints = new LinkedList<>(executablePlan.getCheckPoints());
    virtualEnvironment.makeReservation(agvID, plan, Long.MAX_VALUE);
//    System.out.println(plan.getPath());
    
//    for (CheckPoint checkPoint : checkPoints) {
//      System.out.println(checkPoint.getPoint() + " " + checkPoint.getExpectedTime());
//    }
    
//    path = new LinkedList<>(roadModel.get().getShortestPathTo(this,
//        destination.get()));
  }

  @Override
  public void tick(TimeLapse timeLapse) {
    if (!destination.isPresent()) {
      nextDestination(timeLapse.getStartTime());
    }
    
    // IDEA: check the position. If right before it leaves an edge or a node, it still have to wait, then consume time
    Point currentPos = roadModel.get().getPosition(this);
    Point roundedPos = new Point(round(currentPos.x), round(currentPos.y));
    if (!checkPoints.isEmpty()
        && roundedPos.equals(checkPoints.getFirst().getPoint())) {
//      System.out.println(roadModel.get().getPosition(this));
//      sim.stop();
      if (timeLapse.getStartTime() < checkPoints.getFirst().getExpectedTime()) {
        final long timeDifference = checkPoints.getFirst().getExpectedTime() - timeLapse.getStartTime();
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

    if (roadModel.get().getPosition(this).equals(destination.get())) {
      nextDestination(timeLapse.getEndTime());
      System.out.println(agvID + ": Reached destination: " + ++reachedDestinations);
    }
  }
  
  public double round(double input) {
    return (Math.round(input * 10) / 10d);
  }

  @Override
  public void afterTick(TimeLapse timeLapse) {}

}

