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

import routeplan.CheckPoint;
import routeplan.ExecutablePlan;
import routeplan.Plan;
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
    
    if (timeLapse.getStartTime() == startTime) {
      roadModel.get().addObjectAt(this, origin);
    }
    
    if (!roadModel.get().containsObject(this)) {

      if (timeLapse.getStartTime() == nextExplorationTime) {
        explore(timeLapse.getEndTime(), origin, setting.getNumOfAlterRoutes(), false);
        startTime = checkPoints.getFirst().getExpectedTime();
      }

      if (timeLapse.getStartTime() == nextRefreshTime) {
        refresh(timeLapse.getStartTime());
      }
      return;
    }
    
    // if explore
    if (timeLapse.getStartTime() == nextExplorationTime) {
      // get the next check point
      final CheckPoint nextCheckPoint = checkPoints.getFirst();
      // get the next point of the path, we will explore from this point
      final Point startPoint = path.peek();
      if (startPoint.equals(nextCheckPoint.getPoint())) {
        // if the next check point is a node, then just explore
        explore(nextCheckPoint.getExpectedTime(), startPoint,
            setting.getNumOfAlterRoutes(), true);
      } else {
        if (!startPoint.equals(checkPoints.get(1).getPoint())) {
          // this one cannot happen
          throw new Error("Some problems here!");
        }
        // if the check point is on an edge, then start explore from the end
        // node of that edge
        boolean changePlan = explore(checkPoints.get(1).getExpectedTime(),
            checkPoints.get(1).getPoint(), setting.getNumOfAlterRoutes(), true);
        if (changePlan) {
          // if the AGV change the plan, add the check point on the edge to
          // the checkpoint list
          checkPoints.addFirst(nextCheckPoint);
        }
      }
    }

    if (timeLapse.getStartTime() == nextRefreshTime) {
      refresh(timeLapse.getStartTime());
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
   * @return true, if change the current plan
   */
  public boolean explore(long startTime, Point startNode, int numberOfRoutes, boolean started) {
    nextExplorationTime = startTime + setting.getEvaporationDuration();
    List<Point> dest = new ArrayList<>();
    dest.add(destination);
    Plan plan = virtualEnvironment.exploreRoute(agvID, startTime, startNode, dest, numberOfRoutes, started);
    if (expectedArrivalTime - plan.getArrivalTime() > setting.getSwitchingThreshold()) {
      executablePlan = new ExecutablePlan(plan, setting);
      currentPlan = plan;
      path = new LinkedList<>(executablePlan.getPath());
      checkPoints = new LinkedList<>(executablePlan.getCheckPoints());
      virtualEnvironment.makeReservation(agvID, plan, startTime, startTime + setting.getEvaporationDuration());
      expectedArrivalTime = plan.getArrivalTime();
      nextRefreshTime = startTime + setting.getRefreshDuration();
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

  @Override
  public void afterTick(TimeLapse timeLapse) {}

}