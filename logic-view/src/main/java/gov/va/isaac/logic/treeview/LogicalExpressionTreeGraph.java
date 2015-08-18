package gov.va.isaac.logic.treeview;

import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.shape.Circle;
import gov.va.isaac.gui.treegraph.TreeGraph;
import gov.va.isaac.gui.treegraph.TreeNodeImpl;
import gov.va.isaac.gui.treegraph.TreeNodeUtils;
import gov.va.isaac.logic.treeview.nodes.AndNodeFxNode;
import gov.va.isaac.logic.treeview.nodes.ConceptNodeFxNode;
import gov.va.isaac.logic.treeview.nodes.FeatureNodeFxNode;
import gov.va.isaac.logic.treeview.nodes.OrNodeFxNode;
import gov.va.isaac.logic.treeview.nodes.RoleNodeFxNode;
import gov.va.isaac.logic.treeview.nodes.RootNodeFxNode;
import gov.va.isaac.logic.treeview.nodes.SufficientSetNodeFxNode;
import gov.vha.isaac.metadata.source.IsaacMetadataAuxiliaryBinding;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.logic.LogicalExpression;
import gov.vha.isaac.ochre.api.logic.Node;
import gov.vha.isaac.ochre.model.logic.node.AndNode;
import gov.vha.isaac.ochre.model.logic.node.OrNode;
import gov.vha.isaac.ochre.model.logic.node.RootNode;
import gov.vha.isaac.ochre.model.logic.node.SufficientSetNode;
import gov.vha.isaac.ochre.model.logic.node.external.ConceptNodeWithUuids;
import gov.vha.isaac.ochre.model.logic.node.external.FeatureNodeWithUuids;
import gov.vha.isaac.ochre.model.logic.node.external.RoleNodeAllWithUuids;
import gov.vha.isaac.ochre.model.logic.node.external.RoleNodeSomeWithUuids;
import gov.vha.isaac.ochre.model.logic.node.external.TypedNodeWithUuids;
import gov.vha.isaac.ochre.model.logic.node.internal.ConceptNodeWithSequences;
import gov.vha.isaac.ochre.model.logic.node.internal.FeatureNodeWithSequences;
import gov.vha.isaac.ochre.model.logic.node.internal.RoleNodeAllWithSequences;
import gov.vha.isaac.ochre.model.logic.node.internal.RoleNodeSomeWithSequences;
import gov.vha.isaac.ochre.model.logic.node.internal.TypedNodeWithSequences;

public class LogicalExpressionTreeGraph extends TreeGraph {
	private static final Logger LOG = LoggerFactory.getLogger(LogicalExpressionTreeGraph.class);

	private final boolean ignoreSingleChildConjunctions;
	private final boolean ignoreSingleChildRoleGroups;
	
	private final int defaultNodeWidth;
	private final int defaultNodeHeight;

	public LogicalExpressionTreeGraph() {
		this(
				false, 
				false, 
				200, 50);
	}

	public LogicalExpressionTreeGraph(
			boolean ignoreSingleChildConjunctions,
			boolean ignoreSingleChildRoleGroups,
			int defaultNodeWidth,
			int defaultNodeHeight) {
		this.ignoreSingleChildConjunctions = ignoreSingleChildConjunctions;
		this.ignoreSingleChildRoleGroups = ignoreSingleChildRoleGroups;
		this.defaultNodeWidth = defaultNodeWidth;
		this.defaultNodeHeight = defaultNodeHeight;
	}
	
	public boolean isIgnoreSingleChildConjunctions() {
		return ignoreSingleChildConjunctions;
	}
	public boolean isIgnoreSingleChildRoleGroups() {
		return ignoreSingleChildRoleGroups;
	}
	
	public void displayLogicalExpression(LogicalExpression le) {
		System.out.println("Processing LogicalExpression for concept " + Get.conceptDescriptionText(le.getConceptSequence()));
		System.out.println("Root is " + le.getRoot().getNodeSemantic().name());

		if (le.getNodeCount() > 0) {
			if (le.getNodeCount() > 1) {
				LOG.warn("Passed LogicalExpression with {} > 1 nodes.  Displaying only the first", le.getNodeCount());
			}
			TreeNodeImpl rootTreeNode = new TreeNodeImpl(null, createFxNodeFromLogicalExpression(le));
			setRootNode(rootTreeNode);
			for (Node child : le.getNode(0).getChildren()) {
				displayLogicalNode(rootTreeNode, le.getNode(0), child);
			}
		} else if (le.getNodeCount() == 0) {
			LOG.warn("Passed LogicalExpression with no children");
		}
	}
	public static String logicalNodeTypeToString(Node node) {
		return node.getClass().getName().replaceAll(".*\\.", "");
	}
	public void displayLogicalNode(TreeNodeImpl parentTreeNode, Node parentLogicalNode, Node logicalNode) {
		System.out.println("Processing " + logicalNode.getNodeSemantic().name() + " node");

		TreeNodeImpl currentTreeNode = null;
		
		// Add AndNode or OrNode single child directly to parent
		if (isIgnoreSingleChildConjunctions() && (logicalNode instanceof AndNode || logicalNode instanceof OrNode) && logicalNode.getChildren().length == 1) {
			displayLogicalNode(parentTreeNode, parentLogicalNode, logicalNode.getChildren()[0]);
			return;
		}

		// Add RoleNodeSomeWithSequences or RoleNodeAllWithSequences
		// with type of "role group (ISAAC)" with single child directly to parent
		if (isIgnoreSingleChildRoleGroups()
				&& logicalNode.getChildren().length == 1
				&& (
						logicalNode instanceof RoleNodeSomeWithSequences
						|| logicalNode instanceof RoleNodeAllWithSequences
						|| logicalNode instanceof RoleNodeSomeWithUuids
						|| logicalNode instanceof RoleNodeAllWithUuids)) {
			UUID typeUuid = null;
			
			if (logicalNode instanceof RoleNodeSomeWithSequences
						|| logicalNode instanceof RoleNodeAllWithSequences) {
				int typeSequence = (logicalNode instanceof RoleNodeSomeWithSequences) ? ((RoleNodeSomeWithSequences)logicalNode).getTypeConceptSequence() : ((RoleNodeAllWithSequences)logicalNode).getTypeConceptSequence();

				Optional<UUID> typeUuidOptional = Get.identifierService().getUuidPrimordialFromConceptSequence(typeSequence);
				if (typeUuidOptional.isPresent()) {
					typeUuid = typeUuidOptional.get();
				}
			} else {
				typeUuid = (logicalNode instanceof RoleNodeSomeWithUuids) ? ((RoleNodeSomeWithUuids)logicalNode).getTypeConceptUuid() : ((RoleNodeAllWithUuids)logicalNode).getTypeConceptUuid();
			}

			if (typeUuid != null && typeUuid.equals(IsaacMetadataAuxiliaryBinding.ROLE_GROUP.getPrimodialUuid())) {
				displayLogicalNode(parentTreeNode, parentLogicalNode, logicalNode.getChildren()[0]);
				return;
			}
		}

		// TODO: Properly handle nodes that are to right rather than below
		if (parentLogicalNode != null && (parentLogicalNode instanceof TypedNodeWithSequences || parentLogicalNode instanceof TypedNodeWithUuids)) {
			parentTreeNode.setChildToRight(currentTreeNode = new TreeNodeImpl(parentTreeNode, createFxNodeFromLogicalNode(logicalNode)));
		} 
		else {
			parentTreeNode.addChildTreeNodeBelow(currentTreeNode = new TreeNodeImpl(parentTreeNode, createFxNodeFromLogicalNode(logicalNode)));
		}

		for (Node child : logicalNode.getChildren()) {
			displayLogicalNode(currentTreeNode, logicalNode, child);
		}
	}

	// TODO: properly populate all labels
	public Label createFxNodeFromLogicalExpression(LogicalExpression logicalExpression) {
		RootNode rootNode = (RootNode)logicalExpression.getNode(0);
		
		RootNodeFxNode label = new RootNodeFxNode(logicalExpression, rootNode);
		TreeNodeUtils.setFxNodeSizes(label, defaultNodeWidth, defaultNodeHeight);

		label.setTooltip(new Tooltip(label.getText()));
		
		return label;
	}
	public Label createFxNodeFromLogicalNode(Node logicalNode) {
		Label label = null;
		
		if (logicalNode instanceof ConceptNodeWithSequences) {
			label = new ConceptNodeFxNode((ConceptNodeWithSequences)logicalNode);
			TreeNodeUtils.setFxNodeSizes(label, defaultNodeWidth, defaultNodeHeight);
		} else if (logicalNode instanceof ConceptNodeWithUuids) {
			label = new ConceptNodeFxNode((ConceptNodeWithSequences)logicalNode);
			TreeNodeUtils.setFxNodeSizes(label, defaultNodeWidth, defaultNodeHeight);
		} else if (logicalNode instanceof FeatureNodeWithSequences) {
			label = new FeatureNodeFxNode((FeatureNodeWithSequences)logicalNode);
			TreeNodeUtils.setFxNodeSizes(label, defaultNodeWidth, defaultNodeHeight);
		} else if (logicalNode instanceof FeatureNodeWithUuids) {
			label = new FeatureNodeFxNode((FeatureNodeWithUuids)logicalNode);
			TreeNodeUtils.setFxNodeSizes(label, defaultNodeWidth, defaultNodeHeight);
		} else if (logicalNode instanceof RoleNodeAllWithSequences) {
			label = new RoleNodeFxNode((RoleNodeAllWithSequences)logicalNode);
			TreeNodeUtils.setFxNodeSizes(label, defaultNodeWidth, defaultNodeHeight);
		} else if (logicalNode instanceof RoleNodeSomeWithSequences) {
			label = new RoleNodeFxNode((RoleNodeSomeWithSequences)logicalNode);
			TreeNodeUtils.setFxNodeSizes(label, defaultNodeWidth, defaultNodeHeight);
		} else if (logicalNode instanceof RoleNodeAllWithUuids) {
			label = new RoleNodeFxNode((RoleNodeAllWithUuids)logicalNode);
			TreeNodeUtils.setFxNodeSizes(label, defaultNodeWidth, defaultNodeHeight);
		} else if (logicalNode instanceof RoleNodeSomeWithUuids) {
			label = new RoleNodeFxNode((RoleNodeSomeWithUuids)logicalNode);
			TreeNodeUtils.setFxNodeSizes(label, defaultNodeWidth, defaultNodeHeight);
		} else if (logicalNode instanceof SufficientSetNode) {
			label = new SufficientSetNodeFxNode((SufficientSetNode)logicalNode);
		} else if (logicalNode instanceof AndNode) {
			label = new AndNodeFxNode((AndNode)logicalNode);
		} else if (logicalNode instanceof OrNode) {
			label = new OrNodeFxNode((OrNode)logicalNode);
		}
		else {
			label = new Label(logicalNode.getNodeSemantic().name() + "\n" + logicalNodeTypeToString(logicalNode));
			TreeNodeUtils.setFxNodeSizes(label, defaultNodeWidth, defaultNodeHeight);
		}
		
		label.setTooltip(new Tooltip(label.getText()));

		return label;
	}
}
