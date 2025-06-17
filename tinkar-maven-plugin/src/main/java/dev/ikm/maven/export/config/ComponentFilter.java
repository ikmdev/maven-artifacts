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
package dev.ikm.maven.export.config;

import dev.ikm.tinkar.common.id.IntIdList;
import dev.ikm.tinkar.common.id.IntIdSet;
import dev.ikm.tinkar.common.id.IntIds;
import dev.ikm.tinkar.common.id.PublicId;
import dev.ikm.tinkar.common.service.PrimitiveData;
import dev.ikm.tinkar.coordinate.language.LanguageCoordinateRecord;
import dev.ikm.tinkar.coordinate.language.calculator.LanguageCalculatorWithCache;
import dev.ikm.tinkar.coordinate.navigation.NavigationCoordinateRecord;
import dev.ikm.tinkar.coordinate.navigation.calculator.NavigationCalculatorWithCache;
import dev.ikm.tinkar.coordinate.stamp.StampCoordinateRecord;
import dev.ikm.tinkar.coordinate.stamp.StateSet;
import dev.ikm.tinkar.coordinate.stamp.calculator.Latest;
import dev.ikm.tinkar.coordinate.stamp.calculator.StampCalculatorWithCache;
import dev.ikm.tinkar.entity.ConceptEntity;
import dev.ikm.tinkar.entity.ConceptEntityVersion;
import dev.ikm.tinkar.entity.Entity;
import dev.ikm.tinkar.entity.EntityService;
import dev.ikm.tinkar.entity.EntityVersion;
import dev.ikm.tinkar.entity.PatternEntity;
import dev.ikm.tinkar.entity.PatternEntityVersion;
import dev.ikm.tinkar.entity.StampEntity;
import dev.ikm.tinkar.entity.StampEntityVersion;
import dev.ikm.tinkar.terms.ConceptFacade;
import dev.ikm.tinkar.terms.EntityProxy;
import dev.ikm.tinkar.terms.PatternFacade;
import dev.ikm.tinkar.terms.State;
import dev.ikm.tinkar.terms.TinkarTerm;
import org.antlr.v4.runtime.misc.IntSet;
import org.apache.maven.plugin.MojoExecutionException;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.impl.factory.Lists;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public class ComponentFilter implements Serializable {

	//STAMP Coordinate
	private List<PublicIdConfig> memberships;
	private List<PublicIdConfig> statuses;
	private List<PublicIdConfig> modules;
	private PublicIdConfig path;

	//Navigation Coordinate Fields
	private boolean useInferredDefinitions = true;

	//Language Coordinate Fields
	private PublicIdConfig language;
	private PublicIdConfig descriptionPattern;
	private List<PublicIdConfig> descriptionTypePreference;
	private List<PublicIdConfig> dialectPreference;

	private StampCalculatorWithCache stampCalculator;
	private LanguageCalculatorWithCache languageCalculator;
	private NavigationCalculatorWithCache navigationCalculator;

	public List<PublicIdConfig> getMemberships() {
		return memberships;
	}

	public void setMemberships(List<PublicIdConfig> memberships) {
		this.memberships = memberships;
	}

	public List<PublicIdConfig> getStatuses() {
		return statuses;
	}

	public void setStatuses(List<PublicIdConfig> statuses) {
		this.statuses = statuses;
	}

	public PublicIdConfig getLanguage() {
		return language;
	}

	public void setLanguage(PublicIdConfig language) {
		this.language = language;
	}

	public List<PublicIdConfig> getModules() {
		return modules;
	}

	public void setModules(List<PublicIdConfig> modules) {
		this.modules = modules;
	}

	public PublicIdConfig getPath() {
		return path;
	}

	public void setPath(PublicIdConfig path) {
		this.path = path;
	}

	public boolean isUseInferredDefinitions() {
		return useInferredDefinitions;
	}

	public void setUseInferredDefinitions(boolean useInferredDefinitions) {
		this.useInferredDefinitions = useInferredDefinitions;
	}

	public PublicIdConfig getDescriptionPattern() {
		return descriptionPattern;
	}

	public void setDescriptionPattern(PublicIdConfig descriptionPattern) {
		this.descriptionPattern = descriptionPattern;
	}

	public List<PublicIdConfig> getDescriptionTypePreference() {
		return descriptionTypePreference;
	}

	public void setDescriptionTypePreference(List<PublicIdConfig> descriptionTypePreference) {
		this.descriptionTypePreference = descriptionTypePreference;
	}

	public List<PublicIdConfig> getDialectPreference() {
		return dialectPreference;
	}

	public void setDialectPreference(List<PublicIdConfig> dialectPreference) {
		this.dialectPreference = dialectPreference;
	}

	public StampCalculatorWithCache getStampCalculator() {
		return stampCalculator;
	}

	public void setStampCalculator(StampCalculatorWithCache stampCalculator) {
		this.stampCalculator = stampCalculator;
	}

	public LanguageCalculatorWithCache getLanguageCalculator() {
		return languageCalculator;
	}

	public void setLanguageCalculator(LanguageCalculatorWithCache languageCalculator) {
		this.languageCalculator = languageCalculator;
	}

	public NavigationCalculatorWithCache getNavigationCalculator() {
		return navigationCalculator;
	}

	public void setNavigationCalculator(NavigationCalculatorWithCache navigationCalculator) {
		this.navigationCalculator = navigationCalculator;
	}

	public void process() {
		this.stampCalculator = createStampCalculatorWithCache();
		this. languageCalculator = createLanguageCalculatorWithCache();
		this.navigationCalculator = createNavigationCalculatorWithCache();
	}

	public List<PublicId> allowedMembershipsIds() {
		return memberships.stream()
				.map(PublicIdConfig::getPublicId)
				.toList();
	}

	public List<PublicId> allowedStatusesIds() {
		return statuses.stream()
				.map(PublicIdConfig::getPublicId)
				.toList();
	}

	public List<PublicId> allowedModulesIds() {
		return modules.stream()
				.map(PublicIdConfig::getPublicId)
				.toList();
	}

	public Stream<ConceptEntity<? extends ConceptEntityVersion>> filterConcepts() {
		Stream.Builder<ConceptEntity<? extends ConceptEntityVersion>> builder = Stream.builder();
		if (memberships.isEmpty()) {
			PrimitiveData.get().forEachConceptNid(conceptNid -> {
				Entity<? extends EntityVersion> conceptEntity = Entity.getFast(conceptNid);
				builder.add((ConceptEntity<? extends ConceptEntityVersion>) conceptEntity);
			});
		} else {
			PrimitiveData.get().forEachConceptNid(conceptNid -> {
				memberships.stream().map(pConfig -> pConfig.getProxy().nid())
						.forEach(membershipPatternNid -> {
							int[] membershipSemantics = EntityService.get().semanticNidsForComponentOfPattern(conceptNid, membershipPatternNid);
							if (membershipSemantics.length > 0) {
								Entity<? extends EntityVersion> conceptEntity = Entity.getFast(conceptNid);
								builder.add((ConceptEntity<? extends ConceptEntityVersion>) conceptEntity);
							}
						});
			});
		}
		return builder.build();
	}

	public Stream<PatternEntity<? extends PatternEntityVersion>> filterPatterns() {
		Stream.Builder<PatternEntity<? extends PatternEntityVersion>> builder = Stream.builder();
		if (memberships.isEmpty()) {
			PrimitiveData.get().forEachPatternNid(patternNid -> {
				Entity<? extends EntityVersion> patternEntity = Entity.getFast(patternNid);
				builder.add((PatternEntity<? extends PatternEntityVersion>) patternEntity);
			});
		} else {
			PrimitiveData.get().forEachPatternNid(patternNid -> {
				memberships.stream().map(pConfig -> pConfig.getProxy().nid())
						.forEach(membershipPatternNid -> {
							int[] membershipSemantics = EntityService.get().semanticNidsForComponentOfPattern(patternNid, membershipPatternNid);
							if (membershipSemantics.length > 0) {
								Entity<? extends EntityVersion> patternEntity = Entity.getFast(patternNid);
								builder.add((PatternEntity<? extends PatternEntityVersion>) patternEntity);
							}
						});
			});
		}
		return builder.build();
	}

	private StateSet createStateSet() {
		List<State> states = new ArrayList<>();
		for (PublicIdConfig publicIdConfig : statuses) {
			ConceptFacade conceptFacade = publicIdConfig.getProxy();
			State state = State.fromConcept(conceptFacade);
			states.add(state);
		}
		return StateSet.make(states);
	}

	private StampCoordinateRecord createStampCoordinateRecord() {
		StateSet stateSet = createStateSet();

		int pathNid = path.getProxy().nid();

		Set<ConceptFacade> allowedModules = new HashSet<>();
		for (PublicIdConfig publicIdConfig : modules) {
			allowedModules.add(publicIdConfig.getProxy());
		}
		return StampCoordinateRecord.make(stateSet, pathNid, allowedModules);
	}

	private LanguageCoordinateRecord createLanguageCoordinateRecord() {
		ConceptFacade languageConceptFacade = language.getProxy();
		PatternFacade descriptionPatternFacade = descriptionPattern.getProxy();

		int[] descriptionTypeNids = new int[descriptionTypePreference.size()];
		for (int i = 0; i < descriptionTypePreference.size(); i++) {
			descriptionTypeNids[i] = descriptionTypePreference.get(i).getProxy().nid();
		}
		IntIdList descTypePrefList = IntIds.list.of(descriptionTypeNids);

		int[] dialectPreferenceNids = new int[dialectPreference.size()];
		for (int i = 0; i < dialectPreference.size(); i++) {
			dialectPreferenceNids[i] = dialectPreference.get(i).getProxy().nid();
		}
		IntIdList dialectPrefList = IntIds.list.of(dialectPreferenceNids);

		int[] modulePreferenceNids = new int[modules.size()];
		for (int i = 0; i < modules.size(); i++) {
			modulePreferenceNids[i] = modules.get(i).getProxy().nid();
		}
		IntIdList modulePrefList = IntIds.list.of(modulePreferenceNids);

		return LanguageCoordinateRecord.make(languageConceptFacade,
				descriptionPatternFacade,
				descTypePrefList,
				dialectPrefList,
				modulePrefList);
	}

	private NavigationCoordinateRecord createNavigationCoordinateRecord() {
		IntIdSet navigationPatternNids;
		if (useInferredDefinitions) {
			navigationPatternNids = IntIds.set.of(TinkarTerm.INFERRED_NAVIGATION_PATTERN.nid());
		} else {
			navigationPatternNids = IntIds.set.of(TinkarTerm.STATED_NAVIGATION_PATTERN.nid());
		}
		StateSet stateSet = createStateSet();
		return NavigationCoordinateRecord.make(navigationPatternNids, stateSet, true, IntIds.list.empty());
	}

	private StampCalculatorWithCache createStampCalculatorWithCache() {
		StampCoordinateRecord stampCoordinateRecord = createStampCoordinateRecord();
		return StampCalculatorWithCache.getCalculator(stampCoordinateRecord);
	}

	private LanguageCalculatorWithCache createLanguageCalculatorWithCache() {
		StampCoordinateRecord stampCoordinateRecord = createStampCoordinateRecord();
		LanguageCoordinateRecord languageCoordinateRecord = createLanguageCoordinateRecord();
		ImmutableList<LanguageCoordinateRecord> languageCoordinateRecords = Lists.immutable.of(languageCoordinateRecord);
		return LanguageCalculatorWithCache.getCalculator(stampCoordinateRecord, languageCoordinateRecords);
	}

	private NavigationCalculatorWithCache createNavigationCalculatorWithCache() {
		StampCoordinateRecord stampCoordinateRecord = createStampCoordinateRecord();
		LanguageCoordinateRecord languageCoordinateRecord = createLanguageCoordinateRecord();
		NavigationCoordinateRecord navigationCoordinateRecord = createNavigationCoordinateRecord();
		ImmutableList<LanguageCoordinateRecord> languageCoordinateRecords = Lists.immutable.of(languageCoordinateRecord);
		return NavigationCalculatorWithCache.getCalculator(stampCoordinateRecord, languageCoordinateRecords, navigationCoordinateRecord);
	}
}
