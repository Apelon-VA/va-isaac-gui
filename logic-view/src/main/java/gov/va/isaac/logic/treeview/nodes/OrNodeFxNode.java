package gov.va.isaac.logic.treeview.nodes;

import gov.va.isaac.gui.treegraph.TreeNodeUtils;
import gov.va.isaac.logic.treeview.LogicalExpressionTreeGraph;
import gov.vha.isaac.ochre.model.logic.node.OrNode;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.shape.Circle;

public class OrNodeFxNode extends Label {
	public OrNodeFxNode(OrNode logicalNode) {
		super(logicalNode.getNodeSemantic().name());
		setShape(new Circle(25));
		TreeNodeUtils.setFxNodeSizes(this, 50, 50);
	}
}