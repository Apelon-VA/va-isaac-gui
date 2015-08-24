package gov.va.isaac.logic.treeview.nodes;

import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.impl.utility.Frills;
import gov.vha.isaac.ochre.model.logic.node.AbstractNode;
import gov.vha.isaac.ochre.model.logic.node.external.RoleNodeAllWithUuids;
import gov.vha.isaac.ochre.model.logic.node.external.RoleNodeSomeWithUuids;
import gov.vha.isaac.ochre.model.logic.node.internal.RoleNodeAllWithSequences;
import gov.vha.isaac.ochre.model.logic.node.internal.RoleNodeSomeWithSequences;

import java.util.Optional;

import javafx.scene.control.Label;

// RoleNodeAllWithUuids
// RoleNodeSomeWithUuids
// RoleNodeAllWithSequences
// RoleNodeSomeWithSequences
public class RoleNodeFxNode extends AbstractTreeNodeFxNodeWithConcept {
	private final int typeSequence;
	
	public int getTypeSequence() { return typeSequence; }
	
	public RoleNodeFxNode(RoleNodeAllWithSequences logicalNode) {
		this(logicalNode, ((RoleNodeAllWithSequences)logicalNode).getTypeConceptSequence(), 
				Frills.getSctId(Get.identifierService().getConceptNid(((RoleNodeAllWithSequences)logicalNode).getTypeConceptSequence()), null));
	}
	public RoleNodeFxNode(RoleNodeSomeWithSequences logicalNode) {
		this(logicalNode, ((RoleNodeSomeWithSequences)logicalNode).getTypeConceptSequence(), 
				Frills.getSctId(Get.identifierService().getConceptNid(((RoleNodeSomeWithSequences)logicalNode).getTypeConceptSequence()), null));
	}
	public RoleNodeFxNode(RoleNodeAllWithUuids logicalNode) {
		this(logicalNode, Get.identifierService().getConceptSequenceForUuids(((RoleNodeAllWithUuids)logicalNode).getTypeConceptUuid()), 
				Frills.getSctId(Get.identifierService().getNidForUuids(((RoleNodeAllWithUuids)logicalNode).getTypeConceptUuid()), null));
	}
	public RoleNodeFxNode(RoleNodeSomeWithUuids logicalNode) {
		this(logicalNode, Get.identifierService().getConceptSequenceForUuids(((RoleNodeSomeWithUuids)logicalNode).getTypeConceptUuid()), 
				Frills.getSctId(Get.identifierService().getNidForUuids(((RoleNodeSomeWithUuids)logicalNode).getTypeConceptUuid()), null));
	}
	
	private RoleNodeFxNode(AbstractNode logicalNode, int typeSequence, Optional<Long> sctId) {
		super(logicalNode, logicalNode.getNodeSemantic().name() + /* "\n" + LogicalExpressionTreeGraph.logicalNodeTypeToString(logicalNode) + */ "\n" + /* "type=" + */ 
				Get.conceptDescriptionText(typeSequence) + (sctId.isPresent() ? "\n" + sctId.get() : ""));
		this.typeSequence = typeSequence;
	}

	@Override
	public int getConceptId() {
		return typeSequence;
	}
}
