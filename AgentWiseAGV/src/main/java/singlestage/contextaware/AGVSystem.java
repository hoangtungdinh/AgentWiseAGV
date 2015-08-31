package singlestage.contextaware;

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
import com.github.rinde.rinsim.ui.renderers.AGVRenderer2;
import com.github.rinde.rinsim.ui.renderers.WarehouseRenderer;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;

import resultrecording.Result;
import setting.Setting;
import singlestage.destinationgenerator.DestinationGenerator;
import singlestage.destinationgenerator.OriginDestination;

public final class AGVSystem {

  private Setting setting;

  /**
   * Instantiates a new AGV system.
   *
   * @param setting the setting
   */
  public AGVSystem(Setting setting) {
    this.setting = setting;
  }

  /**
   * Run.
   */
  public Result run() {
    View.Builder viewBuilder = View.builder()
        .withAutoPlay()
        .withSpeedUp(setting.getSpeedUp())
        .withAutoClose()
        .with(WarehouseRenderer.builder()
            .withMargin(setting.getVehicleLength())
            .withNodes()
            .withNodeOccupancy())
        .with(AGVRenderer2.builder()
            .withDifferentColorsForVehicles()
            .withVehicleCreationNumber()
            .withVehicleOrigin());

    viewBuilder = viewBuilder.withTitleAppendix("Warehouse Example");
    
    final Simulator sim = Simulator.builder()
        .addModel(
            RoadModelBuilders.dynamicGraph(createSimpleGraph())
                .withCollisionAvoidance()
                .withDistanceUnit(SI.METER)
                .withVehicleLength(setting.getVehicleLength())
                .withSpeedUnit(SI.METERS_PER_SECOND)
                .withMinDistance(0d))
        .setTimeUnit(SI.MILLI(SI.SECOND))
        .setTickLength(100)
//        .addModel(viewBuilder)
        // add a random seed
        .setRandomSeed(setting.getSeed())
        .build();
    
    CollisionGraphRoadModel roadModel = (CollisionGraphRoadModel) sim.getModelProvider().tryGetModel(RoadModel.class);
    
    // check whether the road model is retrieved successfully
    if (roadModel == null) {
      throw new NullPointerException(
          "Cannot get the road model from the simulator");
    }
    
    VirtualEnvironment virtualEnvironment = new VirtualEnvironment(roadModel,
        sim.getRandomGenerator(), setting);
    sim.addTickListener(virtualEnvironment);
    
    // generate destinations for all AGVs
    final DestinationGenerator destinationGenerator = new DestinationGenerator(
        sim.getRandomGenerator(), roadModel, setting.getNumOfAGVs());
    
    List<OriginDestination> odList = destinationGenerator.run();
    
    Result result = new Result(setting, sim);

    for (int i = 0; i < setting.getNumOfAGVs(); i++) {
      sim.register(new VehicleAgent(odList.get(i), virtualEnvironment, i, sim,
          setting, result));
    }

    sim.start();
    
    return result;
  }

  public ImmutableTable<Integer, Integer, Point> createMatrix(int cols,
      int rows, Point offset) {
    final ImmutableTable.Builder<Integer, Integer, Point> builder = ImmutableTable
        .builder();
    for (int c = 0; c < cols; c++) {
      for (int r = 0; r < rows; r++) {
        builder.put(r, c,
            new Point(offset.x + c * setting.getVehicleLength() * 4,
                offset.y + r * setting.getVehicleLength() * 4));
      }
    }
    return builder.build();
  }

  public ListenableGraph<LengthData> createSimpleGraph() {
    final Graph<LengthData> g = new TableGraph<>();

    final Table<Integer, Integer, Point> matrix = createMatrix(10, 10,
        new Point(0, 0));

    for (final Map<Integer, Point> row : matrix.rowMap().values()) {
      Graphs.addBiPath(g, row.values());
    }

    for (final Map<Integer, Point> col : matrix.columnMap().values()) {
      Graphs.addBiPath(g, col.values());
    }

    return new ListenableGraph<>(g);
  }
}
