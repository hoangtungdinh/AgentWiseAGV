package multistage;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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

  private Setting setting;
  
  private LinkedList<Point> centralStation;
  
  public GraphCreator(Setting setting) {
    this.setting = setting;
  }
  
  public ImmutableTable<Integer, Integer, Point> createMatrix(int cols,
      int rows, Point offset) {
    final ImmutableTable.Builder<Integer, Integer, Point> builder = ImmutableTable
        .builder();
    for (int c = 0; c < cols; c++) {
      for (int r = 0; r < rows; r++) {
        builder.put(r, c,
            new Point(offset.x + c * setting.getVehicleLength() * 4,
                offset.y + r * setting.getVehicleLength() * 4));
      }
    }
    return builder.build();
  }

  public ListenableGraph<LengthData> createGraph() {
    final Graph<LengthData> g = new TableGraph<>();
    
    int numOfNodes = -1;
    
    if (setting.getNumOfAGVs() % 2 == 0) {
      numOfNodes = setting.getNumOfAGVs()  / 2;
    } else {
      numOfNodes = (setting.getNumOfAGVs() + 1) / 2;
    }
    
    Point origin = new Point(numOfNodes * 8, 0);

    final Table<Integer, Integer, Point> matrix = createMatrix(4, 4, origin);

    centralStation = new LinkedList<>();
    final Point stationEntrace = new Point(origin.x - 8, 8);
    final Point stationExit = new Point(origin.x - 8, 0);
    for (int i = 0; i < numOfNodes; i++) {
      centralStation
          .addLast(new Point(origin.x - 8*(i + 1), 8));
    }
    for (int i = 0; i < numOfNodes; i++) {
      centralStation
          .addLast(new Point(8*i, 0));
    }

    for (int i = 0; i < matrix.columnMap().size(); i++) {
      Graphs.addBiPath(g, matrix.column(i).values());
    }

    for (final Map<Integer, Point> row : matrix.rowMap().values()) {
      Graphs.addBiPath(g, row.values());
    }

    Graphs.addPath(g, centralStation);
    Graphs.addPath(g, new Point(numOfNodes * 8, 8), stationEntrace);
    Graphs.addPath(g, stationExit, new Point(numOfNodes * 8, 0));

    return new ListenableGraph<>(g);
  }
  
  /**
   * Gets the central station.
   *
   * @return all nodes in the central station
   */
  public List<Point> getCentralStation() {
    return centralStation;
  }
}
