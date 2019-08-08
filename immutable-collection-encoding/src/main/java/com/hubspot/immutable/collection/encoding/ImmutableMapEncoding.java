package com.hubspot.immutable.collection.encoding;

import java.util.Map;

import org.immutables.encode.Encoding;
import org.immutables.encode.Encoding.Naming;
import org.immutables.encode.Encoding.StandardNaming;

import com.google.common.collect.ImmutableMap;

@Encoding
public class ImmutableMapEncoding<K, V> {

  @Encoding.Impl
  private ImmutableMap<K, V> field = ImmutableMap.of();

  @Encoding.Expose
  ImmutableMap<K, V> getImmutableMap() {
    return field;
  }

  @Encoding.Expose
  Map<K, V> getMap() {
    return field;
  }

  @Encoding.Copy
  @Naming(standard = StandardNaming.WITH)
  ImmutableMap<K, V> withCollection(Map<K, ? extends V> elements) {
    return ImmutableMap.copyOf(elements);
  }

  @Encoding.Builder
  @SuppressWarnings("UnstableApiUsage")
  static class Builder<K, V> {

    private ImmutableMap<K, V> map = null;
    private ImmutableMap.Builder<K, V> builder = null;

    @Encoding.Init
    @Naming(standard = StandardNaming.PUT)
    void put(K key, V value) {
      if (builder != null) {
        builder.put(key, value);
      } else if (map != null) {
        builder = ImmutableMap.<K, V>builderWithExpectedSize(map.size() + 1)
            .putAll(map)
            .put(key, value);

        map = null;
      } else {
        builder = ImmutableMap.builder();
        builder.put(key, value);
      }
    }

    @Encoding.Init
    @Naming(standard = StandardNaming.PUT)
    void putEntry(Map.Entry<K, ? extends V> entry) {
      if (builder != null) {
        builder.put(entry);
      } else if (map != null) {
        builder = ImmutableMap.<K, V>builderWithExpectedSize(map.size() + 1)
            .putAll(map)
            .put(entry);

        map = null;
      } else {
        builder = ImmutableMap.builder();
        builder.put(entry);
      }
    }

    @Encoding.Init
    @Naming(standard = StandardNaming.PUT_ALL)
    void putAll(Map<K, ? extends V> elements) {
      if (builder != null) {
        builder.putAll(elements);
      } else if (map != null) {
        builder = ImmutableMap.<K, V>builderWithExpectedSize(map.size() + elements.size())
            .putAll(map)
            .putAll(elements);

        map = null;
      } else {
        if (elements instanceof ImmutableMap) {
          set(elements);
        } else {
          builder = ImmutableMap.builderWithExpectedSize(elements.size());
          builder.putAll(elements);
        }
      }
    }

    @Encoding.Init
    @Encoding.Copy
    @Naming(standard = StandardNaming.INIT)
    void set(Map<K, ? extends V> input) {
      map = ImmutableMap.copyOf(input);
      builder = null;
    }

    @Encoding.Build
    ImmutableMap<K, V> build() {
      if (builder != null) {
        return builder.build();
      } else if (map != null) {
        return map;
      } else {
        return ImmutableMap.of();
      }
    }

  }
}
