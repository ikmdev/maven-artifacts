package dev.ikm.maven.load;

import dev.ikm.maven.toolkit.isolated.boundary.Isolate;
import dev.ikm.maven.toolkit.isolated.boundary.IsolatedTinkarMojo;
import dev.ikm.tinkar.entity.load.LoadEntitiesFromProtobufFile;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.shared.model.fileset.FileSet;
import org.apache.maven.shared.model.fileset.util.FileSetManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Mojo(name = "load-data-isolated", requiresDependencyResolution = ResolutionScope.RUNTIME_PLUS_SYSTEM, defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class LoadDataIsolatedMojo extends IsolatedTinkarMojo {

	private final FileSetManager fileSetManager = new FileSetManager();

	@Parameter(name= "filesets")
	private FileSet[] filesets;

	@Parameter(name = "fileset")
	private FileSet fileset;

	@Isolate
	private List<File> filesToLoad = new ArrayList<>();

	@Override
	public void handleIsolatedFields() {
		if (filesets != null) {
			for (FileSet innerFileSet : filesets) {
				for (String includeFile : fileSetManager.getIncludedFiles(innerFileSet)) {
					File pbZip = new File(innerFileSet.getDirectory(), includeFile);
					filesToLoad.add(pbZip);
				}
			}
		}
		if (fileset != null) {
			for (String includeFile : fileSetManager.getIncludedFiles(fileset)) {
				File pbZip = new File(fileset.getDirectory(), includeFile);
				filesToLoad.add(pbZip);
			}
		}
	}

	@Override
	public void run() {
		filesToLoad.forEach(file -> {
			var loadTask = new LoadEntitiesFromProtobufFile(file);
			loadTask.compute();
		});
	}
}
