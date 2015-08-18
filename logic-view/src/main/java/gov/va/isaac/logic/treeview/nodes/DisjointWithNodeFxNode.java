package gov.va.isaac.logic.treeview.nodes;

import gov.va.isaac.logic.treeview.LogicalExpressionTreeGraph;
import gov.vha.isaac.ochre.model.logic.node.DisjointWithNode;
import gov.vha.isaac.ochre.model.logic.node.OrNode;
import javafx.scene.Node;
import javafx.scene.control.Label;

public class DisjointWithNodeFxNode extends Label {
	public DisjointWithNodeFxNode(DisjointWithNode logicalNode) {
		super(logicalNode.getNodeSemantic().name() /* + "\n" + LogicalExpressionTreeGraph.logicalNodeTypeToString(logicalNode) */);
	}
}
