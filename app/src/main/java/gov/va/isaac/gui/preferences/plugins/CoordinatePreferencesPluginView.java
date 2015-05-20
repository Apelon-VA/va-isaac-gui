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

/**
 * ViewCoordinatePreferencesPlugin
 * 
 * @author <a href="mailto:joel.kniaz@gmail.com">Joel Kniaz</a>
 */
package gov.va.isaac.gui.preferences.plugins;

import gov.va.isaac.config.generated.StatedInferredOptions;
import gov.va.isaac.interfaces.gui.views.commonFunctionality.PreferencesPluginViewI;
import gov.va.isaac.util.ValidBooleanBinding;
import java.util.Collection;
import java.util.UUID;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;

/**
 * ViewCoordinatePreferencesPlugin
 * 
 * @author <a href="mailto:joel.kniaz@gmail.com">Joel Kniaz</a>
 *
 */
public abstract class CoordinatePreferencesPluginView implements PreferencesPluginViewI {

	protected HBox hBox = null;
	protected ValidBooleanBinding allValid_ = null;
	
	protected ToggleGroup statedInferredToggleGroup = null;
	protected ComboBox<UUID> pathComboBox = null;
	
	protected final ObjectProperty<StatedInferredOptions> currentStatedInferredOptionProperty = new SimpleObjectProperty<>();
	protected final ObjectProperty<UUID> currentPathProperty = new SimpleObjectProperty<>();
	protected final ObjectProperty<Long> currentTimeProperty = new SimpleObjectProperty<>();

	/* (non-Javadoc)
	 * @see gov.va.isaac.interfaces.gui.views.commonFunctionality.PreferencesPluginViewI#getValidationFailureMessage()
	 */
	@Override
	public ReadOnlyStringProperty validationFailureMessageProperty() {
		return allValid_.getReasonWhyInvalid();
	}
	
	protected abstract Collection<UUID> getPathOptions();
	
	protected abstract UUID getStoredPath();
	protected abstract UUID getDefaultPath();
	
	protected abstract Long getStoredTime();
	protected abstract Long getDefaultTime();

	
	protected abstract StatedInferredOptions getStoredStatedInferredOption();
	protected abstract StatedInferredOptions getDefaultStatedInferredOption();
	
	public abstract Region getContent();
	
	public ReadOnlyObjectProperty<Long> currentViewCoordinateTimeProperty() {
		return currentTimeProperty;
	}
	
	public ReadOnlyObjectProperty<StatedInferredOptions> currentStatedInferredOptionProperty() {
		return currentStatedInferredOptionProperty;
	}
	
	public ReadOnlyObjectProperty<UUID> currentPathProperty() {
		return currentPathProperty;
	}

	

}
