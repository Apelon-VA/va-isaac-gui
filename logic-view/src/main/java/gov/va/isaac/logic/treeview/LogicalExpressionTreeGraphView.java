package gov.va.isaac.logic.treeview;

import gov.va.isaac.AppContext;
import gov.va.isaac.config.profiles.UserProfileBindings;
import gov.va.isaac.gui.util.CustomClipboard;
import gov.va.isaac.interfaces.gui.views.commonFunctionality.LogicalExpressionTreeGraphEmbeddableViewI;
import gov.va.isaac.util.UpdateableBooleanBinding;
import gov.va.isaac.util.Utility;
import gov.vha.isaac.ochre.api.DataSource;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.chronicle.LatestVersion;
import gov.vha.isaac.ochre.api.component.sememe.SememeChronology;
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

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
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
	private LogicalExpressionTreeGraph logicalExpressionTreeGraph;
	private Label textGraph;
	private Label title;
	
	private ScrollPane rootScrollPane;
	private AtomicInteger noRefresh_ = new AtomicInteger(0);
	
	Integer conceptId = null;

	private ObservableTaxonomyCoordinate passedObservableTaxonomyCoordinate = null;
	
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

	private void resetRootPanePremiseTypeMenuItemText(PremiseType pt) {
		rootPanePremiseTypeMenuItem.setText("Switch to " + pt.name() + " view");
	}
	private void resetRootPaneTooltipText(PremiseType pt) {
		rootScrollPane.setTooltip(new Tooltip("Right-click and select to switch to " + pt.name() + " view"));
	}
	private void updateRootPanePremiseTypeMenuItem() {
		switch (taxonomyCoordinate.get().premiseTypeProperty().get()) {
		case STATED:
			resetRootPanePremiseTypeMenuItemText(PremiseType.INFERRED);
			resetRootPaneTooltipText(PremiseType.INFERRED);
			rootPanePremiseTypeMenuItem.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event)
				{
					taxonomyCoordinate.get().premiseTypeProperty().set(PremiseType.INFERRED);
				}
			});
			break;
		case INFERRED:
			resetRootPanePremiseTypeMenuItemText(PremiseType.STATED);
			resetRootPaneTooltipText(PremiseType.STATED);
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
			
			rootScrollPane = new ScrollPane();
			
			rootScrollPane.setContextMenu(new ContextMenu());

			rootScrollPane.getContextMenu().getItems().add(rootPanePremiseTypeMenuItem);
			
			MenuItem mi = new MenuItem("Copy Text Graph");
			mi.visibleProperty().bind(textGraph.textProperty().isNotNull());
			mi.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent arg0) {
					CustomClipboard.set(title.getText() + "\n" + textGraph.getText());
				}
			});
			rootScrollPane.getContextMenu().getItems().add(mi);
			
			taxonomyCoordinate.get().premiseTypeProperty().addListener(new ChangeListener<PremiseType>() {
				@Override
				public void changed(
						ObservableValue<? extends PremiseType> observable,
						PremiseType oldValue, PremiseType newValue) {
					if (newValue == PremiseType.INFERRED) {
						resetRootPanePremiseTypeMenuItemText(PremiseType.STATED);
						resetRootPaneTooltipText(PremiseType.STATED);
					} else {
						resetRootPanePremiseTypeMenuItemText(PremiseType.INFERRED);
						resetRootPaneTooltipText(PremiseType.INFERRED);
					}
				}
				
			});
			updateRootPanePremiseTypeMenuItem();
			
			vbox.getChildren().addAll(title, logicalExpressionTreeGraph, textGraph);

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
		displayData(loadData());
		
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
	
	private LogicalExpression loadData() {
		Optional<SememeChronology<? extends SememeVersion<?>>> defChronologyOptional = taxonomyCoordinate.get().getTaxonomyType() == PremiseType.STATED ? Get.statedDefinitionChronology(conceptId) : Get.inferredDefinitionChronology(conceptId);

		SememeChronology rawDefChronology = defChronologyOptional.get();
		Optional<LatestVersion<LogicGraphSememeImpl>> latestGraphLatestVersionOptional = rawDefChronology.getLatestVersion(LogicGraphSememeImpl.class, taxonomyCoordinate.get().getStampCoordinate());
		LogicGraphSememeImpl latestGraph = latestGraphLatestVersionOptional.get().value();	
		
		LogicalExpressionOchreImpl le = new LogicalExpressionOchreImpl(latestGraph.getGraphData(), DataSource.INTERNAL, Get.identifierService().getConceptSequence(latestGraph.getReferencedComponentNid()));

		return le;
	}
	
	private void displayData(LogicalExpression le) {
		title.setText(taxonomyCoordinate.get().getTaxonomyType().name() + " Logic Graph for Concept " + Get.conceptDescriptionText(conceptId));
		
		logicalExpressionTreeGraph.getChildren().clear();
		logicalExpressionTreeGraph.displayLogicalExpression(le, taxonomyCoordinate.get().getStampCoordinate(), taxonomyCoordinate.get().getLanguageCoordinate());
		
		textGraph.setText(le.toString());
	}

	@Override
	public void viewDiscarded() {
		clear();

		if (passedObservableTaxonomyCoordinate != null) {
			unbindFromPassedObservableTaxonomyCoordinate();
			passedObservableTaxonomyCoordinate = null;
		}
	}

	@Override
	public void setConcept(int id) {
		if (conceptId != null && conceptId != id) {
			clear();
		}

		conceptId = id;
		
		init();
		
		refresh();
	}

	@Override
	public void setConcept(UUID uuid) {
		uuid.getClass(); // Throw NPE if null
		Utility.execute(() -> setConcept(Get.identifierService().getConceptSequenceForUuids(uuid)));
	}

	@Override
	public void setConcept(
			TaxonomyCoordinate taxonomyCoordinate,
			int componentNid) {
		if (passedObservableTaxonomyCoordinate != null) {
			unbindFromPassedObservableTaxonomyCoordinate();
		}
		passedObservableTaxonomyCoordinate = null;
		
		this.taxonomyCoordinate.get().languageCoordinateProperty().set(new ObservableLanguageCoordinateImpl(taxonomyCoordinate.getLanguageCoordinate()));
		this.taxonomyCoordinate.get().stampCoordinateProperty().set(new ObservableStampCoordinateImpl(taxonomyCoordinate.getStampCoordinate()));
		this.taxonomyCoordinate.get().logicCoordinateProperty().set(new ObservableLogicCoordinateImpl(taxonomyCoordinate.getLogicCoordinate()));
		this.taxonomyCoordinate.get().premiseTypeProperty().set(taxonomyCoordinate.getTaxonomyType());
		this.taxonomyCoordinate.get().uuidProperty().set(taxonomyCoordinate.getUuid());
		
		setConcept(componentNid);
	}

	private void unbindFromPassedObservableTaxonomyCoordinate() {
		this.taxonomyCoordinate.get().languageCoordinateProperty().bind(passedObservableTaxonomyCoordinate.languageCoordinateProperty());
		this.taxonomyCoordinate.get().stampCoordinateProperty().bind(passedObservableTaxonomyCoordinate.stampCoordinateProperty());
		this.taxonomyCoordinate.get().logicCoordinateProperty().bind(passedObservableTaxonomyCoordinate.logicCoordinateProperty());
		this.taxonomyCoordinate.get().premiseTypeProperty().bind(passedObservableTaxonomyCoordinate.premiseTypeProperty());
		this.taxonomyCoordinate.get().uuidProperty().bind(passedObservableTaxonomyCoordinate.uuidProperty());
	}
	@Override
	public void setConcept(ObservableTaxonomyCoordinate taxonomyCoordinate,
			int componentNid) {
		if (passedObservableTaxonomyCoordinate != null) {
			unbindFromPassedObservableTaxonomyCoordinate();
		}
		passedObservableTaxonomyCoordinate = taxonomyCoordinate;
		
		this.taxonomyCoordinate.get().languageCoordinateProperty().bind(taxonomyCoordinate.languageCoordinateProperty());
		this.taxonomyCoordinate.get().stampCoordinateProperty().bind(taxonomyCoordinate.stampCoordinateProperty());
		this.taxonomyCoordinate.get().logicCoordinateProperty().bind(taxonomyCoordinate.logicCoordinateProperty());
		this.taxonomyCoordinate.get().premiseTypeProperty().bind(taxonomyCoordinate.premiseTypeProperty());
		this.taxonomyCoordinate.get().uuidProperty().bind(taxonomyCoordinate.uuidProperty());
		
		setConcept(componentNid);
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
		logicalExpressionTreeGraph.getChildren().clear();
		logicalExpressionTreeGraph.setRootNode(null);
		textGraph.setText(null);
		title.setText(null);
		conceptId = null;
	}
}
