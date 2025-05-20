package dev.ikm.maven.fork;

import dev.ikm.maven.toolkit.isolated.boundary.Isolate;
import dev.ikm.maven.toolkit.isolated.boundary.IsolatedTinkarMojo;
import dev.ikm.tinkar.common.service.ServiceProperties;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.model.fileset.FileSet;
import org.apache.maven.shared.model.fileset.util.FileSetManager;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Mojo(name = "execFork", defaultPhase = LifecyclePhase.PACKAGE)
public class ExecFork extends IsolatedTinkarMojo {

	@Isolate
	@Parameter(name = "debug", defaultValue = "false")
	private boolean debug;

	@Parameter(readonly = true, defaultValue = "${plugin.artifacts}")
	protected List<Artifact> deps;

	@Isolate
	@Parameter(name = "myFile", defaultValue = "${project.build.directory}")
	private File myFile;

	@Parameter(name = "fileset")
	private FileSet fileset;

	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	private MavenProject projectTwo;

	@Isolate
	private Set<File> filesFromFileSet = new HashSet<>();

	@Override
	public void handleIsolatedFields() {
		FileSetManager fileSetManager = new FileSetManager();
		for (String filePath : fileSetManager.getIncludedFiles(fileset)) {
			filesFromFileSet.add(new File(filePath));
		}
	}

	@Override
	public void run() {
		 System.out.println("exec-fork run(): " + ServiceProperties.jvmUuid());
		 System.out.println("exec-fork debug field: " + debug);
		 System.out.println("exec-fork myFile field: " + myFile);
		 System.out.println("exec-fork filesFromFileSet: " + filesFromFileSet.iterator().next());
	}
}
