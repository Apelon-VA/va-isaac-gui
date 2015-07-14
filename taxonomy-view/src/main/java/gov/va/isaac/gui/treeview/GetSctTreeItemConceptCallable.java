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

import gov.va.isaac.util.ConceptChronologyUtil;
import gov.vha.isaac.ochre.api.component.concept.ConceptChronology;
import gov.vha.isaac.ochre.api.component.concept.ConceptVersion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

import javafx.application.Platform;
import javafx.concurrent.Task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A concrete {@link Callable} for fetching concepts.
 *
 * @author ocarlsen
 * @author kec
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a> 
 */
public class GetSctTreeItemConceptCallable extends Task<Boolean> {
    private static final Logger LOG = LoggerFactory.getLogger(GetSctTreeItemConceptCallable.class);

    private final SctTreeItem treeItem;
    private final boolean addChildren;
    private final ArrayList<SctTreeItem> childrenToAdd = new ArrayList<>();

    private ConceptChronology<? extends ConceptVersion> concept;

    public GetSctTreeItemConceptCallable(SctTreeItem treeItem) {
        this(treeItem, true);
    }

    public GetSctTreeItemConceptCallable(SctTreeItem treeItem, boolean addChildren) {
        this.treeItem = treeItem;
        this.concept = treeItem != null ? treeItem.getValue() : null;
        this.addChildren = addChildren;
        if (addChildren) {
            treeItem.childLoadStarts();
        }
    }

    @Override
    public Boolean call() throws Exception {
        try
        {    
            // TODO is current value == old value.getRelationshipVersion()?
            if (treeItem == null || treeItem.getValue() == null)
            {
                return false;
            }
            
            if (SctTreeView.wasGlobalShutdownRequested() || treeItem.isCancelRequested()) {
                return false;
            }
            
            // TODO is current value == old value.getRelationshipVersion()?
//            if (addChildren) {
//                reference = treeItem.getValue().getRelationshipVersion().getOriginReference();
//            } else {
//                reference = treeItem.getValue().getRelationshipVersion().getDestinationReference();
//            }
    
            if (SctTreeView.wasGlobalShutdownRequested() || treeItem.isCancelRequested()) {
                return false;
            }
    
           concept = treeItem.getValue();
            
            // TODO how do we determine defined status of ConceptChronology?
//            if ((concept.getConceptAttributes() == null)
//                    || concept.getConceptAttributes().getVersions().isEmpty()
//                    || concept.getConceptAttributes().getVersions().get(0).isDefined()) {
//            	// TODO why is defined being set here?
//                treeItem.setDefined(true);
//            }
            
            if (SctTreeView.wasGlobalShutdownRequested() || treeItem.isCancelRequested()) {
                return false;
            }

            if (ConceptChronologyUtil.getParentsAsConceptNids(treeItem.getValue(), treeItem.getTaxonomyTreeProvider().getTaxonomyTree(), treeItem.getViewCoordinateProvider().getViewCoordinate()).size() > 1) {
                treeItem.setMultiParent(true);
            } 
    
            if (addChildren) {
                //TODO it would be nice to show progress here, by binding this status to the 
                //progress indicator in the SctTreeItem - However -that progress indicator displays at 16x16,
                //and ProgressIndicator has a bug, that is vanishes for anything other than indeterminate for anything less than 32x32
                //need a progress indicator that works at 16x16
                for (ConceptChronology<? extends ConceptVersion> destRel : ConceptChronologyUtil.getChildrenAsConceptChronologies(concept, treeItem.getTaxonomyTreeProvider().getTaxonomyTree(), treeItem.getViewCoordinateProvider().getViewCoordinate())) {
                    if (SctTreeView.wasGlobalShutdownRequested() || treeItem.isCancelRequested()) {
                        return false;
                    }
                        SctTreeItem childItem = new SctTreeItem(destRel, treeItem.getDisplayPolicies(), treeItem.getViewCoordinateProvider(), treeItem.getTaxonomyTreeProvider());
                        if (childItem.shouldDisplay()) {
                            childrenToAdd.add(childItem);
                        }
                        if (SctTreeView.wasGlobalShutdownRequested() || treeItem.isCancelRequested()) {
                            return false;
                        }
                    
                }
                Collections.sort(childrenToAdd);
            }
            
            CountDownLatch temp = new CountDownLatch(1);
    
            Platform.runLater(() -> 
            {
                ConceptChronology<? extends ConceptVersion> itemValue = treeItem.getValue();

                treeItem.setValue(null);
                if (addChildren)
                {
                    treeItem.getChildren().clear();
                    treeItem.getChildren().addAll(childrenToAdd);
                }
                treeItem.setValue(itemValue);
                treeItem.setValue(concept);
                temp.countDown();
            });
            temp.await();
            
            return true;
        }
        catch (Exception e)
        {
            LOG.error("Unexpected", e);
            throw e;
        }
        finally
        {
            if (!SctTreeView.wasGlobalShutdownRequested() && !treeItem.isCancelRequested()) 
            {
                treeItem.childLoadComplete();
            }
        }
    }
}
