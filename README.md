# hubspot-immutables

Using [Java Immutables](http://immutables.github.io/) at HubSpot. 

### Getting Started

Include the following in your `pom.xml`

```xml
<dependency>
  <groupId>com.fasterxml.jackson.core</groupId>
  <artifactId>jackson-annotations</artifactId>
</dependency>
<dependency>
  <groupId>com.hubspot.immutables</groupId>
  <artifactId>hubspot-style</artifactId>
</dependency>
<dependency>
  <groupId>com.hubspot.immutables</groupId>
  <artifactId>immutables-exceptions</artifactId>
</dependency>
<dependency>
  <groupId>org.immutables</groupId>
  <artifactId>value</artifactId>
  <scope>provided</scope>
</dependency>
```

if `com.google.code.findbugs:annotations` is included as a transitive dependency, it will be used during code generation; 
this means it will become a full dependency in your project.  If you wish to avoid it, then you'll need to exclude it
from any dependencies that pull it in, otherwise you'll need to explicitly add it.

Start writing POJOs as abstract classes:

```java
@Immutable
@HubSpotStyle
public abstract class AbstractWidget {
  public abstract int getPortalId();
  public abstract List<Integer> getFoo();
  public abstract Optional<String> getBar();
}
```

or interfaces:

```java
@Immutable
@HubSpotStyle
public interface WidgetIF {
  int getPortalId();
  List<Integer> getFoo();
  Optional<String> getBar();
}
```

both interfaces and Abstracts can have the name `AbstractAbc` or `AbcIF` to create classes named `Abc`:

```java
@Immutable
@HubSpotStyle
public interface AbstractWidget {
  int getPortalId();
  List<Integer> getFoo();
  Optional<String> getBar();
}
```

This will generate a class at compile time called `Widget`, which you can use as follows:

```java
Widget widget = Widget.builder()
  .setPortalId(53)
  .addFoo(1, 1, 2, 3)
  .setBar(Optional.of("Hello, World"))
  .build();
  
widget.getPortalId(); // 53
widget.getFoo(); // [1, 1, 2, 3]
widget.getBar(); // Optional.of("Hello, World")
```

If your project isn't in a compilable state, but still want to have access to the generated classes, compile only the
immutable interface in question (This is easily done in intellij by just opening the editor to the file and then
Build->"Compile blahblah.java" in the menu.

You can add derived properties via default methods:

```java
@Immutable
@HubSpotStyle
public abstract class AbstractWidget {
  public abstract int getPortalId();
  public abstract Set<Integer> getFoo();
  public abstract Optional<String> getBar();

  @Value.Derived
  public boolean portalIdInFoo() {
    return getFoo().contains(getPortalId());
  }
}
```

### A few things to note:
 1. Be sure to apply the `@HubSpotStyle`, which will do a few things in the background including
    1. Strip the `Abstract` prefix when naming the generated class
    2. Ensure that the getter methods are properly recognized
    3. Overall, we use it to make `Immutables` across HubSpot a more uniform experience
 2. *All* non `@Nullable` fields must be set before calling `build()`, otherwise a `InvalidImmutableStateException` will
 be thrown. The only exception to this rule are `Optional`s which are detected and defaulted to `empty()`.
 [See docs](http://immutables.github.io/immutable.html#optional-attributes).
 3. Unless explicitly specified (very much discouraged), `null`s are not allowed as field values.
 [See docs](http://immutables.github.io/immutable.html#nullable-attributes)
 4. `ImmutableConditions` methods rely on String.format for formatting, so if there are missing parameters, you may see
 interesting errors.

### Validation

You can also add validation to your built object by annotating a method (or multiple methods) with `@Value.Check`.
Validation methods should throw `InvalidImmutableStateException` on invalid input.  Message bodies parsed with Jackson
server-side will automatically throw a 400 Bad Request on upon encountering such an exception.  Elsewhere, the exception
will be thrown from the `.build()` call (or any `.with` calls).  To assist with validation, the `ImmutableConditions`
class is available; it is more or less a drop-in replacement for `Preconditions`/`PublicPreconditions`:

```java
@Immutable
@HubSpotStyle
public interface WidgetIF {
  int getWidgetId();
  Set<Integer> getFoo();
  Optional<String> getBar();

  @Value.Check
  default void widgetIdInFoo() {
    ImmutableConditions.checkValid(getFoo().contains(getWidgetId()), "widgetId %d must be in Foo!", getWidgetId());
  }
}
```

Methods annotated with `@Value.Check` will be executed on `.build()`, so once you have a built immutable object, you are
guaranteed that it is in a valid state.

### Jackson support

Jackson support is always enabled, since we use it extensively.  If you wish to deserialize your `AbstractWidget` or
`WidgetIF` as a `Widget` you will need to add `@JsonDeserialize(as=Widget.class)` to the annotations of your `WidgetIF`
or `AbstractWidget`.  However if you are only using your `AbstractWidget` or `WidgetIF` for code generation, and always
expecting to receive a `Widget`, you don't need to add any specific Json annotations.

`Rosetta` annotations are also passed through, so you can annotate your methods with the correct `Rosetta` annotations,
so that those properties are only ever used when working with the DB.

*NB: At first adding the annotations will cause your IDE to report an error, as the generated class doesn't exist yet.
All will be fine after the project is compiled. This has to do with the way annotation processing works.*

```java
@Immutable
@HubSpotStyle
@JsonDeserialize(as = Widget.class) // Only add this line if you expect to deserialize something to an AbstractWidget
public abstract class AbstractWidget {
  public abstract int getPortalId();
  public abstract Optional<String> getUserEmail();
  public abstract List<String> getWidgetStrings();
  @RosettaProperty("hubspot_id")
  public abstract int getHubSpotId();
}
```

### Egg Pattern

Sometimes you're going to want to accept an object in a `POST` that doesn't contain an ID. Rather than making your ID
`Optional`, or `@Nullable`, when you know that they are almost never missing, and you don't want to check for them.
That is when the Egg pattern is a good idea:

```java
public interface FooCore {
  String getName();
  Optional<String> getDescription();
}

@Immutable
@HubSpotStyle
public interface FooEggIF extends FooCore {
}

@Immutable
@HubSpotStyle
public interface FooIF extends FooCore {
  int getId();
}
```

With this pattern your `POST` methods can accept a `FooEgg`, and you can do something like this:
```java
@POST
public Foo createFoo(FooEgg toCreate) {
  int fooId = fooDao.create(toCreate);
  return Foo.builder()
      .from(toCreate)
      .setId(fooId)
      .build();
}
```

### Modifiable Pattern

This is similar to the Egg Pattern above, however you might want to use it if there are some fields that are required on
your object, but you might not always receive them from the client.  The Modifiable object does not perform any
validation until `toImmutable` is called.

```java
@HubSpotStyle
@Value.Immutable
@Value.Modifiable
public interface BarIF {
  String getName();
  String getDescription();
}

@PUT
@Path("bars/{name}/description")
public Bar updateDescription(@PathParam("name") String barName, ModifiableBar modifiableBar) {
  Bar bar = modifiableBar
      .setName(barName)
      .toImmutable();
  barDao.update(bar);
  return bar;
}
```

### Normalization

Since immutables 2.1.14 we've been able to use normalization with our classes.  This means that we can perform things
like `String.trim()` during build time. It is done by a special `@Value.Check` that returns the interface, and is always
named `normalize`. The key things to remember are
 1. Always do a test first to see if the instance is already normalized
 2. Validation has to happen in your `normalize` method
 3. Perform your validations after the normalization, so it only gets performed on your normalized instances
 4. There can **ONLY BE ONE** `@Value.Check` method

An example is below:
```java
@HubSpotStyle
@Value.Immutable
public interface NormalizedWidgetIF {
  BigDecimal getOptionalBigDecimal();
  String getCleanEmail();

  @Value.Check
  default NormalizedWidgetIF normalize() {
    if (!isNormalized()) {
        return NormalizedWidget.builder()
            .from(this)
            .setOptionalBigDecimal(getOptionalBigDecimal().map(bigDecimal -> bigDecimal.setScale(6, BigDecimal.ROUND_HALF_UP)))
            .setCleanEmail(getCleanEmail().trim())
            .build();
    }
    ImmutableConditions.checkValid(!getOptionalBigDecimal().isPresent() || 
                               getOptionalBigDecimal().get().compareTo(BigDecimal.ZERO) > 0,
                               "Optional BigDecimal must be greater than zero");
    return this;
  }

  @JsonIgnore
  @Value.Auxiliary
  default boolean isNormalized() {
    return getOptionalBigDecimal().map(bigDecimal -> bigDecimal.scale() == 6).orElse(true)
        && getCleanEmail().equals(getCleanEmail().trim());
  }
}
```

### Setup Intellij for `Immutables`
*Based on https://immutables.github.io/apt.html#intellij-idea*

To work with `Immutables` you must enable anotation processing. To do so globally follow these steps:

1. Go to *File &rarr; Other Settings &rarr; Default Settings*
2. Under *Compiler &rarr; Annotation Processors* toggle `Enable annotation processing`
3. Set `Store generated sources relative to:` to `Module content root`
4. Set the sources directories to `target/generated-sources/annotations` and
`target/generated-test-sources/test-annotations` respectively. 
5. Under *Build Tools &rarr; Maven &rarr; Importing* select `Generated sources folders: Detect automatically`.

You may have to restart IntelliJ (File &rarr; Invalidate Caches and Restart...) for it to pick up all the changes
successfully. You should now be able to use your generated classes, and build, test and run your code inside Intellij
as usual.

IntelliJ compiler seeing the generated classes, but not showing up for autocomplete/autoimport? Try a maven project
refresh.

### Help!
If you're finding that your immutable classes are not being generated; it's possible that there is an unreported error
in your code; in that case add this compiler configuration:
```xml
<compilerArguments>
  <Xmaxerrs>1000000</Xmaxerrs>
</compilerArguments>
```
so it looks like something below:
```xml
<build>
  <plugins>
    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-compiler-plugin</artifactId>
      <version>3.3</version>
      <configuration>
        <compilerVersion>1.8</compilerVersion>
        <source>1.8</source>
        <target>1.8</target>
        <compilerArguments>
          <Xmaxerrs>1000000</Xmaxerrs>
        </compilerArguments>
      </configuration>
    </plugin>
  </plugins>
</build>
```

Then run `mvn clean compile -X` and you'll get the errors out!
