package main;

public class Robinhood extends HashTable
{
  private int[] table;
  private final HashFunction hashFunction;

  public Robinhood(int tableSize)
  {
    this.table = new int[tableSize];
    this.hashFunction = new HashFunction();
  }

  public boolean insert(int key)
  {
    int startIndex = hashFunction.hash1(key) % table.length;
    int index = startIndex;
    int currKey = key;
    int currDisplacement = 0;

    do
    {
      if (table[index] == AppConfig.EMPTY || table[index] == AppConfig.TOMBSTONE)
      {
        table[index] = currKey;
        return true;
      }

      if (table[index] == currKey)
      {
        return true;
      }

      int existingHome = hashFunction.hash1(table[index]) % table.length;
      int existingDisplacement = (index - existingHome + table.length) % table.length;

      if (existingDisplacement < currDisplacement)
      {
        int temp = table[index];
        table[index] = currKey;
        currKey = temp;
        currDisplacement = existingDisplacement;
      }

      index = (index + 1) % table.length;
      currDisplacement++;
    } while (index != startIndex);

    return false;
  }

  public boolean search(int key)
  {
    int startIndex = hashFunction.hash1(key) % table.length;
    int index = startIndex;

    do
    {
      if (table[index] == AppConfig.EMPTY)
      {
        return false;
      }

      if (table[index] == key)
      {
        return true;
      }

      index = (index + 1) % table.length;
    } while (index != startIndex);

    return false;
  }

  public boolean delete(int key)
  {
    int startIndex = hashFunction.hash1(key) % table.length;
    int index = startIndex;

    do
    {
      if (table[index] == AppConfig.EMPTY)
      {
        return false;
      }

      if (table[index] == key)
      {
        table[index] = AppConfig.TOMBSTONE;
        return true;
      }

      index = (index + 1) % table.length;
    } while (index != startIndex);

    return false;
  }
}
