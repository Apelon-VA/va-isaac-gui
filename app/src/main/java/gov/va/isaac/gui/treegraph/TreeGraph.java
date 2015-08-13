package gov.va.isaac.gui.treegraph;

import javafx.scene.Group;

public class TreeGraph extends Group {

	public void addTreeNode(TreeNode treeNode) {
		getChildren().add(treeNode);
	}
}
