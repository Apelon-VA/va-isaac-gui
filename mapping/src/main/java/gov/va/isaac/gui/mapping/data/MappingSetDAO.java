package gov.va.isaac.gui.mapping.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.ihtsdo.otf.query.lucene.indexers.DynamicSememeIndexerConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import gov.va.isaac.AppContext;
import gov.va.isaac.ExtendedAppContext;
import gov.va.isaac.util.Utility;
import gov.vha.isaac.metadata.source.IsaacMetadataAuxiliaryBinding;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.LookupService;
import gov.vha.isaac.ochre.api.State;
import gov.vha.isaac.ochre.api.chronicle.LatestVersion;
import gov.vha.isaac.ochre.api.chronicle.ObjectChronology;
import gov.vha.isaac.ochre.api.chronicle.ObjectChronologyType;
import gov.vha.isaac.ochre.api.commit.ChangeCheckerMode;
import gov.vha.isaac.ochre.api.commit.CommitRecord;
import gov.vha.isaac.ochre.api.component.concept.ConceptSnapshot;
import gov.vha.isaac.ochre.api.component.concept.ConceptSnapshotService;
import gov.vha.isaac.ochre.api.component.concept.description.DescriptionBuilderService;
import gov.vha.isaac.ochre.api.component.sememe.SememeChronology;
import gov.vha.isaac.ochre.api.component.sememe.version.DescriptionSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.DynamicSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.MutableDescriptionSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.SememeVersion;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeColumnInfo;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeDataBI;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeDataType;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeValidatorType;
import gov.vha.isaac.ochre.impl.sememe.DynamicSememeUsageDescription;
import gov.vha.isaac.ochre.impl.sememe.DynamicSememeUtility;
import gov.vha.isaac.ochre.impl.utility.Frills;
import gov.vha.isaac.ochre.model.constants.IsaacMappingConstants;
import gov.vha.isaac.ochre.model.constants.IsaacMetadataConstants;
import gov.vha.isaac.ochre.model.sememe.dataTypes.DynamicSememeString;
import gov.vha.isaac.ochre.model.sememe.dataTypes.DynamicSememeUUID;
import gov.vha.isaac.ochre.model.sememe.version.DynamicSememeImpl;
import javafx.concurrent.Task;

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
			DynamicSememeUsageDescription rdud = DynamicSememeUtility.createNewDynamicSememeUsageDescriptionConcept(
					mappingName, mappingName, description, 
					new DynamicSememeColumnInfo[] {
						new DynamicSememeColumnInfo(0, IsaacMetadataConstants.DYNAMIC_SEMEME_COLUMN_ASSOCIATION_TARGET_COMPONENT.getUUID(), 
								DynamicSememeDataType.UUID, null, false),
						new DynamicSememeColumnInfo(1, IsaacMappingConstants.MAPPING_QUALIFIERS.getUUID(), DynamicSememeDataType.UUID, null, false, 
								DynamicSememeValidatorType.IS_KIND_OF, new DynamicSememeUUID(IsaacMappingConstants.MAPPING_QUALIFIERS.getUUID())),
						new DynamicSememeColumnInfo(2, IsaacMappingConstants.MAPPING_STATUS.getUUID(), DynamicSememeDataType.UUID, null, false, 
								DynamicSememeValidatorType.IS_KIND_OF, new DynamicSememeUUID(IsaacMappingConstants.MAPPING_STATUS.getUUID()))}, 
					null, ObjectChronologyType.CONCEPT, null);
			
			Utility.execute(() ->
			{
				try
				{
					AppContext.getRuntimeGlobals().disableAllCommitListeners();
					DynamicSememeIndexerConfiguration.configureColumnsToIndex(rdud.getDynamicSememeUsageDescriptorSequence(), new Integer[] {0, 1, 2}, true);
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
			
			ConceptSnapshotService css = Get.conceptService().getSnapshot(
					ExtendedAppContext.getUserProfileBindings().getStampCoordinate().get(), 
					ExtendedAppContext.getUserProfileBindings().getLanguageCoordinate().get());
			
			//Then, annotate the concept created above as a member of the MappingSet dynamic sememe, and add the inverse name, if present.
			ConceptSnapshot cs = css.getConceptSnapshot(rdud.getDynamicSememeUsageDescriptorSequence());
			if (!StringUtils.isBlank(inverseName))
			{
				ObjectChronology<?> builtDesc = LookupService.get().getService(DescriptionBuilderService.class).getDescriptionBuilder(inverseName, cs.getConceptSequence(), 
						IsaacMetadataAuxiliaryBinding.SYNONYM, IsaacMetadataAuxiliaryBinding.ENGLISH).build(
								ExtendedAppContext.getUserProfileBindings().getEditCoordinate().get(), ChangeCheckerMode.ACTIVE);
				
				Get.sememeBuilderService().getDyanmicSememeBuilder(builtDesc.getNid(),IsaacMetadataConstants.DYNAMIC_SEMEME_DEFINITION_DESCRIPTION.getSequence()).build(
						ExtendedAppContext.getUserProfileBindings().getEditCoordinate().get(), ChangeCheckerMode.ACTIVE);
			}
			
			@SuppressWarnings("rawtypes")
			SememeChronology mappingAnnotation = Get.sememeBuilderService().getDyanmicSememeBuilder(Get.identifierService().getConceptNid(rdud.getDynamicSememeUsageDescriptorSequence()),
					IsaacMappingConstants.DYNAMIC_SEMEME_MAPPING_SEMEME_TYPE.getSequence(), 
					new DynamicSememeDataBI[] {
							(editorStatus == null ? null : new DynamicSememeUUID(editorStatus)),
							(StringUtils.isBlank(purpose) ? null : new DynamicSememeString(purpose))}).build(
					ExtendedAppContext.getUserProfileBindings().getEditCoordinate().get(), ChangeCheckerMode.ACTIVE);

			
			Get.sememeBuilderService().getDyanmicSememeBuilder(Get.identifierService().getConceptNid(rdud.getDynamicSememeUsageDescriptorSequence()),
					IsaacMetadataConstants.DYNAMIC_SEMEME_ASSOCIATION_SEMEME.getSequence()).build(
					ExtendedAppContext.getUserProfileBindings().getEditCoordinate().get(), ChangeCheckerMode.ACTIVE);
			
			Task<Optional<CommitRecord>> task = Get.commitService().commit("update mapping item");
			
			try
			{
				task.get();
			}
			catch (Exception e)
			{
				throw new RuntimeException();
			}
			
			
			@SuppressWarnings("unchecked")
			Optional<LatestVersion<DynamicSememe<?>>> sememe = mappingAnnotation.getLatestVersion(DynamicSememe.class, 
					ExtendedAppContext.getUserProfileBindings().getStampCoordinate().get());
			
			//Find the constructed dynamic refset
			return new MappingSet(sememe.get().value());
		
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
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void updateMappingSet(MappingSet mappingSet) throws RuntimeException 
	{
		try
		{
			ConceptSnapshot mappingConcept = Get.conceptService().getSnapshot(
					ExtendedAppContext.getUserProfileBindings().getStampCoordinate().get().makeAnalog(State.ACTIVE, State.INACTIVE), 
					ExtendedAppContext.getUserProfileBindings().getLanguageCoordinate().get())
						.getConceptSnapshot(Get.identifierService().getNidForUuids(mappingSet.getPrimordialUUID()));
			
			Get.sememeService().getSememesForComponentFromAssemblage(mappingConcept.getNid(), 
					IsaacMetadataAuxiliaryBinding.DESCRIPTION_ASSEMBLAGE.getConceptSequence())
				.forEach(descriptionC ->
				{
					Optional<LatestVersion<DescriptionSememe<?>>> latest = ((SememeChronology)descriptionC).getLatestVersion(DescriptionSememe.class, 
							ExtendedAppContext.getUserProfileBindings().getStampCoordinate().get());
					if (latest.isPresent())
					{
						DescriptionSememe<?> ds = latest.get().value();
						if (ds.getDescriptionTypeConceptSequence() == IsaacMetadataAuxiliaryBinding.SYNONYM.getConceptSequence())
						{
							if (Frills.isDescriptionPreferred(ds.getNid(), null))
							{
								if (!ds.getText().equals(mappingSet.getName()))
								{
									MutableDescriptionSememe mutable = ((SememeChronology<DescriptionSememe>)ds)
											.createMutableVersion(MutableDescriptionSememe.class, ds.getStampSequence());
									mutable.setText(mappingSet.getName());
									Get.commitService().addUncommitted((SememeChronology<DescriptionSememe>)ds);
								}
							}
							else
							//see if it is the inverse name
							{
								if (Get.sememeService().getSememesForComponentFromAssemblage(ds.getNid(), 
										IsaacMetadataConstants.DYNAMIC_SEMEME_ASSOCIATION_INVERSE_NAME.getSequence()).anyMatch(sememeC -> 
										{
											return sememeC.isLatestVersionActive(ExtendedAppContext.getUserProfileBindings().getStampCoordinate().get());
										}))
								{
									if (!ds.getText().equals(mappingSet.getInverseName()))
									{
										MutableDescriptionSememe mutable = ((SememeChronology<DescriptionSememe>)ds)
												.createMutableVersion(MutableDescriptionSememe.class, ds.getStampSequence());
										mutable.setText(mappingSet.getInverseName());
										Get.commitService().addUncommitted((SememeChronology<DescriptionSememe>)ds);
									}
								}
							}
						}
						else if (ds.getDescriptionTypeConceptSequence() == IsaacMetadataAuxiliaryBinding.DEFINITION_DESCRIPTION_TYPE.getConceptSequence())
						{
							if (Frills.isDescriptionPreferred(ds.getNid(), null))
							{
								if (!mappingSet.getDescription().equals(ds.getText()))
								{
									MutableDescriptionSememe mutable = ((SememeChronology<DescriptionSememe>)ds)
											.createMutableVersion(MutableDescriptionSememe.class, ds.getStampSequence());
									mutable.setText(mappingSet.getDescription());
									Get.commitService().addUncommitted((SememeChronology<DescriptionSememe>)ds);
								}
							}
						}
					}
				});
			

			Optional<SememeChronology<? extends SememeVersion<?>>> mappingSememe =  Get.sememeService().getSememesForComponentFromAssemblage(mappingConcept.getNid(), 
					IsaacMappingConstants.DYNAMIC_SEMEME_MAPPING_SEMEME_TYPE.getSequence()).findAny();
						
			if (!mappingSememe.isPresent())
			{
				LOG.error("Couldn't find mapping refex?");
				throw new RuntimeException("internal error");
			}
			Optional<DynamicSememe<?>> latest = ((SememeChronology)mappingSememe.get()).getLatestVersion(DynamicSememe.class, 
					ExtendedAppContext.getUserProfileBindings().getStampCoordinate().get().makeAnalog(State.ACTIVE, State.INACTIVE));
			
			if (latest.get().getData()[0] == null && mappingSet.getPurpose() != null || mappingSet.getPurpose() == null && latest.get().getData()[0] != null
					|| (latest.get().getData()[0] != null && ((DynamicSememeUUID)latest.get().getData()[0]).getDataUUID().equals(mappingSet.getEditorStatusConcept())) 
					|| latest.get().getData()[1] == null && mappingSet.getPurpose() != null || mappingSet.getPurpose() == null && latest.get().getData()[1] != null
					|| (latest.get().getData()[1] != null && ((DynamicSememeString)latest.get().getData()[1]).getDataString().equals(mappingSet.getPurpose())))
			{
				DynamicSememeImpl mutable = (DynamicSememeImpl) ((SememeChronology)mappingSememe.get()).createMutableVersion(DynamicSememe.class, 
						latest.get().getStampSequence());
	
				mutable.setData(new DynamicSememeDataBI[] {
						(mappingSet.getEditorStatusConcept() == null ? null : new DynamicSememeUUID(mappingSet.getEditorStatusConcept())),
						(StringUtils.isBlank(mappingSet.getPurpose()) ? null : new DynamicSememeString(mappingSet.getPurpose()))});
				Get.commitService().addUncommitted(latest.get().getChronology());
			}
			
			AppContext.getRuntimeGlobals().disableAllCommitListeners();
			Get.commitService().commit("Update mapping");
		}
		finally
		{
			AppContext.getRuntimeGlobals().enableAllCommitListeners();
		}
	}
	
	public static List<MappingSet> getMappingSets(boolean activeOnly) throws IOException
	{
		ArrayList<MappingSet> result = new ArrayList<>();
		
		Get.sememeService().getSememesFromAssemblage(IsaacMappingConstants.DYNAMIC_SEMEME_MAPPING_SEMEME_TYPE.getSequence()).forEach(sememeC -> 
			{
				@SuppressWarnings({ "unchecked", "rawtypes" })
				Optional<LatestVersion<DynamicSememe<?>>> latest = ((SememeChronology)sememeC).getLatestVersion(DynamicSememe.class, 
						ExtendedAppContext.getUserProfileBindings().getStampCoordinate().get().makeAnalog(State.ACTIVE, State.INACTIVE));
				
				if (latest.isPresent() && (!activeOnly || latest.get().value().getState() == State.ACTIVE))
				{
					result.add(new MappingSet(latest.get().value()));
				}
			});

		return result;
	}
	
	public static void retireMappingSet(UUID mappingSetPrimordialUUID) throws IOException
	{
		setConceptStatus(mappingSetPrimordialUUID, State.INACTIVE);
	}
	
	public static void unRetireMappingSet(UUID mappingSetPrimordialUUID) throws IOException
	{
		setConceptStatus(mappingSetPrimordialUUID, State.ACTIVE);
	}
	
	public static ConceptSnapshot getMappingConcept(DynamicSememe<?> sememe) throws RuntimeException {
		return Get.conceptService().getSnapshot(
				ExtendedAppContext.getUserProfileBindings().getStampCoordinate().get().makeAnalog(State.ACTIVE, State.INACTIVE), 
				ExtendedAppContext.getUserProfileBindings().getLanguageCoordinate().get())
					.getConceptSnapshot(sememe.getReferencedComponentNid());
	}
}
