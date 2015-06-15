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

import gov.va.isaac.gui.util.Images;
import gov.va.isaac.interfaces.gui.ApplicationMenus;
import gov.va.isaac.interfaces.gui.MenuItemI;
import gov.va.isaac.interfaces.gui.views.IsaacViewWithMenusI;
import gov.va.isaac.interfaces.gui.views.PopupViewI;

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
 * {@link LogicView}
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
@Service
@Singleton
public class LogicView implements PopupViewI, IsaacViewWithMenusI
{
	private static Logger log = LoggerFactory.getLogger(LogicView.class);
	private BorderPane root_;
	private TextArea summary_;
	private SimpleBooleanProperty running_ = new SimpleBooleanProperty(false);
	private boolean cancelRequested_ = false;
	
	private LogicView()
	{
		//For HK2
	}
	
	private void initGui()
	{
		root_ = new BorderPane();
		root_.setPrefWidth(550);
		
		VBox titleBox = new VBox();
		
		Label title = new Label("Run Full Classification");
		title.getStyleClass().add("titleLabel");
		title.setAlignment(Pos.CENTER);
		title.setMaxWidth(Double.MAX_VALUE);
		title.setPadding(new Insets(10));
		titleBox.getChildren().add(title);
		titleBox.getStyleClass().add("headerBackground");
		titleBox.setPadding(new Insets(5, 5, 5, 5));
		root_.setTop(titleBox);
		
		VBox centerContent = new VBox();
		centerContent.setFillWidth(true);
		centerContent.setPrefWidth(Double.MAX_VALUE);
		centerContent.setPadding(new Insets(10));
		centerContent.getStyleClass().add("itemBorder");
		centerContent.setSpacing(10.0);
		
		
		root_.setCenter(centerContent);
		
		//Bottom buttons
		HBox buttons = new HBox();
		buttons.setMaxWidth(Double.MAX_VALUE);
		buttons.setAlignment(Pos.CENTER);
		buttons.setPadding(new Insets(5));
		buttons.setSpacing(30);

		Button cancel = new Button("Close");
		cancel.setOnAction((action) ->
		{
			if (running_.get())
			{
				addLine("Cancelling...");
				cancel.setDisable(true);
				cancelRequested_ = true;
			}
			else
			{
				cancel.getScene().getWindow().hide();
				root_ = null;
			}
		});
		buttons.getChildren().add(cancel);

		Button action = new Button("Run Classification");
		action.setOnAction((theAction) ->
		{
			//Utility.execute(() -> sync());
		});
		buttons.getChildren().add(action);
		
		cancel.minWidthProperty().bind(action.widthProperty());
		
		running_.addListener(change ->
		{
			if (running_.get())
			{
				cancel.setText("Cancel");
			}
			else
			{
				cancel.setText("Close");
			}
			cancel.setDisable(false);
		});
		
		root_.setBottom(buttons);
	}
	
	private void addLine(String line)
	{
		Runnable work = new Runnable()
		{
			@Override
			public void run()
			{
				summary_.setText(summary_.getText() + line + "\n");
			}
		};
		if (Platform.isFxApplicationThread())
		{
			work.run();
		}
		else
		{
			Platform.runLater(work);
		}
	}
	

	/**
	 * @see gov.va.isaac.interfaces.gui.views.PopupViewI#showView(javafx.stage.Window)
	 */
	@Override
	public void showView(Window parent)
	{
		initGui();
		Stage stage = new Stage(StageStyle.DECORATED);
		stage.initModality(Modality.NONE);
		stage.initOwner(parent);
		Scene scene = new Scene(root_);
		stage.setScene(scene);
		stage.setTitle("Run Classification");
		stage.getScene().getStylesheets().add(LogicView.class.getResource("/isaac-shared-styles.css").toString());
		stage.sizeToScene();
		stage.show();
		stage.setOnCloseRequest(windowEvent -> 
		{
			if (running_.get())
			{
				windowEvent.consume();
			}
		});
	}
	
	public boolean runClassification() {
		return true;
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
				LogicView.this.showView(parent);
			}

			@Override
			public int getSortOrder()
			{
				return 21;
			}

			@Override
			public String getParentMenuId()
			{
				return ApplicationMenus.ACTIONS.getMenuId();
			}

			@Override
			public String getMenuName()
			{
				return "Run Full Classification";
			}

			@Override
			public String getMenuId()
			{
				return "runClassification";
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
