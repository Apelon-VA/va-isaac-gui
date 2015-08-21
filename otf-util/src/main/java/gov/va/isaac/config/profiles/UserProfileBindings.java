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

import gov.va.isaac.ExtendedAppContext;
import gov.va.isaac.config.generated.StatedInferredOptions;
import gov.va.isaac.util.Utility;
import gov.va.isaac.util.ViewCoordinateComponents;
import gov.vha.isaac.metadata.coordinates.LanguageCoordinates;
import gov.vha.isaac.metadata.coordinates.TaxonomyCoordinates;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.State;
import gov.vha.isaac.ochre.api.coordinate.EditCoordinate;
import gov.vha.isaac.ochre.api.coordinate.LanguageCoordinate;
import gov.vha.isaac.ochre.api.coordinate.StampCoordinate;
import gov.vha.isaac.ochre.api.coordinate.StampPosition;
import gov.vha.isaac.ochre.api.coordinate.StampPrecedence;
import gov.vha.isaac.ochre.api.coordinate.TaxonomyCoordinate;
import gov.vha.isaac.ochre.collections.ConceptSequenceSet;
import gov.vha.isaac.ochre.model.coordinate.EditCoordinateImpl;
import gov.vha.isaac.ochre.model.coordinate.StampCoordinateImpl;
import gov.vha.isaac.ochre.model.coordinate.StampPositionImpl;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.application.Platform;
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
	ReadOnlyObjectWrapper<UUID> editCoordinateModule = new ReadOnlyObjectWrapper<>();

	// View Coordinate Components
	ReadOnlyObjectWrapper<StatedInferredOptions> statedInferredPolicy = new ReadOnlyObjectWrapper<>();
	ReadOnlyObjectWrapper<UUID> viewCoordinatePath = new ReadOnlyObjectWrapper<>();
	ReadOnlyObjectWrapper<Long> viewCoordinateTime = new ReadOnlyObjectWrapper<>();

	private final SetProperty<Status> viewCoordinateStatuses = new SimpleSetProperty<Status>(new ObservableSetWrapper<>(new HashSet<>()));
	ReadOnlySetWrapper<Status> readOnlyViewCoordinateStatuses = new ReadOnlySetWrapper<>(viewCoordinateStatuses);

	private final SetProperty<UUID> viewCoordinateModules = new SimpleSetProperty<UUID>(new ObservableSetWrapper<>(new HashSet<>(new HashSet<>())));
	ReadOnlySetWrapper<UUID> readOnlyViewCoordinateModules = new ReadOnlySetWrapper<>(viewCoordinateModules);

	private final ReadOnlyObjectWrapper<ViewCoordinateComponents> viewCoordinateComponents = new ReadOnlyObjectWrapper<>();

	private final ReadOnlyObjectWrapper<StampCoordinate<StampCoordinateImpl>> stampCoordinate = new ReadOnlyObjectWrapper<>();
	private final ReadOnlyObjectWrapper<LanguageCoordinate> languageCoordinate = new ReadOnlyObjectWrapper<>();
	private final ReadOnlyObjectWrapper<EditCoordinate> editCoordinate = new ReadOnlyObjectWrapper<>();
	private final ReadOnlyObjectWrapper<TaxonomyCoordinate<?>> taxonomyCoordinate = new ReadOnlyObjectWrapper<>();

	public Property<?>[] getAll() {
		return new Property<?>[] {
				displayFSN,
				displayRelDirection,
				workflowUsername,
				editCoordinatePath,
				editCoordinateModule,
				
				statedInferredPolicy,
				viewCoordinatePath,
				viewCoordinateTime,
				readOnlyViewCoordinateStatuses,
				readOnlyViewCoordinateModules,
				
				viewCoordinateComponents,
				
				stampCoordinate,
				languageCoordinate,
				taxonomyCoordinate,
				editCoordinate
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
	 * @return the editCoordinateModule
	 */
	public ReadOnlyProperty<UUID> getEditCoordinateModule()
	{
		return editCoordinateModule.getReadOnlyProperty();
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
	public ReadOnlyObjectProperty<StampCoordinate<StampCoordinateImpl>> getStampCoordinate()
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
	 * @return the languageCoordinate
	 */
	public ReadOnlyObjectProperty<EditCoordinate> getEditCoordinate()
	{
		return editCoordinate.getReadOnlyProperty();
	}

	/**
	 * @return the taxonomyCoordinate
	 */
	public ReadOnlyObjectProperty<TaxonomyCoordinate<?>> getTaxonomyCoordinate()
	{
		return taxonomyCoordinate.getReadOnlyProperty();
	}
	
	protected void update(UserProfile up)
	{
		boolean updateViewCoordinateComponents = false;
		AtomicBoolean updateStampCoordinate = new AtomicBoolean(false);
		AtomicBoolean updateLanguageCoordinate = new AtomicBoolean(false);
		AtomicBoolean updateTaxonomyCoordinate = new AtomicBoolean(false);
		AtomicBoolean updateEditCoordinate = new AtomicBoolean(false);
		
		if (displayFSN.get() != up.getDisplayFSN())
		{
			updateLanguageCoordinate.set(true);
			displayFSN.set(up.getDisplayFSN());
		}
		if (displayRelDirection.get() != up.getDisplayRelDirection())
		{
			displayRelDirection.set(up.getDisplayRelDirection());
		}
		if (!Objects.equals(editCoordinatePath.get(), up.getEditCoordinatePath()))
		{
			editCoordinatePath.set(up.getEditCoordinatePath());
			updateEditCoordinate.set(true);
		}
		if (!Objects.equals(editCoordinateModule.get(), up.getEditCoordinateModule()))
		{
			editCoordinateModule.set(up.getEditCoordinateModule());
			updateEditCoordinate.set(true);
		}
		
		if (!Objects.equals(workflowUsername.get(), up.getWorkflowUsername()))
		{
			workflowUsername.set(up.getWorkflowUsername());
		}

		if (statedInferredPolicy.get() != up.getStatedInferredPolicy())
		{
			updateViewCoordinateComponents = true;
			updateTaxonomyCoordinate.set(true);
			statedInferredPolicy.set(up.getStatedInferredPolicy());
		}
		if (!Objects.equals(viewCoordinatePath.get(), up.getViewCoordinatePath()))
		{
			updateViewCoordinateComponents = true;
			updateStampCoordinate.set(true);
			viewCoordinatePath.set(up.getViewCoordinatePath());
		}
		if (!Objects.equals(viewCoordinateTime.get(), up.getViewCoordinateTime()))
		{
			updateViewCoordinateComponents = true;
			updateStampCoordinate.set(true);
			viewCoordinateTime.set(up.getViewCoordinateTime());
		}
		// This depends on the fact that UserProfile.getViewCoordinateStatuses() never returns null
		// and that viewCoordinateStatuses is initialized with an empty SimpleSetProperty<Status>
		if (readOnlyViewCoordinateStatuses.get().size() != up.getViewCoordinateStatuses().size()
				|| ! readOnlyViewCoordinateStatuses.get().containsAll(up.getViewCoordinateStatuses())
				|| ! up.getViewCoordinateStatuses().containsAll(readOnlyViewCoordinateStatuses.get())) {
			updateViewCoordinateComponents = true;
			updateStampCoordinate.set(true);
			viewCoordinateStatuses.get().clear();
			viewCoordinateStatuses.get().addAll(up.getViewCoordinateStatuses());
		}

		// This depends on the fact that UserProfile.getViewCoordinateModules() never returns null
		// and that viewCoordinateModules is initialized with an empty SimpleSetProperty<UUID>
		if (readOnlyViewCoordinateModules.get().size() != up.getViewCoordinateModules().size()
				|| ! readOnlyViewCoordinateModules.get().containsAll(up.getViewCoordinateModules())
				|| ! up.getViewCoordinateModules().containsAll(readOnlyViewCoordinateModules.get())) {
			updateViewCoordinateComponents = true;
			updateStampCoordinate.set(true);
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
		
		//From here down, things are expensive, and can't be done on the JavaFX thread...
		Utility.execute(() ->
		{
			try {
				
				if (updateEditCoordinate.get())
				{
					editCoordinate.set(new EditCoordinateImpl(
							Get.identifierService().getConceptSequenceForUuids(ExtendedAppContext.getCurrentlyLoggedInUserProfile().getConceptUUID()),
							Get.identifierService().getConceptSequenceForUuids(getEditCoordinateModule().getValue()), 
							Get.identifierService().getConceptSequenceForUuids(getEditCoordinatePath().getValue())));
				}
				
				if (updateStampCoordinate.get() || stampCoordinate.get() == null) {
					updateTaxonomyCoordinate.set(true);
					
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
					
					CountDownLatch cdl = new CountDownLatch(1);
					
					Platform.runLater(() ->
					{
						stampCoordinate.set(
							new StampCoordinateImpl(
									StampPrecedence.PATH,
									stampPosition, 
									ConceptSequenceSet.of(moduleSequences), allowedStates));
						cdl.countDown();
					});
					cdl.await();
				}			
				if (updateLanguageCoordinate.get() || languageCoordinate.get() == null) {
					updateTaxonomyCoordinate.set(true);
					
					CountDownLatch cdl = new CountDownLatch(1);
					Platform.runLater(() ->
					{
						languageCoordinate.set(
							displayFSN.get() ? LanguageCoordinates.getUsEnglishLanguageFullySpecifiedNameCoordinate() : LanguageCoordinates.getUsEnglishLanguagePreferredTermCoordinate());
						cdl.countDown();
					});
					cdl.await();
				}
				
				if (updateTaxonomyCoordinate.get()) {
					try {

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
						
						final TaxonomyCoordinate<?> foo = newCoordinate;

						CountDownLatch cdl = new CountDownLatch(1);
						Platform.runLater(() ->
						{
							taxonomyCoordinate.set(foo);
							cdl.countDown();
						});
						cdl.await();
					} catch (Exception e) {
						LOG.error("Failed updating taxonomyCoordinate in UserProfileBindings. Caught " + e.getClass().getName() + " " + e.getLocalizedMessage(), e);
						throw e;
					}
				}
				
			} catch (Exception e) {
				LOG.error("Unexpected error updating user profile bindings", e);
			}
		});
		
	}
}
