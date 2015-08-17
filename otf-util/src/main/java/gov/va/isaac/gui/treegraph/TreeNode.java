package gov.va.isaac.gui.treegraph;

import javafx.beans.property.ReadOnlyListProperty;
import javafx.scene.layout.Region;

import javafx.scene.Group;

public interface TreeNode<T extends TreeNode<T>> {

	public abstract T getRoot();

	public abstract Region getFxNode();

	public abstract T getChildToRight();

	public abstract void setChildToRight(T treeNode);

	public abstract double getHeight();

	public abstract double getWidth();

	public abstract T getParentTreeNode();

	public abstract ReadOnlyListProperty<T> getChildTreeNodesBelow();

	public abstract void addChildTreeNodeBelow(T childTreeNode);
	
	public Group getGroup();
}