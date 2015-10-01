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
import gov.va.isaac.gui.conceptview.descriptions.PanelControllers;
import gov.va.isaac.gui.conceptview.descriptions.ScreensController;
import gov.va.isaac.util.UpdateableBooleanBinding;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
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
	@FXML private Label languageLabel;
	
	@FXML private GridPane acceptabilityGridPane;

	@FXML private VBox neitherVBox;
	@FXML private VBox acceptableVBox;
	@FXML private VBox preferredVBox;
	@FXML private VBox dialectVBox;
	@FXML private VBox addButtonVBox;
	@FXML private VBox removeButtonVBox;

	@FXML private Button cancelButton;
	@FXML private Button saveButton;
	@FXML private Button backButton;

	static ScreensController processController;

	private ArrayList<AcceptabilityRow> acceptabilitySelection = new ArrayList<>();
	private static final Logger LOGGER = LoggerFactory.getLogger(AcceptabilityController.class);

	private UpdateableBooleanBinding allValid;

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
		
		allValid = new UpdateableBooleanBinding()
		{
			{
				setComputeOnInvalidate(true);
			}
			
			@Override
			protected boolean computeValue()
			{
				for (AcceptabilityRow ar : acceptabilitySelection)
				{
					if (!ar.isValid().get())
					{
						return false;
					}
				}
				return true;
			}
		};

		saveButton.disableProperty().bind(allValid.not());
	}

	@Override
	public void finishInit(ScreensController screenParent){
		processController = screenParent;

		String termText = processController.getWizard().getTermText();
		if (termText.length() > 50) {
		termText = termText.substring(0, 50) + "...";
		}
		termLabel.setText(termText);
		languageLabel.setText(languageLabel.getText() + processController.getWizard().getLanguageString());
		
		if (processController.getWizard().isNew() || !addExistingRows()) {
		addDefaultRow();
		}
	}

	@Override
	public void processValues() {
		try {

		UUID uuid = processController.getWizard().persistDescription(acceptabilitySelection);
		
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

	
	private boolean addExistingRows()
	{
		Map<Integer, Integer> existingPairs = processController.getWizard().getAcceptabilitiesForEditDesc();

		// Setup Acceptability
		for (Integer dialect : existingPairs.keySet()) {
		AcceptabilityRow ar = new AcceptabilityRow();
		ar.populateRows(dialect, existingPairs.get(dialect));

		populateRow(ar);
		}

		return !existingPairs.isEmpty();
	}

	private void addBlankRow()
	{
		// Setup Acceptability
		AcceptabilityRow ar = new AcceptabilityRow();
		ar.populateBlankRow();

		populateRow(ar);
	}

	private void addDefaultRow() {
		// Setup Acceptability
		AcceptabilityRow ar = new AcceptabilityRow();
		ar.populateDefaultRow();

		populateRow(ar);
	}

	private void populateRow(AcceptabilityRow ar) {
		acceptabilitySelection.add(ar);
		allValid.addBinding(ar.isValid());

		dialectVBox.getChildren().add(ar.getDialectNode());

		preferredVBox.getChildren().add(ar.getPreferredNode());

		acceptableVBox.getChildren().add(ar.getAcceptableNode());

		neitherVBox.getChildren().add(ar.getNeitherNode());

		// Setup Add Button
		Button addButton = new Button("+");
		addButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent e) {
				addBlankRow();
			}
		});

		addButtonVBox.getChildren().add(addButton);

		// Add new Remove Synonym Button
		Button removeButton = new Button("-");
		removeButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent e) {
				int idx = removeButtonVBox.getChildren().indexOf(e.getSource());
				removeRow(idx);
			}
		});
		removeButtonVBox.getChildren().add(removeButton);
	}

	
	private void removeRow(int idx) {
		AcceptabilityRow ar = acceptabilitySelection.remove(idx);
		allValid.removeBinding(ar.isValid());
		
		dialectVBox.getChildren().remove(idx);
		preferredVBox.getChildren().remove(idx);
		acceptableVBox.getChildren().remove(idx);
		neitherVBox.getChildren().remove(idx);
		
		addButtonVBox.getChildren().remove(idx);
		removeButtonVBox.getChildren().remove(idx);
	}
}

