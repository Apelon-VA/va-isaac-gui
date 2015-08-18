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
package gov.va.isaac.logic.treeview;

import gov.va.isaac.AppContext;
import gov.va.isaac.config.profiles.UserProfileManager;
import gov.va.isaac.gui.treegraph.TreeGraph;
import gov.va.isaac.gui.treegraph.TreeNodeImpl;
import gov.va.isaac.gui.treegraph.TreeNodeUtils;
import gov.va.isaac.init.SystemInit;
import gov.vha.isaac.metadata.coordinates.StampCoordinates;
import gov.vha.isaac.ochre.api.DataSource;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.LookupService;
import gov.vha.isaac.ochre.api.chronicle.LatestVersion;
import gov.vha.isaac.ochre.api.component.sememe.SememeChronology;
import gov.vha.isaac.ochre.api.component.sememe.version.SememeVersion;
import gov.vha.isaac.ochre.api.logic.LogicalExpression;
import gov.vha.isaac.ochre.api.logic.Node;
import gov.vha.isaac.ochre.model.logic.LogicalExpressionOchreImpl;
import gov.vha.isaac.ochre.model.logic.node.AndNode;
import gov.vha.isaac.ochre.model.logic.node.OrNode;
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
import java.util.function.BiConsumer;

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
	
	private LogicalExpressionTreeGraph graph = new LogicalExpressionTreeGraph(
			true, 
			true, 
			200, 100);
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

		/*
		 * Bleeding (finding) 89ce6b87-545b-3138-82c7-aafa76f8f9a0
		 * Fracture of radius c50138b9-70ee-3af2-b567-af2f20359925
		 * Entire skin (body structure) cbb0653c-bc87-37b0-aafb-6d020917e172
		 * Hand pain 9549a066-7d57-371d-8958-82a6a0b5b175
		 * Arthroscopy (procedure) 4bf05b37-076a-3a6a-ad53-b10bbf83cfc5
		 */
		String uuidString = getParameters().getRaw().size() > 0 ? getParameters().getRaw().get(0) : "4bf05b37-076a-3a6a-ad53-b10bbf83cfc5";
		UUID uuid = UUID.fromString(uuidString);

		BiConsumer<LogicGraphSememeImpl, Integer> handler = new BiConsumer<LogicGraphSememeImpl, Integer>() {
			public void accept(LogicGraphSememeImpl lgs, Integer id) {
				System.out.println("STATED LogicGraph for " + Get.conceptDescriptionText(id) + ":\n" + lgs.toString());

				LogicalExpressionOchreImpl lg = new LogicalExpressionOchreImpl(lgs.getGraphData(), DataSource.INTERNAL, Get.identifierService().getConceptSequence(lgs.getReferencedComponentNid()));

				graph.displayLogicalExpression(lg);
				
				textGraph.setText(lg.toString());
			}
		};
		//		handler = new BiConsumer<LogicGraphSememeImpl, Integer>() {
		//			public void accept(LogicGraphSememeImpl lgs, Integer id) {
		//				System.out.println("STATED LogicGraph for " + Get.conceptDescriptionText(id) + ":\n" + lgs.toString());
		//			}
		//		};

		processUuid(uuid, handler);

		//populateTestGraph();
	}

	public void processUuid(UUID uuid, BiConsumer<LogicGraphSememeImpl, Integer> consumer) {
		int nid = Get.identifierService().getNidForUuids(uuid);

		Optional<SememeChronology<? extends SememeVersion<?>>> defChronologyOptional = Get.statedDefinitionChronology(nid);

		SememeChronology rawDefChronology = defChronologyOptional.get();
		Optional<LatestVersion<LogicGraphSememeImpl>> latestGraphLatestVersionOptional = rawDefChronology.getLatestVersion(LogicGraphSememeImpl.class, StampCoordinates.getDevelopmentLatest());
		LogicGraphSememeImpl latestStatedGraph = latestGraphLatestVersionOptional.get().value();

		consumer.accept(latestStatedGraph, nid);
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
}
