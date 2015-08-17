package test;

import java.util.List;

import com.github.rinde.rinsim.geom.Point;

import pathSampling.Path;
import pathSampling.PathSampling;

public class Test {

  public static void main(String[] args) {
    List<Path> paths = PathSampling.getPaths(new Point(0d, 0d), new Point(792d, 792d), 10);
    for (Path path : paths) {
      System.out.println(path.getPath());
    }
  }
}
