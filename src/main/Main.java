package main;

import java.util.HashSet;
import java.util.Random;

public class Main 
{
  private static final Random RANDOM = new Random(AppConfig.SEED);

  public static void main(String[] args)
  {
    System.out.println("================================================================================");
    System.out.println("PERFORMANCE ANALYSIS OF OPEN ADDRESSING STRATEGIES FOR HASH TABLES AT HIGH LOADS");
    System.out.println("================================================================================\n");
    
    runTest();

    System.out.println("================================================================================");
    System.out.println("Program ended successfully. Have a nice day!");
    System.out.println("================================================================================\n");
  }

  private static void runTest()
  {
    double[][] savedInsertTimes = new double[AppConfig.LOAD_FACTORS.length][AppConfig.NUM_TRIALS];
    double[][] savedSearchHitTimes = new double[AppConfig.LOAD_FACTORS.length][AppConfig.NUM_TRIALS];
    double[][] savedSearchMissTimes = new double[AppConfig.LOAD_FACTORS.length][AppConfig.NUM_TRIALS];
    double[][] savedDeleteTimes = new double[AppConfig.LOAD_FACTORS.length][AppConfig.NUM_TRIALS];
    boolean[] skippedLoadFactors = new boolean[AppConfig.LOAD_FACTORS.length];
    String[] skipReasons = new String[AppConfig.LOAD_FACTORS.length];

    for (int trial = 0; trial < AppConfig.NUM_TRIALS; trial++)
    {
      System.out.println("=== Trial " + (trial + 1) + " of " + AppConfig.NUM_TRIALS + " ===");

      for (int i = 0; i < AppConfig.LOAD_FACTORS.length; i++)
      {
        double loadFactor = AppConfig.LOAD_FACTORS[i];

        if (skippedLoadFactors[i])
        {
          continue;
        }
        
        try
        {
          HashTable table = createHashTable(loadFactor);
          
          /*********************************************************************************
           * Generate list of keys
           *********************************************************************************/
          int[] insertKeys = generateKeys(loadFactor);
          shuffle(insertKeys);
          int[] searchKeys = insertKeys.clone();
          shuffle(searchKeys);
          int[] missKeys = generateMissKeys();
          shuffle(missKeys);
          int[] deleteKeys = insertKeys.clone();
          shuffle(deleteKeys);

          /*********************************************************************************
           * Insert all generated keys
           *********************************************************************************/
          // System.out.println("Inserting keys...");
          long startInsertTime = System.nanoTime();
          for (int j = 0; j < insertKeys.length; j++)
          {
            if(!table.insert(insertKeys[j]))
            {
              throw new IllegalStateException("Failed to insert key: " + insertKeys[j]);
            }
          }
          long endInsertTime = System.nanoTime();

          /*********************************************************************************
           * Search for keys known to be included in table
           *********************************************************************************/
          // System.out.println("Searching for included keys...");
          long startSearchHitTime = System.nanoTime();
          for (int j = 0; j < AppConfig.NUM_SEARCHES; j++)
          {
            if(!table.search(searchKeys[j % searchKeys.length]))
            {
              throw new IllegalStateException("Failed on hit search key: " + searchKeys[j % searchKeys.length]);
            }
          }
          long endSearchHitTime = System.nanoTime();

          /*********************************************************************************
           * Search for keys known to be excluded from table
           *********************************************************************************/
          // System.out.println("Searching for excluded keys...");
          long startSearchMissTime = System.nanoTime();
          for (int j = 0; j < AppConfig.NUM_SEARCHES; j++)
          {
            if(table.search(missKeys[j % missKeys.length]))
            {
              throw new IllegalStateException("Failed on miss search key: " + missKeys[j % missKeys.length]);
            }
          }
          long endSearchMissTime = System.nanoTime();

          /*********************************************************************************
           * Delete all generated keys
           *********************************************************************************/
          // System.out.println("Deleting keys...");
          long startDeleteTime = System.nanoTime();
          for (int j = 0; j < deleteKeys.length; j++)
          {
            if(!table.delete(deleteKeys[j]))
            {
              throw new IllegalStateException("Failed to delete key: " + deleteKeys[j]);
            }
          }
          long endDeleteTime = System.nanoTime();

          /*********************************************************************************
           * Saved times
           *********************************************************************************/
          long insertTime = endInsertTime - startInsertTime;
          long searchHitTime = endSearchHitTime - startSearchHitTime;
          long searchMissTime = endSearchMissTime - startSearchMissTime;
          long deleteTime = endDeleteTime - startDeleteTime;

          savedInsertTimes[i][trial] = (double)insertTime / insertKeys.length;
          savedSearchHitTimes[i][trial] = (double)searchHitTime / AppConfig.NUM_SEARCHES;
          savedSearchMissTimes[i][trial] = (double)searchMissTime / AppConfig.NUM_SEARCHES;
          savedDeleteTimes[i][trial] = (double)deleteTime / deleteKeys.length;
        }
        catch (IllegalStateException e)
        {
          skippedLoadFactors[i] = true;
          skipReasons[i] = e.getMessage();
          System.out.printf("Skipping load factor %.2f for all remaining trials (%s).%n", loadFactor, e.getMessage());
        }
      }
      
      System.out.println();
    }

    /*********************************************************************************
     * Print averaged results
     *********************************************************************************/
    System.out.println("\n========== AVERAGED RESULTS ACROSS " + AppConfig.NUM_TRIALS + " TRIALS ==========\n");
    for (int i = 0; i < AppConfig.LOAD_FACTORS.length; i++)
    {
      System.out.println("Load Factor: " + AppConfig.LOAD_FACTORS[i]);

      if (skippedLoadFactors[i])
      {
        System.out.println("Skipped due to trial failure: " + skipReasons[i]);
        System.out.println();
        continue;
      }
      
      double avgInsert = 0;
      double avgSearchHit = 0;
      double avgSearchMiss = 0;
      double avgDelete = 0;

      for (int j = 0; j < AppConfig.NUM_TRIALS; j++)
      {
        avgInsert += savedInsertTimes[i][j];
        avgSearchHit += savedSearchHitTimes[i][j];
        avgSearchMiss += savedSearchMissTimes[i][j];
        avgDelete += savedDeleteTimes[i][j];
      }

      avgInsert /= AppConfig.NUM_TRIALS;
      avgSearchHit /= AppConfig.NUM_TRIALS;
      avgSearchMiss /= AppConfig.NUM_TRIALS;
      avgDelete /= AppConfig.NUM_TRIALS;

      System.out.printf("Average Insert Time: %.2f ns\n", avgInsert);
      System.out.printf("Average Search Hit Time: %.2f ns\n", avgSearchHit);
      System.out.printf("Average Search Miss Time: %.2f ns\n", avgSearchMiss);
      System.out.printf("Average Delete Time: %.2f ns\n", avgDelete);
      
      System.out.println();
    }
  }

  private static HashTable createHashTable(double loadFactor)
  {
    switch (AppConfig.IMPLEMENTATION)
    {
      case LINEAR:
        return new LinearProbe(AppConfig.TABLE_SIZE);
      case CUCKOO:
        return new Cuckoo(AppConfig.TABLE_SIZE);
      case ROBINHOOD:
        return new Robinhood(AppConfig.TABLE_SIZE);
      case FUNNEL:
        return new Funnel(AppConfig.TABLE_SIZE, loadFactor);
      case SWISS:
        return new SwissTable(AppConfig.TABLE_SIZE, loadFactor);
      default:
        throw new IllegalStateException("Unknown hash implementation inputted: " + AppConfig.IMPLEMENTATION);
    }
  }

  private static int[] generateKeys(double loadFactor)
  {
    int numKeys = (int)Math.floor(AppConfig.TABLE_SIZE*loadFactor);
    int[] keys = new int[numKeys];
    HashSet<Integer> insertedKeys = new HashSet<>(numKeys*2);

    int i = 0;
    while (i < numKeys) 
    {
      int candidate = RANDOM.nextInt(Integer.MAX_VALUE-1) + 1;
      if (insertedKeys.add(candidate)) 
      {
        keys[i] = candidate;
        i++;
      }
    }

    return keys;
  }
  
  private static int[] generateMissKeys()
  {
    int[] keys = new int[AppConfig.NUM_SEARCHES];
    HashSet<Integer> usedKeys = new HashSet<>(AppConfig.NUM_SEARCHES * 2);

    int i = 0;
    while (i < AppConfig.NUM_SEARCHES)
    {
      int candidate = RANDOM.nextInt(Integer.MAX_VALUE - 1) - Integer.MAX_VALUE;
      if (usedKeys.add(candidate))
      {
        keys[i] = candidate;
        i++;
      }
    }

    return keys;
  }

  private static void shuffle(int[] array)
  {
    for (int i = array.length-1; i > 0; i--) 
    {
      int j = RANDOM.nextInt(i+1);
      int temp = array[i];
      array[i] = array[j];
      array[j] = temp;
    }
  }
}
