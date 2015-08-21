package gov.va.isaac.logic.treeview.nodes;

import gov.va.isaac.util.OchreUtility;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.model.logic.node.AbstractNode;
import gov.vha.isaac.ochre.model.logic.node.external.FeatureNodeWithUuids;
import gov.vha.isaac.ochre.model.logic.node.internal.FeatureNodeWithSequences;

import java.util.Optional;
import java.util.function.Function;

import javafx.scene.control.Label;

// FeatureNodeWithSequences
// FeatureNodeWithUuids
public class FeatureNodeFxNode extends Label {
	public FeatureNodeFxNode(FeatureNodeWithSequences logicalNode, Function<Integer, String> descriptionRenderer) {
		this(logicalNode, ((FeatureNodeWithSequences)logicalNode).getTypeConceptSequence(), OchreUtility.getSctId(((FeatureNodeWithSequences)logicalNode).getTypeConceptSequence()), descriptionRenderer);
	}
	public FeatureNodeFxNode(FeatureNodeWithUuids logicalNode, Function<Integer, String> descriptionRenderer) {
		this(logicalNode,
				Get.identifierService().getConceptSequenceForUuids(((FeatureNodeWithUuids)logicalNode).getTypeConceptUuid()), OchreUtility.getSctId(Get.identifierService().getConceptSequenceForUuids(((FeatureNodeWithUuids)logicalNode).getTypeConceptUuid())), descriptionRenderer);
	}
	private FeatureNodeFxNode(AbstractNode logicalNode, int conceptId, Optional<Long> sctId, Function<Integer, String> descriptionRenderer) {
		super(logicalNode.getNodeSemantic().name() /* + "\n" + LogicalExpressionTreeGraph.logicalNodeTypeToString(logicalNode) */ + "\ntype=" + descriptionRenderer.apply(conceptId) + "\noperator=" + ((FeatureNodeWithSequences)logicalNode).getOperator().name() + (sctId.isPresent() ? "\n" + sctId.get() : ""));
	}
}
