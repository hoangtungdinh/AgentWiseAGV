package singlestage;

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

    final Table<Integer, Integer, Point> matrix = createMatrix(5, 5,
        new Point(0, 0));

    for (final Map<Integer, Point> row : matrix.rowMap().values()) {
      Graphs.addBiPath(g, row.values());
    }

    for (final Map<Integer, Point> col : matrix.columnMap().values()) {
      Graphs.addBiPath(g, col.values());
    }

    return new ListenableGraph<>(g);
  }
}
