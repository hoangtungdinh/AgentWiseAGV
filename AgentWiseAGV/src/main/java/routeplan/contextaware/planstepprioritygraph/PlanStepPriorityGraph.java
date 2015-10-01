package routeplan.contextaware.planstepprioritygraph;

import static com.google.common.base.Preconditions.checkState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import multistage.garagemodel.contextaware.repair.makespanandplancost.VehicleAgent;
import resourceagents.EdgeAgent;
import resourceagents.EdgeAgentList;
import resourceagents.NodeAgent;
import resourceagents.NodeAgentList;
import resourceagents.ResourceAgent;
import routeplan.CheckPoint;
import routeplan.ResourceType;

public class PlanStepPriorityGraph {
  
  private Multimap<SingleStep, SingleStep> graph;
  
  private List<SingleStep> allNodes;
  
  private NodeAgentList nodeAgentList;
  
  private EdgeAgentList edgeAgentList;
  
  private List<VehicleAgent> agvList;
  
  /** Map the agvID into its plan under SingleStep form. */
  private Map<Integer, List<SingleStep>> agvPlanMap;

  public PlanStepPriorityGraph(NodeAgentList nodeAgentList,
      EdgeAgentList edgeAgentList, List<VehicleAgent> agvList) {
    this.nodeAgentList = nodeAgentList;
    this.edgeAgentList = edgeAgentList;
    this.agvList = agvList;
  }

  public void createGraph() {
    graph = HashMultimap.create();
    agvPlanMap = new HashMap<>();
    allNodes = new ArrayList<>();
    createEdgesStep1();
    createEdgesStep2();
  }
  
  /**
   * Creates the edges according to step 1 in the paper: Plan repair in conflict-free routing
   */
  public void createEdgesStep1() {
    for (VehicleAgent agv : agvList) {
      final List<CheckPoint> checkPoints = agv.getCheckPoints();
      
      if (checkPoints.size() <= 1) {
        continue;
      }
      
      final List<SingleStep> plan = new ArrayList<>();
      
      for (int i = 0; i < checkPoints.size() - 1; i++) {
        final CheckPoint fromCheckPoint = checkPoints.get(i);
        final CheckPoint toCheckPoint = checkPoints.get(i + 1);
        
        checkState(
            fromCheckPoint.getResourceType() != toCheckPoint.getResourceType(),
            "Two consecutive check points must have different types");
        
        final ResourceAgent fromResource;
        final ResourceAgent toResource;
        
        if (fromCheckPoint.getResourceType() == ResourceType.EDGE) {
          // go from edge to node 
          fromResource = edgeAgentList.getEdgeAgent(fromCheckPoint.getResource().get(0), fromCheckPoint.getResource().get(1));
          toResource = nodeAgentList.getNodeAgent(toCheckPoint.getResource().get(0));
        } else {
          // go from node to edge
          fromResource = nodeAgentList.getNodeAgent(fromCheckPoint.getResource().get(0));
          toResource = edgeAgentList.getEdgeAgent(toCheckPoint.getResource().get(0), toCheckPoint.getResource().get(1));
        }
        
        final SingleStep fromStep = new SingleStep(fromResource, agv.getID(), fromCheckPoint.getExpectedTime());
        final SingleStep toStep = new SingleStep(toResource, agv.getID(), toCheckPoint.getExpectedTime());
        
        graph.put(fromStep, toStep);
        
        if (i == 0) {
          plan.add(fromStep);
          plan.add(toStep);
        } else {
          plan.add(toStep);
        }
      }
      
      allNodes.addAll(plan);
      agvPlanMap.put(agv.getID(), plan);
    }
  }
  
  /**
   * Creates the edges according to step 2 in ter mors's paper
   */
  public void createEdgesStep2() {
    for (NodeAgent nodeAgent : nodeAgentList.getAllNodeAgents()) {
      final List<SingleStep> orderedList = nodeAgent.getOrderedList();
      createEdgesFromOrderedList(orderedList);
    }
    
    for (EdgeAgent edgeAgent : edgeAgentList.getAllEdgeAgents()) {
      final List<SingleStep> orderedList = edgeAgent.getOrderedList();
      createEdgesFromOrderedList(orderedList);
    }
  }

  public void createEdgesFromOrderedList(List<SingleStep> orderedList) {
    for (int i = 0; i < orderedList.size() - 1; i++) {
      final SingleStep fromStep = orderedList.get(i);
      final SingleStep toStep = orderedList.get(i + 1);
      
      if (fromStep.getAgvID() == toStep.getAgvID()) {
        continue;
      }
      
      graph.put(fromStep, toStep);
      
      final SingleStep successorOfFromStep = getSuccessorStep(fromStep);
      final SingleStep successorOfToStep = getSuccessorStep(toStep);
      if (successorOfFromStep != null && successorOfToStep != null) {
        graph.put(successorOfFromStep, successorOfToStep);
      }
    }
  }
  
  public SingleStep getSuccessorStep(SingleStep step) {
    final int agvID = step.getAgvID();
    final List<SingleStep> plan = agvPlanMap.get(agvID);
    final int indexOfStep = plan.indexOf(step);
    if (indexOfStep < plan.size() - 1) {
      return plan.get(indexOfStep + 1);
    } else {
      return null;
    }
  }
  
  /**
   * Checks if is acyclic.
   * Implement according to https://en.wikipedia.org/wiki/Topological_sorting#CITEREFKahn1962
   *
   * @return true, if is acyclic
   */
  public boolean isAcyclic() {
    final List<SingleStep> nodesWithIncomingEdges = new ArrayList<>(graph.values());
    final LinkedList<SingleStep> listS = new LinkedList<>(allNodes);
    listS.removeAll(nodesWithIncomingEdges);
    final LinkedList<SingleStep> listL = new LinkedList<>();
    
    while (!listS.isEmpty()) {
      final SingleStep nodeN = listS.remove();
      listL.addLast(nodeN);
      final List<SingleStep> outComingNodes = new ArrayList<>(graph.get(nodeN));
      for (SingleStep nodeM : outComingNodes) {
        final boolean validEdge = graph.remove(nodeN, nodeM);
        checkState(validEdge, "Cannot find any edge!");
        
      }
    }
  }
}
