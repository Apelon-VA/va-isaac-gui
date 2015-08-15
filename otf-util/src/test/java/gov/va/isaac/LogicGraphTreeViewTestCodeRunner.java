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
package gov.va.isaac;

import gov.va.isaac.config.profiles.UserProfileManager;
import gov.va.isaac.gui.treegraph.TreeGraph;
import gov.va.isaac.gui.treegraph.TreeNodeImpl;
import gov.va.isaac.init.SystemInit;
import gov.vha.isaac.metadata.coordinates.StampCoordinates;
import gov.vha.isaac.ochre.api.DataSource;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.LookupService;
import gov.vha.isaac.ochre.api.chronicle.LatestVersion;
import gov.vha.isaac.ochre.api.component.sememe.SememeChronology;
import gov.vha.isaac.ochre.api.component.sememe.version.SememeVersion;
import gov.vha.isaac.ochre.api.logic.Node;
import gov.vha.isaac.ochre.model.logic.LogicalExpressionOchreImpl;
import gov.vha.isaac.ochre.model.logic.node.RootNode;
import gov.vha.isaac.ochre.model.logic.node.SufficientSetNode;
import gov.vha.isaac.ochre.model.logic.node.external.ConceptNodeWithUuids;
import gov.vha.isaac.ochre.model.logic.node.external.TypedNodeWithUuids;
import gov.vha.isaac.ochre.model.logic.node.internal.ConceptNodeWithSequences;
import gov.vha.isaac.ochre.model.logic.node.internal.FeatureNodeWithSequences;
import gov.vha.isaac.ochre.model.logic.node.internal.RoleNodeAllWithSequences;
import gov.vha.isaac.ochre.model.logic.node.internal.RoleNodeSomeWithSequences;
import gov.vha.isaac.ochre.model.logic.node.internal.TypedNodeWithSequences;
import gov.vha.isaac.ochre.model.sememe.version.LogicGraphSememeImpl;

import java.io.File;
import java.util.Optional;
import java.util.UUID;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link LogicGraphTreeViewTestCodeRunner}
 * 
 * @author <a href="mailto:joel.kniaz@gmail.com">Joel Kniaz</a>
 */
public class LogicGraphTreeViewTestCodeRunner extends Application
{
	private static final Logger LOG = LoggerFactory.getLogger(LogicGraphTreeViewTestCodeRunner.class);

	interface LogicGraphSememeHandler {
		void handle(LogicGraphSememeImpl lgs, int id);
	};

	private TreeGraph graph = new TreeGraph();
	private Label textGraph = new Label();

	protected void init(Stage primaryStage) {
		VBox vbox = new VBox();
		vbox.getChildren().addAll(graph, textGraph);

		ScrollPane scrollPane = new ScrollPane();
		scrollPane.setVbarPolicy(ScrollBarPolicy.ALWAYS);
		scrollPane.setHbarPolicy(ScrollBarPolicy.ALWAYS);
		scrollPane.setContent(vbox);
		primaryStage.setScene(new Scene(scrollPane, 500, 500));
	}

	// for testing graph population and panel display only
	private void populateTestGraph() {
		TreeNodeImpl node1 = new TreeNodeImpl(null, new Label("Node 1"));

		TreeNodeImpl node2 = new TreeNodeImpl(node1, new Label("Node 2"));
		node1.addChildTreeNodeBelow(node2);

		TreeNodeImpl node4 = new TreeNodeImpl(node2, new Label("Node 4"));
		node2.addChildTreeNodeBelow(node4);
		TreeNodeImpl node5 = new TreeNodeImpl(node2, new Label("Node 5"));
		node2.addChildTreeNodeBelow(node5);

		TreeNodeImpl node6 = new TreeNodeImpl(node1, new Label("Node 6"));
		node1.addChildTreeNodeBelow(node6);

		Label label3 = new Label("Node 3");
		label3.setShape(new Circle(50));
		TreeNodeImpl node3 = new TreeNodeImpl(node1, label3);
		node1.setChildToRight(node3);	

		TreeNodeImpl node7 = new TreeNodeImpl(node4, new Label("Node 7"));
		node4.setChildToRight(node7);

		graph.setRootNode(node1);
	}
	/**
	 * @see javafx.application.Application#start(javafx.stage.Stage)
	 */
	@Override
	public void start(Stage primaryStage) throws Exception
	{
		init(primaryStage);
		primaryStage.show();

		String uuidString = getParameters().getRaw().size() > 0 ? getParameters().getRaw().get(0) : "89ce6b87-545b-3138-82c7-aafa76f8f9a0";
		UUID uuid = UUID.fromString(uuidString);

		LogicGraphSememeHandler handler = new LogicGraphSememeHandler() {
			public void handle(LogicGraphSememeImpl lgs, int id) {
				System.out.println("STATED LogicGraph for " + Get.conceptDescriptionText(id) + ":\n" + lgs.toString());

				LogicalExpressionOchreImpl lg = new LogicalExpressionOchreImpl(lgs.getGraphData(), DataSource.INTERNAL, Get.identifierService().getConceptSequence(lgs.getReferencedComponentNid()));

				processLogicalExpression(lg);
				
				textGraph.setText(lg.toString());
			}
		};
		//		handler = new LogicGraphSememeHandler() {
		//			public void handle(LogicGraphSememeImpl lgs, int id) {
		//				System.out.println("STATED LogicGraph for " + Get.conceptDescriptionText(id) + ":\n" + lgs.toString());
		//			}
		//		};

		processUuid(uuid, handler);

		//populateTestGraph();
	}

	public void processUuid(UUID uuid, LogicGraphSememeHandler handler) {
		int nid = Get.identifierService().getNidForUuids(uuid);

		Optional<SememeChronology<? extends SememeVersion<?>>> defChronologyOptional = Get.statedDefinitionChronology(nid);

		SememeChronology rawDefChronology = defChronologyOptional.get();
		Optional<LatestVersion<LogicGraphSememeImpl>> latestGraphLatestVersionOptional = rawDefChronology.getLatestVersion(LogicGraphSememeImpl.class, StampCoordinates.getDevelopmentLatest());
		LogicGraphSememeImpl latestStatedGraph = latestGraphLatestVersionOptional.get().value();

		handler.handle(latestStatedGraph, nid);
	}

	public static void main(String[] args) throws Exception
	{
		Exception dataStoreLocationInitException = SystemInit.doBasicSystemInit(new File("../../va-isaac-gui-pa/app-assembly/"));
		if (dataStoreLocationInitException != null)
		{
			System.err.println("Configuration of datastore path failed.  DB will not be able to start properly!  " + dataStoreLocationInitException);
			System.exit(-1);
		}
		LookupService.startupIsaac();
		AppContext.getService(UserProfileManager.class).configureAutomationMode(new File("profiles"));
		launch(args);

		// TODO: get data needed and shut down immediately, rather than after panel closed
		LookupService.shutdownIsaac();
	}

	public void processLogicalExpression(LogicalExpressionOchreImpl le) {
		System.out.println("Processing LogicalExpression for concept " + Get.conceptDescriptionText(le.getConceptSequence()));
		System.out.println("Root is " + le.getRoot().getNodeSemantic().name());

		if (le.getNodeCount() > 0) {
			processLogicalNode(null, null, le.getNode(0));
		}
	}
	private static String logicalNodeTypeToString(Node node) {
		return node.getClass().getName().replaceAll(".*\\.", "");
	}
	public void processLogicalNode(TreeNodeImpl parentTreeNode, Node parentLogicalNode, Node logicalNode) {
		System.out.println("Processing " + logicalNode.getNodeSemantic().name() + " node");

		TreeNodeImpl currentTreeNode = null;
		if (parentTreeNode == null) {
			currentTreeNode = new TreeNodeImpl(null, createLabelFromLogicalNode(logicalNode));
			graph.setRootNode(currentTreeNode);
		} else {
			// TODO: Properly handle nodes that are to right rather than below
			if (parentLogicalNode != null && (parentLogicalNode instanceof TypedNodeWithSequences || parentLogicalNode instanceof TypedNodeWithUuids)) {
				parentTreeNode.setChildToRight(currentTreeNode = new TreeNodeImpl(parentTreeNode, createLabelFromLogicalNode(logicalNode)));
			} 
			else {
				parentTreeNode.addChildTreeNodeBelow(currentTreeNode = new TreeNodeImpl(parentTreeNode, createLabelFromLogicalNode(logicalNode)));
			}
		}
		for (Node child : logicalNode.getChildren()) {
			processLogicalNode(currentTreeNode, logicalNode, child);
		}
	}

	// TODO: properly populate all labels
	public static Label createLabelFromLogicalNode(Node logicalNode) {
		Label label = null;
		if (logicalNode instanceof ConceptNodeWithSequences) {
			label = new Label(logicalNode.getNodeSemantic().name() + "\n" + logicalNodeTypeToString(logicalNode) + "\n" + Get.conceptDescriptionText(((ConceptNodeWithSequences)logicalNode).getConceptSequence()));
		} else if (logicalNode instanceof ConceptNodeWithUuids) {
			label = new Label(logicalNode.getNodeSemantic().name() + "\n" + logicalNodeTypeToString(logicalNode) + "\n" + Get.conceptDescriptionText(Get.identifierService().getConceptSequenceForUuids(((ConceptNodeWithUuids)logicalNode).getConceptUuid())));
		} else if (logicalNode instanceof FeatureNodeWithSequences) {
			label = new Label(logicalNode.getNodeSemantic().name() + "\n" + logicalNodeTypeToString(logicalNode) + "\ntype=" + Get.conceptDescriptionText(((FeatureNodeWithSequences)logicalNode).getTypeConceptSequence()) + "\noperator=" + ((FeatureNodeWithSequences)logicalNode).getOperator().name());
		} else if (logicalNode instanceof RoleNodeAllWithSequences) {
			label = new Label(logicalNode.getNodeSemantic().name() + "\n" + logicalNodeTypeToString(logicalNode) + "\ntype=" + Get.conceptDescriptionText(((RoleNodeAllWithSequences)logicalNode).getTypeConceptSequence()));
		} else if (logicalNode instanceof RoleNodeSomeWithSequences) {
			label = new Label(logicalNode.getNodeSemantic().name() + "\n" + logicalNodeTypeToString(logicalNode) + "\ntype=" + Get.conceptDescriptionText(((RoleNodeSomeWithSequences)logicalNode).getTypeConceptSequence()));
		} else if (logicalNode instanceof RootNode) {
			label = new Label(logicalNode.getNodeSemantic().name() + "\n" + logicalNodeTypeToString(logicalNode));
		} else if (logicalNode instanceof SufficientSetNode) {
			label = new Label(logicalNode.getNodeSemantic().name() + "\n" + logicalNodeTypeToString(logicalNode));
		}
		else {
			label = new Label(logicalNode.getNodeSemantic().name() + "\n" + logicalNodeTypeToString(logicalNode));
		}
		
		label.setTooltip(new Tooltip(label.getText()));

		return label;
	}
}
