package gov.va.isaac.logic.treeview.nodes;

import gov.va.isaac.logic.treeview.LogicalExpressionTreeGraph;
import gov.va.isaac.util.OchreUtility;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.model.logic.node.AbstractNode;
import gov.vha.isaac.ochre.model.logic.node.external.ConceptNodeWithUuids;
import gov.vha.isaac.ochre.model.logic.node.internal.ConceptNodeWithSequences;

import java.util.Optional;
import java.util.function.Function;

import javafx.scene.control.Label;

// ConceptNodeWithSequences
// ConceptNodeWithUuids
public class ConceptNodeFxNode extends Label {
	
	public ConceptNodeFxNode(ConceptNodeWithSequences logicalNode, Function<Integer, String> descriptionRenderer) {
		this(logicalNode, logicalNode.getConceptSequence(), OchreUtility.getSctId(logicalNode.getConceptSequence()), descriptionRenderer);
	}
	public ConceptNodeFxNode(ConceptNodeWithUuids logicalNode, Function<Integer, String> descriptionRenderer) {
		this(logicalNode, Get.identifierService().getConceptSequenceForUuids(logicalNode.getConceptUuid()), OchreUtility.getSctId(Get.identifierService().getConceptSequenceForUuids(logicalNode.getConceptUuid())), descriptionRenderer);
	}
	private ConceptNodeFxNode(AbstractNode logicalNode, int conceptId, Optional<Long> sctId, Function<Integer, String> descriptionRenderer) {
		super(logicalNode.getNodeSemantic().name() /* + "\n" + LogicalExpressionTreeGraph.logicalNodeTypeToString(logicalNode) */ + "\n" + descriptionRenderer.apply(conceptId) + (sctId.isPresent() ? "\n" + sctId.get() : ""));
	}
}
