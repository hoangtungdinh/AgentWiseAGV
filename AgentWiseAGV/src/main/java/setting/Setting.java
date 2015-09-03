package setting;

public class Setting {
  
  private double vehicleLength;
  private double vehicleSpeed;
  private int numOfAGVs;
  private long endTime;
  private int speedUp;
  private long seed;
  
  private int numOfDestinations;
  private long evaporationDuration;
  private long refreshDuration;
  private long explorationDuration;
  private long switchingThreshold;
  private int numOfAlterRoutes;
  private int numOfDestsForEachAGV;
  
  public Setting(double vehicleLength, double vehicleSpeed, int numOfAGVs,
      long endTime, int speedUp, long seed, int numOfDestinations,
      long evaporationDuration, long refreshDuration, long explorationDuration,
      long switchingThreshold, int numOfAlterRoutes, int numOfDestsForEachAGV) {
    this.vehicleLength = vehicleLength;
    this.vehicleSpeed = vehicleSpeed;
    this.numOfAGVs = numOfAGVs;
    this.endTime = endTime;
    this.speedUp = speedUp;
    this.seed = seed;
    this.numOfDestinations = numOfDestinations;
    this.evaporationDuration = evaporationDuration;
    this.refreshDuration = refreshDuration;
    this.explorationDuration = explorationDuration;
    this.switchingThreshold = switchingThreshold;
    this.numOfAlterRoutes = numOfAlterRoutes;
    this.numOfDestsForEachAGV = numOfDestsForEachAGV;
  }

  public double getVehicleLength() {
    return vehicleLength;
  }

  public double getVehicleSpeed() {
    return vehicleSpeed;
  }

  public int getNumOfAGVs() {
    return numOfAGVs;
  }

  public long getEndTime() {
    return endTime;
  }

  public int getSpeedUp() {
    return speedUp;
  }

  public long getSeed() {
    return seed;
  }

  public int getNumOfDestinations() {
    return numOfDestinations;
  }

  public long getEvaporationDuration() {
    return evaporationDuration;
  }

  public long getRefreshDuration() {
    return refreshDuration;
  }

  public long getExplorationDuration() {
    return explorationDuration;
  }

  public long getSwitchingThreshold() {
    return switchingThreshold;
  }

  public int getNumOfAlterRoutes() {
    return numOfAlterRoutes;
  }
  
  public int getNumOfDestsForEachAGV() {
    return numOfDestsForEachAGV;
  }

  public static class SettingBuilder {
    private double vehicleLength = 2d;
    private double vehicleSpeed = 1d;
    private int numOfAGVs = 100;
    private long endTime = 1000 * 1000L;
    private int speedUp = 2;
    private long seed = 0;
    
    private int numOfDestinations = 1000;
    private long evaporationDuration = 10000;
    private long refreshDuration = 8000;
    private long explorationDuration = 5000;
    private long switchingThreshold = 6000;
    private int numOfAlterRoutes = 10;
    private int numOfDestsForEachAGV = 3;
    
    public SettingBuilder() {

    }

    public SettingBuilder setVehicleLength(double vehicleLength) {
      this.vehicleLength = vehicleLength;
      return this;
    }

    public SettingBuilder setVehicleSpeed(double vehicleSpeed) {
      this.vehicleSpeed = vehicleSpeed;
      return this;
    }

    public SettingBuilder setNumOfAGVs(int numOfAGVs) {
      this.numOfAGVs = numOfAGVs;
      return this;
    }

    public SettingBuilder setEndTime(long endTime) {
      this.endTime = endTime;
      return this;
    }

    public SettingBuilder setSpeedUp(int speedUp) {
      this.speedUp = speedUp;
      return this;
    }

    public SettingBuilder setSeed(long seed) {
      this.seed = seed;
      return this;
    }

    public SettingBuilder setNumOfDestinations(int numOfDestinations) {
      this.numOfDestinations = numOfDestinations;
      return this;
    }

    public SettingBuilder setEvaporationDuration(long evaporationDuration) {
      this.evaporationDuration = evaporationDuration;
      return this;
    }

    public SettingBuilder setRefreshDuration(long refreshDuration) {
      this.refreshDuration = refreshDuration;
      return this;
    }

    public SettingBuilder setExplorationDuration(long explorationDuration) {
      this.explorationDuration = explorationDuration;
      return this;
    }

    public SettingBuilder setSwitchingThreshold(long switchingThreshold) {
      this.switchingThreshold = switchingThreshold;
      return this;
    }

    public SettingBuilder setNumOfAlterRoutes(int numOfAlterRoutes) {
      this.numOfAlterRoutes = numOfAlterRoutes;
      return this;
    }
    
    public SettingBuilder setNumOfDestsForEachAGV(int numOfDestsForEachAGV) {
      this.numOfDestsForEachAGV = numOfDestsForEachAGV;
      return this;
    }

    public Setting build() {
      return new Setting(vehicleLength, vehicleSpeed, numOfAGVs, endTime,
          speedUp, seed, numOfDestinations, evaporationDuration,
          refreshDuration, explorationDuration, switchingThreshold,
          numOfAlterRoutes, numOfDestsForEachAGV);
    }
  }
  
}
