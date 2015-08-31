package singlestage.contextaware;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import resultrecording.Result;
import setting.Setting;

public class Experiment {

  public static void main(String[] args) {
    LinkedList<Long> seeds = new LinkedList<>();
    
    try {
      Scanner fileScanner = new Scanner(new File("src/main/resources/seeds.txt"));
      while (fileScanner.hasNextLine()) {
        seeds.add(fileScanner.nextLong());
      }
      fileScanner.close();
      
      List<Sample> samples = new ArrayList<>();
      
      for (int numAGV = 1; numAGV <= 10; numAGV++) {
        final Sample sample = new Sample(numAGV*10);
        for (int i = 0; i < 100; i++) {
          final Setting setting = new Setting.SettingBuilder()
              .setNumOfAGVs(numAGV * 10).setSeed(seeds.removeFirst()).build();
          final AGVSystem agvSystem = new AGVSystem(setting);
          final Result result = agvSystem.run();
          sample.addResult(result);
          System.out.println("num of AGVs: " + (numAGV*10) + "\tSample: " + i);
        }
        samples.add(sample);
      }
      
      print(samples);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
  }
  
  public static void print(List<Sample> samples) {
    try {
      PrintWriter printWriterMS = new PrintWriter(new File("src/main/resources/ResultsCA_makespan.txt"));
      printWriterMS.println("numAGVs\tmakespan");
      for (Sample sample : samples) {
        List<Result> results = sample.getResults();
        for (Result result : results) {
          printWriterMS.println(sample.getNumOfAGVs() + "\t" + result.getMakeSpan());
        }
      }
      printWriterMS.close();
      
      PrintWriter printWriterPC = new PrintWriter(new File("src/main/resources/ResultsCA_plancost.txt"));
      printWriterPC.println("numAGVs\tPlanCost");
      for (Sample sample : samples) {
        List<Result> results = sample.getResults();
        for (Result result : results) {
          printWriterPC.println(sample.getNumOfAGVs() + "\t" + result.getJointPlanCost());
        }
      }
      printWriterPC.close();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
  }

}
