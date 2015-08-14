package gov.va.isaac.gui.mapping.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import gov.va.isaac.AppContext;
import gov.va.isaac.ExtendedAppContext;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.State;
import gov.vha.isaac.ochre.api.chronicle.LatestVersion;
import gov.vha.isaac.ochre.api.commit.ChangeCheckerMode;
import gov.vha.isaac.ochre.api.commit.CommitRecord;
import gov.vha.isaac.ochre.api.component.sememe.SememeChronology;
import gov.vha.isaac.ochre.api.component.sememe.version.DynamicSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeDataBI;
import gov.vha.isaac.ochre.model.constants.IsaacMetadataConstants;
import gov.vha.isaac.ochre.model.sememe.dataTypes.DynamicSememeString;
import javafx.concurrent.Task;

public class MappingItemCommentDAO extends MappingDAO 
{
	/**
	 * Create (and store to the DB) a new comment
	 * @param pMappingItemUUID - The item the comment is being added to
	 * @param pCommentText - The text of the comment
	 * @param commentContext - (optional) field for storing other arbitrary info about the comment.  An editor may wish to put certain keywords on 
	 * some comments - this field is indexed, so a search for comments could query this field.
	 * @throws IOException
	 */
	public static MappingItemComment createMappingItemComment(UUID pMappingItemUUID, String pCommentText, String commentContext) throws RuntimeException
	{
		if (pMappingItemUUID == null)
		{
			throw new RuntimeException("UUID of component to attach the comment to is required");
		}
		if (StringUtils.isBlank(pCommentText))
		{
			throw new RuntimeException("The comment is required");
		}

		try
		{
			
			SememeChronology<? extends DynamicSememe<?>> built =  Get.sememeBuilderService().getDyanmicSememeBuilder(
					Get.identifierService().getNidForUuids(pMappingItemUUID),  
					IsaacMetadataConstants.DYNAMIC_SEMEME_COMMENT_ATTRIBUTE.getSequence(), 
					new DynamicSememeDataBI[] {new DynamicSememeString(pCommentText),
							(StringUtils.isBlank(commentContext) ? null : new DynamicSememeString(commentContext))})
				.build(ExtendedAppContext.getUserProfileBindings().getEditCoordinate().get(),
						ChangeCheckerMode.ACTIVE);

			AppContext.getRuntimeGlobals().disableAllCommitListeners();

			Task<Optional<CommitRecord>> task = Get.commitService().commit("Added comment");
			
			try
			{
				task.get();
			}
			catch (Exception e)
			{
				throw new RuntimeException();
			}
			
			@SuppressWarnings({ "unchecked", "rawtypes" })
			Optional<LatestVersion<DynamicSememe<?>>> latest = ((SememeChronology)built).getLatestVersion(DynamicSememe.class, 
					ExtendedAppContext.getUserProfileBindings().getStampCoordinate().get().makeAnalog(State.ACTIVE, State.INACTIVE));
			
			return new MappingItemComment(latest.get().value());
		}
		finally
		{
			AppContext.getRuntimeGlobals().enableAllCommitListeners();
		}
	}

	/**
	 * Read all comments for a particular mapping item (which could be a mapping set, or a mapping item)
	 * @param mappingUUID - The UUID of a MappingSet or a MappingItem
	 * @param activeOnly - when true, only return active comments
	 * @return
	 * @throws RuntimeException
	 */
	public static List<MappingItemComment> getComments(UUID mappingUUID, boolean activeOnly) throws RuntimeException {
		List<MappingItemComment> comments = new ArrayList<MappingItemComment>();
		
		Get.sememeService().getSememesForComponentFromAssemblage(Get.identifierService().getNidForUuids(mappingUUID),
				IsaacMetadataConstants.DYNAMIC_SEMEME_COMMENT_ATTRIBUTE.getSequence()).forEach(sememeC -> 
				{
					@SuppressWarnings({ "unchecked", "rawtypes" })
					Optional<LatestVersion<DynamicSememe<?>>> latest = ((SememeChronology)sememeC).getLatestVersion(DynamicSememe.class, 
							ExtendedAppContext.getUserProfileBindings().getStampCoordinate().get().makeAnalog(State.ACTIVE, State.INACTIVE));
					
					if (!latest.isPresent() || (activeOnly && latest.get().value().getState() == State.INACTIVE))
					{
						//noop;
					}
					else
					{
						comments.add(new MappingItemComment(latest.get().value()));
					}
				});

		return comments;
	}
	
	/**
	 * @param commentPrimordialUUID - The ID of the comment to be re-activated
	 * @throws IOException
	 */
	public static void unRetireComment(UUID commentPrimordialUUID) throws IOException 
	{
		setSememeStatus(commentPrimordialUUID, State.ACTIVE);
	}
	
	/**
	 * @param commentPrimordialUUID - The ID of the comment to be retired
	 * @throws IOException
	 */
	public static void retireComment(UUID commentPrimordialUUID) throws IOException 
	{
		setSememeStatus(commentPrimordialUUID, State.INACTIVE);
	}
	
	/**
	 * Store the values passed in as a new revision of a comment (the old revision remains in the DB)
	 * @param comment - The MappingItemComment with revisions (contains fields where the setters have been called)
	 * @throws IOException
	 */
	public static void updateComment(MappingItemComment comment) throws IOException 
	{
		try
		{
			DynamicSememe<?> rdv = readCurrentRefex(comment.getPrimordialUUID());
			Get.sememeBuilderService().getDyanmicSememeBuilder(rdv.getReferencedComponentNid(),  
					IsaacMetadataConstants.DYNAMIC_SEMEME_COMMENT_ATTRIBUTE.getSequence(), 
					new DynamicSememeDataBI[] {new DynamicSememeString(comment.getCommentText()),
							(StringUtils.isBlank(comment.getCommentContext()) ? null : new DynamicSememeString(comment.getCommentContext()))})
				.build(ExtendedAppContext.getUserProfileBindings().getEditCoordinate().get(),
						ChangeCheckerMode.ACTIVE);

			AppContext.getRuntimeGlobals().disableAllCommitListeners();

			Task<Optional<CommitRecord>> task = Get.commitService().commit("Added comment");
			
			try
			{
				task.get();
			}
			catch (Exception e)
			{
				throw new RuntimeException();
			}
		}
		finally
		{
			AppContext.getRuntimeGlobals().enableAllCommitListeners();
		}
	}
}
