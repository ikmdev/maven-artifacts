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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import dev.ikm.maven.export.config.ComponentFilter;
import dev.ikm.maven.toolkit.TinkarMojo;
import dev.ikm.maven.toolkit.isolated.boundary.Isolate;
import dev.ikm.tinkar.common.id.PublicId;
import dev.ikm.tinkar.entity.export.ExportEntitiesToProtobufFile;

@Mojo(name = "export-tinkar-data", defaultPhase = LifecyclePhase.PACKAGE)
public class ExportTinkarDataMojo extends TinkarMojo {

    @Isolate
    @Parameter(name = "exportDirectory", defaultValue = "${project.build.directory}")
    File exportDirectory;

    @Isolate
    @Parameter(name = "fileName", defaultValue = "tinkar-export.zip")
    File fileName;

    @Isolate
    @Parameter(name = "filter", defaultValue = "${new ArrayList<ComponentFilter>()}")
    ComponentFilter filter;


    @Override
    public void handleIsolatedFields() {
       //No extra field isolation handling needed
    }

    @Override
    public void run() {
        try {
            Files.createDirectories(exportDirectory.toPath());
        } catch (IOException e) {
            getLog().debug(e);
            throw  new RuntimeException(e);
        }
        File exportFile = exportDirectory.toPath().resolve(fileName.getName()).toFile();

        ExportEntitiesToProtobufFile exportTask;
		List<PublicId> membershipPublicIds;
		membershipPublicIds = filter.allowedMembershipsIds();

		if (!membershipPublicIds.isEmpty()) {
            exportTask = new ExportEntitiesToProtobufFile(exportFile, membershipPublicIds);
        } else {
            exportTask = new ExportEntitiesToProtobufFile(exportFile);
        }
        exportTask.compute();
    }
}
