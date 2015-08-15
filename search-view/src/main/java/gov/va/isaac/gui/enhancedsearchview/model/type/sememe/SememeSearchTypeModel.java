package gov.va.isaac.gui.enhancedsearchview.model.type.sememe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import org.ihtsdo.otf.query.lucene.indexers.DynamicSememeIndexerConfiguration;
import org.ihtsdo.otf.tcc.api.coordinate.ViewCoordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sun.javafx.collections.ObservableListWrapper;
import gov.va.isaac.AppContext;
import gov.va.isaac.gui.ConceptNode;
import gov.va.isaac.gui.SimpleDisplayConcept;
import gov.va.isaac.gui.enhancedsearchview.SearchTypeEnums.ResultsType;
import gov.va.isaac.gui.enhancedsearchview.filters.SememeContentSearchTypeFilter;
import gov.va.isaac.gui.enhancedsearchview.model.SearchModel;
import gov.va.isaac.gui.enhancedsearchview.model.SearchTypeModel;
import gov.va.isaac.search.CompositeSearchResult;
import gov.va.isaac.search.SearchHandle;
import gov.va.isaac.search.SearchHandler;
import gov.va.isaac.util.NumberUtilities;
import gov.va.isaac.util.OchreUtility;
import gov.va.isaac.util.TaskCompleteCallback;
import gov.va.isaac.util.Utility;
import gov.vha.isaac.ochre.api.chronicle.IdentifiedObjectLocal;
import gov.vha.isaac.ochre.api.component.concept.ConceptSnapshot;
import gov.vha.isaac.ochre.api.component.sememe.version.DynamicSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeColumnInfo;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeDataBI;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeDataType;
import gov.vha.isaac.ochre.impl.sememe.DynamicSememeUsageDescription;
import gov.vha.isaac.ochre.model.sememe.dataTypes.DynamicSememeString;
import gov.vha.isaac.ochre.util.Interval;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

public class SememeSearchTypeModel extends SearchTypeModel implements TaskCompleteCallback {
	final static Logger logger = LoggerFactory.getLogger(SememeSearchTypeModel.class);

	private HBox searchInRefexHBox = new HBox();
	private FlowPane searchInColumnsHolder = new FlowPane();
	private ConceptNode searchInRefex;
	private Integer currentlyEnteredAssemblageSequence = null;
	private TextField searchText;
	private Tooltip tooltip = new Tooltip();
	private SearchHandle ssh = null;
	private ObservableList<SimpleDisplayConcept> dynamicRefexList_ = new ObservableListWrapper<>(new ArrayList<>());
	private VBox optionsContentVBox;
	private Font boldFont = new Font("System Bold", 13.0);
	
	public SememeContentSearchTypeFilter getSearchType() {
		return new SememeContentSearchTypeFilter(searchText.getText(), searchInRefex != null ? searchInRefex.getConceptNoWait() : null);
	}
	public void setSearchType(SememeContentSearchTypeFilter filter) {
		ConceptSnapshot conceptFromSearchFilter = filter != null ? filter.getAssemblageConcept() : null;
		searchInRefex.set(conceptFromSearchFilter);
		searchText.setText(filter.getSearchParameter());
	}

	public SememeSearchTypeModel() {
		setupSearchText();
		populateDynamicRefexList();
		
		Label rootExp = new Label("Search Assemblage");
		rootExp.setFont(boldFont);

		searchInRefex = new ConceptNode(null, false, dynamicRefexList_, null);
		searchInRefexHBox.getChildren().addAll(rootExp, searchInRefex.getNode());

		searchInRefex.getConceptProperty().addListener((ChangeListener<ConceptSnapshot>) (observable, oldValue, newValue) -> 
		{
			if (newValue != null)
			{
				searchInColumnsHolder.getChildren().clear();
				try
				{
					DynamicSememeUsageDescription rdud = DynamicSememeUsageDescription.read(newValue.getNid());
					currentlyEnteredAssemblageSequence = rdud.getDynamicSememeUsageDescriptorSequence();
					Integer[] indexedColumns = DynamicSememeIndexerConfiguration.readIndexInfo(currentlyEnteredAssemblageSequence);
					if (indexedColumns == null || indexedColumns.length == 0)
					{
						searchInRefex.isValid().setInvalid("Sememe searches can only be performed on indexed columns in the sememe.  The selected "
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
							optionsContentVBox.getChildren().add(searchInColumnsHolder);
						}
						else
						{
							searchInRefex.isValid().setInvalid("Sememe searches can only be performed on the data in the sememe.  The selected "
									+ "sememe does not contain any data columns.");
							optionsContentVBox.getChildren().remove(searchInColumnsHolder);
						}
					}
				}
				catch (Exception e1)
				{
					searchInRefex.isValid().setInvalid("Sememe searches can only be limited to valid Dynamic Sememe Assemblage concept types."
							+ "  The current value is not a Dynamic Sememe Assemblage concept.");
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
		});
		
		viewCoordinateProperty.addListener(new ChangeListener<ViewCoordinate>() {
			@Override
			public void changed(
					ObservableValue<? extends ViewCoordinate> observable,
					ViewCoordinate oldValue, ViewCoordinate newValue) {	
				isSearchTypeRunnableProperty.set(isValidSearch());
				isSearchTypeSavableProperty.set(isSavableSearch());
			}
		});
		searchText.textProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> observable,
					String oldValue, String newValue) {
				isSearchTypeRunnableProperty.set(isValidSearch());
				isSearchTypeSavableProperty.set(isSavableSearch());
			}
		});
		
		isSearchTypeRunnableProperty.addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable,
					Boolean oldValue, Boolean newValue) {
				if (SearchModel.getSearchTypeSelector().getTypeSpecificModel() == SememeSearchTypeModel.this) {
					SearchModel.isSearchRunnableProperty().set(newValue);
				}
			}
		});
		isSearchTypeRunnableProperty.set(isValidSearch());

		isSearchTypeSavableProperty.addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable,
					Boolean oldValue, Boolean newValue) {
				if (SearchModel.getSearchTypeSelector().getTypeSpecificModel() == SememeSearchTypeModel.this) {
					SearchModel.isSearchSavableProperty().set(newValue);
				}
			}
		});
		isSearchTypeSavableProperty.set(isSavableSearch());
	}
	
	private void setupSearchText() {
		tooltip.setText("Enter the description text to search for.  Advanced query syntax such as 'AND', 'NOT' is supported.  You may also enter UUIDs for concepts.");
		tooltip.setWrapText(true);
		tooltip.setMaxWidth(600);
		searchText = new TextField();
		searchText.setTooltip(tooltip);
	}

	public Integer getCurrentlyEnteredAssemblageSequence() {
		return currentlyEnteredAssemblageSequence;
	}

	public void setCurrentlyEnteredAssemblageSequence(
			Integer currentlyEnteredAssemblageSequence) {
		this.currentlyEnteredAssemblageSequence = currentlyEnteredAssemblageSequence;
	}

	public ConceptNode getSearchInRefex() {
		return searchInRefex;
	}

	public void setSearchInRefex(ConceptNode searchInRefex) {
		this.searchInRefex = searchInRefex;
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

	
	@Override
	public void typeSpecificCopy(SearchTypeModel other) {
	}

	@Override
	public String getModelDisplayString() {
		return ", ";
	}

	@Override
	public void executeSearch(ResultsType resultsType, String modelMaxResults) {
		try
		{
			String searchString = searchText.getText().trim();
			try
			{
				DynamicSememeDataBI data = NumberUtilities.wrapIntoRefexHolder(NumberUtilities.parseUnknown(searchString));
				LOG.debug("Doing a sememe search with a numeric value");
				ssh = SearchHandler.dynamicRefexSearch((indexer) ->
				{
					try
					{
						return indexer.query(data, currentlyEnteredAssemblageSequence, false, getSearchColumns(), Integer.parseInt(modelMaxResults), null);
					}
					catch (Exception e)
					{
						throw new RuntimeException(e);
					}
				}, this, null, null, null, true, false);
			}
			catch (NumberFormatException e)
			{
				//Not a number...  is it a valid interval?
				try
				{
					Interval interval = new Interval(searchString);
					LOG.debug("Doing a sememe search with an interval value");
					ssh = SearchHandler.dynamicRefexSearch((indexer) ->
					{
						try
						{
							return indexer.queryNumericRange(NumberUtilities.wrapIntoRefexHolder(interval.getLeft()), interval.isLeftInclusive(), 
									NumberUtilities.wrapIntoRefexHolder(interval.getRight()), interval.isRightInclusive(), 
									currentlyEnteredAssemblageSequence, getSearchColumns(), Integer.parseInt(modelMaxResults), null);
						}
						catch (Exception e1)
						{
							throw new RuntimeException(e1);
						}
					}, this, null, null, null, true, false);
				}
				catch (NumberFormatException e1) 
				{
					//run it as a string search
					LOG.debug("Doing a sememe search as a string search");
					ssh = SearchHandler.dynamicRefexSearch((indexer) ->
					{
						try
						{
							return indexer.query(new DynamicSememeString(searchText.getText()), currentlyEnteredAssemblageSequence, false, 
									getSearchColumns(), Integer.parseInt(modelMaxResults), null);
						}
						catch (Exception e2)
						{
							throw new RuntimeException(e2);
						}
					}, this, null, null, null, true, false);
				}
			}
		}
		catch (Exception e)
		{
			LOG.error("Search imploded unexpectedly...", e);
			ssh = null;  //force a null ptr in taskComplete, so an error is displayed.
			taskComplete(0, null);
		}
	}

	public void taskComplete(long taskStartTime, Integer taskId)
	{

		// Run on JavaFX thread.
		Platform.runLater(() -> 
		{
			try
			{
				if (!ssh.isCancelled())
				{
					Collection<CompositeSearchResult> results = createSingleEntryPerResults(ssh.getResults());
					setResults(results);

					/*
					SearchModel.getSearchResultsTable().getResults().getItems().clear();
					SearchModel.getSearchResultsTable().getResults().setItems(FXCollections.observableArrayList(results));

					bottomPane.refreshBottomPanel();
					bottomPane.refreshTotalResultsSelectedLabel();
					
					if (splitPane.getItems().contains(taxonomyPane)) {
						ResultsToTaxonomy.resultsToSearchTaxonomy();
					}
					*/
				}
			} catch (Exception ex) {
				getSearchRunning().set(false);
				String title = "Unexpected Search Error";
				LOG.error(title, ex);
				AppContext.getCommonDialogs().showErrorDialog(title,
						"There was an unexpected error running the search",
						ex.toString(), AppContext.getMainApplicationWindow().getPrimaryStage());
				//searchResultsTable.getItems().clear();
				bottomPane.refreshBottomPanel();
				bottomPane.refreshTotalResultsSelectedLabel();
			} finally {
				getSearchRunning().set(false);
			}
		});
	}

	private Collection<CompositeSearchResult> createSingleEntryPerResults(
			Collection<CompositeSearchResult> results) {
		List<CompositeSearchResult> retList = new ArrayList<CompositeSearchResult>();
		
		for (CompositeSearchResult refConResult : results) {
			Optional<ConceptSnapshot> refCon = refConResult.getContainingConcept();
			
			for (IdentifiedObjectLocal c : refConResult.getMatchingComponents())
			{
				if (c instanceof DynamicSememe<?>)
				{
					DynamicSememe<?> rv = (DynamicSememe<?>)c;
					
					try
					{
						DynamicSememeColumnInfo[] ci = rv.getDynamicSememeUsageDescription().getColumnInfo();
						int i = 0;
						
						for (DynamicSememeDataBI data : rv.getData())
						{
							if (data == null)  //might be an unset column, if the col is optional
							{
								continue;
							}

							String attachedData;
							if (DynamicSememeDataType.BYTEARRAY == data.getRefexDataType()) {
								attachedData = ci[i].getColumnName()+  " - [Binary]";
							} else {
								attachedData = ci[i].getColumnName() + " - " + data.getDataObject().toString();
							}	

							SememeSearchResult result = new SememeSearchResult(refCon.get(), rv.getAssemblageSequence(), attachedData);
							retList.add(result);
							i++;
						}
					}
					catch (Exception e)
					{
						LOG.error("Unexpected error reading sememe info", e);
					}
				} else {
					/*
					concept;
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
					*/
				}
			}
		}
		
		return retList;
	}

	private void populateDynamicRefexList()
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

	@Override
	public String getValidationFailureMessage() {
		if (viewCoordinateProperty.get() == null) {
			return "View Coordinate is unset";
		} else if (searchText.getText().length() == 0) {
			return "Text parameter is unset or too short";
		} else {
			return null;
		}
	}

	public TextField getSearchText() {
		return searchText;
	}

	public HBox getSearchInRefexHBox() {
		return searchInRefexHBox;
	}

	public VBox getOptionsContentVBox() {
		return optionsContentVBox;		
	}

	public void setOptionsContentVBox(VBox vBox) {
		optionsContentVBox = vBox;
	}
}
