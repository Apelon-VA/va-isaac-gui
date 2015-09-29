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
package gov.va.isaac.gui.conceptview.descriptions;

import gov.va.isaac.ExtendedAppContext;
import gov.va.isaac.gui.conceptview.data.ConceptDescription;
import gov.va.isaac.gui.conceptview.descriptions.ScreensController.ModificationType;
import gov.va.isaac.gui.conceptview.descriptions.wizardPages.TermRow;
import gov.vha.isaac.metadata.source.IsaacMetadataAuxiliaryBinding;
import gov.vha.isaac.ochre.api.ConceptProxy;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.LookupService;
import gov.vha.isaac.ochre.api.State;
import gov.vha.isaac.ochre.api.chronicle.LatestVersion;
import gov.vha.isaac.ochre.api.commit.ChangeCheckerMode;
import gov.vha.isaac.ochre.api.component.concept.description.DescriptionBuilder;
import gov.vha.isaac.ochre.api.component.concept.description.DescriptionBuilderService;
import gov.vha.isaac.ochre.api.component.sememe.SememeChronology;
import gov.vha.isaac.ochre.api.component.sememe.version.DescriptionSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.DynamicSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.MutableDescriptionSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.SememeVersion;
import gov.vha.isaac.ochre.impl.lang.LanguageCode;

import java.util.ArrayList;
import java.util.Optional;
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
 */
public class WizardController {

    private static Logger logger = LoggerFactory.getLogger(WizardController.class);
    private TermRow term;
    private ModificationType modType;
    private ConceptDescription editDesc;
    private int conceptSequence;

	public void setNewDescription(TermRow term) {
		this.term = term;
	}

	public String getLanguage(int i) {
		return LanguageCode.EN_US.getFormatedLanguageCode();
	}

	// TODO make sure PT and FSN are case insensitive

	public int getNid(UUID input) {
		return Get.identifierService().getNidForUuids(input);
	}

        public String getTermText() {
            return term.getTerm();
        }
/*
        public ConceptChronology<?> createNewTerm()  throws IOException {
		logger.info("Creating concept " + fsn + " " + prefTerm + " in DB");
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
			for(int parent : parents) {
				// TODO compare with UserCreationWizard...
				LogicalExpressionBuilder.NecessarySet(LogicalExpressionBuilder.And(LogicalExpressionBuilder.ConceptAssertion(
						Get.conceptService().getConcept(parent), parentBuilder)));
			} 
			LogicalExpression parentsDef = parentBuilder.build();
			
			ConceptBuilder conBuilder = conceptBuilderService.getDefaultConceptBuilder(this.fsn, OchreUtility.getSemanticTag(this.fsn), parentsDef);
			
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
			DescriptionBuilder<? extends SememeChronology<?>, ? extends MutableDescriptionSememe<?>> definitionBuilderPT 
					= descriptionBuilderService.getDescriptionBuilder(this.prefTerm, conBuilder, IsaacMetadataAuxiliaryBinding.PREFERRED, IsaacMetadataAuxiliaryBinding.ENGLISH);
			definitionBuilderPT.setPreferredInDialectAssemblage(IsaacMetadataAuxiliaryBinding.US_ENGLISH_DIALECT);
			definitionBuilderPT.build(ExtendedAppContext.getUserProfileBindings().getEditCoordinate().get(), ChangeCheckerMode.ACTIVE);
			Get.commitService().addUncommitted(definitionBuilderPT.build(ExtendedAppContext.getUserProfileBindings().getEditCoordinate().get(), ChangeCheckerMode.ACTIVE)); //TODO verify commit
			conBuilder.addDescription(definitionBuilderPT);
			
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
				for(RelRow rel : relMap.get(group)) {					
					switch (rel.getType()) {
					case Some_Role: {
						ConceptChronology<?> roleTypeChronology = Get.conceptService().getConcept(rel.getRelationshipNid());
						ConceptChronology<?> restrictionConceptChronology = Get.conceptService().getConcept(rel.getTargetNid());
						LogicalExpressionBuilder.NecessarySet(
								LogicalExpressionBuilder.And(
										LogicalExpressionBuilder.SomeRole(roleTypeChronology,
												LogicalExpressionBuilder.ConceptAssertion(restrictionConceptChronology, relBuilder))));
						break;
					}
					case All_Role: {
						ConceptChronology<?> roleTypeChronology = Get.conceptService().getConcept(rel.getRelationshipNid());
						ConceptChronology<?> restrictionConceptChronology = Get.conceptService().getConcept(rel.getTargetNid());
						LogicalExpressionBuilder.NecessarySet(
								LogicalExpressionBuilder.And(
										LogicalExpressionBuilder.AllRole(roleTypeChronology,
												LogicalExpressionBuilder.ConceptAssertion(restrictionConceptChronology, relBuilder))));
						break;
					}
					default:
						throw new RuntimeException("Unsupported " + RoleType.class.getName() + " value " + rel.getType());
					}
						
					conBuilder.addLogicalExpression(relBuilder.build());
				}
			}
			
			ConceptChronology<?> newComponentChronology = conBuilder.build(ExtendedAppContext.getUserProfileBindings().getEditCoordinate().get(), ChangeCheckerMode.ACTIVE, new ArrayList<>());
			
			Get.commitService().addUncommitted(newComponentChronology);
			
			return newComponentChronology;
		}
		finally {
			AppContext.getRuntimeGlobals().enableAllCommitListeners(); //TODO - do we want this
		}
	}
*/

    public boolean isNew() {
        return modType == ModificationType.NEW;
    }

    void setModificationType(ScreensController.ModificationType type) {
        modType = type; 
    }

    void setEditDescription(ConceptDescription desc) {
        editDesc = desc;
    }
    
    public ConceptDescription getEditDescription() {
        return editDesc;
    }

    public UUID persistDescription() {
        if (isNew()) {
            DescriptionBuilderService descriptionBuilderService = LookupService.getService(DescriptionBuilderService.class);
            ConceptProxy languageProxy = IsaacMetadataAuxiliaryBinding.ENGLISH;
            ConceptProxy dialectProxy = IsaacMetadataAuxiliaryBinding.US_ENGLISH_DIALECT;

            DescriptionBuilder<? extends SememeChronology<?>, ? extends MutableDescriptionSememe<?>> descBuilder = 
                            descriptionBuilderService.getDescriptionBuilder(getTermText(), 
                                                                            conceptSequence, 
                                                                            term.getType(),
                                                                            IsaacMetadataAuxiliaryBinding.ENGLISH);
            
            DescriptionBuilder<?, ?> descSemChron = descBuilder.setPreferredInDialectAssemblage(IsaacMetadataAuxiliaryBinding.US_ENGLISH_DIALECT);
            return descSemChron.build(ExtendedAppContext.getUserProfileBindings().getEditCoordinate().get(), ChangeCheckerMode.ACTIVE, new ArrayList<>()).getPrimordialUuid();
        } else {
            // get chronology for the desc here
		MutableDescriptionSememe<?> ds = readLatestDescriptionFromUuid(editDesc.getPrimordialUuid());

                MutableDescriptionSememe<?> mds = ((SememeChronology<DescriptionSememe<?>>)ds.getChronology()).createMutableVersion(MutableDescriptionSememe.class, State.ACTIVE,
                                                    ExtendedAppContext.getUserProfileBindings().getEditCoordinate().get());
                
                mds.setCaseSignificanceConceptSequence(term.getSignificance().getConceptSequence());
                mds.setDescriptionTypeConceptSequence(term.getType().getConceptSequence());
                mds.setLanguageConceptSequence(term.getLanguage().getConceptSequence());
                mds.setText(getTermText());
                
                Get.commitService().addUncommitted(ds.getChronology());      
                
                return editDesc.getPrimordialUuid();
        }
    }

    private static MutableDescriptionSememe<?> readLatestDescriptionFromUuid(UUID descUUID) throws RuntimeException
    {
            SememeChronology<? extends SememeVersion<?>> sc = Get.sememeService().getSememe(Get.identifierService().getSememeSequenceForUuids(descUUID));
            @SuppressWarnings({ "unchecked", "rawtypes" })
            Optional<LatestVersion<MutableDescriptionSememe<?>>> latest = ((SememeChronology)sc).getLatestVersion(MutableDescriptionSememe.class, 
                            ExtendedAppContext.getUserProfileBindings().getStampCoordinate().get().makeAnalog(State.ACTIVE, State.INACTIVE));

            return latest.get().value();
    }
    
    void setConcept(int conSeq) {
        conceptSequence = conSeq;
    }
}
