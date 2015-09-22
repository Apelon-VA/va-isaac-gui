package gov.va.isaac.logic.treeview.nodes;

import gov.va.isaac.gui.treegraph.TreeNodeUtils;
import gov.va.isaac.logic.treeview.LogicalExpressionTreeGraph;
import gov.vha.isaac.ochre.model.logic.node.SufficientSetNode;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.shape.Circle;

public class SufficientSetNodeFxNode extends Label {
	public SufficientSetNodeFxNode(SufficientSetNode logicalNode) {
		super("SUFFICIENT" /* + "\n" + LogicalExpressionTreeGraph.logicalNodeTypeToString(logicalNode) */);

		setShape(new Circle(38));
		TreeNodeUtils.configureFxNode(this, 94, 38);
	}
}
