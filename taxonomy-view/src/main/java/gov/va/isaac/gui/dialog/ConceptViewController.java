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
 *	 http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gov.va.isaac.gui.dialog;

import gov.va.isaac.AppContext;
import gov.va.isaac.ExtendedAppContext;
import gov.va.isaac.config.generated.StatedInferredOptions;
import gov.va.isaac.config.profiles.UserProfile;
import gov.va.isaac.config.profiles.UserProfileBindings;
import gov.va.isaac.config.profiles.UserProfileBindings.RelationshipDirection;
import gov.va.isaac.config.profiles.UserProfileManager;
import gov.va.isaac.gui.SimpleDisplayConcept;
import gov.va.isaac.gui.dragAndDrop.DragRegistry;
import gov.va.isaac.gui.dragAndDrop.SingleConceptIdProvider;
import gov.va.isaac.gui.treeview.SctTreeViewIsaacView;
import gov.va.isaac.gui.util.CopyableLabel;
import gov.va.isaac.gui.util.CustomClipboard;
import gov.va.isaac.gui.util.Images;
import gov.va.isaac.interfaces.gui.views.commonFunctionality.SememeViewI;
import gov.va.isaac.util.CommonlyUsedConcepts;
import gov.va.isaac.util.OchreUtility;
import gov.va.isaac.util.Utility;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.LookupService;
import gov.vha.isaac.ochre.api.chronicle.LatestVersion;
import gov.vha.isaac.ochre.api.component.concept.ConceptChronology;
import gov.vha.isaac.ochre.api.component.concept.ConceptService;
import gov.vha.isaac.ochre.api.component.concept.ConceptSnapshot;
import gov.vha.isaac.ochre.api.component.concept.ConceptSnapshotService;
import gov.vha.isaac.ochre.api.component.concept.ConceptVersion;
import gov.vha.isaac.ochre.api.coordinate.TaxonomyCoordinate;
import gov.vha.isaac.ochre.impl.utility.Frills;
import java.util.Optional;
import java.util.UUID;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller class for {@link ConceptView}.
 *
 * @author ocarlsen
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a> 
 */
public class ConceptViewController {

	private static final Logger LOG = LoggerFactory.getLogger(ConceptViewController.class);

	@FXML private AnchorPane anchorPane;
	@FXML private Label conceptDefinedLabel;
	@FXML private Label conceptStatusLabel;
	@FXML private VBox descriptionsTableHolder;
	@FXML private Label fsnLabel;
	@FXML private VBox idVBox;
	@FXML private VBox relationshipsTableHolder;
	@FXML private SplitPane splitPane;
	@FXML private VBox splitRight;
	@FXML private Label uuidLabel;
	@FXML private Label idLabel;
	@FXML private Label idLabelValue;
	@FXML private VBox annotationsRegion;
	@FXML private ToggleButton stampToggle;
	@FXML private ToggleButton historyToggle;
	@FXML private ToggleButton activeOnlyToggle;
	@FXML private Button descriptionTypeButton;
	@FXML private HBox sourceRelTitleHBox;

	private Button showInTreeButton;
	private ProgressIndicator treeViewProgress;
	private final BooleanProperty treeViewSearchRunning = new SimpleBooleanProperty(false);

	private SctTreeViewIsaacView sctTree;
	private SememeViewI sememeView;
	private DescriptionTableView dtv;
	private RelationshipTableView rtv;
	
	private UUID conceptUuid;
	private int conceptNid = 0;
	
	private BooleanProperty displayFSN_ = new SimpleBooleanProperty();

	// Contains StampCoordinate, LanguageCoordinate and LogicCoordinate
    private ReadOnlyObjectWrapper<TaxonomyCoordinate> taxonomyCoordinate = new ReadOnlyObjectWrapper<>();
    
    public ReadOnlyObjectProperty<TaxonomyCoordinate> getTaxonomyCoordinate() {
    	if (taxonomyCoordinate.get() == null) {
    		taxonomyCoordinate.bind(AppContext.getService(UserProfileBindings.class).getTaxonomyCoordinate());
    	}
    	
    	return taxonomyCoordinate;
    }
    
    private ReadOnlyObjectWrapper<ConceptSnapshotService> conceptSnapshotService = new ReadOnlyObjectWrapper<>();
    public ReadOnlyObjectProperty<ConceptSnapshotService> getConceptSnapshotService() {
    	if (conceptSnapshotService.get() == null) {
    		conceptSnapshotService.set(LookupService.getService(ConceptService.class).getSnapshot(
    				getTaxonomyCoordinate().get().getStampCoordinate(),
    				getTaxonomyCoordinate().get().getLanguageCoordinate()));
    	}
    	
    	return conceptSnapshotService;
    }

	@FXML
	void initialize()
	{
		ProgressBar pb = new ProgressBar();
		pb.setMinWidth(300);
		fsnLabel.setGraphic(pb);
		
		stampToggle.setText("");
		stampToggle.setGraphic(Images.STAMP.createImageView());
		stampToggle.setTooltip(new Tooltip("Show Stamp columns when pressed, Hides Stamp columns when not pressed"));
		stampToggle.setSelected(true);

		activeOnlyToggle.setText("");
		activeOnlyToggle.setGraphic(Images.FILTER_16.createImageView());
		activeOnlyToggle.setTooltip(new Tooltip("Filter to only show active items when pressed, Show all items when not pressed"));
		activeOnlyToggle.setSelected(true);
		
		historyToggle.setText("");
		historyToggle.setGraphic(Images.HISTORY.createImageView());
		historyToggle.setTooltip(new Tooltip("Shows full history when pressed, Only shows current items when not pressed"));
		
		//Listen to changes to global - but no longer push local changes back to global
		ExtendedAppContext.getUserProfileBindings().getLanguageCoordinate().addListener(change -> 
		{
			displayFSN_.set(ExtendedAppContext.getUserProfileBindings().getLanguageCoordinate().get().isFSNPreferred());
		});
		
		descriptionTypeButton.setText("");
		ImageView displayFsn = Images.DISPLAY_FSN.createImageView();
		Tooltip.install(displayFsn, new Tooltip("Displaying the Fully Specified Name - click to display the Preferred Term"));
		displayFsn.visibleProperty().bind(displayFSN_);
		ImageView displayPreferred = Images.DISPLAY_PREFERRED.createImageView();
		displayPreferred.visibleProperty().bind(displayFSN_.not());
		Tooltip.install(displayPreferred, new Tooltip("Displaying the Preferred Term - click to display the Fully Specified Name"));
		descriptionTypeButton.setGraphic(new StackPane(displayFsn, displayPreferred));
		descriptionTypeButton.prefHeightProperty().bind(historyToggle.heightProperty());
		descriptionTypeButton.prefWidthProperty().bind(historyToggle.widthProperty());
		descriptionTypeButton.setOnAction(new EventHandler<ActionEvent>()
		{
			@Override
			public void handle(ActionEvent event)
			{
				displayFSN_.set(displayFSN_.not().get());
			}
		});
		
		splitPane.setDividerPositions(0.7);
	}
	
	public Region getRootNode()
	{
		return anchorPane;
	}

	public void setConcept(ConceptChronology<?> concept) {
		conceptUuid = concept.getPrimordialUuid();

		// Update text of labels.
		Optional<LatestVersion<? extends ConceptVersion<?>>> latestVersionOptional = OchreUtility.getLatestConceptVersion(concept, getConceptSnapshotService().get().getStampCoordinate());
		conceptStatusLabel.setText(latestVersionOptional.get().value().getState().name());
		
		final SimpleStringProperty conceptDescriptionSSP = new SimpleStringProperty("Loading...");
		fsnLabel.textProperty().bind(conceptDescriptionSSP);
		CopyableLabel.addCopyMenu(fsnLabel);
		
		MenuItem copyFull = new MenuItem("Copy Full Concept");
		copyFull.setGraphic(Images.COPY.createImageView());
		fsnLabel.getContextMenu().getItems().add(copyFull);
		
		AppContext.getService(DragRegistry.class).setupDragOnly(fsnLabel, new SingleConceptIdProvider()
		{
			@Override
			public String getConceptId()
			{
				return uuidLabel.getText();
			}
		});
		uuidLabel.setText(concept.getPrimordialUuid().toString());
		AppContext.getService(DragRegistry.class).setupDragOnly(uuidLabel, new SingleConceptIdProvider()
		{
			@Override
			public String getConceptId()
			{
				return uuidLabel.getText();
			}
		});
		CopyableLabel.addCopyMenu(uuidLabel);
		
		final SimpleStringProperty conceptID = new SimpleStringProperty("");
		idLabel.setVisible(false);
		idLabelValue.textProperty().bind(conceptID);
		CopyableLabel.addCopyMenu(idLabelValue);

		
		dtv = new DescriptionTableView(stampToggle.selectedProperty(), historyToggle.selectedProperty(), activeOnlyToggle.selectedProperty());
		descriptionsTableHolder.getChildren().add(dtv.getView());
		VBox.setVgrow(dtv.getView(), Priority.ALWAYS);
		
		//rel table section
		rtv = new RelationshipTableView(stampToggle.selectedProperty(), historyToggle.selectedProperty(), activeOnlyToggle.selectedProperty(), getTaxonomyCoordinate(), getConceptSnapshotService());
		relationshipsTableHolder.getChildren().add(rtv.getView());
		VBox.setVgrow(rtv.getView(), Priority.ALWAYS);

		try
		{
			Button taxonomyViewMode = new Button();
			taxonomyViewMode.setPadding(new Insets(2.0));
			ImageView taxonomyInferred = Images.TAXONOMY_INFERRED.createImageView();
			taxonomyInferred.visibleProperty().bind(AppContext.getService(UserProfileBindings.class).getStatedInferredPolicy().isEqualTo(StatedInferredOptions.INFERRED));
			Tooltip.install(taxonomyInferred, new Tooltip("Displaying the Inferred view- click to display the Inferred then Stated view"));
			ImageView taxonomyStated = Images.TAXONOMY_STATED.createImageView();
			taxonomyStated.visibleProperty().bind(AppContext.getService(UserProfileBindings.class).getStatedInferredPolicy().isEqualTo(StatedInferredOptions.STATED));
			Tooltip.install(taxonomyStated, new Tooltip("Displaying the Stated view- click to display the Inferred view"));
			taxonomyViewMode.setGraphic(new StackPane(taxonomyInferred, taxonomyStated));
			taxonomyViewMode.setOnAction(new EventHandler<ActionEvent>()
			{
				@Override
				public void handle(ActionEvent event)
				{
					try
					{
						UserProfile up = ExtendedAppContext.getCurrentlyLoggedInUserProfile();
						StatedInferredOptions sip = null;
						if (AppContext.getService(UserProfileBindings.class).getStatedInferredPolicy().get() == StatedInferredOptions.STATED)
						{
							sip = StatedInferredOptions.INFERRED;
						}
						else if (AppContext.getService(UserProfileBindings.class).getStatedInferredPolicy().get() == StatedInferredOptions.INFERRED)
						{
							sip = StatedInferredOptions.STATED;
						}
						else
						{
							LOG.error("Unexpected error!");
							return;
						}
						up.setStatedInferredPolicy(sip);
						ExtendedAppContext.getService(UserProfileManager.class).saveChanges(up);
					}
					catch (Exception e)
					{
						LOG.error("Unexpected error storing pref change", e);
					}
				}
			});
			HBox.setMargin(taxonomyViewMode, new Insets(0, 0, 0, 5.0));
			sourceRelTitleHBox.getChildren().add(taxonomyViewMode);
			
			Button relationshipViewMode = new Button();
			relationshipViewMode.setPadding(new Insets(2.0));
			ImageView taxonomySource = Images.TAXONOMY_SOURCE.createImageView();
			taxonomySource.visibleProperty().bind(AppContext.getService(UserProfileBindings.class).getDisplayRelDirection().isEqualTo(RelationshipDirection.SOURCE));
			Tooltip.install(taxonomySource, new Tooltip("Displaying the Source (parent) Relationships only, click to display Target"));
			ImageView taxonomyTarget = Images.TAXONOMY_TARGET.createImageView();
			taxonomyTarget.visibleProperty().bind(AppContext.getService(UserProfileBindings.class).getDisplayRelDirection().isEqualTo(RelationshipDirection.TARGET));
			Tooltip.install(taxonomyTarget, new Tooltip("Displaying the Target (child) Relationships only, click to display Source and Target"));
			ImageView taxonomySourceAndTarget = Images.TAXONOMY_SOURCE_AND_TARGET.createImageView();
			taxonomySourceAndTarget.visibleProperty().bind(AppContext.getService(UserProfileBindings.class).getDisplayRelDirection().isEqualTo(RelationshipDirection.SOURCE_AND_TARGET));
			Tooltip.install(taxonomySourceAndTarget, new Tooltip("Displaying the Source and Target Relationships, click to display Source only"));
			relationshipViewMode.setGraphic(new StackPane(taxonomySource, taxonomyTarget, taxonomySourceAndTarget));
			relationshipViewMode.setOnAction(new EventHandler<ActionEvent>()
			{
				@Override
				public void handle(ActionEvent event)
				{
					try
					{
						UserProfile up = ExtendedAppContext.getCurrentlyLoggedInUserProfile();
						if (up.getDisplayRelDirection() == RelationshipDirection.SOURCE)
						{
							up.setDisplayRelDirection(RelationshipDirection.TARGET);
						}
						else if (up.getDisplayRelDirection() == RelationshipDirection.TARGET)
						{
							up.setDisplayRelDirection(RelationshipDirection.SOURCE_AND_TARGET);
						}
						else if (up.getDisplayRelDirection() == RelationshipDirection.SOURCE_AND_TARGET)
						{
							up.setDisplayRelDirection(RelationshipDirection.SOURCE);
						}
						else 
						{
							LOG.error("Unhandeled case!");
						}
						ExtendedAppContext.getService(UserProfileManager.class).saveChanges(up);
					}
					catch (Exception e)
					{
						LOG.error("Unexpected error storing pref change", e);
					}
				}
			});
			HBox.setMargin(relationshipViewMode, new Insets(0, 0, 0, 5.0));
			sourceRelTitleHBox.getChildren().add(relationshipViewMode);

			Label summary = new Label();
			HBox.setMargin(summary, new Insets(0, 0, 0, 5.0));
			sourceRelTitleHBox.getChildren().add(summary);
			summary.textProperty().bind(rtv.getSummaryText());
			
		}
		catch (Exception e)
		{
			LOG.error("Error configuring relationship view", e);
			descriptionsTableHolder.getChildren().add(new Label("Unexpected error configuring descriptions view"));
		}
		
		// Load the inner tree view.
		try {
			sctTree = AppContext.getService(SctTreeViewIsaacView.class); 
			sctTree.init();
			
			showInTreeButton = new Button();
			showInTreeButton.setPadding(new Insets(2.0));
			showInTreeButton.setGraphic(Images.TAXONOMY_SEARCH_RESULT_ANCESTOR.createImageView());
			showInTreeButton.setTooltip(new Tooltip("Find Concept"));
			showInTreeButton.visibleProperty().bind(treeViewSearchRunning.not());
			showInTreeButton.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event) {
					treeViewSearchRunning.set(true);
					sctTree.locateConcept(concept.getPrimordialUuid(), treeViewSearchRunning);
				}
			});
			
			treeViewProgress = new ProgressIndicator(-1);
			treeViewProgress.setMaxSize(16, 16);
			treeViewProgress.visibleProperty().bind(treeViewSearchRunning);

			StackPane sp = new StackPane(showInTreeButton, treeViewProgress);
			sctTree.addToToolBar(sp);
			
			Region r = sctTree.getView();
			splitRight.getChildren().add(r);
			VBox.setVgrow(r, Priority.ALWAYS);
			treeViewSearchRunning.set(true);
			sctTree.locateConcept(concept.getPrimordialUuid(), treeViewSearchRunning);
		} catch (Exception ex) {
			LOG.error("Error creating tree view", ex);
			splitRight.getChildren().add(new Label("Unexpected error building tree"));
		}
		
		Utility.execute(() -> {
			String conceptDescription = OchreUtility.getDescription(concept.getConceptSequence(), getConceptSnapshotService().get().getStampCoordinate(), 
					getConceptSnapshotService().get().getLanguageCoordinate()).get();
			
			ConceptSnapshot conceptSnapshot = getConceptSnapshotService().get().getConceptSnapshot(concept.getNid());
			conceptNid = concept.getNid();
			AppContext.getService(CommonlyUsedConcepts.class).addConcept(new SimpleDisplayConcept(concept.getConceptSequence()));

			
			Optional<Long> sctID = Frills.getSctId(concept.getNid(), conceptSnapshot.getStampCoordinate());

			Platform.runLater(() -> {
				conceptDescriptionSSP.set(conceptDescription);
				fsnLabel.setGraphic(null);
				if (sctID.isPresent())
				{
					idLabel.setVisible(true);
					conceptID.set(sctID.get() + "");
				}

				copyFull.setOnAction(e -> CustomClipboard.set(conceptSnapshot.toUserString()));
				
				try {
					dtv.setConcept(conceptSnapshot);
				} catch (Exception e) {
					LOG.error("Error configuring description view", e);
					descriptionsTableHolder.getChildren().add(new Label("Unexpected error configuring descriptions view"));
				}
				
				try {
					rtv.setConcept(conceptSnapshot);
				} catch (Exception e) {
					LOG.error("Error configuring relationship view", e);
					descriptionsTableHolder.getChildren().add(new Label("Unexpected error configuring relationships view"));
				}

				sememeView = LookupService.getNamedServiceIfPossible(SememeViewI.class, "DynamicRefexView");
				sememeView.setComponent(conceptSnapshot.getNid(), stampToggle.selectedProperty(), activeOnlyToggle.selectedProperty(), historyToggle.selectedProperty(), false);
				sememeView.getView().setMinHeight(100.0);
				VBox.setVgrow(sememeView.getView(), Priority.ALWAYS);
				annotationsRegion.getChildren().add(sememeView.getView());

				stampToggle.setSelected(false);
			});
		});
	}

	public StringProperty getTitle() {
		return fsnLabel.textProperty();
	}

	public UUID getConceptUuid() {
		return conceptUuid;
	}

	public int getConceptNid() {
		if (conceptNid == 0) {
			// TODO background
			conceptNid = Get.identifierService().getNidForUuids(conceptUuid);
		}
		
		return conceptNid;
	}
	
	/**
	 * Disconnects change listeners, prevents refresh
	 */
	public void viewDiscarded()
	{
		if (sctTree != null) {
			sctTree.viewDiscarded();
		}
		if (sememeView != null) {
			sememeView.viewDiscarded();
		}
		if (dtv != null) {
			dtv.viewDiscarded();
		}
		if (rtv != null) {
			rtv.viewDiscarded();
		}
	}
}
