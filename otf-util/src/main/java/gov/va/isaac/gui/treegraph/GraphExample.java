package gov.va.isaac.gui.treegraph;

import java.util.function.BiFunction;

import javafx.scene.layout.Region;
import javafx.scene.control.Label;
import javafx.application.Application;
import javafx.scene.shape.Circle;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.stage.Stage;

public class GraphExample extends Application {
	private <T extends TreeNode<T>> void populateGraph(TreeGraph graph, BiFunction<T, Region, T> factory) {
		Label label1 = new Label("Node 1");
		T node1 = factory.apply(null, label1);

		Label label2 = new Label("Node 2");
		T node2 = factory.apply(node1, label2);
		node1.addChildTreeNodeBelow(node2);
		
		Label label4 = new Label("Node 4");
		T node4 = factory.apply(node2, label4);
		node2.addChildTreeNodeBelow(node4);
		Label label5 = new Label("Node 5");
		T node5 = factory.apply(node2, label5);
		node2.addChildTreeNodeBelow(node5);
		
		Label label6 = new Label("Node 6");
		T node6 = factory.apply(node1, label6);
		node1.addChildTreeNodeBelow(node6);

		Label label3 = new Label("Node 3");
		label3.setShape(new Circle(50));
		T node3 = factory.apply(node1, label3);
		node1.setChildToRight(node3);	

		Label label7 = new Label("Node 7");
		T node7 = factory.apply(node4, label7);
		node4.setChildToRight(node7);
		
		graph.setRootNode(node1);
	}
	
	protected void init(Stage primaryStage) {
		TreeGraph graph = new TreeGraph();

		populateGraph(graph, new BiFunction<TreeNodeImpl, Region, TreeNodeImpl>() {
			@Override
			public TreeNodeImpl apply(TreeNodeImpl parent, Region fxNode) {
				return new TreeNodeImpl(parent, fxNode);
			}
		});
//		populateGraph(graph, new BiFunction<TreeNodeImpl, Region, TreeNodeImpl>() {
//			@Override
//			public ResizableTreeNodeImpl apply(ResizableTreeNodeImpl parent, Region fxNode) {
//				return new ResizableTreeNodeImpl(parent, fxNode);
//			}
//		});
		
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
