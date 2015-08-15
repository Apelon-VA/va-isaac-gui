package gov.va.isaac.gui.treegraph;

import javafx.scene.layout.Region;

public class TreeNodeUtils {
	private TreeNodeUtils() {};
	
	public static void setFxNodeSizes(Region fxNode, int width, int height) {
		fxNode.setMaxHeight(height);
		fxNode.setMaxWidth(width);
		fxNode.setPrefHeight(height);
		fxNode.setPrefWidth(width);
		fxNode.setMinHeight(height);
		fxNode.setMinWidth(width);
	}
}
