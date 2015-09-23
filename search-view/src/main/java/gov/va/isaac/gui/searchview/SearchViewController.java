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
package gov.va.isaac.gui.searchview;

import gov.va.isaac.AppContext;
import gov.va.isaac.gui.ConceptNode;
import gov.va.isaac.gui.ConfigureDynamicRefexIndexingView;
import gov.va.isaac.gui.IndexStatusListener;
import gov.va.isaac.gui.SimpleDisplayConcept;
import gov.va.isaac.gui.dragAndDrop.DragRegistry;
import gov.va.isaac.gui.dragAndDrop.SingleConceptIdProvider;
import gov.va.isaac.gui.enhancedsearchview.model.SearchTypeModel;
import gov.va.isaac.gui.util.Images;
import gov.va.isaac.search.CompositeSearchResult;
import gov.va.isaac.search.SearchHandle;
import gov.va.isaac.search.SearchHandler;
import gov.va.isaac.util.CommonMenuBuilderI;
import gov.va.isaac.util.CommonMenus;
import gov.va.isaac.util.CommonMenusDataProvider;
import gov.va.isaac.util.CommonMenusNIdProvider;
import gov.va.isaac.util.NumberUtilities;
import gov.va.isaac.util.OchreUtility;
import gov.va.isaac.util.TaskCompleteCallback;
import gov.va.isaac.util.Utility;
import gov.va.isaac.util.ValidBooleanBinding;
import gov.vha.isaac.metadata.source.IsaacMetadataAuxiliaryBinding;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.chronicle.IdentifiedObjectLocal;
import gov.vha.isaac.ochre.api.component.concept.ConceptSnapshot;
import gov.vha.isaac.ochre.api.component.sememe.version.DynamicSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeColumnInfo;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeDataBI;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeDataType;
import gov.vha.isaac.ochre.api.index.IndexServiceBI;
import gov.vha.isaac.ochre.impl.sememe.DynamicSememeUsageDescription;
import gov.vha.isaac.ochre.model.sememe.dataTypes.DynamicSememeString;
import gov.vha.isaac.ochre.util.Interval;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import javafx.util.StringConverter;
import org.ihtsdo.otf.query.lucene.LuceneDescriptionType;
import org.ihtsdo.otf.query.lucene.indexers.DynamicSememeIndexer;
import org.ihtsdo.otf.query.lucene.indexers.DynamicSememeIndexerConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sun.javafx.collections.ObservableListWrapper;


/**
 * Controller class for the Search View.
 * <p>
 * Logic was initially copied LEGO {@code SnomedSearchController}. 
 * Has been enhanced / rewritten much since then.
 *
 * @author ocarlsen
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */

public class SearchViewController implements TaskCompleteCallback
{
	private static final Logger LOG = LoggerFactory.getLogger(SearchViewController.class);

	@FXML private ResourceBundle resources;
	@FXML private URL location;
	@FXML private BorderPane borderPane;
	@FXML private TextField searchText;
	@FXML private Button searchButton;
	@FXML private ProgressIndicator searchProgress;
	@FXML private ChoiceBox<SearchInOptions> searchIn;
	@FXML private ChoiceBox<Integer> searchLimit;
	@FXML private ListView<CompositeSearchResult> searchResults;
	@FXML private TitledPane optionsPane;
	@FXML private HBox searchInRefexHBox;
	@FXML private VBox optionsContentVBox;
	@FXML private HBox searchInDescriptionHBox;
	@FXML private ChoiceBox<SimpleDisplayConcept> descriptionTypeSelection;
	
	@FXML private ToolBar toolBar;
	@FXML private Label statusLabel;

	private final BooleanProperty searchRunning = new SimpleBooleanProperty(false);
	private SearchHandle ssh = null;
	private ConceptNode searchInDynamicSememe;
	private ConceptNode searchInSememe;
	private ObservableList<SimpleDisplayConcept> dynamicRefexList_ = new ObservableListWrapper<>(new ArrayList<>());
	private Tooltip tooltip = new Tooltip();
	private Integer currentlyEnteredAssemblageSequence = null;
	private FlowPane searchInColumnsHolder = new FlowPane();
	private enum SearchInOptions {Descriptions, Sememes, DynamicSememes};
	private ArrayList<Consumer<IndexServiceBI>> changeNotificationConsumers_ = new ArrayList<>();
	private SimpleBooleanProperty displayIndexConfigMenu_ = new SimpleBooleanProperty(false);

	public static SearchViewController init() throws IOException
	{
		// Load from FXML.
		URL resource = SearchViewController.class.getResource("SearchView.fxml");
		FXMLLoader loader = new FXMLLoader(resource);
		loader.load();
		return loader.getController();
	}

	@FXML
	public void initialize()
	{
		assert borderPane != null : "fx:id=\"borderPane\" was not injected: check your FXML file 'SearchView.fxml'.";
		assert searchText != null : "fx:id=\"searchText\" was not injected: check your FXML file 'SearchView.fxml'.";
		assert searchButton != null : "fx:id=\"searchButton\" was not injected: check your FXML file 'SearchView.fxml'.";
		assert searchProgress != null : "fx:id=\"searchProgress\" was not injected: check your FXML file 'SearchView.fxml'.";
		assert searchIn != null : "fx:id=\"searchIn\" was not injected: check your FXML file 'SearchView.fxml'.";
		assert searchResults != null : "fx:id=\"searchResults\" was not injected: check your FXML file 'SearchView.fxml'.";
		assert optionsPane != null : "fx:id=\"optionsPane\" was not injected: check your FXML file 'SearchView.fxml'.";
		assert searchInRefexHBox != null : "fx:id=\"searchInRefexHBox\" was not injected: check your FXML file 'SearchView.fxml'.";
		assert optionsContentVBox != null : "fx:id=\"optionsContentVBox\" was not injected: check your FXML file 'SearchView.fxml'.";

		borderPane.getStylesheets().add(SearchViewController.class.getResource("/isaac-shared-styles.css").toString());
		
		searchIn.getItems().add(SearchInOptions.Descriptions);
		searchIn.getItems().add(SearchInOptions.Sememes);
		searchIn.getItems().add(SearchInOptions.DynamicSememes);
		searchIn.setConverter(new StringConverter<SearchInOptions>()
		{
			@Override
			public String toString(SearchInOptions object)
			{
				return (object == SearchInOptions.DynamicSememes ? "Dynamic Sememes" : object.toString());
			}

			@Override
			public SearchInOptions fromString(String string)
			{
				// never used
				return null;
			}
		}); 

		searchIn.getSelectionModel().select(0);
		
		tooltip.setText("Enter the description text to search for.  Advanced query syntax such as 'AND', 'NOT' is supported.  You may also enter UUIDs for concepts.");
		tooltip.setWrapText(true);
		tooltip.setMaxWidth(600);
		searchText.setTooltip(tooltip);
		
		optionsContentVBox.getChildren().remove(searchInRefexHBox);
		
		searchIn.valueProperty().addListener((change) ->
		{
			if (searchIn.getSelectionModel().getSelectedItem() == SearchInOptions.Descriptions)
			{
				tooltip.setText("Enter the description text to search for.  Advanced query syntax such as 'AND', 'NOT', 'OR' is supported.  You may also enter UUIDs "
						+ "or NIDs for concepts.");
				optionsContentVBox.getChildren().remove(searchInRefexHBox);
				optionsContentVBox.getChildren().remove(searchInColumnsHolder);
				optionsContentVBox.getChildren().add(searchInDescriptionHBox);
				searchInDynamicSememe.clear();  //make sure an invalid state here doesn't prevent the search, when the field is hidden.
			}
			else if (searchIn.getSelectionModel().getSelectedItem() == SearchInOptions.DynamicSememes)
			{
				tooltip.setText("Enter the dynamic sememe value to search for.  Advanced query syntax such as 'AND', 'NOT', 'OR' is supported for sememe data fields that "
						+ "are indexed as string values.  For numeric values, mathematical interval syntax is supported - such as [4,6] or (-5,10]."
						+ "  You may also search for 1 or more UUIDs and/or NIDs.");
				optionsContentVBox.getChildren().remove(searchInDescriptionHBox);
				searchInRefexHBox.getChildren().remove(searchInSememe.getNode());
				if (!searchInRefexHBox.getChildren().contains(searchInDynamicSememe.getNode()))
				{
					searchInRefexHBox.getChildren().add(searchInDynamicSememe.getNode());
				}
				if (!optionsContentVBox.getChildren().contains(searchInRefexHBox))
				{
					optionsContentVBox.getChildren().add(searchInRefexHBox);
				}
				if (searchInColumnsHolder.getChildren().size() > 0)
				{
					optionsContentVBox.getChildren().add(searchInColumnsHolder);
				}
			}
			else if (searchIn.getSelectionModel().getSelectedItem() == SearchInOptions.Sememes)
			{
				tooltip.setText("Enter the sememe value to search for.  Advanced query syntax such as 'AND', 'NOT', 'OR' is supported for sememe data fields that "
						+ "are indexed as string values.");
				optionsContentVBox.getChildren().remove(searchInDescriptionHBox);
				searchInRefexHBox.getChildren().remove(searchInDynamicSememe.getNode());
				optionsContentVBox.getChildren().remove(searchInColumnsHolder);
				if (!searchInRefexHBox.getChildren().contains(searchInSememe.getNode()))
				{
					searchInRefexHBox.getChildren().add(searchInSememe.getNode());
				}
				if (!optionsContentVBox.getChildren().contains(searchInRefexHBox))
				{
					optionsContentVBox.getChildren().add(searchInRefexHBox);
				}
			}
			else
			{
				throw new RuntimeException("oops");
			}
		});
		
		searchInDynamicSememe = new ConceptNode(null, false, dynamicRefexList_, null);
		searchInDynamicSememe.getConceptProperty().addListener(new InvalidationListener()
		{
			@Override
			public void invalidated(Observable observable)
			{
				ConceptSnapshot newValue = searchInDynamicSememe.getConceptProperty().get();
				if (newValue != null)
				{
					searchInColumnsHolder.getChildren().clear();
					try
					{
						DynamicSememeUsageDescription rdud = DynamicSememeUsageDescription.read(newValue.getNid());
						displayIndexConfigMenu_.set(true);
						currentlyEnteredAssemblageSequence = rdud.getDynamicSememeUsageDescriptorSequence();
						Integer[] indexedColumns = DynamicSememeIndexerConfiguration.readIndexInfo(currentlyEnteredAssemblageSequence);
						if (indexedColumns == null || indexedColumns.length == 0)
						{
							searchInDynamicSememe.isValid().setInvalid("Sememe searches can only be performed on indexed columns in the sememe.  The selected "
									+ "sememe does not contain any indexed data columns.  Please configure the indexes to search this sememe.");
							optionsContentVBox.getChildren().remove(searchInColumnsHolder);
						}
						else
						{
							Label l = new Label("Search in Columns");
							searchInColumnsHolder.getChildren().add(l);
							l.minWidthProperty().bind(((Label)searchInRefexHBox.getChildren().get(0)).widthProperty());
							DynamicSememeColumnInfo[] rdci = rdud.getColumnInfo();
							if (rdci.length > 0)
							{
								Arrays.sort(rdci);  //We will depend on them being in the correct order later.
								HashSet<Integer> indexedColumnsSet = new HashSet<>(Arrays.asList(indexedColumns));
								int indexNumber = 0;
								for (DynamicSememeColumnInfo ci : rdci)
								{
									StackPane cbStack = new StackPane();
									CheckBox cb = new CheckBox(ci.getColumnName());
									if (ci.getColumnDataType() == DynamicSememeDataType.BYTEARRAY || !indexedColumnsSet.contains(indexNumber))
									{
										cb.setDisable(true);  //No index on this column... not searchable
										Tooltip.install(cbStack, new Tooltip("Column Datatype: " + ci.getColumnDataType().getDisplayName() + " is not indexed"));
									}
									else
									{
										cb.setSelected(true);
										cb.setTooltip(new Tooltip("Column Datatype: " + ci.getColumnDataType().getDisplayName()));
									}
									cbStack.getChildren().add(cb);
									searchInColumnsHolder.getChildren().add(cbStack);
									indexNumber++;
								}
								if (!optionsContentVBox.getChildren().contains(searchInColumnsHolder))
								{
									optionsContentVBox.getChildren().add(searchInColumnsHolder);
								}
							}
							else
							{
								searchInDynamicSememe.isValid().setInvalid("Sememe searches can only be performed on the data in the sememe.  The selected "
										+ "sememe does not contain any data columns.");
								optionsContentVBox.getChildren().remove(searchInColumnsHolder);
							}
						}
					}
					catch (Exception e1)
					{
						searchInDynamicSememe.isValid().setInvalid("Sememe searches can only be limited to valid Dynamic Sememe Assemblage concept types."
								+ "  The current value is not a Dynamic Sememe Assemblage concept.");
						LOG.debug("Exception while checking is sememe concept field in search box was a dynamic sememe", e1);
						displayIndexConfigMenu_.set(false);
						currentlyEnteredAssemblageSequence = null;
						optionsContentVBox.getChildren().remove(searchInColumnsHolder);
						searchInColumnsHolder.getChildren().clear();
					}
				}
				else
				{
					currentlyEnteredAssemblageSequence = null;
					optionsContentVBox.getChildren().remove(searchInColumnsHolder);
					searchInColumnsHolder.getChildren().clear();
				}
				
			}
		});
		
		searchInSememe = new ConceptNode(null, false);
		searchInSememe.getConceptProperty().addListener(new InvalidationListener()
		{
			@Override
			public void invalidated(Observable observable)
			{
				ConceptSnapshot newValue = searchInSememe.getConceptProperty().get();
				if (newValue != null)
				{
					currentlyEnteredAssemblageSequence = newValue.getConceptSequence();
				}
				else
				{
					currentlyEnteredAssemblageSequence = null;
				}
			}
		});
		
		MenuItem configureIndex =  new MenuItem("Configure Sememe Indexing");
		configureIndex.setOnAction((action) ->
		{
			ConceptSnapshot c = searchInDynamicSememe.getConceptProperty().get();
			if (c != null)
			{
				new ConfigureDynamicRefexIndexingView(c.getNid()).showView(null);
			}
		});
		configureIndex.setGraphic(Images.CONFIGURE.createImageView());
		configureIndex.visibleProperty().bind(displayIndexConfigMenu_);
		searchInDynamicSememe.addMenu(configureIndex);
		
		searchLimit.setConverter(new StringConverter<Integer>()
		{
			@Override
			public String toString(Integer object)
			{
				return object == Integer.MAX_VALUE ? "No Limit" : object.toString(); 
			}

			@Override
			public Integer fromString(String string)
			{
				// not needed
				return null;
			}
		});
		
		searchLimit.getItems().add(100);
		searchLimit.getItems().add(500);
		searchLimit.getItems().add(1000);
		searchLimit.getItems().add(10000);
		searchLimit.getItems().add(100000);
		searchLimit.getItems().add(Integer.MAX_VALUE);
		searchLimit.getSelectionModel().select(0);
		
		searchInRefexHBox.getChildren().add(searchInDynamicSememe.getNode());
		HBox.setHgrow(searchInDynamicSememe.getNode(), Priority.ALWAYS);
		HBox.setHgrow(searchInSememe.getNode(), Priority.ALWAYS);
		
		descriptionTypeSelection.getItems().add(new SimpleDisplayConcept("All", Integer.MIN_VALUE));
		descriptionTypeSelection.getItems().add(new SimpleDisplayConcept("Fully Specified Name", LuceneDescriptionType.FSN.ordinal()));
		descriptionTypeSelection.getItems().add(new SimpleDisplayConcept("Synonym", LuceneDescriptionType.SYNONYM.ordinal()));
		descriptionTypeSelection.getItems().add(new SimpleDisplayConcept("Definition", LuceneDescriptionType.DEFINITION.ordinal()));
		descriptionTypeSelection.getItems().add(new SimpleDisplayConcept("--- Terminology Types ---", Integer.MAX_VALUE));
		
		descriptionTypeSelection.valueProperty().addListener((change) ->
		{
			if (descriptionTypeSelection.getValue().getNid() == Integer.MAX_VALUE)
			{
				descriptionTypeSelection.getSelectionModel().clearAndSelect(0);
			}
		});
		
		descriptionTypeSelection.getSelectionModel().clearAndSelect(0);
		
		searchInColumnsHolder.setHgap(10);
		searchInColumnsHolder.setVgap(5.0);

		searchResults.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		searchResults.setCellFactory(new Callback<ListView<CompositeSearchResult>, ListCell<CompositeSearchResult>>()
		{
			@Override
			public ListCell<CompositeSearchResult> call(ListView<CompositeSearchResult> arg0)
			{
				return new ListCell<CompositeSearchResult>()
				{
					@Override
					protected void updateItem(final CompositeSearchResult item, boolean empty)
					{
						super.updateItem(item, empty);
						if (!empty)
						{
							VBox box = new VBox();
							box.setFillWidth(true);
							
							final Optional<ConceptSnapshot> wbConcept = item.getContainingConcept();
							String preferredText = (wbConcept.isPresent() ? wbConcept.get().getConceptDescriptionText() : 
								"Containing Concept for nid " + item.getMatchingDescriptionComponents().iterator().next().getNid() + " not on path!");
						
							if (item.getMatchingComponents().size() > 0 && item.getMatchingComponents().iterator().next() instanceof DynamicSememe<?>)
							{
								HBox hb = new HBox();
								Label concept = new Label("Referenced Concept");
								concept.getStyleClass().add("boldLabel");
								hb.getChildren().add(concept);
								hb.getChildren().add(new Label("  " + preferredText));
								
								box.getChildren().add(hb);
							
								for (IdentifiedObjectLocal c : item.getMatchingComponents())
								{
									if (c instanceof DynamicSememe<?>)
									{
										DynamicSememe<?> rv = (DynamicSememe<?>)c;
										HBox assemblageConBox = new HBox();
										Label assemblageCon = new Label("Assemblage Concept");
										assemblageCon.getStyleClass().add("boldLabel");
										HBox.setMargin(assemblageCon, new Insets(0.0, 0.0, 0.0, 10.0));
										assemblageConBox.getChildren().add(assemblageCon);
										assemblageConBox.getChildren().add(new Label("  " + Get.conceptDescriptionText(rv.getAssemblageSequence())));
										box.getChildren().add(assemblageConBox);

										Label attachedData = new Label("Attached Data");
										attachedData.getStyleClass().add("boldLabel");
										VBox.setMargin(attachedData, new Insets(0.0, 0.0, 0.0, 10.0));
										box.getChildren().add(attachedData);
										
										try
										{
											DynamicSememeColumnInfo[] ci = rv.getDynamicSememeUsageDescription().getColumnInfo();
											int i = 0;
											
											for (DynamicSememeDataBI data : rv.getData())
											{
												Label l = new Label();
												if (data == null)  //might be an unset column, if the col is optional
												{
													continue;
												}
												if (DynamicSememeDataType.BYTEARRAY == data.getDynamicSememeDataType())
												{
													l.setText(ci[i].getColumnName() +  " - [Binary]");
												}
												else
												{
													l.setText(ci[i].getColumnName() + " - " + data.getDataObject().toString());
												}
												VBox.setMargin(l, new Insets(0.0, 0.0, 0.0, 20.0));
												box.getChildren().add(l);
												i++;
											}
										}
										catch (Exception e)
										{
											LOG.error("Unexpected error reading sememe info", e);
										}
									}
									else
									{
										LOG.warn("Unexpected type on match: {}", c);
									}
								}
							}
							else
							{
								Label concept = new Label(preferredText);
								concept.getStyleClass().add("boldLabel");
								box.getChildren().add(concept);
								for (String s : item.getMatchingStrings())
								{
									if (s.equals(preferredText))
									{
										continue;
									}
									Label matchString = new Label(s);
									VBox.setMargin(matchString, new Insets(0.0, 0.0, 0.0, 10.0));
									box.getChildren().add(matchString);
								}
							}
							setGraphic(box);

							// Also show concept details on double-click.
							setOnMouseClicked(new EventHandler<MouseEvent>()
							{
								@Override
								public void handle(MouseEvent mouseEvent)
								{
									if (mouseEvent.getButton().equals(MouseButton.PRIMARY))
									{
										if (mouseEvent.getClickCount() == 2 && wbConcept.isPresent())
										{
											AppContext.getCommonDialogs().showConceptDialog(wbConcept.get().getPrimordialUuid());
										}
									}
								}
							});

							ContextMenu cm = new ContextMenu();
							CommonMenusDataProvider dp = new CommonMenusDataProvider()
							{
								@Override
								public String[] getStrings()
								{
									List<String> items = new ArrayList<>();
									for (CompositeSearchResult currentItem : searchResults.getSelectionModel().getSelectedItems())
									{
										Optional<ConceptSnapshot> currentWbConcept = currentItem.getContainingConcept();
										if (!currentWbConcept.isPresent())
										{
											//not on path, most likely
											continue;
										}
										items.add(currentWbConcept.get().getConceptDescriptionText());
									}

									String[] itemArray = items.toArray(new String[items.size()]);

									return itemArray;
								}
							};
							CommonMenusNIdProvider nidProvider = new CommonMenusNIdProvider()
							{
								@Override
								public Set<Integer> getNIds()
								{
									Set<Integer> nids = new HashSet<>();

									for (CompositeSearchResult r : searchResults.getSelectionModel().getSelectedItems())
									{
										if (r.getContainingConcept().isPresent())
										{
											nids.add(r.getContainingConcept().get().getNid());
										}
										
									}
									return nids;
								}
							}; 
							CommonMenuBuilderI menuBuilder = CommonMenus.CommonMenuBuilder.newInstance();
				
							menuBuilder.setMenuItemsToExclude(
									CommonMenus.CommonMenuItem.LOINC_REQUEST_VIEW, 
									CommonMenus.CommonMenuItem.USCRS_REQUEST_VIEW,
									CommonMenus.CommonMenuItem.WORKFLOW_INITIALIZATION_VIEW);
							CommonMenus.addCommonMenus(cm, menuBuilder, dp, nidProvider);

							setContextMenu(cm);
						}
						else
						{
							setText("");
							setGraphic(null);
						}
					}
				};
			}
		});

		AppContext.getService(DragRegistry.class).setupDragOnly(searchResults, new SingleConceptIdProvider()
		{
			@Override
			public String getConceptId()
			{
				CompositeSearchResult dragItem = searchResults.getSelectionModel().getSelectedItem();
				if (dragItem != null)
				{
					if (dragItem.getContainingConcept().isPresent())
					{
						return dragItem.getContainingConcept().get().getNid() + "";
					}
				}
				return null;
			}
		});

		final ValidBooleanBinding searchTextValid = new ValidBooleanBinding()
		{
			{
				bind(searchText.textProperty(), searchIn.valueProperty());
				setComputeOnInvalidate(true);
			}
			@Override
			protected boolean computeValue()
			{
				if ((searchIn.getValue() == SearchInOptions.DynamicSememes && searchText.getText().length() > 0) || searchText.getText().length() > 1)
				{
					return true;
				}
				else
				{
					return false;
				}
			}
		};
		
		searchProgress.visibleProperty().bind(searchRunning);
		searchButton.disableProperty().bind(searchTextValid.not().or(searchInDynamicSememe.isValid().not()));

		// Perform search or cancel when button pressed.
		searchButton.setOnAction(new EventHandler<ActionEvent>()
		{
			@Override
			public void handle(ActionEvent e)
			{
				if (searchRunning.get() && ssh != null)
				{
					ssh.cancel();
				}
				else
				{
					search();
				}
			}
		});

		// Change button text while search running.
		searchRunning.addListener(new ChangeListener<Boolean>()
		{
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue)
			{
				if (searchRunning.get())
				{
					searchButton.setText("Cancel");
				}
				else
				{
					searchButton.setText("Search");
				}

			}
		});

		// Perform search on Enter keypress.
		searchText.setOnAction(e -> 
		{
			if (searchTextValid.getValue() && !searchRunning.get())
			{
				search();
			}
		});

		// Search text must be greater than one character for description searches
		searchText.textProperty().addListener((ChangeListener<String>) (observable, oldValue, newValue) -> 
		{
			
		});
		
		IndexStatusListener isl = AppContext.getService(IndexStatusListener.class);
		changeNotificationConsumers_.add(isl.onIndexConfigChanged(new Consumer<IndexServiceBI>()
		{
			@Override
			public void accept(IndexServiceBI t)
			{
				Platform.runLater(() -> 
				{
					if (t.getIndexerName().equals(DynamicSememeIndexer.INDEX_NAME))
					{
						//swap the concept in and out, to fire our change listener, so we recheck if the referenced concept is configured in a valid way.
						searchInDynamicSememe.revalidate();
					}
				});
			}
		}));
	}

	public BorderPane getRoot()
	{
		//delay this
		if (dynamicRefexList_.size() == 0)
		{
			populateDynamicSememeList();
		}
		if (descriptionTypeSelection.getItems().size() == 5)
		{
			populateExtendedDescriptionList();
		}
		return borderPane;
	}

	@Override
	public void taskComplete(long taskStartTime, Integer taskId)
	{

		// Run on JavaFX thread.
		Platform.runLater(() -> 
		{
			try
			{
				if (!ssh.isCancelled())
				{
					//Remove off path results
					Collection<CompositeSearchResult> results = ssh.getResults();
					int offPathResults = SearchTypeModel.removeNullResults(results);
					
					searchResults.getItems().addAll(results);
					//searchResults.getItems().addAll(ssh.getResults());
					long time = System.currentTimeMillis() - ssh.getSearchStartTime();
					float inSeconds = (float)time / 1000f;
					inSeconds = ((float)((int)(inSeconds * 100f)) / 100f);
					
					String statusMsg = ssh.getHitCount() + " in " + inSeconds + " seconds";
					if (offPathResults > 0) {
						statusMsg += "; " + offPathResults + " off-path entries ignored";
					}
					statusLabel.setText(statusMsg);
				}
				else
				{
					statusLabel.setText("Search Cancelled");
				}
			}
			catch (Exception ex)
			{
				String title = "Unexpected Search Error";
				LOG.error(title, ex);
				AppContext.getCommonDialogs().showErrorDialog(title, "There was an unexpected error running the search", ex.toString());
				searchResults.getItems().clear();
				statusLabel.setText("Search Failed");
			}
			finally
			{
				searchRunning.set(false);
			}
		});
	}
	
	private Integer[] getSearchColumns()
	{
		if (searchInColumnsHolder.getChildren().size() > 1)
		{
			ArrayList<Integer> result = new ArrayList<>();
			int deselectedCount = 0;
			for (int i = 1; i < searchInColumnsHolder.getChildren().size(); i++)
			{
				CheckBox cb = ((CheckBox)((StackPane)searchInColumnsHolder.getChildren().get(i)).getChildren().get(0));
				if (cb.isSelected())
				{
					result.add(i - 1);
				}
				else if (!cb.isDisable())
				{
					deselectedCount++;
				}
			}
			//If they didn't uncheck any, its more efficient to query without the column filter.
			return deselectedCount == 0 ? null : result.toArray(new Integer[0]);
		}
		return null;
	}

	private synchronized void search()
	{
		try
		{
			// Sanity check if search already running.
			if (searchRunning.get())
			{
				return;
			}
	
			searchRunning.set(true);
			searchResults.getItems().clear();
			// we get called back when the results are ready.
			
			if (searchIn.getValue() == SearchInOptions.Descriptions)
			{
				if (descriptionTypeSelection.getValue().getNid() == Integer.MIN_VALUE)
				{
					LOG.debug("Doing a description search across all description types");
					ssh = SearchHandler.descriptionSearch(searchText.getText(), searchLimit.getValue(), 
							((searchHandle) -> {taskComplete(searchHandle.getSearchStartTime(), searchHandle.getTaskId());}),
							true, false);
				}
				else if (descriptionTypeSelection.getValue().getNid() == LuceneDescriptionType.FSN.ordinal())
				{
					LOG.debug("Doing a description search on FSN");
					ssh = SearchHandler.descriptionSearch(searchText.getText(), searchLimit.getValue(), false, LuceneDescriptionType.FSN, 
							((searchHandle) -> {taskComplete(searchHandle.getSearchStartTime(), searchHandle.getTaskId());}),
							null, null, null, true, false);
				}
				else if (descriptionTypeSelection.getValue().getNid() == LuceneDescriptionType.DEFINITION.ordinal())
				{
					LOG.debug("Doing a description search on Definition");
					ssh = SearchHandler.descriptionSearch(searchText.getText(), searchLimit.getValue(), false, LuceneDescriptionType.DEFINITION, 
							((searchHandle) -> {taskComplete(searchHandle.getSearchStartTime(), searchHandle.getTaskId());}),
							null, null, null, true, false);
				}
				else if (descriptionTypeSelection.getValue().getNid() == LuceneDescriptionType.SYNONYM.ordinal())
				{
					LOG.debug("Doing a description search on Synonym");
					ssh = SearchHandler.descriptionSearch(searchText.getText(), searchLimit.getValue(), false, LuceneDescriptionType.SYNONYM, 
							((searchHandle) -> {taskComplete(searchHandle.getSearchStartTime(), searchHandle.getTaskId());}),
							null, null, null, true, false);
				}
				else
				{
					LOG.debug("Doing a description search on the extended type {}", descriptionTypeSelection.getValue().getDescription());
					ssh = SearchHandler.descriptionSearch(searchText.getText(), searchLimit.getValue(), false, 
						Get.identifierService().getUuidPrimordialForNid(descriptionTypeSelection.getValue().getNid()).get(), 
						((searchHandle) -> {taskComplete(searchHandle.getSearchStartTime(), searchHandle.getTaskId());}),
						null, null, null, true, false);
				}
			}
			else if (searchIn.getValue() == SearchInOptions.DynamicSememes)
			{
				String searchString = searchText.getText().trim();
				try
				{
					DynamicSememeDataBI data = NumberUtilities.wrapIntoRefexHolder(NumberUtilities.parseUnknown(searchString));
					LOG.debug("Doing a sememe search with a numeric value");
					ssh = SearchHandler.dynamicSememeSearch((indexer) ->
					{
						try
						{
							return indexer.query(data, currentlyEnteredAssemblageSequence, false, getSearchColumns(), searchLimit.getValue(), null);
						}
						catch (Exception e)
						{
							throw new RuntimeException(e);
						}
					}, ((searchHandle) -> {taskComplete(searchHandle.getSearchStartTime(), searchHandle.getTaskId());}),
					null, null, null, true, false);
				}
				catch (NumberFormatException e)
				{
					//Not a number...  is it a valid interval?
					try
					{
						Interval interval = new Interval(searchString);
						LOG.debug("Doing a sememe search with an interval value");
						ssh = SearchHandler.dynamicSememeSearch((indexer) ->
						{
							try
							{
								return indexer.queryNumericRange(NumberUtilities.wrapIntoRefexHolder(interval.getLeft()), interval.isLeftInclusive(), 
										NumberUtilities.wrapIntoRefexHolder(interval.getRight()), interval.isRightInclusive(), 
										currentlyEnteredAssemblageSequence, getSearchColumns(), searchLimit.getValue(), null);
							}
							catch (Exception e1)
							{
								throw new RuntimeException(e1);
							}
						}, ((searchHandle) -> {taskComplete(searchHandle.getSearchStartTime(), searchHandle.getTaskId());}),
						null, null, null, true, false);
					}
					catch (NumberFormatException e1) 
					{
						//run it as a string search
						LOG.debug("Doing a sememe search as a string search");
						ssh = SearchHandler.dynamicSememeSearch((indexer) ->
						{
							try
							{
								return indexer.query(new DynamicSememeString(searchText.getText()), currentlyEnteredAssemblageSequence, false, 
										getSearchColumns(), searchLimit.getValue(), null);
							}
							catch (Exception e2)
							{
								throw new RuntimeException(e2);
							}
						}, ((searchHandle) -> {taskComplete(searchHandle.getSearchStartTime(), searchHandle.getTaskId());}),
						null, null, null, true, false);
					}
				}
			}
			else if (searchIn.getValue() == SearchInOptions.Sememes)
			{
				//run it as a string search
				LOG.debug("Doing a sememe search as a string search");
				ssh = SearchHandler.sememeSearch(searchText.getText(), searchLimit.getValue(), currentlyEnteredAssemblageSequence,
						((searchHandle) -> {taskComplete(searchHandle.getSearchStartTime(), searchHandle.getTaskId());}),
				null, null, null, true, false);
			}
			else
			{
				throw new RuntimeException("oops");
			}
		}
		catch (Exception e)
		{
			LOG.error("Search imploded unexpectedly...", e);
			ssh = null;  //force a null ptr in taskComplete, so an error is displayed.
			taskComplete(0, null);
		}
	}
	
	//TODO (artf231420) a listener to trigger this after a user makes a new one...
	private void populateDynamicSememeList()
	{
		Task<Void> t = new Task<Void>()
		{
			HashSet<SimpleDisplayConcept> dynamicRefexAssemblages = new HashSet<>();

			@Override
			protected Void call() throws Exception
			{
				dynamicRefexAssemblages = new HashSet<>();
				dynamicRefexAssemblages.addAll(OchreUtility.getAllDynamicSememeAssemblageConcepts());
				return null;
			}

			/**
			 * @see javafx.concurrent.Task#succeeded()
			 */
			@Override
			protected void succeeded()
			{
				dynamicRefexList_.clear();
				dynamicRefexList_.addAll(dynamicRefexAssemblages);
				dynamicRefexList_.sort(new Comparator<SimpleDisplayConcept>()
				{
					@Override
					public int compare(SimpleDisplayConcept o1, SimpleDisplayConcept o2)
					{
						return o1.getDescription().compareToIgnoreCase(o2.getDescription());
					}
				});
			}
		};
		
		Utility.execute(t);
	}
	
	private void populateExtendedDescriptionList()
	{
		Utility.execute(() ->
		{
			try
			{
				Set<Integer> extendedDescriptionTypes = OchreUtility.getAllChildrenOfConcept(
						IsaacMetadataAuxiliaryBinding.DESCRIPTION_TYPE_IN_SOURCE_TERMINOLOGY.getConceptSequence(), true, true);
				ArrayList<SimpleDisplayConcept> temp = new ArrayList<>();
				for (Integer c : extendedDescriptionTypes)
				{
					temp.add(new SimpleDisplayConcept(c));
				}
				Collections.sort(temp);
				Platform.runLater(() ->
				{
					descriptionTypeSelection.getItems().addAll(temp);
				});
			}
			catch (Exception e1)
			{
				LOG.error("Error reading extended description types", e1);
			}
		});
	}
}
