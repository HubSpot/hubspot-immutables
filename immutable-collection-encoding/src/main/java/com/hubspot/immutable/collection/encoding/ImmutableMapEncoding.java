package com.hubspot.immutable.collection.encoding;

import java.util.Map;

import org.immutables.encode.Encoding;
import org.immutables.encode.Encoding.Naming;
import org.immutables.encode.Encoding.StandardNaming;

import com.google.common.collect.ImmutableMap;
import com.hubspot.immutable.collection.encoding.util.BuilderState;

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

    private BuilderState currentState = BuilderState.EMPTY;

    @Encoding.Init
    @Naming(standard = StandardNaming.PUT)
    void put(K key, V value) {
      switch (currentState) {
        case GROWABLE:
          builder.put(key, value);
          break;
        case FULL:
          builder = ImmutableMap.<K, V>builderWithExpectedSize(map.size() + 1)
              .putAll(map)
              .put(key, value);

          map = null;
          break;
        case EMPTY:
          builder = ImmutableMap.builder();
          builder.put(key, value);
          break;
      }

      currentState = BuilderState.GROWABLE;
    }

    @Encoding.Init
    @Naming(standard = StandardNaming.PUT)
    void putEntry(Map.Entry<K, ? extends V> entry) {
      switch (currentState) {
        case GROWABLE:
          builder.put(entry);
          break;
        case FULL:
          builder = ImmutableMap.<K, V>builderWithExpectedSize(map.size() + 1)
              .putAll(map)
              .put(entry);

          map = null;
          break;
        case EMPTY:
          builder = ImmutableMap.builder();
          builder.put(entry);
          break;
      }

      currentState = BuilderState.GROWABLE;
    }

    @Encoding.Init
    @Naming(standard = StandardNaming.PUT_ALL)
    void putAll(Map<K, ? extends V> elements) {
      switch (currentState) {
        case GROWABLE:
          builder.putAll(elements);
          break;
        case FULL:
          builder = ImmutableMap.<K, V>builderWithExpectedSize(map.size() + elements.size())
              .putAll(map)
              .putAll(elements);

          map = null;
          break;
        case EMPTY:
          if (elements instanceof ImmutableMap) {
            set(elements);
            currentState = BuilderState.FULL;
            return;
          } else {
            builder = ImmutableMap.builderWithExpectedSize(elements.size());
            builder.putAll(elements);
            break;
          }
      }

      currentState = BuilderState.GROWABLE;
      // This return is weird but b/c we use a return above, we need this here to signal immutables to add another return here
      return;
    }

    @Encoding.Init
    @Encoding.Copy
    @Naming(standard = StandardNaming.INIT)
    void set(Map<K, ? extends V> input) {
      currentState = BuilderState.FULL;
      map = ImmutableMap.copyOf(input);
      builder = null;
    }

    @Encoding.Build
    ImmutableMap<K, V> build() {
      switch (currentState) {
        case GROWABLE:
          return builder.build();
        case FULL:
          return map;
        case EMPTY:
        default:
          return ImmutableMap.of();
      }
    }

  }
}
