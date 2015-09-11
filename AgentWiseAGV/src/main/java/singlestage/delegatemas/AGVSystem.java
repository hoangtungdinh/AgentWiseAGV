package singlestage.delegatemas;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.measure.unit.SI;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.road.CollisionGraphRoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModelBuilders;
import com.github.rinde.rinsim.ui.View;
import com.github.rinde.rinsim.ui.renderers.AGVRenderer;
import com.github.rinde.rinsim.ui.renderers.WarehouseRenderer;

import setting.Setting;
import singlestage.GraphCreator;
import singlestage.destinationgenerator.DestinationGenerator;
import singlestage.destinationgenerator.OriginDestination;
import singlestage.result.Result;

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
    final Simulator sim;
    
    if (visualization) {
      View.Builder viewBuilder = View.builder()
//          .withAutoPlay()
          .withSpeedUp(setting.getSpeedUp())
          .withAutoClose()
          .with(WarehouseRenderer.builder()
              .withMargin(setting.getVehicleLength())
              .withNodes()
              .withNodeOccupancy())
          .with(AGVRenderer.builder()
              .withDifferentColorsForVehicles()
              .withVehicleCreationNumber()
              .withVehicleOrigin());

      viewBuilder = viewBuilder.withTitleAppendix("Delegate MAS Single Stage");
      
      sim = Simulator.builder()
          .addModel(
              RoadModelBuilders.dynamicGraph((new GraphCreator(setting)).createGraph())
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
              RoadModelBuilders.dynamicGraph((new GraphCreator(setting)).createGraph())
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
    
    final Map<Integer, VehicleAgent> vehicleAgents = new HashMap<>();
    
    VirtualEnvironment virtualEnvironment = new VirtualEnvironment(roadModel,
        sim.getRandomGenerator(), setting);
    sim.addTickListener(virtualEnvironment);
    
    // generate destinations for all AGVs
    final DestinationGenerator destinationGenerator = new DestinationGenerator(
        sim.getRandomGenerator(), roadModel, setting.getNumOfAGVs());
    
    List<OriginDestination> odList = destinationGenerator.run();
    
    Result result = new Result(setting, sim);

    for (int i = 0; i < setting.getNumOfAGVs(); i++) {
      final VehicleAgent vehicleAgent = new VehicleAgent(odList.get(i), virtualEnvironment, i, sim,
          setting, result);
      sim.register(vehicleAgent);
      vehicleAgents.put(i, vehicleAgent);
    }

    sim.start();
    
    return result;
  }
}
