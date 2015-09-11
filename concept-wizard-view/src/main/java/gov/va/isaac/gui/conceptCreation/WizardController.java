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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.ihtsdo.otf.tcc.api.blueprint.IdDirective;
import org.ihtsdo.otf.tcc.api.blueprint.InvalidCAB;
import org.ihtsdo.otf.tcc.api.blueprint.RelationshipCAB;
import org.ihtsdo.otf.tcc.api.contradiction.ContradictionException;
import org.ihtsdo.otf.tcc.api.relationship.RelationshipType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static gov.vha.isaac.ochre.api.logic.LogicalExpressionBuilder.And;
import static gov.vha.isaac.ochre.api.logic.LogicalExpressionBuilder.ConceptAssertion;
import static gov.vha.isaac.ochre.api.logic.LogicalExpressionBuilder.NecessarySet;

import gov.va.isaac.AppContext;
import gov.va.isaac.ExtendedAppContext;
import gov.va.isaac.config.profiles.UserProfile;
import gov.va.isaac.config.profiles.UserProfileManager;
import gov.va.isaac.gui.conceptCreation.wizardPages.RelRow;
import gov.va.isaac.gui.conceptCreation.wizardPages.TermRow;
import gov.va.isaac.util.OTFUtility;
import gov.va.isaac.util.OchreUtility;
import gov.vha.isaac.metadata.coordinates.EditCoordinates;
import gov.vha.isaac.metadata.coordinates.LogicCoordinates;
import gov.vha.isaac.metadata.source.IsaacMetadataAuxiliaryBinding;
import gov.vha.isaac.ochre.api.ConceptProxy;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.LookupService;
import gov.vha.isaac.ochre.api.chronicle.ObjectChronology;
import gov.vha.isaac.ochre.api.commit.ChangeCheckerMode;
import gov.vha.isaac.ochre.api.component.concept.ConceptBuilder;
import gov.vha.isaac.ochre.api.component.concept.ConceptBuilderService;
import gov.vha.isaac.ochre.api.component.concept.ConceptChronology;
import gov.vha.isaac.ochre.api.component.concept.ConceptVersion;
import gov.vha.isaac.ochre.api.component.concept.description.DescriptionBuilder;
import gov.vha.isaac.ochre.api.component.concept.description.DescriptionBuilderService;
import gov.vha.isaac.ochre.api.component.sememe.SememeChronology;
import gov.vha.isaac.ochre.api.component.sememe.version.MutableDescriptionSememe;
import gov.vha.isaac.ochre.api.coordinate.EditCoordinate;
import gov.vha.isaac.ochre.api.logic.LogicalExpression;
import gov.vha.isaac.ochre.api.logic.LogicalExpressionBuilder;
import gov.vha.isaac.ochre.api.logic.LogicalExpressionBuilderService;
import gov.vha.isaac.ochre.impl.lang.LanguageCode;
import gov.vha.isaac.ochre.model.coordinate.EditCoordinateImpl;

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
	private String fsn;
	private String prefTerm;
	private List<Integer> parents;
	private boolean isPrimitive;
	private List<TermRow> syns;
	private List<RelRow> rels;
	
	private UserProfile userProfile = AppContext.getService(UserProfileManager.class).getCurrentlyLoggedInUserProfile();
    //TODO - is this correct
    private EditCoordinate editCoordinate = new EditCoordinateImpl(getNid(userProfile.getConceptUUID()), getNid(userProfile.getEditCoordinateModule()), getNid(userProfile.getEditCoordinatePath()));

	public void setConceptDefinitionVals(String fsn, String prefTerm, List<Integer> parents, boolean isPrimitive) {
		this.fsn = fsn;
		this.prefTerm = prefTerm;
		this.parents = parents;
		this.isPrimitive = isPrimitive;
	}

	public void setConceptComponents(List<TermRow> syns, List<RelRow> rels) {
		this.syns = syns;
		this.rels = rels;
	}

	public String getConceptFSN() {
		return fsn;
	}

	public String getConceptPT() {
		return prefTerm;
	}

	public String getConceptPrimDef() {
		if (isPrimitive) {
			return "Primitive";
		} else {
			return "Fully Defined";
		}
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
		// TODO Why are we implementing a toString on Reltype? ROLE isn't even
		// an option in the enum
		if (RelationshipType.QUALIFIER == rels.get(i).getType()) {
			return "Qualifier";
		} else {
			return "Role";
		}
	}

	public String getGroup(int i) {
		return String.valueOf(rels.get(i).getGroup());
	}
	
	public int getNid(UUID input) {
		return Get.identifierService().getNidForUuids(input);
	}

	public ConceptChronology createNewConcept()  throws IOException {
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

            //Parent
            LogicalExpressionBuilder defBuilder = expressionBuilderService.getLogicalExpressionBuilder();
            NecessarySet(And(ConceptAssertion(
            		Get.conceptService().getConcept(parents.remove(0)), defBuilder))); //TODO - verify this. Possibly check if parent size > 0
            LogicalExpression conceptDef = defBuilder.build();
            
            //ConceptBuilder
            ConceptBuilder conBuilder = conceptBuilderService.getDefaultConceptBuilder(this.fsn, OchreUtility.getSemanticTag(this.fsn), conceptDef);
            
            //Preferred Term
            DescriptionBuilder<? extends SememeChronology<?>, ? extends MutableDescriptionSememe<?>> definitionBuilderPT 
            		= descriptionBuilderService.getDescriptionBuilder(this.prefTerm, conBuilder, IsaacMetadataAuxiliaryBinding.PREFERRED, IsaacMetadataAuxiliaryBinding.ENGLISH);
            definitionBuilderPT.setPreferredInDialectAssemblage(IsaacMetadataAuxiliaryBinding.US_ENGLISH_DIALECT);
            definitionBuilderPT.build(ExtendedAppContext.getUserProfileBindings().getEditCoordinate().get(), ChangeCheckerMode.ACTIVE);
            // Get.commitService().addUncommitted(definitionBuilderPT); //TODO - commit
            conBuilder.addDescription(definitionBuilderPT);
            
            //FSN
			DescriptionBuilder definitionBuilderFSN = descriptionBuilderService.
                    getDescriptionBuilder(this.fsn, conBuilder,
                            IsaacMetadataAuxiliaryBinding.FULLY_SPECIFIED_NAME,
                            IsaacMetadataAuxiliaryBinding.ENGLISH);
			definitionBuilderFSN.setPreferredInDialectAssemblage(IsaacMetadataAuxiliaryBinding.US_ENGLISH_DIALECT);
			definitionBuilderFSN.build(ExtendedAppContext.getUserProfileBindings().getEditCoordinate().get(), ChangeCheckerMode.ACTIVE); //TODO - build each descBuilder?
			// Get.commitService().addUncommitted(definitionBuilderPT); //TODO - Add Uncommitted
            conBuilder.addDescription(definitionBuilderFSN);
            
            for(int parent : parents) {
            	//TODO - add each parent here 
            }
            
            //TODO - generate UUID?
    
            ConceptChronology<?> chronology = conBuilder.build(ExtendedAppContext.getUserProfileBindings().getEditCoordinate().get(), ChangeCheckerMode.ACTIVE, new ArrayList<>());
            
            return chronology;
        }
        finally {
            AppContext.getRuntimeGlobals().enableAllCommitListeners(); //TODO - do we want this
        }
        //OLD CODE TODO check these are all satisfied
//		String fsn = this.fsn;
//		String prefTerm = this.prefTerm;
//		logger.debug("Creating concept {}", fsn);
//		UUID isA = Snomed.IS_A.getUuids()[0];
//		UUID parentCons[] = new UUID[parents.size()];
//		for (int i = 0; i < parents.size(); i++) {
//			parentCons[i] = parents.get(i).getPrimordialUuid();
//		}
//		IdDirective idDir = IdDirective.GENERATE_HASH;
//		LanguageCode lc = LanguageCode.EN_US;
//		UUID module = Snomed.CORE_MODULE.getLenient().getPrimordialUuid();
//		ConceptCB newConCB = new ConceptCB(fsn, prefTerm, lc, isA, idDir, module,
//				IsaacMetadataAuxiliaryBinding.DEVELOPMENT.getPrimodialUuid(), parentCons);
//		newConCB.setDefined(!isPrimitive);
//		ConceptChronicle newCon = OTFUtility.getBuilder().construct(newConCB);
		// OTFUtility.addUncommitted(newCon);
//		return newCon;
	}

	public void createNewDescription(int conceptSequence, int i) throws IOException {
		
		DescriptionBuilderService descriptionBuilderService = LookupService.getService(DescriptionBuilderService.class); //TODO - twice?
		
		//
		DescriptionBuilder<? extends SememeChronology<?>, ? extends MutableDescriptionSememe<?>> descBuilder = descriptionBuilderService.getDescriptionBuilder(syns.get(i).getTerm(), 
														conceptSequence, 
														getType(i),
														IsaacMetadataAuxiliaryBinding.ENGLISH);
		descBuilder.setPreferredInDialectAssemblage(IsaacMetadataAuxiliaryBinding.US_ENGLISH_DIALECT);
		Get.commitService().addUncommitted(descBuilder.build(ExtendedAppContext.getUserProfileBindings().getEditCoordinate().get(), ChangeCheckerMode.ACTIVE));
        //TODO - isCaseSig desc attribute goes where?
        
        //OLD CODE
//		DescriptionCAB newDesc = new DescriptionCAB(con.getConceptNid(), getTypeNid(i), LanguageCode.EN_US,
//				syns.get(i).getTerm(), syns.get(i).isInitialCaseSig(), IdDirective.GENERATE_HASH);
//		OTFUtility.getBuilder().construct(newDesc);
		// OTFUtility.addUncommitted(con);
	}

	public void createNewRelationship(int conceptSequence, int i) throws IOException {
		RelationshipCAB newRel;
		try {
			newRel = new RelationshipCAB(conceptSequence, rels.get(i).getRelationshipNid(),
					rels.get(i).getTargetNid(), rels.get(i).getGroup(), rels.get(i).getType(), IdDirective.GENERATE_HASH);
		} catch (InvalidCAB e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ContradictionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//OTFUtility.getBuilder().construct(newRel);
		// OTFUtility.addUncommitted(con);
	}
}
