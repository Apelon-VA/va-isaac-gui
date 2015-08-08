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
package gov.va.isaac.gui.preferences.plugins;

import gov.va.isaac.AppContext;
import gov.va.isaac.interfaces.gui.views.commonFunctionality.PreferencesPluginViewI;

import java.io.IOException;

import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;

import javax.inject.Singleton;

import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link ViewCoordinatePreferencesPluginView}
 *
 * @author <a href="mailto:joel.kniaz@gmail.com">Joel Kniaz</a>
 */
@Service
@Singleton
public class ViewCoordinatePreferencesPluginView implements PreferencesPluginViewI
{
	ViewCoordinatePreferencesPluginViewController drlvc_;
	
	private final Logger LOG = LoggerFactory.getLogger(this.getClass());
	
	/**
	 * This slave property, bound to controller validationFailureMessageProperty only after controller construction,
	 * allows a client to bind to this view before the controller is initialized
	 */
	private final StringProperty slaveValidationFailureMessageProperty = new SimpleStringProperty();
	
	/**
	 * Constructor should only be called by HK2
	 */
	private ViewCoordinatePreferencesPluginView() {
		LOG.debug(this.getClass().getSimpleName() + " construct time (blocking GUI): {}", 0);
	}

	/**
	 * @see gov.va.isaac.interfaces.gui.views.commonFunctionality.PreferencesPluginViewI
	 * 
	 * Uses FXMLLoader to construct the underlying controller
	 * and binds the slaveValidationFailureMessageProperty to the controller's validationFailureMessageProperty
	 */
	@Override
	public Region getContent()
	{
		if (drlvc_ == null)
		{
			try
			{
				drlvc_ = ViewCoordinatePreferencesPluginViewController.construct();
				drlvc_.setPersistenceInterface(new ViewCoordinatePreferencesUserProfilePersistenceInterface());
				slaveValidationFailureMessageProperty.bind(drlvc_.validationFailureMessageProperty());
			}
			catch (IOException e)
			{
				LoggerFactory.getLogger(this.getClass()).error("Unexpected error initing ViewCoordinatePreferencesPluginViewController", e);
				AppContext.getCommonDialogs().showErrorDialog("Unexpected error creating ViewCoordinatePreferencesPluginViewController", e);
				return new Label("Unexpected error initializing view, see LOG file");
			}

		}
		return drlvc_.getContent();
	}

	/* (non-Javadoc)
	 * @see gov.va.isaac.interfaces.gui.views.commonFunctionality.PreferencesPluginViewI#getName()
	 */
	@Override
	public String getName() {
		return "View Coordinate";
	}

	/* (non-Javadoc)
	 * @see gov.va.isaac.interfaces.gui.views.commonFunctionality.PreferencesPluginViewI#validationFailureMessageProperty()
	 * 
	 * This slave property, bound to controller validationFailureMessageProperty only after controller construction,
	 * allows a client to bind to this view before the controller is initialized
	 */
	@Override
	public ReadOnlyStringProperty validationFailureMessageProperty() {
		return slaveValidationFailureMessageProperty;
	}

	/* (non-Javadoc)
	 * @see gov.va.isaac.interfaces.gui.views.commonFunctionality.PreferencesPluginViewI#save()
	 * 
	 * If controller not yet initialized, then initialize it and save current values
	 * TODO maybe should be noop, since obviously no values changed.  Could be utility in saving defaults to uninitialized preferences, however
	 */
	@Override
	public void save() throws IOException {
		if (drlvc_ == null) {
			getContent();
		}
		drlvc_.save();
	}

	/* (non-Javadoc)
	 * @see gov.va.isaac.interfaces.gui.views.commonFunctionality.PreferencesPluginViewI#getTabOrder()
	 */
	@Override
	public int getTabOrder() {
		return 10;
	}
}
