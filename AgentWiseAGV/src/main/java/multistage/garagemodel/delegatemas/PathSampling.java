package multistage.garagemodel.delegatemas;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.random.RandomGenerator;

import com.github.rinde.rinsim.geom.Graph;
import com.github.rinde.rinsim.geom.Graphs;
import com.github.rinde.rinsim.geom.LengthData;
import com.github.rinde.rinsim.geom.ListenableGraph;
import com.github.rinde.rinsim.geom.Point;

import multistage.garagemodel.GraphCreator;
import setting.Setting;

public class PathSampling {
  
  private Setting setting;
  
  private RandomGenerator randomGenerator;
  
  public PathSampling(Setting setting, RandomGenerator randomGenerator) {
    this.setting = setting;
    this.randomGenerator = randomGenerator;
  }
  
  @SuppressWarnings("unchecked")
  public List<Path> getFeasiblePaths(Point origin, List<Point> destinations,
      int numOfPaths, List<Point> garages) {
    
    final double threshold = randomGenerator.nextDouble();
    
    final List<Path> paths = new ArrayList<>();

    // if origin is also the last destination
    if (destinations.size() == 1 && origin.equals(destinations.get(0))) {
      List<Point> theOnlyPath = new ArrayList<>();
      theOnlyPath.add(origin);
      paths.add(new Path(theOnlyPath));
      return paths;
    }
    
    // a clone graph
    final ListenableGraph<?> graph = (new GraphCreator(setting)).createGraph();
    
    // if origin is the entrance of the garage
    if (destinations.size() == 1) {
      final List<Point> theOnlyPath = Graphs.shortestPathEuclideanDistance(graph,
          origin, destinations.get(0));
      if (theOnlyPath.size() == 2) {
        paths.add(new Path(theOnlyPath));
        return paths;
      }
    }
    
    // if the origin is the entrance of the garage and the last destination
    // (beside the garage) is also the entrance of the garage
    if (destinations.size() == 2 && origin.equals(destinations.get(0))) {
      final List<Point> theOnlyPath = Graphs.shortestPathEuclideanDistance(graph,
          origin, destinations.get(1));
      if (theOnlyPath.size() == 2) {
        paths.add(new Path(theOnlyPath));
        return paths;
      }
    }

    for (int i = 0; i < garages.size(); i++) {
      if (!garages.get(i).equals(destinations.get(destinations.size() - 1))) {
        graph.removeNode(garages.get(i));
      }
    }
    
    int count = 0;
    
    while (paths.size() < numOfPaths && count < 1000) {
      count++;
      List<Point> candidatePath = new ArrayList<>();

      for (int dest = 0; dest < destinations.size(); dest++) {
        final List<Point> path;
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
      
      final double deltaW = 1000;
      
      for (int pathIndex = 0; pathIndex < candidatePath.size()
          - 1; pathIndex++) {
        if (randomGenerator.nextDouble() < threshold) {
          final double currentLength = graph
              .getConnection(candidatePath.get(pathIndex),
                  candidatePath.get(pathIndex + 1))
              .getLength();
          ((Graph<LengthData>) graph).setConnectionData(
              candidatePath.get(pathIndex), candidatePath.get(pathIndex + 1),
              LengthData.create(currentLength + deltaW));
        }
      }
    }
    
    return paths;
  }
}