package gov.va.isaac.associations;

import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ihtsdo.otf.query.lucene.indexers.DynamicSememeIndexerConfiguration;
import gov.va.isaac.AppContext;
import gov.va.isaac.ExtendedAppContext;
import gov.va.isaac.util.Utility;
import gov.vha.isaac.metadata.source.IsaacMetadataAuxiliaryBinding;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.LookupService;
import gov.vha.isaac.ochre.api.State;
import gov.vha.isaac.ochre.api.chronicle.ObjectChronology;
import gov.vha.isaac.ochre.api.chronicle.ObjectChronologyType;
import gov.vha.isaac.ochre.api.commit.ChangeCheckerMode;
import gov.vha.isaac.ochre.api.component.concept.ConceptChronology;
import gov.vha.isaac.ochre.api.component.concept.ConceptSnapshot;
import gov.vha.isaac.ochre.api.component.concept.ConceptSnapshotService;
import gov.vha.isaac.ochre.api.component.concept.ConceptVersion;
import gov.vha.isaac.ochre.api.component.concept.description.DescriptionBuilderService;
import gov.vha.isaac.ochre.api.component.sememe.SememeChronology;
import gov.vha.isaac.ochre.api.component.sememe.SememeType;
import gov.vha.isaac.ochre.api.component.sememe.version.DescriptionSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.DynamicSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeColumnInfo;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeDataType;
import gov.vha.isaac.ochre.impl.sememe.DynamicSememeUsageDescription;
import gov.vha.isaac.ochre.impl.sememe.DynamicSememeUtility;
import gov.vha.isaac.ochre.impl.utility.Frills;
import gov.vha.isaac.ochre.model.constants.IsaacMetadataConstants;

public class AssociationType
{
	private int associationSequence_;
	private String associationName_;
	private Optional<String> associationInverseName_;
	private String description_;
	
	private static final Logger log = LogManager.getLogger();
	
	private AssociationType(int conceptNidOrSequence)
	{
		this.associationSequence_ = Get.identifierService().getConceptSequence(conceptNidOrSequence);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static AssociationType read(int conceptNidOrSequence)
	{
		AssociationType at = new AssociationType(conceptNidOrSequence);
		String bestName = null;
		for (DescriptionSememe<?> desc : Frills.getDescriptionsOfType(Get.identifierService().getConceptNid(at.getAssociationTypeSequenece()),
				IsaacMetadataAuxiliaryBinding.SYNONYM, ExtendedAppContext.getUserProfileBindings().getStampCoordinate().get().makeAnalog(State.ACTIVE)))
		{
			if (bestName == null)
			{
				bestName = desc.getText();
			}
			if (Frills.isDescriptionPreferred(desc.getNid(), ExtendedAppContext.getUserProfileBindings().getStampCoordinate().get()))
			{
				bestName = desc.getText();
				break;
			}
			
			if (Get.sememeService().getSememesForComponentFromAssemblage(desc.getNid(), 
					IsaacMetadataConstants.DYNAMIC_SEMEME_ASSOCIATION_INVERSE_NAME.getSequence()).anyMatch(nestedSememe ->
			{
				if (nestedSememe.getSememeType() == SememeType.DYNAMIC)
				{
					return ((SememeChronology)nestedSememe).getLatestVersion(DynamicSememe.class, 
							ExtendedAppContext.getUserProfileBindings().getStampCoordinate().get()).isPresent();
				}
				return false;
			}))
			{
				at.associationInverseName_ = Optional.of(desc.getText());
			}
		}
		
		if (bestName == null)
		{
			at.associationName_ = "-No name on path!-";
		}
		at.associationName_= bestName;
		if (at.associationInverseName_ == null)
		{
			at.associationInverseName_ = Optional.empty();
		}
		
		for (DescriptionSememe<?> desc : Frills.getDescriptionsOfType(Get.identifierService().getConceptNid(at.getAssociationTypeSequenece()),
				IsaacMetadataAuxiliaryBinding.DEFINITION_DESCRIPTION_TYPE, ExtendedAppContext.getUserProfileBindings().getStampCoordinate().get().makeAnalog(State.ACTIVE)))
		{
			if (Frills.isDescriptionPreferred(desc.getNid(), ExtendedAppContext.getUserProfileBindings().getStampCoordinate().get()) && 
					Get.sememeService().getSememesForComponentFromAssemblage(desc.getNid(), 
							IsaacMetadataConstants.DYNAMIC_SEMEME_DEFINITION_DESCRIPTION.getSequence()).anyMatch(nestedSememe ->
			{
				if (nestedSememe.getSememeType() == SememeType.DYNAMIC)
				{
					return ((SememeChronology)nestedSememe).getLatestVersion(DynamicSememe.class, 
							ExtendedAppContext.getUserProfileBindings().getStampCoordinate().get()).isPresent();
				}
				return false;
			}))
			{
				at.description_ = desc.getText();
			}
		}
		
		if (bestName == null)
		{
			at.associationName_ = "-No name on path!-";
		}
		at.associationName_= bestName;
		if (at.associationInverseName_ == null)
		{
			at.associationInverseName_ = Optional.empty();
		}
		if (at.description_ == null)
		{
			at.description_ = "-No description on path!-";
		}
		
		return at;
	}
	
	/**
	 * @return the association type concept
	 */
	public ConceptChronology<? extends ConceptVersion<?>> getAssociationTypeConcept() 
	{
		return Get.conceptService().getConcept(associationSequence_);
	}
	
	/**
	 * @return the concept sequence of the association type concept
	 */
	public int getAssociationTypeSequenece() 
	{
		return associationSequence_;
	}
	

	public String getAssociationName()
	{
		return associationName_;
	}
	
	/**
	 * @return the inverse name of the association (if present) (Read from the association type concept)
	 */
	public Optional<String> getAssociationInverseName()
	{
		return associationInverseName_;
	}
	
	
	/**
	 * Create and store a new mapping set in the DB.
	 * @param associationName - The name of the association (used for the FSN and preferred term of the underlying concept)
	 * @param associationInverseName - (optional) inverse name of the association (if it makes sense for the association)
	 * @param description - (optional) description that describes the purpose of the association
	 * @param referencedComponentRestriction - (optional) - may be null - if provided - this restricts the type of object referenced by the nid or 
	 * UUID that is set for the referenced component in an instance of this sememe.  If {@link ObjectChronologyType#UNKNOWN_NID} is passed, it is ignored, as 
	 * if it were null.
	 * @param referencedComponentSubRestriction - (optional) - may be null - subtype restriction for {@link ObjectChronologyType#SEMEME} restrictions
	 * @return the concept sequence of the created concept that carries the association definition
	 */
	public static AssociationType createAssociation(String associationName, String associationInverseName, String description, ObjectChronologyType referencedComponentRestriction,
			SememeType referencedComponentSubRestriction) 
	{
		try
		{
			AppContext.getRuntimeGlobals().disableAllCommitListeners();

			//We need to create a new concept - which itself is defining a dynamic sememe - so set that up here.
			DynamicSememeUsageDescription rdud = DynamicSememeUtility.createNewDynamicSememeUsageDescriptionConcept(
					associationName, associationName, StringUtils.isBlank(description) ? "Defines the association type " + associationInverseName : description, 
					new DynamicSememeColumnInfo[] {
						new DynamicSememeColumnInfo(0, IsaacMetadataConstants.DYNAMIC_SEMEME_COLUMN_ASSOCIATION_TARGET_COMPONENT.getUUID(), 
								DynamicSememeDataType.UUID, null, false)}, 
					IsaacMetadataConstants.DYNAMIC_SEMEME_ASSOCIATION_SEMEME.getNid(), referencedComponentRestriction, referencedComponentSubRestriction);
			
			Utility.execute(() ->
			{
				try
				{
					AppContext.getRuntimeGlobals().disableAllCommitListeners();
					DynamicSememeIndexerConfiguration.configureColumnsToIndex(rdud.getDynamicSememeUsageDescriptorSequence(), new Integer[] {0}, true);
				}
				catch (Exception e)
				{
					log.error("Unexpected error enabling the index on newly created association!", e);
				}
				finally
				{
					AppContext.getRuntimeGlobals().enableAllCommitListeners();
				}
			});
			
			ConceptSnapshotService css = Get.conceptService().getSnapshot(
					ExtendedAppContext.getUserProfileBindings().getStampCoordinate().get(), 
					ExtendedAppContext.getUserProfileBindings().getLanguageCoordinate().get());
			
			//Then add the inverse name, if present.
			if (!StringUtils.isBlank(associationInverseName))
			{
				ConceptSnapshot cs = css.getConceptSnapshot(rdud.getDynamicSememeUsageDescriptorSequence());
				ObjectChronology<?> builtDesc = LookupService.get().getService(DescriptionBuilderService.class).getDescriptionBuilder(associationInverseName, cs.getConceptSequence(), 
						IsaacMetadataAuxiliaryBinding.SYNONYM, IsaacMetadataAuxiliaryBinding.ENGLISH).build(
								ExtendedAppContext.getUserProfileBindings().getEditCoordinate().get(), ChangeCheckerMode.ACTIVE);
				
				Get.commitService().commit(Get.sememeBuilderService().getDyanmicSememeBuilder(
							builtDesc.getNid(), IsaacMetadataConstants.DYNAMIC_SEMEME_ASSOCIATION_INVERSE_NAME.getSequence())
								.build(ExtendedAppContext.getUserProfileBindings().getEditCoordinate().get(), ChangeCheckerMode.ACTIVE), 
						ExtendedAppContext.getUserProfileBindings().getEditCoordinate().get(), "add description to association").get();  
				//final get is to wait for commit completion
			}
			
			//Add the association marker sememe
			Get.commitService().commit(Get.sememeBuilderService().getDyanmicSememeBuilder(
						Get.identifierService().getConceptNid(rdud.getDynamicSememeUsageDescriptorSequence()),
						IsaacMetadataConstants.DYNAMIC_SEMEME_ASSOCIATION_SEMEME.getSequence())
							.build(ExtendedAppContext.getUserProfileBindings().getEditCoordinate().get(), ChangeCheckerMode.ACTIVE), 
					ExtendedAppContext.getUserProfileBindings().getEditCoordinate().get(), "mark assocation as association type sememe").get();
			//final get is to wait for commit completion

			return read(rdud.getDynamicSememeUsageDescriptorSequence());
		}
		catch (Exception e)
		{
			log.error("Unexpected error creating association", e);
			throw new RuntimeException(e);
		}
		finally
		{
			AppContext.getRuntimeGlobals().enableAllCommitListeners();
		}
	}
}
