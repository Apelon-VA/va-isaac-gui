/**
 * Copyright Notice
 *
 * This is a work of the U.S. Government and is not subject to copyright
 * protection in the United States. Foreign copyrights may apply.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gov.va.isaac.gui.conceptCreation;

import gov.va.isaac.AppContext;
import gov.va.isaac.ExtendedAppContext;
import gov.va.isaac.gui.conceptCreation.wizardPages.RelRow;
import gov.va.isaac.gui.conceptCreation.wizardPages.RoleType;
import gov.va.isaac.gui.conceptCreation.wizardPages.TermRow;
import gov.va.isaac.util.OchreUtility;
import gov.vha.isaac.metadata.coordinates.LogicCoordinates;
import gov.vha.isaac.metadata.source.IsaacMetadataAuxiliaryBinding;
import gov.vha.isaac.ochre.api.ConceptProxy;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.LookupService;
import gov.vha.isaac.ochre.api.commit.ChangeCheckerMode;
import gov.vha.isaac.ochre.api.component.concept.ConceptBuilder;
import gov.vha.isaac.ochre.api.component.concept.ConceptBuilderService;
import gov.vha.isaac.ochre.api.component.concept.ConceptChronology;
import gov.vha.isaac.ochre.api.component.concept.description.DescriptionBuilder;
import gov.vha.isaac.ochre.api.component.concept.description.DescriptionBuilderService;
import gov.vha.isaac.ochre.api.component.sememe.SememeChronology;
import gov.vha.isaac.ochre.api.component.sememe.version.MutableDescriptionSememe;
import gov.vha.isaac.ochre.api.logic.LogicalExpression;
import gov.vha.isaac.ochre.api.logic.LogicalExpressionBuilder;
import gov.vha.isaac.ochre.api.logic.LogicalExpressionBuilderService;
import gov.vha.isaac.ochre.api.logic.assertions.Assertion;
import gov.vha.isaac.ochre.api.logic.assertions.ConceptAssertion;
import gov.vha.isaac.ochre.impl.lang.LanguageCode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * {@link WizardController}
 *
 * @author <a href="jefron@apelon.com">Jesse Efron</a>
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 * @author <a href="mailto:vkaloidis@apelon.com.com">Vas Kaloidis</a>
 * @author <a href="mailto:joel.kniaz@gmail.com">Joel Kniaz</a>
 */
public class WizardController {

	private static Logger logger = LoggerFactory.getLogger(WizardController.class);
	private String fsn;
	//private String prefTerm;
	private List<Integer> parents;
	private List<TermRow> syns;
	private List<RelRow> rels;
	
	public void setConceptDefinitionVals(String fsn, List<Integer> parents) {
		this.fsn = fsn;
		this.parents = parents;
	}

	public void setConceptComponents(List<TermRow> syns, List<RelRow> rels) {
		this.syns = syns;
		this.rels = rels;
	}

	public String getConceptFSN() {
		return fsn;
	}

	public List<Integer> getParents() {
		return parents;
	}

	public int getSynonymsCreated() {
		return syns.size();
	}

	public String getTerm(int i) {
		return syns.get(i).getTerm();
	}

	public ConceptProxy getType(int i) {
		return syns.get(i).getType();
	}

	public String getTypeString(int i) {
		return syns.get(i).getTypeString();
	}

	public String getCaseSensitivity(int i) {
		if (syns.get(i).isInitialCaseSig()) {
			return "True";
		} else {
			return "False";
		}
	}

	public String getLanguage(int i) {
		return LanguageCode.EN_US.getFormatedLanguageCode();
	}

	public int getRelationshipsCreated() {
		return rels.size();
	}

	public String getRelType(int i) {
		return OchreUtility.getDescription(rels.get(i).getRelationshipNid(), null).get();
	}

	public String getTarget(int i) {
		return OchreUtility.getDescription(rels.get(i).getTargetNid(), null).get();
	}

	// TODO make sure PT and FSN are case insensitive

	public String getQualRole(int i) {
		return rels.get(i).toString();
	}

	public String getGroup(int i) {
		return String.valueOf(rels.get(i).getGroup());
	}
	
	public int getNid(UUID input) {
		return Get.identifierService().getNidForUuids(input);
	}

	public ConceptChronology<?> createNewConcept()  throws IOException {
		logger.info("Creating concept " + fsn + " in DB");
		AppContext.getRuntimeGlobals().disableAllCommitListeners();
		try {
			
			ConceptBuilderService conceptBuilderService = LookupService.getService(ConceptBuilderService.class);
			DescriptionBuilderService descriptionBuilderService = LookupService.getService(DescriptionBuilderService.class);
			LogicalExpressionBuilderService expressionBuilderService = LookupService.getService(LogicalExpressionBuilderService.class);
			
			//ConceptBuilderService
			conceptBuilderService.setDefaultLanguageForDescriptions(IsaacMetadataAuxiliaryBinding.ENGLISH);
			conceptBuilderService.setDefaultDialectAssemblageForDescriptions(IsaacMetadataAuxiliaryBinding.US_ENGLISH_DIALECT);
			conceptBuilderService.setDefaultLogicCoordinate(LogicCoordinates.getStandardElProfile());

			//Parents
			LogicalExpressionBuilder parentBuilder = expressionBuilderService.getLogicalExpressionBuilder();
			ConceptAssertion[] parentConceptAssertions = new ConceptAssertion[parents.size()];
			for(int i = 0; i < parents.size(); ++i) {
				int parentNid = parents.get(i);
				parentConceptAssertions[i] = LogicalExpressionBuilder.ConceptAssertion(
					Get.conceptService().getConcept(parentNid), parentBuilder);
			} 
			LogicalExpressionBuilder.NecessarySet(
					LogicalExpressionBuilder.And(parentConceptAssertions));
			LogicalExpression parentsDef = parentBuilder.build();
			
			// TODO Remove semantic tag from FSN, use as second arg
			ConceptBuilder conBuilder = conceptBuilderService.getDefaultConceptBuilder(OchreUtility.stripSemanticTag(this.fsn), OchreUtility.getSemanticTag(this.fsn), parentsDef);
			
			//Descriptions
			for (int i = 0; i < getSynonymsCreated(); i++) {
				DescriptionBuilder<? extends SememeChronology<?>, ? extends MutableDescriptionSememe<?>> descBuilder = 
						descriptionBuilderService.getDescriptionBuilder(syns.get(i).getTerm(), 
																conBuilder, 
																getType(i),
																IsaacMetadataAuxiliaryBinding.ENGLISH);
				descBuilder.setPreferredInDialectAssemblage(IsaacMetadataAuxiliaryBinding.US_ENGLISH_DIALECT);
				Get.commitService().addUncommitted(descBuilder.build(ExtendedAppContext.getUserProfileBindings().getEditCoordinate().get(), ChangeCheckerMode.ACTIVE, new ArrayList<>())); //TODO verify commit
				conBuilder.addDescription(descBuilder);
			}
			
			//Preferred Term
//			DescriptionBuilder<? extends SememeChronology<?>, ? extends MutableDescriptionSememe<?>> definitionBuilderPT 
//					= descriptionBuilderService.getDescriptionBuilder(this.prefTerm, conBuilder, IsaacMetadataAuxiliaryBinding.PREFERRED, IsaacMetadataAuxiliaryBinding.ENGLISH);
//			definitionBuilderPT.setPreferredInDialectAssemblage(IsaacMetadataAuxiliaryBinding.US_ENGLISH_DIALECT);
//			definitionBuilderPT.build(ExtendedAppContext.getUserProfileBindings().getEditCoordinate().get(), ChangeCheckerMode.ACTIVE);
//			Get.commitService().addUncommitted(definitionBuilderPT.build(ExtendedAppContext.getUserProfileBindings().getEditCoordinate().get(), ChangeCheckerMode.ACTIVE)); //TODO verify commit
//			conBuilder.addDescription(definitionBuilderPT);
			
			//FSN
//			DescriptionBuilder<? extends SememeChronology<?>, ? extends MutableDescriptionSememe<?>>  definitionBuilderFSN = 
//					descriptionBuilderService.
//					getDescriptionBuilder(this.fsn, conBuilder,
//							IsaacMetadataAuxiliaryBinding.FULLY_SPECIFIED_NAME,
//							IsaacMetadataAuxiliaryBinding.ENGLISH);
//			definitionBuilderFSN.setPreferredInDialectAssemblage(IsaacMetadataAuxiliaryBinding.US_ENGLISH_DIALECT);
//			definitionBuilderFSN.build(ExtendedAppContext.getUserProfileBindings().getEditCoordinate().get(), ChangeCheckerMode.ACTIVE); //TODO - build each descBuilder?
//			Get.commitService().addUncommitted(definitionBuilderFSN.build(ExtendedAppContext.getUserProfileBindings().getEditCoordinate().get(), ChangeCheckerMode.ACTIVE)); //TODO verify commit
//			conBuilder.addDescription(definitionBuilderFSN);
			
			// Creat Hash Table of Group ID's grouping relationships with Group ID;'s
			// IE: rels with group ID 1 all get into one group, group with rel group of 3 all go togethor
			// All rels with same group identifier need to be AND'd togethor
			// For loop walk through hash table and create rels that way
			
			HashMap<Integer, ArrayList<RelRow>> relMap = new HashMap<Integer, ArrayList<RelRow>>();
			for (int i = 0; i < getRelationshipsCreated(); i++) {
				RelRow thisRel = rels.get(i);
				if(!relMap.containsKey(thisRel.getGroup())) {
					ArrayList<RelRow> relRowList = new ArrayList<RelRow>();
					relRowList.add(thisRel);
					relMap.put(thisRel.getGroup(), relRowList);
				} else {
					ArrayList<RelRow> thisRelList = relMap.get(thisRel.getGroup());
					thisRelList.add(thisRel);
				}
			}
			
			//Relationships
			LogicalExpressionBuilder relBuilder;
			for(int group: relMap.keySet()) {
				relBuilder = expressionBuilderService.getLogicalExpressionBuilder();
				Assertion assertions[] = new Assertion[relMap.get(group) != null ? relMap.get(group).size() : 0];
				for(int i = 0; i < relMap.get(group).size(); ++i) {	
					RelRow rel = relMap.get(group).get(i);
					
					switch (rel.getType()) {
					// Pull Necessary set out of loop
					case Some_Role: {
						ConceptChronology<?> roleTypeChronology = Get.conceptService().getConcept(rel.getRelationshipNid());
						ConceptChronology<?> restrictionConceptChronology = Get.conceptService().getConcept(rel.getTargetNid());
						
						assertions[i] = LogicalExpressionBuilder.SomeRole(roleTypeChronology,
								LogicalExpressionBuilder.ConceptAssertion(restrictionConceptChronology, relBuilder));
						break;
					}
					case All_Role: {
						ConceptChronology<?> roleTypeChronology = Get.conceptService().getConcept(rel.getRelationshipNid());
						ConceptChronology<?> restrictionConceptChronology = Get.conceptService().getConcept(rel.getTargetNid());
						
						assertions[i] = LogicalExpressionBuilder.AllRole(roleTypeChronology,
								LogicalExpressionBuilder.ConceptAssertion(restrictionConceptChronology, relBuilder));
						break;
					}
					default:
						throw new RuntimeException("Unsupported " + RoleType.class.getName() + " value " + rel.getType());
					}
					
					LogicalExpressionBuilder.NecessarySet(LogicalExpressionBuilder.And(assertions));
						
					conBuilder.addLogicalExpression(relBuilder.build());
				}
			}
			
			ConceptChronology<?> newComponentChronology = conBuilder.build(ExtendedAppContext.getUserProfileBindings().getEditCoordinate().get(), ChangeCheckerMode.ACTIVE, new ArrayList<>());
			
			return newComponentChronology;
		}
		finally {
			AppContext.getRuntimeGlobals().enableAllCommitListeners(); //TODO - do we want this
		}
	}
}
