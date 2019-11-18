package com.hubspot.immutable.collection.encoding;

import java.util.Collection;
import java.util.List;

import org.immutables.encode.Encoding;
import org.immutables.encode.Encoding.Naming;
import org.immutables.encode.Encoding.StandardNaming;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;

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

  @Encoding.Of
  static <T> ImmutableList<T> of(Collection<? extends T> input) {
    return ImmutableList.copyOf(input);
  }

  @Encoding.Builder
  @SuppressWarnings("UnstableApiUsage")
  static class Builder<T> {

    private ImmutableList<T> list = null;
    private ImmutableList.Builder<T> builder = null;

    @Encoding.Init
    @Encoding.Naming(standard = StandardNaming.ADD)
    void add(T... element) {
      if (builder != null) {
        builder.add(element);
      } else if (list != null) {
        builder = ImmutableList.<T>builderWithExpectedSize(list.size() + 1)
            .addAll(list)
            .add(element);

        list = null;
      } else {
        builder = ImmutableList.builder();
        builder.add(element);
      }
    }

    @Encoding.Init
    @Encoding.Naming(standard = StandardNaming.ADD_ALL)
    void addAll(Iterable<T> elements) {
      if (builder != null) {
        builder.addAll(elements);
      } else if (list != null) {
        int additionalSize = 0;
        if (elements instanceof Collection) {
          additionalSize = ((Collection<T>) elements).size();
        }

        builder = ImmutableList.<T>builderWithExpectedSize(list.size() + additionalSize)
            .addAll(list)
            .addAll(elements);

        list = null;
      } else {
        if (elements instanceof ImmutableCollection) {
          set(((ImmutableCollection<T>) elements));
        } else if (elements instanceof Collection) {
          builder = ImmutableList.builderWithExpectedSize(((Collection<T>) elements).size());
          builder.addAll(elements);
        } else {
          builder = ImmutableList.builder();
          builder.addAll(elements);
        }
      }
    }

    @Encoding.Init
    @Encoding.Copy
    @Naming(standard = StandardNaming.INIT)
    void set(Collection<T> input) {
      list = ImmutableList.copyOf(input);
      builder = null;
    }

    @Encoding.Build
    ImmutableList<T> build() {
      if (builder != null) {
        return builder.build();
      } else if (list != null) {
        return list;
      } else {
        return ImmutableList.of();
      }
    }
  }
}
