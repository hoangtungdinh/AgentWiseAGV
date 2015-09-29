package test;

import java.util.List;

import com.github.rinde.rinsim.scenario.generator.TimeSeries;
import com.github.rinde.rinsim.scenario.generator.TimeSeries.TimeSeriesGenerator;
import com.google.common.base.Predicate;

public class Dummy {

  public static void main(String[] args) {
    Predicate<List<Double>> pred = TimeSeries.numEventsPredicate(10);
    
    TimeSeriesGenerator timeSeriesGenerator = TimeSeries.filter(TimeSeries.homogenousPoisson(10000, 10), pred);
    List<Double> result1 = timeSeriesGenerator.generate(3);
    List<Double> result2 = timeSeriesGenerator.generate(3);
    System.out.println(result1);
    System.out.println(result2);
  }
}
