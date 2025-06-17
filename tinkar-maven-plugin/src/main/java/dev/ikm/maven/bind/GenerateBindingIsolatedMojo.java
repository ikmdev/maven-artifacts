package dev.ikm.maven.bind;

import dev.ikm.maven.bind.config.StringVariable;
import dev.ikm.maven.export.config.ComponentFilter;
import dev.ikm.maven.toolkit.isolated.boundary.Isolate;
import dev.ikm.maven.toolkit.isolated.boundary.IsolatedTinkarMojo;
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

@Mojo(name = "generate-binding-isolated", defaultPhase = LifecyclePhase.PREPARE_PACKAGE)
public class GenerateBindingIsolatedMojo extends IsolatedTinkarMojo {

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
		System.out.println("break");
	}

	@Override
	public void run() {
		filter.process();
		try (FileWriter fw = new FileWriter(bindingOutput)) {
			Forge forge = new TinkarForge();
			forge.config(templateDirectory.toPath());
			for (StringVariable stringVariable : stringVariables) {
				forge.variable(stringVariable.getName(), stringVariable.getValue());
			}
			forge.conceptData(filter.filterConcepts(), integer -> {});
			forge.patternData(filter.filterPatterns(), integer -> {});
			forge.variable("defaultSTAMPCalc", filter.getStampCalculator());
			forge.variable("defaultLanguageCalc", filter.getLanguageCalculator());
			forge.variable("defaultNavigationCalc", filter.getNavigationCalculator());
			forge.template(templateFile.getName(), new BufferedWriter(fw));
			forge.execute();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
