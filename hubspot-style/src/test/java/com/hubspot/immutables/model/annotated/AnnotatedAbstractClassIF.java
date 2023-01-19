package com.hubspot.immutables.model.annotated;

import org.immutables.value.Value;

import com.hubspot.immutables.style.HubSpotStyle;

@Value.Immutable
@HubSpotStyle
@InheritedAnnotation("type")
public abstract class AnnotatedAbstractClassIF {

  @InheritedAnnotation("method")
  public abstract int getAnnotated();

  public abstract int getUnannotated();
}
