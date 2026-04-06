package main;

public class Funnel extends HashTable
{
  private int tableSize;
  private final HashFunction hashFunction;

  private double delta;
  private int alpha;
  private int beta;

  private int[] mainTable;

  private int[] levelSizes;
  private int[] levelOffsets;
  private int[] bucketCounts;

  private int specialArraySize;

  private int[] specialArray;

  public Funnel(int tableSize, double loadFactor)
  {
    this.tableSize = tableSize;
    this.hashFunction = new HashFunction();

    this.delta = 1.0 - loadFactor;
    double logInvDelta = Math.log(1.0 / delta) / Math.log(2);

    this.alpha = (int) Math.ceil(4 * logInvDelta + 10);
    this.beta = (int) Math.ceil(2 * logInvDelta);

    this.specialArraySize = getSpecialArraySize();
    int mainArraySize = this.tableSize - specialArraySize;

    this.specialArray = new int[specialArraySize];

    this.levelSizes = new int[alpha];
    this.levelOffsets = new int[alpha];
    this.bucketCounts = new int[alpha];

    int totalBuckets = mainArraySize / beta;

    double[] weights = new double[alpha];
    double weightSum = 0.0;
    for (int i = 0; i < alpha; i++)
    {
      weights[i] = Math.pow(0.75, i);
      weightSum += weights[i];
    }

    int usedBuckets = 0;
    for (int i = 0; i < alpha; i++)
    {
      bucketCounts[i] = (int) (totalBuckets * weights[i] / weightSum);
      usedBuckets += bucketCounts[i];
    }

    int leftoverBuckets = totalBuckets - usedBuckets;
    int index = 0;
    while (leftoverBuckets > 0)
    {
      bucketCounts[index]++;
      leftoverBuckets--;
      index++;
    }

    int assignedMainSlots = 0;
    for (int i = 0; i < alpha; i++)
    {
      levelSizes[i] = bucketCounts[i] * beta;
      assignedMainSlots += levelSizes[i];
    }

    int remainder = mainArraySize - assignedMainSlots;
    if (remainder > 0)
    {
      levelSizes[alpha - 1] += remainder;
    }
    else if (remainder < 0)
    {
      bucketCounts[alpha - 1] += remainder / beta;
      levelSizes[alpha - 1] = bucketCounts[alpha - 1] * beta;
    }

    int runningOffset = 0;
    for (int i = 0; i < alpha; i++)
    {
      levelOffsets[i] = runningOffset;
      runningOffset += levelSizes[i];
    }

    this.mainTable = new int[mainArraySize];
  }

  private int getSpecialArraySize()
  {
    int minSize = (int) Math.ceil(delta * tableSize / 2.0);
    int maxSize = (int) Math.floor(3.0 * delta * tableSize / 4.0);

    int currSize = minSize;

    while ((this.tableSize - currSize) % this.beta != 0 && currSize <= maxSize)
    {
      currSize++;
    }

    if (currSize > maxSize)
    {
      currSize = minSize;
    }

    return currSize;
  }

  public boolean insert(int key)
  {
    if (key == AppConfig.EMPTY || key == AppConfig.TOMBSTONE)
    {
      return false;
    }

    for (int level = 0; level < alpha; level++)
    {
      if (bucketCounts[level] <= 0)
      {
        continue;
      }

      int bucketIndex = bucketIndexForLevel(key, level);
      int bucketStart = levelOffsets[level] + bucketIndex * beta;
      int bucketLength = Math.min(beta, levelSizes[level] - bucketIndex * beta);

      if (bucketLength <= 0)
      {
        continue;
      }

      for (int i = 0; i < bucketLength; i++)
      {
        int absoluteIndex = bucketStart + i;
        int existing = mainTable[absoluteIndex];

        if (existing == key)
        {
          return true;
        }

        if (existing == AppConfig.EMPTY || existing == AppConfig.TOMBSTONE)
        {
          mainTable[absoluteIndex] = key;
          return true;
        }
      }
    }

    return insertIntoSpecialArray(key);
  }

  private int bucketIndexForLevel(int key, int level)
  {
    int seed = hashFunction.getSeed1() ^ (0x7f4a7c15 * (level + 1));
    int h = hashWithSeed(key, seed);
    return h % bucketCounts[level];
  }

  private int hashWithSeed(int key, int seed)
  {
    int h = hashFunction.hash(key, seed);
    return h % tableSize;
  }

  private boolean insertIntoSpecialArray(int key)
  {
    if (specialArray.length == 0)
    {
      return false;
    }

    int start = Math.floorMod(hashFunction.hash2(key), specialArray.length);
    int index = start;

    do
    {
      if (specialArray[index] == key)
      {
        return true;
      }

      if (specialArray[index] == AppConfig.EMPTY || specialArray[index] == AppConfig.TOMBSTONE)
      {
        specialArray[index] = key;
        return true;
      }

      index = (index + 1) % specialArray.length;
    } while (index != start);

    return false;
  }

  public boolean search(int key)
  {
    if (key == AppConfig.EMPTY || key == AppConfig.TOMBSTONE)
    {
      return false;
    }

    for (int level = 0; level < alpha; level++)
    {
      if (bucketCounts[level] <= 0)
      {
        continue;
      }

      int bucketIndex = bucketIndexForLevel(key, level);
      int bucketStart = levelOffsets[level] + bucketIndex * beta;
      int bucketLength = Math.min(beta, levelSizes[level] - bucketIndex * beta);

      if (bucketLength <= 0)
      {
        continue;
      }

      for (int i = 0; i < bucketLength; i++)
      {
        int absoluteIndex = bucketStart + i;
        if (mainTable[absoluteIndex] == key)
        {
          return true;
        }
      }
    }

    return searchInSpecialArray(key);
  }

  private boolean searchInSpecialArray(int key)
  {
    if (specialArray.length == 0)
    {
      return false;
    }

    int start = Math.floorMod(hashFunction.hash2(key), specialArray.length);
    int index = start;

    do
    {
      if (specialArray[index] == key)
      {
        return true;
      }

      if (specialArray[index] == AppConfig.EMPTY)
      {
        return false;
      }

      index = (index + 1) % specialArray.length;
    } while (index != start);

    return false;
  }

  public boolean delete(int key)
  {
    if (key == AppConfig.EMPTY || key == AppConfig.TOMBSTONE)
    {
      return false;
    }

    for (int level = 0; level < alpha; level++)
    {
      if (bucketCounts[level] <= 0)
      {
        continue;
      }

      int bucketIndex = bucketIndexForLevel(key, level);
      int bucketStart = levelOffsets[level] + bucketIndex * beta;
      int bucketLength = Math.min(beta, levelSizes[level] - bucketIndex * beta);

      if (bucketLength <= 0)
      {
        continue;
      }

      for (int i = 0; i < bucketLength; i++)
      {
        int absoluteIndex = bucketStart + i;
        if (mainTable[absoluteIndex] == key)
        {
          mainTable[absoluteIndex] = AppConfig.TOMBSTONE;
          return true;
        }
      }
    }

    return deleteFromSpecialArray(key);
  }

  private boolean deleteFromSpecialArray(int key)
  {
    if (specialArray.length == 0)
    {
      return false;
    }

    int start = Math.floorMod(hashFunction.hash2(key), specialArray.length);
    int index = start;

    do
    {
      if (specialArray[index] == key)
      {
        specialArray[index] = AppConfig.TOMBSTONE;
        return true;
      }

      if (specialArray[index] == AppConfig.EMPTY)
      {
        return false;
      }

      index = (index + 1) % specialArray.length;
    } while (index != start);

    return false;
  }
}