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
package gov.va.isaac.gui.conceptview.descriptions.wizardPages;

import gov.va.isaac.AppContext;
import gov.va.isaac.ExtendedAppContext;
import gov.va.isaac.gui.conceptview.descriptions.PanelControllers;
import gov.va.isaac.gui.conceptview.descriptions.ScreensController;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.State;
import gov.vha.isaac.ochre.api.chronicle.LatestVersion;
import gov.vha.isaac.ochre.api.component.sememe.SememeChronology;
import gov.vha.isaac.ochre.api.component.sememe.version.DynamicSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.SememeVersion;
import java.util.Optional;
import java.util.UUID;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * 
 * {@link AcceptabilityController}
 *
 * @author <a href="jefron@apelon.com">Jesse Efron</a>
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public class AcceptabilityController implements PanelControllers {
	
        @FXML private AnchorPane acceptabilityModificationPane;
        @FXML private Label termLabel;
        @FXML private GridPane acceptabilityGridPane;
    
	@FXML private Button cancelButton;
	@FXML private Button saveButton;
	@FXML private Button backButton;

	static ScreensController processController;

	private static final Logger LOGGER = LoggerFactory.getLogger(AcceptabilityController.class);

	@Override
	public void initialize() {
		cancelButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent e) {
				((Stage)acceptabilityModificationPane.getScene().getWindow()).close();
			}
		});
	
		saveButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent e) {
				processValues();
				((Stage)acceptabilityModificationPane.getScene().getWindow()).close();
			}
		});

		backButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent e) {
				processController.unloadScreen(ScreensController.ACCEPTABILITY_SCREEN);
				processController.setScreen(ScreensController.DESCRIPTION_SCREEN);
			}
		});
	}
	
	@Override
	public void finishInit(ScreensController screenParent){
            processController = screenParent;

            termLabel.setText(processController.getWizard().getTermText());
        }

	@Override
	public void processValues() {
            try {

                UUID descUid = processController.getWizard().persistDescription();

                String outputMsg;
                
                if (processController.getWizard().isNew()) {
                    outputMsg = "WizardController adding new description: " + processController.getWizard().getTermText();
                } else {
                    outputMsg = "WizardController updating description: " + processController.getWizard().getTermText();
                }
                LOGGER.info(outputMsg);
            } catch (Exception e) {
                LOGGER.error("Unable to create and/or commit new concept", e);
                AppContext.getCommonDialogs().showErrorDialog("Error Creating Concept", "Unexpected error creating the Concept", e.getMessage(), acceptabilityModificationPane.getScene().getWindow());
            }
	}
}

