package dev.ikm.maven.toolkit;

import dev.ikm.maven.toolkit.isolated.boundary.Isolate;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;

/**
 * Abstract Mojo that tries to standardize via inheritance where a Tinkar Mojo stores it's data (aka datastore)
 */
public abstract class TinkarMojo extends AbstractMojo implements Runnable {

	@Isolate
	@Parameter(name = "dataStore", defaultValue = "${project.build.directory}/datastore")
	public File dataStore;
}
