package test;

import static com.google.common.base.Preconditions.checkState;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.github.rinde.rinsim.geom.Point;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import resourceagents.NodeAgent;
import routeplan.contextaware.planstepprioritygraph.SingleStep;
import setting.Setting;

public class Dummy {

  public static void main(String[] args) {
//    final Setting setting = new Setting.SettingBuilder()
//        .setNumOfAGVs(10).setSeed(6277757).build();
//    final SingleStep node1 = new SingleStep(new NodeAgent(new Point(0d, 0d), setting), 1, 1);
//    final SingleStep node2 = new SingleStep(new NodeAgent(new Point(0d, 1d), setting), 2, 2);
//    final SingleStep node3 = new SingleStep(new NodeAgent(new Point(0d, 2d), setting), 3, 3);
//    final List<SingleStep> allNodes = new ArrayList<>();
//    allNodes.add(node1);
//    allNodes.add(node2);
//    allNodes.add(node3);
//    final Multimap<SingleStep, SingleStep> graph = HashMultimap.create();
//    graph.put(node1, node2);
//    graph.put(node2, node3);
//    graph.put(node3, node1);
//    System.out.println(isAcyclic(graph, allNodes));
  }
  
  public static boolean isAcyclic(Multimap<SingleStep, SingleStep> graph, List<SingleStep> allNodes) {
    List<SingleStep> nodesWithIncomingEdges = new ArrayList<>(graph.values());
    final LinkedList<SingleStep> listS = new LinkedList<>(allNodes);
    // listS contains set of all nodes with no incoming edges
    listS.removeAll(nodesWithIncomingEdges);
    final LinkedList<SingleStep> listL = new LinkedList<>();
    
    while (!listS.isEmpty()) {
      final SingleStep nodeN = listS.remove();
      listL.addLast(nodeN);
      final List<SingleStep> outComingNodes = new ArrayList<>(graph.get(nodeN));
      // for each node M with an edge e from N to M
      for (SingleStep nodeM : outComingNodes) {
        // remove edge e from the graph
        final boolean validEdge = graph.remove(nodeN, nodeM);
        checkState(validEdge, "Cannot find any edge!");
        // if M has no other incoming edges then insert M to listS
        nodesWithIncomingEdges = new ArrayList<>(graph.values());
        if (!nodesWithIncomingEdges.contains(nodeM)) {
          listS.add(nodeM);
        }
      }
    }
    
    if (graph.isEmpty()) {
      return true;
    } else {
      return false;
    }
  }
}
