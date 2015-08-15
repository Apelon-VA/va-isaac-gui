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
		
		double defaultWidth = 100;
		double defaultHeight = 50;
		
		Label label1 = new Label("Node 1");
		TreeNode node1 = new TreeNode(null, label1);

		Label label2 = new Label("Node 2");
		TreeNode node2 = new TreeNode(node1, label2);
		node1.addChildTreeNodeBelow(node2);
		
		Label label4 = new Label("Node 4");
		TreeNode node4 = new TreeNode(node2, label4);
		node2.addChildTreeNodeBelow(node4);
		Label label5 = new Label("Node 5");
		TreeNode node5 = new TreeNode(node2, label5);
		node2.addChildTreeNodeBelow(node5);
		
		Label label6 = new Label("Node 6");
		TreeNode node6 = new TreeNode(node1, label6);
		node1.addChildTreeNodeBelow(node6);

		Label label3 = new Label("Node 3");
		label3.setShape(new Circle(50));
		TreeNode node3 = new TreeNode(node1, label3);
		node1.setChildToRight(node3);	

		Label label7 = new Label("Node 7");
		TreeNode node7 = new TreeNode(node4, label7);
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
