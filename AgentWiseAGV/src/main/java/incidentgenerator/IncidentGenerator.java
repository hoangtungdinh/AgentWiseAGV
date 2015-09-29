package incidentgenerator;

import static com.google.common.base.Preconditions.checkState;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.random.RandomGenerator;

import com.github.rinde.rinsim.scenario.generator.TimeSeries;
import com.github.rinde.rinsim.scenario.generator.TimeSeries.TimeSeriesGenerator;
import com.google.common.base.Predicate;

import setting.Setting;

public class IncidentGenerator {

  private RandomGenerator randomGenerator;

  private Setting setting;

  public IncidentGenerator(RandomGenerator randomGenerator, Setting setting) {
    this.randomGenerator = randomGenerator;
    this.setting = setting;
  }

  public IncidentList run() {
    final Predicate<List<Double>> pred = TimeSeries
        .numEventsPredicate(setting.getNumOfIncidents());
    final TimeSeriesGenerator timeSeriesGenerator = TimeSeries.filter(
        TimeSeries.homogenousPoisson((double) setting.getEndTime() / 100,
            setting.getNumOfIncidents()),
        pred);
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
    final List<Long> listLong = new ArrayList<>();
    for (double num : listDouble) {
      listLong.add((long) num);
    }
    return listLong;
  }
  
  public List<Long> generateIncidentDurations(List<Long> startTimeList) {
    final List<Long> durationList = new ArrayList<>();
    for (int i = 0; i < startTimeList.size() - 1; i++) {
      final long duration = (long) randomGenerator
          .nextInt((int) (startTimeList.get(i + 1) - startTimeList.get(i)));
      durationList.add(duration);
    }
    final long duration = (long) randomGenerator.nextInt(
        (int) ((setting.getEndTime() / 100) / setting.getNumOfIncidents()));
    durationList.add(duration);
    return durationList;
  }

}
