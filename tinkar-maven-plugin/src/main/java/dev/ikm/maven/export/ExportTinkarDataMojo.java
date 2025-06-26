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

import dev.ikm.maven.export.config.ComponentFilter;
import dev.ikm.maven.export.config.PublicIdConfig;
import dev.ikm.maven.toolkit.simple.boundary.SimpleTinkarMojo;
import dev.ikm.tinkar.common.id.PublicId;
import dev.ikm.tinkar.entity.aggregator.DefaultEntityAggregator;
import dev.ikm.tinkar.entity.aggregator.EntityAggregator;
import dev.ikm.tinkar.entity.aggregator.InferredEntityAggregatorFilter;
import dev.ikm.tinkar.entity.aggregator.MembershipEntityAggregator;
import dev.ikm.tinkar.entity.export.ExportEntitiesToProtobufFile;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

@Mojo(name = "export-tinkar-data", defaultPhase = LifecyclePhase.PACKAGE)
public class ExportTinkarDataMojo extends SimpleTinkarMojo {

    @Parameter(name = "exportDirectory", defaultValue = "${project.build.directory}")
    File exportDirectory;

    @Parameter(name = "fileName", defaultValue = "tinkar-export.zip")
    File fileName;

    @Parameter(name = "unreasoned", defaultValue = "false")
    boolean unreasoned;

    @Parameter(name = "filters", defaultValue = "${new ArrayList<ComponentFilter>()}")
    List<ComponentFilter> filters;

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
		List<PublicId> membershipPublicIds = null;
		try {
			membershipPublicIds = getMemberships(); //TODO-aks8m: refactor these
		} catch (MojoExecutionException e) {
			throw new RuntimeException(e);
		}

        EntityAggregator entityAggregator = new DefaultEntityAggregator();
        if (!membershipPublicIds.isEmpty()) {
            entityAggregator = new MembershipEntityAggregator(membershipPublicIds);
        }
        if (unreasoned) {
            entityAggregator = new InferredEntityAggregatorFilter(entityAggregator);
        }
        exportTask = new ExportEntitiesToProtobufFile(exportFile, entityAggregator);
        exportTask.compute();
    }

    private List<PublicId> getMemberships() throws MojoExecutionException {
        List<PublicId> memberships = new ArrayList<>();
        for (ComponentFilter filter : filters) {
            for (PublicIdConfig membership : filter.allowedMemberships()) {
                memberships.add(membership.getPublicId());
            }
        }
        return memberships;
    }
}
