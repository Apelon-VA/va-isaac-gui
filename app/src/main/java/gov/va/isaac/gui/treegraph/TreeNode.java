package gov.va.isaac.gui.treegraph;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.Group;
import javafx.scene.layout.Region;
import javafx.scene.shape.Line;

public class TreeNode extends Group {
	static class Coordinates {
		final double x;
		final double y;
		
		public Coordinates(double x, double y) { this.x = x; this.y = y; }
		
		public double getX() { return x; }
		public double getY() { return y; }
		
		public String toString() { return "(" + x + ", " + y + ")"; }
	}

	private final static double preferredWidth = 100;
	private final static double preferredHeight = 25;
	
	private final static double vertSpaceBetweenChildNodes = 10;
	private final static double horizontalSpaceBetweenParentAndChildNodes = 20;
	
	final TreeNode parentTreeNode;
	TreeNode childToRight;
	
	final Region fxNode;
	final List<TreeNode> childTreeNodes = new ArrayList<TreeNode>();
	
	public TreeNode(TreeNode parentTreeNode, Region fxNode) {
		super(fxNode);
		
		fxNode.setStyle("-fx-border-color: black;");
		
		fxNode.setMaxHeight(preferredHeight);
		fxNode.setMaxWidth(preferredWidth);
		fxNode.setPrefHeight(preferredHeight);
		fxNode.setPrefWidth(preferredWidth);
		fxNode.setMinHeight(preferredHeight);
		fxNode.setMinWidth(preferredWidth);
		
		this.parentTreeNode = parentTreeNode;
		this.fxNode = fxNode;
	}
	
	Coordinates getRightConnectionPortCoordinates() {
		return new Coordinates(fxNode.getLayoutX() + fxNode.getMaxWidth(), fxNode.getLayoutY() + fxNode.getMaxHeight() / 2);
	}
	Coordinates getLeftConnectionPortCoordinates() {
		return new Coordinates(fxNode.getLayoutX(), fxNode.getLayoutY() + fxNode.getMaxHeight() / 2);
	}
	Coordinates getBottomConnectionPortCoordinates() {
		return new Coordinates(fxNode.getLayoutX() + fxNode.getMaxWidth() / 2, fxNode.getLayoutY() + fxNode.getMaxHeight());
	}
	
	public Region getFxNode() {
		return fxNode;
	}
	
	public TreeNode getChildToRight() {
		return childToRight;
	}
	public void setChildToRight(TreeNode treeNode) {
		childToRight = treeNode;
		
		childToRight.fxNode.setLayoutX(fxNode.getLayoutX() + fxNode.getMaxWidth() + horizontalSpaceBetweenParentAndChildNodes);
		childToRight.fxNode.setLayoutY(fxNode.getLayoutY());

		this.getChildren().add(childToRight);
		
		Coordinates startCoordinates = this.getRightConnectionPortCoordinates();
		Coordinates endCoordinates = childToRight.getLeftConnectionPortCoordinates();
		Line line = new Line(fxNode.getLayoutX() + fxNode.getMaxWidth(), fxNode.getLayoutY() + fxNode.getMaxHeight() / 2, childToRight.fxNode.getLayoutX(), childToRight.fxNode.getLayoutY() + preferredHeight / 2);
		childToRight.getChildren().add(line);
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
	
	public List<TreeNode> getChildTreeNodesBelow() {
		return childTreeNodes;
	}

	public void addChildTreeNodeBelow(TreeNode childTreeNode) {
		double childLayoutX = fxNode.getLayoutX() + fxNode.getMaxWidth() + horizontalSpaceBetweenParentAndChildNodes; // just an arbitrary indent
		double childLayoutY = fxNode.getLayoutY() + getHeight() + vertSpaceBetweenChildNodes;
		
		childTreeNode.getFxNode().setLayoutX(childLayoutX);
		childTreeNode.getFxNode().setLayoutY(childLayoutY);
		
		// TODO create connector here
		Coordinates startCoordinates = this.getBottomConnectionPortCoordinates();
		Coordinates endCoordinates = childTreeNode.getLeftConnectionPortCoordinates();
		Line verticalLine = new Line(startCoordinates.getX(), startCoordinates.getY(), startCoordinates.getX(), endCoordinates.getY());
		Line horizontalLine = new Line(startCoordinates.getX(), endCoordinates.getY(), endCoordinates.getX(), endCoordinates.getY());
		childTreeNode.getChildren().addAll(verticalLine, horizontalLine);
		
		this.getChildren().add(childTreeNode);
		childTreeNodes.add(childTreeNode);
	}
}
