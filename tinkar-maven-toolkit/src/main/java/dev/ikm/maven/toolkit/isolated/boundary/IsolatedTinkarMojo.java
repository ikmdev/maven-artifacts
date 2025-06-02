package dev.ikm.maven.toolkit.isolated.boundary;

import dev.ikm.maven.toolkit.DatastoreProxy;
import dev.ikm.maven.toolkit.TinkarMojo;
import dev.ikm.maven.toolkit.isolated.controller.IsolationDispatcher;
import dev.ikm.maven.toolkit.isolated.controller.IsolationReceiver;
import dev.ikm.tinkar.common.service.ServiceProperties;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

/**
 * This class enables a mojo to be dynamically inject via Maven Parameters and then ran on a seperate JVM instance
 */
public abstract class IsolatedTinkarMojo extends TinkarMojo {

	private final static Logger LOG = LoggerFactory.getLogger(IsolatedTinkarMojo.class.getSimpleName());

	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	private MavenProject project;

	@Parameter(readonly = true, defaultValue = "${plugin.artifacts}")
	protected List<Artifact> pluginDependencies;

	@Parameter(name = "targetDir", defaultValue = "${project.build.directory}")
	public File targetDir;

	@Override
	public void execute() {
		getLog().info("IsolatedTinkarMojo: " + ServiceProperties.jvmUuid());

		handleIsolatedFields();

		IsolationDispatcher isolationDispatcher = new IsolationDispatcher.Builder()
				.clazz(this)
				.buildDirectory(targetDir.toPath())
				.canonicalName(getClass().getCanonicalName())
				.dependencies(pluginDependencies.stream().map(Artifact::getFile).toList())
				.build();
		isolationDispatcher.dispatch();
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
		LOG.info("fork: " + ServiceProperties.jvmUuid());
		IsolationReceiver isolationReceiver = new IsolationReceiver(args[0], args[1]);
		IsolatedTinkarMojo isolatedTinkarMojo = isolationReceiver.runnableInstance();
		LOG.info("isolated directory: " + args[0]);
		LOG.info("class path: " + args[1]);

		try (DatastoreProxy datastoreProxy = new DatastoreProxy(isolatedTinkarMojo.dataStore)) {
			if (datastoreProxy.running()) {
				isolatedTinkarMojo.run();
			} else {
				throw new RuntimeException("Datastore not running");
			}
		} catch (Exception e) {
			System.err.println(e);
			throw new RuntimeException(e.getMessage(), e);
		}

		Thread.sleep(3000); //FIXME-aks8m: This is to allow time for Lucene to release file lock
	}
}
