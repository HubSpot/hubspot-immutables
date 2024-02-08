package com.hubspot.immutables.model.annotated;

import com.hubspot.immutables.style.HubSpotModifiableStyle;
import org.immutables.value.Value;

@Value.Immutable
@HubSpotModifiableStyle
@InheritedAnnotation("type")
public interface AnnotatedModifiableInterfaceIF {
  @InheritedAnnotation("method")
  int getAnnotated();

  int getUnannotated();
}
