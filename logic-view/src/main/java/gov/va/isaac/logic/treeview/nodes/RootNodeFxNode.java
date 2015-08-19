package gov.va.isaac.logic.treeview.nodes;

import gov.va.isaac.logic.treeview.LogicalExpressionTreeGraph;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.logic.LogicalExpression;
import gov.vha.isaac.ochre.model.logic.node.RootNode;
import javafx.scene.Node;
import javafx.scene.control.Label;

public class RootNodeFxNode extends Label {
	public RootNodeFxNode(LogicalExpression logicalExpression, RootNode logicalNode) {
		super(logicalNode.getNodeSemantic().name() + /* "\n" + LogicalExpressionTreeGraph.logicalNodeTypeToString(logicalNode) + */ "\n" + Get.conceptDescriptionText(logicalExpression.getConceptSequence()));
	}
}
