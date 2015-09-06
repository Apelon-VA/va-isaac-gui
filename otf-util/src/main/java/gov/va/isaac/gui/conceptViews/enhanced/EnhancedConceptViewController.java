package gov.va.isaac.gui.conceptViews.enhanced;

import gov.va.isaac.AppContext;
import gov.va.isaac.ExtendedAppContext;
import gov.va.isaac.config.generated.StatedInferredOptions;
import gov.va.isaac.config.profiles.UserProfileManager;
import gov.va.isaac.gui.conceptViews.helpers.ConceptViewerLabelHelper;
import gov.va.isaac.gui.conceptViews.helpers.ConceptViewerTooltipHelper;
import gov.va.isaac.gui.conceptViews.helpers.EnhancedConceptBuilder;
import gov.va.isaac.interfaces.gui.constants.ConceptViewMode;
import gov.va.isaac.interfaces.gui.views.commonFunctionality.PopupConceptViewI;
import gov.va.isaac.util.OTFUtility;
import gov.va.isaac.util.UpdateableBooleanBinding;
import gov.va.isaac.util.Utility;
import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.ihtsdo.otf.tcc.api.concept.ConceptVersionBI;
import org.ihtsdo.otf.tcc.api.store.Ts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnhancedConceptViewController {
	@FXML protected AnchorPane enhancedConceptPane;

	// Descriptions & Relationships
	@FXML private VBox termVBox;
	@FXML private VBox relVBox;
	@FXML private VBox destVBox;
	@FXML private ScrollPane destScrollPane;
	
	// Top Labels
	@FXML protected Label releaseIdLabel;
	@FXML protected Label isPrimLabel;
	@FXML protected Label fsnLabel;
	@FXML protected VBox conAnnotVBox;
	@FXML protected VBox fsnAnnotVBox;
	
	// Radio Buttons
	@FXML protected ToggleGroup viewGroup;
	@FXML protected RadioButton  historicalRadio;
	@FXML protected RadioButton basicRadio;
	@FXML protected RadioButton detailedRadio;

	// Buttons
	@FXML protected Button closeButton;
	@FXML protected Button previousButton;
	@FXML protected Button commitButton;
	@FXML protected Button cancelButton;
	
	protected ConceptViewerLabelHelper labelHelper;
	protected ConceptViewerTooltipHelper tooltipHelper = new ConceptViewerTooltipHelper();
	
	protected ConceptVersionBI concept;
	private UpdateableBooleanBinding prevButtonQueueFilled;
	
	public PopupConceptViewI conceptView;
	private ConceptViewMode currentMode = ConceptViewMode.SIMPLE_VIEW;

	private EnhancedConceptBuilder creator;

	private boolean initialized = false;
	private boolean discarded = false;
	
	private ArrayList<ChangeListener<?>> changeListeners = new ArrayList<>();

	private static final Logger LOG = LoggerFactory.getLogger(EnhancedConceptViewController.class);
	
	@FXML
	void initialize()
	{
		ProgressBar pb = new ProgressBar();
		pb.setMinWidth(300.0);
		fsnLabel.setGraphic(pb);
		fsnLabel.setText("Loading...");
	}

	AnchorPane getRootNode() {
		return enhancedConceptPane;
	}

	public void setConceptView(EnhancedConceptView enhancedConceptView) {
		conceptView = enhancedConceptView;		
	}

	void setConcept(UUID currentCon, ConceptViewMode mode, ObservableList<Integer> conceptHistoryStack) {
		if (discarded)
		{
			return;
		}
		if (!initialized ) {
			initialized = true;
			UserProfileManager userProfileManager = AppContext.getService(UserProfileManager.class);
			
			{
				ChangeListener<UUID> changeListener = new ChangeListener<UUID>()
				{
					@Override
					public void changed(ObservableValue<? extends UUID> observable, UUID oldValue, UUID newValue)
					{
						LOG.info("Kicking off refresh() due to stated / inferred from {} to {}", oldValue, newValue);
						setConcept(currentCon, mode, conceptHistoryStack);
					}
				};
				
				//Need to keep a ref, otherwise, it will be GC'ed right away
				changeListeners.add(changeListener);
				userProfileManager.getPropertyBindings().getViewCoordinatePath().addListener(new WeakChangeListener<UUID>(changeListener));
			}
			
			{
				ChangeListener<StatedInferredOptions> changeListener = new ChangeListener<StatedInferredOptions>()
				{
					@Override
					public void changed(ObservableValue<? extends StatedInferredOptions> observable, StatedInferredOptions oldValue, StatedInferredOptions newValue)
					{
						LOG.info("Kicking off refresh() due to stated / inferred from {} to {}", oldValue, newValue);
						setConcept(currentCon, mode, conceptHistoryStack);
					}
				};
				//Need to keep a ref, otherwise, it will be GC'ed right away
				changeListeners.add(changeListener);
				userProfileManager.getPropertyBindings().getStatedInferredPolicy().addListener(new WeakChangeListener<StatedInferredOptions>(changeListener));
			}

			initializeWindow(conceptHistoryStack, mode);
		}
		
		clearContents();
		
		// TODO bg thread
		Utility.execute(() -> {
			concept = OTFUtility.getConceptVersion(currentCon);

			Platform.runLater(() -> {
				labelHelper.setConcept(concept.getNid());
				updateCommitButton();
				populateContents(concept, mode);
				//creator.setConceptValues(concept, mode);
			});
		});
	}
	
	void setConcept(UUID currentCon, ConceptViewMode mode) {
		if (!initialized ) {
			initialized = true;
			intializePane(mode);
		}
		
		clearContents();

		// TODO bg thread
		Utility.execute(() -> {
			concept = OTFUtility.getConceptVersion(currentCon);

			Platform.runLater(() -> {
				labelHelper.setConcept(concept.getNid());
				populateContents(concept, mode);
				//creator.setConceptValues(concept, mode);
			});
		});
	}

	void initializeWindow(ObservableList<Integer> conceptHistoryStack, ConceptViewMode view) {
		commonInit(view);
		
		labelHelper.setPrevConStack(conceptHistoryStack);
		labelHelper.setIsWindow(true);

		closeButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent e) {
				((Stage)getRootNode().getScene().getWindow()).close();
			}
		});
		
		previousButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				int lastItemIdx = labelHelper.getPreviousConceptStack().size() - 1;
				int prevConNid = labelHelper.getPreviousConceptStack().remove(lastItemIdx);
				conceptView.setConcept(prevConNid);
			}
		});
		
		commitButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				try
				{
					Ts.get().commit(/* concept */);
					clearContents();
					commitButton.setDisable(true);
					cancelButton.setDisable(true);
					populateContents(concept, currentMode);
					//creator.setConceptValues(concept, currentMode);
				}
				catch (IOException e)
				{
					LOG.error("Unexpected error during commit", e);
					ExtendedAppContext.getCommonDialogs().showErrorDialog("Error committing concept", e);
				}
			}
		});
		
		cancelButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				try {
					Ts.get().forget(concept.getChronicle());

					clearContents();
					commitButton.setDisable(true);
					cancelButton.setDisable(true);
					populateContents(concept, currentMode);
					//creator.setConceptValues(concept, currentMode);
				} catch (Exception e) {
					LOG.error("Unable to cancel concept: " + concept.getNid(), e);
				}
			}
		});
		
		prevButtonQueueFilled = new UpdateableBooleanBinding()
		{
			{
				addBinding(labelHelper.getPreviousConceptStack());
				setComputeOnInvalidate(true);
			}
			
			@Override
			protected boolean computeValue()
			{
				return !labelHelper.getPreviousConceptStack().isEmpty();
			}
		};
		previousButton.disableProperty().bind(prevButtonQueueFilled.not());

		if (getRootNode() == null) {
			LOG.error("getRootNode() is null");
		}
		if (tooltipHelper == null) {
			LOG.error("tooltipHelper is null");
		}
		getRootNode().setOnKeyPressed(tooltipHelper.getCtrlKeyPressEventHandler());
		getRootNode().setOnKeyReleased(tooltipHelper.getCtrlKeyReleasedEventHandler());
		
		detailedRadio.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				conceptView.setViewMode(ConceptViewMode.DETAIL_VIEW);
			}
		});

		basicRadio.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				conceptView.setViewMode(ConceptViewMode.SIMPLE_VIEW);
			}
		});

		historicalRadio.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				conceptView.setViewMode(ConceptViewMode.HISTORICAL_VIEW);
			}
		});
	}
	
	public void setViewMode(ConceptViewMode mode) {
		currentMode = mode;
		clearContents();
		populateContents(concept, mode);
		//creator.setConceptValues(concept, mode);
	}

	private void commonInit(ConceptViewMode mode) {
		creator = new EnhancedConceptBuilder(enhancedConceptPane, termVBox, relVBox, destVBox, destScrollPane, fsnAnnotVBox, conAnnotVBox, fsnLabel, releaseIdLabel, isPrimLabel);
		
		labelHelper = new ConceptViewerLabelHelper(conceptView);
		labelHelper.setPane(getRootNode());
		creator.setLabelHelper(labelHelper);
		
		setModeType(mode);
	}

	void intializePane(ConceptViewMode view) {
		commonInit(view);
		closeButton.setVisible(false);
		previousButton.setVisible(false);
	}

	private void updateCommitButton() {
		// TODO OCHRE getUncommittedConcepts() is unsupported, so never disable commit and cancel buttons
	
		//boolean isUncommitted = OTFUtility.isUncommittened(concept);
		//commitButton.setDisable(!isUncommitted);
		//cancelButton.setDisable(!isUncommitted);
		
		commitButton.setDisable(false);
		cancelButton.setDisable(false);
	}

	public void setModeType(ConceptViewMode mode) {
		currentMode = mode;
		
		if (mode == ConceptViewMode.SIMPLE_VIEW) {
			basicRadio.setSelected(true);
		} else if (mode == ConceptViewMode.DETAIL_VIEW) {
			detailedRadio.setSelected(true);
		} else if (mode == ConceptViewMode.HISTORICAL_VIEW) {
			historicalRadio.setSelected(true);
		}
	}

	public ConceptViewMode getViewMode() {
		return currentMode;
	}
	
	public String getTitle() {
		return fsnLabel.getText();
	}
	
	private void clearContents() {
		releaseIdLabel.setText("");
		isPrimLabel.setText("");
		fsnLabel.setText("");
		termVBox.getChildren().clear();
		destVBox.getChildren().clear();
		relVBox.getChildren().clear();
		conAnnotVBox.getChildren().clear();
		fsnAnnotVBox.getChildren().clear();
		setProgressIndicators(true);
	}
	
	private void populateContents(ConceptVersionBI concept, ConceptViewMode mode) {
		setProgressIndicators(false);
		creator.setConceptValues(concept, mode);
	}
	
	private void setProgressIndicators(boolean enable) {
		if (enable) {
			releaseIdLabel.setGraphic(new ProgressBar());
			isPrimLabel.setGraphic(new ProgressBar());
			if (fsnLabel.getGraphic() == null) {
				fsnLabel.setGraphic(new ProgressBar());
			}
			termVBox.getChildren().add(new ProgressIndicator());
			destVBox.getChildren().add(new ProgressIndicator());
			relVBox.getChildren().add(new ProgressIndicator());
			conAnnotVBox.getChildren().add(new ProgressIndicator());
			fsnAnnotVBox.getChildren().add(new ProgressIndicator());
		} else {
			releaseIdLabel.setGraphic(null);
			isPrimLabel.setGraphic(null);
			fsnLabel.setGraphic(null);
			termVBox.getChildren().clear();
			destVBox.getChildren().clear();
			relVBox.getChildren().clear();
			conAnnotVBox.getChildren().clear();
			fsnAnnotVBox.getChildren().clear();
		}
	}
	
	public void viewDiscarded()
	{
		discarded = true;
	}
}
