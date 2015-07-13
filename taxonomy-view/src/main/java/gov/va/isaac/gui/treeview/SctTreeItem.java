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

import gov.va.isaac.gui.treeview.SctTreeView.TaxonomyTreeProvider;
import gov.va.isaac.interfaces.gui.views.commonFunctionality.taxonomyView.SctTreeItemDisplayPolicies;
import gov.va.isaac.interfaces.gui.views.commonFunctionality.taxonomyView.SctTreeItemI;
import gov.va.isaac.util.ConceptChronologyUtil;
import gov.va.isaac.util.ViewCoordinateFactory.ViewCoordinateProvider;
import gov.vha.isaac.metadata.source.IsaacMetadataAuxiliaryBinding;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.LookupService;
import gov.vha.isaac.ochre.api.component.concept.ConceptChronology;
import gov.vha.isaac.ochre.api.component.concept.ConceptVersion;
import gov.vha.isaac.ochre.util.WorkExecutors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Node;
import javafx.scene.control.TreeItem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link TreeItem} for modeling nodes in the SNOMED CT taxonomy.
 *
 * @author kec
 * @author ocarlsen
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a> 
 */
class SctTreeItem extends TreeItem<ConceptChronology<? extends ConceptVersion>> implements SctTreeItemI, Comparable<SctTreeItem> {

    private static final Logger LOG = LoggerFactory.getLogger(SctTreeItem.class);

    private final List<SctTreeItem> extraParents = new ArrayList<>();
    private CountDownLatch childrenLoadedLatch = new CountDownLatch(1);
    //-2 when not yet started, -1 for started indeterminate - between 0 and 1, if we can determine, 1 when complete.
    private DoubleProperty childLoadPercentComplete = new SimpleDoubleProperty(-2.0);
    private volatile boolean cancelLookup = false;
    private boolean defined = false;
    private boolean multiParent = false;
    private int multiParentDepth = 0;
    private boolean secondaryParentOpened = false;
    private SctTreeItemDisplayPolicies displayPolicies;
    private final ViewCoordinateProvider viewCoordinateProvider;
    private final TaxonomyTreeProvider taxonomyTreeProvider;
    
    private static WorkExecutors workExecutors_ = null;
    
    public ViewCoordinateProvider getViewCoordinateProvider() {
    	return viewCoordinateProvider;
    }
    public TaxonomyTreeProvider getTaxonomyTreeProvider() {
    	return taxonomyTreeProvider;
    }
    
    private static WorkExecutors getWorkExecutors()
    {
        if (workExecutors_ == null)
        {
            workExecutors_ = LookupService.getService(WorkExecutors.class);
        }
        return workExecutors_;
    }

    private static TreeItem<ConceptChronology<? extends ConceptVersion>> getTreeRoot(TreeItem<ConceptChronology<? extends ConceptVersion>> item) {
        TreeItem<ConceptChronology<? extends ConceptVersion>> parent = item.getParent();
        
        if (parent == null) {
            return item;
        } else {
            return getTreeRoot(parent);
        }
    }
    
    SctTreeItem(ConceptChronology<? extends ConceptVersion> taxRef, SctTreeItemDisplayPolicies displayPolicies, ViewCoordinateProvider vcp, TaxonomyTreeProvider ttp) {
        this(taxRef, displayPolicies, vcp, ttp, (Node) null);
    }

    SctTreeItem(ConceptChronology<? extends ConceptVersion> t, SctTreeItemDisplayPolicies displayPolicies, ViewCoordinateProvider vcp, TaxonomyTreeProvider ttp, Node node) {
        super(t, node);
        this.viewCoordinateProvider = vcp;
        this.taxonomyTreeProvider = ttp;
        this.displayPolicies = displayPolicies;
    }

    SctTreeItemDisplayPolicies getDisplayPolicies() {
        return displayPolicies;
    }
    
    void addChildren() {
        childLoadStarts();
        try
        {
            final ConceptChronology<? extends ConceptVersion> taxRef = getValue();
            if (! shouldDisplay()) {
                // Don't add children to something that shouldn't be displayed
                LOG.debug("this.shouldDisplay() == false: not adding children to " + this.getConceptUuid());
            } else if (taxRef == null) {
                LOG.debug("addChildren(): taxRef={}", taxRef);
            } else { // if (taxRef != null)
                // Gather the children
                ArrayList<SctTreeItem> childrenToAdd = new ArrayList<>();
                ArrayList<GetSctTreeItemConceptCallable> childrenToProcess = new ArrayList<>();
    
                for (ConceptChronology<? extends ConceptVersion> rv : ConceptChronologyUtil.getChildrenAsConceptVersions(taxRef, getTaxonomyTreeProvider().getTaxonomyTree(), this.getViewCoordinateProvider().getViewCoordinate())) {
                    SctTreeItem childItem = new SctTreeItem(rv, displayPolicies, viewCoordinateProvider, taxonomyTreeProvider);
                    if (childItem.shouldDisplay()) {
                        childrenToAdd.add(childItem);
                        childrenToProcess.add(new GetSctTreeItemConceptCallable(childItem));
                    } else {
                        LOG.debug("item.shouldDisplay() == false: not adding " + childItem.getConceptUuid() + " as child of " + this.getConceptUuid());
                    }
                }
    
                Collections.sort(childrenToAdd);
                if (cancelLookup) {
                    return;
                }
                
                Platform.runLater(() ->
                {
                    getChildren().addAll(childrenToAdd);
                });
                //This loads the children of this child
                for (GetSctTreeItemConceptCallable child : childrenToProcess) {
                    getWorkExecutors().getPotentiallyBlockingExecutor().execute(child);
                }
                
            }
        }
        catch (Exception e)
        {
            LOG.error("Unexpected error computing children and/or grandchildren", e);
        }
        finally
        {
            childLoadComplete();
        }
    }

    void addChildrenConceptsAndGrandchildrenItems() {
        ArrayList<GetSctTreeItemConceptCallable> grandChildrenToProcess = new ArrayList<>();
        childLoadStarts();
        try
        {
            if (! shouldDisplay()) {
                // Don't add children to something that shouldn't be displayed
                LOG.debug("this.shouldDisplay() == false: not adding children concepts and grandchildren items to " + this.getConceptUuid());
            } else {
                for (TreeItem<ConceptChronology<? extends ConceptVersion>> child : getChildren()) {
                    if (cancelLookup) {
                        return;
                    }
                    if (((SctTreeItem)child).shouldDisplay()) {
                        if (child.getChildren().isEmpty() && (child.getValue() != null)) {
                            if (ConceptChronologyUtil.getChildrenAsConceptNids(child.getValue(), getTaxonomyTreeProvider().getTaxonomyTree()).size() == 0) {
                                ConceptChronology<? extends ConceptVersion> value = child.getValue();
                                child.setValue(null);
                                SctTreeItem noChildItem = (SctTreeItem) child;
                                noChildItem.computeGraphic();
                                noChildItem.setValue(value);
                            } else if (((SctTreeItem)child).getChildLoadPercentComplete().get() == -2.0){ //If this child hasn't yet been told to load
                                ArrayList<SctTreeItem> grandChildrenToAdd = new ArrayList<>();
                                ((SctTreeItem)child).childLoadStarts();
    
                                for (ConceptChronology<? extends ConceptVersion> r : ConceptChronologyUtil.getChildrenAsConceptVersions(child.getValue(), getTaxonomyTreeProvider().getTaxonomyTree(), getViewCoordinateProvider().getViewCoordinate())) {
                                    if (cancelLookup) {
                                        return;
                                    }
                                            SctTreeItem grandChildItem = new SctTreeItem(r, displayPolicies, viewCoordinateProvider, taxonomyTreeProvider);
    
                                            if (grandChildItem.shouldDisplay()) {
                                                grandChildrenToProcess.add(new GetSctTreeItemConceptCallable(grandChildItem));
                                                grandChildrenToAdd.add(grandChildItem);
                                            } else {
                                                LOG.debug("grandChildItem.shouldDisplay() == false: not adding " + grandChildItem.getConceptUuid() + " as child of " + ((SctTreeItem)child).getConceptUuid());
                                            }
                                }
    
                                Collections.sort(grandChildrenToAdd);
                                if (cancelLookup) {
                                    return;
                                }
                                
                                CountDownLatch wait = new CountDownLatch(1);
                                Platform.runLater(() ->
                                {
                                    child.getChildren().addAll(grandChildrenToAdd);
                                    ((SctTreeItem)child).childLoadComplete();
                                    wait.countDown();
                                });
                                wait.await();
                            }
                        } else if ((child.getValue() == null) && ((SctTreeItem)child).getChildLoadPercentComplete().get() == -2.0) {
                            grandChildrenToProcess.add(new GetSctTreeItemConceptCallable((SctTreeItem) child));
                        }
                    } else {
                        LOG.debug("childItem.shouldDisplay() == false: not adding " + ((SctTreeItem)child).getConceptUuid() + " as child of " + this.getConceptUuid());
                    }
                }
                
                if (cancelLookup) {
                    return;
                }
    
                //This loads the childrens children
                for (GetSctTreeItemConceptCallable childsChild : grandChildrenToProcess) {
                    getWorkExecutors().getPotentiallyBlockingExecutor().execute(childsChild);
                }
            }
        }
        catch (Exception e)
        {
            LOG.error("Unexpected error computing children and/or grandchildren", e);
        }
        finally
        {
            childLoadComplete();
        }
    }

    @Override
    public int compareTo(SctTreeItem o) {
        return this.toString().compareTo(o.toString());
    }

    public UUID getConceptUuid() {
        return getValue() != null ? getValue().getPrimordialUuid() : null;
    }
    @Override
    public Integer getConceptNid() {
        return getValue() != null ? getValue().getNid() : null;
    }
    private static Integer getConceptNid(TreeItem<ConceptChronology<? extends ConceptVersion>> item) {
        return item != null && item.getValue() != null ? item.getValue().getNid() : null;
    }
    
    @Override
    public boolean isRoot() {
        ConceptChronology<? extends ConceptVersion> ref = getValue();

        if (IsaacMetadataAuxiliaryBinding.ISAAC_ROOT.getPrimodialUuid().equals(this.getConceptUuid())) {
            return true;
        //} else if (ref != null && ref.getRelationshipVersion() == null) {
        //    return true;
        } else if (this.getParent() == null) {
            return true;
        } else {
            TreeItem<ConceptChronology<? extends ConceptVersion>> root = getTreeRoot(this);

            if (this == root) {
                return true;
            } else if (getConceptNid(root) == getConceptNid()) {
                return true;
            }
            else {
                return false;
            }
        }
    }
    
    public Node computeGraphic() {
        return displayPolicies.computeGraphic(this);
    }
    
    public boolean shouldDisplay() {
        return displayPolicies.shouldDisplay(this);
    }

    /**
     * @see javafx.scene.control.TreeItem#toString()
     * WARNING: toString is currently used in compareTo()
     */
    @Override
    public String toString() {
        return toString(this);
    }
    
    public static String toString(SctTreeItem item) {
        try {
            if (item.getValue() != null) {
            	String desc = ConceptChronologyUtil.getDescription(item.getValue(), item.getViewCoordinateProvider().getViewCoordinate());
                if (desc != null) {
                	return desc;
                } else {
                	LOG.debug("No description found for concept {}", item.getValue().toUserString());
                }
            }

            return "root";
        } catch (RuntimeException re) {
            LOG.error("Caught {} \"{}\"", re.getClass().getName(), re.getLocalizedMessage());
            throw re;
        } catch (Error e) {
            LOG.error("Caught {} \"{}\"", e.getClass().getName(), e.getLocalizedMessage());
            throw e;
        }
    }

    public List<SctTreeItem> getExtraParents() {
        return extraParents;
    }

    @Override
    public int getMultiParentDepth() {
        return multiParentDepth;
    }

    /**
     * returns -2 when not yet started, -1 when started, but indeterminate otherwise, a value between 0 and 1 (1 when complete)
     */
    public DoubleProperty getChildLoadPercentComplete() {
        return childLoadPercentComplete;
    }

    @Override
    public boolean isDefined() {
        return defined;
    }

    @Override
    public boolean isLeaf() {
        if (multiParentDepth > 0) {
            return true;
        }

        return super.isLeaf();
    }

    @Override
    public boolean isMultiParent() {
        if (multiParent) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean isSecondaryParentOpened() {
        if (secondaryParentOpened) {
            return true;
        } else {
            return false;
        }
    }

    public void setDefined(boolean defined) {
        this.defined = defined;
    }

    public void setMultiParent(boolean multiParent) {
        this.multiParent = multiParent;
    }

    public void setMultiParentDepth(int multiParentDepth) {
        this.multiParentDepth = multiParentDepth;
    }

    public void setSecondaryParentOpened(boolean secondaryParentOpened) {
        this.secondaryParentOpened = secondaryParentOpened;
    }

    public void blockUntilChildrenReady() throws InterruptedException {
        childrenLoadedLatch.await();
    }
    
    public void clearChildren()
    {
        cancelLookup = true;
        childrenLoadedLatch.countDown();
        for (TreeItem<ConceptChronology<? extends ConceptVersion>> child : getChildren())
        {
            ((SctTreeItem)child).clearChildren();
        }
        getChildren().clear();
    }
    
    protected void resetChildrenCalculators()
    {
        CountDownLatch cdl = new CountDownLatch(1);
        Runnable r = new Runnable()
        {
            @Override
            public void run()
            {
                cancelLookup = false;
                childLoadPercentComplete.set(-2);
                childrenLoadedLatch.countDown();
                childrenLoadedLatch = new CountDownLatch(1);
                cdl.countDown();
            }
        };
        if (Platform.isFxApplicationThread())
        {
            r.run();
        }
        else
        {
            Platform.runLater(r);
        }
        try
        {
            cdl.await();
        }
        catch (InterruptedException e)
        {
            LOG.error("unexpected interrupt", e);
        }
    }
    
    public void removeGrandchildren() {
        for (TreeItem<ConceptChronology<? extends ConceptVersion>> child : getChildren()) {
           ((SctTreeItem)child).clearChildren();
           ((SctTreeItem)child).resetChildrenCalculators();
        }
    }
    
    /**
     * Can be called on either a background or the FX thread
     */
    protected void childLoadStarts()
    {
        CountDownLatch cdl = new CountDownLatch(1);
        Runnable r = new Runnable()
        {
            @Override
            public void run()
            {
                childLoadPercentComplete.set(-1);
                cdl.countDown();
            }
        };
        if (Platform.isFxApplicationThread())
        {
            r.run();
        }
        else
        {
            Platform.runLater(r);
        }
        try
        {
            cdl.await();
        }
        catch (InterruptedException e)
        {
            LOG.error("unexpected interrupt", e);
        }
    }
    
    /**
     * Can be called on either a background or the FX thread
     */
    protected void childLoadComplete()
    {
        Runnable r = new Runnable()
        {
            @Override
            public void run()
            {
                childLoadPercentComplete.set(1.0);
                childrenLoadedLatch.countDown();
            }
        };
        if (Platform.isFxApplicationThread())
        {
            r.run();
        }
        else
        {
            Platform.runLater(r);
        }
    }
    
    protected boolean isCancelRequested()
    {
        return cancelLookup;
    }
}
