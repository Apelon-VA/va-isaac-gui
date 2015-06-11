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
package gov.va.isaac.logic.view;

import gov.va.isaac.AppContext;
import gov.va.isaac.gui.dialog.CommonDialogs;
import gov.va.isaac.gui.util.Images;
import gov.va.isaac.interfaces.gui.ApplicationMenus;
import gov.va.isaac.interfaces.gui.MenuItemI;
import gov.va.isaac.interfaces.gui.views.IsaacViewWithMenusI;
import gov.va.isaac.interfaces.gui.views.PopupViewI;
import gov.va.isaac.interfaces.utility.DialogResponse;

import java.util.ArrayList;
import java.util.List;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import javax.inject.Singleton;

import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link LogicViewStatus}
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
@Service
@Singleton
public class LogicViewStatus implements PopupViewI, IsaacViewWithMenusI
{
	private static Logger log = LoggerFactory.getLogger(LogicViewStatus.class);
	private BorderPane root_;
	private TextArea summary_;
	private SimpleBooleanProperty running_ = new SimpleBooleanProperty(false);
	private boolean cancelRequested_ = false;
	public boolean classificationStatus = false;
	
	private LogicViewStatus()
	{
		//For HK2
	}
	
	/**
	 * @see gov.va.isaac.interfaces.gui.views.PopupViewI#showView(javafx.stage.Window)
	 */
	@Override
	public void showView(Window parent)
	{
		//No GUI
	}
	
	public boolean toggleClassification() {
		if(classificationStatus) {
			return false;
		} else {
			return true;
		}
		
	}
	
	/**
	 * @see gov.va.isaac.interfaces.gui.views.IsaacViewWithMenusI#getMenuBarMenus()
	 */
	@Override
	public List<MenuItemI> getMenuBarMenus()
	{
		ArrayList<MenuItemI> menus = new ArrayList<>();

		menus.add(new MenuItemI()
		{
			@Override
			public void handleMenuSelection(Window parent, MenuItem menuItem) 
			{
				String question = "Incremental classification is currently " + ((classificationStatus) ? "On" : "Off") + ", would you like to turn it On? No Turns it off.";
				DialogResponse response = AppContext.getCommonDialogs().showYesNoDialog("Incremental Classification", question);
				
				
				if(response == DialogResponse.YES) 
				{
					menuItem.setText("Incremental Classification On (Turn Off)");
					classificationStatus = true;
				} else {
					menuItem.setText("Incremental Classification Off (Turn On)");
					classificationStatus = false;
				}
			}

			@Override
			public int getSortOrder()
			{
				return 22;
			}

			@Override
			public String getParentMenuId()
			{
				return ApplicationMenus.ACTIONS.getMenuId();
			}

			@Override
			public String getMenuName()
			{
				if(classificationStatus) {
					return "Incremental Classification On (Turn Off)";
				} else {
					return "Incremental Classification Off (Turn On)";
				}
			}				

			@Override
			public String getMenuId()
			{
				return "toggleClasification";
			}

			@Override
			public boolean enableMnemonicParsing()
			{
				return false;
			}

			/**
			 * @see gov.va.isaac.interfaces.gui.MenuItemI#getImage()
			 */
			@Override
			public Image getImage()
			{
				return Images.BALLOON_PLUS.getImage();
			}

		});
		return menus;
	}
}
