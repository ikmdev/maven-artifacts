package dev.ikm.maven.toolkit.isolated.boundary;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotate Mojo Parameters and class fields that should inject into a new instance of the Mojo in a separate JVM
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Isolate {
}
