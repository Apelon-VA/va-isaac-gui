package gov.va.isaac.logic.treeview.nodes;

import java.util.Optional;

import gov.va.isaac.logic.treeview.LogicalExpressionTreeGraph;
import gov.va.isaac.util.OchreUtility;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.model.logic.node.AbstractNode;
import gov.vha.isaac.ochre.model.logic.node.external.ConceptNodeWithUuids;
import gov.vha.isaac.ochre.model.logic.node.external.FeatureNodeWithUuids;
import gov.vha.isaac.ochre.model.logic.node.internal.ConceptNodeWithSequences;
import gov.vha.isaac.ochre.model.logic.node.internal.FeatureNodeWithSequences;
import javafx.scene.Node;
import javafx.scene.control.Label;

// FeatureNodeWithSequences
// FeatureNodeWithUuids
public class FeatureNodeFxNode extends Label {
	public FeatureNodeFxNode(FeatureNodeWithSequences logicalNode) {
		this(logicalNode, Get.conceptDescriptionText(((FeatureNodeWithSequences)logicalNode).getTypeConceptSequence()), OchreUtility.getSctId(((FeatureNodeWithSequences)logicalNode).getTypeConceptSequence()));
	}
	public FeatureNodeFxNode(FeatureNodeWithUuids logicalNode) {
		this(logicalNode, Get.conceptDescriptionText(Get.identifierService().getConceptSequenceForUuids(((FeatureNodeWithUuids)logicalNode).getTypeConceptUuid())), OchreUtility.getSctId(Get.identifierService().getConceptSequenceForUuids(((FeatureNodeWithUuids)logicalNode).getTypeConceptUuid())));
	}
	private FeatureNodeFxNode(AbstractNode logicalNode, String desc, Optional<Long> sctId) {
		super(logicalNode.getNodeSemantic().name() /* + "\n" + LogicalExpressionTreeGraph.logicalNodeTypeToString(logicalNode) */ + "\ntype=" + desc + "\noperator=" + ((FeatureNodeWithSequences)logicalNode).getOperator().name() + (sctId.isPresent() ? "\n" + sctId.get() : ""));
	}
}
