package com.hubspot.immutable.collection.encoding;

import java.util.Collection;
import java.util.Set;

import org.immutables.encode.Encoding;
import org.immutables.encode.Encoding.Naming;
import org.immutables.encode.Encoding.StandardNaming;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSet;
import com.hubspot.immutable.collection.encoding.util.BuilderState;

@Encoding
public class ImmutableSetEncoding<T> {

  @Encoding.Impl
  private ImmutableSet<T> field = ImmutableSet.of();

  @Encoding.Expose
  ImmutableSet<T> getImmutableSet() {
    return field;
  }

  @Encoding.Expose
  Set<T> getSet() {
    return field;
  }

  @Encoding.Copy
  @Naming(standard = StandardNaming.WITH)
  ImmutableSet<T> withCollection(Iterable<T> elements) {
    return ImmutableSet.copyOf(elements);
  }

  @Encoding.Builder
  @SuppressWarnings("UnstableApiUsage")
  static class Builder<T> {

    private ImmutableSet<T> set = null;
    private ImmutableSet.Builder<T> builder = null;

    private BuilderState currentState = BuilderState.EMPTY;

    @Encoding.Init
    @Naming(standard = StandardNaming.ADD)
    void add(T element) {
      switch (currentState) {
        case GROWABLE:
          builder.add(element);
          break;
        case FULL:
          builder = ImmutableSet.<T>builderWithExpectedSize(set.size() + 1)
              .addAll(set)
              .add(element);

          set = null;
          break;
        case EMPTY:
          builder = ImmutableSet.builder();
          builder.add(element);
          break;
      }

      currentState = BuilderState.GROWABLE;
    }

    @Encoding.Init
    @Naming(standard = StandardNaming.ADD_ALL)
    void addAll(Iterable<T> elements) {
      switch (currentState) {
        case GROWABLE:
          builder.addAll(elements);
          break;
        case FULL:
          int additionalSize = 0;
          if (elements instanceof Collection) {
            additionalSize = ((Collection<T>) elements).size();
          }

          builder = ImmutableSet.<T>builderWithExpectedSize(set.size() + additionalSize)
              .addAll(set)
              .addAll(elements);

          set = null;
          break;
        case EMPTY:
          if (elements instanceof ImmutableCollection) {
            set(((ImmutableCollection<T>) elements));
            currentState = BuilderState.FULL;
            return;
          } else if (elements instanceof Collection) {
            builder = ImmutableSet.builderWithExpectedSize(((Collection<T>) elements).size());
          } else {
            builder = ImmutableSet.builder();
          }

          builder.addAll(elements);
          break;
      }

      currentState = BuilderState.GROWABLE;
      // This return is weird but b/c we use a return above, we need this here to signal immutables to add another return here
      return;
    }

    @Encoding.Init
    @Encoding.Copy
    @Naming(standard = StandardNaming.INIT)
    void set(Collection<T> input) {
      currentState = BuilderState.FULL;
      set = ImmutableSet.copyOf(input);
      builder = null;
    }

    @Encoding.Build
    ImmutableSet<T> build() {
      switch (currentState) {
        case GROWABLE:
          return builder.build();
        case FULL:
          return set;
        case EMPTY:
        default:
          return ImmutableSet.of();
      }
    }

  }
}
