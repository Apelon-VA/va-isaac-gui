package gov.va.isaac.logic.treeview;

import gov.va.isaac.AppContext;
import gov.va.isaac.interfaces.gui.views.commonFunctionality.LogicalExpressionTreeGraphViewI;
import gov.vha.isaac.metadata.coordinates.StampCoordinates;
import gov.vha.isaac.ochre.api.DataSource;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.chronicle.LatestVersion;
import gov.vha.isaac.ochre.api.component.sememe.SememeChronology;
import gov.vha.isaac.ochre.api.component.sememe.version.SememeVersion;
import gov.vha.isaac.ochre.model.logic.LogicalExpressionOchreImpl;
import gov.vha.isaac.ochre.model.sememe.version.LogicGraphSememeImpl;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import javafx.application.Platform;
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
public class LogicalExpressionTreeGraphView implements LogicalExpressionTreeGraphViewI {
	private LogicalExpressionTreeGraph logicalExpressionTreeGraph;
	private Label textGraph;
	
	private ScrollPane rootScrollPane;
	private AtomicInteger noRefresh_ = new AtomicInteger(0);
	
	int conceptId;
	
	private Logger logger_ = LoggerFactory.getLogger(this.getClass());
	
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
			
			logicalExpressionTreeGraph = new LogicalExpressionTreeGraph(
					true, 
					true, 
					200, 100);
			textGraph = new Label();
			
			rootScrollPane = new ScrollPane();
			
			vbox.getChildren().addAll(logicalExpressionTreeGraph, textGraph);

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
		Optional<SememeChronology<? extends SememeVersion<?>>> defChronologyOptional = Get.statedDefinitionChronology(conceptId);

		SememeChronology rawDefChronology = defChronologyOptional.get();
		Optional<LatestVersion<LogicGraphSememeImpl>> latestGraphLatestVersionOptional = rawDefChronology.getLatestVersion(LogicGraphSememeImpl.class, StampCoordinates.getDevelopmentLatest());
		LogicGraphSememeImpl latestStatedGraph = latestGraphLatestVersionOptional.get().value();	
		
		LogicalExpressionOchreImpl lg = new LogicalExpressionOchreImpl(latestStatedGraph.getGraphData(), DataSource.INTERNAL, Get.identifierService().getConceptSequence(latestStatedGraph.getReferencedComponentNid()));

		logicalExpressionTreeGraph.displayLogicalExpression(lg);
		
		textGraph.setText(lg.toString());
	}

	@Override
	public void viewDiscarded() {
		logicalExpressionTreeGraph.setRootNode(null);
	}

	@Override
	public void setConcept(int id) {
		conceptId = id;
		
		init();
		
		refresh();
	}

	@Override
	public void setConcept(UUID uuid) {
		setConcept(Get.identifierService().getConceptSequenceForUuids(uuid));
	}
}
