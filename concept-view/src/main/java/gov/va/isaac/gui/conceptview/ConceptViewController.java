package gov.va.isaac.gui.conceptview;

import gov.va.isaac.gui.ConceptNode;
import gov.va.isaac.gui.conceptview.data.ConceptDescription;
import gov.va.isaac.gui.conceptview.data.StampedItem;
import gov.va.isaac.gui.util.CustomClipboard;
import gov.va.isaac.gui.util.FxUtils;
import gov.va.isaac.gui.util.Images;
import gov.va.isaac.util.CommonMenus;
import gov.va.isaac.util.CommonMenusDataProvider;
import gov.va.isaac.util.CommonMenusNIdProvider;
import gov.va.isaac.util.UpdateableBooleanBinding;
import gov.va.isaac.util.Utility;
import gov.vha.isaac.metadata.coordinates.StampCoordinates;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.State;
import gov.vha.isaac.ochre.api.chronicle.StampedVersion;
import gov.vha.isaac.ochre.api.component.concept.ConceptChronology;
import gov.vha.isaac.ochre.api.component.concept.ConceptSnapshot;
import gov.vha.isaac.ochre.impl.utility.Frills;

import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.UUID;

import javafx.concurrent.Task;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
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
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.util.Callback;
import javafx.util.StringConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConceptViewController {
	
	private static final Logger LOG = LoggerFactory.getLogger(ConceptViewController.class);

	@FXML private ResourceBundle resources;
	@FXML private URL location;
	
	@FXML private AnchorPane mainPane;
	@FXML private AnchorPane descriptionsPane;
	@FXML private AnchorPane detailPane;
	@FXML private AnchorPane footerPane;
	@FXML private GridPane headerGridPane;
	
	@FXML private TableView<ConceptDescription> descriptionTableView;
	@FXML private TableColumn<ConceptDescription, ConceptDescription> descriptionTypeTableColumn;
	@FXML private TableColumn<ConceptDescription, ConceptDescription> acceptabilityTableColumn;
	@FXML private TableColumn<ConceptDescription, ConceptDescription> significanceTableColumn;
	@FXML private TableColumn<ConceptDescription, ConceptDescription> dialectTableColumn;
	@FXML private TableColumn<ConceptDescription, StampedItem> statusTableColumn;
	@FXML private TableColumn<ConceptDescription, ConceptDescription> descriptionValueTableColumn;
	
    @FXML private TableColumn<ConceptDescription, ?> descriptionTableSTAMPColumn;
	@FXML private TableColumn<ConceptDescription, StampedItem> moduleTableColumn;
	@FXML private TableColumn<ConceptDescription, StampedItem> timeTableColumn;
	@FXML private TableColumn<ConceptDescription, StampedItem> authorTableColumn;
	@FXML private TableColumn<ConceptDescription, StampedItem> pathTableColumn;
	
	@FXML private Label conceptCodeLabel;

	@FXML private ComboBox<State> statusComboBox;
	@FXML private ComboBox<Integer> moduleComboBox;
	@FXML private VBox uuidsVBox;
	
	@FXML private Button minusDescriptionButton;
	@FXML private Button plusDescriptionButton;
	@FXML private Button duplicateDescriptionButton;
	
	@FXML private ToggleButton activeOnlyToggle;
	@FXML private ToggleButton stampToggle;

	@FXML private Button cancelButton;
	@FXML private Button commitButton;
	
	private ConceptNode conceptNode = new ConceptNode(null, false);
	
	private UpdateableBooleanBinding updateableBooleanBinding;
	
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

		assert descriptionTableSTAMPColumn 			!= null : "fx:id=\"descriptionTableSTAMPColumn\" was not injected: check your FXML file 'ConceptView.fxml'.";
		assert moduleTableColumn 			!= null : "fx:id=\"moduleTableColumn\" was not injected: check your FXML file 'ConceptView.fxml'.";
		assert timeTableColumn 				!= null : "fx:id=\"timeTableColumn\" was not injected: check your FXML file 'ConceptView.fxml'.";
		assert authorTableColumn 			!= null : "fx:id=\"authorTableColumn\" was not injected: check your FXML file 'ConceptView.fxml'.";
		assert pathTableColumn 				!= null : "fx:id=\"pathTableColumn\" was not injected: check your FXML file 'ConceptView.fxml'.";
		
		assert mainPane 					!= null : "fx:id=\"mainPane\" was not injected: check your FXML file 'ConceptView.fxml'.";
		assert descriptionsPane 			!= null : "fx:id=\"descriptionsPane\" was not injected: check your FXML file 'ConceptView.fxml'.";
		assert headerGridPane 				!= null : "fx:id=\"headerGridPane\" was not injected: check your FXML file 'ConceptView.fxml'.";
		
		assert conceptCodeLabel 			!= null : "fx:id=\"conceptCodeLabel\" was not injected: check your FXML file 'ConceptView.fxml'.";

		assert statusComboBox 			!= null : "fx:id=\"statusComboBox\" was not injected: check your FXML file 'ConceptView.fxml'.";
		assert moduleComboBox 			!= null : "fx:id=\"moduleComboBox\" was not injected: check your FXML file 'ConceptView.fxml'.";
		assert uuidsVBox 			!= null : "fx:id=\"uuidsVBox\" was not injected: check your FXML file 'ConceptView.fxml'.";

		assert minusDescriptionButton 		!= null : "fx:id=\"minusDescriptionButton\" was not injected: check your FXML file 'ConceptView.fxml'.";
		assert duplicateDescriptionButton 	!= null : "fx:id=\"editDescriptionButton\" was not injected: check your FXML file 'ConceptView.fxml'.";
		assert plusDescriptionButton 		!= null : "fx:id=\"plusDescriptionButton\" was not injected: check your FXML file 'ConceptView.fxml'.";
		assert activeOnlyToggle 			!= null : "fx:id=\"activeOnlyToggle\" was not injected: check your FXML file 'ConceptView.fxml'.";
		assert stampToggle 					!= null : "fx:id=\"stampToggleToggle\" was not injected: check your FXML file 'ConceptView.fxml'.";
		
		assert cancelButton 	!= null : "fx:id=\"cancelButton\" was not injected: check your FXML file 'ConceptView.fxml'.";
		assert commitButton 	!= null : "fx:id=\"commitButton\" was not injected: check your FXML file 'ConceptView.fxml'.";

		FxUtils.assignImageToButton(activeOnlyToggle, 		Images.FILTER_16.createImageView(), "Show Active Only / Show All");
		FxUtils.assignImageToButton(stampToggle, 			Images.STAMP.createImageView(), 	"Show/Hide STAMP Columns");
		FxUtils.assignImageToButton(plusDescriptionButton, 	Images.PLUS.createImageView(), 		"Create Description");
		FxUtils.assignImageToButton(minusDescriptionButton, 	Images.MINUS.createImageView(), 	"Retire/Unretire Description");
		FxUtils.assignImageToButton(duplicateDescriptionButton, 	Images.EDIT.createImageView(), 		"Edit Description");

		setupConceptNode();
		setColumnWidths();
		setupColumnTypes();
		setupDescriptionTable();
		setupStatusComboBox();
		setupModuleComboBox();
		setupConceptCodeLabel();
		setupUuidsVBox();
		setupActiveOnlyToggle();
		setupStampToggle();
		setupCancelButton();
		setupCommitButton();
		
		descriptionTableView.setPlaceholder(new Label("There are no Descriptions for the selected Concept."));

		headerGridPane.add(conceptNode.getNode(), 0, 0, 3, 1);

		// This binding refreshes whenever its bindings change
		updateableBooleanBinding = new UpdateableBooleanBinding() {
			private volatile boolean enabled = false;
            {
                setComputeOnInvalidate(true);
                addBinding(
                		conceptNode.getConceptProperty(),
                		activeOnlyToggle.selectedProperty(),
                		stampToggle.selectedProperty());
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
	
	void setColumnWidths() {
//		descriptionTypeTableColumn.prefWidthProperty().bind(	descriptionTableView.widthProperty().multiply(0.15));
//		descriptionValueTableColumn.prefWidthProperty().bind(	descriptionTableView.widthProperty().multiply(0.25));
//		dialectTableColumn.prefWidthProperty().bind(			descriptionTableView.widthProperty().multiply(0.15));
//		acceptabilityTableColumn.prefWidthProperty().bind(		descriptionTableView.widthProperty().multiply(0.10));
//		significanceTableColumn.prefWidthProperty().bind(		descriptionTableView.widthProperty().multiply(0.15));
//		statusTableColumn.prefWidthProperty().bind(				descriptionTableView.widthProperty().multiply(0.07));
//		moduleTableColumn.prefWidthProperty().bind(				descriptionTableView.widthProperty().multiply(0.13));
	}
	
	private void setupColumnTypes() {
		descriptionTypeTableColumn.setUserData(ConceptViewColumnType.TYPE);
		acceptabilityTableColumn.setUserData(ConceptViewColumnType.ACCEPTABILITY);
		significanceTableColumn.setUserData(ConceptViewColumnType.SIGNIFICANCE);
		dialectTableColumn.setUserData(ConceptViewColumnType.LANGUAGE);
		statusTableColumn.setUserData(ConceptViewColumnType.STAMP_STATE);
		descriptionValueTableColumn.setUserData(ConceptViewColumnType.VALUE);
		
		descriptionTableSTAMPColumn.setUserData(ConceptViewColumnType.STAMP_HEADING);
		moduleTableColumn.setUserData(ConceptViewColumnType.STAMP_MODULE);
		timeTableColumn.setUserData(ConceptViewColumnType.STAMP_TIME);
		authorTableColumn.setUserData(ConceptViewColumnType.STAMP_AUTHOR);
		pathTableColumn.setUserData(ConceptViewColumnType.STAMP_PATH);
	}

	public ConceptSnapshot getConceptSnapshot() {
		return (conceptNode.isValid().get()) ? conceptNode.getConcept() : null;
	}
	public void setConcept(int conceptId) {
		Task<ConceptSnapshot> task = new Task<ConceptSnapshot>() {
			@Override
			protected ConceptSnapshot call() throws Exception {
				return Get.conceptSnapshot().getConceptSnapshot(conceptId);
			}

			@Override
			protected void succeeded() {
				Platform.runLater(() -> conceptNode.set(getValue()));
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
		ConceptChronology<? extends StampedVersion> concept = null;
		if (conceptNode.getConceptProperty().get() != null) {
			concept = conceptNode.getConcept().getChronology();
		}

		refreshConceptDescriptions(concept);
	}

	private void setupConceptNode() {
		conceptNode.getNode().setPadding(new Insets(10,0,10,10));
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
		conceptNode.getConceptProperty().addListener(new ChangeListener<ConceptSnapshot>() {
			@Override
			public void changed(
					ObservableValue<? extends ConceptSnapshot> observable,
					ConceptSnapshot oldValue, ConceptSnapshot newValue) {
				loadConceptCodeFromConcept(newValue);
			}
		});
		loadConceptCodeFromConcept(conceptNode.getConcept());
	}
	private void loadConceptCodeFromConcept(ConceptSnapshot concept) {
		conceptCodeLabel.setText(null);
		if (concept != null) {
			Task<Optional<Long>> task = new Task<Optional<Long>>() {
				@Override
				protected Optional<Long> call() throws Exception {
					return Frills.getSctId(concept.getChronology().getNid(), StampCoordinates.getDevelopmentLatest());
				}

				@Override
				protected void succeeded() {
					conceptCodeLabel.setText(getValue().isPresent() ? getValue().get().toString() : null);
				}
			};
			
			Utility.execute(task);
		}
	}
	
	private void setupUuidsVBox() {
		conceptNode.getConceptProperty().addListener(new ChangeListener<ConceptSnapshot>() {
			@Override
			public void changed(
					ObservableValue<? extends ConceptSnapshot> observable,
					ConceptSnapshot oldValue, ConceptSnapshot newValue) {
				loadUuidsVBoxFromConcept(newValue);
			}
		});
		loadUuidsVBoxFromConcept(conceptNode.getConcept());
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
				},
				new CommonMenusNIdProvider() {
					@Override
					public Collection<Integer> getNIds() {
						return Arrays.asList(new Integer[] {concept.getChronology().getNid()});
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
		conceptNode.getConceptProperty().addListener(new ChangeListener<ConceptSnapshot>() {
			@Override
			public void changed(
					ObservableValue<? extends ConceptSnapshot> observable,
					ConceptSnapshot oldValue, ConceptSnapshot newValue) {
				loadStatusComboBoxFromConcept(newValue);
			}
		});
		
		loadStatusComboBoxFromConcept(conceptNode.getConcept());
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

	private void setupModuleComboBox() {
		moduleComboBox.setCellFactory((param) -> {
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
		moduleComboBox.setButtonCell(new ListCell<Integer>() {
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
		moduleComboBox.setConverter(new StringConverter<Integer>() {
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
		conceptNode.getConceptProperty().addListener(new ChangeListener<ConceptSnapshot>() {
			@Override
			public void changed(
					ObservableValue<? extends ConceptSnapshot> observable,
					ConceptSnapshot oldValue, ConceptSnapshot newValue) {
				loadModuleComboBoxFromConcept(newValue);
			}
		});
		
		loadModuleComboBoxFromConcept(conceptNode.getConcept());
	}
	// In read-only view, set contents/choices of moduleComboBox
	// to module of loaded concept only
	private void loadModuleComboBoxFromConcept(ConceptSnapshot concept) {
		moduleComboBox.getItems().clear();
		if (concept != null) {
			moduleComboBox.getItems().add(concept.getModuleSequence());
			moduleComboBox.getSelectionModel().clearAndSelect(0);
		} else {
			moduleComboBox.buttonCellProperty().set(null);
		}
	}
	
	private void setupDescriptionTable() 
	{
		//descriptionTableView.setTableMenuButtonVisible(true);

		setDescriptionTableFactories(descriptionTableView.getColumns());

		descriptionTypeTableColumn.setComparator(ConceptDescription.valueComparator);
		acceptabilityTableColumn.setComparator(ConceptDescription.valueComparator);
		significanceTableColumn.setComparator(ConceptDescription.valueComparator);
		dialectTableColumn.setComparator(ConceptDescription.valueComparator);
		statusTableColumn.setComparator(StampedItem.statusComparator);
		descriptionValueTableColumn.setComparator(ConceptDescription.valueComparator);
		moduleTableColumn.setComparator(StampedItem.moduleComparator);
	}
	
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
			SimpleStringProperty property = null;
			int conceptSequence = 0;
			ConceptViewColumnType columnType = (ConceptViewColumnType) cell.getTableColumn().getUserData();

			cell.setText(null);
			cell.setGraphic(null);
			cell.setTooltip(null);

			switch (columnType) {
			case STATE_CONDENSED:
				StackPane sp = new StackPane();
				sp.setPrefSize(25, 25);
				String tooltipText = conceptDescription.isActive()? "Active" : "Inactive";
				ImageView image    = conceptDescription.isActive()? Images.BLACK_DOT.createImageView() : Images.GREY_DOT.createImageView();
				sizeAndPosition(image, sp, Pos.CENTER);
				cell.setTooltip(new Tooltip(tooltipText));
				cell.setGraphic(sp);
				break;
				
			case VALUE:
				property = conceptDescription.getValueProperty(); 
				conceptSequence = conceptDescription.getSequence();
				break;
				
			case TYPE:
				property = conceptDescription.getTypeProperty();
				conceptSequence = conceptDescription.getTypeSequence();
				break;
				
			case LANGUAGE:
				property = conceptDescription.getLanguageProperty();
				conceptSequence = conceptDescription.getLanguageSequence();
				break;
				
			case ACCEPTABILITY:
				property = conceptDescription.getAcceptabilityProperty();
				//conceptSequence = conceptDescription.getAcceptabilitySequence();
				break;
			case SIGNIFICANCE:
				property = conceptDescription.getSignificanceProperty();
				conceptSequence = conceptDescription.getSignificanceSequence();
				break;
				
			case STAMP_STATE:
				property = conceptDescription.getStateProperty();
				break;
			case STAMP_TIME:
				property = conceptDescription.getTimeProperty();
				break;
			case STAMP_AUTHOR:
				property = conceptDescription.getAuthorProperty();
				conceptSequence = conceptDescription.getAuthorSequence();
				break;
			case STAMP_MODULE:
				property = conceptDescription.getModuleProperty();
				conceptSequence = conceptDescription.getModuleSequence();
				break;
			case STAMP_PATH:
				property = conceptDescription.getPathProperty();
				conceptSequence = conceptDescription.getPathSequence();
				break;
			default:
				// Nothing
			}
			
			if (property != null) {
				Text text = new Text();
				text.textProperty().bind(property);
				text.wrappingWidthProperty().bind(cell.getTableColumn().widthProperty());
				cell.setGraphic(text);
				
				Tooltip tooltip = new Tooltip();
				tooltip.textProperty().bind(property);
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
			}
			if (conceptSequence != 0) {
				final int sequence = conceptSequence;
				CommonMenus.addCommonMenus(cm, new CommonMenusNIdProvider() {
					@Override
					public Collection<Integer> getNIds() {
						return Arrays.asList(new Integer[] {sequence});
					}
				});
			}
		} else {
			cell.setText(null);
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

	private void refreshConceptDescriptions(ConceptChronology<? extends StampedVersion> concept)
	{
		descriptionTableView.getItems().clear();
		descriptionTableView.getSelectionModel().clearSelection();
		
		if (concept != null) {
			ObservableList<ConceptDescription> descriptionList = ConceptDescription.makeDescriptionList(concept.getConceptDescriptionList(), activeOnlyToggle.selectedProperty().get());
			descriptionTableView.setItems(descriptionList);
		}
	}
}