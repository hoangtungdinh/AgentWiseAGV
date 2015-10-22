package multistage.garagemodel.delegatemas.throughput;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

import result.throughput.Result;
import setting.Setting;

public class SingleRun {

  public static void main(String[] args) {
    final long seed = 442087;
    final Setting setting = new Setting.SettingBuilder()
        .setNumOfAGVs(100).setSeed(seed).setEndTime(2000000).build();
    final AGVSystem agvSystem = new AGVSystem(setting,
        false);
    
    final Result result = agvSystem.run();
    
    print(seed, result);
  }
  
  private static void print(long seed, Result result) {
    try {
      PrintWriter printWriterMS = new PrintWriter(
          new File("SingleRun" + seed + ".txt"));
      printWriterMS.println("Seed: " + seed);
      printWriterMS.print("Throughput: " + result.getNumOfReachedDestinations());
      printWriterMS.close();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    
  }

}