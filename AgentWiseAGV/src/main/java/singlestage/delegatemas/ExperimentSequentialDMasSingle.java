package singlestage.delegatemas;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import experiment.Experiment;
import result.plancostandmakespan.Result;
import result.plancostandmakespan.Sample;
import setting.Setting;

public class ExperimentSequentialDMasSingle {

  public static void main(String[] args) {
    LinkedList<Long> seeds = new LinkedList<>();
    
    try {
      InputStream inputStream = Experiment.class
          .getResourceAsStream("seeds.txt");
      BufferedReader bufferedReader = new BufferedReader(
          new InputStreamReader(inputStream));
      
      String line = bufferedReader.readLine();
      
      while(line != null && !line.isEmpty()) {
        long seed = Long.parseLong(line);
        seeds.add(seed);
        line = bufferedReader.readLine();
      }
      
      bufferedReader.close();
      
      List<Sample> samples = new ArrayList<>();
      
      for (int numAGV = 1; numAGV <= 10; numAGV++) {
        final Sample sample = new Sample(numAGV*10);
        for (int i = 0; i < 100; i++) {
          final long seed = seeds.removeFirst();
          System.out.println("num of AGVs: " + (numAGV*10) + "\tSample: " + i + "\tSeed: " + seed);
          final Setting setting = new Setting.SettingBuilder()
              .setNumOfAGVs(numAGV * 10).setSeed(seed).build();
          final singlestage.delegatemas.AGVSystem agvSystem = new AGVSystem(setting, false);
          final Result result = agvSystem.run();
          sample.addResult(result);
        }
        samples.add(sample);
      }
      
      print(samples);
    } catch (Exception e) {
      e.printStackTrace();
    }
    System.out.println("DONE!");
  }
  
  public static void print(List<Sample> samples) {
    try {
      PrintWriter printWriterMS = new PrintWriter(new File("src/main/resources/ResultsDMAS_makespan.txt"));
      printWriterMS.println("numAGVs\tmakespan");
      for (Sample sample : samples) {
        List<Result> results = sample.getResults();
        for (Result result : results) {
          printWriterMS.println(sample.getNumOfAGVs() + "\t" + result.getMakeSpan());
        }
      }
      printWriterMS.close();
      
      PrintWriter printWriterPC = new PrintWriter(new File("src/main/resources/ResultsDMAS_plancost.txt"));
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
