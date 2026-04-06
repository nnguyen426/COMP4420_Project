package main;

import java.util.Random;

public class HashFunction 
{
  private final Random random;
  
  private int seed1;
  private int seed2;
    
  public HashFunction() 
  {
    this.random = new Random(AppConfig.SEED);
    reseed();
  }

  public void reseed() 
  {
    seed1 = random.nextInt();
    do
    {
      seed2 = random.nextInt();
    } while (seed1 == seed2);
  }

  public int getSeed1() 
  {
    return seed1;
  }

  public int getSeed2() 
  {
    return seed2;
  }

  public int hash1(int key) 
  {
    return hash(key, seed1);
  }

  public int hash2(int key) 
  {
    return hash(key, seed2);
  }

  public int hashRaw(int key)
  {
    return mix(key);
  }

  public int hash(int key, int seed) 
  {
    int h = mix(key ^ seed);
    return (h & 0x7fffffff) % AppConfig.TABLE_SIZE;
  }

  private int mix(int x) 
  {
    x ^= (x >>> 16);
    x *= 0x7feb352d;
    x ^= (x >>> 15);
    x *= 0x846ca68b;
    x ^= (x >>> 16);
    return x;
  }
}