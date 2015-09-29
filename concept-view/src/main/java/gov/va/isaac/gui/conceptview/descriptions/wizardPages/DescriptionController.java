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

import gov.va.isaac.gui.conceptview.data.ConceptDescription;
import gov.va.isaac.gui.conceptview.descriptions.PanelControllers;
import gov.va.isaac.gui.conceptview.descriptions.ScreensController;
import gov.va.isaac.util.UpdateableBooleanBinding;

import java.util.ArrayList;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
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
 * {@link DescriptionController}
 *
 * @author <a href="jefron@apelon.com">Jesse Efron</a>
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public class DescriptionController implements PanelControllers {
	@FXML private ResourceBundle resources;

	// Synonyms
	@FXML private VBox synonymVBox;
	@FXML private VBox descTypeVBox;
	@FXML private VBox languageVBox;
	@FXML private VBox caseVBox;
	@FXML private GridPane synonymGridPane;


	// Panel
	@FXML private AnchorPane descriptionModificationPane;
	@FXML private Label modTypeLabel;
	@FXML private Button continueButton;
	@FXML private Button cancelButton;
	
	private final Logger LOG = LoggerFactory.getLogger(DescriptionController.class);
	
	ScreensController processController;

	private TermRow desc;
	
	private UpdateableBooleanBinding allValid;

	//TODO (artf231890) this page still needs a vertical scrollbar
	@Override
	public void initialize() {
		LOG.debug("Wizarding through a description");
			
		// Buttons
		cancelButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent e) {
				((Stage)descriptionModificationPane.getScene().getWindow()).close();
			}});
	
		continueButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent e) {
				processValues();
				processController.loadAcceptabilityScreen();
                                processController.setScreen(ScreensController.ACCEPTABILITY_SCREEN);
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
                                if (desc != null && !desc.isValid().get())
                                {
                                        return false;
                                }
				
                                return true;
			}
		};
		continueButton.disableProperty().bind(allValid.not());
	}
	
	private void addBlankRow()
	{
		desc = new TermRow();
		allValid.addBinding(desc.isValid());
		synonymVBox.getChildren().add(desc.getTermNode());

		// Setup Acceptable
		descTypeVBox.getChildren().add(desc.getTypeNode());
		
		// Setup Case
		caseVBox.getChildren().add(desc.getSignificanceNode());	

                // Setup Lang
		languageVBox.getChildren().add(desc.getLanguageNode());	
        }

	private void addPopulatedRow()
	{
            ConceptDescription editDesc = processController.getWizard().getEditDescription();
            desc = new TermRow();
            desc.populateRow(editDesc);
            
            allValid.addBinding(desc.isValid());
            synonymVBox.getChildren().add(desc.getTermNode());

            // Setup Acceptable
            descTypeVBox.getChildren().add(desc.getTypeNode());

            // Setup Case
            caseVBox.getChildren().add(desc.getSignificanceNode());	

            // Setup Language
            languageVBox.getChildren().add(desc.getLanguageNode());	
	}
	
	@Override
	public void finishInit(ScreensController screenPage) {
		processController = screenPage;
                if (processController.getWizard().isNew()) {
                    modTypeLabel.setText("New Description");
                } else {
                    modTypeLabel.setText("Edit Description");
                }
                
		
		// Screen Components
                if (processController.getWizard().isNew()) {
                    addBlankRow();
                } else {
                    addPopulatedRow();
                }
		
	}

	@Override
	public void processValues() {
		processController.getWizard().setNewDescription(desc); 
	}

}