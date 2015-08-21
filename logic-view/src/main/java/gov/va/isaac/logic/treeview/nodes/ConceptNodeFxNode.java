package gov.va.isaac.logic.treeview.nodes;

import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.impl.utility.Frills;
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
		this(logicalNode, logicalNode.getConceptSequence(), descriptionRenderer);
	}
	public ConceptNodeFxNode(ConceptNodeWithUuids logicalNode, Function<Integer, String> descriptionRenderer) {
		this(logicalNode, Get.identifierService().getConceptSequenceForUuids(logicalNode.getConceptUuid()), descriptionRenderer);
	}
	private ConceptNodeFxNode(AbstractNode logicalNode, int conceptId, Function<Integer, String> descriptionRenderer) {
		super(logicalNode.getNodeSemantic().name() /* + "\n" + LogicalExpressionTreeGraph.logicalNodeTypeToString(logicalNode) */ + "\n" + descriptionRenderer.apply(conceptId));
	}
}
