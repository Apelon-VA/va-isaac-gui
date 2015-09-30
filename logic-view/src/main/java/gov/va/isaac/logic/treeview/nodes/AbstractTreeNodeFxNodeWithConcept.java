package gov.va.isaac.logic.treeview.nodes;

import gov.vha.isaac.ochre.model.logic.node.AbstractNode;
import javafx.scene.control.Label;

public abstract class AbstractTreeNodeFxNodeWithConcept extends Label {
	public abstract int getConceptId();
	
	protected AbstractTreeNodeFxNodeWithConcept(AbstractNode treeNode, String labelText) {
		super(labelText);
		
		javafx.application.Platform.runLater(() -> TreeNodeFxNodeUtils.addContextMenu(this));
	}
}
