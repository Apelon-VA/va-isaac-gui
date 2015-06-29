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

	private final SetProperty<UUID> viewCoordinateModules = new SimpleSetProperty<UUID>(new ObservableSetWrapper<>(new HashSet<>()));
	ReadOnlySetWrapper<UUID> readOnlyViewCoordinateModules = new ReadOnlySetWrapper<>(viewCoordinateModules);

	private final ReadOnlyObjectWrapper<ViewCoordinateComponents> viewCoordinateComponents = new ReadOnlyObjectWrapper<>();

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
				
				viewCoordinateComponents
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
	
	protected void update(UserProfile up)
	{
		boolean updateViewCoordinateComponents = false;
		
		if (displayFSN.get() != up.getDisplayFSN())
		{
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
			statedInferredPolicy.set(up.getStatedInferredPolicy());
		}
		if (!Objects.equals(viewCoordinatePath.get(), up.getViewCoordinatePath()))
		{
			updateViewCoordinateComponents = true;
			viewCoordinatePath.set(up.getViewCoordinatePath());
		}
		if (!Objects.equals(viewCoordinateTime.get(), up.getViewCoordinateTime()))
		{
			updateViewCoordinateComponents = true;
			viewCoordinateTime.set(up.getViewCoordinateTime());
		}
		// This depends on the fact that UserProfile.getViewCoordinateStatuses() never returns null
		// and that viewCoordinateStatuses is initialized with an empty SimpleSetProperty<Status>
		if (readOnlyViewCoordinateStatuses.get().size() != up.getViewCoordinateStatuses().size()
				|| ! readOnlyViewCoordinateStatuses.get().containsAll(up.getViewCoordinateStatuses())
				|| ! up.getViewCoordinateStatuses().containsAll(readOnlyViewCoordinateStatuses.get())) {
			updateViewCoordinateComponents = true;
			viewCoordinateStatuses.get().clear();
			viewCoordinateStatuses.get().addAll(up.getViewCoordinateStatuses());
		}

		// This depends on the fact that UserProfile.getViewCoordinateModules() never returns null
		// and that viewCoordinateModules is initialized with an empty SimpleSetProperty<UUID>
		if (readOnlyViewCoordinateModules.get().size() != up.getViewCoordinateModules().size()
				|| ! readOnlyViewCoordinateModules.get().containsAll(up.getViewCoordinateModules())
				|| ! up.getViewCoordinateModules().containsAll(readOnlyViewCoordinateModules.get())) {
			updateViewCoordinateComponents = true;
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
	}
}