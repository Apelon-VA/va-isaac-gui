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

public class TreeNode extends Group {
	private static class Coordinates {
		final double x;
		final double y;
		
		public Coordinates(double x, double y) { this.x = x; this.y = y; }
		
		public double getX() { return x; }
		public double getY() { return y; }
		
		public String toString() { return "(" + x + ", " + y + ")"; }
	}

	private final static double preferredWidth = 100;
	private final static double preferredHeight = 50;
	
	private final static double vertSpaceBetweenChildNodes = 10;
	private final static double horizontalSpaceBetweenParentAndChildNodes = 20;
	
	private final TreeNode parentTreeNode;
	private TreeNode childToRight;
	
	private final Region fxNode;

	private final ListProperty<TreeNode> childTreeNodes = new SimpleListProperty<>(new ObservableListWrapper<>(new ArrayList<>()));
	private final ReadOnlyListWrapper<TreeNode> readOnlyChildTreeNodes = new ReadOnlyListWrapper<>(childTreeNodes);

	public TreeNode(TreeNode parentTreeNode, Region fxNode) {
		super(fxNode);
		
		fxNode.setStyle("-fx-border-color: black;");
		
		fxNode.setMaxHeight(preferredHeight);
		fxNode.setMaxWidth(preferredWidth);
		fxNode.setPrefHeight(preferredHeight);
		fxNode.setPrefWidth(preferredWidth);
		fxNode.setMinHeight(preferredHeight);
		fxNode.setMinWidth(preferredWidth);
		
		if (fxNode instanceof Labeled) {
			((Labeled)fxNode).setAlignment(Pos.CENTER);
		}
		
		this.parentTreeNode = parentTreeNode;
		this.fxNode = fxNode;
	}
	
	public Region getFxNode() {
		return fxNode;
	}
	
	public TreeNode getChildToRight() {
		return childToRight;
	}
	public void setChildToRight(TreeNode treeNode) {
		childToRight = treeNode;
		
		// Position childToRight
		childToRight.fxNode.setLayoutX(fxNode.getLayoutX() + fxNode.getMaxWidth() + horizontalSpaceBetweenParentAndChildNodes);
		childToRight.fxNode.setLayoutY(fxNode.getLayoutY());
		
		// add childToRight to FX Group
		this.getChildren().add(childToRight);
		
		// Add connector
		Coordinates startCoordinates = this.getRightConnectionPortCoordinates();
		Coordinates endCoordinates = childToRight.getLeftConnectionPortCoordinates();
		Line line = new Line(startCoordinates.getX(), startCoordinates.getY(), endCoordinates.getX(), endCoordinates.getY());
		childToRight.getChildren().add(line); // add connector line to FX Group
	}
	
	public double getHeight() {
		double height = fxNode.getMaxHeight();
		
		for (TreeNode treeNode : childTreeNodes) {
			height = height + vertSpaceBetweenChildNodes + treeNode.getHeight();
		}
		return height;
	}

	public double getWidth() {
		return childToRight == null ? fxNode.getMaxWidth() : fxNode.getMaxWidth() + horizontalSpaceBetweenParentAndChildNodes + childToRight.getWidth();
	}
	
	public TreeNode getParentTreeNode() { return parentTreeNode; }
	
	public ReadOnlyListProperty<TreeNode> getChildTreeNodesBelow() {
		return readOnlyChildTreeNodes;
	}

	public void addChildTreeNodeBelow(TreeNode childTreeNode) {
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
