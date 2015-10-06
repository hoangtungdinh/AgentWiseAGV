package singlestage.contextaware.repair;

import static com.google.common.base.Preconditions.checkState;

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
import result.plancostandmakespan.Result;
import routeplan.CheckPoint;
import routeplan.ExecutablePlan;
import routeplan.Plan;
import routeplan.ResourceType;
import setting.Setting;
import singlestage.destinationgenerator.OriginDestination;

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
  
  @SuppressWarnings("unused")
  private Plan currentPlan;
  
  /**
   * The swap token, to indicate that each the agv is only allowed to swap at
   * each resource once.
   */
  private boolean swapToken;
  
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
    currentPlan = plan;
    path = new LinkedList<>(executablePlan.getPath());
    checkPoints = new LinkedList<>(executablePlan.getCheckPoints());
    virtualEnvironment.makeReservation(agvID, plan, 0, Long.MAX_VALUE - 100000);
    virtualEnvironment.notifyPlanned();
  }

  @Override
  public void tick(TimeLapse timeLapse) {
    
    if (!virtualEnvironment.finishPlanningPhase()) {
      return;
    }
    
    final long currentTime = timeLapse.getStartTime();

    // add the agv to the model if the start time has passed and no delayed
    // vehicle at the origin
    if (!roadModel.get().containsObject(this)) {
      if (nextResourceIsFree(checkPoints.getFirst()) && virtualEnvironment
          .isAllowedToMove(agvID, checkPoints.getFirst().getResource())) {
        roadModel.get().addObjectAt(this, origin);
        swapToken = true;
      } else {
        if (swapToken) {
          swapToken = false;
          virtualEnvironment.swapOrder(checkPoints, agvID, false);
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
      virtualEnvironment.removeFirstAGV(checkPoints.getFirst().getResource());
      checkPoints.removeFirst();
      checkState(checkPoints.isEmpty(),
          "Checkpoints has to be empty after agv reaching the destination");
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
      
      if (checkPoints.getFirst().getResourceType() == ResourceType.NODE) {
        if (nextResourceIsFree(checkPoints.get(1)) && virtualEnvironment.isAllowedToMove(agvID, checkPoints.get(1).getResource())) {
          // if the agv is allowed to move to the next edge
          virtualEnvironment.setFirstOrderVisited(checkPoints.getFirst().getResource());
          virtualEnvironment.setFirstOrderVisited(checkPoints.get(1).getResource());
          // remove itself from the order list of the current node
          virtualEnvironment.removeFirstAGV(checkPoints.getFirst().getResource());
          // remove the current checkpoint
          checkPoints.removeFirst();
          swapToken = true;
        } else {
          // if it is not allowed to move
          if (swapToken) {
            swapToken = false;
            virtualEnvironment.swapOrder(checkPoints, agvID, true);
          }
          timeLapse.consumeAll();
        }
      } else {
        // if the current checkpoint is on an edge
        if (nextResourceIsFree(checkPoints.get(1)) && virtualEnvironment.isAllowedToMove(agvID, checkPoints.get(1).getResource())) {
          // if the agv is allowed to move to the next node
          // only remove the current checkpoint (since its order on the list of the current edge was removed)
          virtualEnvironment.removeFirstAGV(checkPoints.getFirst().getResource());
          checkPoints.removeFirst();
          swapToken = true;
        } else {
          // if it is not allowed to move
          if (swapToken) {
            swapToken = false;
            virtualEnvironment.swapOrder(checkPoints, agvID, true);
          }
          timeLapse.consumeAll();
        }
      }
    }
    
    if (timeLapse.hasTimeLeft()) {
      roadModel.get().followPath(this, path, timeLapse);
    }
  }
  
  public boolean nextResourceIsFree(CheckPoint nextCheckPoint) {
    if (nextCheckPoint.getResourceType() == ResourceType.NODE) {
      final Point nextNode = nextCheckPoint.getResource().get(0);
      return !roadModel.get().isOccupied(nextNode);
    } else {
      if (getNumOfAGVsOnEdge(nextCheckPoint.getResource().get(0), nextCheckPoint.getResource().get(1)) < 2) {
        return true;
      } else {
        return false;
      }
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
  
  /**
   * Gets the id.
   *
   * @return the id
   */
  public int getID() {
    return agvID;
  }
  
  /**
   * Gets the check points.
   *
   * @return the check points
   */
  public List<CheckPoint> getCheckPoints() {
    return checkPoints;
  }

}

