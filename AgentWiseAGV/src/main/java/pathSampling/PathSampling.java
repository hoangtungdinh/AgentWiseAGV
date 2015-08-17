package pathSampling;

import java.util.ArrayList;
import java.util.List;

import com.github.rinde.rinsim.geom.Graph;
import com.github.rinde.rinsim.geom.Graphs;
import com.github.rinde.rinsim.geom.LengthData;
import com.github.rinde.rinsim.geom.ListenableGraph;
import com.github.rinde.rinsim.geom.Point;

import dmasForRouting.AGVSystem.GraphCreator;

public class PathSampling {
  
  public PathSampling() {}
  
  @SuppressWarnings("unchecked")
  public static List<Path> getPaths(Point origin, Point destination, int numOfPaths) {
    // a clone graph
    ListenableGraph<?> graph = GraphCreator.createSimpleGraph();
    
    List<Path> paths = new ArrayList<>();
    double w = -1;
    
    for (int i = 0; i < numOfPaths; i++) {
      final List<Point> candidatePath = Graphs.shortestPathEuclideanDistance(graph, origin, destination);
      
      paths.add(new Path(candidatePath));
      
      if (w == -1) {
        w = 2 * Graphs.pathLength(candidatePath);
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
