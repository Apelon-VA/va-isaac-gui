package gov.va.isaac.logic.treeview.nodes;

import gov.va.isaac.logic.treeview.LogicalExpressionTreeGraph;
import gov.vha.isaac.ochre.model.logic.node.SubstitutionNodeBoolean;
import gov.vha.isaac.ochre.model.logic.node.SubstitutionNodeConcept;
import javafx.scene.Node;
import javafx.scene.control.Label;

public class SubstitutionNodeConceptFxNode extends Label {
	public SubstitutionNodeConceptFxNode(SubstitutionNodeConcept logicalNode) {
		super(LogicalExpressionTreeGraph.logicalNodeTypeToString(logicalNode));
	}
}
