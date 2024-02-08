package com.hubspot.immutables.model.annotated;

import com.hubspot.immutables.style.HubSpotStyle;
import org.immutables.value.Value;

@Value.Immutable
@HubSpotStyle
@InheritedAnnotation("type")
public abstract class AnnotatedAbstractClassIF {

  @InheritedAnnotation("method")
  public abstract int getAnnotated();

  public abstract int getUnannotated();
}
