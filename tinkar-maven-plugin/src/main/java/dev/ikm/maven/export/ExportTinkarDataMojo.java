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
package dev.ikm.maven.export;

import dev.ikm.maven.toolkit.SimpleTinkarMojo;
import dev.ikm.tinkar.entity.export.ExportEntitiesToProtobufFile;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;

@Mojo(name = "export-tinkar-data", defaultPhase = LifecyclePhase.PACKAGE)
public class ExportTinkarDataMojo extends SimpleTinkarMojo {

    @Parameter(name = "exportDirectory", defaultValue = "${project.build.directory}")
    File exportDirectory;

    @Parameter(name = "fileName", defaultValue = "tinkar-export.zip")
    File fileName;

    @Override
    public void run() {
        File exportFile = exportDirectory.toPath().resolve(fileName.getName()).toFile();
        var exportTask = new ExportEntitiesToProtobufFile(exportFile);
        exportTask.compute();
    }
}
