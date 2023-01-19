package com.hubspot.immutables.model.annotated;

import org.immutables.value.Value;

import com.hubspot.immutables.style.HubSpotImmutableStyle;

@Value.Immutable
@HubSpotImmutableStyle
@InheritedAnnotation("type")
public interface AnnotatedImmutableStyleInterfaceIF {
  @InheritedAnnotation("method")
  int getAnnotated();

  int getUnannotated();
}
