package gov.va.isaac.gui.treegraph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.ReadOnlyListWrapper;
import javafx.collections.ObservableList;

public class TreeNode extends Group {
	private final static double preferredWidth = 200;
	private final static double preferredHeight = 50;
	
	private final static double vertSpaceBetweenChildNodes = 10;
	private final static double horizontalSpaceBetweenParentAndChildNodes = 50;
	
	final TreeNode parentTreeNode;
	TreeNode childToRight;
	
	final Node fxNode;
	final List<TreeNode> childTreeNodes = new ArrayList<TreeNode>();
	
	public TreeNode(TreeNode parentTreeNode, Node fxNode) {
		super(fxNode);

		fxNode.prefHeight(preferredHeight);
		fxNode.prefWidth(preferredWidth);
		fxNode.maxHeight(preferredHeight);
		fxNode.maxWidth(preferredWidth);
		fxNode.minHeight(preferredHeight);
		fxNode.minWidth(preferredWidth);
		
		this.parentTreeNode = parentTreeNode;
		this.fxNode = fxNode;
	}

	public Node getFxNode() {
		return fxNode;
	}
	
	public TreeNode getChildToRight() {
		return childToRight;
	}
	public void setChildToRight(TreeNode treeNode) {
		if (childToRight != null) {
			this.getChildren().remove(childToRight);
		}
		childToRight = treeNode;
		
		childToRight.setLayoutX(fxNode.getLayoutBounds().getMaxX() + horizontalSpaceBetweenParentAndChildNodes);
		childToRight.setLayoutY(fxNode.getLayoutY());
		
		this.getChildren().add(childToRight);
	}
	
	public double getHeight() {
		double height = preferredHeight;
		
		for (TreeNode treeNode : childTreeNodes) {
			height = height + vertSpaceBetweenChildNodes + treeNode.getHeight();
		}
		return height;
	}

	public double getMaxX() {
		return childToRight == null ? fxNode.getLayoutBounds().getMaxX() : childToRight.getMaxX();
	}
	
	public TreeNode getParentTreeNode() { return parentTreeNode; }
	
	public List<TreeNode> getChildTreeNodesBelow() {
		return childTreeNodes;
	}

	public void addChildTreeNodeBelow(TreeNode childTreeNode) {
		double childLayoutX = fxNode.getLayoutX() + horizontalSpaceBetweenParentAndChildNodes; // just an arbitrary indent
		double childLayoutY = fxNode.getLayoutY() + getHeight() + vertSpaceBetweenChildNodes;
		
		childTreeNode.getFxNode().setLayoutX(childLayoutX);
		childTreeNode.getFxNode().setLayoutY(childLayoutY);
		
		// TODO create connector here
		
		this.getChildren().add(childTreeNode);
		childTreeNodes.add(childTreeNode);
	}
}
