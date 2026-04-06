package main;

public class AppConfig 
{
  public enum OAHTImplementation
  {
    LINEAR,
    CUCKOO,
    ROBINHOOD,
    FUNNEL,
    SWISS
  }

  public static final int SEED = 0;

  public static final int TOMBSTONE = -1;
  public static final int EMPTY = 0;
  
  /*********************************************************************************
   * CHANGE THESE CONFIGURATIONS AS NEEDED FOR TESTING
  *********************************************************************************/
  public static final OAHTImplementation IMPLEMENTATION = OAHTImplementation.SWISS;
  public static final int TABLE_SIZE = 10000000;
  public static final double[] LOAD_FACTORS = {0.05, 0.10, 0.20, 0.30, 0.40, 0.50, 0.60, 0.70, 0.80, 0.90};
  public static final int NUM_TRIALS = 20;
  public static final int NUM_SEARCHES = 10000000;
  

  
}
