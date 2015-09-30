package multistage.garagemodel.delegatemas.makespanandplancost;

import java.util.ArrayList;
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
import routeplan.RangeEndPoint;
import routeplan.ResourceType;
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
  
  @SuppressWarnings("unused")
  private int reachedDestinations = 0;

  private boolean isGoingToExplore;
  
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
    this.isGoingToExplore = false;
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
        roadModel.get().getPosition(this), destinations,
        setting.getNumOfAlterRoutes(), garageList);
    
    executablePlan = new ExecutablePlan(plan, setting);
    currentPlan = plan;
    path = new LinkedList<>(executablePlan.getPath());
    checkPoints = new LinkedList<>(executablePlan.getCheckPoints());
    virtualEnvironment.makeReservation(agvID, plan, currentTime, currentTime + setting.getEvaporationDuration());
    nextExplorationTime = currentTime + setting.getExplorationDuration();
    isGoingToExplore = false;
    nextRefreshTime = currentTime + setting.getRefreshDuration();
  }

  @Override
  public void tick(TimeLapse timeLapse) {
    
    final long currentTime = timeLapse.getStartTime();
    
    if (state == State.ACTIVE
        && roadModel.get().getPosition(this).equals(garage)
        && path.size() == 1) {
      // if the agv reaches the entrance of the garage, then it becomes idle
      // and will try to move into the garage
      state = State.IDLE;
      finishTime = currentTime;
      result.updateResult(0, finishTime);
    }
    
    if (state == State.IDLE) {
      return;
    }
    
    if (!currentPlan.getIntervals().isEmpty() && currentTime >= currentPlan.getIntervals().get(0).upperEndpoint()) {
      final long endTime = currentPlan.getIntervals().get(0).upperEndpoint();
      final List<Point> resource = new ArrayList<>();
      if (currentPlan.getIntervals().size() % 2 == 1) {
        // first plan step is for a node
        resource.add(currentPlan.getPath().get(0));
        virtualEnvironment.setVisited(agvID, resource, endTime);
      } else {
        // first plan step is for an edge
        resource.add(currentPlan.getPath().get(0));
        resource.add(currentPlan.getPath().get(1));
        virtualEnvironment.setVisited(agvID, resource, endTime);
      }
      currentPlan.removeFirstStep();
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
      nextExplorationTime = currentTime;
    }
    
    if (isFreezing && roundedPos.equals(checkPoints.getFirst().getPoint()) && propagatedDelay) {
      return;
    }
    
    if (currentTime == nextExplorationTime) {
      nextExplorationTime = currentTime + setting.getExplorationDuration();
      isGoingToExplore = true;
    }
    
    if (state == State.ACTIVE) {
      // if not idle
      // if explore, only explore at checkpoints
      if (isGoingToExplore && roundedPos.equals(checkPoints.getFirst().getPoint())) {
        // get the next check point
        final CheckPoint nextCheckPoint = checkPoints.getFirst();
        // get the next point of the path, we will explore from this point
        if (nextCheckPoint.getResourceType() == ResourceType.NODE) {
          // if the next check point is a node, then just explore
          boolean changePlan = explore(Range.closed(currentTime, nextCheckPoint.getExpectedTime()), roundedPos, setting.getNumOfAlterRoutes());
          isGoingToExplore = false;
          if (changePlan) {
            virtualEnvironment.makeReservation(agvID, currentPlan, currentTime, currentTime + setting.getEvaporationDuration());
            nextRefreshTime = currentTime + setting.getRefreshDuration();
          }
        } else {
          // if the check point is on an edge, then start explore from the end
          // node of that edge
          final Point lastNode = currentPlan.getPath().get(0);
          final Range<Long> edgeInterval = currentPlan.getIntervals().get(0);
          final long timeToEnterNodeFromEdgeCheckPoint = (long) ((setting
              .getVehicleLength() + 0.1) * 1000 / setting.getVehicleSpeed());
          final long earliestStartTime = currentTime
              + timeToEnterNodeFromEdgeCheckPoint;
          final long latestStartTime = nextCheckPoint.getExpectedTime()
              + timeToEnterNodeFromEdgeCheckPoint;
          boolean changePlan = explore(Range.closed(earliestStartTime, latestStartTime), checkPoints.get(1).getPoint(),
              setting.getNumOfAlterRoutes());
          if (changePlan) {
            // if the AGV change the plan, add the check point on the edge to
            // the checkpoint list
            // the current check point on the edge
            final long currentCheckPointTime = currentPlan.getIntervals().get(0)
                .lowerEndpoint()
                - ((long) (0.1 * 1000 / setting.getVehicleSpeed()));
            checkPoints.addFirst(new CheckPoint(nextCheckPoint.getPoint(),
                currentCheckPointTime, nextCheckPoint.getResource(),
                nextCheckPoint.getResourceType(), nextCheckPoint.getID()));
            // add two last plan step before exploration to the plan. Note that
            // the interval of the edge may change
            final long timeToLeaveEdgeFromCheckPoint = (long) ((setting
                .getVehicleLength() + 0.1) * 1000 / setting.getVehicleSpeed());
            currentPlan.addPreviousEdgeStep(lastNode,
                Range.closed(edgeInterval.lowerEndpoint(),
                    currentCheckPointTime + timeToLeaveEdgeFromCheckPoint));
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
      if (!checkPoints.isEmpty()
          && roundedPos.equals(checkPoints.getFirst().getPoint())) {
        
        // set visited the current check point
        final long endTime = currentPlan.getIntervals().get(0).upperEndpoint();
        final List<Point> resource = new ArrayList<>();
        if (currentPlan.getIntervals().size() % 2 == 1) {
          // first plan step is for a node
          resource.add(currentPlan.getPath().get(0));
          virtualEnvironment.setVisited(agvID, resource, endTime);
        } else {
          // first plan step is for an edge
          resource.add(currentPlan.getPath().get(0));
          resource.add(currentPlan.getPath().get(1));
          virtualEnvironment.setVisited(agvID, resource, endTime);
        }
        
        if (currentTime < checkPoints.getFirst().getExpectedTime()) {
          if (noHigherPriorityAGVs(checkPoints.getFirst().getExpectedTime(),
              checkPoints.get(1).getResource()) && nextResourceIsFree(checkPoints.get(1))) {
            // remove the first step of the plan
            if (checkPoints.getFirst().getResourceType() == ResourceType.NODE) {
              // if is currently at a node
              final long timeToLeaveNodeFromCheckPoint = (long) (setting
                  .getVehicleLength()* 1000 / setting.getVehicleSpeed());
              final Range<Long> newFirstInterval = Range.closed(currentPlan.getIntervals().get(0).lowerEndpoint(),
                  currentTime + timeToLeaveNodeFromCheckPoint);
              final long newLowerEndPoint = currentTime;
              final long newUpperEndPoint = currentPlan.getIntervals().get(1)
                  .upperEndpoint();
              final Range<Long> newSecondInterval = Range.closed(newLowerEndPoint,
                  newUpperEndPoint);
              currentPlan.modifyFirstAndSecondIntervals(newFirstInterval,
                  newSecondInterval);
              virtualEnvironment.modifyReservation(agvID,
                  checkPoints.get(0).getResource(), newFirstInterval, RangeEndPoint.UPPER);
              virtualEnvironment.modifyReservation(agvID,
                  checkPoints.get(1).getResource(), newSecondInterval, RangeEndPoint.LOWER);
            } else {
              // if is currently at an edge
              final long timeToLeaveEdgeFromCheckPoint = (long) ((setting
                  .getVehicleLength() + 0.1)* 1000 / setting.getVehicleSpeed());
              final Range<Long> newFirstInterval = Range.closed(currentPlan.getIntervals().get(0).lowerEndpoint(),
                  currentTime + timeToLeaveEdgeFromCheckPoint);
              
              final long timeLeftToEnterNodeFromEdgeCheckPoint = (long) (0.1 * 1000 / setting.getVehicleSpeed());
              final long newLowerEndPoint = currentTime + timeLeftToEnterNodeFromEdgeCheckPoint;
              final long newUpperEndPoint = currentPlan.getIntervals().get(1)
                  .upperEndpoint();
              final Range<Long> newSecondInterval = Range.closed(newLowerEndPoint,
                  newUpperEndPoint);
              currentPlan.modifyFirstAndSecondIntervals(newFirstInterval,
                  newSecondInterval);
              virtualEnvironment.modifyReservation(agvID,
                  checkPoints.get(0).getResource(), newFirstInterval, RangeEndPoint.UPPER);
              virtualEnvironment.modifyReservation(agvID,
                  checkPoints.get(1).getResource(), newSecondInterval, RangeEndPoint.LOWER);
            }
            checkPoints.removeFirst();
          } else {
            final long timeDifference = checkPoints.getFirst().getExpectedTime()
                - currentTime;
            if (timeDifference < timeLapse.getTimeLeft()) {
              timeLapse.consume(timeDifference);
              checkPoints.removeFirst();
            } else if (timeDifference == timeLapse.getTimeLeft()) {
              timeLapse.consume(timeDifference);
              checkPoints.removeFirst();
            } else {
              // time difference is larger than time left
              timeLapse.consumeAll();
            }
          }
        } else {
          checkPoints.removeFirst();
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
  
  /**
   * Explore new routes.
   *
   * @param startTime the start time
   * @param startNode the start node
   * @param numberOfRoutes the number of routes
   * @return true, if change the current plan
   */
  public boolean explore(Range<Long> startTime, Point startNode, int numberOfRoutes) {
    Plan plan = virtualEnvironment.exploreRoute(agvID, startTime, startNode, destinations, numberOfRoutes, garageList);
    
    if (plan == null) {
      return false;
    }
    
    final long expectedArrivalTime = currentPlan.getArrivalTime();
    
    if (expectedArrivalTime - plan.getArrivalTime() > setting.getSwitchingThreshold()) {
      executablePlan = new ExecutablePlan(plan, setting);
      currentPlan = plan;
      path = new LinkedList<>(executablePlan.getPath());
      checkPoints = new LinkedList<>(executablePlan.getCheckPoints());
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
  
  public Plan getCurrentPlan() {
    return currentPlan;
  }
  
  public void notifyDelay(Plan newPlan, long currentTime) {
    currentPlan = newPlan;
    executablePlan = new ExecutablePlan(currentPlan, setting);
    checkPoints = new LinkedList<>(executablePlan.getCheckPoints());

    while (checkPoints.getFirst().getExpectedTime() < currentTime) {
      checkPoints.removeFirst();
    }

    refresh(currentTime);
    nextExplorationTime = currentTime + 100;
    isGoingToExplore = false;
  }
  
  /**
   * Checks if there is no higher priority agvs that have not entered the resource (count from the timePoint downward)
   *
   * @param timePoint the time point
   * @param resource the resource
   * @return true, if there is no delayed agv that hasn't entered the resource
   */
  public boolean noHigherPriorityAGVs(long timePoint, List<Point> resource) {
    return virtualEnvironment
        .getListOfHigherPriorityAGVs(agvID, timePoint, resource).isEmpty();
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

  @Override
  public void afterTick(TimeLapse timeLapse) {
    if (state == State.IDLE) {
      return;
    }
    
    final Point currentPos = roadModel.get().getPosition(this);
    final Point roundedPos = new Point(round(currentPos.x), round(currentPos.y));
    
    final long currentTime = timeLapse.getEndTime();
    
    if (isFreezing && roundedPos.equals(checkPoints.getFirst().getPoint())) {
      if (!propagatedDelay) {
        virtualEnvironment.propagateDelay(agvID, currentTime);
        propagatedDelay = true;
      } else {
        if (currentTime == nextRefreshTime - 100) {
          refresh(currentTime);
        }
      }
    }
  }

}