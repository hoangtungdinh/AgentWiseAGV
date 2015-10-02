package multistage.garagemodel.contextaware.repair.makespanandplancost;

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
import com.google.common.collect.Range;

import incidentgenerator.Incident;
import incidentgenerator.IncidentList;
import multistage.Destinations;
import multistage.State;
import result.plancostandmakespan.Result;
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
  
  /** The current plan. */
  private Plan currentPlan;
  
  /** The executable plan. */
  private ExecutablePlan executablePlan;
  
  /** The check points. */
  private LinkedList<CheckPoint> checkPoints;
  
  /** The initial pos. */
  private Point initialPos;
  
  /** The state. */
  private State state;
  
  private LinkedList<Point> destinations;
  
  private Setting setting;
  
  private Result result;
  
  private List<Point> garageList;
  
  private Point garage;
  
  @SuppressWarnings("unused")
  private int reachedDestinations = 0;

  private boolean isFreezing;
  
  private boolean propagatedDelay;
  
  private IncidentList incidentList;

  private Incident nextIncident;

  private long endOfFreezingTime;
  
  private long finishTime;
  
  public VehicleAgent(Destinations destinationList, VirtualEnvironment virtualEnvironment,
      int agvID, List<Point> garageList, Setting setting, Result result, IncidentList incidentList) {
    roadModel = Optional.absent();
    path = new LinkedList<>();
    this.destinationList = destinationList;
    this.virtualEnvironment = virtualEnvironment;
    this.agvID = agvID;
    this.garageList = garageList;
    this.garage = garageList.get(agvID);
    this.initialPos = garage;
    state = State.ACTIVE;
    this.setting = setting;
    this.destinations = new LinkedList<>();
    this.result = result;
    this.propagatedDelay = true;
    this.isFreezing = false;
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
    roadModel.get().addObjectAt(this, initialPos);
    path = new LinkedList<>();
    nextDestination(0);
    virtualEnvironment.notifyPlanned();
  }

  @Override
  public double getSpeed() {
    return setting.getVehicleSpeed();
  }

  void nextDestination(long currentTime) {
    
    destinations = new LinkedList<>();
    for (int i = 0; i < setting.getNumOfDestsForEachAGV(); i++) {
      destinations.addLast(destinationList.getDestination());
    }
    destinations.addLast(garage);
    
    Plan plan = virtualEnvironment.exploreRoute(agvID, Range.atLeast(currentTime),
        roadModel.get().getPosition(this), destinations, garageList);
    
    executablePlan = new ExecutablePlan(plan, setting);
    currentPlan = plan;
    path = new LinkedList<>(executablePlan.getPath());
    checkPoints = new LinkedList<>(executablePlan.getCheckPoints());
    virtualEnvironment.makeReservation(agvID, currentPlan, currentTime, Long.MAX_VALUE - currentTime);
  }

  @Override
  public void tick(TimeLapse timeLapse) {
    
    if (!virtualEnvironment.finishPlanningPhase()) {
      return;
    }
    
    final long currentTime = timeLapse.getStartTime();
    
    if (state == State.ACTIVE
        && roadModel.get().getPosition(this).equals(garage)
        && path.size() == 1) {
      // if the agv reaches the entrance of the garage, then it becomes idle
      // and will try to move into the garage
      finishTime = currentTime;
      result.updateResult(0, finishTime);
      state = State.IDLE;
    }
    
    if (state == State.IDLE) {
      return;
    }
    
    final Point currentPos = roadModel.get().getPosition(this);
    final Point roundedPos = new Point(round(currentPos.x), round(currentPos.y));
    
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
    
    if (state == State.ACTIVE) {
      // if not idle
      // IDEA: check the position. If right before it leaves an edge or a node,
      // it still have to wait, then consume time
      if (!checkPoints.isEmpty()
          && roundedPos.equals(checkPoints.getFirst().getPoint())) {
        
        if (checkPoints.getFirst().getResourceType() == ResourceType.NODE) {
          if (nextResourceIsFree(checkPoints.get(1)) && virtualEnvironment.isAllowedToMove(agvID, checkPoints.get(1).getResource())) {
            // if the agv is allowed to move to the next edge
            // remove itself from the order list of the current node
            virtualEnvironment.removeFirstAGV(checkPoints.getFirst().getResource());
            // also remove itself from the order list of the next edge
            virtualEnvironment.removeFirstAGV(checkPoints.get(1).getResource());
            // remove the current checkpoint
            checkPoints.removeFirst();
          } else {
            // if it is not allowed to move
            timeLapse.consumeAll();
          }
        } else {
          // if the current checkpoint is on an edge
          if (nextResourceIsFree(checkPoints.get(1)) && virtualEnvironment.isAllowedToMove(agvID, checkPoints.get(1).getResource())) {
            // if the agv is allowed to move to the next node
            // only remove the current checkpoint (since its order on the list of the current edge was removed)
            checkPoints.removeFirst();
          } else {
            // if it is not allowed to move
            timeLapse.consumeAll();
          }
        }
      }

      if (timeLapse.hasTimeLeft()) {
        roadModel.get().followPath(this, path, timeLapse);
      }
    }
    
    if (destinations.size() > 1
        && roadModel.get().getPosition(this).equals(destinations.getFirst())) {
      reachedDestinations++;
      destinations.removeFirst();
    }
  }
  
  public double round(double input) {
    return (Math.round(input * 10) / 10d);
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
  
  public Plan getCurrentPlan() {
    return currentPlan;
  }
  
  public int getID() {
    return agvID;
  }
  
  public List<CheckPoint> getCheckPoints() {
    return checkPoints;
  }

  @Override
  public void afterTick(TimeLapse timeLapse) {
    if (state == State.IDLE) {
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