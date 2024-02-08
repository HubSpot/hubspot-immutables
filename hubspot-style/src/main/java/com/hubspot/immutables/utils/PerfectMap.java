package com.hubspot.immutables.utils;

import com.google.common.collect.ImmutableList;

import java.util.BitSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * This map attempts to find a <a href="https://en.wikipedia.org/wiki/Perfect_hash_function">perfect hash function</a>
 * for the given set of strings.
 * This map trades a relatively expensive construction cost and immutability for the ability to
 * avoid collision lookups.
 */
class PerfectMap<V> implements Map<String, V> {
  private static final int MAXIMUM_ENUM_CONSTANT_SIZE = 511;
  private static final int MAXIMUM_TABLE_SIZE = 1024;

  private final String[] keyTable;
  private final V[] valueTable;
  private final ImmutableList<V> values;
  private final int size;

  private final int hashMultiplier;
  private final int hashAdder;
  private final int hashShifter;

  private final int hashMask;

  private PerfectMap(
    String[] keyTable,
    V[] valueTable,
    int size,
    int hashMultiplier,
    int hashAdder,
    int hashShifter
  ) {
    this.keyTable = keyTable;
    this.valueTable = valueTable;
    this.size = size;
    this.hashMultiplier = hashMultiplier;
    this.hashAdder = hashAdder;
    this.hashShifter = hashShifter;
    this.hashMask = keyTable.length - 1;
    ImmutableList.Builder<V> valuesListBuilder = ImmutableList.builder();
    for (int i = 0; i < keyTable.length; ++i) {
      if (keyTable[i] != null) {
        valuesListBuilder.add(valueTable[i]);
      }
    }
    this.values = valuesListBuilder.build();
  }

  public static <V> Optional<Map<String, V>> of(Map<String, V> map) {
    if (map.size() > MAXIMUM_ENUM_CONSTANT_SIZE) {
      return Optional.empty();
    }
    int[] hashCodes = new int[map.size()];
    Set<Integer> seenHashes = new HashSet<>();
    ImmutableList<String> keys = ImmutableList.copyOf(map.keySet());
    for (int i = 0; i < keys.size(); ++i) {
      hashCodes[i] = keys.get(i).hashCode();
      if (!seenHashes.add(hashCodes[i])) {
        return Optional.empty();
      }
    }
    // highest power of two at least as large as hashCodes.length
    int tableSize = 1 << (32 - Integer.numberOfLeadingZeros(hashCodes.length - 1));
    // multiply by two for some space in our table
    tableSize <<= 1;
    PerfectHashSearcher perfectHashSearcher = null;
    for (int j = 0; j < 4; ++j) {
      if (tableSize > MAXIMUM_TABLE_SIZE) {
        return Optional.empty();
      }
      perfectHashSearcher = new PerfectHashSearcher(
          hashCodes,
          tableSize
      );
      perfectHashSearcher.findPerfectHash();
      if (perfectHashSearcher.foundValue) {
        break;
      }
    }
    if (!perfectHashSearcher.foundValue) {
      return Optional.empty();
    }
    String[] keyTable = new String[tableSize];
    V[] valueTable = (V[]) new Object[tableSize];
    for (int i = 0; i < keys.size(); ++i) {
      String key = keys.get(i);
      int reHashed = reHash(
        hashCodes[i],
        perfectHashSearcher.multiplier,
        perfectHashSearcher.adder,
        perfectHashSearcher.shifter,
        perfectHashSearcher.mask
      );
      keyTable[reHashed] = key;
      valueTable[reHashed] = map.get(key);
    }
    return Optional.of(
      new PerfectMap<>(
        keyTable,
        valueTable,
        map.size(),
        perfectHashSearcher.multiplier,
        perfectHashSearcher.adder,
        perfectHashSearcher.shifter
      )
    );
  }

  @Override
  public int size() {
    return size;
  }

  @Override
  public boolean isEmpty() {
    return size > 0;
  }

  @Override
  public boolean containsKey(Object key) {
    int tableHash = getTableHash(key);
    return keyTable[tableHash] != null;
  }

  @Override
  public boolean containsValue(Object value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public V get(Object key) {
    int tableHash = getTableHash(key);
    String storedKey = keyTable[tableHash];
    if (storedKey == null || !storedKey.equals(key)) {
      return null;
    }
    return valueTable[tableHash];
  }

  private int getTableHash(Object key) {
    if (key instanceof String) {
      try {
        return reHash(
          key.hashCode(),
          hashMultiplier,
          hashAdder,
          hashShifter,
          hashMask
        );
      } catch (Exception ignored) {
        return -1;
      }
    }
    return -1;
  }

  @Override
  public V put(String key, V value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public V remove(Object key) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void putAll(Map<? extends String, ? extends V> m) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void clear() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Set<String> keySet() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Collection<V> values() {
    return values;
  }

  @Override
  public Set<Entry<String, V>> entrySet() {
    throw new UnsupportedOperationException();
  }

  private static class PerfectHashSearcher {
    private final BitSet seenHashes;
    private final int[] keyHashCodes;
    private final int mask;
    private boolean foundValue;
    private int multiplier;
    private int adder;
    private int shifter;

    private PerfectHashSearcher(int[] keyHashCodes, int tableSize) {
      this.keyHashCodes = keyHashCodes;
      this.mask = tableSize - 1;
      this.seenHashes = new BitSet(tableSize);
    }

    private void findPerfectHash() {
      for (int multiplier = 1; multiplier < 10_000; ++multiplier) {
        for (int adder = 0; adder <= multiplier; ++adder) {
          for (int shifter = 0; shifter <= 4; ++shifter) {
            if (isPerfect(multiplier, adder, shifter)) {
              this.multiplier = multiplier;
              this.adder = adder;
              this.shifter = shifter;
              this.foundValue = true;
              return;
            }
          }
        }
      }
    }

    private boolean isPerfect(int multiplier, int adder, int shifter) {
      seenHashes.clear();
      for (int keyHash : keyHashCodes) {
        int reHashed = reHash(keyHash, multiplier, adder, shifter, mask);
        if (seenHashes.get(reHashed)) {
          return false;
        }
        seenHashes.set(reHashed);
      }
      return true;
    }
  }

  private static int reHash(
    int hashCode,
    int multiplier,
    int adder,
    int shifter,
    int mask
  ) {
    return ((hashCode * multiplier + adder) >> shifter) & mask;
  }
}
