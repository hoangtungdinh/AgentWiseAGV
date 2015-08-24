package destinationGenerator;

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

  /** The number of destinations for each AGVs. */
  private int numOfDesForEachAGV;
  
  /** The road model. */
  private CollisionGraphRoadModel roadModel;

  public DestinationGenerator(RandomGenerator randomGenerator,
      CollisionGraphRoadModel roadModel, int numberOfAGVs,
      int numOfDesForEachAGV) {
    this.randomGenerator = randomGenerator;
    this.roadModel = roadModel;
    this.numberOfAGVs = numberOfAGVs;
    this.numOfDesForEachAGV = numOfDesForEachAGV;
  }
  
  /**
   * Generate all the destinations for each AGV
   *
   * @return the list of the destinationList of each AGV
   */
  public Destinations run() {
    // List of all destination
    List<Point> destinations = new ArrayList<>();
    
    // total number of destinations
    final int numOfDestinations = numberOfAGVs * numOfDesForEachAGV;
    
    for (int i = 0; i < numOfDestinations; i++) {
      if (i == 0) {
        // generate the first destination
        Point nextDes = roadModel.getRandomPosition(randomGenerator);
        destinations.add(nextDes);
      } else {
        Point nextDes = roadModel.getRandomPosition(randomGenerator);
        // make sure that two consecutive destinations have to be different
        while (nextDes == destinations.get(i - 1)) {
          nextDes = roadModel.getRandomPosition(randomGenerator);
        }
        destinations.add(nextDes);
      }
    }
    
    return new Destinations(destinations);
  }
}
