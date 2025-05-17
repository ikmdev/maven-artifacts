package dev.ikm.maven.toolkit.boundary;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;

public abstract class TinkarMojo extends AbstractMojo implements Runnable {

	@Isolate
	@Parameter(name = "dataStore", defaultValue = "${project.build.directory}/datastore")
	public File dataStore;

}
