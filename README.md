This is a class project for COMP4420 (Advanced Design of Analysis of Algorithms) at the University of Manitoba. It contains five implementations of different hash table schemes: linear probing, Robin Hood hashing, Cuckoo hashing, funnel hashing, and Swiss tables. These data structures are tested using a benchmark which generates a random set of keys, inserts these into each of the implementations, searches for keys known to be present in the table (hits), searches for keys known to be excluded from the table (misses), and deletions of all the keys.

For running this program, you can simply execute this command into the console at the root directory:

```javac -d out src/main/*.java; java -cp out main.Main```

You may want to set some configurations as desired. These are contained in the AppConfig.java file.
IMPLEMENTATION - This switches which implementation should be used for testing.
TABLE_SIZE - The maximum possible number of keys that can exist in the table
LOAD_FACTORS - A list of the target load factor the table should reach after insertion
NUM_TRIALS - The number of repetitions to account for variability
NUM_SEARCHES - The number of search operations performed for each of search hits and search misses. 

Notes:
The Swiss table implemenation used this following repository as a reference. Code was removed from it to adjust for
the benchmark, assumptions of the study, and to ensure a fair comparison of hash table schemes.
https://bluuewhale.github.io/posts/building-a-fast-and-memory-efficient-hash-table-in-java-by-borrowing-the-best-ideas/