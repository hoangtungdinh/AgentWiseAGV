package incidentgenerator;

import static com.google.common.base.Preconditions.checkState;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.math3.random.RandomGenerator;

import com.github.rinde.rinsim.scenario.generator.TimeSeries;
import com.github.rinde.rinsim.scenario.generator.TimeSeries.TimeSeriesGenerator;

import setting.Setting;

public class IncidentGenerator {

  private RandomGenerator randomGenerator;

  private Setting setting;

  public IncidentGenerator(RandomGenerator randomGenerator, Setting setting) {
    this.randomGenerator = randomGenerator;
    this.setting = setting;
  }

  public IncidentList run() {
    final TimeSeriesGenerator timeSeriesGenerator = TimeSeries
        .homogenousPoisson((double) setting.getEndTime() / 100,
            setting.getNumOfIncidents());
    final List<Double> startTimesInDouble = timeSeriesGenerator.generate(randomGenerator.nextLong());
    final List<Long> startTimesInLong = toLong(startTimesInDouble);
    final List<Long> durationList = generateIncidentDurations(startTimesInLong);
    checkState(startTimesInLong.size() == durationList.size(), "List of incident start times and list of incident durations must be equal in length!");
    
    final List<Incident> incidentList = new ArrayList<>();
    
    for (int i = 0; i < startTimesInLong.size(); i++) {
      final Incident incident = new Incident(startTimesInLong.get(i)*100, durationList.get(i)*100);
      incidentList.add(incident);
    }

    return new IncidentList(incidentList);
  }
  
  public List<Long> toLong(List<Double> listDouble) {
    final LinkedList<Long> listLong = new LinkedList<>();
    for (double num : listDouble) {
      final long numInLong = (long) num;
      if (listLong.isEmpty()
          || numInLong > listLong.getLast() + 10){
        listLong.add((long) num);
      }
    }
    return listLong;
  }
  
  public List<Long> generateIncidentDurations(List<Long> startTimeList) {
    final List<Long> durationList = new ArrayList<>();
    
    for (int i = 0; i < startTimeList.size() - 1; i++) {
      long duration = (long) randomGenerator
          .nextInt((int) (startTimeList.get(i + 1) - startTimeList.get(i)));
      
      // incident duration must be larger than 0 and smaller than 100000
      while (duration == 0 || duration > 1000) {
        duration = (long) randomGenerator
            .nextInt((int) (startTimeList.get(i + 1) - startTimeList.get(i)));
      }
      
      durationList.add(duration);
    }
    
    long duration = (long) randomGenerator.nextInt(
        (int) ((setting.getEndTime() / 100) / setting.getNumOfIncidents()));
    
    while (duration == 0 || duration > 1000) {
      duration = (long) randomGenerator
          .nextInt(1000);
    }
    
    durationList.add(duration);
    
    return durationList;
  }

}
