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
import gov.va.isaac.interfaces.treeview.SctTreeItemDisplayPolicies;
import gov.va.isaac.interfaces.treeview.SctTreeItemI;
import gov.va.isaac.interfaces.utility.ShutdownBroadcastListenerI;
import gov.va.isaac.util.WBUtility;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TreeItem;

import org.ihtsdo.otf.tcc.api.concurrency.FutureHelper;
import org.ihtsdo.otf.tcc.api.contradiction.ContradictionException;
import org.ihtsdo.otf.tcc.api.thread.NamedThreadFactory;
import org.ihtsdo.otf.tcc.ddo.ComponentReference;
import org.ihtsdo.otf.tcc.ddo.TaxonomyReferenceWithConcept;
import org.ihtsdo.otf.tcc.ddo.concept.component.relationship.RelationshipChronicleDdo;
import org.ihtsdo.otf.tcc.ddo.concept.component.relationship.RelationshipVersionDdo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link TreeItem} for modeling nodes in the SNOMED CT taxonomy.
 *
 * @author kec
 * @author ocarlsen
 */
public class SctTreeItem extends TreeItem<TaxonomyReferenceWithConcept> implements SctTreeItemI, Comparable<SctTreeItem>, ShutdownBroadcastListenerI {

    private static final Logger LOG = LoggerFactory.getLogger(SctTreeItem.class);

    private static final ThreadGroup sctTreeItemThreadGroup = new ThreadGroup("SctTreeItem child fetcher pool");

    //TODO dan needs to fix this - the executors are static - but the shutdown registry is per-instance... which isn't right.
    //realistically, it shouldn't have its own executor service anyway, we should be using thread pools from OTF-UTIL.
    static ExecutorService childFetcherService;
    static ExecutorService conceptFetcherService;
    static {
        initExecutorPools();
    }

    private final List<SctTreeItem> extraParents = new ArrayList<>();

    private ProgressIndicator progressIndicator;
    private boolean defined = false;
    private boolean multiParent = false;
    private int multiParentDepth = 0;
    private boolean secondaryParentOpened = false;
    private SctTreeItemDisplayPolicies displayPolicies;

    public SctTreeItem(TaxonomyReferenceWithConcept taxRef, SctTreeItemDisplayPolicies displayPolicies) {
        this(taxRef, displayPolicies, (Node) null);
    }

    public SctTreeItem(TaxonomyReferenceWithConcept t, SctTreeItemDisplayPolicies displayPolicies, Node node) {
        super(t, node);
        this.displayPolicies = displayPolicies;
        AppContext.getMainApplicationWindow().registerShutdownListener(this);
    }

    public SctTreeItemDisplayPolicies getDisplayPolicies() {
    	return displayPolicies;
    }
   
    public void addChildren() {
    	LOG.debug("Adding children to " + getConceptUuid());
        final TaxonomyReferenceWithConcept taxRef = getValue();
        if (! shouldDisplay()) {
    		// Don't add children to something that shouldn't be displayed
    		LOG.debug("this.shouldDisplay() == false: not adding children to " + this.getConceptUuid());
    	} else if (taxRef.getConcept() != null) {
            // Configure a new progress indicator.
            ProgressIndicator pi = new ProgressIndicator();
            //TODO Figure out what to do with the progress indicator
//            pi.setSkin(new TaxonomyProgressIndicatorSkin(pi));
            pi.setPrefSize(16, 16);
            pi.setProgress(-1);
            setProgressIndicator(pi);

            // Gather the children
            ArrayList<SctTreeItem> childrenToProcess = new ArrayList<>();

            for (RelationshipChronicleDdo r : taxRef.conceptProperty().get().getDestinationRelationships()) {
                for (RelationshipVersionDdo rv : r.getVersions()) {
                    try {
                        TaxonomyReferenceWithConcept fxtrc = new TaxonomyReferenceWithConcept(rv);
                        SctTreeItem childItem = new SctTreeItem(fxtrc, displayPolicies);
                        if (childItem.shouldDisplay()) {
                            childrenToProcess.add(childItem);
                        } else {
                    		LOG.debug("item.shouldDisplay() == false: not adding " + childItem.getConceptUuid() + " as child of " + this.getConceptUuid());
                        }
                    } catch (IOException | ContradictionException ex) {
                        LOG.error(null, ex);
                    }
                }
            }

            Collections.sort(childrenToProcess);
            getChildren().addAll(childrenToProcess);

            FetchConceptsTask fetchChildrenTask = new FetchConceptsTask(childrenToProcess);

            pi.progressProperty().bind(fetchChildrenTask.progressProperty());
            FutureHelper.addFuture(childFetcherService.submit(fetchChildrenTask));
        }
    }

    public void addChildrenConceptsAndGrandchildrenItems(ProgressIndicator p1) {
        ArrayList<SctTreeItem> grandChildrenToProcess = new ArrayList<>();

        if (! shouldDisplay()) {
        	// Don't add children to something that shouldn't be displayed
        	LOG.debug("this.shouldDisplay() == false: not adding children concepts and grandchildren items to " + this.getConceptUuid());
        } else {
        	for (TreeItem<TaxonomyReferenceWithConcept> child : getChildren()) {
        		SctTreeItem tempChild = new SctTreeItem(child.getValue(), this.getDisplayPolicies());
        		if (tempChild.shouldDisplay()) {
        			if (child.getChildren().isEmpty() && (child.getValue().getConcept() != null)) {
        				if (child.getValue().getConcept().getDestinationRelationships().isEmpty()) {
        					TaxonomyReferenceWithConcept value = child.getValue();
        					child.setValue(null);
        					SctTreeItem noChildItem = (SctTreeItem) child;
        					noChildItem.computeGraphic();
        					noChildItem.setValue(value);
        				} else {
        					ArrayList<SctTreeItem> grandChildrenToAdd = new ArrayList<>();

        					for (RelationshipChronicleDdo r :
        						child.getValue().conceptProperty().get().getDestinationRelationships()) {
        						for (RelationshipVersionDdo rv : r.getVersions()) {
        							try {
        								TaxonomyReferenceWithConcept taxRef = new TaxonomyReferenceWithConcept(rv);
        								SctTreeItem grandChildItem = new SctTreeItem(taxRef, displayPolicies);

        								if (grandChildItem.shouldDisplay()) {
        									grandChildrenToProcess.add(grandChildItem);
        									grandChildrenToAdd.add(grandChildItem);
        								} else {
        									LOG.debug("grandChildItem.shouldDisplay() == false: not adding " + grandChildItem.getConceptUuid() + " as child of " + tempChild.getConceptUuid());
        								}
        							} catch (IOException | ContradictionException ex) {
        								LOG.error(null, ex);
        							}
        						}
        					}

        					Collections.sort(grandChildrenToAdd);
        					child.getChildren().addAll(grandChildrenToAdd);
        				}
        			} else if (child.getValue().getConcept() == null) {
        				grandChildrenToProcess.add((SctTreeItem) child);
        			}
        		} else {
					LOG.debug("childItem.shouldDisplay() == false: not adding " + tempChild.getConceptUuid() + " as child of " + this.getConceptUuid());
        		}
        	}

        	FetchConceptsTask fetchChildrenTask = new FetchConceptsTask(grandChildrenToProcess);

        	p1.progressProperty().bind(fetchChildrenTask.progressProperty());
        	FutureHelper.addFuture(childFetcherService.submit(fetchChildrenTask));
        }
    }

    @Override
    public int compareTo(SctTreeItem o) {
        return this.toString().compareTo(o.toString());
    }

    public UUID getConceptUuid() {
    	TaxonomyReferenceWithConcept ref = getValue();
    	
    	if (ref != null && ref.getConceptFromRelationshipOrConceptProperties() != null) {
    		return getValue().getConceptFromRelationshipOrConceptProperties().getUuid();
    	} else {
    		return null;
    	}
    }
    public Integer getConceptNid() {
    	TaxonomyReferenceWithConcept ref = getValue();
    	
    	if (ref != null && ref.getConceptFromRelationshipOrConceptProperties() != null) {
    		return getValue().getConceptFromRelationshipOrConceptProperties().getNid();
    	} else {
    		return null;
    	}
    }
    
    public boolean isRoot() {
        TaxonomyReferenceWithConcept ref = getValue();

    	if (ref != null && ref.getRelationshipVersion() == null) {
            return true;
        } else if (ref != null && ref.getConcept() != null && ref.getConcept().getOriginRelationships().isEmpty()) {
            return true;
        } else {
        	return false;
        }
    }
    
    public Node computeGraphic() {
        return displayPolicies.computeGraphic(this);
    }
    
    public boolean shouldDisplay() {
    	return displayPolicies.shouldDisplay(this);
    }

    public void removeGrandchildren() {
        for (TreeItem<TaxonomyReferenceWithConcept> child : getChildren()) {
            child.getChildren().clear();
        }
    }

    /**
     * Initialize the {@link #childFetcherService} and {@link #conceptFetcherService}.
     */
    private static void initExecutorPools() {
        childFetcherService = Executors.newFixedThreadPool(Math.min(6,
                Runtime.getRuntime().availableProcessors() + 1), new NamedThreadFactory(sctTreeItemThreadGroup,
                "SctTreeItem child fetcher"));
        conceptFetcherService = Executors.newFixedThreadPool(Math.min(6,
                Runtime.getRuntime().availableProcessors() + 1), new NamedThreadFactory(sctTreeItemThreadGroup,
                "SctTreeItem concept fetcher"));
    }

    /**
     * Stop the {@link #childFetcherService} and {@link #conceptFetcherService}.
     */
    @Override
    public void shutdown() {
        childFetcherService.shutdown();
        conceptFetcherService.shutdown();
    }

    @Override
    public String toString() {
        if (getValue().getRelationshipVersion() != null) {
            if (multiParentDepth > 0) {
                ComponentReference destRef = getValue().getRelationshipVersion().getDestinationReference();
                String temp = WBUtility.getDescription(destRef.getUuid());
                if (temp == null) {
                    return destRef.getText();
                } else {
                    return temp;
                }
            } else {
                ComponentReference originRef = getValue().getRelationshipVersion().getOriginReference();
                String temp = WBUtility.getDescription(originRef.getUuid());
                if (temp == null) {
                    return originRef.getText();
                } else {
                    return temp;
                }
            }
        }

        if (getValue().conceptProperty().get() != null) {
            return WBUtility.getDescription(getValue().conceptProperty().get());
        }

        return "root";
    }

    public List<SctTreeItem> getExtraParents() {
        return extraParents;
    }

    public int getMultiParentDepth() {
        return multiParentDepth;
    }

    public ProgressIndicator getProgressIndicator() {
        return progressIndicator;
    }

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

    public boolean isMultiParent() {
        return multiParent;
    }

    public boolean isSecondaryParentOpened() {
        return secondaryParentOpened;
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

    public void setProgressIndicator(ProgressIndicator pi) {
        synchronized (childFetcherService) {
            this.progressIndicator = pi;
            childFetcherService.notifyAll();
        }
    }

    public void setSecondaryParentOpened(boolean secondaryParentOpened) {
        this.secondaryParentOpened = secondaryParentOpened;
    }


    public void blockUntilChildrenReady() {
        if (progressIndicator != null) {
            synchronized (childFetcherService) {
                while (progressIndicator != null) {
                    try {
                        childFetcherService.wait();
                    } catch (InterruptedException e) {
                        // noop
                    }
                }
            }
        }
    }

    /**
     * A concrete {@link Task} to fetch concepts.
     */
    private final class FetchConceptsTask extends Task<Boolean> {

        List<SctTreeItem> children;

        public FetchConceptsTask(List<SctTreeItem> children) {
            this.children = children;
        }

        @Override
        protected Boolean call() throws Exception {
            int size = children.size() - 1;
            int processedCount = 0;
            List<Future<Boolean>> futureList = new ArrayList<>();

            for (SctTreeItem childItem : children) {
                if (SctTreeView.shutdownRequested) {
                    return false;
                }
                if (childItem.getValue().conceptProperty().get() == null) {
                    GetSctTreeItemConceptCallable fetcher = new GetSctTreeItemConceptCallable(childItem);
                    futureList.add(conceptFetcherService.submit(fetcher));
                } else {
                    updateProgress(Math.min(processedCount++, size), size);
                }
            }

            updateProgress(Math.min(processedCount, size), size);

            for (Future<Boolean> future : futureList) {
                try {
                    future.get();
                    updateProgress(Math.min(processedCount++, size), size);
                } catch (ExecutionException ex) {
                    if (SctTreeView.shutdownRequested) {
                        return false;
                    } else {
                        LOG.error(null, ex);
                    }
                } catch (InterruptedException ex) {
                    LOG.error(null, ex);
                }
            }

            if (SctTreeView.shutdownRequested) {
                return false;
            }

            updateProgress(1, 1);

            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    TaxonomyReferenceWithConcept item = SctTreeItem.this.getValue();

                    SctTreeItem.this.setValue(null);
                    setProgressIndicator(null);
                    SctTreeItem.this.setValue(item);
                }
            });

            return true;
        }
    }
}
