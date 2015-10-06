package multistage.garagemodel.contextaware.repair.throughput;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import result.throughput.Result;
import result.throughput.Sample;
import setting.Setting;

public class SequentialExperimentCA {

  public static void main(String[] args) {
    LinkedList<Long> seeds = new LinkedList<>();
    
    try {
      Scanner fileScanner = new Scanner(new File("src/main/resources/seeds.txt"));
      while (fileScanner.hasNextLine()) {
        seeds.add(fileScanner.nextLong());
      }
      fileScanner.close();
      
      List<Sample> samples = new ArrayList<>();
      
      for (int numAGV = 1; numAGV <= 8; numAGV++) {
        final Sample sample = new Sample(numAGV*5);
        for (int i = 0; i < 5; i++) {
          final long seed = seeds.removeFirst();
          System.out.println("num of AGVs: " + (numAGV*5) + "\tSample: " + i + "\tSeed: " + seed);
          final Setting setting = new Setting.SettingBuilder()
              .setNumOfAGVs(numAGV * 5).setSeed(seed).build();
          final AGVSystem agvSystem = new AGVSystem(setting, false);
          final Result result = agvSystem.run();
          sample.addResult(result);
        }
        samples.add(sample);
      }
      
      print(samples);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    
    System.out.println("DONE!");
  }
  
  public static void print(List<Sample> samples) {
    try {
      PrintWriter printWriterMS = new PrintWriter(new File("src/main/resources/ResultsMultiCAThroughput.txt"));
      printWriterMS.println("numAGVs\tFinishedTask");
      for (Sample sample : samples) {
        List<Result> results = sample.getResults();
        for (Result result : results) {
          printWriterMS.println(sample.getNumOfAGVs() + "\t" + result.getNumOfReachedDestinations());
        }
      }
      printWriterMS.close();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
  }

}