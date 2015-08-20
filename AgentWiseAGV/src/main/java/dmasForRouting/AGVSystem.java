package dmasForRouting;

import java.util.List;
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
import destinationGenerator.DestinationList;
import virtualEnvironment.VirtualEnvironment;

public final class AGVSystem {

  public static final double VEHICLE_LENGTH = 2d;
  public static final double VEHICLE_SPEED = 1d;
  public static final int NUM_AGVS = 1;
  public static final long TEST_END_TIME = 10 * 60 * 1000L;
  public static final int TEST_SPEED_UP = 16;
  public static final int NUM_DESTS = 1;

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
    
    VirtualEnvironment virtualEnvironment = new VirtualEnvironment(roadModel, sim.getRandomGenerator());
    
//    Plan plan = virtualEnvironment.exploreRoute(1, 2000, new Point(0d, 0d), new Point(0d, 24d), 3);
//    virtualEnvironment.makeReservation(1, plan, 10);
//    ExecutablePlan executablePlan = new ExecutablePlan(plan);
//    
//    try {
//      PrintWriter printWriter = new PrintWriter("testResult.txt");
//      
//      printWriter.println(plan.getPath());
//      
//      for (Range<Long> range : plan.getIntervals()) {
//        printWriter.println(range);
//      }
//      
//      printWriter.println();
//      
//      for (CheckPoint checkPoint : executablePlan.getCheckPoints()) {
//        printWriter.println(checkPoint.getPoint() + " " + checkPoint.getExpectedTime());
//      }
//
//      printWriter.close();
//    } catch (FileNotFoundException e) {
//      e.printStackTrace();
//    }
     
    // generate destinations for all AGVs
    final DestinationGenerator destinationGenerator = new DestinationGenerator(
        sim.getRandomGenerator(), roadModel, NUM_AGVS,
        NUM_DESTS);
    
    List<DestinationList> destinationLists = destinationGenerator.run();

    for (int i = 0; i < NUM_AGVS; i++) {
      sim.register(new VehicleAgent(sim.getRandomGenerator(),
          destinationLists.get(i).getDestinationList(), virtualEnvironment,
          i + 1, sim));
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
          new Point(0, 0));

//      for (int i = 0; i < matrix.columnMap().size(); i++) {
//
//        Iterable<Point> path;
//        if (i % 2 == 0) {
//          path = Lists.reverse(newArrayList(matrix.column(i).values()));
//        } else {
//          path = matrix.column(i).values();
//        }
//        Graphs.addBiPath(g, path);
//      }
//
//      Graphs.addBiPath(g, matrix.row(0).values());
//      Graphs.addBiPath(g, Lists.reverse(newArrayList(matrix.row(
//          matrix.rowKeySet().size() - 1).values())));
      
      for (final Map<Integer, Point> column : matrix.columnMap().values()) {
        Graphs.addBiPath(g, column.values());
      }
      
      for (final Map<Integer, Point> row : matrix.rowMap().values()) {
        Graphs.addBiPath(g, row.values());
      }

      return new ListenableGraph<>(g);
    }

    static ListenableGraph<LengthData> createGraph() {
      final Graph<LengthData> g = new TableGraph<>();

      final Table<Integer, Integer, Point> leftMatrix = createMatrix(5, 10,
          new Point(0, 0));
      for (final Map<Integer, Point> column : leftMatrix.columnMap().values()) {
        Graphs.addBiPath(g, column.values());
      }
      Graphs.addBiPath(g, leftMatrix.row(LEFT_CENTER_U_ROW).values());
      Graphs.addBiPath(g, leftMatrix.row(LEFT_CENTER_L_ROW).values());

      final Table<Integer, Integer, Point> rightMatrix = createMatrix(10, 7,
          new Point(30, 6));
      for (final Map<Integer, Point> row : rightMatrix.rowMap().values()) {
        Graphs.addBiPath(g, row.values());
      }
      Graphs.addBiPath(g, rightMatrix.column(0).values());
      Graphs.addBiPath(g, rightMatrix.column(rightMatrix.columnKeySet().size()
          - 1).values());

      Graphs.addPath(g,
          rightMatrix.get(RIGHT_CENTER_U_ROW, RIGHT_COL),
          leftMatrix.get(LEFT_CENTER_U_ROW, LEFT_COL));
      Graphs.addPath(g,
          leftMatrix.get(LEFT_CENTER_L_ROW, LEFT_COL),
          rightMatrix.get(RIGHT_CENTER_L_ROW, RIGHT_COL));

      return new ListenableGraph<>(g);
    }
  }
}
