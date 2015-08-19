package gov.va.isaac.logic.treeview.nodes;

import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.impl.utility.Frills;
import gov.vha.isaac.ochre.model.logic.node.AbstractNode;
import gov.vha.isaac.ochre.model.logic.node.external.ConceptNodeWithUuids;
import gov.vha.isaac.ochre.model.logic.node.internal.ConceptNodeWithSequences;
import java.util.Optional;
import javafx.scene.control.Label;

// ConceptNodeWithSequences
// ConceptNodeWithUuids
public class ConceptNodeFxNode extends Label {
	public ConceptNodeFxNode(ConceptNodeWithSequences logicalNode) {
		this(logicalNode, Get.conceptDescriptionText(logicalNode.getConceptSequence()), 
				Frills.getSctId(Get.identifierService().getConceptNid(logicalNode.getConceptSequence()), null));
	}
	public ConceptNodeFxNode(ConceptNodeWithUuids logicalNode) {
		this(logicalNode, Get.conceptDescriptionText(Get.identifierService().getConceptSequenceForUuids(logicalNode.getConceptUuid())), 
				Frills.getSctId(Get.identifierService().getNidForUuids(logicalNode.getConceptUuid()), null));
	}
	private ConceptNodeFxNode(AbstractNode logicalNode, String desc, Optional<Long> sctId) {
		super(logicalNode.getNodeSemantic().name() /* + "\n" + LogicalExpressionTreeGraph.logicalNodeTypeToString(logicalNode) */ + "\n" + desc + (sctId.isPresent() ? "\n" + sctId.get() : ""));
	}
}
