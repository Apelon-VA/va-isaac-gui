package gov.va.isaac.logic.treeview.nodes;

import gov.va.isaac.gui.treegraph.TreeNodeUtils;
import gov.va.isaac.logic.treeview.LogicalExpressionTreeGraph;
import gov.vha.isaac.ochre.model.logic.node.NecessarySetNode;
import gov.vha.isaac.ochre.model.logic.node.SufficientSetNode;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.shape.Circle;

public class NecessarySetNodeFxNode extends Label {
	public NecessarySetNodeFxNode(NecessarySetNode logicalNode) {
		super(logicalNode.getNodeSemantic().name() + "\n" + LogicalExpressionTreeGraph.logicalNodeTypeToString(logicalNode));

		setShape(new Circle(50));
		TreeNodeUtils.setFxNodeSizes(this, 100, 100);
	}
}
