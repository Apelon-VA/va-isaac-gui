package gov.va.isaac.logic.treeview.nodes;

import gov.va.isaac.logic.treeview.LogicalExpressionTreeGraph;
import gov.vha.isaac.ochre.model.logic.node.AndNode;
import gov.vha.isaac.ochre.model.logic.node.SubstitutionNodeFloat;
import javafx.scene.Node;
import javafx.scene.control.Label;

public class SubstitutionNodeFloatFxNode extends Label {
	public SubstitutionNodeFloatFxNode(SubstitutionNodeFloat logicalNode) {
		super(logicalNode.getNodeSemantic().name() + "\n" + LogicalExpressionTreeGraph.logicalNodeTypeToString(logicalNode));
	}
}
