package dmasForRouting;

public class Setting {
  
  private double vehicleLength;
  private double vehicleSpeed;
  private int numOfAGVs;
  private long endTime;
  private int speedUp;
  private long seed;
  
  public Setting(double vehicleLength, double vehicleSpeed, int numOfAGVs,
      long endTime, int speedUp, long seed) {
    this.vehicleLength = vehicleLength;
    this.vehicleSpeed = vehicleSpeed;
    this.numOfAGVs = numOfAGVs;
    this.endTime = endTime;
    this.speedUp = speedUp;
    this.seed = seed;
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
  
  public static class SettingBuilder {
    private double vehicleLength = 2d;
    private double vehicleSpeed = 1d;
    private int numOfAGVs = 1000;
    private long endTime = 10 * 60 * 1000L;
    private int speedUp = 16;
    private long seed = 0;
    
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
    
    public Setting build() {
      return new Setting(vehicleLength, vehicleSpeed, numOfAGVs, endTime,
          speedUp, seed);
    }
  }
  
}
