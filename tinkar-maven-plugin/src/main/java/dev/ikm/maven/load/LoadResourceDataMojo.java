package dev.ikm.maven.load;

import dev.ikm.maven.toolkit.SimpleTinkarMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

@Mojo(name = "load-resource-data", requiresDependencyResolution = ResolutionScope.RUNTIME_PLUS_SYSTEM, defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class LoadResourceDataMojo extends SimpleTinkarMojo {


    @Override
    public void run() throws Exception {

        LoadResourceDataMojo.class.getClassLoader().getResourceAsStream("/pb");
    }
}
