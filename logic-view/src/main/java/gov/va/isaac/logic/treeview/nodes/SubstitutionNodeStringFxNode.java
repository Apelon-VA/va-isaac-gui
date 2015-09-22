package gov.va.isaac.logic.treeview.nodes;

import gov.va.isaac.logic.treeview.LogicalExpressionTreeGraph;
import gov.vha.isaac.ochre.model.logic.node.SubstitutionNodeInteger;
import gov.vha.isaac.ochre.model.logic.node.SubstitutionNodeString;
import javafx.scene.Node;
import javafx.scene.control.Label;

// ConceptNodeWithSequences
// ConceptNodeWithUuids
public class SubstitutionNodeStringFxNode extends Label {
	public SubstitutionNodeStringFxNode(SubstitutionNodeString logicalNode) {
		super(LogicalExpressionTreeGraph.logicalNodeTypeToString(logicalNode));
	}
}
