package dmasForRouting;

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

  /** The number of destinations. */
  private int numberOfDestinations;
  
  /** The road model. */
  private CollisionGraphRoadModel roadModel;

  /**
   * Instantiates a new destination generator.
   *
   * @param randomGenerator the random generator
   */
  public DestinationGenerator(RandomGenerator randomGenerator,
      CollisionGraphRoadModel roadModel, int numberOfAGVs,
      int numberOfDestinations) {
    this.randomGenerator = randomGenerator;
    this.roadModel = roadModel;
    this.numberOfAGVs = numberOfAGVs;
    this.numberOfDestinations = numberOfDestinations;
  }
  
  /**
   * Generate all the destinations for each AGV
   *
   * @return the list of the destinationList of each AGV
   */
  public List<DestinationList> run() {
    // List of all start points, it is used to guarantee that the initial positions of AGV are not overlapping.
    List<Point> startPoints = new ArrayList<Point>();
    
    // List of destination lists of AGVs
    List<DestinationList> allDestinations = new ArrayList<DestinationList>();
    
    for (int i = 0; i < numberOfAGVs; i++) {
      // list of destination of agv i
      List<Point> destinationList = new ArrayList<Point>();
      
      for (int d = 0; d < numberOfDestinations; d++) {
        // generate a random destination
        Point destination = roadModel.getRandomPosition(randomGenerator);
        
        // if it is the start point, check overlap
        if (d == 0) {
          while (startPoints.contains(destination)) {
            destination = roadModel.getRandomPosition(randomGenerator);
          }
          startPoints.add(destination);
        }
        
        // add the destination to the list
        destinationList.add(destination);
      }
      
      // save the destination list of AGV i
      DestinationList destList = new DestinationList(destinationList);
      allDestinations.add(destList);
    }
    
    return allDestinations;
  }
}
