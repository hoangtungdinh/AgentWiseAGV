package singlestage.delegatemas;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.random.RandomGenerator;

import com.github.rinde.rinsim.geom.Graph;
import com.github.rinde.rinsim.geom.Graphs;
import com.github.rinde.rinsim.geom.LengthData;
import com.github.rinde.rinsim.geom.ListenableGraph;
import com.github.rinde.rinsim.geom.Point;

import setting.Setting;
import singlestage.GraphCreator;

public class PathSampling {
  
  private Setting setting;
  
  private RandomGenerator randomGenerator;
  
  public PathSampling(Setting setting, RandomGenerator randomGenerator) {
    this.setting = setting;
    this.randomGenerator = randomGenerator;
  }
  
  @SuppressWarnings("unchecked")
  public List<Path> getFeasiblePaths(Point origin, List<Point> destinations,
      int numOfPaths) {
    
    final double threshold = randomGenerator.nextDouble();
//    if (randomGenerator.nextDouble() < 0.5) {
//      threshold = 0.2;
//    } else {
//      threshold = 0.8;
//    }
    
    final List<Path> paths = new ArrayList<>();
    
    if (destinations.size() == 1 && origin.equals(destinations.get(0))) {
      List<Point> theOnlyPath = new ArrayList<>();
      theOnlyPath.add(origin);
      paths.add(new Path(theOnlyPath));
      return paths;
    }
    
    // a clone graph
    final ListenableGraph<?> graph = (new GraphCreator(setting)).createGraph();
    
    // sometimes, we cannot find enough number of different path. If after 1000
    // runs we still cannot found enough number of paths then return
    int count = 0;
    
    while (paths.size() < numOfPaths && count < 100) {
      count++;
      List<Point> candidatePath = new ArrayList<>();

      for (int dest = 0; dest < destinations.size(); dest++) {
        List<Point> path;
        if (dest == 0) {
          path = Graphs.shortestPathEuclideanDistance(graph, origin,
              destinations.get(0));
          candidatePath.addAll(path);
        } else {
          path = Graphs.shortestPathEuclideanDistance(graph,
              destinations.get(dest - 1), destinations.get(dest));
          path.remove(0);
          candidatePath.addAll(path);
        }
      }

      final Path newPath = new Path(candidatePath);
      
      if (!paths.contains(newPath)) {
        paths.add(newPath);
      }
      
      final double deltaW = 100;
      
      //////////////
//      for (int pathIndex = 0; pathIndex < candidatePath.size()
//          - 1; pathIndex++) {
//        if (randomGenerator.nextDouble() < threshold) {
//          final double currentLength = graph
//              .getConnection(candidatePath.get(pathIndex),
//                  candidatePath.get(pathIndex + 1))
//              .getLength();
//          ((Graph<LengthData>) graph).setConnectionData(
//              candidatePath.get(pathIndex), candidatePath.get(pathIndex + 1),
//              LengthData.create(currentLength + deltaW));
//        }
//      }
      ////////////////
      
      for (int pathIndex = 0; pathIndex < candidatePath.size(); pathIndex++) {
        if (randomGenerator.nextDouble() < threshold) {
          final List<Point> neighboringPoints = new ArrayList<>(
              graph.getIncomingConnections(candidatePath.get(pathIndex)));

          for (Point neighboringPoint : neighboringPoints) {
            ((Graph<LengthData>) graph).setConnectionData(
                candidatePath.get(pathIndex), neighboringPoint,
                LengthData.create(deltaW));
          }
        }
      }
      
    }
    
//    System.out.println(paths.size());
    
    return paths;
  }
}
