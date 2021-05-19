package com.hubspot.immutables.style;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.immutables.value.Value;
import org.immutables.value.Value.Style.ImplementationVisibility;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.hubspot.immutable.collection.encoding.ImmutableListEncodingEnabled;
import com.hubspot.immutable.collection.encoding.ImmutableMapEncodingEnabled;
import com.hubspot.immutable.collection.encoding.ImmutableSetEncodingEnabled;
import com.hubspot.immutables.validation.InvalidImmutableStateException;

@Target({ ElementType.PACKAGE, ElementType.TYPE})
@Retention(RetentionPolicy.CLASS) // Make it class retention for incremental compilation
@JsonSerialize
@Value.Style(
    get = {"is*", "get*"}, // Detect 'get' and 'is' prefixes in accessor methods
    init = "set*", // Builder initialization methods will have 'set' prefix
    typeAbstract = {"Abstract*", "*IF"}, // 'Abstract' prefix, and 'IF' suffix, will be detected and trimmed
    typeImmutable = "*", // No prefix or suffix for generated immutable type
    throwForInvalidImmutableState = InvalidImmutableStateException.class,
    optionalAcceptNullable = true, // allow for an Optional<T> to have a setter that takes a null value of T
    forceJacksonPropertyNames = false, // otherwise we can't use RosettaNamingStrategies
    visibility = ImplementationVisibility.SAME // Generated class will have the same visibility as the abstract class/interface)
)
@ImmutableSetEncodingEnabled
@ImmutableListEncodingEnabled
@ImmutableMapEncodingEnabled
public @interface HubSpotImmutableStyle {}
