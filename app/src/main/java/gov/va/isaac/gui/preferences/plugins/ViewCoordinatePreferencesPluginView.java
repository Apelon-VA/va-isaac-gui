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
	
	// ReflectUtil needs this to be public
	public ViewCoordinatePreferencesPluginView()
	{
		// created by HK2
		LOG.debug(this.getClass().getSimpleName() + " construct time (blocking GUI): {}", 0);
	}

	/**
	 * @see gov.va.isaac.interfaces.gui.views.commonFunctionality.PreferencesPluginViewI
	 */
	@Override
	public Region getContent()
	{
		if (drlvc_ == null)
		{
			try
			{
				drlvc_ = ViewCoordinatePreferencesPluginViewController.construct();
			}
			catch (IOException e)
			{
				LoggerFactory.getLogger(this.getClass()).error("Unexpected error initing ViewCoordinatePreferencesPluginViewController", e);
				AppContext.getCommonDialogs().showErrorDialog("Unexpected error creating ViewCoordinatePreferencesPluginViewController", e);
				return new Label("Unexpected error initializing view, see log file");
			}

		}
		return drlvc_.getContent();
	}

	@Override
	public String getName() {
		return "View Coordinate";
	}

	@Override
	public ReadOnlyStringProperty validationFailureMessageProperty() {
		if (drlvc_ == null) {
			getContent();
		}
		return drlvc_.validationFailureMessageProperty();
	}

	@Override
	public void save() throws IOException {
		if (drlvc_ == null) {
			getContent();
		}
		drlvc_.save();
	}

	@Override
	public int getTabOrder() {
		return 10;
	}
}
