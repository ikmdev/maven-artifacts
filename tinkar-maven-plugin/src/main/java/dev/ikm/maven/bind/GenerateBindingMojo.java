package dev.ikm.maven.bind;

import dev.ikm.maven.bind.config.StringVariable;
import dev.ikm.maven.export.config.ComponentFilter;
import dev.ikm.maven.toolkit.TinkarMojo;
import dev.ikm.maven.toolkit.isolated.boundary.Isolate;
import dev.ikm.tinkar.coordinate.language.calculator.LanguageCalculatorWithCache;
import dev.ikm.tinkar.coordinate.navigation.calculator.NavigationCalculatorWithCache;
import dev.ikm.tinkar.coordinate.stamp.calculator.StampCalculator;
import dev.ikm.tinkar.forge.Forge;
import dev.ikm.tinkar.forge.TinkarForge;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

@Mojo(name = "generate-binding", defaultPhase = LifecyclePhase.PREPARE_PACKAGE)
public class GenerateBindingMojo extends TinkarMojo {

	@Isolate
	@Parameter(name = "bindingOutput", defaultValue = "${project.build.directory}")
	private File bindingOutput;

	@Isolate
	@Parameter(name = "templateDirectory", defaultValue = "${project.build.directory}/template")
	private File templateDirectory;

	@Isolate
	@Parameter(name = "templateFile")
	private File templateFile;

	@Isolate
	@Parameter(name = "stringVariables")
	private List<StringVariable> stringVariables;

	@Isolate
	@Parameter(name = "filter")
	private ComponentFilter filter;

	@Override
	public void handleIsolatedFields() {
		//No handling
	}

	@Override
	public void run() {
		getLog().info("Starting Forge...");

		try (FileWriter fw = new FileWriter(bindingOutput)) {
			Forge forge = new TinkarForge();
			forge.config(templateDirectory.toPath());
			for (StringVariable stringVariable : stringVariables) {
				forge.variable(stringVariable.getName(), stringVariable.getValue());
			}

			StampCalculator stampCalculator = filter.createStampCalculatorWithCache();
			LanguageCalculatorWithCache languageCalculator = filter.createLanguageCalculatorWithCache();
			NavigationCalculatorWithCache navigationCalculator = filter.createNavigationCalculatorWithCache();

			forge.conceptData(filter.filterConcepts(), integer -> System.out.println("Concept " + languageCalculator.getDescriptionText(integer)));
			forge.patternData(filter.filterPatterns(), integer -> System.out.println("Pattern " + languageCalculator.getDescriptionText(integer)));
			forge.variable("defaultSTAMPCalc", stampCalculator);
			forge.variable("defaultLanguageCalc", languageCalculator);
			forge.variable("defaultNavigationCalc", navigationCalculator);
			forge.template(templateFile.getName(), new BufferedWriter(fw));
			forge.execute();
		} catch (IOException e) {
			e.printStackTrace();
		}

		getLog().info("Finished Forge....");
	}
}
