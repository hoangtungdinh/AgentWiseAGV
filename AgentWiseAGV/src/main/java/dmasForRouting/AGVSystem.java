package dmasForRouting;

import java.util.Collections;
import java.util.LinkedList;
import java.util.Map;

import javax.measure.unit.SI;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.road.CollisionGraphRoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModelBuilders;
import com.github.rinde.rinsim.geom.Graph;
import com.github.rinde.rinsim.geom.Graphs;
import com.github.rinde.rinsim.geom.LengthData;
import com.github.rinde.rinsim.geom.ListenableGraph;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.geom.TableGraph;
import com.github.rinde.rinsim.ui.View;
import com.github.rinde.rinsim.ui.renderers.AGVRenderer;
import com.github.rinde.rinsim.ui.renderers.WarehouseRenderer;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;

import destinationGenerator.DestinationGenerator;
import destinationGenerator.Destinations;
import vehicleAgent.VehicleAgent;
import virtualEnvironment.VirtualEnvironment;

public final class AGVSystem {

  public static final double VEHICLE_LENGTH = 2d;
  public static final double VEHICLE_SPEED = 1d;
  public static final int NUM_AGVS = 5;
  public static final long TEST_END_TIME = 10 * 60 * 1000L;
  public static final int TEST_SPEED_UP = 16;
  public static final int NUM_DESTS = 100;
  public static final long EVAPORATION_DURATION = 10000;
  public static final long REFRESH_DURATION = 8000;
  public static final long EXPLORATION_DURATION = 5000;
  public static final long SWITCHING_THRESHOLD = 6000;
  public static final int NUM_OF_ROUTES = 30;
  
  private static LinkedList<Point> centralStation;

  private AGVSystem() {}

  /**
   * @param args - No args.
   */
  public static void main(String[] args) {
    run(false);
  }

  /**
   * Runs the example.
   * @param testing If <code>true</code> the example will run in testing mode,
   *          automatically starting and stopping itself such that it can be run
   *          from a unit test.
   */
  public static void run(boolean testing) {
    View.Builder viewBuilder = View.builder()
        .with(WarehouseRenderer.builder()
            .withMargin(VEHICLE_LENGTH)
            .withNodes()
            .withNodeOccupancy())
        .with(AGVRenderer.builder()
            .withDifferentColorsForVehicles()
            .withVehicleCreationNumber()
            .withVehicleOrigin());

    if (testing) {
      viewBuilder = viewBuilder.withAutoPlay()
          .withAutoClose()
          .withSimulatorEndTime(TEST_END_TIME)
          .withTitleAppendix("TESTING")
          .withSpeedUp(TEST_SPEED_UP);
    } else {
      viewBuilder = viewBuilder.withTitleAppendix("Warehouse Example");
    }
    
    final Simulator sim = Simulator.builder()
        .addModel(
            RoadModelBuilders.dynamicGraph(GraphCreator.createSimpleGraph())
                .withCollisionAvoidance()
                .withDistanceUnit(SI.METER)
                .withVehicleLength(VEHICLE_LENGTH)
                .withSpeedUnit(SI.METERS_PER_SECOND)
                .withMinDistance(0d))
        .setTimeUnit(SI.MILLI(SI.SECOND))
        .setTickLength(100)
        .addModel(viewBuilder)
        // add a random seed
        .setRandomSeed(0)
        .build();
    
    CollisionGraphRoadModel roadModel = (CollisionGraphRoadModel) sim.getModelProvider().tryGetModel(RoadModel.class);
    
    // check whether the road model is retrieved successfully
    if (roadModel == null) {
      throw new NullPointerException(
          "Cannot get the road model from the simulator");
    }
    
    VirtualEnvironment virtualEnvironment = new VirtualEnvironment(roadModel, sim.getRandomGenerator(), centralStation);
    sim.addTickListener(virtualEnvironment);
    
    // generate destinations for all AGVs
    final DestinationGenerator destinationGenerator = new DestinationGenerator(
        sim.getRandomGenerator(), roadModel, NUM_AGVS,
        NUM_DESTS, centralStation);
    
    Destinations destinations = destinationGenerator.run();

    for (int i = 0; i < NUM_AGVS; i++) {
      sim.register(new VehicleAgent(destinations, virtualEnvironment, i, sim,
          centralStation.get(i), centralStation));
    }

    sim.start();
  }
  
  public static class GraphCreator {
    static final int LEFT_CENTER_U_ROW = 4;
    static final int LEFT_CENTER_L_ROW = 5;
    static final int LEFT_COL = 4;
    static final int RIGHT_CENTER_U_ROW = 2;
    static final int RIGHT_CENTER_L_ROW = 4;
    static final int RIGHT_COL = 0;

    GraphCreator() {}

    static ImmutableTable<Integer, Integer, Point> createMatrix(int cols,
        int rows, Point offset) {
      final ImmutableTable.Builder<Integer, Integer, Point> builder =
          ImmutableTable.builder();
      for (int c = 0; c < cols; c++) {
        for (int r = 0; r < rows; r++) {
          builder.put(r, c, new Point(
              offset.x + c * VEHICLE_LENGTH * 4,
              offset.y + r * VEHICLE_LENGTH * 4));
        }
      }
      return builder.build();
    }

    public static ListenableGraph<LengthData> createSimpleGraph() {
      final Graph<LengthData> g = new TableGraph<>();

      final Table<Integer, Integer, Point> matrix = createMatrix(4, 4,
          new Point(8, 0));
      
      centralStation = new LinkedList<>();
      final Point stationEntrace = new Point(0, 0);
      final Point stationExit = new Point(0, 24);
      centralStation.addLast(stationEntrace);
      for (int i = 0; i < NUM_AGVS - 1; i++) {
        centralStation.addLast(new Point(0, (i + 1) * VEHICLE_LENGTH));
      }
      centralStation.addLast(stationExit);
      Collections.reverse(centralStation);
      
      for (final Map<Integer, Point> column : matrix.columnMap().values()) {
        Graphs.addBiPath(g, column.values());
      }

      for (final Map<Integer, Point> row : matrix.rowMap().values()) {
        Graphs.addBiPath(g, row.values());
      }
      
      Graphs.addPath(g, centralStation);
      Graphs.addBiPath(g, stationEntrace, new Point(8, 0));
      Graphs.addBiPath(g, stationExit, new Point(8, 24));
      
      Collections.reverse(centralStation);

      return new ListenableGraph<>(g);
    }
  }
}
