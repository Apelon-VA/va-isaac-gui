package gov.va.isaac.gui.treegraph;

import java.util.ArrayList;

import javafx.beans.property.ListProperty;
import javafx.beans.property.ReadOnlyListProperty;
import javafx.beans.property.ReadOnlyListWrapper;
import javafx.beans.property.SimpleListProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Labeled;
import javafx.scene.Group;
import javafx.scene.layout.Region;
import javafx.scene.shape.Line;

import com.sun.javafx.collections.ObservableListWrapper;

public class TreeNodeImpl extends Group implements TreeNode<TreeNodeImpl> {
	private final static double vertSpaceBetweenChildNodes = 5;
	private final static double horizontalSpaceBetweenParentAndChildNodes = 20;
	
	private final TreeNodeImpl parentTreeNode;
	private TreeNodeImpl childToRight;
	
	private final Region fxNode;

	private final ListProperty<TreeNodeImpl> childTreeNodes = new SimpleListProperty<>(new ObservableListWrapper<>(new ArrayList<>()));
	private final ReadOnlyListWrapper<TreeNodeImpl> readOnlyChildTreeNodes = new ReadOnlyListWrapper<>(childTreeNodes);
	
	public TreeNodeImpl(TreeNodeImpl parentTreeNode, Region fxNode) {
		super(fxNode);
		
		fxNode.setStyle("-fx-border-color: black;");
		
		if (fxNode instanceof Labeled) {
			((Labeled)fxNode).setAlignment(Pos.CENTER);
		}
		
		this.parentTreeNode = parentTreeNode;
		this.fxNode = fxNode;
	}
	
	/* (non-Javadoc)
	 * @see gov.va.isaac.gui.treegraph.TreeNode#getRoot()
	 */
	@Override
	public TreeNodeImpl getRoot() {
		return this.parentTreeNode == null ? this : this.parentTreeNode.getRoot();
	}
	
	/* (non-Javadoc)
	 * @see gov.va.isaac.gui.treegraph.TreeNode#getFxNode()
	 */
	@Override
	public Region getFxNode() {
		return fxNode;
	}
	
	/* (non-Javadoc)
	 * @see gov.va.isaac.gui.treegraph.TreeNode#getChildToRight()
	 */
	@Override
	public TreeNodeImpl getChildToRight() {
		return childToRight;
	}
	/* (non-Javadoc)
	 * @see gov.va.isaac.gui.treegraph.TreeNode#setChildToRight(gov.va.isaac.gui.treegraph.TreeNodeImpl)
	 */
	@Override
	public void setChildToRight(TreeNodeImpl treeNode) {
		childToRight = treeNode;
		
		// Position childToRight
		childToRight.fxNode.setLayoutX(fxNode.getLayoutX() + fxNode.getMaxWidth() + horizontalSpaceBetweenParentAndChildNodes);
		childToRight.fxNode.setLayoutY(fxNode.getLayoutY());
		
		// add childToRight to FX Group
		this.getChildren().add(childToRight);
		
		// Add connector
		Coordinates startCoordinates = this.getRightConnectionPortCoordinates();
		Coordinates endCoordinates = childToRight.getLeftConnectionPortCoordinates();
		if (startCoordinates.getY() == endCoordinates.getY()) {
			// Simple case, in which parent and child are same height
			// Create connector of one Line instance
			Line line = new Line(startCoordinates.getX(), startCoordinates.getY(), endCoordinates.getX(), endCoordinates.getY());
			childToRight.getChildren().add(line); // add connector line to FX Group
		} else {
			// Complex case, in which parent and child are different heights
			// Create connector consisting of three separate Line instances
			double midpoint = startCoordinates.getX() + (endCoordinates.getX() - startCoordinates.getX()) / 2;
			Line leftHorizontal = new Line(startCoordinates.getX(), startCoordinates.getY(), midpoint, startCoordinates.getY());
			Line vertical = new Line(midpoint, startCoordinates.getY(), midpoint, endCoordinates.getY());
			Line rightHorizontal = new Line(midpoint, endCoordinates.getY(), endCoordinates.getX(), endCoordinates.getY());
			Group newCompoundLineGroup = new Group(leftHorizontal, vertical, rightHorizontal);
			childToRight.getChildren().add(newCompoundLineGroup);
		}
	}
	
	/* (non-Javadoc)
	 * @see gov.va.isaac.gui.treegraph.TreeNode#getHeight()
	 */
	@Override
	public double getHeight() {
		double heightOfFxNode = fxNode.getMaxHeight();
		
		double heightWithChildrenBelow = heightOfFxNode;
		for (TreeNodeImpl treeNode : childTreeNodes) {
			heightWithChildrenBelow += vertSpaceBetweenChildNodes + treeNode.getHeight();
		}
		
		double heightOfChildToRight = childToRight != null ? childToRight.getHeight() : 0;
		
		return Math.max(heightOfChildToRight, Math.max(heightOfFxNode, heightWithChildrenBelow));
	}

	/* (non-Javadoc)
	 * @see gov.va.isaac.gui.treegraph.TreeNode#getWidth()
	 */
	@Override
	public double getWidth() {
		return childToRight == null ? fxNode.getMaxWidth() : fxNode.getMaxWidth() + horizontalSpaceBetweenParentAndChildNodes + childToRight.getWidth();
	}
	
	/* (non-Javadoc)
	 * @see gov.va.isaac.gui.treegraph.TreeNode#getParentTreeNode()
	 */
	@Override
	public TreeNodeImpl getParentTreeNode() { return parentTreeNode; }
	
	/* (non-Javadoc)
	 * @see gov.va.isaac.gui.treegraph.TreeNode#getChildTreeNodesBelow()
	 */
	@Override
	public ReadOnlyListProperty<TreeNodeImpl> getChildTreeNodesBelow() {
		return readOnlyChildTreeNodes;
	}

	/* (non-Javadoc)
	 * @see gov.va.isaac.gui.treegraph.TreeNode#addChildTreeNodeBelow(gov.va.isaac.gui.treegraph.TreeNodeImpl)
	 */
	@Override
	public void addChildTreeNodeBelow(TreeNodeImpl childTreeNode) {
		// position childTreeNode
		double childLayoutX = fxNode.getLayoutX() + fxNode.getMaxWidth() + horizontalSpaceBetweenParentAndChildNodes; // just an arbitrary indent
		double childLayoutY = fxNode.getLayoutY() + getHeight() + vertSpaceBetweenChildNodes;
		childTreeNode.getFxNode().setLayoutX(childLayoutX);
		childTreeNode.getFxNode().setLayoutY(childLayoutY);
		
		// create connector
		Coordinates startCoordinates = this.getBottomConnectionPortCoordinates();
		Coordinates endCoordinates = childTreeNode.getLeftConnectionPortCoordinates();
		// create vertical line segment
		Line verticalLine = new Line(startCoordinates.getX(), startCoordinates.getY(), startCoordinates.getX(), endCoordinates.getY());
		// create horizontal line segment
		Line horizontalLine = new Line(startCoordinates.getX(), endCoordinates.getY(), endCoordinates.getX(), endCoordinates.getY());
		// add connector vertical and horizontal line segments to new FX Group
		Group compositeLine = new Group(verticalLine, horizontalLine);
		// add new connector FX Group to childTreeNode FX Group
		childTreeNode.getChildren().addAll(compositeLine);
		
		this.getChildren().add(childTreeNode);
		childTreeNodes.add(childTreeNode);
	}

	@Override
	public Group getGroup() {
		return this;
	}

	private Coordinates getRightConnectionPortCoordinates() {
		return new Coordinates(fxNode.getLayoutX() + fxNode.getMaxWidth(), fxNode.getLayoutY() + fxNode.getMaxHeight() / 2);
	}
	private Coordinates getLeftConnectionPortCoordinates() {
		return new Coordinates(fxNode.getLayoutX(), fxNode.getLayoutY() + fxNode.getMaxHeight() / 2);
	}
	private Coordinates getBottomConnectionPortCoordinates() {
		return new Coordinates(fxNode.getLayoutX() + fxNode.getMaxWidth() / 2, fxNode.getLayoutY() + fxNode.getMaxHeight());
	}
}
