package test;

import com.github.rinde.rinsim.geom.Point;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import resourceagents.NodeAgent;
import routeplan.contextaware.planstepprioritygraph.SingleStep;
import setting.Setting;

public class Dummy {

  public static void main(String[] args) {
    final Setting setting = new Setting.SettingBuilder()
        .setNumOfAGVs(10).setSeed(8496955).build();
    final NodeAgent nodeAgent = new NodeAgent(new Point(0d, 0d), setting);
    final Multimap<SingleStep, SingleStep> multimap = HashMultimap.create();
    SingleStep singleStep1 = new SingleStep(nodeAgent, 0, 3);
    SingleStep singleStep2 = new SingleStep(nodeAgent, 1, 3);
    SingleStep singleStep3 = new SingleStep(nodeAgent, 2, 5);
    multimap.put(singleStep1, singleStep3);
    multimap.put(singleStep2, singleStep3);
    return;
  }

}
