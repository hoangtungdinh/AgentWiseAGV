package singlestage.contextaware;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.road.CollisionGraphRoadModel;
import com.github.rinde.rinsim.core.model.road.MovingRoadUser;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.core.model.time.TickListener;
import com.github.rinde.rinsim.core.model.time.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;

import incidentgenerator.Incident;
import incidentgenerator.IncidentList;
import routeplan.CheckPoint;
import routeplan.ExecutablePlan;
import routeplan.Plan;
import routeplan.ResourceType;
import setting.Setting;
import singlestage.destinationgenerator.OriginDestination;
import singlestage.result.Result;

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
  
  /** The start time of the plan of this AGV. */
  private long startTime;
  
  /** The simulator. */
  private Simulator sim;
  
  /** The setting. */
  private Setting setting;
  
  /** The result. */
  private Result result;
  
  private IncidentList incidentList;

  private Incident nextIncident;
  
  private long endOfFreezingTime;
  
  private boolean isFreezing;
  
  private boolean propagatedDelay;
  
  /**
   * Instantiates a new vehicle agent.
   *
   * @param originDestination the origin destination
   * @param virtualEnvironment the virtual environment
   * @param agvID the agv id
   * @param sim the sim
   * @param setting the setting
   * @param result the result
   */
  public VehicleAgent(OriginDestination originDestination, VirtualEnvironment virtualEnvironment,
      int agvID, Simulator sim, Setting setting, Result result, IncidentList incidentList) {
    roadModel = Optional.absent();
    path = new LinkedList<>();
    this.origin = originDestination.getOrigin();
    this.destination = originDestination.getDestination();
    this.virtualEnvironment = virtualEnvironment;
    this.agvID = agvID;
    this.sim = sim;
    this.setting = setting;
    planRoute();
    startTime = checkPoints.getFirst().getExpectedTime();
    this.result = result;
    this.isFreezing = false;
    this.propagatedDelay = true;
    this.incidentList = incidentList;
    if (!this.incidentList.isEmpty()) {
      this.nextIncident = this.incidentList.getNextIncident();
    } else {
      nextIncident = null;
    }
    this.endOfFreezingTime = -1;
  }

  @Override
  public void initRoadUser(RoadModel model) {
    roadModel = Optional.of((CollisionGraphRoadModel) model);
  }

  @Override
  public double getSpeed() {
    return setting.getVehicleSpeed();
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
    executablePlan = new ExecutablePlan(plan, setting);
    path = new LinkedList<>(executablePlan.getPath());
    checkPoints = new LinkedList<>(executablePlan.getCheckPoints());
    virtualEnvironment.makeReservation(agvID, plan, 0, Long.MAX_VALUE - 100000);
  }

  @Override
  public void tick(TimeLapse timeLapse) {
    
    final long currentTime = timeLapse.getStartTime();

    // add the agv to the model if the start time has passed and no delayed
    // vehicle at the origin
    if (timeLapse.getStartTime() >= startTime) {
      if (!roadModel.get().containsObject(this)) {
        if (isSafeToMove(true) && !roadModel.get().isOccupied(checkPoints.getFirst().getPoint())) {
          // if no delayed agv, the node is not occupied then start
          roadModel.get().addObjectAt(this, origin);
          virtualEnvironment.setVisited(agvID, checkPoints.getFirst());
        }
      }
    }
    
    if (!roadModel.get().containsObject(this)) {
      return;
    }
    
    final Point currentPos = roadModel.get().getPosition(this);
    final Point roundedPos = new Point(round(currentPos.x), round(currentPos.y));
    
    // check if check point is the destination then update result and return
    if (roundedPos.equals(destination)) {
      virtualEnvironment.setVisited(agvID, checkPoints.getFirst());
      result.updateResult(startTime, timeLapse.getTime());
      sim.unregister(this);
      return;
    }
    
    if (nextIncident != null) {
      if (currentTime >= nextIncident.getStartTime() && propagatedDelay == true) {
        isFreezing = true;
        propagatedDelay = false;
        
        final double distanceToNextCheckPoint = Point.distance(roundedPos,
            checkPoints.getFirst().getPoint());
        final long timeToNextCheckPoint = (long) (distanceToNextCheckPoint*1000 / setting.getVehicleSpeed());
        
        if (endOfFreezingTime > currentTime) {
          endOfFreezingTime += nextIncident.getDuration() + timeToNextCheckPoint;
        } else {
          endOfFreezingTime = currentTime + nextIncident.getDuration() + timeToNextCheckPoint;
        }
        
        if (!incidentList.isEmpty()) {
          nextIncident = incidentList.getNextIncident();
        } else {
          nextIncident = null;
        }
      }
    }
    
    if (isFreezing && currentTime >= endOfFreezingTime) {
      isFreezing = false;
    }
    
    if (isFreezing && roundedPos.equals(checkPoints.getFirst().getPoint()) && propagatedDelay) {
      return;
    }
    
    // IDEA: check the position. If right before it leaves an edge or a node,
    // it still have to wait, then consume time
    if (!checkPoints.isEmpty()
        && roundedPos.equals(checkPoints.getFirst().getPoint())) {
      // if the AGV is at exactly the check point
      
      // if the check point is a node then announce to both the node and the next edge that it has visited
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
  
  /**
   * Checks if is safe to move to the next resource.
   *
   * @return true, if there is no delayed agv that hasn't entered the resource
   */
  public boolean isSafeToMove(boolean isFirstCheckPoint) {
    if (isFirstCheckPoint) {
      return virtualEnvironment.getListOfDelayedAGVs(agvID,
          startTime, checkPoints.getFirst()).isEmpty();
    } else {
      return virtualEnvironment.getListOfDelayedAGVs(agvID,
          checkPoints.getFirst().getExpectedTime(), checkPoints.get(1)).isEmpty();
    }
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
  
  public double round(double input) {
    return (Math.round(input * 10) / 10d);
  }
  
  @Override
  public void afterTick(TimeLapse timeLapse) {
    if (!roadModel.get().containsObject(this)) {
      return;
    }
    
    final Point currentPos = roadModel.get().getPosition(this);
    final Point roundedPos = new Point(round(currentPos.x), round(currentPos.y));
    
    if (isFreezing && roundedPos.equals(checkPoints.getFirst().getPoint())) {
      if (!propagatedDelay) {
        propagatedDelay = true;
      }
    }
  }

}

