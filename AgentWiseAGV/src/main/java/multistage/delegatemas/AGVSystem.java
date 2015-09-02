package multistage.delegatemas;

import java.util.List;

import javax.measure.unit.SI;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.road.CollisionGraphRoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModelBuilders;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.ui.View;
import com.github.rinde.rinsim.ui.renderers.AGVRenderer2;
import com.github.rinde.rinsim.ui.renderers.WarehouseRenderer;

import multistage.GraphCreator;
import multistage.destinationgenerator.DestinationGenerator;
import multistage.destinationgenerator.Destinations;
import multistage.result.Result;
import setting.Setting;

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
//        .withAutoPlay()
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

    viewBuilder = viewBuilder.withTitleAppendix("Delegate MAS Multi Stage");
    
    final GraphCreator graph = new GraphCreator(setting);
    
    final Simulator sim = Simulator.builder()
        .addModel(
            RoadModelBuilders.dynamicGraph(graph.createGraph())
                .withCollisionAvoidance()
                .withDistanceUnit(SI.METER)
                .withVehicleLength(setting.getVehicleLength())
                .withSpeedUnit(SI.METERS_PER_SECOND)
                .withMinDistance(0d))
        .setTimeUnit(SI.MILLI(SI.SECOND))
        .setTickLength(100)
        .addModel(viewBuilder)
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
    
    List<Point> centralStation = graph.getCentralStation();
    
    // generate destinations for all AGVs
    final DestinationGenerator destinationGenerator = new DestinationGenerator(
        sim.getRandomGenerator(), roadModel, setting.getNumOfAGVs(),
        setting.getNumOfDestinations(), graph.getCentralStation());
    
    Destinations destinations = destinationGenerator.run();
    
    Result result = new Result(setting, sim);

    for (int i = 0; i < setting.getNumOfAGVs(); i++) {
      sim.register(new VehicleAgent(destinations, virtualEnvironment, i,
          centralStation, setting, result));
    }

    sim.start();
    
    return result;
  }
}
