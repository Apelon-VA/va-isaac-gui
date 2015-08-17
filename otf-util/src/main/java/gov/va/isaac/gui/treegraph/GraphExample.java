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
	private final static int defaultWidth = 200;
	private final static int defaultHeight = 100;
	
	private <T extends TreeNode<T>> void populateGraph(TreeGraph graph, BiFunction<T, Region, T> factory) {
		Label label1 = new Label("Node 1");
		TreeNodeUtils.setFxNodeSizes(label1, defaultWidth, defaultHeight);
		T node1 = factory.apply(null, label1);

		Label label2 = new Label("Node 2");
		TreeNodeUtils.setFxNodeSizes(label2, 100, 50);
		T node2 = factory.apply(node1, label2);
		node1.addChildTreeNodeBelow(node2);
		
		Label label4 = new Label("Node 4");
		TreeNodeUtils.setFxNodeSizes(label4, 100, 50);
		T node4 = factory.apply(node2, label4);
		node2.addChildTreeNodeBelow(node4);

		Label label7 = new Label("Node 7");
		TreeNodeUtils.setFxNodeSizes(label7, defaultWidth, defaultHeight);
		T node7 = factory.apply(node4, label7);
		node4.setChildToRight(node7);
		Label label5 = new Label("Node 5");
		TreeNodeUtils.setFxNodeSizes(label5, defaultWidth, defaultHeight);
		T node5 = factory.apply(node2, label5);
		node2.addChildTreeNodeBelow(node5);
		
		Label label6 = new Label("Node 6");
		TreeNodeUtils.setFxNodeSizes(label6, defaultWidth, defaultHeight);
		T node6 = factory.apply(node1, label6);
		node1.addChildTreeNodeBelow(node6);

		Label label3 = new Label("Node 3");
		label3.setShape(new Circle(50));
		TreeNodeUtils.setFxNodeSizes(label3, 100, 100);
		T node3 = factory.apply(node1, label3);
		node1.setChildToRight(node3);	

		graph.setRootNode(node1);
	}
	
	private void populateGraphWithResizableTreeNode(TreeGraph graph) {
		Label label1 = new Label("Node 1");
		ResizableTreeNodeImpl node1 = new ResizableTreeNodeImpl(null, label1);

		Label label2 = new Label("Node 2");
		ResizableTreeNodeImpl node2 = new ResizableTreeNodeImpl(node1, label2);
		node1.addChildTreeNodeBelow(node2);
		
		Label label4 = new Label("Node 4");
		ResizableTreeNodeImpl node4 = new ResizableTreeNodeImpl(node2, label4);
		node2.addChildTreeNodeBelow(node4);

		Label label7 = new Label("Node 7");
		ResizableTreeNodeImpl node7 = new ResizableTreeNodeImpl(node4, label7);
		node4.setChildToRight(node7);
		Label label5 = new Label("Node 5");
		ResizableTreeNodeImpl node5 = new ResizableTreeNodeImpl(node2, label5);
		node2.addChildTreeNodeBelow(node5);
		
		Label label6 = new Label("Node 6");
		ResizableTreeNodeImpl node6 = new ResizableTreeNodeImpl(node1, label6);
		node1.addChildTreeNodeBelow(node6);

		Label label3 = new Label("Node 3");
		label3.setShape(new Circle(50));
		ResizableTreeNodeImpl node3 = new ResizableTreeNodeImpl(node1, label3);
		node1.setChildToRight(node3);	

		graph.setRootNode(node1);
	}
	
	protected void init(Stage primaryStage) {
		TreeGraph graph = new TreeGraph();

//		populateGraphWithResizableTreeNode(graph);
		populateGraph(graph, new BiFunction<TreeNodeImpl, Region, TreeNodeImpl>() {
			@Override
			public TreeNodeImpl apply(TreeNodeImpl parent, Region fxNode) {
				return new TreeNodeImpl(parent, fxNode);
			}
		});
//		populateGraph(graph, new BiFunction<ResizableTreeNodeImpl, Region, ResizableTreeNodeImpl>() {
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
