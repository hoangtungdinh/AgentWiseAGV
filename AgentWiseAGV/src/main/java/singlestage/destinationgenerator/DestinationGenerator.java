package singlestage.destinationgenerator;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.random.RandomGenerator;

import com.github.rinde.rinsim.core.model.road.CollisionGraphRoadModel;
import com.github.rinde.rinsim.geom.Point;

/**
 * The Class DestinationGenerator.
 *
 * @author Tung
 */
public class DestinationGenerator {

  /** The random generator. */
  private RandomGenerator randomGenerator;

  /** The number of AGVs. */
  private int numberOfAGVs;
  
  /** The road model. */
  private CollisionGraphRoadModel roadModel;

  /**
   * Instantiates a new destination generator.
   *
   * @param randomGenerator the random generator
   * @param roadModel the road model
   * @param numberOfAGVs the number of agvs
   */
  public DestinationGenerator(RandomGenerator randomGenerator,
      CollisionGraphRoadModel roadModel, int numberOfAGVs) {
    this.randomGenerator = randomGenerator;
    this.roadModel = roadModel;
    this.numberOfAGVs = numberOfAGVs;
  }
  
  /**
   * Generate all the destinations for each AGV
   *
   * @return the list of the destinationList of each AGV
   */
  public List<OriginDestination> run() {
    // List of all destination
    List<OriginDestination> odList = new ArrayList<>();
    
    // total number of destinations
    final int numOfDestinations = numberOfAGVs;
    
    for (int i = 0; i < numOfDestinations; i++) {
      Point origin = roadModel.getRandomPosition(randomGenerator);
      Point destination = roadModel.getRandomPosition(randomGenerator);
      while (destination.equals(origin)) {
        destination = roadModel.getRandomPosition(randomGenerator);
      }
      odList.add(new OriginDestination(origin, destination));
    }
    
    return odList;
  }
}
