package multistage.incidentgenerator;

import java.util.LinkedList;
import java.util.List;

public class IncidentList {
  
  private LinkedList<Incident> incidentList;

  public IncidentList(List<Incident> incidentList) {
    this.incidentList = new LinkedList<>(incidentList);
  }
  
  public Incident getIncident() {
    return incidentList.removeFirst();
  }
}
