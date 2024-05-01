package com.hubspot.immutables.encoding;

import com.hubspot.immutables.utils.WireSafeEnum;
import org.immutables.encode.Encoding;
import org.immutables.encode.Encoding.Naming;
import org.immutables.encode.Encoding.StandardNaming;

@Encoding
class WireSafeEnumEncoding<T extends Enum<T>> {

  @Encoding.Impl
  private WireSafeEnum<T> field;

  @Encoding.Expose
  @Naming(standard = StandardNaming.GET)
  WireSafeEnum<T> getValue() {
    return field;
  }

  @Encoding.Copy
  @Naming(standard = StandardNaming.WITH)
  WireSafeEnum<T> withValue(T value) {
    return WireSafeEnum.of(value);
  }

  @Encoding.Init
  @Encoding.Copy
  @Naming(standard = StandardNaming.WITH)
  WireSafeEnum<T> withWireSafeValue(WireSafeEnum<T> value) {
    return value;
  }

  @Encoding.Builder
  static class Builder<T extends Enum<T>> {

    private WireSafeEnum<T> fieldValue;

    @Encoding.Init
    @Naming(standard = StandardNaming.INIT)
    void setValue(T value) {
      fieldValue = WireSafeEnum.of(value);
    }

    @Encoding.Init
    @Encoding.Copy
    @Naming(standard = StandardNaming.INIT)
    void setWireSafeValue(WireSafeEnum<T> value) {
      fieldValue = value;
    }

    @Encoding.IsInit
    boolean getIsSet() {
      return fieldValue != null;
    }

    @Encoding.Build
    WireSafeEnum<T> build() {
      return fieldValue;
    }
  }
}
