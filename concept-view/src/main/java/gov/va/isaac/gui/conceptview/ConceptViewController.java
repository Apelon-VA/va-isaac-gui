package gov.va.isaac.gui.conceptview;

import gov.va.isaac.AppContext;
import gov.va.isaac.config.profiles.UserProfileBindings;
import gov.va.isaac.gui.conceptview.data.ConceptDescription;
import gov.va.isaac.gui.conceptview.data.StampedItem;
import gov.va.isaac.gui.conceptview.popups.PopupHelper;
import gov.va.isaac.gui.dialog.DetachablePopOverHelper;
import gov.va.isaac.gui.dragAndDrop.DragRegistry;
import gov.va.isaac.gui.dragAndDrop.SingleConceptIdProvider;
import gov.va.isaac.gui.util.CustomClipboard;
import gov.va.isaac.gui.util.FxUtils;
import gov.va.isaac.gui.util.Images;
import gov.va.isaac.interfaces.PreferencesPersistenceI;
import gov.va.isaac.interfaces.gui.constants.SharedServiceNames;
import gov.va.isaac.interfaces.gui.views.commonFunctionality.LogicalExpressionTreeGraphEmbeddableViewI;
import gov.va.isaac.interfaces.gui.views.commonFunctionality.PreferencesViewI;
import gov.va.isaac.interfaces.gui.views.commonFunctionality.SememeViewI;
import gov.va.isaac.interfaces.gui.views.commonFunctionality.ViewCoordinatePreferencesPluginViewI;
import gov.va.isaac.util.CommonMenuBuilderI;
import gov.va.isaac.util.CommonMenus;
import gov.va.isaac.util.CommonMenus.CommonMenuBuilder;
import gov.va.isaac.util.CommonMenus.CommonMenuItem;
import gov.va.isaac.util.CommonMenusDataProvider;
import gov.va.isaac.util.CommonMenusNIdProvider;
import gov.va.isaac.util.OchreUtility;
import gov.va.isaac.util.UpdateableBooleanBinding;
import gov.va.isaac.util.Utility;
import gov.vha.isaac.metadata.coordinates.StampCoordinates;
import gov.vha.isaac.metadata.coordinates.TaxonomyCoordinates;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.State;
import gov.vha.isaac.ochre.api.chronicle.LatestVersion;
import gov.vha.isaac.ochre.api.chronicle.StampedVersion;
import gov.vha.isaac.ochre.api.commit.ChronologyChangeListener;
import gov.vha.isaac.ochre.api.commit.CommitRecord;
import gov.vha.isaac.ochre.api.component.concept.ConceptChronology;
import gov.vha.isaac.ochre.api.component.concept.ConceptSnapshot;
import gov.vha.isaac.ochre.api.component.sememe.SememeChronology;
import gov.vha.isaac.ochre.api.component.sememe.version.DescriptionSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.SememeVersion;
import gov.vha.isaac.ochre.api.coordinate.LanguageCoordinate;
import gov.vha.isaac.ochre.api.coordinate.PremiseType;
import gov.vha.isaac.ochre.api.coordinate.StampCoordinate;
import gov.vha.isaac.ochre.api.coordinate.StampPosition;
import gov.vha.isaac.ochre.api.coordinate.StampPrecedence;
import gov.vha.isaac.ochre.api.coordinate.TaxonomyCoordinate;
import gov.vha.isaac.ochre.collections.ConceptSequenceSet;
import gov.vha.isaac.ochre.impl.sememe.DynamicSememeUsageDescription;
import gov.vha.isaac.ochre.impl.utility.Frills;













import gov.vha.isaac.ochre.model.coordinate.StampCoordinateImpl;
import gov.vha.isaac.ochre.model.coordinate.StampPositionImpl;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.function.Function;














import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableColumn.SortType;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.util.Callback;
import javafx.util.StringConverter;














import org.slf4j.Logger;
import org.slf4j.LoggerFactory;














import com.sun.javafx.tk.Toolkit;

public class ConceptViewController {
	
	public static final double MIN_HEIGHT = 150;
	public static final double MIN_WIDTH  = 150;
	
	private static final Logger LOG = LoggerFactory.getLogger(ConceptViewController.class);

	@FXML private ResourceBundle resources;
	@FXML private URL location;
	
	@FXML private AnchorPane	mainPane;
	@FXML private AnchorPane	descriptionsPane;
	@FXML private AnchorPane	detailPane;
	@FXML private AnchorPane	footerPane;
	@FXML private GridPane		headerGridPane;
	@FXML private VBox			relationshipsVBox;
	
	@FXML private TableView<ConceptDescription> descriptionTableView;
	@FXML private TableColumn<ConceptDescription, ConceptDescription>	descriptionTypeTableColumn;
	@FXML private TableColumn<ConceptDescription, ConceptDescription>	acceptabilityTableColumn;
	@FXML private TableColumn<ConceptDescription, ConceptDescription>	significanceTableColumn;
	@FXML private TableColumn<ConceptDescription, ConceptDescription>	dialectTableColumn;
	@FXML private TableColumn<ConceptDescription, StampedItem<?>>			statusTableColumn;
	@FXML private TableColumn<ConceptDescription, ConceptDescription>	descriptionValueTableColumn;
	@FXML private TableColumn<ConceptDescription, ConceptDescription>	sememeTableColumn;
	
    @FXML private TableColumn<ConceptDescription, ?> descriptionTableSTAMPColumn;
	@FXML private TableColumn<ConceptDescription, StampedItem<?>> moduleTableColumn;
	@FXML private TableColumn<ConceptDescription, StampedItem<?>> timeTableColumn;
	@FXML private TableColumn<ConceptDescription, StampedItem<?>> authorTableColumn;
	@FXML private TableColumn<ConceptDescription, StampedItem<?>> pathTableColumn;

	@FXML private Label conceptCodeLabel;
	@FXML private Label conceptLabel;

	@FXML private ComboBox<State> statusComboBox;
	@FXML private ComboBox<Integer> modulesComboBox;
	@FXML private VBox uuidsVBox;
	
	@FXML private Button minusDescriptionButton;
	@FXML private Button plusDescriptionButton;
	@FXML private Button duplicateDescriptionButton;
	@FXML private Button panelPreferencesPopupButton;
	@FXML private ToggleButton panelVsGlobalPreferencesToggleButton;

	
	@FXML private ToggleButton activeOnlyToggle;
	@FXML private ToggleButton stampToggle;

	@FXML private Button cancelButton;
	@FXML private Button commitButton;

	private ObjectProperty<ConceptSnapshot> conceptProperty = new SimpleObjectProperty<ConceptSnapshot>();
	private UpdateableBooleanBinding refreshBinding;
	
	private ObjectProperty<ChronologyChangeListener> conceptChronologyChangeListenerProperty = new SimpleObjectProperty<ChronologyChangeListener>();
	private ObjectProperty<ConceptChronology<? extends StampedVersion>> conceptChronologyProperty = new SimpleObjectProperty<ConceptChronology<? extends StampedVersion>>();
	private ObjectProperty<CommitRecord> conceptCommitRecordProperty = new SimpleObjectProperty<CommitRecord>();

	// TODO should modules be displayed and selectable that are off path of panel coordinates?
	//private ReadOnlyObjectProperty<StampCoordinate> panelStampCoordinateProperty;
	//private ReadOnlyObjectProperty<LanguageCoordinate> panelLanguageCoordinateProperty;
	
	// All bindings and listeners for constituent coordinates should be on change of TaxonomyCoordinate
	private ObjectProperty<TaxonomyCoordinate> panelTaxonomyCoordinate = new SimpleObjectProperty<TaxonomyCoordinate>();

	private LogicalExpressionTreeGraphEmbeddableViewI relationshipsView;
	
	private PreferencesViewI panelPreferencesView = null;
	
	@FXML
	void initialize() {
		assert detailPane 					!= null : "fx:id=\"detailPane\" was not injected: check your FXML file 'ConceptView.fxml'.";
		assert descriptionTypeTableColumn 	!= null : "fx:id=\"descriptionTypeTableColumn\" was not injected: check your FXML file 'ConceptView.fxml'.";
		assert acceptabilityTableColumn 	!= null : "fx:id=\"acceptabilityTableColumn\" was not injected: check your FXML file 'ConceptView.fxml'.";
		assert significanceTableColumn 		!= null : "fx:id=\"significanceTableColumn\" was not injected: check your FXML file 'ConceptView.fxml'.";
		assert footerPane 					!= null : "fx:id=\"footerPane\" was not injected: check your FXML file 'ConceptView.fxml'.";
		assert dialectTableColumn 			!= null : "fx:id=\"dialectTableColumn\" was not injected: check your FXML file 'ConceptView.fxml'.";
		assert statusTableColumn 			!= null : "fx:id=\"statusTableColumn\" was not injected: check your FXML file 'ConceptView.fxml'.";
		assert descriptionTableView 		!= null : "fx:id=\"descriptionTableView\" was not injected: check your FXML file 'ConceptView.fxml'.";
		assert descriptionValueTableColumn 	!= null : "fx:id=\"descriptionValueTableColumn\" was not injected: check your FXML file 'ConceptView.fxml'.";
		assert sememeTableColumn			!= null : "fx:id=\"sememeTableColumn\" was not injected: check your FXML file 'ConceptView.fxml'.";
		
		assert descriptionTableSTAMPColumn 	!= null : "fx:id=\"descriptionTableSTAMPColumn\" was not injected: check your FXML file 'ConceptView.fxml'.";
		assert moduleTableColumn 			!= null : "fx:id=\"moduleTableColumn\" was not injected: check your FXML file 'ConceptView.fxml'.";
		assert timeTableColumn 				!= null : "fx:id=\"timeTableColumn\" was not injected: check your FXML file 'ConceptView.fxml'.";
		assert authorTableColumn 			!= null : "fx:id=\"authorTableColumn\" was not injected: check your FXML file 'ConceptView.fxml'.";
		assert pathTableColumn 				!= null : "fx:id=\"pathTableColumn\" was not injected: check your FXML file 'ConceptView.fxml'.";
		
		assert mainPane 					!= null : "fx:id=\"mainPane\" was not injected: check your FXML file 'ConceptView.fxml'.";
		assert descriptionsPane 			!= null : "fx:id=\"descriptionsPane\" was not injected: check your FXML file 'ConceptView.fxml'.";
		assert headerGridPane 				!= null : "fx:id=\"headerGridPane\" was not injected: check your FXML file 'ConceptView.fxml'.";
		assert relationshipsVBox			!= null : "fx:id=\"relationshipsVBox\" was not injected: check your FXML file 'ConceptView.fxml'.";
		
		assert conceptCodeLabel 			!= null : "fx:id=\"conceptCodeLabel\" was not injected: check your FXML file 'ConceptView.fxml'.";
		assert conceptLabel 			!= null : "fx:id=\"conceptLabel\" was not injected: check your FXML file 'ConceptView.fxml'.";

		assert statusComboBox 			!= null : "fx:id=\"statusComboBox\" was not injected: check your FXML file 'ConceptView.fxml'.";
		assert modulesComboBox 			!= null : "fx:id=\"modulesComboBox\" was not injected: check your FXML file 'ConceptView.fxml'.";
		assert uuidsVBox 			!= null : "fx:id=\"uuidsVBox\" was not injected: check your FXML file 'ConceptView.fxml'.";

		assert minusDescriptionButton 		!= null : "fx:id=\"minusDescriptionButton\" was not injected: check your FXML file 'ConceptView.fxml'.";
		assert duplicateDescriptionButton 	!= null : "fx:id=\"editDescriptionButton\" was not injected: check your FXML file 'ConceptView.fxml'.";
		assert plusDescriptionButton 		!= null : "fx:id=\"plusDescriptionButton\" was not injected: check your FXML file 'ConceptView.fxml'.";
		assert activeOnlyToggle 			!= null : "fx:id=\"activeOnlyToggle\" was not injected: check your FXML file 'ConceptView.fxml'.";
		assert stampToggle 					!= null : "fx:id=\"stampToggleToggle\" was not injected: check your FXML file 'ConceptView.fxml'.";
		assert panelPreferencesPopupButton 			!= null : "fx:id=\"panelPreferencesPopupButton\" was not injected: check your FXML file 'ConceptView.fxml'.";
		assert panelVsGlobalPreferencesToggleButton 	!= null : "fx:id=\"panelVsGlobalPreferencesToggleButton\" was not injected: check your FXML file 'ConceptView.fxml'.";
		
		assert cancelButton 	!= null : "fx:id=\"cancelButton\" was not injected: check your FXML file 'ConceptView.fxml'.";
		assert commitButton 	!= null : "fx:id=\"commitButton\" was not injected: check your FXML file 'ConceptView.fxml'.";

		FxUtils.assignImageToButton(activeOnlyToggle, 		Images.FILTER_16.createImageView(), "Show Active Only / Show All");
		FxUtils.assignImageToButton(stampToggle, 			Images.STAMP.createImageView(), 	"Show/Hide STAMP Columns");
		FxUtils.assignImageToButton(plusDescriptionButton, 	Images.PLUS.createImageView(), 		"Create Description");
		FxUtils.assignImageToButton(minusDescriptionButton, 	Images.MINUS.createImageView(), 	"Retire/Unretire Description");
		FxUtils.assignImageToButton(duplicateDescriptionButton, 	Images.EDIT.createImageView(), 		"Edit Description");
		FxUtils.assignImageToButton(panelPreferencesPopupButton, 	Images.CONFIGURE.createImageView(), 		"Panel Preferences");
		FxUtils.assignImageToButton(panelVsGlobalPreferencesToggleButton, 	Images.CONFIGURE.createImageView(), "Use Local Preferences");

		setColumnWidths();
		setupPreferences();
		setupColumnTypes();
		setupDescriptionTable();
		setupRelationshipsView();
		setupStatusComboBox();
		setupModulesComboBox();
		setupConceptCodeLabel();
		setupUuidsVBox();
		setupActiveOnlyToggle();
		setupStampToggle();
		setupCancelButton();
		setupCommitButton();
		setupConceptLabel();
		setupPanelPreferencesPopupButton();
		setupPanelVsGlobalPreferencesToggleButton();
		
		setupConceptChronologyChangeListener();
		
		descriptionTableView.setPlaceholder(new Label("There are no Descriptions for the selected Concept."));

		// This binding refreshes whenever its bindings change
		refreshBinding = new UpdateableBooleanBinding() {
			private volatile boolean enabled = false;
            {
                setComputeOnInvalidate(true);
                addBinding(
                		conceptProperty,
                		activeOnlyToggle.selectedProperty(),
                		panelTaxonomyCoordinate,
                		stampToggle.selectedProperty(),
                		conceptChronologyProperty,
                		conceptCommitRecordProperty
                		);
                		//modulesComboBox.getSelectionModel().selectedItemProperty());
                enabled = true;
            }

            @Override
            protected boolean computeValue()
            {
                if (!enabled)
                {
                    LOG.debug("Skip initial spurious refresh calls");
                    return false;
                }
                LOG.debug("Refreshing ConceptView due to change in updateableBooleanBinding");

				refresh();

				return false;
            }
		};
		//updateableBooleanBinding.addBinding(conceptNode.getConceptProperty(), activeOnlyToggle.selectedProperty());
	}

	public AnchorPane getRoot()	{
		return mainPane;
	}

	public StringProperty getTitle() {
		return new SimpleStringProperty("Concept Viewer");
	}
	
	private void setupCancelButton() {
		cancelButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				// TODO setupCancelButton()
				LOG.debug("Cancel button pressed");
			}
		});
	}
	private void setupCommitButton() {
		commitButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				// TODO setupCommitButton()
				LOG.debug("Commit button pressed");
			}
		});
	}

	private void setupPreferences() {
		// Effectively clone the TaxonomyCoordinate from UserProfileBindings
		panelTaxonomyCoordinate.set(AppContext.getService(UserProfileBindings.class).getTaxonomyCoordinate().get().makeAnalog(AppContext.getService(UserProfileBindings.class).getTaxonomyCoordinate().get().getTaxonomyType()));
	}

	private void setupPanelVsGlobalPreferencesToggleButton() {
		final Tooltip selectedTooltip = new Tooltip("Use local preferences only (on)");
		final Tooltip unselectedTooltip = new Tooltip("Use local preferences only (off)");
		panelVsGlobalPreferencesToggleButton.selectedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(
					ObservableValue<? extends Boolean> observable,
					Boolean oldValue, Boolean newValue) {
				if (newValue) {
					panelTaxonomyCoordinate.unbind();
					panelVsGlobalPreferencesToggleButton.setTooltip(selectedTooltip);
				} else {
					panelTaxonomyCoordinate.bind(AppContext.getService(UserProfileBindings.class).getTaxonomyCoordinate());
					panelVsGlobalPreferencesToggleButton.setTooltip(unselectedTooltip);
				}
			}
		});
		
		panelVsGlobalPreferencesToggleButton.setSelected(false);
	}

	private void setupPanelPreferencesPopupButton() {
		panelPreferencesPopupButton.setOnAction((event) -> {
			if (panelPreferencesView == null) {
				panelPreferencesView = AppContext.getService(PreferencesViewI.class);
				panelPreferencesView.setRequestedPlugins(SharedServiceNames.VIEW_COORDINATE_PREFERENCES_PLUGIN);
				panelPreferencesView.loadPlugins();
				final ViewCoordinatePreferencesPluginViewI vcPrefPluginView = (ViewCoordinatePreferencesPluginViewI)panelPreferencesView.getPlugin(SharedServiceNames.VIEW_COORDINATE_PREFERENCES_PLUGIN);

				PreferencesPersistenceI panelPersistenceInterface = new PreferencesPersistenceI() {
					@Override
					public UUID getPath() {
						// TODO Auto-generated method stub
						Optional<UUID> opt = Get.identifierService().getUuidPrimordialFromConceptSequence(panelTaxonomyCoordinate.get().getStampCoordinate().getStampPosition().getStampPathSequence());
						return opt.get();
					}

					@Override
					public PremiseType getStatedInferredOption() {
						return panelTaxonomyCoordinate.get().getTaxonomyType();
					}

					@Override
					public Long getTime() {
						return panelTaxonomyCoordinate.get().getStampCoordinate().getStampPosition().getTime();
					}

					@Override
					public Set<State> getStatuses() {
						return Collections.unmodifiableSet(panelTaxonomyCoordinate.get().getStampCoordinate().getAllowedStates());
					}

					@Override
					public Set<UUID> getModules() {
						ConceptSequenceSet seqences = panelTaxonomyCoordinate.get().getStampCoordinate().getModuleSequences();

						Set<UUID> moduleUuids = new HashSet<>();
						for (int sequence : seqences.asArray()) {
							Optional<UUID> opt = Get.identifierService().getUuidPrimordialFromConceptSequence(sequence);
							if (opt.isPresent()) {
								moduleUuids.add(opt.get());
							}
						}

						return Collections.unmodifiableSet(moduleUuids);
					}

					@Override
					public void save() throws IOException {

						StampPosition stampPosition = new StampPositionImpl(
								vcPrefPluginView.getCurrentTime(),
								Get.identifierService().getConceptSequenceForUuids(vcPrefPluginView.getCurrentPath()));

						int[] moduleSequences = new int[vcPrefPluginView.getCurrentSelectedModules().size()];
						int index = 0;
						for (UUID moduleUuid : vcPrefPluginView.getCurrentSelectedModules()) {
							if (moduleUuid != null) {
								moduleSequences[index++] = Get.identifierService().getConceptSequenceForUuids(moduleUuid);
							}
						}

						EnumSet<State> allowedStates = EnumSet.allOf(State.class);
						allowedStates.clear();
						for (State status : vcPrefPluginView.getCurrentStatuses()) {
							allowedStates.add(status);
						}								
						StampCoordinate	stampCoordinate =
								new StampCoordinateImpl(
										StampPrecedence.PATH,
										stampPosition, 
										ConceptSequenceSet.of(moduleSequences), allowedStates);

						TaxonomyCoordinate newCoordinate = null;
						switch (vcPrefPluginView.getCurrentStatedInferredOption()) {
						case STATED:
							newCoordinate = TaxonomyCoordinates.getStatedTaxonomyCoordinate(stampCoordinate, panelTaxonomyCoordinate.get().getLanguageCoordinate());
							break;
						case INFERRED:
							newCoordinate = TaxonomyCoordinates.getInferredTaxonomyCoordinate(stampCoordinate, panelTaxonomyCoordinate.get().getLanguageCoordinate());
							break;
						default:
							throw new RuntimeException("Unsupported StatedInferredOptions value " + vcPrefPluginView.getCurrentStatedInferredOption() + ".  Expected STATED or INFERRED.");
						}

						panelTaxonomyCoordinate.set(newCoordinate);
					}
				};

				vcPrefPluginView.setPersistenceInterface(panelPersistenceInterface);
			}

			panelVsGlobalPreferencesToggleButton.setSelected(true);

			String title = "ConceptView Panel Preferences";
			panelPreferencesView.setTitle("View/Edit " + title);
			panelPreferencesView.setPanelTitle(title);
			panelPreferencesView.showView(mainPane.getScene().getWindow());
		});
	}

	private void setupRelationshipsView() {
		// TODO Make this work
		relationshipsView = AppContext.getService(LogicalExpressionTreeGraphEmbeddableViewI.class);
		relationshipsVBox.getChildren().add(relationshipsView.getView());
	}
	
	void setColumnWidths() {
		/*
		descriptionTypeTableColumn.setPrefWidth(	descriptionTableView.getWidth() * 0.15);
		acceptabilityTableColumn.setPrefWidth(		descriptionTableView.getWidth() * 0.15);
		descriptionValueTableColumn.setPrefWidth(	descriptionTableView.getWidth() * 0.30);
		dialectTableColumn.setPrefWidth(			descriptionTableView.getWidth() * 0.15);
		significanceTableColumn.setPrefWidth(		descriptionTableView.getWidth() * 0.15);
		statusTableColumn.setPrefWidth(				descriptionTableView.getWidth() * 0.09);
		*/
		// TODO This bind is not exactly what we want.  DT
		descriptionTypeTableColumn.prefWidthProperty().bind(	descriptionTableView.widthProperty().multiply(0.15));
		acceptabilityTableColumn.prefWidthProperty().bind(		descriptionTableView.widthProperty().multiply(0.15));
		descriptionValueTableColumn.prefWidthProperty().bind(	descriptionTableView.widthProperty().multiply(0.30));
		dialectTableColumn.prefWidthProperty().bind(			descriptionTableView.widthProperty().multiply(0.15));
		significanceTableColumn.prefWidthProperty().bind(		descriptionTableView.widthProperty().multiply(0.15));
		statusTableColumn.prefWidthProperty().bind(				descriptionTableView.widthProperty().multiply(0.09));
	}
	
	private void setupColumnTypes() {
		descriptionTypeTableColumn.setUserData(ConceptViewColumnType.TYPE);
		acceptabilityTableColumn.setUserData(ConceptViewColumnType.ACCEPTABILITY);
		significanceTableColumn.setUserData(ConceptViewColumnType.SIGNIFICANCE);
		dialectTableColumn.setUserData(ConceptViewColumnType.LANGUAGE);
		statusTableColumn.setUserData(ConceptViewColumnType.STAMP_STATE);
		descriptionValueTableColumn.setUserData(ConceptViewColumnType.TERM);
		sememeTableColumn.setUserData(ConceptViewColumnType.SEMEMES);
		
		descriptionTableSTAMPColumn.setUserData(ConceptViewColumnType.STAMP_HEADING);
		moduleTableColumn.setUserData(ConceptViewColumnType.STAMP_MODULE);
		timeTableColumn.setUserData(ConceptViewColumnType.STAMP_TIME);
		authorTableColumn.setUserData(ConceptViewColumnType.STAMP_AUTHOR);
		pathTableColumn.setUserData(ConceptViewColumnType.STAMP_PATH);
	}

	public ConceptSnapshot getConceptSnapshot() {
		return (conceptProperty.get() != null) ? conceptProperty.get() : null;
	}
	public void setConcept(int conceptId) {
		setConcept(conceptId, panelTaxonomyCoordinate.get().getStampCoordinate(), panelTaxonomyCoordinate.get().getLanguageCoordinate());
	}
	public void setConcept(int conceptId, StampCoordinate stampCoordinate, LanguageCoordinate languageCoordinate) {
		Task<ConceptSnapshot> task = new Task<ConceptSnapshot>() {
			@Override
			protected ConceptSnapshot call() throws Exception {
				return OchreUtility.getConceptSnapshot(conceptId, stampCoordinate != null ? stampCoordinate : panelTaxonomyCoordinate.get().getStampCoordinate(), languageCoordinate != null ? languageCoordinate : panelTaxonomyCoordinate.get().getLanguageCoordinate()).get();
			}

			@Override
			protected void succeeded() {
				Platform.runLater(() -> conceptProperty.set(getValue()));
			}

			@Override
			protected void failed() {
				String title = "Failed retrieving ConceptSnapshot";
				LOG.error(title, getException());
				AppContext.getCommonDialogs().showErrorDialog(title, "There was an unexpected error retrieving ConceptSnapshot (nid=" + conceptId + ")", getException().toString());
			}
		};
		
		Utility.execute(task);
	}
	public void setConcept(UUID conceptUuid) {
		Utility.execute(() -> {
			ConceptChronology<?> concept = Get.conceptService().getConcept(conceptUuid);
			setConcept(concept.getConceptSequence());
		});
	}
	
	private void refresh() {
		refreshConceptDescriptions();
		refreshLogicGraph();
	}

	void viewDiscarded() {
		refreshBinding.clearBindings();
		panelTaxonomyCoordinate.unbind();
		relationshipsView.viewDiscarded();
		removeConceptChronologyChangeListener();
	}

	private void setupConceptChronologyChangeListener() {
		conceptProperty.addListener(new ChangeListener<ConceptSnapshot>() {
			@Override
			public void changed(
					ObservableValue<? extends ConceptSnapshot> observable,
					ConceptSnapshot oldValue, ConceptSnapshot newValue) {
				removeConceptChronologyChangeListener();
				
				addConceptChronologyChangeListener(newValue);
			}
		});
	}
	private void removeConceptChronologyChangeListener() {
		if (conceptChronologyChangeListenerProperty.get() != null) {
			Get.commitService().removeChangeListener(conceptChronologyChangeListenerProperty.get());
		}
		conceptChronologyProperty.set(null);
		conceptCommitRecordProperty.set(null);
	}
	private void addConceptChronologyChangeListener(ConceptSnapshot newValue) {
		if (newValue != null) {
			final UUID listenerUuid = UUID.randomUUID();
			ChronologyChangeListener listener = new ChronologyChangeListener() {
				@Override
				public UUID getListenerUuid() {
					return listenerUuid;
				}

				@Override
				public void handleChange(ConceptChronology<? extends StampedVersion> cc) {
					LOG.debug("Handling chronology change for concept \"{}\" (nid={}, uuid={}) by resetting conceptChronologyProperty", Get.conceptDescriptionText(cc.getNid()), cc.getNid(), cc.getPrimordialUuid());
					conceptChronologyProperty.set(cc);
				}

				@Override
				public void handleChange(SememeChronology<? extends SememeVersion<?>> sc) {
					// TODO Auto-generated method stub
				}

				@Override
				public void handleCommit(CommitRecord commitRecord) {
					LOG.debug("Handling CommitRecord for concept \"{}\" (nid={}, uuid={}) by resetting conceptCommitRecordProperty: CommitRecord: {}", Get.conceptDescriptionText(newValue.getNid()), newValue.getNid(), newValue.getPrimordialUuid(), commitRecord);
					conceptCommitRecordProperty.set(commitRecord);
				}
			};

			conceptChronologyChangeListenerProperty.set(listener);
			Get.commitService().addChangeListener(listener);
		}
	}
	
	private void setupConceptLabel() {
		conceptLabel.setPadding(new Insets(10,0,10,10));
		conceptProperty.addListener(new ChangeListener<ConceptSnapshot>() {
			@Override
			public void changed(
					ObservableValue<? extends ConceptSnapshot> observable,
					ConceptSnapshot oldValue, ConceptSnapshot newValue) {
				if (newValue != null) {
					Task<String> task = new Task<String>() {
						@Override
						protected String call() throws Exception {
							Optional<LatestVersion<DescriptionSememe<?>>> desc = newValue.getLanguageCoordinate().getFullySpecifiedDescription(newValue.getChronology().getConceptDescriptionList(), newValue.getStampCoordinate());
							return desc.isPresent() ? desc.get().value().getText() : null;
						}
						@Override
						public void succeeded() {
							Platform.runLater(() -> {
								conceptLabel.setText(getValue());
								conceptLabel.setContextMenu(null);
								if (conceptLabel.getText() != null) {
									conceptLabel.setContextMenu(new ContextMenu());
									CommonMenuBuilderI builder = CommonMenuBuilder.newInstance();
									builder.setMenuItemsToExclude(
											CommonMenuItem.LOGIC_GRAPH_VIEW,
											CommonMenuItem.CONCEPT_DIAGRAM_VIEW,
											CommonMenuItem.USCRS_REQUEST_VIEW,
											CommonMenuItem.LOINC_REQUEST_VIEW);
									CommonMenus.addCommonMenus(conceptLabel.getContextMenu(),
											builder,
											new CommonMenusDataProvider() {
										@Override
										public String[] getStrings() {
											return conceptProperty.get() == null ? new String[0] : new String[] { conceptLabel.getText() };
										}
									},
									new CommonMenusNIdProvider() {
										@Override
										public Collection<Integer> getNIds() {
											return conceptProperty.get() == null ? new ArrayList<Integer>() : Arrays.asList(new Integer[] { conceptProperty.get().getChronology().getNid() });
										}
									});
									
									// test display ids
									{
										MenuItem mi = new MenuItem("Display IDs");
										mi.setOnAction(new EventHandler<ActionEvent>() {
											@Override
											public void handle(ActionEvent arg0) {
												PopupHelper.showConceptIdList(conceptProperty.getValue(), conceptLabel);
											}
										});
										conceptLabel.getContextMenu().getItems().add(mi);
									}

									{
										MenuItem miHistory = new MenuItem("Concept History");
										miHistory.setOnAction(new EventHandler<ActionEvent>() {
											@Override
											public void handle(ActionEvent arg0) {
												PopupHelper.showConceptHistory(conceptProperty.getValue(), conceptLabel);
											}
										});
										conceptLabel.getContextMenu().getItems().add(miHistory);
									}
									
									{
										MenuItem mi = new MenuItem("Copy Details");
										mi.visibleProperty().bind(conceptProperty.isNotNull());
										mi.setOnAction(new EventHandler<ActionEvent>() {
											@Override
											public void handle(ActionEvent arg0) {
												CustomClipboard.set(conceptProperty.get().toUserString());
											}
										});
										conceptLabel.getContextMenu().getItems().add(mi);
									}
								}
							});
						}
						@Override
						public void failed() {
							LOG.error("Failed setting concept label on conceptProperty change", getException());
						}
					};

					Utility.execute(task);
				} else {
					conceptLabel.setText("Drop a concept here");
				}
			}
		});
		AppContext.getService(DragRegistry.class).setupDragAndDrop(
				conceptLabel,
				new SingleConceptIdProvider()
				{
					@Override
					public String getConceptId()
					{
						return conceptProperty.getValue().getNid() + "";
					}
				},
				true,
				new Function<String, String>() {
					@Override
					public String apply(String t) {
						int nid = 0;
						if (t != null) {
							try {
								nid = Integer.parseInt(t);
							} catch (Exception e1) {
								LOG.debug("Dropped non-integer text value \"{}\" on concept label", t);

								try {
									UUID uuid = UUID.fromString(t);

									nid = Get.identifierService().getNidForUuids(uuid);
								} catch (Exception e2) {
									LOG.warn("Dropped non-integer, non-uuid text value \"{}\" on concept label", t);
								}
							}
						}

						LOG.debug("Text \"{}\" dropped on concept label resulting in use of concept nid={}", t, nid);

						if (nid == 0) {
							Platform.runLater(() -> conceptProperty.set(null));
							return null;
						} else {
							Optional<ConceptSnapshot> cs = OchreUtility.getConceptSnapshot(nid, panelTaxonomyCoordinate.get().getStampCoordinate(), Get.configurationService().getDefaultLanguageCoordinate());
							
							Platform.runLater(() -> conceptProperty.set(cs.get()));
							Optional<LatestVersion<DescriptionSememe<?>>> desc = cs.get().getLanguageCoordinate().getFullySpecifiedDescription(cs.get().getChronology().getConceptDescriptionList(), cs.get().getStampCoordinate());
							return desc.isPresent() ? desc.get().value().getText() : null;
						}
					}
				});
	}
	
	private void setupActiveOnlyToggle() {
		activeOnlyToggle.setSelected(false);
	}
	private void setupStampToggle() {
		stampToggle.setSelected(false);

		descriptionTableSTAMPColumn.visibleProperty().set(stampToggle.selectedProperty().get());
		descriptionTableSTAMPColumn.visibleProperty().bind(stampToggle.selectedProperty());
	}

	private void setupConceptCodeLabel() {
		
		conceptProperty.addListener(new ChangeListener<ConceptSnapshot>() {
			@Override
			public void changed(
					ObservableValue<? extends ConceptSnapshot> observable,
					ConceptSnapshot oldValue, ConceptSnapshot newValue) {
				Platform.runLater(() -> loadConceptCodeFromConcept(newValue));
			}
		});
		loadConceptCodeFromConcept(conceptProperty.get());
	} 
	private void loadConceptCodeFromConcept(ConceptSnapshot concept) {
		conceptCodeLabel.setText(null);
		if (concept != null) {
			Task<Optional<Long>> task = new Task<Optional<Long>>() {
				@Override
				protected Optional<Long> call() throws Exception {
					return Frills.getSctId(concept.getChronology().getNid(), concept.getStampCoordinate());
				}

				@Override
				protected void succeeded() {
					conceptCodeLabel.setText(getValue().isPresent() ? getValue().get().toString() : null);
					conceptCodeLabel.setContextMenu(null);
					if (conceptCodeLabel.getText() != null) {
						conceptCodeLabel.setContextMenu(new ContextMenu());
						CommonMenus.addCommonMenus(conceptCodeLabel.getContextMenu(),
								new CommonMenusDataProvider() {
							@Override
							public String[] getStrings() {
								return (conceptCodeLabel.getText() == null  || conceptCodeLabel.getText().length() == 0) ? new String[0] : new String[] { conceptCodeLabel.getText() };
							}
						});
					}
				}
			};

			Utility.execute(task);
		}
	}
	
	private void setupUuidsVBox() {
		conceptProperty.addListener(new ChangeListener<ConceptSnapshot>() {
			@Override
			public void changed(
					ObservableValue<? extends ConceptSnapshot> observable,
					ConceptSnapshot oldValue, ConceptSnapshot newValue) {
				Platform.runLater(() -> loadUuidsVBoxFromConcept(newValue));
			}
		});
		loadUuidsVBoxFromConcept(conceptProperty.get());
	}
	private void loadUuidsVBoxFromConcept(ConceptSnapshot concept) {
		uuidsVBox.getChildren().clear();
		if (concept != null) {
			for (final UUID uuid : concept.getUuidList()) {
				Label label = new Label(uuid.toString());
				label.setContextMenu(new ContextMenu());
				CommonMenus.addCommonMenus(label.getContextMenu(),
						new CommonMenusDataProvider() {
					@Override
					public String[] getStrings() {
						return new String[] {uuid.toString()};
					}
				});
				uuidsVBox.getChildren().add(label);
			}
		}
	}

	private void setupStatusComboBox() {
		statusComboBox.setCellFactory((param) -> {
			final ListCell<State> cell = new ListCell<State>() {
				@Override
				protected void updateItem(State c, boolean emptyRow) {
					super.updateItem(c, emptyRow);
					if(c == null) {
						setText(null);
					} else {
						setText(c.name());
					}
				}
			};
			return cell;
		});
		statusComboBox.setButtonCell(new ListCell<State>() {
			@Override
			protected void updateItem(State c, boolean emptyRow) {
				super.updateItem(c, emptyRow); 
				if (emptyRow) {
					setText("");
				} else {
					setText(c.name());
				}
			}
		});
		statusComboBox.setConverter(new StringConverter<State>() {
			@Override
			public String toString(State state) {
				if (state == null){
					return null;
				} else {
					return state.name();
				}
			}

			@Override
			public State fromString(String state) {
				return State.valueOf(state);
			}
		});
		conceptProperty.addListener(new ChangeListener<ConceptSnapshot>() {
			@Override
			public void changed(
					ObservableValue<? extends ConceptSnapshot> observable,
					ConceptSnapshot oldValue, ConceptSnapshot newValue) {
				loadStatusComboBoxFromConcept(newValue);
			}
		});
		
		loadStatusComboBoxFromConcept(conceptProperty.get());
	}
	// In read-only view, set contents/choices of statusComboBox
	// to state of loaded property only
	private void loadStatusComboBoxFromConcept(ConceptSnapshot concept) {
		statusComboBox.getItems().clear();
		if (concept != null) {
			statusComboBox.getItems().add(concept.getState());
			statusComboBox.getSelectionModel().clearAndSelect(0);
		} else {
			statusComboBox.buttonCellProperty().set(null);
		}
	}
	
	private void setupModulesComboBox() {
		modulesComboBox.setCellFactory((param) -> {
			final ListCell<Integer> cell = new ListCell<Integer>() {
				@Override
				protected void updateItem(Integer c, boolean emptyRow) {
					super.updateItem(c, emptyRow);
					if(c == null) {
						setText(null);
					} else {
						Task<String> task = new Task<String>() {
							@Override
							protected String call() throws Exception {
								return Get.conceptDescriptionText(c);
							}

							@Override
							protected void succeeded() {
								Platform.runLater(() -> setText(getValue()));
							}
						};
						
						Utility.execute(task);
					}
				}
			};
			return cell;
		});
		modulesComboBox.setButtonCell(new ListCell<Integer>() {
			@Override
			protected void updateItem(Integer c, boolean emptyRow) {
				super.updateItem(c, emptyRow); 
				if (emptyRow) {
					setText("");
				} else {
					Task<String> task = new Task<String>() {
						@Override
						protected String call() throws Exception {
							return Get.conceptDescriptionText(c);
						}

						@Override
						protected void succeeded() {
							Platform.runLater(() -> setText(getValue()));
						}
					};
					
					Utility.execute(task);
				}
			}
		});
		modulesComboBox.setConverter(new StringConverter<Integer>() {
			@Override
			public String toString(Integer moduleSequence) {
				if (moduleSequence == null){
					return null;
				} else {
					return Get.conceptDescriptionText(moduleSequence);
				}
			}

			@Override
			public Integer fromString(String module) {
				return null;
			}
		});
		conceptProperty.addListener(new ChangeListener<ConceptSnapshot>() {
			@Override
			public void changed(
					ObservableValue<? extends ConceptSnapshot> observable,
					ConceptSnapshot oldValue, ConceptSnapshot newValue) {
				loadModulesComboBoxFromConcept(newValue);
			}
		});
		
		loadModulesComboBoxFromConcept(conceptProperty.get());
	}
	private final ChangeListener<Integer> modulesComboBoxSelectedItemChangeListener = new ChangeListener<Integer>() {
		@Override
		public void changed(ObservableValue<? extends Integer> observable, Integer oldValue, Integer newValue) {
			LOG.debug("modulesComboBoxSelectedItemChangeListener: selected item changed from {} to {}", oldValue, newValue);
			// Always use latest stamp coordinate with module specified
			if (oldValue != newValue) {
				StampCoordinate stampCoordinate = Frills.makeStampCoordinateAnalogVaryingByModulesOnly(StampCoordinates.getDevelopmentLatest(), newValue);
				setConcept(conceptProperty.get().getChronology().getConceptSequence(), stampCoordinate, panelTaxonomyCoordinate.get().getLanguageCoordinate());
			}
		}
	};
	// In read-only view, set contents/choices of moduleComboBox
	// to module of loaded concepts only
	private void loadModulesComboBoxFromConcept(ConceptSnapshot concept) {
		// Must remove listener or will infinitely loop
		modulesComboBox.getSelectionModel().selectedItemProperty().removeListener(modulesComboBoxSelectedItemChangeListener);
		modulesComboBox.getItems().clear();
		if (concept != null) {
			modulesComboBox.getItems().addAll(Frills.getAllModuleSequences(concept.getChronology()));
			modulesComboBox.getSelectionModel().clearSelection();
			// NOTE: Must use Integer.valueOf() otherwise argument interpreted as index instead of sequence
			modulesComboBox.getSelectionModel().select(Integer.valueOf(concept.getModuleSequence()));
		} else {
			modulesComboBox.buttonCellProperty().set(null);
		}
		modulesComboBox.getSelectionModel().selectedItemProperty().addListener(modulesComboBoxSelectedItemChangeListener);
	}

	@SuppressWarnings({ "unchecked", "restriction" })
	private void setupDescriptionTable() 
	{
		setDescriptionTableFactories(descriptionTableView.getColumns());

		descriptionTypeTableColumn.setComparator(ConceptDescription.typeComparator);
		acceptabilityTableColumn.setComparator(ConceptDescription.acceptabilityComparator);
		significanceTableColumn.setComparator(ConceptDescription.significanceComparator);
		dialectTableColumn.setComparator(ConceptDescription.languageComparator);
		statusTableColumn.setComparator(StampedItem.statusComparator);
		descriptionValueTableColumn.setComparator(ConceptDescription.valueComparator);
		moduleTableColumn.setComparator(StampedItem.moduleComparator);
		sememeTableColumn.setComparator(ConceptDescription.sememesComparator);
		
		descriptionTypeTableColumn.setSortType(SortType.ASCENDING);
		acceptabilityTableColumn.setSortType(SortType.ASCENDING);
		descriptionValueTableColumn.setSortType(SortType.ASCENDING);
		descriptionTableView.getSortOrder().clear();
		descriptionTableView.getSortOrder().addAll(descriptionTypeTableColumn,
				   acceptabilityTableColumn,
				   descriptionValueTableColumn);
		
		sememeTableColumn.setVisible(false);
		
		// Hack to dynamically set column min widths
		Font f = new Font("System Bold", 13.0);
		for (TableColumn<ConceptDescription,?> column : descriptionTableView.getColumns()) {
			column.setMinWidth(Toolkit.getToolkit().getFontLoader().computeStringWidth(column.getText(), f) + 30);
		}
		
	}
	
	@SuppressWarnings("unchecked")
	private void setDescriptionTableFactories(ObservableList<TableColumn<ConceptDescription,?>> tableColumns)
	{
		for (TableColumn<ConceptDescription, ?> tableColumn : tableColumns) {
			TableColumn<ConceptDescription, ConceptDescription> descriptionTableColumn = (TableColumn<ConceptDescription, ConceptDescription>)tableColumn;
			descriptionTableColumn.setCellValueFactory(descriptionCellValueFactory);
			descriptionTableColumn.setCellFactory(descriptionCellFactory);
			
			ObservableList<TableColumn<ConceptDescription,?>> nestedTableColumns = descriptionTableColumn.getColumns();
			if (nestedTableColumns.size() > 0) {
				setDescriptionTableFactories(nestedTableColumns);
			}
		}
		
	}

	private Callback<TableColumn.CellDataFeatures<ConceptDescription, ConceptDescription>, ObservableValue<ConceptDescription>> descriptionCellValueFactory = 
			new Callback<TableColumn.CellDataFeatures<ConceptDescription, ConceptDescription>, ObservableValue<ConceptDescription>>()	{
		@Override
		public ObservableValue<ConceptDescription> call(CellDataFeatures<ConceptDescription, ConceptDescription> param) {
			return new SimpleObjectProperty<ConceptDescription>(param.getValue());
		}
	};
	
	private Callback<TableColumn<ConceptDescription, ConceptDescription>, TableCell<ConceptDescription, ConceptDescription>> descriptionCellFactory =
			new Callback<TableColumn<ConceptDescription, ConceptDescription>, TableCell<ConceptDescription, ConceptDescription>>() {

		@Override
		public TableCell<ConceptDescription, ConceptDescription> call(TableColumn<ConceptDescription, ConceptDescription> param) {
			return new TableCell<ConceptDescription, ConceptDescription>() {
				@Override
				public void updateItem(final ConceptDescription conceptDescription, boolean empty) {
					super.updateItem(conceptDescription, empty);
					updateCell(this, conceptDescription);
				}
			};
		}
	};

	private void updateCell(TableCell<?, ?> cell, ConceptDescription conceptDescription) {
		if (!cell.isEmpty() && conceptDescription != null) {
			ContextMenu cm = new ContextMenu();
			cell.setContextMenu(cm);
			StringProperty textProperty = null;
			int conceptSequence = 0;
			int conceptNid = 0;
			ConceptViewColumnType columnType = (ConceptViewColumnType) cell.getTableColumn().getUserData();

			cell.setText(null);
			cell.setGraphic(null);
			cell.setTooltip(null);

			switch (columnType) {
			case STATE_CONDENSED:
			{
				StackPane sp = new StackPane();
				sp.setPrefSize(25, 25);
				String tooltipText = conceptDescription.isActive()? "Active" : "Inactive";
				ImageView image    = conceptDescription.isActive()? Images.BLACK_DOT.createImageView() : Images.GREY_DOT.createImageView();
				sizeAndPosition(image, sp, Pos.CENTER);
				cell.setTooltip(new Tooltip(tooltipText));
				cell.setGraphic(sp);
				break;
			}
				
			case TERM:
				textProperty = conceptDescription.getValueProperty();
				//conceptSequence = conceptDescription.getSequence();
				//conceptNid = Get.identifierService().getConceptNid(conceptSequence);
				break;
				
			case TYPE:
				textProperty = conceptDescription.getTypeProperty();
				conceptSequence = conceptDescription.getTypeSequence();
				conceptNid = Get.identifierService().getConceptNid(conceptSequence);
				break;
				
			case LANGUAGE:
				textProperty = conceptDescription.getLanguageProperty();
				conceptSequence = conceptDescription.getLanguageSequence();
				conceptNid = Get.identifierService().getConceptNid(conceptSequence);
				break;
				
			case ACCEPTABILITY:
				textProperty = conceptDescription.getAcceptabilityProperty();
				//conceptSequence = conceptDescription.getAcceptabilitySequence();
				break;
			case SIGNIFICANCE:
				textProperty = conceptDescription.getSignificanceProperty();
				conceptSequence = conceptDescription.getSignificanceSequence();
				conceptNid = Get.identifierService().getConceptNid(conceptSequence);
				break;
			case SEMEMES:
			{
				
				String tooltipText = "";
				StackPane sp = new StackPane();
				sp.setPrefSize(25, 25);

				try
				{
//					if (conceptDescription.getDescriptionSememe().getState() == State.ACTIVE)
//					{
//						sizeAndPosition(Images.BLACK_DOT.createImageView(), sp, Pos.TOP_LEFT);
//						tooltipText += "Active";
//					}
//					else
//					{
//						sizeAndPosition(Images.GREY_DOT.createImageView(), sp, Pos.TOP_LEFT);
//						tooltipText += "Inactive";
//					}
					
//					if (!conceptDescription.getDescriptionSememe().isCurrent())
//					{
//						sizeAndPosition(Images.HISTORICAL.createImageView(), sp, Pos.BOTTOM_LEFT);
//						tooltipText += " and Historical";
//					}
//					else
//					{
//						tooltipText += " and Current";
//					}
					
//					if (conceptDescription.getDescriptionSememe().getTime() == Long.MAX_VALUE)
//					{
//						sizeAndPosition(Images.YELLOW_DOT.createImageView(), sp, Pos.TOP_RIGHT);
//						tooltipText += " - Uncommitted";
//					}
					//if (Frills.hasNestedSememe(conceptDescription.getDescriptionSememe().getChronology()))
					if (conceptDescription.hasSememes())
					{
						//I can't seem to get just and image view to pick up mouse clicks
						//but it works in a button... sigh.
						tooltipText = tooltipText.length() == 0 ? "has sememes" : "and has sememes";
						
						Button b = new Button();
						b.setPadding(new Insets(0));
						b.setPrefHeight(12.0);
						b.setPrefWidth(12.0);
						ImageView iv = Images.ATTACH.createImageView();
						iv.setFitHeight(12.0);
						iv.setFitWidth(12.0);
						b.setGraphic(iv);
						b.setOnAction((event) ->
						{
							SememeViewI drv = AppContext.getService(SememeViewI.class);
							drv.setComponent(conceptDescription.getStampedVersion().getNid(), null, null, null, true);
		
							DetachablePopOverHelper.showDetachachablePopOver(b, DetachablePopOverHelper.newDetachachablePopover("Sememes attached to Description", drv.getView()));
						});
						sizeAndPosition(b, sp, Pos.CENTER);
					}
				}
				catch (Exception e)
				{
					LOG.error("Unexpected", e);
				}
				Node graphic = sp;
				cell.setTooltip(new Tooltip(tooltipText));
				cell.setGraphic(graphic);
				
				//TODO Only want to set the style once per row - note - this breaks if they turn off this row :(
//				if (ref.isCurrent())
//				{
//					getTableRow().getStyleClass().removeAll("historical");
//				}
//				else
//				{
//					if (!getTableRow().getStyleClass().contains("historical"))
//					{
//						getTableRow().getStyleClass().add("historical");
//					}
//				}
				break;
			}
			case STAMP_STATE:
				textProperty = conceptDescription.getStateProperty();
				break;
			case STAMP_TIME:
				textProperty = conceptDescription.getTimeProperty();
				break;
			case STAMP_AUTHOR:
				textProperty = conceptDescription.getAuthorProperty();
				conceptSequence = conceptDescription.getAuthorSequence();
				conceptNid = Get.identifierService().getConceptNid(conceptSequence);
				break;
			case STAMP_MODULE:
				textProperty = conceptDescription.getModuleProperty();
				conceptSequence = conceptDescription.getModuleSequence();
				conceptNid = Get.identifierService().getConceptNid(conceptSequence);
				break;
			case STAMP_PATH:
				textProperty = conceptDescription.getPathProperty();
				conceptSequence = conceptDescription.getPathSequence();
				conceptNid = Get.identifierService().getConceptNid(conceptSequence);
				break;
			default:
				// Nothing
			}
			
			if (textProperty != null) {
				// TODO Make text overrun work on text property
				Text text = new Text();
				text.textProperty().bind(textProperty);
				//text.wrappingWidthProperty().bind(cell.getTableColumn().widthProperty());
				cell.setGraphic(text);
				
				Tooltip tooltip = new Tooltip();
				tooltip.textProperty().bind(textProperty);
				cell.setTooltip(tooltip);
	
				MenuItem mi = new MenuItem("Copy Value");
				mi.setOnAction(new EventHandler<ActionEvent>() {
					@Override
					public void handle(ActionEvent arg0) {
						CustomClipboard.set(((Text)cell.getGraphic()).getText());
					}
				});
				mi.setGraphic(Images.COPY.createImageView());
				cm.getItems().add(mi);

				MenuItem miWrap = new MenuItem("Wrap Text");
				miWrap.setOnAction(new EventHandler<ActionEvent>() {
					@Override
					public void handle(ActionEvent arg0) {
						Text text = (Text)cell.getGraphic();
						if (text.wrappingWidthProperty().isBound()) {
							miWrap.setText("Wrap Text");
							text.wrappingWidthProperty().unbind();
							text.setWrappingWidth(0.0);
							
						} else {
							miWrap.setText("Truncate Text");
							text.wrappingWidthProperty().bind(cell.getTableColumn().widthProperty());
						}
					}
				});
				cm.getItems().add(miWrap);

				MenuItem miIds = new MenuItem("Display IDs");
				miIds.setOnAction(new EventHandler<ActionEvent>() {
					@Override
					public void handle(ActionEvent arg0) {
						PopupHelper.showDescriptionIdList(conceptDescription, cell);
					}
				});
				cm.getItems().add(miIds);

				MenuItem miHistory = new MenuItem("Description History");
				miHistory.setOnAction(new EventHandler<ActionEvent>() {
					@Override
					public void handle(ActionEvent arg0) {
						PopupHelper.showDescriptionHistory(conceptDescription, cell);
					}
				});
				cm.getItems().add(miHistory);
			}
			
			final String textValue = (textProperty != null)? textProperty.get() : null;
			if (conceptNid != 0) {
				final int finalConceptNid = conceptNid;
				final int finalConceptSequence = conceptSequence;
				CommonMenuBuilderI builder = CommonMenuBuilder.newInstance();
				builder.setMenuItemsToExclude(
						CommonMenuItem.COPY,
						CommonMenuItem.COPY_CONTENT,
						CommonMenuItem.COPY_NID,
						CommonMenuItem.COPY_SCTID,
						CommonMenuItem.COPY_UUID,
						CommonMenuItem.LOINC_REQUEST_VIEW,
						CommonMenuItem.USCRS_REQUEST_VIEW,
						CommonMenuItem.SEND_TO);
				CommonMenus.addCommonMenus(cm,
						builder,
						new CommonMenusDataProvider() {
					@Override
					public String[] getStrings() {
						return textValue == null ? new String[0] : new String[] { textValue };
					}
				}, new CommonMenusNIdProvider() {
					@Override
					public Collection<Integer> getNIds() {
						try {
							boolean isDynamicSememe = DynamicSememeUsageDescription.isDynamicSememe(finalConceptNid);
							LOG.debug("Creating common menus for sequence={}, nid={}, desc={}, which {} a dynamic sememe", finalConceptSequence, finalConceptNid, Get.conceptDescriptionText(finalConceptNid), isDynamicSememe ? "is" : "is not");
						} catch (Exception e) {
							//
						}
						return Arrays.asList(new Integer[] { finalConceptNid });
					}
				});
			}
		} else {
			//cell.setText(null);
			cell.setGraphic(null);
			cell.setTooltip(null);
		}
	}

	public static void sizeAndPosition(Node node, StackPane sp, Pos position)
	{
		if (node instanceof ImageView)
		{
			((ImageView)node).setFitHeight(12);
			((ImageView)node).setFitWidth(12);
		}
		Insets insets;
		switch (position) {
		case TOP_LEFT:
			insets = new Insets(0,0,0,0);
			break;
		case TOP_RIGHT:
			insets = new Insets(0,0,0,13);
			break;
		case BOTTOM_LEFT:
			insets = new Insets(13,0,0,0);
			break;
		case BOTTOM_RIGHT:
			insets = new Insets(13,0,0,13);
			break;
		case CENTER:
			insets = new Insets(5,0,0,5);
			break;
		default:
			insets = new Insets(0,0,0,0);
		}
		StackPane.setMargin(node, insets);
		sp.getChildren().add(node);
		StackPane.setAlignment(node, Pos.TOP_LEFT);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void refreshConceptDescriptions()
	{
		TableColumn[] sortColumns = descriptionTableView.getSortOrder().toArray(new TableColumn[0]);
		
		descriptionTableView.getItems().clear();
		descriptionTableView.getSelectionModel().clearSelection();
		descriptionTableView.setPlaceholder(new ProgressIndicator());
		
		if (conceptProperty.get() != null) {
			ObservableList<ConceptDescription> descriptionList =
					ConceptDescription.makeDescriptionList(
							conceptProperty.get().getChronology().getConceptDescriptionList(),
							panelTaxonomyCoordinate.get().getStampCoordinate(),
							activeOnlyToggle.selectedProperty().get());
			descriptionTableView.setItems(descriptionList);
		}
		descriptionTableView.getSortOrder().clear();
		descriptionTableView.getSortOrder().addAll(sortColumns);
	}
	
	private void refreshLogicGraph() {
		if (conceptProperty.get() != null) {
			relationshipsView.setConcept(panelTaxonomyCoordinate.get(), conceptProperty.get().getNid());
		} else {
			relationshipsView.clear();
		}
	}
}
