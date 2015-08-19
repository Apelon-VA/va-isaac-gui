package gov.va.isaac.logic.treeview.nodes;

import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.impl.utility.Frills;
import gov.vha.isaac.ochre.model.logic.node.AbstractNode;
import gov.vha.isaac.ochre.model.logic.node.external.FeatureNodeWithUuids;
import gov.vha.isaac.ochre.model.logic.node.internal.FeatureNodeWithSequences;
import java.util.Optional;
import javafx.scene.control.Label;

// FeatureNodeWithSequences
// FeatureNodeWithUuids
public class FeatureNodeFxNode extends Label {
	public FeatureNodeFxNode(FeatureNodeWithSequences logicalNode) {
		this(logicalNode, Get.conceptDescriptionText(((FeatureNodeWithSequences)logicalNode).getTypeConceptSequence()), 
				Frills.getSctId(Get.identifierService().getConceptNid(((FeatureNodeWithSequences)logicalNode).getTypeConceptSequence()), null));
	}
	public FeatureNodeFxNode(FeatureNodeWithUuids logicalNode) {
		this(logicalNode, Get.conceptDescriptionText(Get.identifierService().getConceptSequenceForUuids(((FeatureNodeWithUuids)logicalNode).getTypeConceptUuid())), 
				Frills.getSctId(Get.identifierService().getNidForUuids((((FeatureNodeWithUuids)logicalNode).getTypeConceptUuid())), null));
	}
	private FeatureNodeFxNode(AbstractNode logicalNode, String desc, Optional<Long> sctId) {
		super(logicalNode.getNodeSemantic().name() /* + "\n" + LogicalExpressionTreeGraph.logicalNodeTypeToString(logicalNode) */ + "\ntype=" + desc + "\noperator=" + ((FeatureNodeWithSequences)logicalNode).getOperator().name() + (sctId.isPresent() ? "\n" + sctId.get() : ""));
	}
}
