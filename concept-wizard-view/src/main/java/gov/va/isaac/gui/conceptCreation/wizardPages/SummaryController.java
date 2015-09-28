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
package gov.va.isaac.gui.conceptCreation.wizardPages;

import gov.va.isaac.AppContext;
import gov.va.isaac.gui.conceptCreation.PanelControllers;
import gov.va.isaac.gui.conceptCreation.ScreensController;
import gov.va.isaac.interfaces.gui.constants.SharedServiceNames;
import gov.va.isaac.interfaces.gui.views.commonFunctionality.PopupConceptViewI;
import gov.va.isaac.util.OchreUtility;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.LookupService;
import gov.vha.isaac.ochre.api.component.concept.ConceptChronology;

import java.util.List;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * 
 * {@link SummaryController}
 *
 * @author <a href="jefron@apelon.com">Jesse Efron</a>
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 * @author <a href="mailto:joel.kniaz@gmail.com">Joel Kniaz</a>
 */
public class SummaryController implements PanelControllers {
	
	@FXML private TextField conceptFSN;
	@FXML private TextField conceptPT;
	@FXML private TextField conceptPrimDef;
	@FXML private VBox parentVBox;
	
	@FXML private GridPane synonymGridPane;
	@FXML private Label noSynsLabel;
	@FXML private VBox termVBox;
	@FXML private VBox acceptVBox;
	@FXML private VBox caseVBox;
	@FXML private VBox langVBox;
	
	@FXML private GridPane relationshipGridPane;
	@FXML private Label noRelsLabel;
	@FXML private VBox relationshipVBox;
	@FXML private VBox relTypeVBox;
	@FXML private VBox targetVBox;
	@FXML private VBox qualRoleVBox;
	@FXML private VBox groupVBox;

	@FXML private BorderPane summaryPane;
	@FXML private Button cancelButton;
	@FXML private Button startOverButton;
	@FXML private Button saveButton;
	@FXML private Button backButton;

	static ScreensController processController;

	private static final Logger LOGGER = LoggerFactory.getLogger(SummaryController.class);

	@Override
	public void initialize() {
		cancelButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent e) {
				((Stage)summaryPane.getScene().getWindow()).close();
			}
		});
	
		saveButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent e) {
				processValues();
				((Stage)summaryPane.getScene().getWindow()).close();
			}
		});

		startOverButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent e) {
				processController.unloadScreen(ScreensController.SUMMARY_SCREEN);
				processController.setScreen(ScreensController.DEFINITION_SCREEN);
			}
		});
		
		backButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent e) {
				processController.unloadScreen(ScreensController.SUMMARY_SCREEN);
				processController.setScreen(ScreensController.COMPONENTS_SCREEN);
			}
		});
	}
	
	private void setupConcept() {
		conceptFSN.setText(processController.getWizard().getConceptFSN());
		conceptPT.setText(OchreUtility.stripSemanticTag(processController.getWizard().getConceptFSN()));

		addAllParents(processController.getWizard().getParents());
	}

	private void setupSynonyms() {
		int synCount = processController.getWizard().getSynonymsCreated();
		
		if (synCount == 0) {
			noSynsLabel.setVisible(true);
			synonymGridPane.setVisible(false);
		} else {
			noSynsLabel.setVisible(false);
			synonymGridPane.setVisible(true);
			for (int i = 0; i < synCount; i++) {
				Label term = new Label(processController.getWizard().getTerm(i));
				Label accept = new Label(processController.getWizard().getTypeString(i));
				Label caseSens = new Label(processController.getWizard().getCaseSensitivity(i));
				Label lang = new Label(processController.getWizard().getLanguage(i));
	
				termVBox.getChildren().add(term);
				acceptVBox.getChildren().add(accept);
				caseVBox.getChildren().add(caseSens);
				langVBox.getChildren().add(lang);
			}
		}
	}
	
	private void setupRelationships() {
		int relCount = processController.getWizard().getRelationshipsCreated();
		
		if (relCount == 0) {
			noRelsLabel.setVisible(true);
			relationshipGridPane.setVisible(false);
		} else {
			noRelsLabel.setVisible(false);
			relationshipGridPane.setVisible(true);
			
			for (int i = 0; i < relCount; i++) {
				Label relType = new Label(processController.getWizard().getRelType(i));
				Label target = new Label(processController.getWizard().getTarget(i));
				Label qualRole = new Label(processController.getWizard().getQualRole(i));
				Label group = new Label(processController.getWizard().getGroup(i));
	
				relTypeVBox.getChildren().add(relType);
				targetVBox.getChildren().add(target);
				qualRoleVBox.getChildren().add(qualRole);
				groupVBox.getChildren().add(group);
			}
		}
	}

	private void addAllParents(List<Integer> parents) {
		try {
			for (int p : parents) {
				Label tf = new Label(Get.conceptService().getConcept(p).getConceptDescriptionText());
				parentVBox.getChildren().add(tf);
			}
				
		} catch (Exception e) {
			LOGGER.error("Could not find preferred description of one or more parents", e);
		}
	}

	@Override
	public void finishInit(ScreensController screenParent){
		processController = screenParent;

		setupConcept();
		setupSynonyms();
		setupRelationships();
	}

	@Override
	public void processValues() {
		try {
			
			ConceptChronology newChronology = processController.getWizard().createNewConcept();
			
//			Get.commitService().addUncommitted(newChronology);
//			Get.commitService().commit(
//					newChronology, 
//					ExtendedAppContext.getUserProfileBindings().getEditCoordinate().get(), 
//					"WizardController adding concept: " + "fsn");
			
			PopupConceptViewI cv = LookupService.getService(PopupConceptViewI.class, SharedServiceNames.DIAGRAM_STYLE);
			cv.setConcept(newChronology.getNid());

			cv.showView(null);

			
			
//			Ts.get().addUncommitted(Ts.get().getConceptForNid(newCon.getNid()));
			//boolean committed = 
//			Ts.get().commit(/* newCon.getNid() */);
			//TODO OCHRE - figure out how commits get voided now that they don't return boolean
//			if (!committed)
//			{
//				AppContext.getCommonDialogs().showErrorDialog("Commit Failed", "The concept could not be committed", "The commit was vetoed by a validator");
//				ListBatchViewI lv = LookupService.getService(ListBatchViewI.class, SharedServiceNames.DOCKED);
//				if (lv != null)
//				{
//					lv.addConcept(newCon.getNid());
//					AppContext.getMainApplicationWindow().ensureDockedViewIsVisble((DockedViewI)lv);
//				}
//			}
		} catch (Exception e) {
			LOGGER.error("Unable to create and/or commit new concept", e);
			AppContext.getCommonDialogs().showErrorDialog("Error Creating Concept", "Unexpected error creating the Concept", e.getMessage(), summaryPane.getScene().getWindow());
		}
	}
}