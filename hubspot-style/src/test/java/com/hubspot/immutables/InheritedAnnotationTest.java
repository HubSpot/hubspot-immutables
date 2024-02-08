package com.hubspot.immutables;

import static org.assertj.core.api.Assertions.assertThat;

import com.hubspot.immutables.model.annotated.AnnotatedAbstractClass;
import com.hubspot.immutables.model.annotated.AnnotatedImmutableStyleInterface;
import com.hubspot.immutables.model.annotated.AnnotatedInterface;
import com.hubspot.immutables.model.annotated.AnnotatedModifiableInterface;
import com.hubspot.immutables.model.annotated.InheritedAnnotation;
import org.junit.Test;

public class InheritedAnnotationTest {

  @Test
  public void itInherits() throws Exception {
    checkAnnotations(AnnotatedInterface.class);
    checkAnnotations(AnnotatedAbstractClass.class);
    checkAnnotations(AnnotatedImmutableStyleInterface.class);
    checkAnnotations(AnnotatedModifiableInterface.class);
  }

  private void checkAnnotations(Class<?> clazz) throws NoSuchMethodException {
    InheritedAnnotation classAnnotation = clazz.getAnnotation(InheritedAnnotation.class);
    assertThat(classAnnotation).as("%s is annotated", clazz.getSimpleName()).isNotNull();
    assertThat(classAnnotation.value())
      .as("%s has correct annotation value", clazz.getSimpleName())
      .isEqualTo("type");

    InheritedAnnotation methodAnnotation = clazz
      .getMethod("getAnnotated")
      .getAnnotation(InheritedAnnotation.class);
    assertThat(methodAnnotation)
      .as("%s#getAnnotated() is annotated", clazz.getSimpleName())
      .isNotNull();
    assertThat(methodAnnotation.value())
      .as("%s#getAnnotated() has correct annotation value", clazz.getSimpleName())
      .isEqualTo("method");

    assertThat(clazz.getMethod("getUnannotated").getAnnotation(InheritedAnnotation.class))
      .as("%s#getUnannotated() is not annotated", clazz.getSimpleName())
      .isNull();
  }
}
