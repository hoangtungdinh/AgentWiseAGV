package singlestage.delegatemas;

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
import com.google.common.collect.Range;

import incidentgenerator.Incident;
import incidentgenerator.IncidentList;
import routeplan.CheckPoint;
import routeplan.ExecutablePlan;
import routeplan.Plan;
import routeplan.RangeEndPoint;
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
  
  /** The start time of the plan of this AGV. */
  private long startTime;
  
  /** The simulator. */
  private Simulator sim;
  
  /** The setting. */
  private Setting setting;
  
  /** The result. */
  private Result result;

  private boolean hasCompleted;

  private IncidentList incidentList;

  private Incident nextIncident;

  private long endOfFreezingTime;
  
  /**
   * True if the agv is going to explore route. Note that agv only explores
   * route at the checkpoint.
   */
  private boolean isGoingToExplore;
  
  /** True if this agv is freezing. */
  private boolean isFreezing;
  
  private boolean propagatedDelay;
  
  private boolean planAgain;
  
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
    nextDestination(0);
    startTime = checkPoints.getFirst().getExpectedTime();
    this.result = result;
    this.isGoingToExplore = false;
    this.hasCompleted = false;
    this.propagatedDelay = true;
    this.planAgain = false;
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
  }

  @Override
  public double getSpeed() {
    return setting.getVehicleSpeed();
  }

  void nextDestination(long currentTime) {
    List<Point> dest = new ArrayList<>();
    dest.add(destination);

    Plan plan = virtualEnvironment.exploreRoute(agvID, Range.atLeast(currentTime), origin, dest,
        setting.getNumOfAlterRoutes(), false);
    
    executablePlan = new ExecutablePlan(plan, setting);
    currentPlan = plan;
    path = new LinkedList<>(executablePlan.getPath());
    checkPoints = new LinkedList<>(executablePlan.getCheckPoints());
    startTime = checkPoints.getFirst().getExpectedTime();
    virtualEnvironment.makeReservation(agvID, plan, currentTime, currentTime + setting.getEvaporationDuration());
    nextExplorationTime = currentTime + setting.getExplorationDuration();
    isGoingToExplore = false;
    nextRefreshTime = currentTime + setting.getRefreshDuration();
  }

  @Override
  public void tick(TimeLapse timeLapse) {
    
    final long currentTime = timeLapse.getStartTime();
    
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
    
    // this code is only executed when the agv hasn't entered the map
    if (planAgain) {
      nextDestination(currentTime);
      planAgain = false;
    }
    
    if (currentTime == startTime) {
      roadModel.get().addObjectAt(this, origin);
      final List<Point> resource = new ArrayList<>();
      final long endTime = currentPlan.getIntervals().get(0).upperEndpoint();
      resource.add(currentPlan.getPath().get(0));
      virtualEnvironment.setVisited(agvID, resource, endTime);
    }
    
    if (!roadModel.get().containsObject(this)) {
      
      startTime = checkPoints.getFirst().getExpectedTime();
      
      if (currentTime == nextExplorationTime) {
        final long startTimeOfExploration = timeLapse.getEndTime();
        boolean changePlan = explore(Range.atLeast(startTimeOfExploration), origin, setting.getNumOfAlterRoutes(), false);
        nextExplorationTime = currentTime + setting.getExplorationDuration();
        if (changePlan) {
          startTime = checkPoints.getFirst().getExpectedTime();
          virtualEnvironment.makeReservation(agvID, currentPlan, currentTime, currentTime + setting.getEvaporationDuration());
          nextRefreshTime = currentTime + setting.getRefreshDuration();
        }
      }

      if (currentTime == nextRefreshTime) {
        refresh(currentTime);
      }
      
      if (noHigherPriorityAGVs(startTime,
          checkPoints.getFirst().getResource()) && nextResourceIsFree(checkPoints.getFirst())) {
        final long newLowerEndPoint = currentTime;
        final long newUpperEndPoint = currentPlan.getIntervals().get(0)
            .upperEndpoint();
        final Range<Long> newInterval = Range.closed(newLowerEndPoint,
            newUpperEndPoint);
        currentPlan.modifyFirstInterval(newInterval);
        virtualEnvironment.modifyReservation(agvID,
            checkPoints.getFirst().getResource(), newInterval, RangeEndPoint.LOWER);
        startTime = currentTime;
        roadModel.get().addObjectAt(this, origin);
        final List<Point> resource = new ArrayList<>();
        final long endTime = currentPlan.getIntervals().get(0).upperEndpoint();
        resource.add(currentPlan.getPath().get(0));
        virtualEnvironment.setVisited(agvID, resource, endTime);
      }
      return;
    }
    
    final Point currentPos = roadModel.get().getPosition(this);
    final Point roundedPos = new Point(round(currentPos.x), round(currentPos.y));
    
    if (roundedPos.equals(destination)) {
      // virtualEnvironment.setVisited(agvID, checkPoints.getFirst());
      this.hasCompleted = true;
      result.updateResult(startTime, timeLapse.getTime());
      virtualEnvironment.modifyReservation(agvID,
          checkPoints.getFirst().getResource(),
          Range.closed(currentPlan.getIntervals().get(0).lowerEndpoint(),
              currentTime),
          RangeEndPoint.UPPER);
      final long endTime = currentPlan.getIntervals().get(0).upperEndpoint();
      final List<Point> resource = new ArrayList<>();
      resource.add(currentPlan.getPath().get(0));
      virtualEnvironment.setVisited(agvID, resource, endTime);
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
      nextExplorationTime = currentTime;
    }
    
    if (isFreezing && roundedPos.equals(checkPoints.getFirst().getPoint()) && propagatedDelay) {
      return;
    }
    
    if (currentTime == nextExplorationTime) {
      nextExplorationTime = currentTime + setting.getExplorationDuration();
      isGoingToExplore = true;
    }
    
    // if explore, only explore at checkpoint
    if (isGoingToExplore && roundedPos.equals(checkPoints.getFirst().getPoint())) {
      // get the next check point
      final CheckPoint nextCheckPoint = checkPoints.getFirst();
      // get the next point of the path, we will explore from this point
      if (nextCheckPoint.getResourceType() == ResourceType.NODE) {
        // if the next check point is a node, then just explore
        boolean changePlan = explore(Range.closed(currentTime, nextCheckPoint.getExpectedTime()), roundedPos, setting.getNumOfAlterRoutes(), true);
        isGoingToExplore = false;
        if (changePlan) {
          virtualEnvironment.makeReservation(agvID, currentPlan, currentTime, currentTime + setting.getEvaporationDuration());
          nextRefreshTime = currentTime + setting.getRefreshDuration();
        }
      } else {
        // if the check point is on an edge, then start explore from the end
        // node of that edge
        final Point lastNode = currentPlan.getPath().get(0);
        // the last node and edge interval before exploration
        final Range<Long> edgeInterval = currentPlan.getIntervals().get(0);
        final long timeToEnterNodeFromEdgeCheckPoint = (long) ((setting
            .getVehicleLength() + 0.1) * 1000 / setting.getVehicleSpeed());
        final long earliestStartTime = currentTime
            + timeToEnterNodeFromEdgeCheckPoint;
        final long latestStartTime = nextCheckPoint.getExpectedTime()
            + timeToEnterNodeFromEdgeCheckPoint;
        boolean changePlan = explore(
            Range.closed(earliestStartTime, latestStartTime),
            checkPoints.get(1).getPoint(), setting.getNumOfAlterRoutes(), true);
        isGoingToExplore = false;
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
          virtualEnvironment.makeReservation(agvID, currentPlan, currentTime,
              currentTime + setting.getEvaporationDuration());
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
  
  public double round(double input) {
    return (Math.round(input * 10) / 10d);
  }
  
  /**
   * Explore new routes.
   *
   * @param startTime the start time
   * @param startNode the start node
   * @param numberOfRoutes the number of routes
   * @param started the started
   * @return true, if change the current plan
   */
  public boolean explore(Range<Long> startTime, Point startNode, int numberOfRoutes, boolean started) {
    List<Point> dest = new ArrayList<>();
    dest.add(destination);
    Plan plan = virtualEnvironment.exploreRoute(agvID, startTime, startNode, dest, numberOfRoutes, started);
    
    if (started && plan == null) {
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
  
  public Plan getCurrentPlan() {
    return currentPlan;
  }
  
  public void notifyDelay(Plan newPlan, long currentTime) {
    if (currentTime < startTime) {
      // case when the agv hasn't entered the map
      planAgain = true;
    } else {
      // case when the agv entered the map
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
  }
  
  public boolean hasCompleted() {
    return hasCompleted;
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
    if (!roadModel.get().containsObject(this)) {
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