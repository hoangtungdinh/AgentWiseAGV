package singlestage.contextaware.waiting;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import result.plancostandmakespan.Result;
import setting.Setting;

public class ExperimentSingleContextAware {

  public static void main(String[] args) {
    ListeningExecutorService executor = MoreExecutors
        .listeningDecorator(Executors
            .newFixedThreadPool(Runtime.getRuntime().availableProcessors()));

    List<ListenableFuture<Result>> futures = new ArrayList<>();
    
    try {
      LinkedList<Long> seeds = new LinkedList<>();
      
      InputStream inputStream = ExperimentSingleContextAware.class
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

      for (int numAGV = 1; numAGV <= 10; numAGV++) {
        for (int i = 0; i < 100; i++) {
          final long seed = seeds.removeFirst();
          final Setting setting = new Setting.SettingBuilder()
              .setNumOfAGVs(numAGV * 10).setSeed(seed).build();
          futures.add(executor.submit(new ExperimentRunner(setting)));
        }
      }
      
      executor.shutdown();
      
      print(futures);

    } catch (Exception e) {
      e.printStackTrace();
    }
    
    System.out.println("DONE!");
  }
  
  public static void print(List<ListenableFuture<Result>> futures) {
    try {
      PrintWriter printWriterMS = new PrintWriter(
          new File("ResultsCA_makespan.txt"));
      printWriterMS.println("numAGVs\tmakespan");
      for (ListenableFuture<Result> result : futures) {
        try {
          printWriterMS.println(result.get().getSetting().getNumOfAGVs() + "\t"
              + result.get().getMakeSpan());
        } catch (InterruptedException e) {
          e.printStackTrace();
        } catch (ExecutionException e) {
          e.printStackTrace();
        }
      }
      printWriterMS.close();

      PrintWriter printWriterPC = new PrintWriter(
          new File("ResultsCA_plancost.txt"));
      printWriterPC.println("numAGVs\tPlanCost");
      for (ListenableFuture<Result> result : futures) {
        try {
          printWriterPC.println(result.get().getSetting().getNumOfAGVs() + "\t"
              + result.get().getJointPlanCost());
        } catch (InterruptedException e) {
          e.printStackTrace();
        } catch (ExecutionException e) {
          e.printStackTrace();
        }
      }
      printWriterPC.close();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
  }
}
