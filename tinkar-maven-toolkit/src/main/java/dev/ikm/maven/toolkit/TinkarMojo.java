package dev.ikm.maven.toolkit;

import dev.ikm.maven.toolkit.isolated.boundary.Isolate;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;

public abstract class TinkarMojo extends AbstractMojo implements Runnable {

	@Isolate
	@Parameter(name = "dataStore", defaultValue = "${project.build.directory}/datastore")
	public File dataStore;
}
