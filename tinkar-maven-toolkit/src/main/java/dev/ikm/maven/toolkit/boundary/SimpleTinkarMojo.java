package dev.ikm.maven.toolkit.boundary;

import dev.ikm.maven.toolkit.controller.DatastoreProxy;
import dev.ikm.tinkar.common.service.ServiceProperties;
import dev.ikm.tinkar.entity.Entity;

public abstract class SimpleTinkarMojo extends TinkarMojo {

	public static void main(String... args) {

	}

	@Override
	public void execute(){
		getLog().info("execute: " + ServiceProperties.jvmUuid());
		try (DatastoreProxy datastoreProxy = new DatastoreProxy(dataStore)) {
			if (datastoreProxy.running()) {
				Entity.provider().beginLoadPhase();
				run();
				Entity.provider().endLoadPhase();
			} else {
				throw new RuntimeException("Datastore not running");
			}
		} catch (Exception e) {
			getLog().error(e);
			throw new RuntimeException(e.getMessage(), e);
		}
	}
}
