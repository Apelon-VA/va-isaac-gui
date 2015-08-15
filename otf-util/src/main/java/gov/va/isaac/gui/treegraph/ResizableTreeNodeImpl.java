package gov.va.isaac.gui.treegraph;

import gov.va.isaac.util.UpdateableDoubleBinding;

import java.util.ArrayList;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ReadOnlyListProperty;
import javafx.beans.property.ReadOnlyListWrapper;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.geometry.Pos;
import javafx.scene.control.Labeled;
import javafx.scene.Group;
import javafx.scene.layout.Region;
import javafx.scene.shape.Line;

import com.sun.javafx.collections.ObservableListWrapper;

public class ResizableTreeNodeImpl extends Group implements TreeNode<ResizableTreeNodeImpl> {
	private static class Coordinates {
		final double x;
		final double y;
		
		public Coordinates(double x, double y) { this.x = x; this.y = y; }
		
		public double getX() { return x; }
		public double getY() { return y; }
		
		public String toString() { return "(" + x + ", " + y + ")"; }
	}
	abstract class RecomputingBinding extends UpdateableDoubleBinding {
		{
			super.setComputeOnInvalidate(true);
		}
		
	};
	
	private final static double preferredWidth = 200;
	private final static double preferredHeight = 100;
	
	private final static double vertSpaceBetweenChildNodes = 5;
	private final static double horizontalSpaceBetweenParentAndChildNodes = 20;
	
	private final ResizableTreeNodeImpl parentTreeNode;

	private final ObjectProperty<ResizableTreeNodeImpl> childToRightProperty = new SimpleObjectProperty<>();
	
	private final Region fxNode;
	
	private final DoubleProperty widthProperty = new SimpleDoubleProperty();
	private final UpdateableDoubleBinding widthBinding;
	
	private final DoubleProperty heightProperty = new SimpleDoubleProperty();
	private final UpdateableDoubleBinding heightBinding;
	
	private final ListProperty<ResizableTreeNodeImpl> childTreeNodes = new SimpleListProperty<>(new ObservableListWrapper<>(new ArrayList<>()));
	private final ReadOnlyListWrapper<ResizableTreeNodeImpl> readOnlyChildTreeNodes = new ReadOnlyListWrapper<>(childTreeNodes);

	public ResizableTreeNodeImpl(ResizableTreeNodeImpl parentTreeNode, Region fxNode) {
		super(fxNode);
		
		fxNode.setStyle("-fx-border-color: black;");
		
//		fxNode.setMaxHeight(preferredHeight);
//		fxNode.setMaxWidth(preferredWidth);
//		fxNode.setPrefHeight(preferredHeight);
//		fxNode.setPrefWidth(preferredWidth);
//		fxNode.setMinHeight(preferredHeight);
//		fxNode.setMinWidth(preferredWidth);
		
		if (fxNode instanceof Labeled) {
			((Labeled)fxNode).setAlignment(Pos.CENTER);
		}
		
		this.parentTreeNode = parentTreeNode;
		this.fxNode = fxNode;

		widthBinding = new RecomputingBinding() {
			@Override
			protected double computeValue() {
				return childToRightProperty.get() == null ? fxNode.getWidth() : fxNode.getWidth() + horizontalSpaceBetweenParentAndChildNodes + childToRightProperty.get().getWidth();
			}
		};
		widthBinding.addBinding(fxNode.widthProperty(), childToRightProperty);
		childToRightProperty.addListener(new ChangeListener<ResizableTreeNodeImpl>() {
			@Override
			public void changed(
					ObservableValue<? extends ResizableTreeNodeImpl> observable,
					ResizableTreeNodeImpl oldValue,
					ResizableTreeNodeImpl newValue) {
				if (oldValue == null && newValue != null) {
					widthBinding.addBinding(newValue.widthProperty);
				} else if (oldValue != null && newValue == null) {
					widthBinding.removeBinding(oldValue.widthProperty);
				} else {
					widthBinding.removeBinding(oldValue.widthProperty);
					widthBinding.addBinding(newValue.widthProperty);
				}
			}
		});
		widthProperty.bind(widthBinding);

		heightBinding = new RecomputingBinding() {
			@Override
			protected double computeValue() {
				double heightOfFxNode = fxNode.heightProperty().get();
				
				double heightWithChildrenBelow = heightOfFxNode;
				for (ResizableTreeNodeImpl treeNode : childTreeNodes) {
					heightWithChildrenBelow += vertSpaceBetweenChildNodes + treeNode.getHeight();
				}
				
				double heightOfChildToRight = childToRightProperty.get() != null ? childToRightProperty.get().getHeight() : 0;
				
				return Math.max(heightOfChildToRight, Math.max(heightOfFxNode, heightWithChildrenBelow));
			}
		};
		heightBinding.addBinding(fxNode.heightProperty(), childTreeNodes, childToRightProperty);
		childTreeNodes.addListener(new ListChangeListener<ResizableTreeNodeImpl>() {
			@Override
			public void onChanged(
					javafx.collections.ListChangeListener.Change<? extends ResizableTreeNodeImpl> c) {
				while (c.next()) {
					System.err.println("Change");
					if (c.wasRemoved()) {
						for (ResizableTreeNodeImpl node : c.getRemoved()) {
							heightBinding.removeBinding(node.heightProperty);
						}
					}
					if (c.wasAdded()) {
						for (ResizableTreeNodeImpl node : c.getAddedSubList()) {
							heightBinding.addBinding(node.heightProperty);
						}
					}
				}
			}
		});
		childToRightProperty.addListener(new ChangeListener<ResizableTreeNodeImpl>() {
			@Override
			public void changed(
					ObservableValue<? extends ResizableTreeNodeImpl> observable,
					ResizableTreeNodeImpl oldValue,
					ResizableTreeNodeImpl newValue) {
				if (oldValue == null && newValue != null) {
					heightBinding.addBinding(newValue.heightProperty);
				} else if (oldValue != null && newValue == null) {
					heightBinding.removeBinding(oldValue.heightProperty);
				} else {
					heightBinding.removeBinding(oldValue.heightProperty);
					heightBinding.addBinding(newValue.heightProperty);
				}
			}
		});
		heightProperty.bind(heightBinding);
	}
	
	public ResizableTreeNodeImpl getRoot() {
		return this.parentTreeNode == null ? this : this.parentTreeNode.getRoot();
	}
	
	public Region getFxNode() {
		return fxNode;
	}
	
	public ResizableTreeNodeImpl getChildToRight() {
		return childToRightProperty.get();
	}
	public void setChildToRight(ResizableTreeNodeImpl treeNode) {
		// Position childToRight
		treeNode.getFxNode().layoutXProperty().bind(Bindings.add(fxNode.layoutXProperty(), Bindings.add(fxNode.widthProperty(), horizontalSpaceBetweenParentAndChildNodes)));
		treeNode.getFxNode().layoutYProperty().bind(fxNode.layoutYProperty());
		childToRightProperty.set(treeNode);
		
		// add childToRight to FX Group
		this.getChildren().add(childToRightProperty.get());
		
		// Add connector
		Coordinates startCoordinates = this.getRightConnectionPortCoordinates();
		Coordinates endCoordinates = childToRightProperty.get().getLeftConnectionPortCoordinates();
		Line line = new Line(startCoordinates.getX(), startCoordinates.getY(), endCoordinates.getX(), endCoordinates.getY());
		childToRightProperty.get().getChildren().add(line); // add connector line to FX Group
	}
	
	public DoubleProperty heightProperty() {
		return heightProperty;
	}
	public double getHeight() {
		return heightProperty.get();
	}

	public DoubleProperty widthProperty() {
		return widthProperty;
	}
	public double getWidth() {
		return widthProperty.get();
	}
	
	public ResizableTreeNodeImpl getParentTreeNode() { return parentTreeNode; }
	
	public ReadOnlyListProperty<ResizableTreeNodeImpl> getChildTreeNodesBelow() {
		return readOnlyChildTreeNodes;
	}

	public void addChildTreeNodeBelow(ResizableTreeNodeImpl childTreeNode) {
		// position childTreeNode
		//double childLayoutY = fxNode.getLayoutY() + getHeight() + vertSpaceBetweenChildNodes;
		childTreeNode.getFxNode().layoutXProperty().bind(Bindings.add(fxNode.layoutXProperty(), Bindings.add(horizontalSpaceBetweenParentAndChildNodes, fxNode.widthProperty())));
		childTreeNode.getFxNode().layoutYProperty().bind(Bindings.add(vertSpaceBetweenChildNodes, Bindings.add(fxNode.layoutYProperty(), heightProperty)));
		
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

	@Override
	public Group getGroup() {
		return this;
	}
}
