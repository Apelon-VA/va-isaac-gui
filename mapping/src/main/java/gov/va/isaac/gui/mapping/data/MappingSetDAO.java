package gov.va.isaac.gui.mapping.data;

import java.beans.PropertyVetoException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.ihtsdo.otf.query.lucene.indexers.DynamicSememeIndexerConfiguration;
import org.ihtsdo.otf.tcc.api.blueprint.DescriptionCAB;
import org.ihtsdo.otf.tcc.api.blueprint.IdDirective;
import org.ihtsdo.otf.tcc.api.blueprint.InvalidCAB;
import org.ihtsdo.otf.tcc.api.blueprint.RefexDirective;
import org.ihtsdo.otf.tcc.api.concept.ConceptVersionBI;
import org.ihtsdo.otf.tcc.api.contradiction.ContradictionException;
import org.ihtsdo.otf.tcc.api.coordinate.Status;
import org.ihtsdo.otf.tcc.api.description.DescriptionVersionBI;
import org.ihtsdo.otf.tcc.api.lang.LanguageCode;
import org.ihtsdo.otf.tcc.api.metadata.ComponentType;
import org.ihtsdo.otf.tcc.api.metadata.binding.Snomed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import gov.va.isaac.AppContext;
import gov.va.isaac.ExtendedAppContext;
import gov.va.isaac.util.OTFUtility;
import gov.va.isaac.util.Utility;
import gov.vha.isaac.metadata.coordinates.ViewCoordinates;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeColumnInfo;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeDataBI;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeDataType;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeValidatorType;
import gov.vha.isaac.ochre.api.constants.ISAAC;
import gov.vha.isaac.ochre.api.constants.MappingConstants;
import gov.vha.isaac.ochre.api.index.SearchResult;
import gov.vha.isaac.ochre.model.sememe.dataTypes.DynamicSememeString;
import gov.vha.isaac.ochre.model.sememe.dataTypes.DynamicSememeUUID;

/**
 * {@link MappingSet}
 *
 * A Convenience class to hide unnecessary OTF bits from the Mapping APIs.
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a> 
 */
public class MappingSetDAO extends MappingDAO
{
	private static final Logger LOG = LoggerFactory.getLogger(MappingSetDAO.class);
	
	/**
	 * Create and store a new mapping set in the DB.
	 * @param mappingName - The name of the mapping set (used for the FSN and preferred term of the underlying concept)
	 * @param inverseName - (optional) inverse name of the mapping set (if it makes sense for the mapping)
	 * @param purpose - (optional) - user specified purpose of the mapping set
	 * @param description - the intended use of the mapping set
	 * @param editorStatus - (optional) user specified status concept of the mapping set
	 * @return
	 * @throws IOException
	 */
	public static MappingSet createMappingSet(String mappingName, String inverseName, String purpose, String description, UUID editorStatus) 
			throws IOException
	{
		try
		{
			AppContext.getRuntimeGlobals().disableAllCommitListeners();

			//We need to create a new concept - which itself is defining a dynamic refex - so set that up here.
			DynamicSememeUsageDescription rdud = DynamicSememeUsageDescriptionBuilder
					.createNewDynamicSememeUsageDescriptionConcept(mappingName, mappingName, description, 
					new DynamicSememeColumnInfo[] {
						new DynamicSememeColumnInfo(0, ISAAC.REFEX_COLUMN_TARGET_COMPONENT.getPrimodialUuid(), DynamicSememeDataType.UUID, null, false, null, null),
						new DynamicSememeColumnInfo(1, MappingConstants.MAPPING_QUALIFIERS.getPrimodialUuid(), DynamicSememeDataType.UUID, null, false, 
								DynamicSememeValidatorType.IS_KIND_OF, new DynamicSememeUUID(MappingConstants.MAPPING_QUALIFIERS.getPrimodialUuid())),
						new DynamicSememeColumnInfo(2, MappingConstants.MAPPING_STATUS.getPrimodialUuid(), DynamicSememeDataType.UUID, null, false, 
								DynamicSememeValidatorType.IS_KIND_OF, new DynamicSememeUUID(MappingConstants.MAPPING_STATUS.getPrimodialUuid()))}, 
					null, true, ComponentType.CONCEPT, ViewCoordinates.getMetadataViewCoordinate());
			
			Utility.execute(() ->
			{
				try
				{
					AppContext.getRuntimeGlobals().disableAllCommitListeners();
					DynamicSememeIndexerConfiguration.configureColumnsToIndex(rdud.getRefexUsageDescriptorNid(), new Integer[] {0, 1, 2}, true);
				}
				catch (Exception e)
				{
					LOG.error("Unexpected error enabling the index on newly created mapping set!", e);
				}
				finally
				{
					AppContext.getRuntimeGlobals().enableAllCommitListeners();
				}
			});
			
			//Then, annotate the concept created above as a member of the MappingSet dynamic refex, and add the inverse name, if present.
			ConceptVersionBI createdConcept = OTFUtility.getConceptVersion(rdud.getRefexUsageDescriptorNid());
			if (!StringUtils.isBlank(inverseName))
			{
				DescriptionCAB dCab = new DescriptionCAB(createdConcept.getNid(), Snomed.SYNONYM_DESCRIPTION_TYPE.getNid(), LanguageCode.EN, inverseName,
						false, IdDirective.GENERATE_HASH);
				dCab.addAnnotationBlueprint(new DynamicSememeCAB(dCab.getComponentUuid(), ISAAC.ASSOCIATION_INVERSE_NAME.getPrimodialUuid()));
				OTFUtility.getBuilder().construct(dCab);
			}
			
			DynamicSememeCAB mappingAnnotation = new DynamicSememeCAB(rdud.getRefexUsageDescriptorNid(), MappingConstants.MAPPING_SEMEME_TYPE.getNid());
			mappingAnnotation.setData(new DynamicSememeDataBI[] {
					(editorStatus == null ? null : new DynamicSememeUUID(editorStatus)),
					(StringUtils.isBlank(purpose) ? null : new DynamicSememeString(purpose))}, OTFUtility.getViewCoordinateAllowInactive());
			OTFUtility.getBuilder().construct(mappingAnnotation);
			
			DynamicSememeCAB associationAnnotation = new DynamicSememeCAB(rdud.getRefexUsageDescriptorNid(), ISAAC.ASSOCIATION_SEMEME.getNid());
			associationAnnotation.setData(new DynamicSememeDataBI[] {}, null);
			OTFUtility.getBuilder().construct(associationAnnotation);
			
			ExtendedAppContext.getDataStore().addUncommitted(createdConcept);
			ExtendedAppContext.getDataStore().commit(/* createdConcept */);
			
			//Find the constructed dynamic refset
			return new MappingSet((DynamicSememeVersionBI<?>)ExtendedAppContext.getDataStore().getComponent(mappingAnnotation.getMemberUUID())
					.getVersion(OTFUtility.getViewCoordinateAllowInactive()).get());
		
		}
		catch (ContradictionException | InvalidCAB | PropertyVetoException e)
		{
			LOG.error("unexpected", e);
			throw new IOException("Unexpected error creating mapping", e);
		}
		finally
		{
			AppContext.getRuntimeGlobals().enableAllCommitListeners();
		}
	}
	
	/**
	 * Store the changes (done via set methods) on the passed in mapping set.  
	 * @param mappingSet - The mappingSet that carries the changes
	 * @throws IOException
	 */
	public static void updateMappingSet(MappingSet mappingSet) throws IOException {
		try
		{
			ConceptVersionBI mappingConcept = ExtendedAppContext.getDataStore().getConceptVersion(OTFUtility.getViewCoordinateAllowInactive(), mappingSet.getPrimordialUUID());
			
			for (DescriptionVersionBI<?> desc : mappingConcept.getDescriptionsActive())
			{
				if (desc.getTypeNid() == Snomed.SYNONYM_DESCRIPTION_TYPE.getNid())
				{
					if (OTFUtility.isPreferred(desc.getAnnotations()))
					{
						//Set the name
						DescriptionCAB dCab = desc.makeBlueprint(OTFUtility.getViewCoordinateAllowInactive(), IdDirective.PRESERVE, RefexDirective.EXCLUDE);
						dCab.setText(mappingSet.getName());
						OTFUtility.getBuilder().construct(dCab);
					}
					else
					//see if it is the inverse name
					{
						for (DynamicSememeChronicleBI<?> annotation : desc.getDynamicSememeAnnotations())
						{
							if (annotation.getAssemblageNid() == ISAAC.ASSOCIATION_INVERSE_NAME.getNid())
							{
								//set the inverse name
								DescriptionCAB dCab = desc.makeBlueprint(OTFUtility.getViewCoordinateAllowInactive(), IdDirective.PRESERVE, RefexDirective.EXCLUDE);
								dCab.setText(mappingSet.getInverseName());
								OTFUtility.getBuilder().construct(dCab);
								break;
							}
						}
					}
				}
				else if (desc.getTypeNid() == Snomed.DEFINITION_DESCRIPTION_TYPE.getNid())
				{
					if (OTFUtility.isPreferred(desc.getAnnotations()))
					{
						//set the description
						DescriptionCAB dCab = desc.makeBlueprint(OTFUtility.getViewCoordinateAllowInactive(), IdDirective.PRESERVE, RefexDirective.EXCLUDE);
						dCab.setText(mappingSet.getDescription());
						OTFUtility.getBuilder().construct(dCab);
					}
				}
			}
			
			Optional<? extends DynamicSememeVersionBI<?>> mappingRefex = null;
			for (DynamicSememeChronicleBI<?> refex : mappingConcept.getDynamicSememeAnnotations())
			{
				if (refex.getAssemblageNid() == MappingConstants.MAPPING_SEMEME_TYPE.getNid())
				{
					mappingRefex = refex.getVersion(OTFUtility.getViewCoordinateAllowInactive());
					break;
				}
			}
			
			if (!mappingRefex.isPresent())
			{
				LOG.error("Couldn't find mapping refex?");
				throw new IOException("internal error");
			}
			
			DynamicSememeCAB mappingRefexCab = mappingRefex.get().makeBlueprint(OTFUtility.getViewCoordinateAllowInactive(), IdDirective.PRESERVE, RefexDirective.EXCLUDE);
			mappingRefexCab.setData(new DynamicSememeDataBI[] {
					(mappingSet.getEditorStatusConcept() == null ? null : new DynamicSememeUUID(mappingSet.getEditorStatusConcept())),
					(StringUtils.isBlank(mappingSet.getPurpose()) ? null : new DynamicSememeString(mappingSet.getPurpose()))}, OTFUtility.getViewCoordinateAllowInactive());
			OTFUtility.getBuilder().construct(mappingRefexCab);

			AppContext.getRuntimeGlobals().disableAllCommitListeners();
			ExtendedAppContext.getDataStore().addUncommitted(mappingConcept);
			ExtendedAppContext.getDataStore().commit(/* mappingConcept */);
		}
		catch (InvalidCAB | ContradictionException | PropertyVetoException e)
		{
			LOG.error("Unexpected!", e);
			throw new IOException("Internal error");
		}
		finally
		{
			AppContext.getRuntimeGlobals().enableAllCommitListeners();
		}
	}
	
	public static List<MappingSet> getMappingSets(boolean activeOnly) throws IOException
	{
		try
		{
			ArrayList<MappingSet> result = new ArrayList<>();
			for (SearchResult sr : search(MappingConstants.MAPPING_SEMEME_TYPE.getPrimodialUuid()))
			{
				Optional<DynamicSememeVersionBI<?>> rc = (Optional<DynamicSememeVersionBI<?>>) ExtendedAppContext.getDataStore().
						getComponentVersion(OTFUtility.getViewCoordinateAllowInactive(), sr.getNid());
				if (rc.isPresent())
				{
					MappingSet mappingSet = new MappingSet(rc.get());
					if (mappingSet.isActive() || !activeOnly)
					{
						result.add(mappingSet);
					}
				}
			}
			
			return result;
		}
		catch (ContradictionException e)
		{
			LOG.error("Unexpected error reading mappings", e);
			throw new IOException("Error reading mappings", e);
		}
	}
	
	public static void retireMappingSet(UUID mappingSetPrimordialUUID) throws IOException
	{
		setConceptStatus(mappingSetPrimordialUUID, Status.INACTIVE);
	}
	
	public static void unRetireMappingSet(UUID mappingSetPrimordialUUID) throws IOException
	{
		setConceptStatus(mappingSetPrimordialUUID, Status.ACTIVE);
	}
	
	public static ConceptVersionBI getMappingConcept(DynamicSememeVersionBI<?> refex) throws IOException {
		return ExtendedAppContext.getDataStore().getConceptVersion(OTFUtility.getViewCoordinateAllowInactive(), refex.getReferencedComponentNid());
	}
}
