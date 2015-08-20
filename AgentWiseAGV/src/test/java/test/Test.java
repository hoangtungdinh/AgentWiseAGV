package test;

import java.util.ArrayList;
import java.util.List;

public class Test {

  public static void main(String[] args) {
    List<Integer> testList = new ArrayList<>();
    testList.add(1);
    testList.add(2);
    testList.add(3);
    testList.add(4);
    
    List<Integer> newList = new ArrayList<>(testList);
    newList.add(5);
    newList.add(6);
    
    System.out.println(testList);
    System.out.println(newList);
    System.out.println(Math.round(12.70000000001 * 10) / 10d);
  }
}
