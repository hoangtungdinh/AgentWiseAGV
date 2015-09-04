package experiment;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import multistage.result.Result;
import setting.Setting;

public class Experiment {

  public static void main(String[] args) {
    ListeningExecutorService executor = MoreExecutors
        .listeningDecorator(Executors
            .newFixedThreadPool(Runtime.getRuntime().availableProcessors()));

    List<ListenableFuture<Result>> futures = new ArrayList<>();
    
    try {
      LinkedList<Long> seeds = new LinkedList<>();
      
      Scanner fileScanner = new Scanner(
          new File("src/main/resources/seeds.txt"));
      
      while (fileScanner.hasNextLine()) {
        seeds.add(fileScanner.nextLong());
      }
      
      fileScanner.close();

      for (int numAGV = 1; numAGV <= 2; numAGV++) {
        for (int i = 0; i < 2; i++) {
          final long seed = seeds.removeFirst();
          final Setting setting = new Setting.SettingBuilder()
              .setNumOfAGVs(numAGV * 10).setSeed(seed).build();
          futures.add(executor.submit(new ExperimentRunner(setting)));
        }
      }
      
      try {
        executor.shutdown();
        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      
      print(futures);

    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    
    System.out.println("DONE!");
  }
  
  public static void print(List<ListenableFuture<Result>> futures) {
    try {
      PrintWriter printWriterMS = new PrintWriter(
          new File("src/main/resources/TestParExperiment.txt"));
      printWriterMS.println("numAGVs\tFinishedTask");
      for (ListenableFuture<Result> result : futures) {
        try {
          printWriterMS.println(result.get().getSetting().getNumOfAGVs() + "\t"
              + result.get().getNumOfReachedDestinations());
        } catch (InterruptedException e) {
          e.printStackTrace();
        } catch (ExecutionException e) {
          e.printStackTrace();
        }
      }
      printWriterMS.close();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
  }

}
