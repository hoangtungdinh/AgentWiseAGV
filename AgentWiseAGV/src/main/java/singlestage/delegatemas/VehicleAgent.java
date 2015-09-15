package singlestage.delegatemas;

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
import com.google.common.collect.Range;

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
  
  /** The current plan. */
  private Plan currentPlan;
  
  /** The executable plan. */
  private ExecutablePlan executablePlan;
  
  /** The check points. */
  private LinkedList<CheckPoint> checkPoints;
  
  /** The expected arrival time. */
  private long expectedArrivalTime;
  
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
  
  /** True if the agv is freezing. */
  private boolean isFreezing;
  
  /**
   * True if the agv is going to explore routes. Note that the agv is only
   * explore route at the check point. If it is time to explore but the agv has
   * not reached the checkpoint yet, it will wait until it reaches the
   * checkpoint and then explore
   */
  private boolean isGoingToExplore;

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
      int agvID, Simulator sim, Setting setting, Result result) {
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
    this.isFreezing = false;
    this.isGoingToExplore = false;
  }

  @Override
  public void initRoadUser(RoadModel model) {
    roadModel = Optional.of((CollisionGraphRoadModel) model);
  }

  @Override
  public double getSpeed() {
    return setting.getVehicleSpeed();
  }

  void nextDestination(long startTime) {
    List<Point> dest = new ArrayList<>();
    dest.add(destination);

    Plan plan = virtualEnvironment.exploreRoute(agvID, startTime, origin, dest,
        setting.getNumOfAlterRoutes(), false);
    
    executablePlan = new ExecutablePlan(plan, setting);
    currentPlan = plan;
    path = new LinkedList<>(executablePlan.getPath());
    checkPoints = new LinkedList<>(executablePlan.getCheckPoints());
    virtualEnvironment.makeReservation(agvID, plan, startTime, startTime + setting.getEvaporationDuration());
    expectedArrivalTime = plan.getArrivalTime();
    nextExplorationTime = startTime + setting.getExplorationDuration();
    nextRefreshTime = startTime + setting.getRefreshDuration();
  }

  @Override
  public void tick(TimeLapse timeLapse) {
    
    final long currentTime = timeLapse.getStartTime();
    
    if (currentTime > 5000 && currentTime < 50000 && agvID == 5) {
      isFreezing = true;
    } else {
      isFreezing = false;
    }
    
    if (currentPlan != null && currentPlan.getIntervals().size() > 1
        && currentPlan.getIntervals().get(1).upperEndpoint() <= currentTime) {
      currentPlan.removeOldSteps();
    }
    
    if (currentTime == startTime) {
      roadModel.get().addObjectAt(this, origin);
    }
    
    if (!roadModel.get().containsObject(this)) {
      
      final boolean refreshSuccess;
      if (currentTime == nextRefreshTime) {
        // refresh, if refresh is not success then explore (by setting the
        // nextExplorationTime as currentTime)
        refreshSuccess = refresh(currentTime);
        if (!refreshSuccess) {
          nextExplorationTime = currentTime;
        }
      } else {
        refreshSuccess = true;
      }
      
      if (currentTime == nextExplorationTime) {
        final long startTimeOfExploration = timeLapse.getEndTime();
        boolean changePlan = explore(startTimeOfExploration, origin, setting.getNumOfAlterRoutes(), false, refreshSuccess);
        nextExplorationTime = currentTime + setting.getExplorationDuration();
        if (changePlan) {
          startTime = checkPoints.getFirst().getExpectedTime();
          virtualEnvironment.makeReservation(agvID, currentPlan, currentTime, currentTime + setting.getEvaporationDuration());
          nextRefreshTime = currentTime + setting.getRefreshDuration();
        }
      }

      return;
    }
    
    // the current position of the agv
    final Point currentPos = roadModel.get().getPosition(this);
    final Point roundedPos = new Point(round(currentPos.x), round(currentPos.y));
    
    // if the agv is freezing
    if (isFreezing) {
      // if the agv is at a node then freeze it
      if (currentPos.equals(checkPoints.getFirst().getPoint())
          && checkPoints.getFirst().getResourceType() == ResourceType.NODE) {
        if (!currentPlan.isPlanForFreezingAGV()) {
          final List<Point> currentPath = currentPlan.getPath();
          final List<Range<Long>> currentIntervals = currentPlan.getIntervals();
          
          // compute the new intervals for freezing agv
          final LinkedList<Range<Long>> newIntervals = new LinkedList<>();
          for (Range<Long> interval : currentIntervals) {
            final long start = interval.lowerEndpoint() + setting.getExpectedFreezingDuration();
            final long end = interval.upperEndpoint() + setting.getExpectedFreezingDuration();
            if (newIntervals.isEmpty()) {
              newIntervals.addLast(Range.open(currentTime, end));
            } else {
              newIntervals.addLast(Range.open(start, end));
            }
          }
          currentPlan = new Plan(currentPath, newIntervals, true);
          virtualEnvironment.makeReservation(agvID, currentPlan, currentTime, currentTime + setting.getEvaporationDuration());
          nextRefreshTime = currentTime + setting.getRefreshDuration();
        } else {
          if (currentTime == nextRefreshTime) {
            virtualEnvironment.makeReservation(agvID, currentPlan, currentTime, currentTime + setting.getEvaporationDuration());
            nextRefreshTime = currentTime + setting.getRefreshDuration();
          }
        }
        return;
      }
    }
    
    final boolean refreshSuccess;
    if (currentTime == nextRefreshTime) {
      // refresh, if refresh is not success then explore (by setting the
      // nextExplorationTime as currentTime)
      refreshSuccess = refresh(currentTime);
      if (!refreshSuccess) {
        nextExplorationTime = currentTime;
      }
    } else {
      refreshSuccess = true;
    }
    
    if (currentTime == nextExplorationTime) {
      nextExplorationTime = currentTime + setting.getExplorationDuration();
      isGoingToExplore = true;
    }
    
    // if explore
    if (isGoingToExplore
        && roundedPos.equals(checkPoints.getFirst().getPoint())) {
      // get the next check point
      final CheckPoint nextCheckPoint = checkPoints.getFirst();
      // get the next point of the path, we will explore from this point
      final Point startPoint = path.peek();
      if (startPoint.equals(nextCheckPoint.getPoint())) {
        // if the next check point is a node, then just explore
        // TODO change to current time
        final long startTime = nextCheckPoint.getExpectedTime();
        boolean changePlan = explore(startTime, startPoint, setting.getNumOfAlterRoutes(), true, refreshSuccess);
        if (changePlan) {
          virtualEnvironment.makeReservation(agvID, currentPlan, currentTime, currentTime + setting.getEvaporationDuration());
          nextRefreshTime = currentTime + setting.getRefreshDuration();
        }
      } else {
        if (!startPoint.equals(checkPoints.get(1).getPoint())) {
          // this one cannot happen
          throw new Error("Some problems here!");
        }
        // if the check point is on an edge, then start explore from the end
        // node of that edge
        final Point lastNode = currentPlan.getPath().get(0);
        final Range<Long> nodeInterval = currentPlan.getIntervals().get(0);
        final Range<Long> edgeInterval = currentPlan.getIntervals().get(1);
        final long startTime = nextCheckPoint.getExpectedTime()
            + ((long) ((setting.getVehicleLength() + 0.1) * 1000
                / setting.getVehicleSpeed()));
        boolean changePlan = explore(startTime, checkPoints.get(1).getPoint(),
            setting.getNumOfAlterRoutes(), true, refreshSuccess);
        if (changePlan) {
          // if the AGV change the plan, add the check point on the edge to
          // the checkpoint list
          checkPoints.addFirst(nextCheckPoint);
          currentPlan.addLastNode(lastNode, nodeInterval, edgeInterval);
          virtualEnvironment.makeReservation(agvID, currentPlan, currentTime, currentTime + setting.getEvaporationDuration());
          nextRefreshTime = currentTime + setting.getRefreshDuration();
        }
      }
    }

    // IDEA: check the position. If right before it leaves an edge or a node,
    // it still have to wait, then consume time
    if (!checkPoints.isEmpty()
        && roundedPos.equals(checkPoints.getFirst().getPoint())) {
      // System.out.println(roadModel.get().getPosition(this));
      // sim.stop();
      if (currentTime < checkPoints.getFirst().getExpectedTime()) {
        final long timeDifference = checkPoints.getFirst().getExpectedTime()
            - currentTime;
        if (timeDifference < timeLapse.getTimeLeft()) {
          timeLapse.consume(timeDifference);
          checkPoints.removeFirst();
        } else if (timeDifference == timeLapse.getTimeLeft()) {
          timeLapse.consume(timeDifference);
          checkPoints.removeFirst();
          if (currentPos.x % 1 == 0 && currentPos.y % 1 == 0) {
            path.remove();
          }
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
      result.updateResult(startTime, timeLapse.getTime());
      sim.unregister(this);
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
   * @param refreshSuccess true if the last refresh is successful
   * @return true, if change the current plan
   */
  public boolean explore(long startTime, Point startNode, int numberOfRoutes,
      boolean started, boolean refreshSuccess) {
    List<Point> dest = new ArrayList<>();
    dest.add(destination);
    Plan plan = virtualEnvironment.exploreRoute(agvID, startTime, startNode,
        dest, numberOfRoutes, started);

    if (started && plan == null) {
      return false;
    }

    if (!refreshSuccess || expectedArrivalTime - plan.getArrivalTime() > setting
        .getSwitchingThreshold()) {
      executablePlan = new ExecutablePlan(plan, setting);
      currentPlan = plan;
      path = new LinkedList<>(executablePlan.getPath());
      checkPoints = new LinkedList<>(executablePlan.getCheckPoints());
      expectedArrivalTime = plan.getArrivalTime();
      // virtualEnvironment.makeReservation(agvID, plan, startTime, startTime +
      // setting.getEvaporationDuration());
      // nextRefreshTime = startTime + setting.getRefreshDuration();
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
  public boolean refresh(long currentTime) {
    final boolean success = virtualEnvironment.refreshReservation(agvID,
        currentPlan, currentTime,
        currentTime + setting.getEvaporationDuration());
    nextRefreshTime = currentTime + setting.getRefreshDuration();
    return success;
  }

  @Override
  public void afterTick(TimeLapse timeLapse) {}

}