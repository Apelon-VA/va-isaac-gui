package gov.va.isaac.logic.treeview.nodes;

import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.impl.utility.Frills;
import gov.vha.isaac.ochre.model.logic.node.AbstractNode;
import gov.vha.isaac.ochre.model.logic.node.external.RoleNodeAllWithUuids;
import gov.vha.isaac.ochre.model.logic.node.external.RoleNodeSomeWithUuids;
import gov.vha.isaac.ochre.model.logic.node.internal.RoleNodeAllWithSequences;
import gov.vha.isaac.ochre.model.logic.node.internal.RoleNodeSomeWithSequences;

import java.util.Optional;
import java.util.function.Function;

// RoleNodeAllWithUuids
// RoleNodeSomeWithUuids
// RoleNodeAllWithSequences
// RoleNodeSomeWithSequences
public class RoleNodeFxNode extends AbstractTreeNodeFxNodeWithConcept {
	private final int typeSequence;
	
	public int getTypeSequence() { return typeSequence; }
	
	public RoleNodeFxNode(RoleNodeAllWithSequences logicalNode, Function<Integer, String> descriptionRenderer) {
		this(logicalNode, ((RoleNodeAllWithSequences)logicalNode).getTypeConceptSequence(), 
				Frills.getSctId(Get.identifierService().getConceptNid(((RoleNodeAllWithSequences)logicalNode).getTypeConceptSequence()), null),
                                Get.identifierService().getConceptNid(((RoleNodeAllWithSequences)logicalNode).getTypeConceptSequence()), 
                                descriptionRenderer);
	}
	public RoleNodeFxNode(RoleNodeSomeWithSequences logicalNode, Function<Integer, String> descriptionRenderer) {
		this(logicalNode, ((RoleNodeSomeWithSequences)logicalNode).getTypeConceptSequence(), 
				Frills.getSctId(Get.identifierService().getConceptNid(((RoleNodeSomeWithSequences)logicalNode).getTypeConceptSequence()), null),
                                Get.identifierService().getConceptNid(((RoleNodeSomeWithSequences)logicalNode).getTypeConceptSequence()),
                                descriptionRenderer);
	}
	public RoleNodeFxNode(RoleNodeAllWithUuids logicalNode, Function<Integer, String> descriptionRenderer) {
		this(logicalNode, Get.identifierService().getConceptSequenceForUuids(((RoleNodeAllWithUuids)logicalNode).getTypeConceptUuid()), 
				Frills.getSctId(Get.identifierService().getNidForUuids(((RoleNodeAllWithUuids)logicalNode).getTypeConceptUuid()), null),
                                Get.identifierService().getNidForUuids(((RoleNodeAllWithUuids)logicalNode).getTypeConceptUuid()),
                                descriptionRenderer);
	}
	public RoleNodeFxNode(RoleNodeSomeWithUuids logicalNode, Function<Integer, String> descriptionRenderer) {
		this(logicalNode, Get.identifierService().getConceptSequenceForUuids(((RoleNodeSomeWithUuids)logicalNode).getTypeConceptUuid()), 
				Frills.getSctId(Get.identifierService().getNidForUuids(((RoleNodeSomeWithUuids)logicalNode).getTypeConceptUuid()), null),
                                Get.identifierService().getNidForUuids(((RoleNodeSomeWithUuids)logicalNode).getTypeConceptUuid()),
                                descriptionRenderer);
	}
	/*
sctId.isPresent() && sctId.get() <= Integer.MAX_VALUE ? "\n" + descriptionRenderer.apply(sctId.get().intValue())
*/
	private RoleNodeFxNode(AbstractNode logicalNode, int typeSequence, Optional<Long> sctId, int sequenceId, Function<Integer, String> descriptionRenderer) {
		super(logicalNode, descriptionRenderer.apply(sequenceId));
		this.typeSequence = typeSequence;
	}

	@Override
	public int getConceptId() {
		return typeSequence;
	}
}
