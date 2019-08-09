package com.hubspot.immutable.collection.encoding.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.immutables.value.Value;
import org.immutables.value.Value.Style.ImplementationVisibility;

import com.hubspot.immutable.collection.encoding.ImmutableListEncodingEnabled;
import com.hubspot.immutable.collection.encoding.ImmutableMapEncodingEnabled;
import com.hubspot.immutable.collection.encoding.ImmutableSetEncodingEnabled;

@Target({ ElementType.PACKAGE, ElementType.TYPE})
@Retention(RetentionPolicy.CLASS) // Make it class retention for incremental compilation
@Value.Style(
    get = {"is*", "get*"}, // Detect 'get' and 'is' prefixes in accessor methods
    init = "set*", // Builder initialization methods will have 'set' prefix
    typeAbstract = {"Abstract*", "*IF"}, // 'Abstract' prefix, and 'IF' suffix, will be detected and trimmed
    typeImmutable = "*", // No prefix or suffix for generated immutable type
    visibility = ImplementationVisibility.SAME)
@ImmutableMapEncodingEnabled
@ImmutableSetEncodingEnabled
@ImmutableListEncodingEnabled
public @interface TestStyle {
}
