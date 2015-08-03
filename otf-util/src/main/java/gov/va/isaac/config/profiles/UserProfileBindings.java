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
package gov.va.isaac.config.profiles;

import gov.va.isaac.config.generated.StatedInferredOptions;
import gov.va.isaac.util.ViewCoordinateComponents;
import gov.vha.isaac.metadata.coordinates.LanguageCoordinates;
import gov.vha.isaac.metadata.coordinates.TaxonomyCoordinates;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.LookupService;
import gov.vha.isaac.ochre.api.State;
import gov.vha.isaac.ochre.api.component.concept.ConceptService;
import gov.vha.isaac.ochre.api.component.concept.ConceptSnapshotService;
import gov.vha.isaac.ochre.api.coordinate.LanguageCoordinate;
import gov.vha.isaac.ochre.api.coordinate.StampCoordinate;
import gov.vha.isaac.ochre.api.coordinate.StampPosition;
import gov.vha.isaac.ochre.api.coordinate.StampPrecedence;
import gov.vha.isaac.ochre.api.coordinate.TaxonomyCoordinate;
import gov.vha.isaac.ochre.api.tree.Tree;
import gov.vha.isaac.ochre.collections.ConceptSequenceSet;
import gov.vha.isaac.ochre.model.coordinate.StampCoordinateImpl;
import gov.vha.isaac.ochre.model.coordinate.StampPositionImpl;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Objects;
import java.util.UUID;

import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.property.ReadOnlySetProperty;
import javafx.beans.property.ReadOnlySetWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SetProperty;
import javafx.beans.property.SimpleSetProperty;

import javax.inject.Singleton;

import org.ihtsdo.otf.tcc.api.coordinate.Status;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.javafx.collections.ObservableSetWrapper;

/**
 * {@link UserProfileBindings}
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
@Service
@Singleton
public class UserProfileBindings
{
	private static final Logger LOG = LoggerFactory.getLogger(UserProfileBindings.class);
	public enum RelationshipDirection {SOURCE, TARGET, SOURCE_AND_TARGET};
	
	ReadOnlyBooleanWrapper displayFSN = new ReadOnlyBooleanWrapper();
	ReadOnlyObjectWrapper<RelationshipDirection> displayRelDirection = new ReadOnlyObjectWrapper<>();
	ReadOnlyStringWrapper workflowUsername = new ReadOnlyStringWrapper();
	ReadOnlyObjectWrapper<UUID> editCoordinatePath = new ReadOnlyObjectWrapper<>();

	// View Coordinate Components
	ReadOnlyObjectWrapper<StatedInferredOptions> statedInferredPolicy = new ReadOnlyObjectWrapper<>();
	ReadOnlyObjectWrapper<UUID> viewCoordinatePath = new ReadOnlyObjectWrapper<>();
	ReadOnlyObjectWrapper<Long> viewCoordinateTime = new ReadOnlyObjectWrapper<>();

	private final SetProperty<Status> viewCoordinateStatuses = new SimpleSetProperty<Status>(new ObservableSetWrapper<>(new HashSet<>()));
	ReadOnlySetWrapper<Status> readOnlyViewCoordinateStatuses = new ReadOnlySetWrapper<>(viewCoordinateStatuses);

	private final SetProperty<UUID> viewCoordinateModules = new SimpleSetProperty<UUID>(new ObservableSetWrapper<>(new HashSet<>(new HashSet<>())));
	ReadOnlySetWrapper<UUID> readOnlyViewCoordinateModules = new ReadOnlySetWrapper<>(viewCoordinateModules);

	private final ReadOnlyObjectWrapper<ViewCoordinateComponents> viewCoordinateComponents = new ReadOnlyObjectWrapper<>();

	private final ReadOnlyObjectWrapper<StampCoordinate<?>> stampCoordinate = new ReadOnlyObjectWrapper<>();
	private final ReadOnlyObjectWrapper<LanguageCoordinate> languageCoordinate = new ReadOnlyObjectWrapper<>();
	private final ReadOnlyObjectWrapper<TaxonomyCoordinate<?>> taxonomyCoordinate = new ReadOnlyObjectWrapper<>();

	private final ReadOnlyObjectWrapper<Tree> taxonomyTree = new ReadOnlyObjectWrapper<>();
	
	private final ReadOnlyObjectWrapper<ConceptSnapshotService> conceptSnapshotService = new ReadOnlyObjectWrapper<>();

	public Property<?>[] getAll() {
		return new Property<?>[] {
				displayFSN,
				displayRelDirection,
				workflowUsername,
				editCoordinatePath,
				
				statedInferredPolicy,
				viewCoordinatePath,
				viewCoordinateTime,
				readOnlyViewCoordinateStatuses,
				readOnlyViewCoordinateModules,
				
				viewCoordinateComponents,
				
				stampCoordinate,
				languageCoordinate,
				taxonomyCoordinate,
				taxonomyTree,
				conceptSnapshotService
		};
	}

	/**
	 * @return displayFSN when true, display the preferred term when false
	 */
	public ReadOnlyBooleanProperty getDisplayFSN()
	{
		return displayFSN.getReadOnlyProperty();
	}
	
	/**
	 * @return which direction of relationships should be displayed
	 */
	public ReadOnlyObjectProperty<RelationshipDirection> getDisplayRelDirection()
	{
		return displayRelDirection.getReadOnlyProperty();
	}
	/**
	 * @return the workflowUsername
	 */
	public ReadOnlyStringProperty getWorkflowUsername()
	{
		return workflowUsername.getReadOnlyProperty();
	}

	/**
	 * @return the editCoordinatePath
	 */
	public ReadOnlyProperty<UUID> getEditCoordinatePath()
	{
		return editCoordinatePath.getReadOnlyProperty();
	}

	/**
	 * @return the statedInferredPolicy
	 */
	public ReadOnlyObjectProperty<StatedInferredOptions> getStatedInferredPolicy()
	{
		return statedInferredPolicy.getReadOnlyProperty();
	}
	/**
	 * @return the viewCoordinatePath
	 */
	public ReadOnlyProperty<UUID> getViewCoordinatePath()
	{
		return viewCoordinatePath.getReadOnlyProperty();
	}
	/**
	 * @return the viewCoordinateTime
	 */
	public ReadOnlyProperty<Long> getViewCoordinateTime()
	{
		return viewCoordinateTime.getReadOnlyProperty();
	}
	/**
	 * @return the viewCoordinateStatuses
	 */
	public ReadOnlySetProperty<Status> getViewCoordinateStatuses()
	{
		return readOnlyViewCoordinateStatuses;
	}

	/**
	 * @return the viewCoordinateModules
	 */
	public ReadOnlySetProperty<UUID> getViewCoordinateModules()
	{
		return readOnlyViewCoordinateModules;
	}

	/**
	 * @return the viewCoordinateModules
	 */
	public ReadOnlyObjectProperty<ViewCoordinateComponents> getViewCoordinateComponents()
	{
		return viewCoordinateComponents.getReadOnlyProperty();
	}

	/**
	 * @return the stampCoordinate
	 */
	public ReadOnlyObjectProperty<StampCoordinate<?>> getStampCoordinate()
	{
		return stampCoordinate.getReadOnlyProperty();
	}

	/**
	 * @return the languageCoordinate
	 */
	public ReadOnlyObjectProperty<LanguageCoordinate> getLanguageCoordinate()
	{
		return languageCoordinate.getReadOnlyProperty();
	}

	/**
	 * @return the taxonomyCoordinate
	 */
	public ReadOnlyObjectProperty<TaxonomyCoordinate<?>> getTaxonomyCoordinate()
	{
		return taxonomyCoordinate.getReadOnlyProperty();
	}
	
	/**
	 * @return the taxonomyTree
	 */
	public ReadOnlyObjectProperty<Tree> getTaxonomyTree()
	{
		return taxonomyTree.getReadOnlyProperty();
	}
	
	/**
	 * @return the conceptSnapshotService
	 */
	public ReadOnlyObjectProperty<ConceptSnapshotService> getConceptSnapshotService()
	{
		return conceptSnapshotService.getReadOnlyProperty();
	}
	
	protected void update(UserProfile up)
	{
		boolean updateViewCoordinateComponents = false;
		boolean updateStampCoordinate = false;
		boolean updateLanguageCoordinate = false;
		boolean updateTaxonomyCoordinate = false;
		boolean updateTaxonomyTree = false;
		boolean updateConceptSnapshotService = false;
		
		if (displayFSN.get() != up.getDisplayFSN())
		{
			updateLanguageCoordinate = true;
			displayFSN.set(up.getDisplayFSN());
		}
		if (displayRelDirection.get() != up.getDisplayRelDirection())
		{
			displayRelDirection.set(up.getDisplayRelDirection());
		}
		if (!Objects.equals(editCoordinatePath.get(), up.getEditCoordinatePath()))
		{
			editCoordinatePath.set(up.getEditCoordinatePath());
		}
		if (!Objects.equals(workflowUsername.get(), up.getWorkflowUsername()))
		{
			workflowUsername.set(up.getWorkflowUsername());
		}

		if (statedInferredPolicy.get() != up.getStatedInferredPolicy())
		{
			updateViewCoordinateComponents = true;
			updateTaxonomyCoordinate = true;
			statedInferredPolicy.set(up.getStatedInferredPolicy());
		}
		if (!Objects.equals(viewCoordinatePath.get(), up.getViewCoordinatePath()))
		{
			updateViewCoordinateComponents = true;
			updateStampCoordinate = true;
			viewCoordinatePath.set(up.getViewCoordinatePath());
		}
		if (!Objects.equals(viewCoordinateTime.get(), up.getViewCoordinateTime()))
		{
			updateViewCoordinateComponents = true;
			updateStampCoordinate = true;
			viewCoordinateTime.set(up.getViewCoordinateTime());
		}
		// This depends on the fact that UserProfile.getViewCoordinateStatuses() never returns null
		// and that viewCoordinateStatuses is initialized with an empty SimpleSetProperty<Status>
		if (readOnlyViewCoordinateStatuses.get().size() != up.getViewCoordinateStatuses().size()
				|| ! readOnlyViewCoordinateStatuses.get().containsAll(up.getViewCoordinateStatuses())
				|| ! up.getViewCoordinateStatuses().containsAll(readOnlyViewCoordinateStatuses.get())) {
			updateViewCoordinateComponents = true;
			updateStampCoordinate = true;
			viewCoordinateStatuses.get().clear();
			viewCoordinateStatuses.get().addAll(up.getViewCoordinateStatuses());
		}

		// This depends on the fact that UserProfile.getViewCoordinateModules() never returns null
		// and that viewCoordinateModules is initialized with an empty SimpleSetProperty<UUID>
		if (readOnlyViewCoordinateModules.get().size() != up.getViewCoordinateModules().size()
				|| ! readOnlyViewCoordinateModules.get().containsAll(up.getViewCoordinateModules())
				|| ! up.getViewCoordinateModules().containsAll(readOnlyViewCoordinateModules.get())) {
			updateViewCoordinateComponents = true;
			updateStampCoordinate = true;
			viewCoordinateModules.get().clear();
			viewCoordinateModules.get().addAll(up.getViewCoordinateModules());
		}
		
		if (updateViewCoordinateComponents) {
			ViewCoordinateComponents vcc = new ViewCoordinateComponents(
					statedInferredPolicy.get(),
					viewCoordinatePath.get(),
					readOnlyViewCoordinateStatuses,
					viewCoordinateTime.get(),
					readOnlyViewCoordinateModules);
			viewCoordinateComponents.set(vcc);
		}
		
		if (updateStampCoordinate || stampCoordinate.get() == null) {
			updateTaxonomyCoordinate = true;
			updateConceptSnapshotService = true;
			
			StampPosition stampPosition = new StampPositionImpl(
					viewCoordinateTime.get(),
					Get.identifierService().getConceptSequenceForUuids(viewCoordinatePath.get()));
			int[] moduleSequences = new int[viewCoordinateModules.get().size()];
			int index = 0;
			for (UUID moduleUuid : viewCoordinateModules.get()) {
				if (moduleUuid != null) {
					moduleSequences[index++] = Get.identifierService().getConceptSequenceForUuids(moduleUuid);
				}
			}

			EnumSet<State> allowedStates = EnumSet.allOf(State.class);
			allowedStates.clear();
			for (Status status : viewCoordinateStatuses.get()) {
				allowedStates.add(status.getState());
			}
			stampCoordinate.set(
					new StampCoordinateImpl(
							StampPrecedence.PATH,
							stampPosition, 
							ConceptSequenceSet.of(moduleSequences), allowedStates));
		}
		
		if (updateLanguageCoordinate || languageCoordinate.get() == null) {
			updateTaxonomyCoordinate = true;
			updateConceptSnapshotService = true;
	
			languageCoordinate.set(
					displayFSN.get() ? LanguageCoordinates.getUsEnglishLanguageFullySpecifiedNameCoordinate() : LanguageCoordinates.getUsEnglishLanguagePreferredTermCoordinate());
		}
		
		if (updateTaxonomyCoordinate) {
			try {
				updateTaxonomyTree = true;

				TaxonomyCoordinate<?> newCoordinate = null;
				switch (statedInferredPolicy.get()) {
				case STATED:
					newCoordinate = TaxonomyCoordinates.getStatedTaxonomyCoordinate(stampCoordinate.get(), languageCoordinate.get());
					break;
				case INFERRED:
					newCoordinate = TaxonomyCoordinates.getInferredTaxonomyCoordinate(stampCoordinate.get(), languageCoordinate.get());
					break;
				default:
					throw new RuntimeException("Unsupported StatedInferredOptions value " + statedInferredPolicy.get() + ".  Expected STATED or INFERRED.");
				}

				taxonomyCoordinate.set(newCoordinate);
			} catch (Exception e) {
				LOG.error("Failed updating taxonomyCoordinate in UserProfileBindings. Caught " + e.getClass().getName() + " " + e.getLocalizedMessage(), e);
				throw e;
			}
		}
		
		if (updateTaxonomyTree) {
			try {
				taxonomyTree.set(Get.taxonomyService().getTaxonomyTree(taxonomyCoordinate.get()));
			} catch (Exception e) {
				LOG.error("Failed updating taxonomyTree in UserProfileBindings. Caught " + e.getClass().getName() + " " + e.getLocalizedMessage(), e);
				throw e;
			}
		}

		if (updateConceptSnapshotService) {
			try {
				conceptSnapshotService.set(
						LookupService.getService(ConceptService.class).getSnapshot(
								getStampCoordinate().get(),
								getLanguageCoordinate().get()));
			} catch (Exception e) {
				LOG.error("Failed updating conceptSnapshotService in UserProfileBindings. Caught " + e.getClass().getName() + " " + e.getLocalizedMessage(), e);
				throw e;
			}
		}
	}
}
