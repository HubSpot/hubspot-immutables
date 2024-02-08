package com.hubspot.immutables.model.annotated;

import com.hubspot.immutables.style.HubSpotImmutableStyle;
import org.immutables.value.Value;

@Value.Immutable
@HubSpotImmutableStyle
@InheritedAnnotation("type")
public interface AnnotatedImmutableStyleInterfaceIF {
  @InheritedAnnotation("method")
  int getAnnotated();

  int getUnannotated();
}
