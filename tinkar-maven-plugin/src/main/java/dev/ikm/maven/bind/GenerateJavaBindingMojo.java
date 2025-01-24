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
package dev.ikm.maven.bind;

import dev.ikm.maven.DatastoreProxy;
import dev.ikm.maven.bind.config.CharacterReplacement;
import dev.ikm.maven.bind.config.LanguageConfiguration;
import dev.ikm.maven.bind.config.StampConfiguration;
import dev.ikm.tinkar.common.service.CachingService;
import dev.ikm.tinkar.common.service.PrimitiveData;
import dev.ikm.tinkar.common.service.ServiceKeys;
import dev.ikm.tinkar.common.service.ServiceProperties;
import dev.ikm.tinkar.coordinate.language.LanguageCoordinateRecord;
import dev.ikm.tinkar.coordinate.language.calculator.LanguageCalculator;
import dev.ikm.tinkar.coordinate.language.calculator.LanguageCalculatorWithCache;
import dev.ikm.tinkar.coordinate.stamp.calculator.StampCalculator;
import dev.ikm.tinkar.entity.Entity;
import dev.ikm.tinkar.entity.EntityService;
import dev.ikm.tinkar.entity.EntityVersion;
import dev.ikm.tinkar.ext.binding.BindingHelper;
import dev.ikm.tinkar.ext.binding.GenerateJavaBindingTask;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;

import java.io.*;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

@Mojo(name = "generate-java-binding", defaultPhase = LifecyclePhase.PREPARE_PACKAGE)
public class GenerateJavaBindingMojo extends AbstractMojo {

	@Parameter(name = "dataStore", defaultValue = "${project.build.directory}/datastore")
	File dataStore;

	@Parameter(name = "bindingOutputFile", required = true)
	private File bindingOutputFile;

	@Parameter(name = "author", required = true)
	private String author;

	@Parameter(name = "packageName", required = true)
	private String packageName;

	@Parameter(name = "namespace", required = true)
	private String namespace;

	@Parameter(name = "characterReplacements", required = true)
	private List<CharacterReplacement> characterReplacements;

	@Parameter(name = "stampConfiguration", required = true)
	private String stampConfiguration;

	@Parameter(name = "languageConfigurations", required = true)
	private List<String> languageConfigurations;

	@Override
	public void execute() throws MojoExecutionException {
		try (DatastoreProxy datastoreProxy = new DatastoreProxy(dataStore)) {
			Stream.Builder<Entity<? extends EntityVersion>> conceptStreamBuilder = Stream.builder();
			Stream.Builder<Entity<? extends EntityVersion>> patternStreamBuilder = Stream.builder();
			PrimitiveData.get().forEachConceptNid(nid -> conceptStreamBuilder.add(EntityService.get().getEntityFast(nid)));
			PrimitiveData.get().forEachPatternNid(nid -> patternStreamBuilder.add(EntityService.get().getEntityFast(nid)));
			String className = bindingOutputFile.toPath().getFileName().toString().replace(".java", "");
			UUID namespaceUUID;

			//Check for correctly formed class name based on java file name
			if (className.contains(" ")) {
				throw new MojoExecutionException("Binding output file name contains spaces");
			}

			//Check for correctly formed package name
			if (!packageName.contains(".")) {
				throw new MojoExecutionException("Package Name malformed and doesn't contain '.'");
			}

			//Check for correctly formed namespace input
			try {
				if (namespace.isEmpty()) {
					namespaceUUID = UUID.randomUUID();
				} else {
					namespaceUUID = UUID.fromString(namespace);
				}
			} catch (IllegalArgumentException e) {
				getLog().error(e.getMessage());
				throw new MojoExecutionException(e.getMessage());
			}

			try (DataOutputStream dataOutputStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(bindingOutputFile)))) {
				StampConfiguration stampConfiguration = StampConfiguration.DEVELOPMENT_LATEST;
				List<LanguageConfiguration> languageConfigurations = Arrays.asList(LanguageConfiguration.US_ENGLISH_FULLY_QUALIFIED_NAME, LanguageConfiguration.US_ENGLISH_REGULAR_NAME);
				final StampCalculator stampCalculator = stampConfiguration.getStampCoordinateRecord().stampCalculator();
				MutableList<LanguageCoordinateRecord> languageCoordinateRecords = Lists.mutable.empty();
				languageConfigurations.stream().map(LanguageConfiguration::getLanguageCoordinateRecord).forEach(languageCoordinateRecords::add);

				final LanguageCalculator languageCalculator = LanguageCalculatorWithCache.getCalculator(
						stampConfiguration.getStampCoordinateRecord(),
						languageCoordinateRecords.toImmutableList());

				GenerateJavaBindingTask generateJavaBindingTask = new GenerateJavaBindingTask(
						conceptStreamBuilder.build(),
						patternStreamBuilder.build(),
						Stream.empty(),
						author,
						packageName,
						className,
						namespaceUUID,
						interpolationConsumer -> {
							try {
								dataOutputStream.writeBytes(interpolationConsumer);
							} catch (IOException e) {
								throw new RuntimeException(e);
							}
						},
						new BindingHelper(languageCalculator, stampCalculator, fqn -> {
							AtomicReference<String> fqnRef = new AtomicReference<>(fqn);
							characterReplacements.forEach(characterReplacement -> {
								if (fqn.contains(characterReplacement.getCharacter())) {
									String newFQN = fqn.replace(characterReplacement.getCharacter(), characterReplacement.getReplacement());
									fqnRef.set(newFQN);
								}
							});
							return fqnRef.get();
						})
				);

				generateJavaBindingTask.call();
			} catch (Exception e) {
				getLog().error(e);
				throw new MojoExecutionException(e.getMessage(), e);
			}
		} catch (Exception e) {
			getLog().error(e);
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}
}
