package gov.va.isaac.gui.treegraph;

import javafx.scene.Group;

public class TreeGraph extends Group {
	public TreeNode getRootNode() {
		return getChildren().size() > 0 ? (TreeNode)getChildren().get(0) : null;
	}

	public void setRootNode(TreeNode treeNode) {
		if (getChildren().size() > 0) {
			getChildren().clear();
		}
		getChildren().add(treeNode);
	}
}
