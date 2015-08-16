package test;

import com.github.rinde.rinsim.geom.Point;

public class Test {

  public static void main(String[] args) {
    Point p1 = new Point(24d, 24d);
    Point p2 = new Point(24d, 24d);
    System.out.println(p1.equals(p2));
  }
}
