package multistage.garagemodel.delegatemas;

import java.util.ArrayList;
import java.util.List;

import com.github.rinde.rinsim.geom.Graph;
import com.github.rinde.rinsim.geom.Graphs;
import com.github.rinde.rinsim.geom.LengthData;
import com.github.rinde.rinsim.geom.ListenableGraph;
import com.github.rinde.rinsim.geom.Point;

public class PathSampling {
  
  public PathSampling() {
  }
  
  @SuppressWarnings("unchecked")
  public List<Path> getFeasiblePaths(Point origin, List<Point> destinations,
      int numOfPaths, List<Point> garages, ListenableGraph<?> graph) {
    // a clone graph
//    ListenableGraph<?> graph = (new GraphCreator(setting)).createGraph();
    
//    for (int i = 0; i < garages.size(); i++) {
//      if (!garages.get(i).equals(destinations.get(destinations.size() - 1))) {
//        graph.removeNode(garages.get(i));
//      }
//    }
    
    List<Path> paths = new ArrayList<>();
    double w = -1;
    
    for (int i = 0; i < numOfPaths; i++) {
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

      paths.add(new Path(candidatePath));
      
      if (w == -1) {
//        w = 2 * Graphs.pathLength(candidatePath);
        w = 2 * 8;
      }
      
      final double deltaW = Math.pow(0.5, i) * w;
      
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
