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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

@Mojo(name = "load-data", requiresDependencyResolution = ResolutionScope.RUNTIME_PLUS_SYSTEM, defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class LoadArtifactDataMojo extends SimpleTinkarMojo {

    @Parameter(name = "dataFiles")
    private List<File> dataFiles;

    @Parameter(name = "dataDirectory")
    private File dataDirectory;

    @Override
    public void run() {
        if (dataFiles != null) {
            dataFiles.forEach(file -> {
                if (!file.getName().endsWith("pb.zip")) {
                    getLog().warn("Unsupported file type: " + file.getName());
                }
                loadEntities(file);
            });
        }
        if (dataDirectory != null) {
            try(Stream<Path> paths = Files.walk(dataDirectory.toPath())) {
                paths.forEach(path -> {
                    loadEntities(path.toFile());
                });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void loadEntities(File file) {
        var loadTask = new LoadEntitiesFromProtobufFile(file);
        loadTask.compute();
    }
}
