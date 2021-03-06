package gov.va.isaac.gui.enhancedsearchview.model;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.ihtsdo.otf.tcc.api.contradiction.ContradictionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import gov.va.isaac.AppContext;
import gov.va.isaac.ExtendedAppContext;
import gov.va.isaac.gui.dialog.UserPrompt.UserPromptResponse;
import gov.va.isaac.gui.enhancedsearchview.SearchConceptHelper;
import gov.va.isaac.gui.enhancedsearchview.SearchConceptHelper.SearchConceptException;
import gov.va.isaac.gui.enhancedsearchview.SearchDisplayConcept;
import gov.va.isaac.gui.enhancedsearchview.SearchTypeEnums.ComponentSearchType;
import gov.va.isaac.gui.enhancedsearchview.SearchTypeEnums.SearchType;
import gov.va.isaac.gui.enhancedsearchview.model.type.text.TextSearchTypeModel;
import gov.va.isaac.gui.enhancedsearchview.resulthandler.SaveSearchPrompt;
import gov.va.isaac.util.ComponentDescriptionHelper;
import gov.va.isaac.util.OTFUtility;
import gov.va.isaac.util.OchreUtility;
import gov.va.isaac.util.Utility;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.impl.sememe.DynamicSememeUsageDescription;
import gov.vha.isaac.ochre.model.constants.QueryBuilderConstants;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.ListCell;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;

//TODO saved search has not been repaired for ochre
public class EnhancedSavedSearch {
	public TextField getSearchSaveNameTextField() {
		return searchSaveNameTextField;
	}

	public TextField getSearchSaveDescriptionTextField() {
		return searchSaveDescriptionTextField;
	}

	public TextField getDroolsExprTextField() {
		return droolsExprTextField;
	}

	public ComboBox<SearchDisplayConcept> getSavedSearchesComboBox() {
		return savedSearchesComboBox;
	}
	
	private static Button saveSearchButton;
	private static Button restoreSearchButton;
	private static ComboBox<SearchDisplayConcept> savedSearchesComboBox;

	private static TextField searchSaveNameTextField;
	private static TextField searchSaveDescriptionTextField;
	private static TextField droolsExprTextField;

	private SearchModel searchModel = new SearchModel();
	private static final Logger LOG = LoggerFactory.getLogger(EnhancedSavedSearch.class);
	private static boolean searchTypeComboBoxChangeListenerSet = false;
	
	public EnhancedSavedSearch() {
		
		if (searchSaveNameTextField == null) {
			searchSaveNameTextField = new TextField();
		}
		if (searchSaveDescriptionTextField == null) {
			searchSaveDescriptionTextField = new TextField();
		}
		if (droolsExprTextField == null) {
			droolsExprTextField = new TextField();
		}

		initializeSavedSearchComboBox();
		
		saveSearchButton = new Button("Save Search");
		saveSearchButton.setPrefWidth(Control.USE_COMPUTED_SIZE);
		saveSearchButton.setMinWidth(Control.USE_PREF_SIZE);

		saveSearchButton.setOnAction((e) -> {
			saveSearch();
		});
		
		restoreSearchButton = new Button("Restore Search");
		restoreSearchButton.setPrefWidth(Control.USE_COMPUTED_SIZE);
		restoreSearchButton.setMinWidth(Control.USE_PREF_SIZE);

		restoreSearchButton.setOnAction((e) -> {
			loadSavedSearch(); 
		});
		restoreSearchButton.setDisable(true);
		savedSearchesComboBox.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<SearchDisplayConcept>() {
			@Override
			public void changed(
					ObservableValue<? extends SearchDisplayConcept> observable,
					SearchDisplayConcept oldValue, SearchDisplayConcept newValue) {
				restoreSearchButton.setDisable(newValue == null);
			}
		});
		

		
		if (! searchTypeComboBoxChangeListenerSet) {
			SearchModel.getSearchTypeSelector().getSearchTypeComboBox().getSelectionModel().selectedItemProperty().addListener(new ChangeListener<SearchType>() {
				@Override
				public void changed(
						ObservableValue<? extends SearchType> observable,
						SearchType oldValue, SearchType newValue) {
					if (oldValue != newValue) {
						refreshSavedSearchComboBox();
					}
				}
			});

			searchTypeComboBoxChangeListenerSet = true;
		}
	}
	
	public void loadSavedSearch() {
		SearchDisplayConcept searchToRestore = savedSearchesComboBox.getSelectionModel().getSelectedItem();
		loadSavedSearch(searchToRestore);
	}

	public static void loadSavedSearch(SearchDisplayConcept searchToRestore) {
		LOG.info("loadSavedSearch(" + searchToRestore + ")");

		SearchModel model = null;
		try {
			model = SearchConceptHelper.loadSavedSearch(searchToRestore);
			
			SearchType currentType = SearchModel.getSearchTypeSelector().getCurrentType();
			SearchModel.getSearchTypeSelector().getSearchTypeComboBox().getSelectionModel().select(null);
			SearchModel.getSearchTypeSelector().getSearchTypeComboBox().getSelectionModel().select(currentType);
		} catch (SearchConceptException e) {
			LOG.error("Failed loading saved search. Caught " + e.getClass().getName() + " \"" + e.getLocalizedMessage() + "\"");
			e.printStackTrace();

			String title = "Failed loading saved search";
			String msg = "Cannot load existing saved search \"" + searchToRestore + "\"";
			String details = "Caught " + e.getClass().getName() + " \"" + e.getLocalizedMessage() + "\"." + "\n" + "model:" + model;
			AppContext.getCommonDialogs().showErrorDialog(title, msg, details, AppContext.getMainApplicationWindow().getPrimaryStage());

			return;
		}

	}
	
	public void saveSearch() {
		if (! SearchModel.getSearchTypeSelector().getTypeSpecificModel().isSavableSearch()) {
			AppContext.getCommonDialogs().showErrorDialog("Save of Search Failed", "Search is not savable", SearchModel.getSearchTypeSelector().getTypeSpecificModel().getSearchSavabilityValidationFailureMessage());
		} else {
			LOG.debug("saveSearch() called.  Search specified: " + savedSearchesComboBox.valueProperty().getValue());
			// Save Search popup
			try {
//				String searchName = getSaveSearchRequest();
				throw new RuntimeException("Save disabled");
//
//				if (searchName != null) {
//					AppContext.getCommonDialogs().showInformationDialog("Search Successfully Saved", "Saved new search:" + searchName);
//				}
			} catch (Exception e) {
				LOG.error("Failed saving search.", e);
				AppContext.getCommonDialogs().showErrorDialog("Save Search Failure", "Save Search Failure", "Failed to save new search\n\n" + e.getLocalizedMessage()); 
			}
		}
	}

//	private String getSaveSearchRequest() throws SearchConceptException {
//		SaveSearchPrompt prompt = new SaveSearchPrompt();
//		prompt.showUserPrompt(AppContext.getMainApplicationWindow().getPrimaryStage(), "Define Refset");
//
//
//		if (prompt.getButtonSelected() == UserPromptResponse.APPROVE) {
//			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd @ HH:mm:ss");
//			LocalDateTime dateTime = LocalDateTime.now();
//			String formattedDateTime = dateTime.format(formatter);
//			String user = ExtendedAppContext.getCurrentlyLoggedInUserProfile().getUserLogonName();
//			final String nameToSave = prompt.getNameTextField().getText() + " by " + user + " on " + formattedDateTime;
//
//			SearchConceptHelper.buildAndSaveSearchConcept(searchModel, nameToSave, prompt.getDescTextField().getText());
//			refreshSavedSearchComboBox();
//
//			return prompt.getNameTextField().getText();
//		}
//		
//		return null;
//	}

	private void initializeSavedSearchComboBox() {
		savedSearchesComboBox = new ComboBox<SearchDisplayConcept>();

		// Force single selection
		savedSearchesComboBox.getSelectionModel().selectFirst();
		savedSearchesComboBox.setPrefWidth(250);
		savedSearchesComboBox.setCellFactory((p) -> {
			final ListCell<SearchDisplayConcept> cell = new ListCell<SearchDisplayConcept>() {
				@Override
				protected void updateItem(SearchDisplayConcept c, boolean emptyRow) {
					super.updateItem(c, emptyRow);

					if(c == null) {
						setText(null);
					}else {
						setText(c.getFullySpecifiedName());
						Tooltip tooltip = new Tooltip(c.getPreferredTerm());
						Tooltip.install(this, tooltip);
					}
				}
			};

			return cell;
		});
		savedSearchesComboBox.setButtonCell(new ListCell<SearchDisplayConcept>() {
			@Override
			protected void updateItem(SearchDisplayConcept c, boolean emptyRow) {
				super.updateItem(c, emptyRow); 
				if (emptyRow) {
					setText("");
				} else {
					setText(c.getFullySpecifiedName());
					Tooltip tooltip = new Tooltip(c.getPreferredTerm());
					Tooltip.install(this, tooltip);
				}
			}
		});
		
		refreshSavedSearchComboBox();
	}

	
	private static ObservableList<SearchDisplayConcept> getSavedSearches() {
		ObservableList<SearchDisplayConcept> searches = FXCollections.observableList(new ArrayList<>());
		
		try {
			throw new RuntimeException("Saved searches disabled");
//			Set<Integer> savedSearches = OchreUtility.getAllChildrenOfConcept(QueryBuilderConstants.STORED_QUERIES.getConceptSequence(), true, false);
//
//			SearchType currentSearchType = SearchModel.getSearchTypeSelector().getSearchTypeComboBox().getSelectionModel().getSelectedItem();
//			for (Integer conceptSeq : savedSearches) {
//				int nid = Get.identifierService().getConceptNid(conceptSeq);
//				if (getCachedSearchTypeFromSearchConcept(nid) == currentSearchType) {
//					boolean addSearchToList = true;
//					if (currentSearchType == SearchType.TEXT) {
//						ComponentSearchType currentlyViewedComponentSearchType = TextSearchTypeModel.getCurrentComponentSearchType();
//						ComponentSearchType loadedComponentSearchType = getCachedComponentSearchTypeFromSearchConcept(nid);
//						
//						if (currentlyViewedComponentSearchType != null && loadedComponentSearchType != null) {
//							if (currentlyViewedComponentSearchType != loadedComponentSearchType) {
//								addSearchToList = false;
//							}
//						}
//					}
//					
//					if (addSearchToList) {
//						String fsn = OchreUtility.getFSNForConceptNid(nid, null).get();
//						String preferredTerm = OchreUtility.getPreferredTermForConceptNid(nid, null).get();
//						searches.add(new SearchDisplayConcept(fsn, preferredTerm, nid));
//					}
//				}
//			}
		} catch (Exception e) {
			LOG.error("Error reading saved searches", e);
		}

		return searches;
	}
	
	public static void refreshSavedSearchComboBox() {
		Task<List<SearchDisplayConcept>> loadSavedSearches = new Task<List<SearchDisplayConcept>>() {
			private ObservableList<SearchDisplayConcept> searches = FXCollections.observableList(new ArrayList<>());

			@Override
			protected List<SearchDisplayConcept> call() throws Exception {
				searches = getSavedSearches();
				return searches;
				/*
				Set<ConceptVersionBI> savedSearches = OTFUtility.getAllChildrenOfConcept(Search.STORED_QUERIES.getNid(), true);

				SearchType currentSearchType = SearchModel.getSearchTypeSelector().getSearchTypeComboBox().getSelectionModel().getSelectedItem();
				for (ConceptVersionBI concept : savedSearches) {
					if (getCachedSearchTypeFromSearchConcept(concept) == currentSearchType) {
						boolean addSearchToList = true;
						if (currentSearchType == SearchType.TEXT) {
							ComponentSearchType currentlyViewedComponentSearchType = TextSearchTypeModel.getCurrentComponentSearchType();
							ComponentSearchType loadedComponentSearchType = getCachedComponentSearchTypeFromSearchConcept(concept);
							
							if (currentlyViewedComponentSearchType != null && loadedComponentSearchType != null) {
								if (currentlyViewedComponentSearchType != loadedComponentSearchType) {
									addSearchToList = false;
								}
							}
						}
						
						if (addSearchToList) {
							String fsn = OTFUtility.getFullySpecifiedName(concept);
							String preferredTerm = OTFUtility.getConPrefTerm(concept.getNid());
							searches.add(new SearchDisplayConcept(fsn, preferredTerm, concept.getNid()));
						}
					}
				}

				return searches;
				*/
			}

			@Override
			protected void succeeded() {
				super.succeeded();

				savedSearchesComboBox.setItems(searches);
			}
		};

		savedSearchesComboBox.getItems().clear();
		Utility.execute(loadSavedSearches);
	}
	
	public static void refreshSavedSearchMenu(Menu menu) {
		Utility.execute(() -> {
			ObservableList<SearchDisplayConcept> searches = getSavedSearches();
			Platform.runLater(() -> {
				menu.getItems().clear();
				for (SearchDisplayConcept search : searches) {
					MenuItem item = new MenuItem(search.getFullySpecifiedName());
					item.setOnAction((e) -> {
						loadSavedSearch(search);
					});
					menu.getItems().add(item);
				}
			});
		});
	}

	void refreshSearchViewModelBindings() {
		Bindings.bindBidirectional(searchSaveNameTextField.textProperty(), SearchModel.getSearchTypeSelector().getTypeSpecificModel().getNameProperty());
		Bindings.bindBidirectional(searchSaveDescriptionTextField.textProperty(), SearchModel.getSearchTypeSelector().getTypeSpecificModel().getDescriptionProperty());

		Bindings.bindBidirectional(searchModel.getMaxResultsCustomTextField().valueProperty(), SearchModel.getSearchTypeSelector().getTypeSpecificModel().getMaxResultsProperty());

		Bindings.bindBidirectional(droolsExprTextField.textProperty(), SearchModel.getSearchTypeSelector().getTypeSpecificModel().getDroolsExprProperty());
	}

	public Button getSaveButton() {
 		return saveSearchButton;
	}

	public Button getRestoreSearchButton() {
		return restoreSearchButton;
	}
	
	private static Map<Integer, SearchType> conceptSearchTypeCache = new HashMap<>();
//	private static SearchType getCachedSearchTypeFromSearchConcept(Integer conNid) throws IOException {
//		synchronized (conceptSearchTypeCache) {
//			if (conceptSearchTypeCache.get(conNid) == null) {
//				conceptSearchTypeCache.put(conNid, getSearchTypeFromSearchConcept(conNid));
//			}
//		}
//
//		return conceptSearchTypeCache.get(conNid);
//	}
//	private static Set<Integer> badSearchConceptsToIgnore = new HashSet<>();
//	private static SearchType getSearchTypeFromSearchConcept(Integer conNid) throws IOException {
//		synchronized (badSearchConceptsToIgnore) {
//			if (badSearchConceptsToIgnore.contains(conNid)) {
//				LOG.debug("Ignoring invalid/unsupported search filter concept nid=" + conNid + ", desc=\"" + ComponentDescriptionHelper.getComponentDescription(conNid) + "\"");
//			
//				return null;
//			}
//		}
//
//		Collection<? extends DynamicSememeVersionBI<?>> refexes = OTFUtility.getConceptVersion(conNid).getRefexesDynamicActive(OTFUtility.getViewCoordinate());
//		for (DynamicSememeVersionBI<?> refex : refexes) {
//			DynamicSememeUsageDescription dud = null;
//			try {
//				dud = refex.getDynamicSememeUsageDescription();
//			} catch (IOException | ContradictionException e) {
//				LOG.error("Failed performing getDynamicSememeUsageDescription(): caught " + e.getClass().getName() + " \"" + e.getLocalizedMessage() + "\"", e);
//
//				return null;
//			}
//
//			if (dud.getRefexName().equals(QueryBuilderConstants.SEARCH_LUCENE_FILTER.getConceptDescriptionText())) {
//				return SearchType.TEXT;
//			} else if (dud.getRefexName().equals(QueryBuilderConstants.SEARCH_REGEXP_FILTER.getConceptDescriptionText())) {
//				return SearchType.TEXT;
//			} else if (dud.getRefexName().equals(QueryBuilderConstants.SEARCH_SEMEME_CONTENT_FILTER.getConceptDescriptionText())) {
//				return SearchType.SEMEME;
//			} else {
//				LOG.debug("getSearchTypeFromSearchConcept() ignoring refex \"" + dud.getRefexName() + "\" on search filter concept nid=" + conNid 
//						+ ", desc=\"" + ComponentDescriptionHelper.getComponentDescription(conNid) + "\""); 
//			}
//		}
//		
//		//String warn = "Automatically RETIRING invalid/unsupported search filter concept nid=" + concept.getConceptNid() + ", status=" + concept.getStatus() + ", uuid=" + concept.getPrimordialUuid() + ", desc=\"" + ComponentDescriptionHelper.getComponentDescription(concept) + "\"";
//		String warn = "Invalid/unsupported search filter concept nid=" + conNid+ ", desc=\"" + ComponentDescriptionHelper.getComponentDescription(conNid) + "\"";
//
//		LOG.warn(warn);
//		
//		synchronized (badSearchConceptsToIgnore) {
//			// Retire Concept for bad search refex
////			RuntimeGlobalsI globals = AppContext.getService(RuntimeGlobalsI.class);
//			try {
//				// disable WorkflowInitiationPropertyChangeListener
////				globals.disableAllCommitListeners();
//
//				// TODO: Make retirement of bad search concepts work: https://csfe.aceworkspace.net/sf/go/artf231405
////				ConceptAttributeAB cab = new ConceptAttributeAB(concept.getConceptNid(), /* concept.getConceptAttributesActive().isDefined() */ true, RefexDirective.EXCLUDE);
////				ConceptAttributeChronicleBI cabi = OTFUtility.getBuilder().constructIfNotCurrent(cab);
////				
////				//ConceptCB cab = concept.makeBlueprint(OTFUtility.getViewCoordinate(), IdDirective.PRESERVE, RefexDirective.INCLUDE);
////				//ConceptChronicleBI cabi = OTFUtility.getBuilder().constructIfNotCurrent(cab);
////				
////				cab.setStatus(Status.INACTIVE);
////				
////				OTFUtility.addUncommitted(cabi.getEnclosingConcept());
////				
////				// Commit
////				OTFUtility.commit(concept);
//			} catch (Exception e) {
//				String error = "FAILED to automatically retire invalid/unsupported search filter concept nid=" + conNid 
//						+ ", desc=\"" + ComponentDescriptionHelper.getComponentDescription(conNid) + "\".  Caught " + e.getClass().getName() + " " + e.getLocalizedMessage();
//				LOG.error(error, e);
//				e.printStackTrace();
//			} finally {
////				globals.enableAllCommitListeners();
//
//				badSearchConceptsToIgnore.add(conNid);
//			}
//		}
//		return null;
//	}
//	
//	private static Map<Integer, ComponentSearchType> componentSearchTypeCache = new HashMap<>();
//	private static ComponentSearchType getCachedComponentSearchTypeFromSearchConcept(Integer conNid) throws IOException {
//		synchronized (componentSearchTypeCache) {
//			if (componentSearchTypeCache.get(conNid) == null) {
//				componentSearchTypeCache.put(conNid, getComponentSearchTypeFromSearchConcept(conNid));
//			}
//		}
//		
//		return componentSearchTypeCache.get(conNid);
//	}
//	private static ComponentSearchType getComponentSearchTypeFromSearchConcept(Integer conNid) throws IOException {
//		Collection<? extends DynamicSememeVersionBI<?>> refexes = OTFUtility.getConceptVersion(conNid).getRefexesDynamicActive(OTFUtility.getViewCoordinate());
//		for (DynamicSememeVersionBI<?> refex : refexes) {
//			DynamicSememeUsageDescription dud = null;
//			try {
//				dud = refex.getDynamicSememeUsageDescription();
//			} catch (IOException | ContradictionException e) {
//				LOG.error("Failed performing getDynamicSememeUsageDescription(): caught " + e.getClass().getName() + " \"" + e.getLocalizedMessage() + "\"", e);
//
//				return null;
//			}
//
//			if (dud.getRefexName().equals(QueryBuilderConstants.SEARCH_LUCENE_FILTER.getConceptDescriptionText())) {
//				return ComponentSearchType.LUCENE;
//			} else if (dud.getRefexName().equals(QueryBuilderConstants.SEARCH_REGEXP_FILTER.getConceptDescriptionText())) {
//				return ComponentSearchType.REGEXP;
//			}
//		}
//		
//		String error = "Invalid/unsupported search filter concept nid=" + conNid + ", desc=\"" + ComponentDescriptionHelper.getComponentDescription(conNid) + "\"";
//		LOG.error(error);
//		return null;
//	}
}
