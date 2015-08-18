package gov.va.isaac.logic.treeview.nodes;

import gov.va.isaac.logic.treeview.LogicalExpressionTreeGraph;
import gov.va.isaac.util.OchreUtility;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.model.logic.node.AbstractNode;
import gov.vha.isaac.ochre.model.logic.node.external.ConceptNodeWithUuids;
import gov.vha.isaac.ochre.model.logic.node.internal.ConceptNodeWithSequences;

import java.util.Optional;

import javafx.scene.control.Label;

// ConceptNodeWithSequences
// ConceptNodeWithUuids
public class ConceptNodeFxNode extends Label {
	public ConceptNodeFxNode(ConceptNodeWithSequences logicalNode) {
		this(logicalNode, Get.conceptDescriptionText(logicalNode.getConceptSequence()), OchreUtility.getSctId(logicalNode.getConceptSequence()));
	}
	public ConceptNodeFxNode(ConceptNodeWithUuids logicalNode) {
		this(logicalNode, Get.conceptDescriptionText(Get.identifierService().getConceptSequenceForUuids(logicalNode.getConceptUuid())), OchreUtility.getSctId(Get.identifierService().getConceptSequenceForUuids(logicalNode.getConceptUuid())));
	}
	private ConceptNodeFxNode(AbstractNode logicalNode, String desc, Optional<Long> sctId) {
		super(logicalNode.getNodeSemantic().name() /* + "\n" + LogicalExpressionTreeGraph.logicalNodeTypeToString(logicalNode) */ + "\n" + desc + (sctId.isPresent() ? "\n" + sctId.get() : ""));
	}
}
