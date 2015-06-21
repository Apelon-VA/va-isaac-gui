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
package gov.va.isaac.gui.preferences.plugins;

import gov.va.isaac.AppContext;
import gov.va.isaac.ExtendedAppContext;
import gov.va.isaac.config.generated.StatedInferredOptions;
import gov.va.isaac.config.profiles.UserProfile;
import gov.va.isaac.config.profiles.UserProfileDefaults;
import gov.va.isaac.config.profiles.UserProfileManager;
import gov.va.isaac.config.users.InvalidUserException;
import gov.va.isaac.gui.util.TextErrorColorHelper;
import gov.va.isaac.util.OTFUtility;
import gov.va.isaac.util.ValidBooleanBinding;
import gov.vha.isaac.metadata.source.IsaacMetadataAuxiliaryBinding;
import gov.vha.isaac.ochre.api.commit.CommitService;

import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlySetProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleSetProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import javafx.util.StringConverter;

import org.apache.commons.lang3.time.DateUtils;
import org.ihtsdo.otf.tcc.api.concept.ConceptChronicleBI;
import org.ihtsdo.otf.tcc.api.concept.ConceptVersionBI;
import org.ihtsdo.otf.tcc.api.contradiction.ContradictionException;
import org.ihtsdo.otf.tcc.api.coordinate.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.javafx.collections.ObservableSetWrapper;

/**
 * {@link ViewCoordinatePreferencesPluginViewController}
 *
 * @author <a href="mailto:joel.kniaz@gmail.com">Joel Kniaz</a>
 */

public class ViewCoordinatePreferencesPluginViewController
{
	private final static Logger log = LoggerFactory.getLogger(ViewCoordinatePreferencesPluginViewController.class);

	private static final UUID moduleRootUuid = UUID.fromString("96ca29b8-a934-5abd-8d4e-0ee6aeaba520");

	private boolean contentLoaded = false;

	@FXML VBox vBoxInTab;
	@FXML GridPane topGridPane;
	@FXML GridPane bottomGridPane;
	@FXML DatePicker datePicker;
	@FXML ComboBox<UUID> pathComboBox;
	@FXML ListView<SelectableModule> selectableModuleListView;
	@FXML VBox statusesToggleGroupVBox;
	@FXML VBox statedInferredToggleGroupVBox;

	private ValidBooleanBinding allValid_ = null;

	private ToggleGroup statusesToggleGroup = null;
	private ToggleGroup statedInferredToggleGroup = null;

	
	private final ObjectProperty<StatedInferredOptions> currentStatedInferredOptionProperty = new SimpleObjectProperty<>();
	private final ObjectProperty<UUID> currentPathProperty = new SimpleObjectProperty<>();
	private final ObjectProperty<Long> currentTimeProperty = new SimpleObjectProperty<>();
	private final SimpleSetProperty<Status> currentStatusesProperty = new SimpleSetProperty<>(new ObservableSetWrapper<Status>(new HashSet<Status>()));

	private final List<RadioButton> statedInferredOptionButtons = new ArrayList<>();
	
	private RadioButton activeStatusButton;
	private RadioButton inactiveStatusButton;
	private RadioButton activeAndInactiveStatusButton;

	private TreeSet<Long> times = new TreeSet<Long>();
	private HashSet<LocalDate> pathDatesList = new HashSet<LocalDate>();
	private Date stampDate = null;
//	private SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy");
//	private DateTimeFormatter dtf = DateTimeFormatter.ofPattern("d/M/y");
//	private SimpleDateFormat regularDateFormat = new SimpleDateFormat("yyyy-MM-dd");
	private HashMap<Long, Long> truncTimeToFullTimeMap = new HashMap<Long, Long>();
//	private SimpleDateFormat timeFormatter = new SimpleDateFormat("hh:mm:ss a");
	private LocalDate stampDateInstant = null;
	private Long storedTimePref = null;
	private UUID storedPathPref = null;

	private boolean datePickerFirstRun = false; //This will probably need to go
	private boolean pathComboFirstRun = false;

	private Long overrideTimestamp;

	private ObservableList<SelectableModule> selectableModules = null;
	private ObservableSet<UUID> selectedModules = FXCollections.observableSet(new HashSet<UUID>());

	protected static ViewCoordinatePreferencesPluginViewController construct() throws IOException
	{
		// Load from FXML.
		URL resource = ViewCoordinatePreferencesPluginViewController.class.getResource("ViewCoordinatePreferencesPluginView.fxml");
		FXMLLoader loader = new FXMLLoader(resource);
		loader.load();
		return loader.getController();
	}

	@FXML
	void initialize()
	{
		initializeDateSelectorComboBox();
		initializePathComboBox();
		initializeSelectableModuleListView();
		initializeStatusesToggleGroup();
		initializeStatedInferredToggleGroup();
		initializeValidBooleanBinding();
	}

	private void initializeDateSelectorComboBox() {
		//Calendar Date Picker
		final Callback<DatePicker, DateCell> dayCellFactory = 
				new Callback<DatePicker, DateCell>() {
			@Override
			public DateCell call(final DatePicker datePicker) {
				return new DateCell() {
					@Override
					public void updateItem(LocalDate thisDate, boolean empty) {
						super.updateItem(thisDate, empty);
						if(pathDatesList != null) {
							if(pathDatesList.contains(thisDate)) { 
								setDisable(false); 
							} else {
								setDisable(true);
							}
						}
					}
				};
			}
		};
		datePicker.setDayCellFactory(dayCellFactory);
		datePicker.setOnAction((event) -> {
			if(!datePickerFirstRun) {
				UUID selectedPath = pathComboBox.getSelectionModel().getSelectedItem();

				Instant instant = Instant.from(datePicker.getValue().atStartOfDay(ZoneId.systemDefault()));
				Long dateSelected = Date.from(instant).getTime();

				if(selectedPath != null && dateSelected != 0) {

					int path = OTFUtility.getConceptVersion(selectedPath).getPathNid();
					setTimeOptions(path, dateSelected);
					try {
						//							timeSelectCombo.setValue(times.first()); //Default Dropdown Value
						currentTimeProperty.set(times.first());
					} catch(Exception e) {
						// Eat it.. like a sandwich! TODO: Create Read Only Property Conditional for checking if Time Combo is disabled
						// Right now, Sometimes Time Combo is disabled, so we catch this and eat it
						// Otherwise make a conditional from the Read Only Boolean Property to check first
					}
				} else {
					log.debug("The path isn't set or the date isn't set. Both are needed right now");
				}
			} else {
				datePickerFirstRun = false;
			}
		});
	}
	
	private void initializePathComboBox() {
		//Path Combo Box
		pathComboBox.setCellFactory(new Callback<ListView<UUID>, ListCell<UUID>> () {
			@Override
			public ListCell<UUID> call(ListView<UUID> param) {
				final ListCell<UUID> cell = new ListCell<UUID>() {
					@Override
					protected void updateItem(UUID c, boolean emptyRow) {
						super.updateItem(c, emptyRow);
						if(c == null) {
							setText(null);
						} else {
							String desc = OTFUtility.getDescription(c);
							setText(desc);
						}
					}
				};
				return cell;
			}
		});
		pathComboBox.setButtonCell(new ListCell<UUID>() {
			@Override
			protected void updateItem(UUID c, boolean emptyRow) {
				super.updateItem(c, emptyRow); 
				if (emptyRow) {
					setText("");
				} else {
					String desc = OTFUtility.getDescription(c);
					setText(desc);
				}
			}
		});
		pathComboBox.setOnAction((event)-> {
			if(!pathComboFirstRun) {
				UUID selectedPath = pathComboBox.getSelectionModel().getSelectedItem();
				if(selectedPath != null) {

					CommitService stampDb = AppContext.getService(CommitService.class);

					//TODO: Make this multi-threaded and possibly implement setTimeOptions() here also
					//TODO OCHRE use the right path here
					//int path = OTFUtility.getConceptVersion(selectedPath).getPathNid();
					IntStream stamps = getStamps(IsaacMetadataAuxiliaryBinding.DEVELOPMENT.getSequence(), Long.MIN_VALUE, Long.MAX_VALUE);

					pathDatesList.clear(); 
					currentTimeProperty.set(Long.MAX_VALUE);
					stamps.forEach((thisStamp) ->
					{
						try {
							this.stampDate = new Date(stampDb.getTimeForStamp(thisStamp));
							stampDateInstant = stampDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
							this.pathDatesList.add(stampDateInstant); //Build DatePicker
						} catch (Exception e) {
							e.printStackTrace();  //TODO OCHRE fix!
						}
					});

					datePicker.setValue(LocalDate.now());
				}
			} else {
				pathComboFirstRun = false;
			}
		});
		currentPathProperty.bind(pathComboBox.getSelectionModel().selectedItemProperty());
	}
	
	private void initializeSelectableModuleListView() {
		selectableModuleListView.setCellFactory(CheckBoxListCell.forListView(SelectableModule::selectedProperty, new StringConverter<SelectableModule>() {
			@Override
			public String toString(SelectableModule object) {
				return object.getDescription();
			}

			@Override
			public SelectableModule fromString(String string) {
				return null;
			}
		}));
	}

	private void initializeStatusesToggleGroup() {
		statusesToggleGroup = new ToggleGroup();
		Tooltip statusButtonsTooltip = new Tooltip("Default Status(es) is " + getDefaultViewCoordinateStatuses());
		activeStatusButton = new RadioButton();
		activeStatusButton.setText("Active");
		activeStatusButton.setTooltip(statusButtonsTooltip);
		statusesToggleGroup.getToggles().add(activeStatusButton);
		activeStatusButton.selectedProperty().addListener(new ChangeListener<Boolean> () {
			@Override
			public void changed(
					ObservableValue<? extends Boolean> observable,
					Boolean oldValue, Boolean newValue) {
				if (newValue) {
					currentStatusesProperty.get().clear();
					currentStatusesProperty.add(Status.ACTIVE);
				}
			}
		});
		statusesToggleGroupVBox.getChildren().add(activeStatusButton);

		inactiveStatusButton = new RadioButton();
		inactiveStatusButton.setText("Inactive");
		inactiveStatusButton.setTooltip(statusButtonsTooltip);
		statusesToggleGroup.getToggles().add(inactiveStatusButton);
		inactiveStatusButton.selectedProperty().addListener(new ChangeListener<Boolean> () {
			@Override
			public void changed(
					ObservableValue<? extends Boolean> observable,
					Boolean oldValue, Boolean newValue) {
				if (newValue) {
					currentStatusesProperty.get().clear();
					currentStatusesProperty.add(Status.INACTIVE);
				}
			}
		});
		statusesToggleGroupVBox.getChildren().add(inactiveStatusButton);

		activeAndInactiveStatusButton = new RadioButton();
		activeAndInactiveStatusButton.setText("All");
		activeAndInactiveStatusButton.setTooltip(statusButtonsTooltip);
		statusesToggleGroup.getToggles().add(activeAndInactiveStatusButton);
		activeAndInactiveStatusButton.selectedProperty().addListener(new ChangeListener<Boolean> () {
			@Override
			public void changed(
					ObservableValue<? extends Boolean> observable,
					Boolean oldValue, Boolean newValue) {
				if (newValue) {
					currentStatusesProperty.get().clear();
					currentStatusesProperty.add(Status.ACTIVE);
					currentStatusesProperty.add(Status.INACTIVE);
				}
			}
		});
		statusesToggleGroupVBox.getChildren().add(activeAndInactiveStatusButton);
	}

	private void initializeStatedInferredToggleGroup() {
		statedInferredToggleGroup = new ToggleGroup(); //Stated / Inferred

		for (StatedInferredOptions option : StatedInferredOptions.values()) {
			RadioButton optionButton = new RadioButton();
			if (option == StatedInferredOptions.STATED)
			{
				optionButton.setText("Stated");
			}
			else if (option == StatedInferredOptions.INFERRED)
			{
				optionButton.setText("Inferred");
			}
			else
			{
				throw new RuntimeException("oops");
			}
			optionButton.setUserData(option);
			optionButton.setTooltip(new Tooltip("Default StatedInferredOption is " + getDefaultStatedInferredOption()));
			statedInferredToggleGroup.getToggles().add(optionButton);
			statedInferredToggleGroupVBox.getChildren().add(optionButton);
			statedInferredOptionButtons.add(optionButton);
		}
		statedInferredToggleGroup.selectedToggleProperty().addListener(new ChangeListener<Toggle>() {
			@Override
			public void changed(
					ObservableValue<? extends Toggle> observable,
					Toggle oldValue, Toggle newValue) {
				currentStatedInferredOptionProperty.set((StatedInferredOptions)newValue.getUserData());
			}	
		});
	}
	
	private void initializeValidBooleanBinding() {
		allValid_ = new ValidBooleanBinding() {
			{
				bind(currentStatedInferredOptionProperty, currentPathProperty, currentTimeProperty, currentStatusesProperty);
				setComputeOnInvalidate(true);
			}

			@Override
			protected boolean computeValue() {
				if (currentStatedInferredOptionProperty.get() == null) {
					this.setInvalidReason("Null/unset/unselected StatedInferredOption");
					for (RadioButton button : statedInferredOptionButtons) {
						TextErrorColorHelper.setTextErrorColor(button);
					}
					return false;
				} else {
					for (RadioButton button : statedInferredOptionButtons) {
						TextErrorColorHelper.clearTextErrorColor(button);
					}
				}
				if (currentPathProperty.get() == null) {
					this.setInvalidReason("Null/unset/unselected path");
					TextErrorColorHelper.setTextErrorColor(pathComboBox);

					return false;
				} else {
					TextErrorColorHelper.clearTextErrorColor(pathComboBox);
				}
				if (OTFUtility.getConceptVersion(currentPathProperty.get()) == null) {
					this.setInvalidReason("Invalid path");
					TextErrorColorHelper.setTextErrorColor(pathComboBox);

					return false;
				} else {
					TextErrorColorHelper.clearTextErrorColor(pathComboBox);
				}
				if (currentStatusesProperty.get() == null || currentStatusesProperty.get().size() < 1) {
					this.setInvalidReason("Status unset");
					TextErrorColorHelper.setTextErrorColor(activeStatusButton);
					TextErrorColorHelper.setTextErrorColor(inactiveStatusButton);
					TextErrorColorHelper.setTextErrorColor(activeAndInactiveStatusButton);

					return false;
				} else {
					TextErrorColorHelper.clearTextErrorColor(activeStatusButton);
					TextErrorColorHelper.clearTextErrorColor(inactiveStatusButton);
					TextErrorColorHelper.clearTextErrorColor(activeAndInactiveStatusButton);
				}
				//					if(currentTimeProperty.get() == null && currentTimeProperty.get() != Long.MAX_VALUE)
				//					{
				//						this.setInvalidReason("View Coordinate Time is unselected");
				//						TextErrorColorHelper.setTextErrorColor(timeSelectCombo);
				//						return false;
				//					}
				this.clearInvalidReason();
				return true;
			}
		};
	}

	public ReadOnlyStringProperty validationFailureMessageProperty() {
		return allValid_.getReasonWhyInvalid();
	}

	public void save() throws IOException {
		log.debug("Saving ViewCoordinatePreferencesPluginView data");
		UserProfile loggedIn = ExtendedAppContext.getCurrentlyLoggedInUserProfile();

		//Path Property
		log.debug("Setting stored VC path (currently \"{}\") to {}", loggedIn.getViewCoordinatePath(), currentPathProperty().get()); 
		loggedIn.setViewCoordinatePath(currentPathProperty().get());

		//Stated/Inferred Policy
		log.debug("Setting stored VC StatedInferredPolicy (currently \"{}\") to {}", loggedIn.getStatedInferredPolicy(), currentStatedInferredOptionProperty().get()); 
		loggedIn.setStatedInferredPolicy(currentStatedInferredOptionProperty().get());

		//Time Property
		log.debug("Setting stored VC time to :" + currentViewCoordinateTimeProperty().get());
		loggedIn.setViewCoordinateTime(currentViewCoordinateTimeProperty().get());

		//Statuses Property
		log.debug("Setting stored VC statuses to :" + currentViewCoordinateStatusesProperty().get());
		loggedIn.setViewCoordinateStatuses(currentViewCoordinateStatusesProperty().get());

		//Modules Property
		log.debug("Setting stored VC modules to :" + selectedModules);
		loggedIn.setViewCoordinateModules(selectedModules);

		if (overrideTimestamp != null) {
			loggedIn.setViewCoordinateTime(overrideTimestamp);
		}
		try {
			AppContext.getService(UserProfileManager.class).saveChanges(loggedIn);
		} catch (InvalidUserException e) {
			String msg = "Caught " + e.getClass().getName() + " " + e.getLocalizedMessage() + " attempting to save UserProfile for " + getClass().getName();

			log.error(msg, e);
			throw new IOException(msg, e);
		}
	}

	public static Date getEndOfDay(Date date) {
		return DateUtils.addMilliseconds(DateUtils.ceiling(date, Calendar.DATE), -1);
	}

	public static Date getStartOfDay(Date date) {
		return DateUtils.truncate(date, Calendar.DATE);
	}

	public Region getContent() {
		if (! contentLoaded) {
			contentLoaded = true;

			// Populate selectableModules
			ConceptVersionBI moduleRootConcept = OTFUtility.getConceptVersion(moduleRootUuid);
			Set<ConceptVersionBI> moduleConcepts = new HashSet<>();
			try {
				moduleConcepts.addAll(OTFUtility.getAllChildrenOfConcept(moduleRootConcept.getNid(), false));
			} catch (IOException | ContradictionException e1) {
				e1.printStackTrace();
			}
			List<SelectableModule> modules = new ArrayList<>();
			for (ConceptVersionBI cv : moduleConcepts) {
				modules.add(new SelectableModule(cv.getNid()));
			}
			selectableModules = FXCollections.observableArrayList(modules);

			selectableModuleListView.getItems().addAll(selectableModules);
			selectableModules.forEach(selectableModule -> selectableModule.selectedProperty().addListener((observable, wasSelected, isSelected) -> {
				if (isSelected) {
					if (! wasSelected) {
						log.debug("Adding module nid={}, uuid={}, desc={}", selectableModule.getNid(), selectableModule.getUuid(), selectableModule.getDescription());
						selectedModules.add(selectableModule.getUuid());
					}
				} else {
					if (wasSelected) {
						log.debug("Removing module nid={}, uuid={}, desc={}", selectableModule.getNid(), selectableModule.getUuid(), selectableModule.getDescription());
						selectedModules.remove(selectableModule.getUuid());
					}
				}
			}));

			pathComboBox.setTooltip(new Tooltip("Default path is \"" + OTFUtility.getDescription(getDefaultPath()) + "\""));

			// DEFAULT VALUES
			UserProfile loggedIn = ExtendedAppContext.getCurrentlyLoggedInUserProfile();
			storedTimePref = loggedIn.getViewCoordinateTime();
			storedPathPref = loggedIn.getViewCoordinatePath();

			if(storedPathPref != null) {
				pathComboBox.getItems().clear(); //Set the path Dates by default
				pathComboBox.getItems().addAll(getPathOptions());
				final UUID storedPath = getStoredPath();
				if(storedPath != null) {
					pathComboBox.getSelectionModel().select(storedPath);
				}

				if(storedTimePref != null) {
					//final Long storedTime = loggedIn.getViewCoordinateTime();
					Calendar cal = Calendar.getInstance();
					cal.setTime(new Date(storedTimePref));
					cal.set(Calendar.MILLISECOND, 0); //Strip milliseconds
					//					Long storedTruncTime = cal.getTimeInMillis();

					if(!storedTimePref.equals(Long.MAX_VALUE)) { //***** FIX THIS, not checking default vc time value
						int path = OTFUtility.getConceptVersion(storedPathPref).getPathNid();
						setTimeOptions(path, storedTimePref);
						//						timeSelectCombo.setValue(storedTruncTime);
						//						timeSelectCombo.getItems().addAll(getTimeOptions()); //The correct way, but doesen't work

						Date storedDate = new Date(storedTimePref);
						datePicker.setValue(storedDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
					} else {
						datePicker.setValue(LocalDate.now());
						//						timeSelectCombo.getItems().addAll(Long.MAX_VALUE); //The correct way, but doesen't work
						//						timeSelectCombo.setValue(Long.MAX_VALUE);
						//						disableTimeCombo(false);
					}
				} else { //Stored Time Pref == null
					log.error("ERROR: Stored Time Preference = null");
				}
			} else { //Stored Path Pref == null
				log.error("We could not load a stored path, ISAAC cannot run");
				throw new Error("No stored PATH could be found. ISAAC can't run without a path");
			}

			// FOR DEBUGGING CURRENTLY SELECTED PATH, TIME AND POLICY
			/*			
			UserProfile userProfile = ExtendedAppContext.getCurrentlyLoggedInUserProfile();
			StatedInferredOptions chosenPolicy = userProfile.getStatedInferredPolicy();
			UUID chosenPathUuid = userProfile.getViewCoordinatePath();
			Long chosenTime = userProfile.getViewCoordinateTime();

			Label printSelectedPathLabel = new Label("Path: " + OTFUtility.getDescription(chosenPathUuid));
			gridPane.add(printSelectedPathLabel, 0, 4);
			GridPane.setHalignment(printSelectedPathLabel, HPos.LEFT);
			Label printSelectedTimeLabel = null;
			if(chosenTime != getDefaultTime()) {
				printSelectedTimeLabel = new Label("Time: " + dateFormat.format(new Date(chosenTime)));
			} else {
				printSelectedTimeLabel = new Label("Time: LONG MAX VALUE");
			}
			gridPane.add(printSelectedTimeLabel, 1, 4);
			GridPane.setHalignment(printSelectedTimeLabel, HPos.LEFT);
			Label printSelectedPolicyLabel = new Label("Policy: " + chosenPolicy);
			gridPane.add(printSelectedPolicyLabel, 2, 4);
			GridPane.setHalignment(printSelectedPolicyLabel, HPos.LEFT);
			 */
		}

		// Reload persisted values every time

		// Reload storedStatedInferredOption
		final StatedInferredOptions storedStatedInferredOption = getStoredStatedInferredOption();
		for (Toggle toggle : statedInferredToggleGroup.getToggles()) {
			if (toggle.getUserData() == storedStatedInferredOption) {
				toggle.setSelected(true);
			}
		}

		// Reload storedStatuses
		final Set<Status> storedStatuses = this.getStoredViewCoordinateStatuses();
		if (storedStatuses.contains(Status.ACTIVE) && storedStatuses.contains(Status.INACTIVE)) {
			statusesToggleGroup.selectToggle(activeAndInactiveStatusButton);
		} else if (storedStatuses.contains(Status.ACTIVE)) {
			statusesToggleGroup.selectToggle(activeStatusButton);
		} else if (storedStatuses.contains(Status.INACTIVE)) {
			statusesToggleGroup.selectToggle(inactiveStatusButton);
		} else if (storedStatuses.size() == 0) {
			log.warn("UserProfile does not contain any view coordinate Status values");
		} else {
			log.error("UserProfile contains unsupported view coordinate Status values: {}", storedStatuses.toArray());
		}

		// Reload storedModules
		final Set<UUID> storedModules = this.getStoredViewCoordinateModules();
		for (SelectableModule module : selectableModules) {
			if (storedModules.contains(module.getUuid())) {
				module.setSelected(true);
			} else {
				module.setSelected(false);
			}
		}

		return vBoxInTab;
	}

	private IntStream getStamps(int pathSequence, long startTime, long endTime)
	{
		CommitService cs = AppContext.getService(CommitService.class);
		return  cs.getStampSequences().filter((stampSequence) ->
		{
			if (cs.getPathSequenceForStamp(stampSequence) == pathSequence
					&& cs.getTimeForStamp(stampSequence) >= startTime
					&& cs.getTimeForStamp(stampSequence) <= endTime)
			{
				return true;
			}
			return false;
		});
	}

	/**
	 * 
	 * @param path int of the path to get the Time Options for
	 * @param storedTimePref Long of anytime during the specific day that we want to return times for
	 * @return populates the "times" TreeSet (time longs truncated at the "the seconds" position) 
	 * 			which populates Time Combo box, the truncTimeToFullTimeMap which maps the truncated times
	 * 			im times TreeSet to each times full Long value. The truncTimeToFullTimeMap chooses each time
	 * 			up to the second and maps it to the greatest equivalent time up to the milliseconds.
	 * 			
	 */
	protected void setTimeOptions(int path, Long storedTimePref) {
		try {
			overrideTimestamp = null;

			Date startDate = null, finishDate = null;
			if(storedTimePref != null) {
				CommitService stampDb = AppContext.getService(CommitService.class);
				//TODO OCHRE use the right path here
				IntStream stamps = null;
				if(!storedTimePref.equals(getDefaultTime())) {
					startDate = getStartOfDay(new Date(storedTimePref)); 
					finishDate = getEndOfDay(new Date(storedTimePref));
					stamps = getStamps(IsaacMetadataAuxiliaryBinding.DEVELOPMENT.getSequence(), startDate.getTime(), finishDate.getTime());
				} else {
					stamps = getStamps(IsaacMetadataAuxiliaryBinding.DEVELOPMENT.getSequence(), Long.MIN_VALUE, Long.MAX_VALUE);
				}

				truncTimeToFullTimeMap.clear();
				times.clear();

				HashSet<Integer> stampSet = new HashSet<>(stamps.boxed().collect(Collectors.toList()));

				Date d = new Date(storedTimePref);
				if (dateIsLocalDate(d)) {
					// Get stamps of day
					Date todayStartDate = getStartOfDay(new Date()); 
					Date todayFinishDate = getEndOfDay(new Date());
					IntStream todayStamps = getStamps(IsaacMetadataAuxiliaryBinding.DEVELOPMENT.getSequence(), todayStartDate.getTime(), todayFinishDate.getTime());

					// If have stamps, no action, if not, show Latest and set stamps to latest stamp we have in stampset
					if (todayStamps.toArray().length == 0) {
						IntStream allStamps = getStamps(IsaacMetadataAuxiliaryBinding.DEVELOPMENT.getSequence(), Long.MIN_VALUE, Long.MAX_VALUE);
						HashSet<Integer> allStampSet = new HashSet<>(allStamps.boxed().collect(Collectors.toList()));
						SortedSet<Integer> s = new TreeSet<Integer>(allStampSet);
						if (!s.isEmpty()) {
							Integer stampToSet = s.last();
							overrideTimestamp = stampDb.getTimeForStamp(stampToSet);
							currentTimeProperty.set(Long.MAX_VALUE);
						}
					}
				}

				this.pathDatesList.add(LocalDate.now());
				if (overrideTimestamp == null) {
					if(!stampSet.isEmpty()) {
						//						enableTimeCombo(true);
						for(Integer thisStamp : stampSet) {
							Long fullTime = null;
							Date stampDate;
							LocalDate stampInstant = null;
							try {
								fullTime = stampDb.getTimeForStamp(thisStamp);
								stampDate = new Date(fullTime);
								stampInstant = stampDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
							} catch (Exception e) {
								e.printStackTrace();
							}
							Calendar cal = Calendar.getInstance();
							cal.setTime(new Date(fullTime));
							cal.set(Calendar.MILLISECOND, 0); //Strip milliseconds
							Long truncTime = cal.getTimeInMillis();

							this.pathDatesList.add(stampInstant); //Build DatePicker
							times.add(truncTime); //This can probably go, we don't populate hashmap like this at initialization

							if(truncTimeToFullTimeMap.containsKey(truncTime)) { //Build Truncated Time to Full Time HashMap
								//If truncTimeToFullTimeMap has this key, is the value the newest time in milliseconds?
								if(new Date(truncTimeToFullTimeMap.get(truncTime)).before(new Date(fullTime))) {
									truncTimeToFullTimeMap.put(truncTime, fullTime);
								}
							} else {
								truncTimeToFullTimeMap.put(truncTime, fullTime);
							}
						}
					} else {
						currentTimeProperty.set(Long.MAX_VALUE);
						log.warn("Could not retrieve any Stamps");
					}
				}
			}
		} catch (Exception e) {
			log.error("Error setting the default Time Dropdown");
			e.printStackTrace();
		}
	}

	private boolean dateIsLocalDate(Date d) {
		Month ldMonth = LocalDate.now().atStartOfDay().getMonth();
		int ldDate = LocalDate.now().atStartOfDay().getDayOfMonth();
		int ldYear = LocalDate.now().atStartOfDay().getYear();

		Calendar cal = Calendar.getInstance();
		cal.setTime(d);

		if (cal.get(Calendar.YEAR) == ldYear &&
				cal.get(Calendar.DAY_OF_MONTH) == ldDate &&
				cal.get(Calendar.MONTH) == (ldMonth.getValue() -1)) {
			return true;
		}

		return false;
	}

	protected Collection<UUID> getPathOptions() {
		List<UUID> list = new ArrayList<>();

		try {
			List<ConceptChronicleBI> pathConcepts = OTFUtility.getPathConcepts();
			for (ConceptChronicleBI cc : pathConcepts) {
				list.add(cc.getPrimordialUuid());
			}
		} catch (IOException | ContradictionException e) {
			log.error("Failed loading path concepts. Caught {} {}", e.getClass().getName(), e.getLocalizedMessage());
			e.printStackTrace();
		}
		// Add currently-stored value to list of options, if not already there
		UUID storedPath = getStoredPath();
		if (storedPath != null && ! list.contains(storedPath)) {
			list.add(storedPath);
		}

		return list;
	}

	protected Long getStoredTime() {
		UserProfile loggedIn = ExtendedAppContext.getCurrentlyLoggedInUserProfile();
		return loggedIn.getViewCoordinateTime();
	}

	protected UUID getStoredPath() {
		UserProfile loggedIn = ExtendedAppContext.getCurrentlyLoggedInUserProfile();
		return loggedIn.getViewCoordinatePath();
	}

	protected StatedInferredOptions getStoredStatedInferredOption() {
		UserProfile loggedIn = ExtendedAppContext.getCurrentlyLoggedInUserProfile();
		return loggedIn.getStatedInferredPolicy();
	}

	protected Set<Status> getStoredViewCoordinateStatuses() {
		UserProfile loggedIn = ExtendedAppContext.getCurrentlyLoggedInUserProfile();
		return loggedIn.getViewCoordinateStatuses();
	}

	protected Set<UUID> getStoredViewCoordinateModules() {
		UserProfile loggedIn = ExtendedAppContext.getCurrentlyLoggedInUserProfile();
		return loggedIn.getViewCoordinateModules();
	}
	protected Set<UUID> getDefaultViewCoordinateModules() {
		return UserProfileDefaults.getDefaultViewCoordinateModules();
	}

	protected UUID getDefaultPath() {
		return UserProfileDefaults.getDefaultViewCoordinatePath();
	}

	protected Long getDefaultTime() {
		return UserProfileDefaults.getDefaultViewCoordinateTime();
	}

	protected StatedInferredOptions getDefaultStatedInferredOption() {
		return UserProfileDefaults.getDefaultStatedInferredPolicy();
	}
	protected Set<Status> getDefaultViewCoordinateStatuses() {
		return UserProfileDefaults.getDefaultViewCoordinateStatuses();
	}

	public ReadOnlySetProperty<Status> currentViewCoordinateStatusesProperty() {
		return currentStatusesProperty;
	}
	public ReadOnlyObjectProperty<Long> currentViewCoordinateTimeProperty() {
		return currentTimeProperty;
	}

	public ReadOnlyObjectProperty<StatedInferredOptions> currentStatedInferredOptionProperty() {
		return currentStatedInferredOptionProperty;
	}

	public ReadOnlyObjectProperty<UUID> currentPathProperty() {
		return currentPathProperty;
	}

	static class SelectableModule {
		private final IntegerProperty nid = new SimpleIntegerProperty();
		private final BooleanProperty selected = new SimpleBooleanProperty(false);
		private final String description;
		private final UUID uuid;

		public SelectableModule(Integer nid) {
			this.nid.set(nid);

			String desc = null;
			try {
				desc = OTFUtility.getDescription(nid);
			} catch (Exception e) {
				log.error("Failed to set description for concept with nid={}", nid);
			}
			description = desc;

			UUID aUuid = null;
			try {
				ConceptVersionBI cv = OTFUtility.getConceptVersion(nid);
				aUuid = cv.getPrimordialUuid();
			} catch (Exception e) {
				log.error("Failed to set uuid for concept with nid={} and desc={}", nid, description);
			}
			uuid = aUuid;
		}

		public Integer getNid() {
			return nid.get();
		}
		public ReadOnlyIntegerProperty nidProperty() {
			return nid;
		}

		public BooleanProperty selectedProperty() {
			return selected;
		}
		public boolean isSelected() {
			return selected.get();
		}
		public void setSelected(boolean selected) {
			this.selected.set(selected);
		}
		public String getDescription() {
			return description;
		}
		public UUID getUuid() {
			return uuid;
		}
	}
}
