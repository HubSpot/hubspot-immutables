package com.hubspot.immutables.utils;

/**
 * This interface is to provide support for cases where the json value in one system
 * is different from the json value in another system.
 * For example, lower case vs upper case.
 * This adds some of the functionality that a @JsonCreator-annotated method would allow
 */
public interface WireSafeEnumAlternativeJsonInputValue {
    String getExtraJsonInputValue();
}
