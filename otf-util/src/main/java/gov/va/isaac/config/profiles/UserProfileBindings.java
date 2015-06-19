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
import java.util.Objects;
import java.util.UUID;
import org.ihtsdo.otf.tcc.api.coordinate.Status;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlySetProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlySetWrapper;
import javafx.beans.property.SimpleSetProperty;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javax.inject.Singleton;
import org.jvnet.hk2.annotations.Service;

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
	
	ReadOnlyObjectWrapper<StatedInferredOptions> statedInferredPolicy = new ReadOnlyObjectWrapper<>();
	ReadOnlyBooleanWrapper displayFSN = new ReadOnlyBooleanWrapper();
	ReadOnlyObjectWrapper<RelationshipDirection> displayRelDirection = new ReadOnlyObjectWrapper<>();
	ReadOnlyStringWrapper workflowUsername = new ReadOnlyStringWrapper();
	ReadOnlyObjectWrapper<UUID> viewCoordinatePath = new ReadOnlyObjectWrapper<>();
	ReadOnlyObjectWrapper<UUID> editCoordinatePath = new ReadOnlyObjectWrapper<>();
	ReadOnlySetWrapper<Status> viewCoordinateStatuses = new ReadOnlySetWrapper<>(new SimpleSetProperty<Status>());
	
	public Property<?>[] getAll()
	{
		return new Property<?>[] {statedInferredPolicy, displayFSN, displayRelDirection, workflowUsername, viewCoordinatePath, editCoordinatePath, viewCoordinateStatuses};
	}
	
	/**
	 * @return the statedInferredPolicy
	 */
	public ReadOnlyObjectProperty<StatedInferredOptions> getStatedInferredPolicy()
	{
		return statedInferredPolicy.getReadOnlyProperty();
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
	 * @return the viewCoordinatePath
	 */
	public ReadOnlyProperty<UUID> getViewCoordinatePath()
	{
		return viewCoordinatePath.getReadOnlyProperty();
	}
	/**
	 * @return the editCoordinatePath
	 */
	public ReadOnlyProperty<UUID> getEditCoordinatePath()
	{
		return editCoordinatePath.getReadOnlyProperty();
	}

	/**
	 * @return the viewCoordinateStatuses
	 */
	public ReadOnlySetProperty<Status> getViewCoordinateStatuses()
	{
		return viewCoordinateStatuses.getReadOnlyProperty();
	}
	
	protected void update(UserProfile up)
	{
		if (displayFSN.get() != up.getDisplayFSN())
		{
			displayFSN.set(up.getDisplayFSN());
		}
		if (displayRelDirection.get() != up.getDisplayRelDirection())
		{
			displayRelDirection.set(up.getDisplayRelDirection());
		}
		if ((editCoordinatePath.get() == null && up.getEditCoordinatePath() != null) || !Objects.equals(editCoordinatePath.get(), up.getEditCoordinatePath()))
		{
			editCoordinatePath.set(up.getEditCoordinatePath());
		}
		if (statedInferredPolicy.get() != up.getStatedInferredPolicy())
		{
			statedInferredPolicy.set(up.getStatedInferredPolicy());
		}
		if ((viewCoordinatePath.get() == null && up.getViewCoordinatePath() != null) || !Objects.equals(viewCoordinatePath.get(), up.getViewCoordinatePath()))
		{
			viewCoordinatePath.set(up.getViewCoordinatePath());
		}
		if ((workflowUsername.get() == null && up.getWorkflowUsername() != null) || !Objects.equals(workflowUsername.get(), up.getWorkflowUsername()))
		{
			workflowUsername.set(up.getWorkflowUsername());
		}
		
		// This depends on the fact that UserProfile.getViewCoordinateStatuses() never returns null
		// and that viewCoordinateStatuses is initialized with an empty SimpleSetProperty<Status>
		if (viewCoordinateStatuses.get().size() != up.getViewCoordinateStatuses().size()
				|| ! viewCoordinateStatuses.get().containsAll(up.getViewCoordinateStatuses())
				|| ! up.getViewCoordinateStatuses().containsAll(viewCoordinateStatuses.get())) {
			viewCoordinateStatuses.get().clear();
			viewCoordinateStatuses.get().addAll(up.getViewCoordinateStatuses());
		}
	}
}