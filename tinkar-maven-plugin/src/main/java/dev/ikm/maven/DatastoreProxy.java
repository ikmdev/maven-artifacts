package dev.ikm.maven;

import dev.ikm.tinkar.common.service.CachingService;
import dev.ikm.tinkar.common.service.PrimitiveData;
import dev.ikm.tinkar.common.service.ServiceKeys;
import dev.ikm.tinkar.common.service.ServiceProperties;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class DatastoreProxy implements Closeable {


    public void start() {
        File datastoreDirectory = new File(System.getProperty("project.build.directory"), "datastore");
        start(datastoreDirectory);
    }

    public void start(File datastoreDirectory) {
        if (!datastoreDirectory.exists()) {
            try {
                Files.createDirectories(datastoreDirectory.toPath());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        CachingService.clearAll();
        ServiceProperties.set(ServiceKeys.DATA_STORE_ROOT, datastoreDirectory);
        PrimitiveData.selectControllerByName("Open SpinedArrayStore");
        PrimitiveData.start();
    }

    @Override
    public void close() {
        PrimitiveData.stop();
    }
}
