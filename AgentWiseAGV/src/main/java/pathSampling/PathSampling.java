package pathSampling;

import java.util.ArrayList;
import java.util.List;

import com.github.rinde.rinsim.geom.Graph;
import com.github.rinde.rinsim.geom.Graphs;
import com.github.rinde.rinsim.geom.LengthData;
import com.github.rinde.rinsim.geom.ListenableGraph;
import com.github.rinde.rinsim.geom.Point;

import dmasForRouting.AGVSystem.GraphCreator;
import vehicleAgent.State;

public class PathSampling {
  
  private PathSampling() {}
  
  @SuppressWarnings("unchecked")
  public static List<Path> getFeasiblePaths(Point origin, Point destination,
      int numOfPaths, List<Point> centralStation, State state) {
    // a clone graph
    ListenableGraph<?> graph = GraphCreator.createSimpleGraph();
    
    for (int i = 1; i < centralStation.size() - 1; i++) {
      graph.removeNode(centralStation.get(i));
    }
    final Point stationExit = centralStation.get(centralStation.size() - 1);
    
    List<Path> paths = new ArrayList<>();
    double w = -1;
    
    for (int i = 0; i < numOfPaths; i++) {
      List<Point> candidatePath;
      if (state == State.ACTIVE) {
        candidatePath = Graphs.shortestPathEuclideanDistance(graph, origin, destination);;
        final List<Point> pathToStation = Graphs.shortestPathEuclideanDistance(graph, destination, stationExit);
        pathToStation.remove(0);
        candidatePath.addAll(pathToStation);
      } else {
        candidatePath = Graphs.shortestPathEuclideanDistance(graph, origin, stationExit);
      }
      
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
