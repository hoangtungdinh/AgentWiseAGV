package multistage.garagemodel.contextaware;

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

import multistage.Destinations;
import multistage.garagemodel.GraphCreator;
import multistage.garagemodel.destinationgenerator.DestinationGenerator;
import multistage.result.Result;
import setting.Setting;

public final class AGVSystem {

  private Setting setting;
  
  private boolean visualization;

  /**
   * Instantiates a new AGV system.
   *
   * @param setting the setting
   */
  public AGVSystem(Setting setting, boolean visualization) {
    this.setting = setting;
    this.visualization = visualization;
  }

  /**
   * Run.
   */
  public Result run() {
    final GraphCreator graph = new GraphCreator(setting);
    final Simulator sim;
    
    if (visualization) {
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

    viewBuilder = viewBuilder.withTitleAppendix("Context Aware Multi Stage");
    
    sim = Simulator.builder()
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
    } else {
      sim = Simulator.builder()
          .addModel(
              RoadModelBuilders.dynamicGraph(graph.createGraph())
                  .withCollisionAvoidance()
                  .withDistanceUnit(SI.METER)
                  .withVehicleLength(setting.getVehicleLength())
                  .withSpeedUnit(SI.METERS_PER_SECOND)
                  .withMinDistance(0d))
          .setTimeUnit(SI.MILLI(SI.SECOND))
          .setTickLength(100)
          // add a random seed
          .setRandomSeed(setting.getSeed())
          .build();
    }
    
    CollisionGraphRoadModel roadModel = (CollisionGraphRoadModel) sim.getModelProvider().tryGetModel(RoadModel.class);
    
    // check whether the road model is retrieved successfully
    if (roadModel == null) {
      throw new NullPointerException(
          "Cannot get the road model from the simulator");
    }
    
    VirtualEnvironment virtualEnvironment = new VirtualEnvironment(roadModel,
        sim.getRandomGenerator(), setting);
    sim.addTickListener(virtualEnvironment);
    
    List<Point> garageList = graph.getGarages();
    
    if (setting.getNumOfAGVs() > garageList.size()) {
      throw new IllegalArgumentException("the number of agvs cannot be larger than the number of garages");
    }

    // generate destinations for all AGVs
    final DestinationGenerator destinationGenerator = new DestinationGenerator(
        sim.getRandomGenerator(), roadModel, setting.getNumOfDestinations(),
        garageList);
    
    
    Result result = new Result(setting, sim);

    for (int i = 0; i < setting.getNumOfAGVs(); i++) {
      final Destinations destinations = destinationGenerator.run();
      sim.register(new VehicleAgent(destinations, virtualEnvironment, i,
          garageList, setting, result));
    }

    sim.start();
    
    return result;
  }
}
