package dmasForRouting;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.apache.commons.math3.random.RandomGenerator;

import com.github.rinde.rinsim.core.model.road.CollisionGraphRoadModel;
import com.github.rinde.rinsim.geom.Graphs;
import com.github.rinde.rinsim.geom.Point;

public class PathSampling {
  
  private CollisionGraphRoadModel roadModel;
  private RandomGenerator randomGenerator;

  public PathSampling(CollisionGraphRoadModel roadModel, RandomGenerator randomGenerator) {
    this.roadModel = roadModel;
    this.randomGenerator = randomGenerator;
  }
  
  public List<Point> getRandomPath(Point origin, Point destination) {
    LinkedList<Point> path = new LinkedList<>();
    
    path.addLast(origin);
    
    Point currentPoint = origin;
    Map<Point, Double> lengthMap = new HashMap<>();
    
    while (!currentPoint.equals(destination)) {
      Collection<Point> nextPoints = roadModel.getGraph().getOutgoingConnections(currentPoint);
      double totalLength = 0;
      
      NavigableMap<Double, Point> navigableMap = new TreeMap<>();
      
      for (Point point : nextPoints) {
//        if (path.contains(point)) {
//          continue;
//        }
        
        if (lengthMap.containsKey(point)) {
          totalLength += lengthMap.get(point);
        } else {
          double length = 100.0 / Graphs.pathLength(roadModel.getShortestPathTo(point, destination));
          totalLength += length;
          lengthMap.put(point, length);
        }
        navigableMap.put(totalLength, point);
      }
      
      if (navigableMap.isEmpty()) {
        return null;
      }
      
//      System.out.println(currentPoint);
//      if (currentPoint.equals(new Point(24d, 24d))) {
//        System.out.println("----------");
//      }
//      System.out.println(navigableMap);
      
      double randomIndex = randomGenerator.nextDouble() * totalLength;
      Point selectedNextPoint = navigableMap.ceilingEntry(randomIndex).getValue();
      path.addLast(selectedNextPoint);
      currentPoint = selectedNextPoint;
    }
    
    return path;
  }
}
