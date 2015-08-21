package gov.va.isaac.logic.treeview.nodes;

import gov.va.isaac.logic.treeview.LogicalExpressionTreeGraph;
import gov.vha.isaac.ochre.model.logic.node.SubstitutionNodeInstant;
import gov.vha.isaac.ochre.model.logic.node.SubstitutionNodeInteger;
import javafx.scene.Node;
import javafx.scene.control.Label;

// ConceptNodeWithSequences
// ConceptNodeWithUuids
public class SubstitutionNodeIntegerFxNode extends Label {
	public SubstitutionNodeIntegerFxNode(SubstitutionNodeInteger logicalNode) {
		super(logicalNode.getNodeSemantic().name() + "\n" + LogicalExpressionTreeGraph.logicalNodeTypeToString(logicalNode));
	}
}
