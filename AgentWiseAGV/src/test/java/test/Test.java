package test;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;

public class Test {

  public static void main(String[] args) {
    Range<Long> r1 = Range.open(30L, 100L);
    Range<Long> r2 = Range.open(90L, 200L);
    RangeSet<Long> rangeSet = TreeRangeSet.create();
    rangeSet.add(r1);
    rangeSet.add(r2);
    System.out.println(rangeSet);
    System.out.println(rangeSet.complement());
    if (r1 == r2) {
      System.out.println("true");
    }
    Map<Range<Long>, Integer> rangeMapTest = new HashMap<>();
    rangeMapTest.put(r1, 2);
    rangeMapTest.put(r2, 3);
    Range<Long> r3 = Range.open(30L, 100L);
    System.out.println(rangeMapTest.containsKey(r3));
    Range<Long> r4 = Range.open(0L, 330L);
    System.out.println(rangeSet.subRangeSet(r4));
    System.out.println(r1.lowerEndpoint());
  }
}
