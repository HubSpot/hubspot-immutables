package com.hubspot.immutables.model.annotated;

import com.hubspot.immutables.style.HubSpotStyle;
import org.immutables.value.Value;

@Value.Immutable
@HubSpotStyle
@InheritedAnnotation("type")
public interface AnnotatedInterfaceIF {
  @InheritedAnnotation("method")
  int getAnnotated();

  int getUnannotated();
}
