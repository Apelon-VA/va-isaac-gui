package gov.va.isaac.logic.treeview.nodes;

import gov.va.isaac.logic.treeview.LogicalExpressionTreeGraph;
import gov.vha.isaac.ochre.model.logic.node.SubstitutionNodeConcept;
import gov.vha.isaac.ochre.model.logic.node.SubstitutionNodeInstant;
import javafx.scene.Node;
import javafx.scene.control.Label;

// ConceptNodeWithSequences
// ConceptNodeWithUuids
public class SubstitutionNodeInstantFxNode extends Label {
	public SubstitutionNodeInstantFxNode(SubstitutionNodeInstant logicalNode) {
		super(LogicalExpressionTreeGraph.logicalNodeTypeToString(logicalNode));
	}
}
