package com.hubspot.immutable.collection.encoding;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.immutables.encode.Encoding;
import org.immutables.encode.Encoding.Naming;
import org.immutables.encode.Encoding.StandardNaming;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

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
  ImmutableSet<T> withCollectionVarargs(T... elements) {
    return ImmutableSet.copyOf(elements);
  }


  @Encoding.Copy
  @Naming(standard = StandardNaming.WITH)
  ImmutableSet<T> withCollection(Iterable<? extends T> elements) {
    return ImmutableSet.copyOf(elements);
  }

  @Encoding.Of
  static <T> ImmutableSet<T> of(Collection<? extends T> input) {
    return ImmutableSet.copyOf(input);
  }

  @Encoding.Builder
  @SuppressWarnings("UnstableApiUsage")
  static class Builder<T> {

    private ImmutableSet<T> set = null;
    private ImmutableSet.Builder<T> builder = null;

    @Encoding.Init
    @Naming(standard = StandardNaming.ADD)
    void add(T... element) {
      if (builder != null) {
        builder.add(element);
      } else if (set != null) {
        builder = ImmutableSet.<T>builderWithExpectedSize(set.size() + 1)
            .addAll(set)
            .add(element);

        set = null;
      } else {
        builder = ImmutableSet.builder();
        builder.add(element);
      }
    }

    @Encoding.Init
    @Naming(standard = StandardNaming.ADD_ALL)
    void addAll(Iterable<? extends T> elements) {
      if (builder != null) {
        builder.addAll(elements);
      } else if (set != null) {
        int additionalSize = 0;
        if (elements instanceof Collection) {
          additionalSize = ((Collection<? extends T>) elements).size();
        }

        builder = ImmutableSet.<T>builderWithExpectedSize(set.size() + additionalSize)
            .addAll(set)
            .addAll(elements);

        set = null;
      } else {
        if (elements instanceof ImmutableCollection) {
          set(elements);
        } else if (elements instanceof Collection) {
          builder = ImmutableSet.builderWithExpectedSize(((Collection<? extends T>) elements).size());
          builder.addAll(elements);
        } else {
          builder = ImmutableSet.builder();
          builder.addAll(elements);
        }
      }
    }

    @Encoding.Init
    @Encoding.Copy
    @Naming(standard = StandardNaming.INIT)
    void set(Iterable<? extends T> input) {
      set = ImmutableSet.copyOf(input);
      builder = null;
    }

    @Encoding.IsInit
    boolean isSet() {
      return set != null || builder != null;
    }

    @Encoding.Build
    ImmutableSet<T> build() {
      if (builder != null) {
        return builder.build();
      } else if (set != null) {
        return set;
      } else {
        return ImmutableSet.of();
      }
    }
  }
}
