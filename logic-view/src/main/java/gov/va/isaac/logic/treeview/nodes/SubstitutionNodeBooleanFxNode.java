package gov.va.isaac.logic.treeview.nodes;

import gov.va.isaac.logic.treeview.LogicalExpressionTreeGraph;
import gov.vha.isaac.ochre.model.logic.node.SubstitutionNodeBoolean;
import gov.vha.isaac.ochre.model.logic.node.SubstitutionNodeFloat;
import javafx.scene.Node;
import javafx.scene.control.Label;

// ConceptNodeWithSequences
// ConceptNodeWithUuids
public class SubstitutionNodeBooleanFxNode extends Label {
	public SubstitutionNodeBooleanFxNode(SubstitutionNodeBoolean logicalNode) {
		super(logicalNode.getNodeSemantic().name() + "\n" + LogicalExpressionTreeGraph.logicalNodeTypeToString(logicalNode));
	}
}
