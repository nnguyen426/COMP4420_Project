package main;

import java.util.Arrays;

public class SwissTable extends HashTable
{
    private static final byte EMPTY = (byte) 0x80;
    private static final byte DELETED = (byte) 0xFE;
    private static final byte SENTINEL = (byte) 0xFF;

    private static final int GROUP_SIZE = 8;

    private static final long BITMASK_LSB = 0x0101010101010101L;
    private static final long BITMASK_MSB = 0x8080808080808080L;
    private static final long MATCH_MASK_MUL = 0x0002040810204081L;
    private static final long EMPTY_BROADCAST = broadcast(EMPTY);
    private static final long DELETED_BROADCAST = broadcast(DELETED);

    private final HashFunction hashFunction;
    private final double loadFactor;

    private long[] ctrl;
    private int[] keys;

    private int capacity;
    private int size;
    private int tombstones;
    private int maxLoad;

    public SwissTable(int tableSize, double loadFactor)
    {
        this.hashFunction = new HashFunction();
        this.loadFactor = loadFactor;

        int groupCount = Math.max(1, (tableSize + GROUP_SIZE - 1) / GROUP_SIZE);

        this.capacity = tableSize;
        this.ctrl = new long[groupCount];
        this.keys = new int[capacity];
        this.size = 0;
        this.tombstones = 0;
        this.maxLoad = calcMaxLoad(capacity);

        Arrays.fill(ctrl, EMPTY_BROADCAST);
        markPaddingAsSentinel(ctrl, capacity);
    }

    public boolean insert(int key)
    {
        if (size >= maxLoad)
        {
            return false;
        }

        // maybeRehash();

        int hash = hash(key);
        int h1 = h1(hash);
        byte h2 = h2(hash);
        long h2Broadcast = broadcast(h2);

        int groupCount = ctrl.length;
        int group = Math.floorMod(h1, groupCount);
        int firstTombstone = -1;
        int visitedGroups = 0;

        while (visitedGroups < groupCount)
        {
            long word = ctrl[group];
            int base = group << 3;

            int matchMask = eqMask(word, h2Broadcast);
            while (matchMask != 0)
            {
                int index = base + Integer.numberOfTrailingZeros(matchMask);
                if (keys[index] == key)
                {
                    return false;
                }

                matchMask &= matchMask - 1;
            }

            if (firstTombstone < 0)
            {
                int deletedMask = eqMask(word, DELETED_BROADCAST);
                if (deletedMask != 0)
                {
                    firstTombstone = base + Integer.numberOfTrailingZeros(deletedMask);
                }
            }

            int emptyMask = eqMask(word, EMPTY_BROADCAST);
            if (emptyMask != 0)
            {
                int emptyIndex = base + Integer.numberOfTrailingZeros(emptyMask);
                int targetIndex = (firstTombstone >= 0) ? firstTombstone : emptyIndex;
                insertAt(targetIndex, key, h2);
                return true;
            }

            group = (group + 1) % groupCount;
            visitedGroups++;
        }

        return false;
    }

    public boolean search(int key)
    {
        if (size == 0)
        {
            return false;
        }

        int hash = hash(key);
        int h1 = h1(hash);
        byte h2 = h2(hash);
        long h2Broadcast = broadcast(h2);

        int groupCount = ctrl.length;
        int group = Math.floorMod(h1, groupCount);
        int visitedGroups = 0;

        while (visitedGroups < groupCount)
        {
            long word = ctrl[group];
            int base = group << 3;

            int matchMask = eqMask(word, h2Broadcast);
            while (matchMask != 0)
            {
                int index = base + Integer.numberOfTrailingZeros(matchMask);
                if (keys[index] == key)
                {
                    return true;
                }

                matchMask &= matchMask - 1;
            }

            if (eqMask(word, EMPTY_BROADCAST) != 0)
            {
                return false;
            }

            group = (group + 1) % groupCount;
            visitedGroups++;
        }

        return false;
    }

    public boolean delete(int key)
    {
        if (size == 0)
        {
            return false;
        }

        int hash = hash(key);
        int h1 = h1(hash);
        byte h2 = h2(hash);
        long h2Broadcast = broadcast(h2);

        int groupCount = ctrl.length;
        int group = Math.floorMod(h1, groupCount);
        int visitedGroups = 0;

        while (visitedGroups < groupCount)
        {
            long word = ctrl[group];
            int base = group << 3;

            int matchMask = eqMask(word, h2Broadcast);
            while (matchMask != 0)
            {
                int index = base + Integer.numberOfTrailingZeros(matchMask);
                if (keys[index] == key)
                {
                    setCtrlAt(index, DELETED);
                    keys[index] = 0;
                    size--;
                    tombstones++;
                    // maybeRehash();
                    return true;
                }

                matchMask &= matchMask - 1;
            }

            if (eqMask(word, EMPTY_BROADCAST) != 0)
            {
                return false;
            }

            group = (group + 1) % groupCount;
            visitedGroups++;
        }

        return false;
    }

    public int size()
    {
        return size;
    }

    private void insertAt(int index, int key, byte h2)
    {
        if (ctrlAt(index) == DELETED)
        {
            tombstones--;
        }

        keys[index] = key;
        setCtrlAt(index, h2);
        size++;
    }

    /*********************************************************************************
     * For a fair comparison, remove any rehashing and helper functions here for 
     * tombstones as the other implementations are not doing this. 
     *********************************************************************************/

    // private void maybeRehash()
    // {
    //     boolean tooManyTombstones = tombstones > (size >>> 1);

    //     if (!tooManyTombstones)
    //     {
    //         return;
    //     }

    //     rehash(capacity);
    // }

    // private void rehash(int newCapacity)
    // {
    //     long[] oldCtrl = ctrl;
    //     int[] oldKeys = keys;
    //     int oldCapacity = oldKeys.length;

    //     int groupCount = Math.max(1, (newCapacity + GROUP_SIZE - 1) / GROUP_SIZE);

    //     this.capacity = newCapacity;
    //     this.ctrl = new long[groupCount];
    //     this.keys = new int[this.capacity];
    //     this.size = 0;
    //     this.tombstones = 0;
    //     this.maxLoad = calcMaxLoad(this.capacity);

    //     Arrays.fill(this.ctrl, EMPTY_BROADCAST);
    //     markPaddingAsSentinel(this.ctrl, this.capacity);

    //     for (int i = 0; i < oldCapacity; i++)
    //     {
    //         if (isFull(ctrlAt(oldCtrl, i)))
    //         {
    //             insertFresh(oldKeys[i]);
    //         }
    //     }
    // }

    // private void insertFresh(int key)
    // {
    //     int hash = hash(key);
    //     int h1 = h1(hash);
    //     byte h2 = h2(hash);

    //     int groupCount = ctrl.length;
    //     int group = Math.floorMod(h1, groupCount);
    //     int visitedGroups = 0;

    //     while (visitedGroups < groupCount)
    //     {
    //         long word = ctrl[group];
    //         int emptyMask = eqMask(word, EMPTY_BROADCAST);

    //         if (emptyMask != 0)
    //         {
    //             int index = (group << 3) + Integer.numberOfTrailingZeros(emptyMask);
    //             keys[index] = key;
    //             setCtrlAt(index, h2);
    //             size++;
    //             return;
    //         }

    //         group = (group + 1) % groupCount;
    //         visitedGroups++;
    //     }

    //     throw new IllegalStateException("Rehash failed to place key in fixed-capacity table");
    // }

    private byte ctrlAt(int index)
    {
        return ctrlAt(ctrl, index);
    }

    private static byte ctrlAt(long[] ctrl, int index)
    {
        int group = index >> 3;
        int offset = (index & 7) << 3;
        return (byte) (ctrl[group] >>> offset);
    }

    private void setCtrlAt(int index, byte value)
    {
        int group = index >> 3;
        int offset = (index & 7) << 3;
        long mask = 0xFFL << offset;
        ctrl[group] = (ctrl[group] & ~mask) | ((value & 0xFFL) << offset);
    }

    private static int eqMask(long word, long broadcastedByte)
    {
        long x = word ^ broadcastedByte;
        long matches = (((x >>> 1) | BITMASK_MSB) - x) & BITMASK_MSB;
        return (int) ((matches * MATCH_MASK_MUL) >>> 56);
    }

    private static long broadcast(byte value)
    {
        return (value & 0xFFL) * BITMASK_LSB;
    }

    private static int h1(int hash)
    {
        return hash >>> 7;
    }

    private static byte h2(int hash)
    {
        return (byte) (hash & 0x7F);
    }

    private int hash(int key)
    {
        return hashFunction.hashRaw(key);
    }

    // private static boolean isFull(byte ctrlByte)
    // {
    //     return (ctrlByte & 0x80) == 0;
    // }

    private int calcMaxLoad(int capacity)
    {
        return (int) (capacity * loadFactor);
    }

    private static void markPaddingAsSentinel(long[] ctrl, int capacity)
    {
        int paddedCapacity = ctrl.length * GROUP_SIZE;
        for (int index = capacity; index < paddedCapacity; index++)
        {
            int group = index >> 3;
            int offset = (index & 7) << 3;
            long mask = 0xFFL << offset;
            ctrl[group] = (ctrl[group] & ~mask) | ((SENTINEL & 0xFFL) << offset);
        }
    }
}