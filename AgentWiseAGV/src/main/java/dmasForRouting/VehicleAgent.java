package dmasForRouting;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.apache.commons.math3.random.RandomGenerator;

import com.github.rinde.rinsim.core.model.road.CollisionGraphRoadModel;
import com.github.rinde.rinsim.core.model.road.MovingRoadUser;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;

import virtualEnvironment.VirtualEnvironment;

class VehicleAgent implements TickListener, MovingRoadUser {
  private final RandomGenerator rng;
  private Optional<CollisionGraphRoadModel> roadModel;
  private Optional<Point> destination;
  private Queue<Point> path;
  private LinkedList<Point> destinationList;
  private VirtualEnvironment virtualEnvironment;
  private int agvID;

  VehicleAgent(RandomGenerator r, List<Point> destinationList,
      VirtualEnvironment virtualEnvironment, int agvID) {
    rng = r;
    roadModel = Optional.absent();
    destination = Optional.absent();
    path = new LinkedList<>();
    this.destinationList = new LinkedList<Point>(destinationList);
    this.virtualEnvironment = virtualEnvironment;
    this.agvID = agvID;
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

  void nextDestination() {
    if (destinationList.isEmpty()) {
      throw new IllegalStateException("There is no destination left!");
    }
    destination = Optional.of(destinationList.getFirst());
    destinationList.removeFirst();
    path = new LinkedList<>(roadModel.get().getShortestPathTo(this,
        destination.get()));
  }

  @Override
  public void tick(TimeLapse timeLapse) {
    if (!destination.isPresent()) {
      nextDestination();
    }
    
    // IDEA: check the position. If right before it leaves an edge or a node, it still have to wait, then consume time
    System.out.println(roadModel.get().getPosition(this));
    
    
    roadModel.get().followPath(this, path, timeLapse);

    if (roadModel.get().getPosition(this).equals(destination.get())) {
      nextDestination();
    }
  }

  @Override
  public void afterTick(TimeLapse timeLapse) {}

}

