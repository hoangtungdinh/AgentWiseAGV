package multistage;

import java.util.Collections;
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

    final Table<Integer, Integer, Point> matrix = createMatrix(4, 4,
        new Point(8, 0));

    centralStation = new LinkedList<>();
    final Point stationEntrace = new Point(0, 0);
    final Point stationExit = new Point(0, 24);
    centralStation.addLast(stationEntrace);
    for (int i = 0; i < setting.getNumOfAGVs() - 1; i++) {
      centralStation
          .addLast(new Point(0, (i + 1) * setting.getVehicleLength()));
    }
    centralStation.addLast(stationExit);
    Collections.reverse(centralStation);

    for (int i = 0; i < matrix.columnMap().size(); i++) {

      if (i == 0) {
        Graphs.addBiPath(g, matrix.column(i).values());
      }
    }

    for (final Map<Integer, Point> row : matrix.rowMap().values()) {
      Graphs.addBiPath(g, row.values());
    }

    Graphs.addPath(g, centralStation);
    Graphs.addBiPath(g, stationEntrace, new Point(8, 0));
    Graphs.addBiPath(g, stationExit, new Point(8, 24));

    Collections.reverse(centralStation);

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
