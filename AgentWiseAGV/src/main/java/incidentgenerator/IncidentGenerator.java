package incidentgenerator;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.random.RandomGenerator;

import setting.Setting;

public class IncidentGenerator {

  private RandomGenerator randomGenerator;

  private Setting setting;

  public IncidentGenerator(RandomGenerator randomGenerator, Setting setting) {
    this.randomGenerator = randomGenerator;
    this.setting = setting;
  }

  public IncidentList run() {
    final List<Incident> incidentList = new ArrayList<>();

    long startOfTimeBlock = 0;

    while (startOfTimeBlock < setting.getEndTime()) {
      if (randomGenerator.nextDouble() < setting.getIncidentRate()) {
        final long incidentStartTime = (long) (startOfTimeBlock
            + randomGenerator.nextDouble() * setting.getTimeBlock());
        final long incidentDuration = (long) (setting.getMinIncidentDuration()
            + randomGenerator.nextDouble() * (setting.getMaxIncidentDuration()
                - setting.getMinIncidentDuration()));
        final Incident incident = new Incident(incidentStartTime,
            incidentDuration);
        incidentList.add(incident);
      }
      startOfTimeBlock += setting.getTimeBlock();
    }

    return new IncidentList(incidentList);
  }

}
