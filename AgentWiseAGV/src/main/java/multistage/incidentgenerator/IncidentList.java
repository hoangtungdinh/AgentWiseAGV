package multistage.incidentgenerator;

import java.util.LinkedList;
import java.util.List;

public class IncidentList {
  
  private LinkedList<Incident> incidentList;

  public IncidentList(List<Incident> incidentList) {
    this.incidentList = new LinkedList<>(incidentList);
  }
  
  public Incident getNextIncident() {
    return incidentList.removeFirst();
  }
  
  public List<Incident> getAllIncidents() {
    return incidentList;
  }
  
  public boolean isEmpty() {
    return incidentList.isEmpty();
  }
}
