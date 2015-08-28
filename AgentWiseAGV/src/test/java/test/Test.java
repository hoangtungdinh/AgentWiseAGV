package test;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.github.rinde.rinsim.geom.Point;

public class Test {

  public static void main(String[] args) {
    Map<Point, Double> unsortMap = new HashMap<Point, Double>();
    unsortMap.put(new Point(0d, 0d), 10d);
    unsortMap.put(new Point(0d, 1d), 1d);
    unsortMap.put(new Point(0d, 2d), 1d);
    unsortMap.put(new Point(0d, 3d), 3d);
    unsortMap.put(new Point(0d, 4d), 5d);

    System.out.println("Unsort Map......");
    printMap(unsortMap);

    System.out.println("\nSorted Map......");
    Map<Point, Double> sortedMap = sortByComparator(unsortMap);
    printMap(sortedMap);
  }
  
  public static Map<Point, Double> sortByComparator(
      Map<Point, Double> unsortMap) {

    // Convert Map to List
    List<Map.Entry<Point, Double>> list = new LinkedList<Map.Entry<Point, Double>>(
        unsortMap.entrySet());

    // Sort list with comparator, to compare the Map values
    Collections.sort(list, new Comparator<Map.Entry<Point, Double>>() {
      public int compare(Map.Entry<Point, Double> o1,
          Map.Entry<Point, Double> o2) {
        return (o1.getValue()).compareTo(o2.getValue());
      }
    });

    // Convert sorted map back to a Map
    Map<Point, Double> sortedMap = new LinkedHashMap<Point, Double>();
    for (Iterator<Map.Entry<Point, Double>> it = list.iterator(); it
        .hasNext();) {
      Map.Entry<Point, Double> entry = it.next();
      sortedMap.put(entry.getKey(), entry.getValue());
    }
    return sortedMap;
  }
  
  public static void printMap(Map<Point, Double> map) {
    for (Map.Entry<Point, Double> entry : map.entrySet()) {
      System.out.println("[Key] : " + entry.getKey() 
                                      + " [Value] : " + entry.getValue());
    }
  }
}
