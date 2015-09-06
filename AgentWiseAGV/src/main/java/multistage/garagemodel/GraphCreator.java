package multistage.garagemodel;

import java.util.ArrayList;
import java.util.List;

import com.github.rinde.rinsim.geom.Graph;
import com.github.rinde.rinsim.geom.Graphs;
import com.github.rinde.rinsim.geom.LengthData;
import com.github.rinde.rinsim.geom.ListenableGraph;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.geom.TableGraph;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;

import setting.Setting;

public class GraphCreator {

  private double edgeLength;
  
  private List<Point> garages;
  
  public GraphCreator(Setting setting) {
    edgeLength = setting.getVehicleLength() * 4;
    garages = new ArrayList<Point>();
  }
  
  public ImmutableTable<Integer, Integer, Point> createMatrix(int cols,
      int rows, Point offset) {
    final ImmutableTable.Builder<Integer, Integer, Point> builder = ImmutableTable
        .builder();
    for (int c = 0; c < cols; c++) {
      for (int r = 0; r < rows; r++) {
        builder.put(r, c,
            new Point(offset.x + c * edgeLength,
                offset.y + r * edgeLength));
      }
    }
    return builder.build();
  }

  public ListenableGraph<LengthData> createGraph() {
    final int size = 7; // mean that the real map is 5
    
    final Graph<LengthData> g = new TableGraph<>();

    final Table<Integer, Integer, Point> matrix = createMatrix(size, size,
        new Point(0, 0));

    for (int i = 0; i < matrix.rowKeySet().size(); i++) {
      if (i != 0 && i != size - 1) {
        Graphs.addBiPath(g, matrix.rowMap().get(i).values());
      }
    }
    
    for (int i = 0; i < matrix.columnKeySet().size(); i++) {
      if (i != 0 && i != size - 1) {
        Graphs.addBiPath(g, matrix.columnMap().get(i).values());
      }
    }
    
    double mapEdgeLength = (double) ((size - 1) * edgeLength);
    g.removeNode(new Point(0d, 0d));
    g.removeNode(new Point(0d, mapEdgeLength));
    g.removeNode(new Point(mapEdgeLength, 0d));
    g.removeNode(new Point(mapEdgeLength, mapEdgeLength));
    
    for (Point p : g.getNodes()) {
      if (p.x == 0 || p.y == 0 || p.x == (size - 1) * edgeLength
          || p.y == (size - 1) * edgeLength) {
        garages.add(p);
      }
    }
    
    System.out.println(garages.size());

    return new ListenableGraph<>(g);
  }

  public List<Point> getGarages() {
    return garages;
  }
}
