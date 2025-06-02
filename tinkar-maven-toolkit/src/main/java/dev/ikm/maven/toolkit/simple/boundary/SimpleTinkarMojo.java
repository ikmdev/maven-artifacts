package dev.ikm.maven.toolkit.simple.boundary;

import dev.ikm.maven.toolkit.TinkarMojo;
import dev.ikm.maven.toolkit.DatastoreProxy;
import dev.ikm.tinkar.common.service.ServiceProperties;
import dev.ikm.tinkar.entity.Entity;

/**
 * Abstract class that runs non-isolated tinkar mojos
 */
public abstract class SimpleTinkarMojo extends TinkarMojo {

	@Override
	public void execute(){
		getLog().info("SimpleTinkarMojo: " + ServiceProperties.jvmUuid());
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
