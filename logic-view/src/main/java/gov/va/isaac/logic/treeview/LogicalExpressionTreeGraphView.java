package gov.va.isaac.logic.treeview;

import gov.va.isaac.AppContext;
import gov.va.isaac.config.profiles.UserProfileBindings;
import gov.va.isaac.gui.dialog.DetachablePopOverHelper;
import gov.va.isaac.gui.util.CustomClipboard;
import gov.va.isaac.interfaces.gui.views.commonFunctionality.LogicalExpressionTreeGraphEmbeddableViewI;
import gov.va.isaac.util.UpdateableBooleanBinding;
import gov.va.isaac.util.Utility;
import gov.vha.isaac.ochre.api.DataSource;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.chronicle.LatestVersion;
import gov.vha.isaac.ochre.api.component.sememe.SememeChronology;
import gov.vha.isaac.ochre.api.component.sememe.version.LogicGraphSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.SememeVersion;
import gov.vha.isaac.ochre.api.coordinate.PremiseType;
import gov.vha.isaac.ochre.api.coordinate.TaxonomyCoordinate;
import gov.vha.isaac.ochre.api.logic.LogicalExpression;
import gov.vha.isaac.ochre.api.observable.coordinate.ObservableTaxonomyCoordinate;
import gov.vha.isaac.ochre.model.logic.LogicalExpressionOchreImpl;
import gov.vha.isaac.ochre.model.sememe.version.LogicGraphSememeImpl;
import gov.vha.isaac.ochre.observable.model.coordinate.ObservableLanguageCoordinateImpl;
import gov.vha.isaac.ochre.observable.model.coordinate.ObservableLogicCoordinateImpl;
import gov.vha.isaac.ochre.observable.model.coordinate.ObservableStampCoordinateImpl;
import gov.vha.isaac.ochre.observable.model.coordinate.ObservableTaxonomyCoordinateImpl;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import javax.inject.Named;

import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Named (value="LogicalExpressionTreeGraphView")
@PerLookup
public class LogicalExpressionTreeGraphView implements LogicalExpressionTreeGraphEmbeddableViewI {
	private static final DateFormat DATETIME_FORMAT = new SimpleDateFormat("MM/dd/yy HH:mm:ss");
	private static final DateFormat DATE_FORMAT = new SimpleDateFormat("MM/dd/yy");
	
	private LogicalExpressionTreeGraph logicalExpressionTreeGraph;
	private Label textGraph;
	private Label title;
	
	private ScrollPane rootScrollPane;
	private AtomicInteger noRefresh_ = new AtomicInteger(0);
	
	private Integer conceptId = null;

	private LogicGraphSememe<?> specifiedLogicGraphSememe = null;
	
	private LogicGraphSememe<?> cachedLogicGraphSememe = null;
	
	private ObjectProperty<ObservableTaxonomyCoordinate> passedObservableTaxonomyCoordinateProperty = new SimpleObjectProperty();
	
	private Logger logger_ = LoggerFactory.getLogger(this.getClass());
	
	final ObjectProperty<ObservableTaxonomyCoordinate> taxonomyCoordinate = new SimpleObjectProperty<>(new ObservableTaxonomyCoordinateImpl(AppContext.getService(UserProfileBindings.class).getTaxonomyCoordinate().get()));
	final UpdateableBooleanBinding taxonomyCoordinateBinding = new UpdateableBooleanBinding()
    {
        private volatile boolean enabled = false;
        {
            setComputeOnInvalidate(true);
            addBinding(
            		taxonomyCoordinate,
            		taxonomyCoordinate.get().languageCoordinateProperty(),
            		taxonomyCoordinate.get().stampCoordinateProperty(),
            		taxonomyCoordinate.get().premiseTypeProperty());
            enabled = true;
        }

        @Override
        protected boolean computeValue()
        {
            if (!enabled)
            {
            	logger_.debug("Skip initial spurious refresh calls");
                return false;
            }
            logger_.debug("Kicking off tree refresh() due to change of property");

			updateRootPanePremiseTypeMenuItem();
            LogicalExpressionTreeGraphView.this.refresh();
            return false;
        }
    };

	private final MenuItem rootPanePremiseTypeMenuItem = new MenuItem();

	private void resetPremiseTypeMenuItemText(PremiseType pt) {
		rootPanePremiseTypeMenuItem.setText("Switch to " + pt.name() + " view");
	}
	private void resetPremiseTypeMenuItemTooltipText(PremiseType pt) {
		if (title != null) {
			title.setTooltip(new Tooltip("Right-click and select to switch to " + pt.name() + " view"));
		}
	}
	private void updateRootPanePremiseTypeMenuItem() {
		switch (taxonomyCoordinate.get().premiseTypeProperty().get()) {
		case STATED:
			resetPremiseTypeMenuItemText(PremiseType.INFERRED);
			resetPremiseTypeMenuItemTooltipText(PremiseType.INFERRED);
			rootPanePremiseTypeMenuItem.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event)
				{
					taxonomyCoordinate.get().premiseTypeProperty().set(PremiseType.INFERRED);
				}
			});
			break;
		case INFERRED:
			resetPremiseTypeMenuItemText(PremiseType.STATED);
			resetPremiseTypeMenuItemTooltipText(PremiseType.STATED);
			rootPanePremiseTypeMenuItem.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event)
				{
					taxonomyCoordinate.get().premiseTypeProperty().set(PremiseType.STATED);
				}
			});
			break;
			default:
				throw new RuntimeException("Invalid PremiseType value " + taxonomyCoordinate.get().premiseTypeProperty().get());
		}
	}

    @Override
    public Integer getConceptId() {
    	return conceptId;
    }
	
	private LogicalExpressionTreeGraphView() {
		//Created by HK2 - no op - delay till getView called
	}
	
	public static final Comparator<LogicalExpressionTreeGraphEmbeddableViewI> TREEGRAPH_COMPARATOR = new Comparator<LogicalExpressionTreeGraphEmbeddableViewI>() {
		@Override
		public int compare(LogicalExpressionTreeGraphEmbeddableViewI o1, LogicalExpressionTreeGraphEmbeddableViewI o2) {
			long diff = o1.getLogicGraphSememe().getTime() - o2.getLogicGraphSememe().getTime();
			if (diff > 0) {
				return 1;
			} else if (diff < 0) {
				return -1;
			} else {
				return 0;
			}
		}
	};
	
	private static String getTabLabelText(LogicalExpressionTreeGraphEmbeddableViewI view, LocalDate priorDate, LocalDate currentDate) {
		String text = null;
		if (currentDate.equals(priorDate)) {
			text = DATETIME_FORMAT.format(view.getLogicGraphSememe().getTime());
		} else {
			text = DATE_FORMAT.format(view.getLogicGraphSememe().getTime());
		}
		return text;
	}
	private void init() {
		if (rootScrollPane == null) {
			noRefresh_.getAndIncrement();
			rootScrollPane = new ScrollPane();
			rootScrollPane.setVbarPolicy(ScrollBarPolicy.ALWAYS);
			rootScrollPane.setHbarPolicy(ScrollBarPolicy.ALWAYS);
			
			VBox vbox = new VBox();
			vbox.setAlignment(Pos.TOP_CENTER);
			
			title = new Label();
			title.setAlignment(Pos.CENTER);
			
			logicalExpressionTreeGraph = new LogicalExpressionTreeGraph(
					true, 
					true, 
					150, 75, 12);
			textGraph = new Label();
			textGraph.setAlignment(Pos.CENTER);
			textGraph.setContextMenu(new ContextMenu());
			{
				MenuItem mi = new MenuItem("Copy Text Graph");
				mi.visibleProperty().bind(textGraph.textProperty().isNotNull());
				mi.setOnAction(new EventHandler<ActionEvent>() {
					@Override
					public void handle(ActionEvent arg0) {
						CustomClipboard.set(title.getText() + "\n" + textGraph.getText());
					}
				});

				textGraph.getContextMenu().getItems().add(mi);
			}

			rootScrollPane = new ScrollPane();
			
			rootScrollPane.setContextMenu(new ContextMenu());

			rootScrollPane.getContextMenu().getItems().add(rootPanePremiseTypeMenuItem);

			// Do not enable PremiseType menu item for views based on passed ObservableTaxonomyCoordinate
			rootPanePremiseTypeMenuItem.visibleProperty().bind(passedObservableTaxonomyCoordinateProperty.isNull());

			{
				MenuItem mi = new MenuItem("Display Text Graph");
				mi.visibleProperty().bind(textGraph.textProperty().isNotNull());
				mi.setOnAction(new EventHandler<ActionEvent>() {
					@Override
					public void handle(ActionEvent arg0) {
						DetachablePopOverHelper.showDetachachablePopOver(title, DetachablePopOverHelper.newDetachachablePopoverWithCloseButton(title.getText(), textGraph));
					}
				});

				rootScrollPane.getContextMenu().getItems().add(mi);
			}
			
			// Only offer to display history if not already displaying history
			if (this.specifiedLogicGraphSememe == null)
			{
				MenuItem mi = new MenuItem("Display History");
				mi.visibleProperty().bind(textGraph.textProperty().isNotNull());
				mi.setOnAction(new EventHandler<ActionEvent>() {
					@Override
					public void handle(ActionEvent arg0) {
						//VBox historicalLogicGraphViewNode = new VBox();
						TabPane historicalLogicGraphViewNode = new TabPane();
						historicalLogicGraphViewNode.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
						List<LogicalExpressionTreeGraphEmbeddableViewI> historicalViews = getHistoricalViews();
						Collections.sort(historicalViews, TREEGRAPH_COMPARATOR);

						// Place views into buckets by date to determine which dates gave multiple views
						// which will result in the tab displaying both date and time
						Map<LocalDate, List<LogicalExpressionTreeGraphEmbeddableViewI>> dateToViewsMap = new HashMap<>();
						for (LogicalExpressionTreeGraphEmbeddableViewI version : historicalViews) {
							Date date = new Date(version.getLogicGraphSememe().getTime());
							LocalDateTime time = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
							LocalDate currentDate = time.toLocalDate();
							if (dateToViewsMap.get(currentDate) == null) {
								dateToViewsMap.put(currentDate, new ArrayList<LogicalExpressionTreeGraphEmbeddableViewI>());
							}
							dateToViewsMap.get(currentDate).add(version);
						}
						
						for (LogicalExpressionTreeGraphEmbeddableViewI version : historicalViews) {
							Date date = new Date(version.getLogicGraphSememe().getTime());
							LocalDateTime time = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
							LocalDate currentDate = time.toLocalDate();
							
							Tab tab = new Tab();
							if (dateToViewsMap.get(currentDate).size() > 1) {
								tab.setText(DATETIME_FORMAT.format(date));
							} else {
								tab.setText(DATE_FORMAT.format(date));
							}
							
							tab.setContent(version.getView());
							historicalLogicGraphViewNode.getTabs().add(tab);
						}
						
						DetachablePopOverHelper.showDetachachablePopOver(title, DetachablePopOverHelper.newDetachachablePopoverWithCloseButton(title.getText() + " History", historicalLogicGraphViewNode));
					}
				});

				rootScrollPane.getContextMenu().getItems().add(mi);
			}
			
			taxonomyCoordinate.get().premiseTypeProperty().addListener(new ChangeListener<PremiseType>() {
				@Override
				public void changed(
						ObservableValue<? extends PremiseType> observable,
						PremiseType oldValue, PremiseType newValue) {
					if (newValue == PremiseType.INFERRED) {
						resetPremiseTypeMenuItemText(PremiseType.STATED);
						resetPremiseTypeMenuItemTooltipText(PremiseType.STATED);
					} else {
						resetPremiseTypeMenuItemText(PremiseType.INFERRED);
						resetPremiseTypeMenuItemTooltipText(PremiseType.INFERRED);
					}
				}
				
			});
			updateRootPanePremiseTypeMenuItem();
			
			vbox.getChildren().addAll(title, logicalExpressionTreeGraph /*, textGraph */);

			rootScrollPane.setContent(vbox);
			
			noRefresh_.decrementAndGet();
		}
	}

	@Override
	public Region getView() {
		if (rootScrollPane == null) {
			init();
		}
		
		return rootScrollPane;
	}

	@Override
	public LogicGraphSememe getLogicGraphSememe() {
		return cachedLogicGraphSememe;
	}

	protected void refresh()
	{
		if (conceptId == null) {
			clear();
			
			return;
		}

		// ELSE
		
		if (noRefresh_.get() > 0)
		{
			logger_.info("Skip refresh of LogicalExpressionTreeGraphView due to wait count {}", noRefresh_.get());
			return;
		}
		
		// ELSE
		noRefresh_.getAndIncrement();
		
		// TODO get background processes working
		LogicalExpression le = loadData();
		
		displayData(le);
		
		noRefresh_.decrementAndGet();

//		Task<LogicalExpression> task = new Task<LogicalExpression>() {
//			@Override
//			protected LogicalExpression call() throws Exception {
//				return loadData();
//			}
//			
//			@Override
//			public void succeeded() {
//				displayData(getValue());
//				noRefresh_.decrementAndGet();
//			}
//			@Override
//			public void failed() {
//				logger_.error("Unexpected error building the LogicalExpressionTreeGraphView display", getException());
//				//null check, as the error may happen before the scene is visible
//				Platform.runLater(() ->
//					AppContext.getCommonDialogs().showErrorDialog("Error", "There was an unexpected error building the LogicalExpressionTreeGraphView display", getException().getMessage(), 
//						(rootScrollPane.getScene() == null ? null : rootScrollPane.getScene().getWindow())));
//				noRefresh_.decrementAndGet();
//			}
//			@Override
//			public void cancelled() {
//				logger_.warn("Unexpected cancellation building the LogicalExpressionTreeGraphView display");
//				//null check, as may happen before the scene is visible
//				Platform.runLater(() ->
//					AppContext.getCommonDialogs().showErrorDialog("Error", "There was an unexpected cancellation building the LogicalExpressionTreeGraphView display", "Task cancelled", 
//						(rootScrollPane.getScene() == null ? null : rootScrollPane.getScene().getWindow())));
//				noRefresh_.decrementAndGet();
//			}
//		};
//		
//		Utility.execute(task);
	}
	
	private LogicGraphSememe<?> loadLogicGraphSememe(LogicGraphSememe<?> sememeVersion) {
		if (sememeVersion != null) {
			return sememeVersion;
		} else {
			Optional<SememeChronology<? extends SememeVersion<?>>> defChronologyOptional = taxonomyCoordinate.get().getTaxonomyType() == PremiseType.STATED ? Get.statedDefinitionChronology(conceptId) : Get.inferredDefinitionChronology(conceptId);
			if (! defChronologyOptional.isPresent()) {
				String error = "No " + taxonomyCoordinate.get().getTaxonomyType().name() + " definition chronology found for " + Get.conceptDescriptionText(conceptId) + " for  specified TaxonomyCoordinate";
				AppContext.getCommonDialogs().showErrorDialog("Missing Definition Chronology", taxonomyCoordinate.get().getTaxonomyType().name() + " not found", error);
				logger_.error(error);

				return null;
			}
			SememeChronology rawDefChronology = defChronologyOptional.get();
			Optional<LatestVersion<LogicGraphSememeImpl>> latestGraphLatestVersionOptional = rawDefChronology.getLatestVersion(LogicGraphSememeImpl.class, taxonomyCoordinate.get().getStampCoordinate());
			if (! latestGraphLatestVersionOptional.isPresent()) {
				String error = "No " + taxonomyCoordinate.get().getTaxonomyType().name() + " relationship LogicGraph found for " + Get.conceptDescriptionText(conceptId) + " for specified TaxonomyCoordinate";
				AppContext.getCommonDialogs().showErrorDialog("Missing LogicGraph", "Latest version of LogicGraph not found", error);

				return null;
			} else {
				return (LogicGraphSememeImpl)latestGraphLatestVersionOptional.get().value();
			}
		}
	}
	
	private LogicalExpression loadData() {
		cachedLogicGraphSememe = loadLogicGraphSememe(specifiedLogicGraphSememe);
	
		if (cachedLogicGraphSememe == null) {
			return null;
		}
		
		LogicalExpressionOchreImpl le = new LogicalExpressionOchreImpl(cachedLogicGraphSememe.getGraphData(), DataSource.INTERNAL, Get.identifierService().getConceptSequence(cachedLogicGraphSememe.getReferencedComponentNid()));

		return le;
	}
	

	private void displayData(LogicalExpression le) {
		logicalExpressionTreeGraph.getChildren().clear();

		if (le != null) {
			if (specifiedLogicGraphSememe == null) { 
				title.setText(taxonomyCoordinate.get().getTaxonomyType().name());
			} else {
				title.setText(taxonomyCoordinate.get().getTaxonomyType().name() + " (" + DATETIME_FORMAT.format(cachedLogicGraphSememe.getTime()) + ")");
			}

			logicalExpressionTreeGraph.displayLogicalExpression(le, taxonomyCoordinate.get().getStampCoordinate(), taxonomyCoordinate.get().getLanguageCoordinate());

			textGraph.setText(le.toString());
		} else {
			title.setText(null);
			textGraph.setText(null);
		}
	}

	@Override
	public void viewDiscarded() {
		clear();

		if (passedObservableTaxonomyCoordinateProperty.get() != null) {
			unbindFromPassedObservableTaxonomyCoordinate();
			passedObservableTaxonomyCoordinateProperty.set(null);
		}
	}


	@Override
	public void setConcept(int id) {
		if ((conceptId != null && conceptId != id) || specifiedLogicGraphSememe != null) {
			clear();
		}

		specifiedLogicGraphSememe = null;
		conceptId = id;
		
		init();
		
		refresh();
	}
	@Override
	public void setConcept(LogicGraphSememe<?> specifiedLogicGraphSememe) {
		clear();

		this.specifiedLogicGraphSememe = specifiedLogicGraphSememe;
		conceptId = specifiedLogicGraphSememe.getReferencedComponentNid();
		
		init();
		
		refresh();
	}

	private void unbindFromPassedObservableTaxonomyCoordinate() {
		this.taxonomyCoordinate.get().languageCoordinateProperty().bind(passedObservableTaxonomyCoordinateProperty.get().languageCoordinateProperty());
		this.taxonomyCoordinate.get().stampCoordinateProperty().bind(passedObservableTaxonomyCoordinateProperty.get().stampCoordinateProperty());
		this.taxonomyCoordinate.get().logicCoordinateProperty().bind(passedObservableTaxonomyCoordinateProperty.get().logicCoordinateProperty());
		this.taxonomyCoordinate.get().premiseTypeProperty().bind(passedObservableTaxonomyCoordinateProperty.get().premiseTypeProperty());
		this.taxonomyCoordinate.get().uuidProperty().bind(passedObservableTaxonomyCoordinateProperty.get().uuidProperty());
	}

	@Override
	public void setConcept(
			TaxonomyCoordinate taxonomyCoordinate,
			LogicGraphSememe<?> specifiedLogicGraphSememe) {
		if (passedObservableTaxonomyCoordinateProperty.get() != null) {
			unbindFromPassedObservableTaxonomyCoordinate();
		}
		passedObservableTaxonomyCoordinateProperty.set(null);
		
		this.taxonomyCoordinate.get().languageCoordinateProperty().set(new ObservableLanguageCoordinateImpl(taxonomyCoordinate.getLanguageCoordinate()));
		this.taxonomyCoordinate.get().stampCoordinateProperty().set(new ObservableStampCoordinateImpl(taxonomyCoordinate.getStampCoordinate()));
		this.taxonomyCoordinate.get().logicCoordinateProperty().set(new ObservableLogicCoordinateImpl(taxonomyCoordinate.getLogicCoordinate()));
		this.taxonomyCoordinate.get().premiseTypeProperty().set(taxonomyCoordinate.getTaxonomyType());
		this.taxonomyCoordinate.get().uuidProperty().set(taxonomyCoordinate.getUuid());
		
		setConcept(specifiedLogicGraphSememe);
	}
	@Override
	public void setConcept(
			TaxonomyCoordinate taxonomyCoordinate,
			int componentNid) {
		if (passedObservableTaxonomyCoordinateProperty.get() != null) {
			unbindFromPassedObservableTaxonomyCoordinate();
		}
		passedObservableTaxonomyCoordinateProperty.set(null);
		
		this.taxonomyCoordinate.get().languageCoordinateProperty().set(new ObservableLanguageCoordinateImpl(taxonomyCoordinate.getLanguageCoordinate()));
		this.taxonomyCoordinate.get().stampCoordinateProperty().set(new ObservableStampCoordinateImpl(taxonomyCoordinate.getStampCoordinate()));
		this.taxonomyCoordinate.get().logicCoordinateProperty().set(new ObservableLogicCoordinateImpl(taxonomyCoordinate.getLogicCoordinate()));
		this.taxonomyCoordinate.get().premiseTypeProperty().set(taxonomyCoordinate.getTaxonomyType());
		this.taxonomyCoordinate.get().uuidProperty().set(taxonomyCoordinate.getUuid());
		
		setConcept(componentNid);
	}

	@Override
	public void setConcept(ObservableTaxonomyCoordinate taxonomyCoordinate,
			int componentNid) {
		if (passedObservableTaxonomyCoordinateProperty.get() != null) {
			unbindFromPassedObservableTaxonomyCoordinate();
		}
		passedObservableTaxonomyCoordinateProperty.set(taxonomyCoordinate);
		
		this.taxonomyCoordinate.get().languageCoordinateProperty().bind(taxonomyCoordinate.languageCoordinateProperty());
		this.taxonomyCoordinate.get().stampCoordinateProperty().bind(taxonomyCoordinate.stampCoordinateProperty());
		this.taxonomyCoordinate.get().logicCoordinateProperty().bind(taxonomyCoordinate.logicCoordinateProperty());
		this.taxonomyCoordinate.get().premiseTypeProperty().bind(taxonomyCoordinate.premiseTypeProperty());
		this.taxonomyCoordinate.get().uuidProperty().bind(taxonomyCoordinate.uuidProperty());
		
		setConcept(componentNid);	
	}
	@Override
	public void setConcept(ObservableTaxonomyCoordinate taxonomyCoordinate,
			LogicGraphSememe<?> specifiedLogicGraphSememe) {
		if (passedObservableTaxonomyCoordinateProperty.get() != null) {
			unbindFromPassedObservableTaxonomyCoordinate();
		}
		passedObservableTaxonomyCoordinateProperty.set(taxonomyCoordinate);
		
		this.taxonomyCoordinate.get().languageCoordinateProperty().bind(taxonomyCoordinate.languageCoordinateProperty());
		this.taxonomyCoordinate.get().stampCoordinateProperty().bind(taxonomyCoordinate.stampCoordinateProperty());
		this.taxonomyCoordinate.get().logicCoordinateProperty().bind(taxonomyCoordinate.logicCoordinateProperty());
		this.taxonomyCoordinate.get().premiseTypeProperty().bind(taxonomyCoordinate.premiseTypeProperty());
		this.taxonomyCoordinate.get().uuidProperty().bind(taxonomyCoordinate.uuidProperty());
		
		setConcept(specifiedLogicGraphSememe);
	}

	@Override
	public void setConcept(UUID uuid) {
		uuid.getClass(); // Throw NPE if null
		Utility.execute(() -> setConcept(Get.identifierService().getConceptSequenceForUuids(uuid)));
	}
	
	@Override
	public void setConcept(
			TaxonomyCoordinate taxonomyCoordinate,
			UUID uuid) {
		uuid.getClass(); // Throw NPE if null
		Utility.execute(() -> setConcept(taxonomyCoordinate, Get.identifierService().getConceptSequenceForUuids(uuid)));
	}
	@Override
	public void setConcept(ObservableTaxonomyCoordinate taxonomyCoordinate,
			UUID uuid) {
		uuid.getClass(); // Throw NPE if null
		Utility.execute(() -> setConcept(taxonomyCoordinate, Get.identifierService().getConceptSequenceForUuids(uuid)));
	}
	
	@Override
	public void clear() {
		cachedLogicGraphSememe = null;
		specifiedLogicGraphSememe = null;
		if (logicalExpressionTreeGraph != null) {
			logicalExpressionTreeGraph.getChildren().clear();
			logicalExpressionTreeGraph.setRootNode(null);
		}
		if (textGraph != null) {
			textGraph.setText(null);
		}
		if (title != null) {
			title.setText(null);
		}
		conceptId = null;
	}
	
	public List<LogicalExpressionTreeGraphEmbeddableViewI> getHistoricalViews() {
		List<LogicalExpressionTreeGraphEmbeddableViewI> historicalViews = new ArrayList<>();
		
		Optional<SememeChronology<? extends SememeVersion<?>>> defChronologyOptional = taxonomyCoordinate.get().getTaxonomyType() == PremiseType.STATED ? Get.statedDefinitionChronology(conceptId) : Get.inferredDefinitionChronology(conceptId);
		if (! defChronologyOptional.isPresent()) {
			AppContext.getCommonDialogs().showInformationDialog("Missing Definition Chronology", "No " + taxonomyCoordinate.get().getTaxonomyType().name() + " definition chronology found for " + Get.conceptDescriptionText(conceptId) + " for  specified TaxonomyCoordinate");

			return historicalViews;
		}

		for (SememeVersion<?> version : defChronologyOptional.get().getVersionList()) {
			LogicalExpressionTreeGraphEmbeddableViewI embeddableView = AppContext.getService(LogicalExpressionTreeGraphEmbeddableViewI.class);

			embeddableView.setConcept(taxonomyCoordinate.get(), (LogicGraphSememe<?>)version);
			
			if (embeddableView.getLogicGraphSememe() != null) {
				historicalViews.add(embeddableView);
				//logger_.debug("getHistoricalViews() Added historical LogicGraph version {}", version.toUserString());
			} else {
				logger_.debug("getHistoricalViews() Did not add historical LogicGraph version {}", version.toUserString());
			}
		}

		return historicalViews;
	}
}
