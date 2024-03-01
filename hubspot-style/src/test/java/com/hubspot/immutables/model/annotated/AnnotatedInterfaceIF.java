package com.hubspot.immutables.model.annotated;

import org.immutables.value.Value;

import com.hubspot.immutables.style.HubSpotStyle;

@Value.Immutable
@HubSpotStyle
@InheritedAnnotation("type")
public interface AnnotatedInterfaceIF {

  @InheritedAnnotation("method")
  int getAnnotated();

  int getUnannotated();
}
