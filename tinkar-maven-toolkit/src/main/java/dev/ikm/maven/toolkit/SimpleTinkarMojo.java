package dev.ikm.maven.toolkit;

public abstract class SimpleTinkarMojo extends TinkarMojo {

	@Override
	public void execute(){
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

	/**
	 * Runs the plugin in code. This is a convenience method to handle logging and exception cases.
	 * @throws Exception
	 */
	public abstract void run() throws Exception;
}
