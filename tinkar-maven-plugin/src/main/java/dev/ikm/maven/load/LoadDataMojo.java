/*
 * Copyright © 2015 Integrated Knowledge Management (support@ikm.dev)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.ikm.maven.load;

import dev.ikm.maven.toolkit.SimpleTinkarMojo;
import dev.ikm.tinkar.entity.load.LoadEntitiesFromProtobufFile;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.shared.model.fileset.FileSet;
import org.apache.maven.shared.model.fileset.util.FileSetManager;

import java.io.File;

@Mojo(name = "load-data", requiresDependencyResolution = ResolutionScope.RUNTIME_PLUS_SYSTEM, defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class LoadDataMojo extends SimpleTinkarMojo {

    private final FileSetManager fileSetManager = new FileSetManager();

    @Parameter(name= "filesets")
    private FileSet[] filesets;

    @Parameter(name = "fileset")
    private FileSet fileset;

    @Override
    public void run() {
        if (filesets != null) {
            for (FileSet fileSet : filesets) {
               loadFileset(fileSet);
            }
        }
        if (fileset != null) {
           loadFileset(fileset);
        }
    }

    private void loadFileset(FileSet fileset) {
        for (String includeFile : fileSetManager.getIncludedFiles(fileset)) {
            File pbZip = new File(fileset.getDirectory(), includeFile);
            var loadTask = new LoadEntitiesFromProtobufFile(pbZip);
            loadTask.compute();
        }
    }
}
