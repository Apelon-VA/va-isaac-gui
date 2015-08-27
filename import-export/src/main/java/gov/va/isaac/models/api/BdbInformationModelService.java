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
package gov.va.isaac.models.api;

import java.beans.PropertyVetoException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.ihtsdo.otf.query.lucene.indexers.DescriptionIndexer;
import org.ihtsdo.otf.tcc.api.blueprint.ConceptCB;
import org.ihtsdo.otf.tcc.api.blueprint.DescriptionCAB;
import org.ihtsdo.otf.tcc.api.blueprint.IdDirective;
import org.ihtsdo.otf.tcc.api.blueprint.InvalidCAB;
import org.ihtsdo.otf.tcc.api.blueprint.RefexDirective;
import org.ihtsdo.otf.tcc.api.blueprint.RelationshipCAB;
import org.ihtsdo.otf.tcc.api.concept.ConceptChronicleBI;
import org.ihtsdo.otf.tcc.api.concept.ConceptVersionBI;
import org.ihtsdo.otf.tcc.api.contradiction.ContradictionException;
import org.ihtsdo.otf.tcc.api.lang.LanguageCode;
import org.ihtsdo.otf.tcc.api.metadata.binding.Snomed;
import org.ihtsdo.otf.tcc.api.relationship.RelationshipChronicleBI;
import org.ihtsdo.otf.tcc.api.relationship.RelationshipType;
import org.ihtsdo.otf.tcc.api.relationship.RelationshipVersionBI;
import org.ihtsdo.otf.tcc.api.spec.ValidationException;
import org.ihtsdo.otf.tcc.api.store.TerminologyStoreDI;
import org.ihtsdo.otf.tcc.api.store.Ts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import gov.va.isaac.AppContext;
import gov.va.isaac.ExtendedAppContext;
import gov.va.isaac.model.InformationModelType;
import gov.va.isaac.models.DefaultInformationModelProperty;
import gov.va.isaac.models.InformationModel;
import gov.va.isaac.models.InformationModelMetadata;
import gov.va.isaac.models.InformationModelProperty;
import gov.va.isaac.models.util.DefaultInformationModel;
import gov.va.isaac.util.OTFUtility;
import gov.va.isaac.util.OchreUtility;
import gov.vha.isaac.metadata.source.IsaacMetadataAuxiliaryBinding;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.State;
import gov.vha.isaac.ochre.api.chronicle.LatestVersion;
import gov.vha.isaac.ochre.api.commit.ChangeCheckerMode;
import gov.vha.isaac.ochre.api.component.sememe.SememeChronology;
import gov.vha.isaac.ochre.api.component.sememe.SememeType;
import gov.vha.isaac.ochre.api.component.sememe.version.DynamicSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.MutableSememeVersion;
import gov.vha.isaac.ochre.api.component.sememe.version.SememeVersion;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeDataBI;
import gov.vha.isaac.ochre.api.index.SearchResult;
import gov.vha.isaac.ochre.impl.sememe.DynamicSememeUsageDescription;
import gov.vha.isaac.ochre.model.constants.InformationModelsConstants;
import gov.vha.isaac.ochre.model.sememe.dataTypes.DynamicSememeString;

/**
 * Represents a service for interacting with information models that uses an
 * ISAAC BDB back-end for persistence.
 *
 * @author bcarlsenca
 */
public class BdbInformationModelService implements InformationModelService {

  /** The Constant LOG. */
  private static final Logger LOG = LoggerFactory
      .getLogger(BdbInformationModelService.class);

  /** The data store. */
  private TerminologyStoreDI dataStore;

  /**
   * Instantiates a {@link BdbInformationModelService} from the specified
   * parameters.
   *
   * @param dataStore the data store
   * @throws IOException
   */
  public BdbInformationModelService(TerminologyStoreDI dataStore)
      throws IOException {
    if (dataStore == null) {
      throw new IOException("Data store unexpectedly null");
    }
    this.dataStore = dataStore;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * gov.va.isaac.models.api.InformationModelService#exists(gov.va.isaac.models
   * .InformationModel)
   */
  @Override
  public boolean exists(InformationModel model) throws IOException {
    LOG.info("Check whether information model exists: "
        + model.getType().getDisplayName() + ", " + model.getKey());

    if (model.getType() == null) {
      throw new IOException("Model unexpectedly has null type");
    }
    if (model.getKey() == null) {
      throw new IOException("Model unexpectedly has null key");
    }
    try {
      boolean flag =
          getInformationModel(model.getType(), model.getKey()) != null;
      LOG.debug("  " + flag);
      return flag;
    } catch (ContradictionException e) {
      e.printStackTrace();
      throw new IOException(
          "Unexpected inability to acquire UUID for type and key, "
              + model.getType() + ", " + model.getKey());
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * gov.va.isaac.models.api.InformationModelService#getInformationModel(java
   * .util.UUID)
   */
  @Override
  public InformationModel getInformationModel(UUID uuid) throws IOException,
    ContradictionException {
    if (uuid == null) {
      throw new IOException("UUID unexpectedly null");
    }
    LOG.info("Get information model: " + uuid);
    InformationModel model =
        conceptToInformationModel(dataStore.getConcept(uuid));
    LOG.debug("  " + model.getType().getDisplayName() + ", " + model.getKey());
    return model;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * gov.va.isaac.models.api.InformationModelService#getInformationModel(java
   * .lang.String, java.lang.String)
   */
  @Override
  public InformationModel getInformationModel(InformationModelType type,
    String key) throws IOException, ContradictionException {
    if (type == null) {
      throw new IllegalArgumentException("Unexpected null type parameter.");
    }
    LOG.info("Get information model: " + type.getDisplayName() + ", " + key);
    if (key == null) {
      throw new IllegalArgumentException("Unexpected null key parameter.");
    }
    try {
      LOG.debug("  Lucene Search: '" + key + "'");
      DescriptionIndexer descriptionIndexer =
          AppContext.getService(DescriptionIndexer.class);
      if (descriptionIndexer == null) {
        throw new IOException("No description indexer found, aborting.");
      }
      // Look for description matches.
      int limit = 10;
      List<SearchResult> searchResults;
      searchResults =
          descriptionIndexer.query(key, false, IsaacMetadataAuxiliaryBinding.DESCRIPTION_ASSEMBLAGE.getConceptSequence(), limit, Long.MIN_VALUE);

      // Results are descriptions, need to look up concepts
      for (SearchResult result : searchResults) {
        int conceptNid =
            dataStore.getComponent(result.getNid()).getEnclosingConceptNid();
        ConceptVersionBI conceptVersion =
            OTFUtility.getConceptVersion(conceptNid);
        LOG.debug("    Check " + conceptVersion.getPrimordialUuid() + ", "
            + conceptVersion.getPreferredDescription().getText());

        if (conceptVersion.getPreferredDescription().getText().equals(key)
            || conceptVersion.getFullySpecifiedDescription().getText()
                .equals(key)) {
          LOG.debug("    Found match, compute information model type");
          InformationModelType computedType =
              computeInformationModelType(conceptVersion);
          if (computedType == null) {
            LOG.debug("      type = null");
            continue;
          } else {
            LOG.debug("      type = " + computedType.getDisplayName());
          }
          if (type == computedType) {
            InformationModel model =
                getInformationModel(conceptVersion.getPrimordialUuid());
            LOG.debug("  Model found, UUID = " + model.getUuid());
            return model;
          }
        }
      }
      LOG.debug("  Model not found");
      return null;
    } catch (Exception e) {
      throw new IOException("Unexepected error, " + key, e);
    }

  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * gov.va.isaac.models.api.InformationModelService#getInformationModelChildren
   * (gov.va.isaac.models.InformationModel)
   */
  @Override
  public Set<InformationModel> getInformationModelChildren(
    InformationModel model) throws ValidationException, IOException,
    ContradictionException {
    ConceptVersionBI modelConcept =
        OTFUtility.getConceptVersion(model.getUuid());
    Set<InformationModel> models = new HashSet<>();
    for (RelationshipChronicleBI rel : modelConcept.getRelationshipsIncoming()) {
      RelationshipVersionBI<?> relVersion =
          rel.getVersion(OTFUtility.getViewCoordinate()).get();
      // Look for matching typeId and "active" flag
      if (relVersion.getTypeNid() == Snomed.IS_A.getLenient().getNid()
          && relVersion.isActive()) {
        // Add the model
        models.add(getInformationModel(OTFUtility.getConceptVersion(
            relVersion.getDestinationNid()).getPrimordialUuid()));
      }
    }
    return models;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * gov.va.isaac.models.api.InformationModelService#createOrAmmendInformationModel
   * (gov.va.isaac.models.InformationModel)
   */
  @Override
  public void saveInformationModel(InformationModel model) throws IOException,
    InvalidCAB, ContradictionException, NoSuchAlgorithmException,
    PropertyVetoException {
    if (model == null) {
      throw new IOException("Model unexpectedly null");
    }
    LOG.info("Save information model: " + model.getType().getDisplayName()
        + ", " + model.getKey());
    // disable commit listeners for this operation
    try {
      AppContext.getRuntimeGlobals().disableAllCommitListeners();
      syncInformationModelConcept(model);
    } catch (Exception e) {
      LOG.error("Coudn't Disable WF Init & Commit CEM Information Model", e);
    } finally {
      AppContext.getRuntimeGlobals().enableAllCommitListeners();
    }
  }

  /**
   * Concept to information model.
   *
   * @param modelConcept the model concept
   * @return the information model
   * @throws ContradictionException
   * @throws IOException
   */
  private InformationModel conceptToInformationModel(
    ConceptChronicleBI modelConcept) throws IOException, ContradictionException {
    if (modelConcept == null) {
      throw new IOException("Model concept unexpectedly null");
    }

    LOG.info("Convert concept to information model: "
        + modelConcept.getPrimordialUuid());

    // Key is the FN
    String key = OchreUtility.getFSNForConceptNid(modelConcept.getNid(), null).get();
    LOG.debug("  key = " + key);
    // Name is the PT
    String name = OchreUtility.getPreferredTermForConceptNid(modelConcept.getNid(), null, null).get();
    LOG.debug("  name = " + name);
    // UUID of the concept
    UUID uuid = modelConcept.getPrimordialUuid();
    LOG.debug("  uuid = " + uuid);
    // information model type - obtain by walking up the tree
    InformationModelType type = computeInformationModelType(modelConcept);
    LOG.debug("  type = " + type.getDisplayName());
    InformationModel model = new DefaultInformationModel(key, name, uuid, type);

    // Assemble Metadata
    // Need to look at stamp nid for concept attributes instead of concept
    int stampNid =
        modelConcept.getConceptAttributes()
            .getVersion(OTFUtility.getViewCoordinate()).get().getStamp();
    InformationModelMetadata metadata =
        InformationModelMetadata.newInstance(stampNid, dataStore,
            OTFUtility.getViewCoordinate());
    model.setMetadata(metadata);

    // Check super-model
    // Build associated concept UUIDs from relationships
    for (RelationshipChronicleBI rel : modelConcept.getRelationshipsOutgoing()) {
      RelationshipVersionBI<?> relVersion =
          rel.getVersion(OTFUtility.getViewCoordinate()).get();
      // Look for matching typeId and "active" flag
      if (relVersion.getTypeNid() == Snomed.IS_A.getLenient().getNid()
          && relVersion.isActive()) {
        UUID superModelUuid =
            OTFUtility.getConceptVersion(relVersion.getDestinationNid())
                .getPrimordialUuid();
        // Avoid assigning uuid if parent is the info model type concept
        if (!superModelUuid.equals(model.getType().getUuid())) {
          // set the super model uuid unless already set
          if (model.getSuperModelUuid() != null) {
            throw new IOException(
                "Unexpected multiple lineage in information model concept: "
                    + model.getUuid());
          } else {
            model.setSuperModelUuid(superModelUuid);
          }
        }
      }
    }

    // Get property information from dynamic refset members
    // Create refex entries for properties
    DynamicSememeUsageDescription propertyRefset =
        DynamicSememeUsageDescription.read(InformationModelsConstants.INFORMATION_MODEL_PROPERTIES.getNid());
    LOG.debug("  property refset = " + propertyRefset.getDyanmicSememeName());
    
    Get.sememeService().getSememesForComponentFromAssemblage(modelConcept.getNid(), propertyRefset.getDynamicSememeUsageDescriptorSequence())
    	.forEach(sememeC ->
    	{
    		LOG.debug("  sememe = " + sememeC.toUserString());
    		if (sememeC.getSememeType() == SememeType.DYNAMIC)
    		{
    			@SuppressWarnings({ "unchecked", "rawtypes" })
				Optional<LatestVersion<DynamicSememe<?>>> latest = ((SememeChronology)sememeC).getLatestVersion(DynamicSememe.class, 
    					ExtendedAppContext.getUserProfileBindings().getStampCoordinate().get());
    			
    			if (latest.isPresent())
    			{
    				// Create properties for each annotation
    		        InformationModelProperty property = new DefaultInformationModelProperty();
    		        DynamicSememeDataBI[] data = latest.get().value().getData();
    		        if (data[0] != null)
    		          property.setLabel(((DynamicSememeString) data[0]).getDataString());
    		        if (data[1] != null)
    		          property.setType(((DynamicSememeString) data[1]).getDataString());
    		        if (data[2] != null)
    		          property.setName(((DynamicSememeString) data[2]).getDataString());
    		        if (data[3] != null)
    		          property.setDefaultValue(((DynamicSememeString) data[3]).getDataString());
    		        if (data[4] != null)
    		          property.setValue(((DynamicSememeString) data[4]).getDataString());
    		        if (data[5] != null)
    		          property.setCardinalityMin(((DynamicSememeString) data[5]).getDataString());
    		        if (data[6] != null)
    		          property.setCardinalityMax(((DynamicSememeString) data[6]).getDataString());
    		        if (data[7] != null)
    		          property.setVisibility(((DynamicSememeString) data[7]).getDataString());
    		        LOG.debug("    property " + property.getLabel() + ", " + property.getName());
    		        model.addProperty(property);
    			}
    		}
    	});


    // Build associated concept UUIDs from relationships
    for (RelationshipChronicleBI rel : modelConcept.getRelationshipsOutgoing()) {
      RelationshipVersionBI<?> relVersion =
          rel.getVersion(OTFUtility.getViewCoordinate()).get();
      // Look for matching typeId and "active" flag
      if (relVersion.getTypeNid() == InformationModelsConstants.HAS_TERMINOLOGY_CONCEPT.getNid() && relVersion.isActive()) {
        // Add the destination UUID
        model.addAssociatedConceptUuid(OTFUtility.getConceptVersion(
            relVersion.getDestinationNid()).getPrimordialUuid());
      }
    }

    return model;
  }

  /**
   * Returns the information model type. Walks up the tree from the
   * corresponding concept to the level just below "information models" root and
   * returns the type corresponding with the preferred name of the concept at
   * that level.
   *
   * @param modelConcept the model concept
   * @return the information model type
   * @throws ContradictionException
   * @throws IOException
   */
  private InformationModelType computeInformationModelType(
    ConceptChronicleBI modelConcept) throws IOException, ContradictionException {
    if (modelConcept == null) {
      throw new IOException("Model concept unexpectedly null");
    }
    LOG.info("Compute information model type: "
        + modelConcept.getPrimordialUuid());
    ConceptVersionBI concept =
        OTFUtility.getConceptVersion(modelConcept.getNid());
    if (concept == null) {
      throw new IOException("Model concept version unexpectedly null "
          + modelConcept.getPrimordialUuid());
    }
    if (!concept.isActive()) {
      throw new IOException("Model concept is inactive "
          + modelConcept.getPrimordialUuid());
    }

    InformationModelType[] values = InformationModelType.values();
    // Walk up tree until we encounter a "type" concept
    while (true) {
      // Look for match (the pref name will match an information model type)
      String prefName = OchreUtility.getFSNForConceptNid(concept.getNid(), null).get();
      for (InformationModelType type : values) {
        if (prefName.equals(type.getDisplayName())) {
          LOG.debug("  FOUND TYPE: " + type.getDisplayName());
          return type;
        }
      }

      // continue walking
      @SuppressWarnings("rawtypes")
      Collection<? extends RelationshipVersionBI> rels =
          concept.getRelationshipsOutgoingActiveIsa();
      if (rels.size() < 1) {
        break;
      }
      concept =
          OTFUtility.getConceptVersion(rels.iterator().next()
              .getDestinationNid());
      LOG.debug("  " + concept.getPrimordialUuid());
    }
    throw new IOException("Information model type not found for "
        + modelConcept.getPrimordialUuid());
  }

  /**
   * Synchronizes the current state of the information model with the underlying
   * strored concept representation. If it does not exist, it creates it. If it
   * has changed, it updates it.
   *
   * TODO: to properly implement sync, we should take the old version of the
   * model and the new version of the model and compare each field to see
   * whether its changed. either retire or add the appropriate underlying data
   * element.
   * @param modelConcept the model concept
   * @return the information model
   * @throws ContradictionException
   * @throws InvalidCAB
   * @throws IOException
   * @throws UnsupportedEncodingException
   * @throws NoSuchAlgorithmException
   * @throws PropertyVetoException
   */
  private ConceptChronicleBI syncInformationModelConcept(InformationModel model)
    throws NoSuchAlgorithmException, UnsupportedEncodingException, IOException,
    InvalidCAB, ContradictionException, PropertyVetoException {
    if (model == null) {
      throw new IOException("Model unexpectedly null");
    }
    LOG.info("Convert information model to concept");

    // Determine parent - either model type or super model uuid
    ConceptChronicleBI parent = determineParent(model);

    // Acquire concept blueprint - either load from DB or create new one
    LOG.debug("  parent = " + parent.getPrimordialUuid());
    LOG.debug("  FN = " + model.getKey());
    LOG.debug("  PT = " + model.getName());
    ConceptCB modelConceptCB = null;
    ConceptChronicleBI modelConcept = null;
    // handle case where model concept exists already
    if (exists(model)) {
      // Obtain the UUID
      UUID modelUuid =
          this.getInformationModel(model.getType(), model.getKey()).getUuid();
      LOG.debug("  Model exists already, UUID = " + modelUuid);
      // Look it up by UUID0
      modelConcept = dataStore.getConcept(modelUuid);
      ConceptVersionBI modelConceptVersion =
          modelConcept.getVersion(OTFUtility.getViewCoordinate()).get();
      modelConceptCB =
          modelConceptVersion.makeBlueprint(OTFUtility.getViewCoordinate(),
              IdDirective.PRESERVE, RefexDirective.EXCLUDE);
      // synchronize descriptions (in case name or key changed)
      syncDescriptions(modelConceptCB, model);

      // Sync super model UUID
      syncSuperModel(modelConceptCB, model, parent);

      // Sync "has terminology concept" relationships
      syncConnectedTerminologyConcepts(modelConceptCB, model);

    }
    // handle case where model concept does not exist
    else {
      LOG.debug("  Model concept does not exist yet, create it");
      modelConceptCB =
          createNewConceptBlueprint(parent, model.getKey(), model.getName());
      // Apply concept changes
      modelConcept = OTFUtility.getBuilder().construct(modelConceptCB);
      model.setUuid(modelConcept.getPrimordialUuid());
      LOG.debug("    UUID = " + modelConcept.getPrimordialUuid());
    }

    // Apply refex changes - modelConcept has been set above
    syncRefexes(modelConcept, model);

    LOG.debug("  add uncommitted");
    Ts.get().addUncommitted(modelConcept);
    LOG.debug("  commit");
    Ts.get().commit();

    return modelConcept;
  }

  /**
   * Determines the parent concept. (there will be only one)
   *
   * @param model the model
   * @return the concept chronicle bi
   * @throws Exception the exception
   */
  private ConceptChronicleBI determineParent(InformationModel model)
    throws IOException {
    // Determine parent - either model type or super model uuid
    InformationModelType type = model.getType();
    ConceptChronicleBI parent = null;
    if (model.getSuperModelUuid() != null) {
      parent = dataStore.getConcept(model.getSuperModelUuid());
    } else {
      parent = dataStore.getConcept(type.getUuid());
    }
    if (parent == null) {
      throw new IOException("Something went wrong determining parent concept "
          + type.getUuid() + ", " + model.getSuperModelUuid());
    }
    return parent;
  }

  /**
   * Sync descriptions. Retire descriptions no longer matching key or name.
   * @param modelConceptCB the model concept cb
   * @param model the model
   * @throws ContradictionException
   * @throws InvalidCAB
   * @throws IOException
   */
  private void syncDescriptions(ConceptCB modelConceptCB, InformationModel model)
    throws IOException, InvalidCAB, ContradictionException {
    for (DescriptionCAB descCAB : modelConceptCB.getDescriptionCABs()) {
      if (descCAB.getText().equals(model.getKey())) {
        continue;
      }
      if (descCAB.getText().equals(model.getName())) {
        continue;
      }
      descCAB.setRetired();
      OTFUtility.getBuilder().constructIfNotCurrent(descCAB);
    }
  }

  /**
   * Sync super model relationships
   *
   * @param modelConceptCB the model concept cb
   * @param model the model
   * @throws IOException
   * @throws ContradictionException
   * @throws InvalidCAB
   */
  private void syncSuperModel(ConceptCB modelConceptCB, InformationModel model,
    ConceptChronicleBI parent) throws IOException, InvalidCAB,
    ContradictionException {

    LOG.debug("  Handle super-model UUID");
    LOG.debug("    parent UUID = " + parent.getPrimordialUuid());

    // Retire any active Isa relationships to non-type UUIDs that are not the
    // super model UUID
    boolean found = false;
    for (RelationshipCAB relCAB : modelConceptCB.getRelationshipCABs()) {
      // LOG.debug("    rel cab type nid = " + relCAB.getTypeNid());
      // LOG.debug("    TermAux.IS_A = " + TermAux.IS_A.getLenient().getNid());
      // LOG.debug("    Snomed.IS_A = " + Snomed.IS_A.getLenient().getNid());
      // LOG.debug("    SnomedRelType.IS_A = " +
      // SnomedRelType.IS_A.getLenient().getNid());

      // Look for matching typeId flag
      if (relCAB.getTypeNid() == Snomed.IS_A.getLenient().getNid()) {

        UUID uuid =
            OTFUtility.getConceptVersion(relCAB.getTargetNid())
                .getPrimordialUuid();
        LOG.debug("    target UUID = " + uuid);

        // If the target of this rel doesn't match the computed parent, retire
        // it
        if (!parent.getPrimordialUuid().equals(uuid)) {
          relCAB.setRetired();
          OTFUtility.getBuilder().constructIfNotCurrent(relCAB);
        } else {
          found = true;
        }
      }
    }

    // if we didn't find it, create a new rel
    if (!found) {
      RelationshipCAB relCAB =
          new RelationshipCAB(modelConceptCB.getComponentUuid(),
              Snomed.IS_A.getUuids()[0], parent.getPrimordialUuid(), 0,
              RelationshipType.STATED_ROLE, IdDirective.GENERATE_HASH);
      OTFUtility.getBuilder().constructIfNotCurrent(relCAB);
    }
  }

  /**
   * Sync connected terminology concepts.
   *
   * @param modelConceptCB the model concept cb
   * @param model the model
   * @throws IOException Signals that an I/O exception has occurred.
   * @throws ContradictionException
   * @throws InvalidCAB
   */
  private void syncConnectedTerminologyConcepts(ConceptCB modelConceptCB,
    InformationModel model) throws IOException, InvalidCAB,
    ContradictionException {

    LOG.debug("  Iterate through associated UUIDs");
    Set<UUID> associatedConceptUuids = model.getAssociatedConceptUuids();
    // Retire any active relationships to UUIDs no longer in this set
    for (RelationshipCAB relCAB : modelConceptCB.getRelationshipCABs()) {
      // Look for matching typeId and "active" flag
      if (relCAB.getTypeNid() == InformationModelsConstants.HAS_TERMINOLOGY_CONCEPT.getNid()) {
        UUID uuid =
            OTFUtility.getConceptVersion(relCAB.getTargetNid())
                .getPrimordialUuid();

        // If the UUID is not in range, retire the rel
        if (!associatedConceptUuids.contains(uuid)) {
          LOG.debug("    Found relationship to retire - "+ Get.conceptDescriptionText(relCAB.getTargetNid()));
          relCAB.setRetired();
          OTFUtility.getBuilder().constructIfNotCurrent(relCAB);
        }

        // Otherwise, remove from list, no need to create it
        else {
          associatedConceptUuids.remove(uuid);
        }
      }
    }

    // Create rels for any UUIDs not accounted for, make new rels
    for (UUID destinationUuid : associatedConceptUuids) {
      LOG.debug("  Create relationship for "
          + modelConceptCB.getComponentUuid() + " => " + destinationUuid);
      UUID typeUid =
          InformationModelsConstants.HAS_TERMINOLOGY_CONCEPT.getUUID();
      RelationshipCAB relCAB =
          new RelationshipCAB(modelConceptCB.getComponentUuid(), typeUid,
              destinationUuid, 0, RelationshipType.STATED_ROLE,
              IdDirective.GENERATE_HASH);
      OTFUtility.getBuilder().constructIfNotCurrent(relCAB);
      modelConceptCB.setRelationshipCAB(relCAB);
    }
  }

  /**
   * Sync refexes. Determine which one should exist, add them if they do not,
   * retire them if they do.
   * @param modelConceptCB the model concept cb
   * @param modelCB the model cb
   * @throws ContradictionException
   * @throws IOException
   * @throws ValidationException
   * @throws InvalidCAB
   * @throws PropertyVetoException
   */
  private void syncRefexes(ConceptChronicleBI modelConcept, InformationModel model) throws ValidationException, IOException,
    ContradictionException, InvalidCAB, PropertyVetoException {
    // Create refex entries for properties
    DynamicSememeUsageDescription propertyRefset =
    		DynamicSememeUsageDescription.read(InformationModelsConstants.INFORMATION_MODEL_PROPERTIES.getSequence());
    LOG.debug("  Found " + propertyRefset.getDyanmicSememeName());
    // Iterate through information model properties and add refexes
    LOG.debug("  Iterate through properties");
    // Create a dynamic refex CAB for each entry
    // If these already exist, then the construct will have no practical effect
    Set<UUID> refexUuids = new HashSet<>();
    for (InformationModelProperty property : model.getProperties()) {
      LOG.debug("    " + property.getLabel() + ", " + property.getName());
      
      // The order of these columns is tightly bound to the definition,
      // if the definition changes, this has to be updated as well.
      if (propertyRefset.getColumnInfo().length != 8) {
        throw new IOException(
            "Information model properties refset has unexpected number of columns");
      }
      DynamicSememeDataBI[] data = new DynamicSememeDataBI[propertyRefset.getColumnInfo().length];
      data[0] = new DynamicSememeString(property.getLabel());
      data[1] = new DynamicSememeString(property.getType());
      data[2] = new DynamicSememeString(property.getName());
      data[3] = new DynamicSememeString(property.getDefaultValue());
      data[4] = new DynamicSememeString(property.getValue());
      data[5] = new DynamicSememeString(property.getCardinalityMin());
      data[6] = new DynamicSememeString(property.getCardinalityMax());
      data[7] = new DynamicSememeString(property.getVisibility());
      
      SememeChronology<?> sc = Get.sememeBuilderService().getDyanmicSememeBuilder(modelConcept.getNid(), propertyRefset.getDynamicSememeUsageDescriptorSequence(), data)
      	.build(ExtendedAppContext.getUserProfileBindings().getEditCoordinate().get(), ChangeCheckerMode.ACTIVE);
      
      Get.commitService().commit("Creating info model");

      // Construct and wire the dynamic refex
      refexUuids.add(sc.getPrimordialUuid());
      LOG.debug("    UUID = " + sc.getPrimordialUuid());
    }

    // Now iterate through all refexes and retire those that do not
    // have uuids from the code above
    
    Get.sememeService().getSememesForComponent(modelConcept.getNid()).forEach(sememeC ->
	{
		@SuppressWarnings({ "unchecked", "rawtypes" })
		Optional<LatestVersion<SememeVersion<?>>> latest = ((SememeChronology)sememeC).getLatestVersion(SememeVersion.class, 
				ExtendedAppContext.getUserProfileBindings().getStampCoordinate().get());
		if (latest.isPresent() && latest.get().value().getState() == State.ACTIVE)
		{
			if (!refexUuids.contains(sememeC.getPrimordialUuid()))
			{
				((SememeChronology)latest.get().value().getChronology()).createMutableVersion(MutableSememeVersion.class, 
						State.INACTIVE, ExtendedAppContext.getUserProfileBindings().getEditCoordinate().get());
				Get.commitService().addUncommitted(((SememeChronology)latest.get().value().getChronology()));
				LOG.debug("  Retire sememe UUID = " + sememeC.getPrimordialUuid());
			}
		}
	});
  }

  /**
   * Creates the new concept blueprint
   *
   * @param parent the parent
   * @param fsn the fsn
   * @param prefTerm the pref term
   * @return the concept cb
   * @throws ValidationException the validation exception
   * @throws IOException Signals that an I/O exception has occurred.
   * @throws InvalidCAB the invalid cab
   * @throws ContradictionException the contradiction exception
   */
  private ConceptCB createNewConceptBlueprint(ConceptChronicleBI parent,
    String fsn, String prefTerm) throws ValidationException, IOException,
    InvalidCAB, ContradictionException {
    if (parent == null) {
      throw new IOException("Parent is unexpectedly null");
    }
    if (fsn == null) {
      throw new IOException("FN is unexpectedly null");
    }
    if (prefTerm == null) {
      throw new IOException("PT is unexpectedly null");
    }
    // Assume language US English
    LanguageCode lc = LanguageCode.EN_US;
    // Assume hierarchy is represented as IS_A rel
    UUID isA = Snomed.IS_A.getUuids()[0];
    // Compute UUIDs in standard way
    IdDirective idDir = IdDirective.GENERATE_HASH;
    // Assume module should be SNOMED core module (this may be something to
    // change)
    UUID module = Snomed.CORE_MODULE.getLenient().getPrimordialUuid();
    // Create single parent
    UUID parentUUIDs[] = new UUID[1];
    parentUUIDs[0] = parent.getPrimordialUuid();
    //TODO OCHRE deal with path
    return new ConceptCB(fsn, prefTerm, lc, isA, idDir, module, IsaacMetadataAuxiliaryBinding.DEVELOPMENT.getPrimodialUuid(), parentUUIDs);
  }
}
