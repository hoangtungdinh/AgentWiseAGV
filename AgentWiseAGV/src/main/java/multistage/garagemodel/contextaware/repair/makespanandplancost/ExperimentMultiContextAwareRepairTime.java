package multistage.garagemodel.contextaware.repair.makespanandplancost;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

import result.plancostandmakespan.Result;
import setting.Setting;

public class ExperimentMultiContextAwareRepairTime {

  public static void main(String[] args) {
    final long seed = 5799613;
    final Setting setting = new Setting.SettingBuilder()
        .setNumOfAGVs(100).setSeed(seed).build();
    final AGVSystem agvSystem = new AGVSystem(setting,
        false);
    
    final long startTime = System.nanoTime();
    final Result result = agvSystem.run();
    final long endTime = System.nanoTime();
    
    final long durationInNano = endTime - startTime;
    final long durationInSecond = durationInNano / 1000000000;
    final long durationInMinutes = durationInSecond / 60;
    final long durationInHours = durationInMinutes / 60;
    
    print(seed, durationInNano, durationInSecond, durationInMinutes, durationInHours);
    
    System.out.println(result.getJointPlanCost() + "\t DONE");
  }
  
  private static void print(long seed, long durationInNano,
      long durationInSecond, long durationInMinutes, long durationInHours) {
    try {
      PrintWriter printWriterMS = new PrintWriter(
          new File("CARepair" + seed + ".txt"));
      printWriterMS.println("Seed\tNanos\tSeconds\tMinutes\tHours");
      printWriterMS
          .println(seed + "\t" + durationInNano + "\t" + durationInSecond + "\t"
              + durationInMinutes + "\t" + durationInHours + "\tCARepair");
      printWriterMS.close();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    
  }

}