package dev.ikm.maven.toolkit;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;

public abstract class TinkarMojo extends AbstractMojo {

	@Parameter(name = "dataStore", defaultValue = "${project.build.directory}/datastore")
	protected File dataStore;

}
