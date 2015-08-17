package gov.va.isaac.gui.treegraph;

import gov.va.isaac.util.UpdateableDoubleBinding;

import java.util.ArrayList;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyListProperty;
import javafx.beans.property.ReadOnlyListWrapper;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Labeled;
import javafx.scene.layout.Region;
import javafx.scene.shape.Line;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.javafx.collections.ObservableListWrapper;

public class ResizableTreeNodeImpl extends Group implements TreeNode<ResizableTreeNodeImpl> {
	//private static final Logger LOG = LoggerFactory.getLogger(ResizableTreeNodeImpl.class);
	
	abstract class RecomputingDoubleBinding extends UpdateableDoubleBinding {
		{
			super.setComputeOnInvalidate(true);
		}
	};

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

	// Left connection port
	private final DoubleBinding leftConnectionPortXBinding;
	private final DoubleBinding leftConnectionPortYBinding;

	// Right connection port
	private final DoubleBinding rightConnectionPortXBinding;
	private final DoubleBinding rightConnectionPortYBinding;

	// bottom connection port
	private final DoubleBinding bottomConnectionPortXBinding;
	private final DoubleBinding bottomConnectionPortYBinding;

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

		widthBinding = new RecomputingDoubleBinding() {
			@Override
			protected double computeValue() {
				double value = childToRightProperty.get() == null ? getWidthPropertyFromFxNode(fxNode).get() : getWidthPropertyFromFxNode(fxNode).get() + horizontalSpaceBetweenParentAndChildNodes + childToRightProperty.get().getWidth();

				//String text = (fxNode instanceof Labeled) ? ((Labeled)fxNode).getText() : "node";
				//System.out.println("Recomputing " + text + " width to " + value);
				return value;
			}
		};
		widthBinding.addBinding(getWidthPropertyFromFxNode(fxNode), childToRightProperty);
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

		heightBinding = new RecomputingDoubleBinding() {
			@Override
			protected double computeValue() {
				double heightOfFxNode = getHeightPropertyFromFxNode(fxNode).get();
				
				double heightWithChildrenBelow = heightOfFxNode;
				for (ResizableTreeNodeImpl childTreeNode : childTreeNodes) {
					heightWithChildrenBelow += vertSpaceBetweenChildNodes + childTreeNode.getHeight();
				}
				
				double heightOfChildToRight = childToRightProperty.get() != null ? childToRightProperty.get().getHeight() : 0;
				
				double value = Math.max(heightOfChildToRight, Math.max(heightOfFxNode, heightWithChildrenBelow));

//				String text = fxNode instanceof Labeled ? ((Labeled)fxNode).getText() : "node";
//				System.out.println("Recomputing " + text + " TreeNode height to " + value);
				
				return value;
			}
		};
		heightBinding.addBinding(getHeightPropertyFromFxNode(fxNode), childTreeNodes, childToRightProperty);
		childTreeNodes.addListener(new ListChangeListener<ResizableTreeNodeImpl>() {
			@Override
			public void onChanged(
					javafx.collections.ListChangeListener.Change<? extends ResizableTreeNodeImpl> c) {
				while (c.next()) {
					if (c.wasRemoved()) {
						for (ResizableTreeNodeImpl node : c.getRemoved()) {
							heightBinding.removeBinding(node.heightProperty);
						}
					}
					if (c.wasAdded()) {
						for (ResizableTreeNodeImpl node : c.getAddedSubList()) {
//							String parentText = fxNode instanceof Labeled ? ((Labeled)fxNode).getText() : "node";
//							String childText = node.fxNode instanceof Labeled ? ((Labeled)node.fxNode).getText() : "node";
//							System.out.println("Adding child (below) binding for " + childText + " of FX Node height " + getHeightPropertyFromFxNode(node.fxNode).get() + " to " + parentText + " of FX Node height " + getHeightPropertyFromFxNode(fxNode).get());
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
		
		rightConnectionPortXBinding = new DoubleBinding() {
			{ bind(fxNode.layoutXProperty(), getWidthPropertyFromFxNode(fxNode)); }
			@Override protected double computeValue() { return fxNode.getLayoutX() + getWidthPropertyFromFxNode(fxNode).get(); }
		};
		rightConnectionPortYBinding = new DoubleBinding() {
			{ bind(fxNode.layoutYProperty(), getHeightPropertyFromFxNode(fxNode)); }
			@Override protected double computeValue() { return fxNode.getLayoutY() + getHeightPropertyFromFxNode(fxNode).get() / 2; }
		};
		
		leftConnectionPortXBinding = new DoubleBinding() {
			{ bind(fxNode.layoutXProperty()); }
			@Override protected double computeValue() { return fxNode.getLayoutX(); }
		};
		leftConnectionPortYBinding = new DoubleBinding() {
			{ bind(fxNode.layoutXProperty(), getHeightPropertyFromFxNode(fxNode)); }
			@Override protected double computeValue() { return fxNode.getLayoutY() + getHeightPropertyFromFxNode(fxNode).get() / 2; }
		};
		
		bottomConnectionPortXBinding = new DoubleBinding() {
			{ bind(fxNode.layoutXProperty(), getWidthPropertyFromFxNode(fxNode)); }
			@Override protected double computeValue() { return fxNode.getLayoutX() + getWidthPropertyFromFxNode(fxNode).get() / 2; }
		};
		bottomConnectionPortYBinding = new DoubleBinding() {
			{ bind(fxNode.layoutYProperty(), getHeightPropertyFromFxNode(fxNode)); }
			@Override protected double computeValue() { return fxNode.getLayoutY() + getHeightPropertyFromFxNode(fxNode).get(); }
		};
		
		// The following listeners are for debug purposes only
//		fxNode.layoutXProperty().addListener(new ChangeListener<Number>() {
//			@Override
//			public void changed(
//					ObservableValue<? extends Number> observable,
//					Number oldValue, Number newValue) {
//				String text = (fxNode instanceof Labeled) ? ((Labeled)fxNode).getText() : "node";
//				System.out.println("Changing " + text + " X coordinate from " + oldValue + " to " + newValue);
//			}
//		});
//		fxNode.layoutYProperty().addListener(new ChangeListener<Number>() {
//			@Override
//			public void changed(
//					ObservableValue<? extends Number> observable,
//					Number oldValue, Number newValue) {
//				String text = (fxNode instanceof Labeled) ? ((Labeled)fxNode).getText() : "node";
//				System.out.println("Changing " + text + " Y coordinate from " + oldValue + " to " + newValue);
//			}
//		});
//		
//		getWidthPropertyFromFxNode(fxNode).addListener(new ChangeListener<Number>() {
//			@Override
//			public void changed(
//					ObservableValue<? extends Number> observable,
//					Number oldValue, Number newValue) {
//				String text = (fxNode instanceof Labeled) ? ((Labeled)fxNode).getText() : "node";
//				System.out.println("Changing " + text + " FX Node width from " + oldValue + " to " + newValue);
//			}
//		});
//		getHeightPropertyFromFxNode(fxNode).addListener(new ChangeListener<Number>() {
//			@Override
//			public void changed(
//					ObservableValue<? extends Number> observable,
//					Number oldValue, Number newValue) {
//				String text = (fxNode instanceof Labeled) ? ((Labeled)fxNode).getText() : "node";
//				System.out.println("Changing " + text + " FX Node height from " + oldValue + " to " + newValue);
//			}
//		});
	}

	// This method is only here to allow replacing widthProperty() with maxWidthProperty() for testing
	protected ReadOnlyDoubleProperty getWidthPropertyFromFxNode(Region fxNode) {
		return fxNode.widthProperty();
	}
	// This method is only here to allow replacing heightProperty() with maxHeightProperty() for testing
	protected ReadOnlyDoubleProperty getHeightPropertyFromFxNode(Region fxNode) {
		return fxNode.heightProperty();
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
		treeNode.getFxNode().layoutXProperty().bind(Bindings.add(fxNode.layoutXProperty(), Bindings.add(getWidthPropertyFromFxNode(fxNode), horizontalSpaceBetweenParentAndChildNodes)));
		treeNode.getFxNode().layoutYProperty().bind(fxNode.layoutYProperty());
		childToRightProperty.set(treeNode);
		
		// add childToRight to FX Group
		this.getChildren().add(childToRightProperty.get());
		
		// Add connector
		// Create connector consisting of three separate Line instances
		//double midpoint = startCoordinates.getX() + (endCoordinates.getX() - startCoordinates.getX()) / 2;
		DoubleBinding midpointXBinding = new DoubleBinding() {
			{ bind(rightConnectionPortXBinding, childToRightProperty.get().leftConnectionPortXBinding); }
			@Override
			protected double computeValue() {
				return rightConnectionPortXBinding.get() + ((childToRightProperty.get().leftConnectionPortXBinding.get() - rightConnectionPortXBinding.get()) / 2);
			}
		};

		Line leftHorizontal = new Line();
		leftHorizontal.startXProperty().bind(rightConnectionPortXBinding);
		leftHorizontal.startYProperty().bind(rightConnectionPortYBinding);
		leftHorizontal.endXProperty().bind(midpointXBinding);
		leftHorizontal.endYProperty().bind(rightConnectionPortYBinding);
		
		Line vertical = new Line();
		vertical.startXProperty().bind(midpointXBinding);
		vertical.startYProperty().bind(rightConnectionPortYBinding);
		vertical.endXProperty().bind(midpointXBinding);
		vertical.endYProperty().bind(childToRightProperty.get().leftConnectionPortYBinding);
		
		Line rightHorizontal = new Line();
		rightHorizontal.startXProperty().bind(midpointXBinding);
		rightHorizontal.startYProperty().bind(childToRightProperty.get().leftConnectionPortYBinding);
		rightHorizontal.endXProperty().bind(childToRightProperty.get().leftConnectionPortXBinding);
		rightHorizontal.endYProperty().bind(childToRightProperty.get().leftConnectionPortYBinding);
		
		Group newCompoundLineGroup = new Group(leftHorizontal, vertical, rightHorizontal);
		childToRightProperty.get().getChildren().add(newCompoundLineGroup);
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
		childTreeNode.getFxNode().layoutXProperty().bind(Bindings.add(fxNode.layoutXProperty(), Bindings.add(horizontalSpaceBetweenParentAndChildNodes, getWidthPropertyFromFxNode(fxNode))));
		childTreeNode.getFxNode().layoutYProperty().bind(Bindings.add(vertSpaceBetweenChildNodes, Bindings.add(fxNode.layoutYProperty(), heightProperty)));
		
		// create connector
		// create vertical line segment
		Line verticalLine = new Line();
		verticalLine.startXProperty().bind(bottomConnectionPortXBinding);
		verticalLine.startYProperty().bind(bottomConnectionPortYBinding);
		verticalLine.endXProperty().bind(bottomConnectionPortXBinding);
		verticalLine.endYProperty().bind(childTreeNode.leftConnectionPortYBinding);
		
		// create horizontal line segment
		Line horizontalLine = new Line();
		horizontalLine.startXProperty().bind(bottomConnectionPortXBinding);
		horizontalLine.startYProperty().bind(childTreeNode.leftConnectionPortYBinding);
		horizontalLine.endXProperty().bind(childTreeNode.leftConnectionPortXBinding);
		horizontalLine.endYProperty().bind(childTreeNode.leftConnectionPortYBinding);
		
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
}
