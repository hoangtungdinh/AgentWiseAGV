package multistage.incidentgenerator;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class IncidentList {
  
  private LinkedList<Incident> incidentList;
  
  private List<Incident> initialList;

  public IncidentList(List<Incident> incidentList) {
    this.incidentList = new LinkedList<>(incidentList);
    this.initialList = new ArrayList<>(incidentList);
  }
  
  public Incident getNextIncident() {
    return incidentList.removeFirst();
  }
  
  public List<Incident> getAllIncidents() {
    return initialList;
  }
  
  public boolean isEmpty() {
    return incidentList.isEmpty();
  }
}
