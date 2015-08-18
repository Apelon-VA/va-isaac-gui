package gov.va.isaac.logic.treeview.nodes;

import java.util.Optional;
import java.util.UUID;

import gov.va.isaac.logic.treeview.LogicalExpressionTreeGraph;
import gov.va.isaac.util.OchreUtility;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.model.logic.node.AbstractNode;
import gov.vha.isaac.ochre.model.logic.node.external.RoleNodeAllWithUuids;
import gov.vha.isaac.ochre.model.logic.node.external.RoleNodeSomeWithUuids;
import gov.vha.isaac.ochre.model.logic.node.internal.RoleNodeAllWithSequences;
import gov.vha.isaac.ochre.model.logic.node.internal.RoleNodeSomeWithSequences;
import javafx.scene.Node;
import javafx.scene.control.Label;

// RoleNodeAllWithUuids
// RoleNodeSomeWithUuids
// RoleNodeAllWithSequences
// RoleNodeSomeWithSequences
public class RoleNodeFxNode extends Label {
	private final int typeSequence;
	
	public int getTypeSequence() { return typeSequence; }
	
	public RoleNodeFxNode(RoleNodeAllWithSequences logicalNode) {
		this(logicalNode, ((RoleNodeAllWithSequences)logicalNode).getTypeConceptSequence(), OchreUtility.getSctId(((RoleNodeAllWithSequences)logicalNode).getTypeConceptSequence()));
	}
	public RoleNodeFxNode(RoleNodeSomeWithSequences logicalNode) {
		this(logicalNode, ((RoleNodeSomeWithSequences)logicalNode).getTypeConceptSequence(), OchreUtility.getSctId(((RoleNodeSomeWithSequences)logicalNode).getTypeConceptSequence()));
	}
	public RoleNodeFxNode(RoleNodeAllWithUuids logicalNode) {
		this(logicalNode, Get.identifierService().getConceptSequenceForUuids(((RoleNodeAllWithUuids)logicalNode).getTypeConceptUuid()), OchreUtility.getSctId(Get.identifierService().getConceptSequenceForUuids(((RoleNodeAllWithUuids)logicalNode).getTypeConceptUuid())));
	}
	public RoleNodeFxNode(RoleNodeSomeWithUuids logicalNode) {
		this(logicalNode, Get.identifierService().getConceptSequenceForUuids(((RoleNodeSomeWithUuids)logicalNode).getTypeConceptUuid()), OchreUtility.getSctId(Get.identifierService().getConceptSequenceForUuids(((RoleNodeSomeWithUuids)logicalNode).getTypeConceptUuid())));
	}
	
	private RoleNodeFxNode(AbstractNode logicalNode, int typeSequence, Optional<Long> sctId) {
		super(logicalNode.getNodeSemantic().name() + /* "\n" + LogicalExpressionTreeGraph.logicalNodeTypeToString(logicalNode) + */ "\n" + /* "type=" + */ Get.conceptDescriptionText(typeSequence) + (sctId.isPresent() ? "\n" + sctId.get() : ""));
		this.typeSequence = typeSequence;
	}
}
