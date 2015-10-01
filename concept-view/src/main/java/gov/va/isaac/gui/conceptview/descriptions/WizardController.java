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
import gov.va.isaac.gui.conceptview.descriptions.wizardPages.AcceptabilityRow;
import gov.va.isaac.gui.conceptview.descriptions.wizardPages.TermRow;
import gov.vha.isaac.metadata.coordinates.LanguageCoordinates;
import gov.vha.isaac.metadata.coordinates.StampCoordinates;
import gov.vha.isaac.metadata.source.IsaacMetadataAuxiliaryBinding;
import gov.vha.isaac.ochre.api.ConceptProxy;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.LookupService;
import gov.vha.isaac.ochre.api.State;
import gov.vha.isaac.ochre.api.chronicle.LatestVersion;
import gov.vha.isaac.ochre.api.commit.ChangeCheckerMode;
import gov.vha.isaac.ochre.api.component.concept.ConceptChronology;
import gov.vha.isaac.ochre.api.component.concept.ConceptVersion;
import gov.vha.isaac.ochre.api.component.concept.description.DescriptionBuilder;
import gov.vha.isaac.ochre.api.component.concept.description.DescriptionBuilderService;
import gov.vha.isaac.ochre.api.component.sememe.SememeBuilder;
import gov.vha.isaac.ochre.api.component.sememe.SememeBuilderService;
import gov.vha.isaac.ochre.api.component.sememe.SememeChronology;
import gov.vha.isaac.ochre.api.component.sememe.SememeType;
import gov.vha.isaac.ochre.api.component.sememe.version.ComponentNidSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.DescriptionSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.MutableComponentNidSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.MutableDescriptionSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.SememeVersion;
import gov.vha.isaac.ochre.api.coordinate.StampCoordinate;
import gov.vha.isaac.ochre.impl.lang.LanguageCode;
import gov.vha.isaac.ochre.impl.utility.Frills;
import gov.vha.isaac.ochre.model.coordinate.StampCoordinateImpl;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
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
    private TermRow modifiedTerm;
    private ModificationType modType;
    private ConceptDescription editDesc = null;
    private ConceptChronology<?> conChron;
    private StampCoordinate stampCoordinate;

    public void setModifiedDescription(TermRow term) {
        this.modifiedTerm = term;
    }

    public String getLanguage(int i) {
        return LanguageCode.EN_US.getFormatedLanguageCode();
    }

    // TODO make sure PT and FSN are case insensitive

    public int getNid(UUID input) {
        return Get.identifierService().getNidForUuids(input);
    }

    public String getTermText() {
        return modifiedTerm.Text();
    }


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

    /*
            for (AcceptabilityRow acceptSelect : acceptabilitySelections) {
            DescriptionBuilder<?, ?> descSemChron = null; 

            if (acceptSelect.isPreferred()) {
            descSemChron = descBuilder.setPreferredInDialectAssemblage(acceptSelect.getDialect());
            } else {
            descSemChron = descBuilder.setAcceptableInDialectAssemblage(acceptSelect.getDialect());
            }
        }


    */

    public UUID persistDescription(ArrayList<AcceptabilityRow> acceptabilitySelections) {
        if (isNew()) {
            DescriptionBuilderService descriptionBuilderService = LookupService.getService(DescriptionBuilderService.class);
    
            DescriptionBuilder<? extends SememeChronology<?>, ? extends MutableDescriptionSememe<?>> descBuilder = 
                    descriptionBuilderService.getDescriptionBuilder(getTermText(), 
                                            conChron.getConceptSequence(), 
                                            new ConceptProxy(modifiedTerm.getType()),
                                            new ConceptProxy(modifiedTerm.getLanguage()));
           
            SememeChronology<?> newDescription = descBuilder.build(ExtendedAppContext.getUserProfileBindings().getEditCoordinate().get(), ChangeCheckerMode.ACTIVE, new ArrayList<>());
            
            handleAcceptability(newDescription.getNid(), acceptabilitySelections);
            
            return newDescription.getPrimordialUuid();
        } else {
            // get chronology for the desc here
            MutableDescriptionSememe<?> ds = readLatestDescriptionFromUuid(editDesc.getPrimordialUuid());
    
            MutableDescriptionSememe<?> mds = ((SememeChronology<DescriptionSememe<?>>)ds.getChronology()).createMutableVersion(MutableDescriptionSememe.class, State.ACTIVE,
                                ExtendedAppContext.getUserProfileBindings().getEditCoordinate().get());
            
            mds.setCaseSignificanceConceptSequence(modifiedTerm.getSignificance());
            mds.setDescriptionTypeConceptSequence(modifiedTerm.getType());
            mds.setLanguageConceptSequence(modifiedTerm.getLanguage());
            mds.setText(getTermText());
            
            Get.commitService().addUncommitted(ds.getChronology());      
    
            handleAcceptability(mds.getNid(), acceptabilitySelections);
            
            return editDesc.getPrimordialUuid();
        }
    }
    
    private void handleAcceptability(int descNid, ArrayList<AcceptabilityRow> acceptabilitySelections) {
        if (acceptabilitySelections != null && !acceptabilitySelections.isEmpty()) {
            SememeBuilderService sememeBuilderService = LookupService.getService(SememeBuilderService.class);
            
            // Get all ACT/INACT latest versions
            EnumSet<State> allStates = EnumSet.allOf(State.class);
            StampCoordinate actInact = new StampCoordinateImpl(stampCoordinate.getStampPrecedence(), 
                                        stampCoordinate.getStampPosition(), 
                                        stampCoordinate.getModuleSequences(), 
                                        allStates);
    
            Map<Integer, ComponentNidSememe> existingPairs = getAcceptabilitySememes(descNid, actInact);
            
            for (AcceptabilityRow acceptSelect : acceptabilitySelections) {
                SememeBuilder compBuilder = null;
                
                if (!existingPairs.keySet().contains(acceptSelect.getDialect())) {
                    // addNew
                    if (acceptSelect.isPreferred()) {
                        compBuilder = sememeBuilderService.getComponentSememeBuilder(
                            IsaacMetadataAuxiliaryBinding.PREFERRED.getNid(), descNid,
                            acceptSelect.getDialect());
                    } else {
                        compBuilder = sememeBuilderService.getComponentSememeBuilder(
                            IsaacMetadataAuxiliaryBinding.ACCEPTABLE.getNid(), descNid,
                            acceptSelect.getDialect());
                    }
                    
                    if (compBuilder != null) {
                        compBuilder.build(ExtendedAppContext.getUserProfileBindings().getEditCoordinate().get(), ChangeCheckerMode.ACTIVE, new ArrayList<>());
                    }        
                } else {
                    // See if any changed or impliciately must be reactivated
                    ComponentNidSememe oldVer = existingPairs.get(acceptSelect.getDialect());
                    MutableComponentNidSememe<?> newVer = ((SememeChronology<ComponentNidSememe<?>>)oldVer.getChronology()).createMutableVersion(MutableComponentNidSememe.class, State.ACTIVE,
                                    ExtendedAppContext.getUserProfileBindings().getEditCoordinate().get());
        
                    Integer acceptabilityNid = oldVer.getComponentNid();

                    boolean updateVersion = false;
        
                    if (acceptSelect.isPreferred() && acceptabilityNid != IsaacMetadataAuxiliaryBinding.PREFERRED.getNid()) {
                        newVer.setComponentNid(IsaacMetadataAuxiliaryBinding.PREFERRED.getNid());
                        updateVersion = true;
                    } else if (acceptSelect.isAcceptable() && acceptabilityNid != IsaacMetadataAuxiliaryBinding.ACCEPTABLE.getNid()) {
                        newVer.setComponentNid(IsaacMetadataAuxiliaryBinding.ACCEPTABLE.getNid());
                        updateVersion = true;
                    } else if (oldVer.getState() != State.ACTIVE) {
                        updateVersion = true;
                    }
                    
                    if (updateVersion) {
                        Get.commitService().addUncommitted(newVer.getChronology());      
                    } else if (acceptSelect.isNeither()) {
                        // Retire b/c specified "Neither" option
                        newVer = ((SememeChronology<ComponentNidSememe<?>>)oldVer.getChronology()).createMutableVersion(MutableComponentNidSememe.class, State.INACTIVE,
                        ExtendedAppContext.getUserProfileBindings().getEditCoordinate().get());
                        Get.commitService().addUncommitted(newVer.getChronology());      
                    }
                }
            }
    
            // See if any to be retired b/c person removed existing row (rather than b/c of explicit "Neither" selection
            for (Integer dialect : existingPairs.keySet()) {
                    boolean dialectFound = false;
                    for (AcceptabilityRow acceptSelect : acceptabilitySelections) {
                    if (acceptSelect.getDialect() == dialect) {
                        dialectFound = true;
                        break;
                    }
                }
                
                if (!dialectFound) {
                // implicit retire
                ComponentNidSememe oldVer = existingPairs.get(dialect);
                MutableComponentNidSememe<?> newVer = ((SememeChronology<ComponentNidSememe<?>>)oldVer.getChronology()).createMutableVersion(MutableComponentNidSememe.class, State.INACTIVE,
                    ExtendedAppContext.getUserProfileBindings().getEditCoordinate().get());
                Get.commitService().addUncommitted(newVer.getChronology());      
                }
            }
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
    
    void setConcept(ConceptChronology<?> con) {
        conChron = con;
    }

    void setStampCoordinate(StampCoordinate stampCoord) {
        stampCoordinate = stampCoord;
    }
    public boolean needsAcceptabilityScreen() {
        if (!isNew() &&
            modifiedTerm.getType() == IsaacMetadataAuxiliaryBinding.DEFINITION_DESCRIPTION_TYPE.getConceptSequence()) {
            return false;
        } else if (isNew() && 
                (!conceptHasSctModule() || 
                modifiedTerm.getType() == IsaacMetadataAuxiliaryBinding.DEFINITION_DESCRIPTION_TYPE.getConceptSequence())) {
            return false;
        }
        
        return true;
    }

    private boolean conceptHasSctModule() {
       for (ConceptVersion<?> conVer : conChron.getVersionList()) {
           int modSeq = conVer.getModuleSequence();
	
           if (Get.identifierService().getConceptNid(modSeq) == IsaacMetadataAuxiliaryBinding.SNOMED_CT_CORE_MODULE.getNid()) {
               return true;
           }
       }
       
       return false;
    }
    
    public Map<Integer, Integer> getAcceptabilitiesForEditDesc() {
        int descNid = Get.identifierService().getNidForUuids(editDesc.getPrimordialUuid());
        
        return Frills.getAcceptabilities(descNid, stampCoordinate);
    }

    public String getLanguageString() {
        return modifiedTerm.getLanguageString();
    }
    
    public static Map<Integer, ComponentNidSememe> getAcceptabilitySememes(int descriptionSememeNid, StampCoordinate stamp) throws RuntimeException
    {
        Map<Integer, ComponentNidSememe> dialectSequenceToAcceptabilityNidMap = new ConcurrentHashMap<>();
    
        Get.sememeService().getSememesForComponent(descriptionSememeNid).forEach(nestedSememe ->
        {
            if (nestedSememe.getSememeType() == SememeType.COMPONENT_NID)
            {
                int dialectSequence = nestedSememe.getAssemblageSequence();

                @SuppressWarnings({ "rawtypes", "unchecked" })
                Optional<LatestVersion<ComponentNidSememe>> latest = ((SememeChronology)nestedSememe).getLatestVersion(ComponentNidSememe.class, 
                        stamp == null ? Get.configurationService().getDefaultStampCoordinate() : stamp);

                if (latest.isPresent())
                {
                    if (latest.get().value().getComponentNid() == IsaacMetadataAuxiliaryBinding.PREFERRED.getNid()
                            || latest.get().value().getComponentNid() == IsaacMetadataAuxiliaryBinding.ACCEPTABLE.getNid()
                            ) {
                        if (dialectSequenceToAcceptabilityNidMap.get(dialectSequence) != null
                                && dialectSequenceToAcceptabilityNidMap.get(dialectSequence).getSememeSequence() != latest.get().value().getSememeSequence()) {
                            throw new RuntimeException("contradictory annotations about acceptability!");
                        } else {
                            dialectSequenceToAcceptabilityNidMap.put(dialectSequence, latest.get().value());
                        }
                    } else {
                        UUID uuid = null;
                        String componentDesc = null;
                        try {
                            Optional<UUID> uuidOptional = Get.identifierService().getUuidPrimordialForNid(latest.get().value().getComponentNid());
                            if (uuidOptional.isPresent()) {
                                uuid = uuidOptional.get();
                            }
                            Optional<LatestVersion<DescriptionSememe<?>>> desc = Get.conceptService().getSnapshot(StampCoordinates.getDevelopmentLatest(), LanguageCoordinates.getUsEnglishLanguageFullySpecifiedNameCoordinate()).getDescriptionOptional(latest.get().value().getComponentNid());
                            componentDesc = desc.isPresent() ? desc.get().value().getText() : null;
                        } catch (Exception e) {
                            // NOOP
                        }
                    }
                }
            }
        });
        return dialectSequenceToAcceptabilityNidMap;
    }
}
