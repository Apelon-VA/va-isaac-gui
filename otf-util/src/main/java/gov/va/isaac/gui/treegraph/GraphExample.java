package gov.va.isaac.gui.treegraph;

import javafx.scene.control.Label;
import javafx.application.Application;
import javafx.scene.shape.Circle;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.stage.Stage;

public class GraphExample extends Application {
	
	protected void init(Stage primaryStage) {
		TreeGraph graph = new TreeGraph();
		
		TreeNode node1 = new TreeNode(null, new Label("Node 1"));

		TreeNode node2 = new TreeNode(node1, new Label("Node 2"));
		node1.addChildTreeNodeBelow(node2);
		
		
		TreeNode node4 = new TreeNode(node2, new Label("Node 4"));
		node2.addChildTreeNodeBelow(node4);
		TreeNode node5 = new TreeNode(node2, new Label("Node 5"));
		node2.addChildTreeNodeBelow(node5);
		
		TreeNode node6 = new TreeNode(node1, new Label("Node 6"));
		node1.addChildTreeNodeBelow(node6);

		Label label3 = new Label("Node 3");
		label3.setShape(new Circle(50));
		TreeNode node3 = new TreeNode(node1, label3);
		node1.setChildToRight(node3);	

		TreeNode node7 = new TreeNode(node4, new Label("Node 7"));
		node4.setChildToRight(node7);
		
		graph.setRootNode(node1);
		ScrollPane scrollPane = new ScrollPane();
		scrollPane.setVbarPolicy(ScrollBarPolicy.ALWAYS);
		scrollPane.setHbarPolicy(ScrollBarPolicy.ALWAYS);
		scrollPane.setContent(graph);
		primaryStage.setScene(new Scene(scrollPane, 500, 500));
	}
	
	public static void main(String[] args) { launch(args); }
	
	@Override
	public void start(Stage primaryStage) throws Exception {
        init(primaryStage);
        primaryStage.show();
    }
}
