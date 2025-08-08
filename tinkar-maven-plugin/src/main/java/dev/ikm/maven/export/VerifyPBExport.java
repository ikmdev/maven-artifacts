package dev.ikm.maven.export;

import dev.ikm.maven.toolkit.TinkarMojo;
import dev.ikm.maven.toolkit.isolated.boundary.Isolate;
import dev.ikm.maven.toolkit.isolated.controller.IsolationDispatcher;
import dev.ikm.tinkar.common.id.IntIdList;
import dev.ikm.tinkar.common.id.IntIdSet;
import dev.ikm.tinkar.common.id.PublicId;
import dev.ikm.tinkar.entity.ConceptEntity;
import dev.ikm.tinkar.entity.ConceptEntityVersion;
import dev.ikm.tinkar.entity.Entity;
import dev.ikm.tinkar.entity.EntityService;
import dev.ikm.tinkar.entity.PatternEntity;
import dev.ikm.tinkar.entity.PatternEntityVersion;
import dev.ikm.tinkar.entity.SemanticEntity;
import dev.ikm.tinkar.entity.SemanticEntityVersion;
import dev.ikm.tinkar.entity.StampEntity;
import dev.ikm.tinkar.entity.StampEntityVersion;
import dev.ikm.tinkar.entity.load.LoadEntitiesFromProtobufFile;
import dev.ikm.tinkar.integration.DataIntegrity;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.shared.model.fileset.FileSet;
import org.apache.maven.shared.model.fileset.util.FileSetManager;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mojo(name = "verify-export", requiresDependencyResolution = ResolutionScope.RUNTIME_PLUS_SYSTEM, defaultPhase = LifecyclePhase.VERIFY)
public class VerifyPBExport extends TinkarMojo {

	private final FileSetManager fileSetManager = new FileSetManager();

	@Parameter(name= "filesets")
	private FileSet[] filesets;

	@Parameter(name = "fileset")
	private FileSet fileset;

	@Isolate
	private List<File> filesToLoad = new ArrayList<>();

	@Override
	public void handleIsolatedFields() {
		if (filesets != null) {
			for (FileSet innerFileSet : filesets) {
				for (String includeFile : fileSetManager.getIncludedFiles(innerFileSet)) {
					File pbZip = new File(innerFileSet.getDirectory(), includeFile);
					filesToLoad.add(pbZip);
				}
			}
		}
		if (fileset != null) {
			for (String includeFile : fileSetManager.getIncludedFiles(fileset)) {
				File pbZip = new File(fileset.getDirectory(), includeFile);
				filesToLoad.add(pbZip);
			}
		}
	}

	@Override
	public void run() {
		if (!isolate) {
			handleIsolatedFields();
		}
		filesToLoad.forEach(file -> {
			var loadTask = new LoadEntitiesFromProtobufFile(file);
			loadTask.compute();
		});

		StringBuilder sb = new StringBuilder();

		List<Integer> aggregatedNullNidList = new ArrayList<>();
		boolean failed = false;

		List<? extends Entity> entities = DataIntegrity.validateStampReferences(aggregatedNullNidList);

		if (!entities.isEmpty()) {
			failed = true;
			buildStampRefs(sb, entities);
		}

		entities = DataIntegrity.validateConceptReferences(aggregatedNullNidList);

		if (!entities.isEmpty()) {
			failed = true;
			buildConceptRefs(sb, entities);
		}

		entities = DataIntegrity.validateSemanticReferences(aggregatedNullNidList);

		if (!entities.isEmpty()) {
			failed = true;
			buildSemanticRefs(sb, entities);
		}

		entities = DataIntegrity.validatePatternReferences(aggregatedNullNidList);

		if (!entities.isEmpty()) {
			failed = true;
			buildPatternRefs(sb, entities);
		}

		if (failed) {
			LocalTime localTime = LocalTime.now();
			DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HHmmss");

			String reportFile = targetDir + File.separator + "FailedVerifyPBExportReport" + dateTimeFormatter.format(localTime) + ".txt";

			sb.append("\nFound " + aggregatedNullNidList.size() + " Nids containing incorrect references.\n");

			try (BufferedWriter bw = new BufferedWriter(new FileWriter(reportFile))) {
				bw.write(sb.toString());
			} catch(IOException e) {
				getLog().error(e);
			}

			getLog().error("VerifyPBExport failed due to null references. See this file for more details " + reportFile);
			System.exit(IsolationDispatcher.VERIFY_EXIT_CODE);
		}
	}

	private void buildStampRefs(StringBuilder sb, List<? extends Entity> entities) {
		sb.append("\nFound Null Stamp references\n");
		entities.forEach(entity -> {
			StampEntity<? extends StampEntityVersion> e = (StampEntity) entity;
			sb.append("  Stamp with PublicId " + e.publicId() + "\n");
			e.versions().stream().forEach(version -> {
				if (DataIntegrity.referencedEntityIsNull(version.stateNid())) {
					sb.append("    null state entity with nid " + version.stateNid() + "\n");
				}
				if (DataIntegrity.referencedEntityIsNull(version.authorNid())) {
					sb.append("    null author entity with nid " + version.authorNid() + "\n");
				}
				if (DataIntegrity.referencedEntityIsNull(version.moduleNid())) {
					sb.append("    null module entity with nid " + version.moduleNid() + "\n");
				}
				if (DataIntegrity.referencedEntityIsNull(version.pathNid())) {
					sb.append("    null path entity with nid " + version.pathNid() + "\n");
				}
			});
		});
	}

	private void buildConceptRefs(StringBuilder sb, List<? extends Entity> entities) {
		sb.append("\nFound Null Concept references\n");
		entities.forEach(entity -> {
			ConceptEntity<? extends ConceptEntityVersion> e = (ConceptEntity) entity;
			sb.append("  Concept with PublicId " + e.publicId() + "\n");
			e.versions().stream().forEach(version -> {
				if (DataIntegrity.referencedEntityIsNull(version.nid())) {
					sb.append("    null version entity with nid " + version.nid() + "\n");
				}
				if (DataIntegrity.referencedEntityIsNull(version.stampNid())) {
					sb.append("    null stamp entity with nid " + version.stampNid() + "\n");
				}
			});
		});
	}

	private void buildSemanticRefs(StringBuilder sb, List<? extends Entity> entities) {
		sb.append("\nFound Null Semantic references\n");
		entities.forEach(entity -> {
			SemanticEntity<? extends SemanticEntityVersion> e = (SemanticEntity) entity;
			sb.append("  Semantic with PublicId " + e.publicId() + "\n");
			e.versions().stream().forEach(version -> {
				if (DataIntegrity.referencedEntityIsNull(version.nid())) {
					sb.append("    null version entity with nid " + version.nid() + "\n");
				}
				if (DataIntegrity.referencedEntityIsNull(version.stampNid())) {
					sb.append("    null stamp entity with nid " + version.stampNid() + "\n");
				}
				if (DataIntegrity.referencedEntityIsNull(version.patternNid())) {
					sb.append("    null pattern entity with nid " + version.patternNid() + "\n");
				}
				if (DataIntegrity.referencedEntityIsNull(version.referencedComponentNid())) {
					sb.append("    null referenced component entity with nid " + version.referencedComponentNid() + "\n");
				} else {
					try {
						sb.append("    referenced component entity " + version.referencedComponent() + "\n");
					} catch (Exception ex) {
						// ignore possible toString errors
					}
				}

				try {
					sb.append("    version field values " + version.fieldValues() + "\n");
				} catch (Exception ex) {
					// ignore possible toString errors
				}
				version.fieldValues().forEach(fieldVal -> {
					if (fieldVal instanceof IntIdSet nidSet) {
						nidSet.forEach(nid -> {
							if (DataIntegrity.referencedEntityIsNull(nid)) {
								sb.append("    null entry in field value of type IntIdSet " + nid + "\n");
							}
						});
					} else if (fieldVal instanceof IntIdList nidList) {
						nidList.forEach(nid -> {
							if (DataIntegrity.referencedEntityIsNull(nid)) {
								sb.append("    null entry in field value of type IntIdList " + nid + "\n");
							}
						});
					} else if (fieldVal instanceof PublicId pubId) {
						try {
							EntityService.get().getEntity(pubId.asUuidList());
						} catch (NullPointerException ex) {
							sb.append("    null entry in field value of type PublicId " + fieldVal + "\n");
						}
					}
				});
			});
		});
	}

	private void buildPatternRefs(StringBuilder sb, List<? extends Entity> entities) {
		sb.append("\nFound Null Pattern references\n");
		entities.forEach(entity -> {
			PatternEntity<? extends PatternEntityVersion> e = (PatternEntity) entity;
			sb.append("  Pattern with PublicId " + e.publicId() + "\n");

			try {
				sb.append("  Pattern entity  " + e + "\n");
			} catch (Exception exc) {
				// catching exception and ignoring potential NullPointerException during toString
			}
			e.versions().stream().forEach(version -> {
				if (DataIntegrity.referencedEntityIsNull(version.nid())) {
					sb.append("    null version entity with nid " + version.nid() + "\n");
				}
				if (DataIntegrity.referencedEntityIsNull(version.stampNid())) {
					sb.append("    null stamp entity with nid " + version.stampNid() + "\n");
				}
				if (DataIntegrity.referencedEntityIsNull(version.semanticMeaningNid())) {
					sb.append("    null semantic meaning entity with nid " + version.semanticMeaningNid() + "\n");
				}
				if (DataIntegrity.referencedEntityIsNull(version.semanticPurposeNid())) {
					sb.append("    null semantic purpose entity with nid " + version.semanticPurposeNid() + "\n");
				}

				version.fieldDefinitions().forEach(fieldDef -> {
					if (DataIntegrity.referencedEntityIsNull(fieldDef.dataTypeNid())) {
						sb.append("    null data type in field definition " + fieldDef.dataTypeNid() + "\n");
					}
					if (DataIntegrity.referencedEntityIsNull(fieldDef.meaningNid())) {
						sb.append("    null meaning in field definition " + fieldDef.meaningNid() + "\n");
					}
					if (DataIntegrity.referencedEntityIsNull(fieldDef.purposeNid())) {
						sb.append("    null purpose in field definition " + fieldDef.purposeNid() + "\n");
					}
				});
			});
		});
	}
}
