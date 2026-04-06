package main;

public class LinearProbe extends HashTable
{
  private int[] table;
  private final HashFunction hashFunction;

  public LinearProbe(int tableSize)
  {
    this.table = new int[tableSize];
    this.hashFunction = new HashFunction();
  }

  public boolean insert(int key)
  {
    int startIndex = hashFunction.hash1(key) % table.length;
    int index = startIndex;

    do 
    {
      if (table[index] == AppConfig.EMPTY || table[index] == AppConfig.TOMBSTONE) 
      {
        table[index] = key;
        return true;
      }

      if (table[index] == key) 
      {
        return true;
      }

      index = (index + 1) % table.length;
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
