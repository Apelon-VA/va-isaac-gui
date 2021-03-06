package gov.va.isaac.logic.treeview.nodes;

import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.model.logic.node.AbstractNode;
import gov.vha.isaac.ochre.model.logic.node.external.FeatureNodeWithUuids;
import gov.vha.isaac.ochre.model.logic.node.internal.FeatureNodeWithSequences;

import java.util.function.Function;

import javafx.scene.control.Label;

// FeatureNodeWithSequences
// FeatureNodeWithUuids
public class FeatureNodeFxNode extends AbstractTreeNodeFxNodeWithConcept {
	private final int conceptId;
	
	public FeatureNodeFxNode(FeatureNodeWithSequences logicalNode, Function<Integer, String> descriptionRenderer) {
		this(logicalNode, ((FeatureNodeWithSequences)logicalNode).getTypeConceptSequence(), descriptionRenderer);
	}
	public FeatureNodeFxNode(FeatureNodeWithUuids logicalNode, Function<Integer, String> descriptionRenderer) {
		this(logicalNode,
				Get.identifierService().getConceptSequenceForUuids(((FeatureNodeWithUuids)logicalNode).getTypeConceptUuid()), descriptionRenderer);
	}
	private FeatureNodeFxNode(AbstractNode logicalNode, int conceptId, Function<Integer, String> descriptionRenderer) {
		super(logicalNode, "FEATURE" /* + "\n" + LogicalExpressionTreeGraph.logicalNodeTypeToString(logicalNode) */ + "\ntype=" + descriptionRenderer.apply(conceptId) + "\noperator=" + ((FeatureNodeWithSequences)logicalNode).getOperator().name());
		this.conceptId = conceptId;
	}
	
	@Override
	public int getConceptId() {
		return conceptId;
	}
}
