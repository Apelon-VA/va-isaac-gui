/**
 * Copyright Notice
 *
 * This is a work of the U.S. Government and is not subject to copyright 
 * protection in the United States. Foreign copyrights may apply.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gov.va.isaac.gui.treeview;

import gov.va.isaac.AppContext;
import gov.va.isaac.ExtendedAppContext;
import gov.va.isaac.gui.dragAndDrop.DragRegistry;
import gov.va.isaac.gui.dragAndDrop.SingleConceptIdProvider;
import gov.va.isaac.gui.util.Images;
import gov.va.isaac.util.CommonMenuBuilderI;
import gov.va.isaac.util.CommonMenus;
import gov.va.isaac.util.CommonMenus.CommonMenuItem;
import gov.va.isaac.util.CommonMenusNIdProvider;
import gov.va.isaac.util.ConceptChronologyUtil;
import gov.va.isaac.util.Utility;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.component.concept.ConceptChronology;
import gov.vha.isaac.ochre.api.component.concept.ConceptVersion;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.RectangleBuilder;

import org.ihtsdo.otf.tcc.api.contradiction.ContradictionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link TreeCell} for rendering {@link ConceptChronology<ConceptVersion>} objects.
 *
 * @author kec
 * @author ocarlsen
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a> 
 */
@SuppressWarnings("deprecation")
final class SctTreeCell extends TreeCell<ConceptChronology<? extends ConceptVersion>> {

    private static final Logger LOG = LoggerFactory.getLogger(SctTreeCell.class);

    SctTreeCell() {
        super();

        // Handle left-clicks.
        ClickListener eventHandler = new ClickListener();
        setOnMouseClicked(eventHandler);

        // Handle right-clicks.
        ContextMenu cm = buildContextMenu();
        setContextMenu(cm);
        
        //Allow drags
        AppContext.getService(DragRegistry.class).setupDragOnly(this, new SingleConceptIdProvider()
        {
            @Override
            public String getConceptId()
            {
                final ConceptChronology<? extends ConceptVersion> conceptChronicle = SctTreeCell.this.getItem();
                final UUID conceptUuid = conceptChronicle.getPrimordialUuid();

                return conceptUuid.toString();
            }
        });
    }

    private void openOrCloseParent(SctTreeItem treeItem) throws IOException, ContradictionException {
        ConceptChronology<? extends ConceptVersion> value = treeItem.getValue();

        if (value != null) {
            treeItem.setValue(null);

            SctTreeItem parentItem = (SctTreeItem) treeItem.getParent();
            ObservableList<TreeItem<ConceptChronology<? extends ConceptVersion>>> siblings = parentItem.getChildren();

            if (treeItem.isSecondaryParentOpened()) {
                removeExtraParents(treeItem, siblings);
            } else {
                ArrayList<ConceptChronology<? extends ConceptVersion>> allParents = new ArrayList<>(ConceptChronologyUtil.getParentsAsConceptVersions(value, treeItem.getTaxonomyTreeProvider().getTaxonomyTree(), treeItem.getViewCoordinateProvider().getViewCoordinate()));

                List<ConceptChronology<? extends ConceptVersion>> secondaryParents = new ArrayList<>();
                for (ConceptChronology<? extends ConceptVersion> parent : allParents) {
                	if (allParents.size() == 1 || parent.getNid() != parentItem.getValue().getNid()) {
                		secondaryParents.add(parent);
                	}
                }

                ArrayList<SctTreeItem> secondaryParentItems = new ArrayList<>(secondaryParents.size());

                for (ConceptChronology<? extends ConceptVersion> extraParent : secondaryParents) {
                        SctTreeItem extraParentItem =
                                new SctTreeItem(extraParent,
                                                treeItem.getDisplayPolicies(),
                                                treeItem.getViewCoordinateProvider(),
                                                treeItem.getTaxonomyTreeProvider());
                        extraParentItem.setMultiParentDepth(treeItem.getMultiParentDepth() + 1);
                        secondaryParentItems.add(extraParentItem);
                }

                Collections.sort(secondaryParentItems);
                Collections.reverse(secondaryParentItems);

                int startIndex = siblings.indexOf(treeItem);

                for (SctTreeItem extraParentItem : secondaryParentItems) {
                    parentItem.getChildren().add(startIndex++, extraParentItem);
                    treeItem.getExtraParents().add(extraParentItem);
                    Utility.execute(new GetSctTreeItemConceptCallable(extraParentItem, false));
                }
            }

            treeItem.setValue(value);
            treeItem.setSecondaryParentOpened(!treeItem.isSecondaryParentOpened());
            treeItem.computeGraphic();
        }
    }

    @Override
    protected void updateItem(ConceptChronology<? extends ConceptVersion> taxRef, boolean empty) {
        super.updateItem(taxRef, empty);
        double opacity = 0.0;
        
        if (empty)
        {
            setText("");
            setGraphic(null);
        }
        else if (!empty && taxRef == null) {
            LOG.debug("ConceptChronology<? extends ConceptVersion> is null");
            setText("");
            setGraphic(null);
        }
        else if (!empty && taxRef != null) {
            final SctTreeItem treeItem = (SctTreeItem) getTreeItem();

            if (treeItem.getMultiParentDepth() > 0) {
                if (treeItem.isLeaf()) {
                    BorderPane graphicBorderPane = new BorderPane();
                    int multiParentInset = (treeItem.getMultiParentDepth() * 16) + 10;
                    Rectangle leftRect = RectangleBuilder.create().width(multiParentInset).height(16).build();

                    leftRect.setOpacity(opacity);
                    
                    StackPane spacerProgressStack = new StackPane();
                    spacerProgressStack.getChildren().add(leftRect);
                    
                    ProgressIndicator pi = new ProgressIndicator();
                    pi.setPrefSize(16, 16);
                    pi.setMaxSize(16, 16);
                    pi.progressProperty().bind(treeItem.getChildLoadPercentComplete());
                    pi.visibleProperty().bind(treeItem.getChildLoadPercentComplete().lessThan(1.0).and(treeItem.getChildLoadPercentComplete().greaterThanOrEqualTo(-1.0)));
                    pi.setMouseTransparent(true);
                    spacerProgressStack.getChildren().add(pi);
                    StackPane.setAlignment(pi, Pos.CENTER_RIGHT);
                    StackPane.setMargin(pi, new Insets(0, 10, 0, 0));
                    graphicBorderPane.setLeft(spacerProgressStack);
                    graphicBorderPane.setCenter(treeItem.computeGraphic());
                    setGraphic(graphicBorderPane);
                }

                String desc = ConceptChronologyUtil.getDescription(taxRef, treeItem.getViewCoordinateProvider().getViewCoordinate());
                if (desc != null) {
                    setText(desc);
                } else {
                	LOG.debug("No description found for concept {}", taxRef.toUserString());
                }

                return;
            }

            ImageView iv = treeItem.isExpanded() ? Images.TAXONOMY_CLOSE.createImageView() : Images.TAXONOMY_OPEN.createImageView();
            iv.setFitHeight(16.0);
            iv.setFitWidth(16.0);

            setDisclosureNode(iv);

            String desc = ConceptChronologyUtil.getDescription(taxRef, treeItem.getViewCoordinateProvider().getViewCoordinate());
            if (desc != null) {
                setText(desc);
            } else {
            	LOG.debug("No description found for concept {}", taxRef.toUserString());
            }

            Rectangle leftRect = RectangleBuilder.create().width(treeItem.isLeaf() ? 16 : 18).height(16).build();
            leftRect.setOpacity(opacity);

            BorderPane graphicBorderPane = new BorderPane();
            StackPane spacerProgressStack = new StackPane();
            spacerProgressStack.getChildren().add(leftRect);
            
            graphicBorderPane.setLeft(spacerProgressStack);
            graphicBorderPane.setCenter(treeItem.computeGraphic());
            
            ProgressIndicator pi = new ProgressIndicator();
            pi.setPrefSize(16, 16);
            pi.setMaxSize(16, 16);
            pi.progressProperty().bind(treeItem.getChildLoadPercentComplete());
            pi.visibleProperty().bind(treeItem.getChildLoadPercentComplete().lessThan(1.0).and(treeItem.getChildLoadPercentComplete().greaterThanOrEqualTo(-1.0)));
            pi.setMouseTransparent(true);
            spacerProgressStack.getChildren().add(pi);
            StackPane.setAlignment(pi, Pos.CENTER_LEFT);
            
            setGraphic(graphicBorderPane);
        }
    }

    private void removeExtraParents(SctTreeItem treeItem, ObservableList<TreeItem<ConceptChronology<? extends ConceptVersion>>> siblings) {
        for (SctTreeItem extraParent : treeItem.getExtraParents()) {
            removeExtraParents(extraParent, siblings);
            siblings.remove(extraParent);
        }
    }

    private ContextMenu buildContextMenu() {
        ContextMenu cm = new ContextMenu();

        // Add a Menus item.
        
        CommonMenuBuilderI builder = CommonMenus.getDefaultMenuBuilder();
        builder.setMenuItemsToExclude(CommonMenuItem.TAXONOMY_VIEW);
        
        CommonMenus.addCommonMenus(cm, builder, new CommonMenusNIdProvider()
        {
            
            @Override
            public Collection<Integer> getNIds()
            {
                ConceptChronology<? extends ConceptVersion> item = SctTreeCell.this.getItem();
                try
                {
                    if (item != null ) {
                        return Arrays.asList(new Integer[] {ExtendedAppContext.getDataStore().getNidForUuids(item.getPrimordialUuid())});
                    } else {
                    	String text = SctTreeCell.this.getText();
                        LOG.warn("Couldn't locate an identifer for the node (with null item) {}", text);
                        return Arrays.asList(new Integer[] {});
                    }
                }
                catch (Exception e)
                {
                    LOG.error("Unexpected", e);
                    return Arrays.asList(new Integer[] {});
                }
            }
        }); 
        
        return cm;
    }

    /**
     * Listens for mouse clicks to expand/collapse node.
     */
    private final class ClickListener implements EventHandler<MouseEvent> {

        @Override
        public void handle(MouseEvent t) {
            if (getItem() != null) {
                if (getGraphic().getBoundsInParent().contains(t.getX(), t.getY())) {
                    SctTreeItem item = (SctTreeItem) getTreeItem();

                    if (item.isMultiParent() || item.getMultiParentDepth() > 0) {
                        try {
                            openOrCloseParent(item);
                        } catch (ContradictionException | IOException ex) {
                            LOG.error(null, ex);
                        }
                    }
                }
            }
        }
    }
}
