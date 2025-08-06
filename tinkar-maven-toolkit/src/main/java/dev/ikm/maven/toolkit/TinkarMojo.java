package dev.ikm.maven.toolkit;

import dev.ikm.maven.toolkit.isolated.boundary.Isolate;
import dev.ikm.maven.toolkit.isolated.controller.IsolationDispatcher;
import dev.ikm.maven.toolkit.isolated.controller.IsolationReceiver;
import dev.ikm.tinkar.common.service.ServiceProperties;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

/**
 * Abstract Mojo that tries to standardize via inheritance where a Tinkar Mojo stores it's data (aka datastore)
 */
public abstract class TinkarMojo extends AbstractMojo implements Runnable {

	private final static Logger LOG = LoggerFactory.getLogger(TinkarMojo.class.getSimpleName());

	@Parameter(name = "isolate", defaultValue = "true")
	public boolean isolate;

	@Parameter(readonly = true, defaultValue = "${plugin.artifacts}")
	protected List<Artifact> pluginDependencies;

	@Isolate
	@Parameter(name = "targetDir", defaultValue = "${project.build.directory}")
	public File targetDir;

	@Isolate
	@Parameter(name = "dataStore", defaultValue = "${project.build.directory}/datastore")
	public File dataStore;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (isolate) {
			getLog().info("IsolatedTinkarMojo: " + ServiceProperties.jvmUuid());

			handleIsolatedFields();

			IsolationDispatcher isolationDispatcher = new IsolationDispatcher.Builder()
					.clazz(this)
					.buildDirectory(targetDir.toPath())
					.canonicalName(getClass().getCanonicalName())
					.dependencies(pluginDependencies.stream().map(Artifact::getFile).toList())
					.build();
			isolationDispatcher.dispatch();
		} else {
			try (DatastoreProxy datastoreProxy = new DatastoreProxy(dataStore)) {
				if (datastoreProxy.running()) {
					run();
				} else {
					throw new RuntimeException("Datastore not running");
				}
			} catch (Exception e) {
				getLog().error(e);
				throw new RuntimeException(e.getMessage(), e);
			}
		}
	}

	/**
	 * Handle any Maven injected Parameters that aren't serializable
	 */
	public abstract void handleIsolatedFields();

	/**
	 * Main method used to re-run a new instance of an isolated mojo in a separate JVM
	 * @param args - Isolated Fields Directory and Canonical Class Name
	 * @throws Exception
	 */
	public static void main(String... args) throws Exception {
		LOG.info("isolate: " + ServiceProperties.jvmUuid());
		IsolationReceiver isolationReceiver = new IsolationReceiver(args[0], args[1]);
		TinkarMojo tinkarMojo= isolationReceiver.runnableInstance();
		LOG.info("isolated directory: " + args[0]);
		LOG.info("class path: " + args[1]);

		try (DatastoreProxy datastoreProxy = new DatastoreProxy(tinkarMojo.dataStore)) {
			if (datastoreProxy.running()) {
				tinkarMojo.run();
			} else {
				throw new RuntimeException("Datastore not running");
			}
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}
}
