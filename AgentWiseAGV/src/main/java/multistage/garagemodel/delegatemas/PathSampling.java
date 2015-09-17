package multistage.garagemodel.delegatemas;

import java.util.ArrayList;
import java.util.List;

import com.github.rinde.rinsim.geom.Graph;
import com.github.rinde.rinsim.geom.Graphs;
import com.github.rinde.rinsim.geom.LengthData;
import com.github.rinde.rinsim.geom.ListenableGraph;
import com.github.rinde.rinsim.geom.Point;

import multistage.garagemodel.GraphCreator;
import setting.Setting;

public class PathSampling {
  
  private Setting setting;
  
  public PathSampling(Setting setting) {
    this.setting = setting;
  }
  
  @SuppressWarnings("unchecked")
  public List<Path> getFeasiblePaths(Point origin, List<Point> destinations,
      int numOfPaths, List<Point> garages) {
    
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
    
    // if origin is the entrace to the garage
    if (destinations.size() == 1) {
      final List<Point> theOnlyPath = Graphs.shortestPathEuclideanDistance(graph,
          origin, destinations.get(0));
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
    
    while (paths.size() < numOfPaths) {
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
      
      final double deltaW = 8;
      
      for (int pathIndex = 0; pathIndex < candidatePath.size()
          - 1; pathIndex++) {
        final double currentLength = graph
            .getConnection(candidatePath.get(pathIndex),
                candidatePath.get(pathIndex + 1))
            .getLength();
        ((Graph<LengthData>) graph).setConnectionData(
            candidatePath.get(pathIndex), candidatePath.get(pathIndex + 1),
            LengthData.create(currentLength + deltaW));
      }
    }
    
    return paths;
  }
}
