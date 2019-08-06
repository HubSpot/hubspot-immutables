package com.hubspot.immutable.collection.encoding;

import java.util.Collection;
import java.util.List;

import org.immutables.encode.Encoding;
import org.immutables.encode.Encoding.Naming;
import org.immutables.encode.Encoding.StandardNaming;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.hubspot.immutable.collection.encoding.util.BuilderState;

@Encoding
public class ImmutableListEncoding<T> {

  @Encoding.Impl
  private ImmutableList<T> field = ImmutableList.of();

  @Encoding.Expose
  ImmutableList<T> getImmutableList() {
    return field;
  }

  @Encoding.Expose
  List<T> getList() {
    return field;
  }

  @Encoding.Copy
  @Naming(standard = StandardNaming.WITH)
  ImmutableList<T> withCollection(Iterable<T> elements) {
    return ImmutableList.copyOf(elements);
  }

  @Encoding.Builder
  @SuppressWarnings("UnstableApiUsage")
  static class Builder<T> {

    private ImmutableList<T> list = null;
    private ImmutableList.Builder<T> builder = null;

    private BuilderState currentState = BuilderState.EMPTY;

    @Encoding.Init
    @Encoding.Naming(standard = StandardNaming.ADD)
    void add(T element) {
      switch (currentState) {
        case GROWABLE:
          builder.add(element);
          break;
        case FULL:
          builder = ImmutableList.<T>builderWithExpectedSize(list.size() + 1)
              .addAll(list)
              .add(element);

          list = null;
          break;
        case EMPTY:
          builder = ImmutableList.builder();
          builder.add(element);
          break;
      }

      currentState = BuilderState.GROWABLE;
    }

    @Encoding.Init
    @Encoding.Naming(standard = StandardNaming.ADD_ALL)
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

          builder = ImmutableList.<T>builderWithExpectedSize(list.size() + additionalSize)
              .addAll(list)
              .addAll(elements);

          list = null;
          break;
        case EMPTY:
          if (elements instanceof ImmutableCollection) {
            set(((ImmutableCollection<T>) elements));
            currentState = BuilderState.FULL;
            return;
          } else if (elements instanceof Collection) {
            builder = ImmutableList.builderWithExpectedSize(((Collection<T>) elements).size());
          } else {
            builder = ImmutableList.builder();
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
      list = ImmutableList.copyOf(input);
      builder = null;
    }

    @Encoding.Build
    ImmutableList<T> build() {
      switch (currentState) {
        case GROWABLE:
          return builder.build();
        case FULL:
          return list;
        case EMPTY:
        default:
          return ImmutableList.of();
      }
    }

  }
}
