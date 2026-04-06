package main;

public class Cuckoo extends HashTable
{
  private int[] table;
  private final HashFunction hashFunction;
  private int maxIterations;
  private int maxRehashes;

  public Cuckoo(int tableSize)
  {
    this.table = new int[tableSize];
    this.hashFunction = new HashFunction();
    this.maxIterations = Math.max(1000, (int)Math.sqrt(tableSize));
    this.maxRehashes = Math.max(8, (int)(Math.log(tableSize) / Math.log(2)));
  }

  public boolean insert(int key)
  {
    if (search(key))
    {
      return false;
    }

    int pendingKey = key;

    for (int rehashCount = 0; rehashCount <= maxRehashes; rehashCount++)
    {
      int displacedKey = placeWithoutRehash(pendingKey);
      if (displacedKey == AppConfig.EMPTY)
      {
        return true;
      }

      pendingKey = displacedKey;
      if (rehashCount == maxRehashes)
      {
        break;
      }

      if (!rehash())
      {
        return false;
      }
    }

    return false;
  }

  private int placeWithoutRehash(int key)
  {
    int h1 = hashFunction.hash1(key);
    int h2 = hashFunction.hash2(key);

    if (table[h1] == AppConfig.EMPTY)
    {
      table[h1] = key;
      return AppConfig.EMPTY;
    }
    else if (table[h2] == AppConfig.EMPTY)
    {
      table[h2] = key;
      return AppConfig.EMPTY;
    }

    int pos = h1;
    int i = 0;

    while (table[pos] != AppConfig.EMPTY && i < maxIterations)
    {
      int temp = key;
      key = table[pos];
      table[pos] = temp;

      if (pos == hashFunction.hash1(key))
        pos = hashFunction.hash2(key);
      else
        pos = hashFunction.hash1(key);

      i++;
    }

    if (table[pos] == AppConfig.EMPTY)
    {
      table[pos] = key;
      return AppConfig.EMPTY;
    }

    return key;
  }

  private boolean rehash()
  {
    int[] oldTable = table;
    table = new int[oldTable.length];
    hashFunction.reseed();

    for (int i = 0; i < oldTable.length; i++)
    {
      if (oldTable[i] != AppConfig.EMPTY)
      {
        if (placeWithoutRehash(oldTable[i]) != AppConfig.EMPTY)
          return false;
      }
    }
    return true;
  }

  public boolean search(int key)
  {
    if (table[hashFunction.hash1(key)] != AppConfig.EMPTY && table[hashFunction.hash1(key)] == key)
    {
      return true;
    }
    else if (table[hashFunction.hash2(key)] != AppConfig.EMPTY && table[hashFunction.hash2(key)] == key)
    {
      return true;
    }
    return false;
  }

  public boolean delete(int key)
  {
    boolean result = false;

    if (table[hashFunction.hash1(key)] != AppConfig.EMPTY && table[hashFunction.hash1(key)] == key)
    {
      table[hashFunction.hash1(key)] = AppConfig.EMPTY;
      result = true;
    }
    else if (table[hashFunction.hash2(key)] != AppConfig.EMPTY && table[hashFunction.hash2(key)] == key)
    {
      table[hashFunction.hash2(key)] = AppConfig.EMPTY;
      result = true;
    }

    return result;
  }

}
