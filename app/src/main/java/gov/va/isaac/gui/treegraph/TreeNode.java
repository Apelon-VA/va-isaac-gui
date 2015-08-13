package gov.va.isaac.gui.treegraph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javafx.scene.Group;
import javafx.scene.shape.Shape;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.Node;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.ReadOnlyListWrapper;
import javafx.collections.ObservableList;

public class TreeNode extends Group {
	static class Coordinates {
		final double x;
		final double y;
		
		public Coordinates(double x, double y) { this.x = x; this.y = y; }
		
		public double getX() {
			return x;
		}
		public double getY() {
			return y;
		}
		
		public String toString() { return "(" + x + ", " + y + ")"; }
	}

	private final static double preferredWidth = 50;
	private final static double preferredHeight = 25;
	
	private final static double vertSpaceBetweenChildNodes = 10;
	private final static double horizontalSpaceBetweenParentAndChildNodes = 20;
	
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

		fxNode.setStyle("-fx-border-color: black;");
		
		this.parentTreeNode = parentTreeNode;
		this.fxNode = fxNode;
	}
	
	Coordinates getRightConnectionPortCoordinates() {
		return new Coordinates(fxNode.getLayoutX() + preferredWidth, fxNode.getLayoutY() + preferredHeight / 2);
	}
	Coordinates getLeftConnectionPortCoordinates() {
		return new Coordinates(fxNode.getLayoutX(), fxNode.getLayoutY() + preferredHeight / 2);
	}
	Coordinates getBottomConnectionPortCoordinates() {
		return new Coordinates(fxNode.getLayoutX() + preferredWidth / 2, fxNode.getLayoutY() + preferredHeight);
	}
	
	public Node getFxNode() {
		return fxNode;
	}
	
	public TreeNode getChildToRight() {
		return childToRight;
	}
	public void setChildToRight(TreeNode treeNode) {
		childToRight = treeNode;
		
		childToRight.fxNode.setLayoutX(fxNode.getLayoutX() + preferredWidth + horizontalSpaceBetweenParentAndChildNodes);
		childToRight.fxNode.setLayoutY(fxNode.getLayoutY());

		this.getChildren().add(childToRight);
		
		Coordinates startCoordinates = this.getRightConnectionPortCoordinates();
		Coordinates endCoordinates = childToRight.getLeftConnectionPortCoordinates();
		Line line = new Line(fxNode.getLayoutX() + preferredWidth, fxNode.getLayoutY() + preferredHeight / 2, childToRight.fxNode.getLayoutX(), childToRight.fxNode.getLayoutY() + preferredHeight / 2);
		getChildren().add(line);
	}
	
	public double getHeight() {
		double height = preferredHeight;
		
		for (TreeNode treeNode : childTreeNodes) {
			height = height + vertSpaceBetweenChildNodes + treeNode.getHeight();
		}
		return height;
	}

	public double getWidth() {
		return childToRight == null ? preferredHeight : preferredHeight + horizontalSpaceBetweenParentAndChildNodes + childToRight.getWidth();
	}
	
	public TreeNode getParentTreeNode() { return parentTreeNode; }
	
	public List<TreeNode> getChildTreeNodesBelow() {
		return childTreeNodes;
	}

	public void addChildTreeNodeBelow(TreeNode childTreeNode) {
		double childLayoutX = fxNode.getLayoutX() + preferredWidth + horizontalSpaceBetweenParentAndChildNodes; // just an arbitrary indent
		double childLayoutY = fxNode.getLayoutY() + getHeight() + vertSpaceBetweenChildNodes;
		
		childTreeNode.getFxNode().setLayoutX(childLayoutX);
		childTreeNode.getFxNode().setLayoutY(childLayoutY);
		
		// TODO create connector here
		Coordinates startCoordinates = this.getBottomConnectionPortCoordinates();
		Coordinates endCoordinates = childTreeNode.getLeftConnectionPortCoordinates();
		Line verticalLine = new Line(startCoordinates.getX(), startCoordinates.getY(), startCoordinates.getX(), endCoordinates.getY());
		Line horizontalLine = new Line(startCoordinates.getX(), endCoordinates.getY(), endCoordinates.getX(), endCoordinates.getY());
		getChildren().addAll(verticalLine, horizontalLine);

		
		this.getChildren().add(childTreeNode);
		childTreeNodes.add(childTreeNode);
	}
}
