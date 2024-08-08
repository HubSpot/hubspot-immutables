package com.hubspot.immutables.style;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.hubspot.immutable.collection.encoding.ImmutableListEncodingEnabled;
import com.hubspot.immutable.collection.encoding.ImmutableMapEncodingEnabled;
import com.hubspot.immutable.collection.encoding.ImmutableSetEncodingEnabled;
import com.hubspot.immutables.validation.InvalidImmutableStateException;
import com.hubspot.rosetta.annotations.RosettaAnnotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.immutables.value.Value;
import org.immutables.value.Value.Style.ImplementationVisibility;

/**
 * This style is preferred over {@link HubSpotStyle} because it enforces use of guava's Immutable* collection types. Using these types in immutables is more efficient than using JDK default collections, because immutable collections allow us to make copies of immutables without copying whole collections and avoid copies when calling {@code build()} on immutable builders.
 * <br>
 * This style does however introduce some behavioral differences to the standard style, the most major of these being:
 * <ul>
 *   <li>You must define methods as returning `ImmutableList` etc. not `List`</li>
 *   <li>Inserting duplicate map keys will throw an exception.</li>
 *   <li>The returned values from various getters is actually immutable, and mutations will throw {@link UnsupportedOperationException}</li>
 * </ul>
 */
@Target({ ElementType.PACKAGE, ElementType.TYPE })
@Retention(RetentionPolicy.CLASS) // Make it class retention for incremental compilation
@JsonSerialize
@Value.Style(
  get = { "is*", "get*" }, // Detect 'get' and 'is' prefixes in accessor methods
  init = "set*", // Builder initialization methods will have 'set' prefix
  typeAbstract = { "Abstract*", "*IF" }, // 'Abstract' prefix, and 'IF' suffix, will be detected and trimmed
  typeImmutable = "*", // No prefix or suffix for generated immutable type
  throwForInvalidImmutableState = InvalidImmutableStateException.class,
  optionalAcceptNullable = true, // allow for an Optional<T> to have a setter that takes a null value of T
  forceJacksonPropertyNames = false, // otherwise we can't use RosettaNamingStrategies
  visibility = ImplementationVisibility.SAME, // Generated class will have the same visibility as the abstract class/interface)
  passAnnotations = { ImmutableInherited.class, RosettaAnnotation.class },
  redactedMask = "**REDACTED**"
)
@ImmutableSetEncodingEnabled
@ImmutableListEncodingEnabled
@ImmutableMapEncodingEnabled
public @interface HubSpotImmutableStyle {
}
