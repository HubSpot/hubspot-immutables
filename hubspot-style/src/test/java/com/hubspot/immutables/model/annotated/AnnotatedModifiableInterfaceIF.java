package com.hubspot.immutables.model.annotated;

import org.immutables.value.Value;

import com.hubspot.immutables.style.HubSpotModifiableStyle;

@Value.Immutable
@HubSpotModifiableStyle
@InheritedAnnotation("type")
public interface AnnotatedModifiableInterfaceIF {

  @InheritedAnnotation("method")
  int getAnnotated();

  int getUnannotated();
}
