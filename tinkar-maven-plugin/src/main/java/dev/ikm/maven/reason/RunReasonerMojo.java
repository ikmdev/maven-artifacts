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
package dev.ikm.maven.reason;

import dev.ikm.maven.DatastoreProxy;
import dev.ikm.tinkar.common.service.PluggableService;
import dev.ikm.tinkar.coordinate.Calculators;
import dev.ikm.tinkar.reasoner.service.ClassifierResults;
import dev.ikm.tinkar.reasoner.service.ReasonerService;
import dev.ikm.tinkar.terms.TinkarTerm;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;

@Mojo(name = "run-full-reasoner", defaultPhase = LifecyclePhase.PROCESS_CLASSES)
public class RunReasonerMojo extends AbstractMojo {
	private static final Logger LOG = LoggerFactory.getLogger(RunReasonerMojo.class.getSimpleName());

	@Parameter(name = "dataStore", defaultValue = "${project.build.directory}/datastore")
	File dataStore;

	@Parameter(property = "reasonerType", defaultValue = "ElkOwlReasoner")
	String reasonerType;

	@Parameter(property = "reinferAllHierarchy", defaultValue = "false")
	String reinferAllHierarchy;

	@Override
	public void execute() throws MojoExecutionException {
		try (DatastoreProxy datastoreProxy = new DatastoreProxy(dataStore)) {
			List<ReasonerService> rss = PluggableService.load(ReasonerService.class).stream().map(ServiceLoader.Provider::get).filter(reasoner -> reasoner.getName().contains(reasonerType))
					.sorted(Comparator.comparing(ReasonerService::getName)).toList();
			LOG.info("Number of reasoners " + rss.size());
			for (ReasonerService rs : rss) {
				LOG.info("Reasoner service: " + rs);

				rs.init(Calculators.View.Default(), TinkarTerm.EL_PLUS_PLUS_STATED_AXIOMS_PATTERN, TinkarTerm.EL_PLUS_PLUS_INFERRED_AXIOMS_PATTERN);
				rs.setProgressUpdater(null);
				// Extract
				rs.extractData();
				// Load
				rs.loadData();
				// Compute
				rs.computeInferences();
				// Process Results
				ClassifierResults results = rs.processResults(null, Boolean.parseBoolean(reinferAllHierarchy));

				LOG.info("After Size of ConceptSet: " + rs.getReasonerConceptSet().size());
				LOG.info("ClassifierResults: inferred changes size " + results.getConceptsWithInferredChanges().size());
				LOG.info("ClassifierResults: navigation changes size " + results.getConceptsWithNavigationChanges().size());
				LOG.info("ClassifierResults: classificationconcept size " + results.getClassificationConceptSet().size());
			}
		} catch (Exception e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}
}
