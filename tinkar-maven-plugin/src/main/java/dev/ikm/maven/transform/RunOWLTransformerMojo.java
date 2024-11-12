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
package dev.ikm.maven.transform;

import dev.ikm.maven.DatastoreProxy;
import dev.ikm.tinkar.common.service.CachingService;
import dev.ikm.tinkar.common.service.PrimitiveData;
import dev.ikm.tinkar.common.service.ServiceKeys;
import dev.ikm.tinkar.common.service.ServiceProperties;
import dev.ikm.tinkar.entity.transaction.Transaction;
import dev.ikm.tinkar.ext.lang.owl.Rf2OwlToLogicAxiomTransformer;
import dev.ikm.tinkar.terms.TinkarTerm;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@Mojo(name = "run-owl-transformer", defaultPhase = LifecyclePhase.PROCESS_CLASSES)
public class RunOWLTransformerMojo extends AbstractMojo {

	@Override
	public void execute() throws MojoExecutionException {
		try (DatastoreProxy datastoreProxy = new DatastoreProxy()){
			datastoreProxy.start();
			Transaction owlTransformTransaction = new Transaction();
			Rf2OwlToLogicAxiomTransformer rf2OwlToLogicAxiomTransformer = new Rf2OwlToLogicAxiomTransformer(owlTransformTransaction, TinkarTerm.OWL_AXIOM_SYNTAX_PATTERN, TinkarTerm.EL_PLUS_PLUS_STATED_AXIOMS_PATTERN);
			rf2OwlToLogicAxiomTransformer.call();
		} catch (Exception e) {
			getLog().error(e);
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}
}
