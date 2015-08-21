package gov.va.isaac.logic.treeview;

import gov.va.isaac.AppContext;
import gov.va.isaac.config.profiles.UserProfileBindings;
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

import javafx.geometry.Pos;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
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
            logger_.debug("Kicking off tree refresh() due to change of user preference property");
            LogicalExpressionTreeGraphView.this.refresh();
            return false;
        }
    };

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
			
			logicalExpressionTreeGraph = new LogicalExpressionTreeGraph(
					true, 
					true, 
					200, 100);
			textGraph = new Label();
			
			rootScrollPane = new ScrollPane();
			
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
		if (noRefresh_.get() > 0)
		{
			logger_.info("Skip refresh of LogicalExpressionTreeGraphView due to wait count {}", noRefresh_.get());
			return;
		}
		else
		{
			noRefresh_.getAndIncrement();
		}

		Platform.runLater(() ->
		{
			try
			{
				loadData();
			}
			catch (Exception e)
			{
				logger_.error("Unexpected error building the LogicalExpressionTreeGraphView display", e);
				//null check, as the error may happen before the scene is visible
				AppContext.getCommonDialogs().showErrorDialog("Error", "There was an unexpected error building the LogicalExpressionTreeGraphView display", e.getMessage(), 
						(rootScrollPane.getScene() == null ? null : rootScrollPane.getScene().getWindow()));
			}
			finally
			{
				noRefresh_.decrementAndGet();
			}
		});
	}
	
	private void loadData() {
		Optional<SememeChronology<? extends SememeVersion<?>>> defChronologyOptional = taxonomyCoordinate.get().getTaxonomyType() == PremiseType.STATED ? Get.statedDefinitionChronology(conceptId) : Get.inferredDefinitionChronology(conceptId);

		SememeChronology rawDefChronology = defChronologyOptional.get();
		Optional<LatestVersion<LogicGraphSememeImpl>> latestGraphLatestVersionOptional = rawDefChronology.getLatestVersion(LogicGraphSememeImpl.class, taxonomyCoordinate.get().getStampCoordinate());
		LogicGraphSememeImpl latestGraph = latestGraphLatestVersionOptional.get().value();	
		
		LogicalExpressionOchreImpl lg = new LogicalExpressionOchreImpl(latestGraph.getGraphData(), DataSource.INTERNAL, Get.identifierService().getConceptSequence(latestGraph.getReferencedComponentNid()));

		title.setText(taxonomyCoordinate.get().getTaxonomyType().name() + " Logic Graph for Concept " + Get.conceptDescriptionText(conceptId));
		
		logicalExpressionTreeGraph.displayLogicalExpression(lg, taxonomyCoordinate.get().getStampCoordinate(), taxonomyCoordinate.get().getLanguageCoordinate());
		
		textGraph.setText(lg.toString());
	}

	@Override
	public void viewDiscarded() {
		title.setText(null);
		logicalExpressionTreeGraph.setRootNode(null);
		textGraph.setText(null);
	}

	@Override
	public void setConcept(int id) {
		conceptId = id;
		
		init();
		
		refresh();
	}

	@Override
	public void setConcept(UUID uuid) {
		Utility.execute(() -> setConcept(Get.identifierService().getConceptSequenceForUuids(uuid)));
	}

	@Override
	public void setConcept(
			TaxonomyCoordinate<? extends TaxonomyCoordinate<?>> taxonomyCoordinate,
			int componentNid) {
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

		this.taxonomyCoordinate.get().languageCoordinateProperty().bind(taxonomyCoordinate.languageCoordinateProperty());
		this.taxonomyCoordinate.get().stampCoordinateProperty().bind(taxonomyCoordinate.stampCoordinateProperty());
		this.taxonomyCoordinate.get().logicCoordinateProperty().bind(taxonomyCoordinate.logicCoordinateProperty());
		this.taxonomyCoordinate.get().premiseTypeProperty().bind(taxonomyCoordinate.premiseTypeProperty());
		this.taxonomyCoordinate.get().uuidProperty().bind(taxonomyCoordinate.uuidProperty());
		
		setConcept(componentNid);
	}

	@Override
	public void setConcept(
			TaxonomyCoordinate<? extends TaxonomyCoordinate<?>> taxonomyCoordinate,
			UUID uuid) {
		Utility.execute(() -> setConcept(taxonomyCoordinate, Get.identifierService().getConceptSequenceForUuids(uuid)));
	}

	@Override
	public void setConcept(ObservableTaxonomyCoordinate taxonomyCoordinate,
			UUID uuid) {
		Utility.execute(() -> setConcept(taxonomyCoordinate, Get.identifierService().getConceptSequenceForUuids(uuid)));
	}
}
