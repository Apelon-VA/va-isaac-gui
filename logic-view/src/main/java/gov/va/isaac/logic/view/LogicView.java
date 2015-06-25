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
import gov.va.isaac.util.Utility;
import gov.vha.isaac.csiro.classify.ClassifierProvider;
import gov.vha.isaac.metadata.coordinates.EditCoordinates;
import gov.vha.isaac.metadata.coordinates.LogicCoordinates;
import gov.vha.isaac.metadata.coordinates.StampCoordinates;
import gov.vha.isaac.ochre.api.coordinate.EditCoordinate;
import gov.vha.isaac.ochre.api.coordinate.LogicCoordinate;
import gov.vha.isaac.ochre.api.coordinate.StampCoordinate;
import gov.vha.isaac.ochre.model.coordinate.EditCoordinateImpl;
import gov.vha.isaac.ochre.api.classifier.ClassifierResults;
import gov.vha.isaac.ochre.api.classifier.ClassifierService;

import java.util.ArrayList;
import java.util.List;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
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
	private TextArea summary_ = new TextArea("");
	private SimpleBooleanProperty running_ = new SimpleBooleanProperty(false);
	//private boolean cancelRequested_ = false;
	
	private VBox titleBox_ = new VBox();
	private Label title_ = new Label("Ready to run classification");
	private ProgressBar progressBar_ = new ProgressBar();
	private ScrollPane summaryPane_ = new ScrollPane();
	
	private Task<ClassifierResults> classifierTask_;
	
	private LogicView()
	{
		//For HK2
		title_.getStyleClass().add("titleLabel");
		title_.setAlignment(Pos.CENTER);
		title_.setMaxWidth(Double.MAX_VALUE);
		title_.setPadding(new Insets(10));

		title_.textProperty().addListener(new ChangeListener<String>() {
			@Override
            public void changed(ObservableValue<? extends String> ov, String t, String t1) {
            	if (running_.get()) {
                	addLine(ov.getValue());
            	}
			}
		}); 
		
		progressBar_.setMaxWidth(Double.MAX_VALUE);
		progressBar_.setVisible(false);
		
		summary_.setMaxHeight(100);
		summary_.setMaxWidth(Double.MAX_VALUE);
		summary_.setEditable(false);
		
		summaryPane_.setFitToWidth(true);
		summaryPane_.setHbarPolicy(ScrollBarPolicy.NEVER);
		summaryPane_.setVbarPolicy(ScrollBarPolicy.ALWAYS);
		summaryPane_.setMaxHeight(100);
		summaryPane_.setContent(summary_);
		
		titleBox_.getChildren().addAll(title_, progressBar_, summary_);
		titleBox_.getStyleClass().add("headerBackground");
		titleBox_.setPadding(new Insets(5, 5, 5, 5));
	}
	
	private void initGui()
	{
		root_ = new BorderPane();
		root_.setPrefWidth(550);
		
		//VBox titleBox = new VBox();
		//Label title = new Label("Run Full Classification");

		root_.setTop(titleBox_);
		
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
				//cancelRequested_ = true;
				classifierTask_.cancel();
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
			StampCoordinate stampCoordinate = StampCoordinates.getDevelopmentLatest();
			EditCoordinate  editCoordinate  = EditCoordinates.getDefaultUserSolorOverlay();
	        LogicCoordinate logicCoordinate = LogicCoordinates.getStandardElProfile();
	        editCoordinate = new EditCoordinateImpl(
	                logicCoordinate.getClassifierSequence(), 
	                editCoordinate.getModuleSequence(), editCoordinate.getModuleSequence());

	        ClassifierService classifierService = new ClassifierProvider(stampCoordinate, logicCoordinate, editCoordinate);
			classifierTask_ = classifierService.classify();

			title_.textProperty().bind(classifierTask_.messageProperty());
			running_.bind(classifierTask_.runningProperty());
			//progressBar_.setVisible(true);
			progressBar_.progressProperty().bind(classifierTask_.progressProperty());
			
			classifierTask_.setOnSucceeded(e -> {
				try {
					title_.setText("Classification complete");
					ClassifierResults results = classifierTask_.get();
					//addLine("Results go here");
					addLine("");
					addLine("Classification Complete");
					addLine("  Affected Concepts: " + results.getAffectedConcepts().size());
					addLine("  Equivalent Sets: " + results.getEquivalentSets().size());
							
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			});
			
			classifierTask_.setOnCancelled(e -> {
				title_.setText("Classification cancelled");
				addLine("Classification cancelled");
			});
			
			//Thread classifierThread = new Thread(classifierTask_);
			//classifierThread.setDaemon(false);
			//classifierThread.start();
			
			Utility.execute(classifierTask_);
		});
		buttons.getChildren().add(action);
		
		cancel.minWidthProperty().bind(action.widthProperty());
		
		running_.addListener(change ->
		{
			if (running_.get())
			{
				progressBar_.setVisible(true);
				cancel.setText("Cancel");
				title_.textProperty().bind(classifierTask_.messageProperty());
				action.setDisable(true);
			}
			else
			{
				cancel.setText("Close");
				cancel.setDisable(false);
				title_.textProperty().unbind();
				action.setDisable(false);
				progressBar_.setVisible(false);
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
				summary_.appendText(line + "\n");
				summaryPane_.setVvalue(summaryPane_.getVmax());
				
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
				return "Run Classification";
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
				return Images.CLASSIFIER.getImage();
			}
		});
		return menus;
	}
}
