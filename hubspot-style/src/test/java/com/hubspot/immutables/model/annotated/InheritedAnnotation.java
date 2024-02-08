package com.hubspot.immutables.model.annotated;

import com.hubspot.immutables.style.ImmutableInherited;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@ImmutableInherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface InheritedAnnotation {
  String value();
}
