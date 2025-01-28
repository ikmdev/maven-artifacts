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
import java.util.List;

@Mojo(name = "load-data",requiresDependencyResolution = ResolutionScope.RUNTIME_PLUS_SYSTEM, defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class LoadDataMojo extends SimpleTinkarMojo {

    @Parameter(name = "dataFiles", required = true)
    private List<File> dataFiles;

    @Override
    public void run() {
        if (dataFiles.isEmpty()) {
            throw new RuntimeException("No data found to load");
        }
        dataFiles.forEach(file -> {
            var loadTask = new LoadEntitiesFromProtobufFile(file);
            loadTask.compute();
        });
    }
}
