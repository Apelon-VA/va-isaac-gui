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
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gov.va.isaac.gui.refexViews.refexEdit;

import gov.va.isaac.AppContext;
import gov.va.isaac.ExtendedAppContext;
import gov.va.isaac.gui.SimpleDisplayConcept;
import gov.va.isaac.gui.dialog.YesNoDialog;
import gov.va.isaac.gui.refexViews.refexEdit.HeaderNode.Filter;
import gov.va.isaac.gui.util.Images;
import gov.va.isaac.gui.util.TableHeaderRowTooltipInstaller;
import gov.va.isaac.interfaces.gui.constants.SharedServiceNames;
import gov.va.isaac.interfaces.gui.views.PopupViewI;
import gov.va.isaac.interfaces.gui.views.commonFunctionality.SememeViewI;
import gov.va.isaac.interfaces.utility.DialogResponse;
import gov.va.isaac.util.UpdateableBooleanBinding;
import gov.va.isaac.util.Utility;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.State;
import gov.vha.isaac.ochre.api.component.sememe.SememeChronology;
import gov.vha.isaac.ochre.api.component.sememe.SememeType;
import gov.vha.isaac.ochre.api.component.sememe.version.DynamicSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.MutableDynamicSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.SememeVersion;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeColumnInfo;
import gov.vha.isaac.ochre.api.index.IndexedGenerationCallable;
import gov.vha.isaac.ochre.impl.sememe.DynamicSememeUsageDescription;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import javafx.application.Platform;
import javafx.beans.binding.FloatBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javax.inject.Named;
import org.apache.lucene.queryparser.classic.ParseException;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sun.javafx.collections.ObservableMapWrapper;
import com.sun.javafx.tk.Toolkit;


/**
 * 
 * DynamicSememeView
 * 
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */

@Service
@Named (value=SharedServiceNames.SEMEME_VIEW)
@PerLookup
public class SememeView implements SememeViewI
{
	private VBox rootNode_ = null;
	private TreeTableView<SememeGUI> ttv_;
	private TreeItem<SememeGUI> treeRoot_;
	private Button retireButton_, addButton_, commitButton_, cancelButton_, editButton_, viewUsageButton_;
	private Label summary_ = new Label("");
	private ToggleButton stampButton_, activeOnlyButton_, historyButton_;
	private Button displayFSNButton_;
	private UpdateableBooleanBinding currentRowSelected_, selectedRowIsActive_;
	private UpdateableBooleanBinding showStampColumns_, showActiveOnly_, showFullHistory_, showViewUsageButton_;
	private TreeTableColumn<SememeGUI, String> stampColumn_;
	private BooleanProperty hasUncommitted_ = new SimpleBooleanProperty(false);

	private Text placeholderText = new Text("No Dynamic Sememes were found associated with the component");
	private ProgressBar progressBar_;
	
	private Button clearColumnHeaderNodesButton_ = new Button("Clear Filters");
	
	private Logger logger_ = LoggerFactory.getLogger(this.getClass());

	ViewFocus viewFocus_;
	int viewFocusNid_;

	IndexedGenerationCallable newComponentIndexGen_ = null; //Useful when adding from the assemblage perspective - if they are using an index, we need to wait till the new thing is indexed
	private AtomicInteger noRefresh_ = new AtomicInteger(0);
	
	// Display refreshes on change of UserProfileBindings.getViewCoordinatePath() or UserProfileBindings.getDisplayFSN()
	private UpdateableBooleanBinding refreshRequiredListenerHack;

	private final ObservableMap<ColumnId, Filter<?>> filterCache_ = new ObservableMapWrapper<>(new WeakHashMap<>());
	
	BooleanProperty displayFSN_ = new SimpleBooleanProperty(ExtendedAppContext.getUserProfileBindings().getLanguageCoordinate().get().isFSNPreferred());
	
	// Display refreshes on change of UserProfileBindings.getDisplayFSN().
	// If UserProfileBindings.getDisplayFSN() at time of refresh has changed since last refresh
	// then all filters must be cleared, because they may contain outdated display text values
	private boolean filterCacheLastBuildDisplayFSNValue_ = displayFSN_.get();
	
	private final MapChangeListener<ColumnId, Filter<?>> filterCacheListener_ = new MapChangeListener<ColumnId, Filter<?>>() {
		@Override
		public void onChanged(
				javafx.collections.MapChangeListener.Change<? extends ColumnId, ? extends Filter<?>> c) {
			if (c.wasAdded() || c.wasRemoved()) {
				refresh();
			}
		}
	};
	private final ListChangeListener<Object> filterListener_ = new ListChangeListener<Object>() {
		@Override
		public void onChanged(
				javafx.collections.ListChangeListener.Change<? extends Object> c) {
			while (c.next()) {
				if (c.wasPermutated()) {
					// irrelevant
				} else if (c.wasUpdated()) {
					// irrelevant
				} else {
					refresh();
					break;
				}
			}
		}
	};
	private void addFilterCacheListeners() {
		removeFilterCacheListeners();
		filterCache_.addListener(filterCacheListener_);
		for (HeaderNode.Filter<?> filter : filterCache_.values()) {
			filter.getFilterValues().addListener(filterListener_);
		}
	}
	private void removeFilterCacheListeners() {
		filterCache_.removeListener(filterCacheListener_);
		for (HeaderNode.Filter<?> filter : filterCache_.values()) {
			filter.getFilterValues().removeListener(filterListener_);
		}
	}

	private SememeView() 
	{
		//Created by HK2 - no op - delay till getView called
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void initialInit()
	{
		if (rootNode_ == null)
		{
			noRefresh_.getAndIncrement();
			ttv_ = new TreeTableView<>();
			ttv_.setTableMenuButtonVisible(true);
			ttv_.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
	
			treeRoot_ = new TreeItem<>();
			treeRoot_.setExpanded(true);
			ttv_.setShowRoot(false);
			ttv_.setRoot(treeRoot_);
			progressBar_ = new ProgressBar(-1);
			progressBar_.setPrefWidth(200);
			progressBar_.setPadding(new Insets(15, 15, 15, 15));
			ttv_.setPlaceholder(progressBar_);
			
			rootNode_ = new VBox();
			rootNode_.setFillWidth(true);
			rootNode_.getChildren().add(ttv_);
			VBox.setVgrow(ttv_, Priority.ALWAYS);
			
			ToolBar t = new ToolBar();
			
			clearColumnHeaderNodesButton_.setOnAction(event -> {
				removeFilterCacheListeners();
				for (HeaderNode.Filter<?> filter : filterCache_.values()) {
					filter.getFilterValues().clear();
				}
				refresh();
			});
			t.getItems().add(clearColumnHeaderNodesButton_);
			
			currentRowSelected_ = new UpdateableBooleanBinding()
			{
				{
					addBinding(ttv_.getSelectionModel().getSelectedItems());
				}
				@Override
				protected boolean computeValue()
				{
					if (ttv_.getSelectionModel().getSelectedItems().size() > 0 && 
							ttv_.getSelectionModel().getSelectedItem() != null && 
							ttv_.getSelectionModel().getSelectedItem().getValue() != null)
					{
						return ttv_.getSelectionModel().getSelectedItem().getValue().isCurrent();
					}
					else
					{
						return false;
					}
				}
			};
			
			selectedRowIsActive_ = new UpdateableBooleanBinding()
			{
				{
					addBinding(ttv_.getSelectionModel().getSelectedItems());
				}
				@Override
				protected boolean computeValue()
				{
					if (ttv_.getSelectionModel().getSelectedItems().size() > 0 && ttv_.getSelectionModel().getSelectedItem() != null 
							&& ttv_.getSelectionModel().getSelectedItem().getValue() != null)
					{
						return ttv_.getSelectionModel().getSelectedItem().getValue().getSememe().getState() == State.ACTIVE;
					}
					else
					{
						return false;
					}
				}
			};
			
			retireButton_ = new Button(null, Images.MINUS.createImageView());
			retireButton_.setTooltip(new Tooltip("Retire Selected Sememe Extension(s)"));
			retireButton_.disableProperty().bind(selectedRowIsActive_.and(currentRowSelected_).not());
			retireButton_.setOnAction((action) ->
			{
				try
				{
					YesNoDialog dialog = new YesNoDialog(rootNode_.getScene().getWindow());
					DialogResponse dr = dialog.showYesNoDialog("Retire?", "Do you want to retire the selected sememe entries?");
					if (DialogResponse.YES == dr)
					{
						ObservableList<TreeItem<SememeGUI>> selected = ttv_.getSelectionModel().getSelectedItems();
						if (selected != null && selected.size() > 0)
						{
							for (TreeItem<SememeGUI> refexTreeItem : selected)
							{
								SememeGUI refex = refexTreeItem.getValue();
								if (refex.getSememe().getState() == State.INACTIVE)
								{
									continue;
								}
								if (refex.getSememe().getChronology().getSememeType() == SememeType.DYNAMIC)
								{
									MutableDynamicSememe<?> mds = ((SememeChronology<DynamicSememe>)refex.getSememe().getChronology())
											.createMutableVersion(MutableDynamicSememe.class, State.INACTIVE, ExtendedAppContext.getUserProfileBindings().getEditCoordinate().get());
									mds.setData(((DynamicSememe<?>)refex.getSememe()).getData());
									Get.commitService().addUncommitted(refex.getSememe().getChronology());
									Get.commitService().commit("retire dynamic sememe").get();
								}
								else
								{
									//TODO
									throw new RuntimeException("Not yet supported");
								}
							}
							refresh();
						}
					}
				}
				catch (Exception e)
				{
					logger_.error("Unexpected error retiring sememe", e);
					AppContext.getCommonDialogs().showErrorDialog("Error", "There was an unexpected error retiring the sememe", e.getMessage(), rootNode_.getScene().getWindow());
				}
			});
			
			t.getItems().add(retireButton_);
			
			addButton_ = new Button(null, Images.PLUS.createImageView());
			addButton_.setTooltip(new Tooltip("Add a new Sememe Extension"));
			addButton_.setOnAction((action) ->
			{
				AddSememePopup arp = AppContext.getService(AddSememePopup.class);
				arp.finishInit(viewFocus_, viewFocusNid_, this);
				arp.showView(rootNode_.getScene().getWindow());
			});
			
			addButton_.setDisable(true);
			t.getItems().add(addButton_);
			
			editButton_ = new Button(null, Images.EDIT.createImageView());
			editButton_.setTooltip(new Tooltip("Edit a Sememe"));
			editButton_.disableProperty().bind(currentRowSelected_.not());
			editButton_.setOnAction((action) ->
			{
				AddSememePopup arp = AppContext.getService(AddSememePopup.class);
				arp.finishInit(ttv_.getSelectionModel().getSelectedItem().getValue(), this);
				arp.showView(rootNode_.getScene().getWindow());
			});
			t.getItems().add(editButton_);
			
			viewUsageButton_ = new Button(null, Images.SEARCH.createImageView());
			viewUsageButton_.setTooltip(new Tooltip("The displayed concept also defines a dynamic sememe itself.  Click to see the usage of this sememe."));
			viewUsageButton_.setOnAction((action) ->
			{
				try
				{
					SememeViewI driv = AppContext.getService(SememeViewI.class);
					driv.setAssemblage(viewFocusNid_, null, null, null, true);
					driv.showView(null);
				}
				catch (Exception e)
				{
					logger_.error("Error launching sememe dynamic member viewer", e);
					AppContext.getCommonDialogs().showErrorDialog("Error", "There was an unexpected launching the sememe member viewer", e.getMessage(), 
							(rootNode_.getScene() == null ? null : rootNode_.getScene().getWindow()));
				}
			});
			showViewUsageButton_ = new UpdateableBooleanBinding()
			{
				{
					setComputeOnInvalidate(true);
				}
				@Override
				protected boolean computeValue()
				{
					boolean show = false;
					if (viewFocus_ != null && viewFocus_ == ViewFocus.REFERENCED_COMPONENT && Get.conceptService().getOptionalConcept(viewFocusNid_).isPresent())
					{
						//Need to find out if this component has a the dynamic sememe definition annotation on it.
						try
						{
							DynamicSememeUsageDescription.read(viewFocusNid_);
							show = true;
						}
						catch (Exception e)
						{
							//noop - this concept simply isn't configured as a dynamic sememe concept.
						}
					}
					return show;
				}
			};
			viewUsageButton_.visibleProperty().bind(showViewUsageButton_);
			t.getItems().add(viewUsageButton_);
			
			t.getItems().add(summary_);
			
			//fill to right
			Region r = new Region();
			HBox.setHgrow(r, Priority.ALWAYS);
			t.getItems().add(r);
			
			stampButton_ = new ToggleButton("");
			stampButton_.setGraphic(Images.STAMP.createImageView());
			stampButton_.setTooltip(new Tooltip("Show / Hide Stamp Attributes"));
			stampButton_.setVisible(false);
			stampButton_.setSelected(true);
			t.getItems().add(stampButton_);
			
			showStampColumns_ = new UpdateableBooleanBinding()
			{
				{
					setComputeOnInvalidate(true);
				}
				@Override
				protected boolean computeValue()
				{
					boolean visible = false;
					if (listeningTo.size() > 0)
					{
						visible = ((ReadOnlyBooleanProperty)listeningTo.iterator().next()).get();
					}
					if (stampColumn_ != null)
					{
						stampColumn_.setVisible(visible);
						for (TreeTableColumn<SememeGUI, ?> nested : stampColumn_.getColumns())
						{
							nested.setVisible(visible);
						}
					}
					return visible;
				}
			};
			
			activeOnlyButton_ = new ToggleButton("");
			activeOnlyButton_.setGraphic(Images.FILTER_16.createImageView());
			activeOnlyButton_.setTooltip(new Tooltip("Show Active Only / Show All"));
			activeOnlyButton_.setVisible(false);
			activeOnlyButton_.setSelected(true);
			t.getItems().add(activeOnlyButton_);
			
			showActiveOnly_ = new UpdateableBooleanBinding()
			{
				{
					setComputeOnInvalidate(true);
				}
				@Override
				protected boolean computeValue()
				{
					boolean showActive = false;
					if (listeningTo.size() > 0)
					{
						showActive = ((ReadOnlyBooleanProperty)listeningTo.iterator().next()).get();
					}
					refresh();
					return showActive;
				}
			};
			
			historyButton_ = new ToggleButton("");
			historyButton_.setGraphic(Images.HISTORY.createImageView());
			historyButton_.setTooltip(new Tooltip("Show Current Only / Show Full History"));
			historyButton_.setVisible(false);
			t.getItems().add(historyButton_);
			
			showFullHistory_ = new UpdateableBooleanBinding()
			{
				{
					setComputeOnInvalidate(true);
				}
				@Override
				protected boolean computeValue()
				{
					boolean showFullHistory = false;
					if (listeningTo.size() > 0)
					{
						showFullHistory = ((ReadOnlyBooleanProperty)listeningTo.iterator().next()).get();
					}
					refresh();
					return showFullHistory;
				}
			};
			
			ExtendedAppContext.getUserProfileBindings().getLanguageCoordinate().addListener(change ->
			{
				displayFSN_.set(ExtendedAppContext.getUserProfileBindings().getLanguageCoordinate().get().isFSNPreferred());
			});
			
			displayFSNButton_ = new Button("");
			ImageView displayFsn = Images.DISPLAY_FSN.createImageView();
			Tooltip.install(displayFsn, new Tooltip("Displaying the Fully Specified Name - click to display the Preferred Term"));
			displayFsn.visibleProperty().bind(displayFSN_);
			ImageView displayPreferred = Images.DISPLAY_PREFERRED.createImageView();
			displayPreferred.visibleProperty().bind(displayFSN_.not());
			Tooltip.install(displayPreferred, new Tooltip("Displaying the Preferred Term - click to display the Fully Specified Name"));
			displayFSNButton_.setGraphic(new StackPane(displayFsn, displayPreferred));
			displayFSNButton_.prefHeightProperty().bind(historyButton_.heightProperty());
			displayFSNButton_.prefWidthProperty().bind(historyButton_.widthProperty());
			displayFSNButton_.setOnAction(new EventHandler<ActionEvent>()
			{
				@Override
				public void handle(ActionEvent event)
				{
					//TODO Dan displayFSN fixes are not yet complete
					displayFSN_.set(displayFSN_.not().get());
				}
			});
			t.getItems().add(displayFSNButton_);
			
			cancelButton_ = new Button("Cancel");
			cancelButton_.setDisable(true);
			//TODO figure out to handle cancel
			//cancelButton_.disableProperty().bind(hasUncommitted_.not());
			t.getItems().add(cancelButton_);
			cancelButton_.setOnAction((action) ->
			{
				try
				{
//					Get.commitService().cancel();
//					forgetAllUncommitted(treeRoot_.getChildren());
//					HashSet<Integer> assemblageNids = getAllAssemblageNids(treeRoot_.getChildren());
//					for (Integer i : assemblageNids)
//					{
//						ConceptVersionBI cv = ExtendedAppContext.getDataStore().getConceptVersion(OTFUtility.getViewCoordinate(), i);
//						if (!cv.isAnnotationStyleRefex() && cv.isUncommitted())
//						{
//							cv.cancel();
//						}
//					}
				}
				catch (Exception e)
				{
					logger_.error("Error cancelling", e);
					AppContext.getCommonDialogs().showErrorDialog("Error", "There was an unexpected during cancel", e.getMessage(), 
							(rootNode_.getScene() == null ? null : rootNode_.getScene().getWindow()));
				}
				refresh();
			});
			
			commitButton_ = new Button("Commit");
			commitButton_.disableProperty().bind(hasUncommitted_.not());
			t.getItems().add(commitButton_);
			
			commitButton_.setOnAction((action) ->
			{
				try
				{
					//TODO ochre commit issues
//					HashSet<Integer> componentNids = getAllComponentNids(treeRoot_.getChildren());
//					for (Integer i : componentNids)
//					{
//						ConceptChronicleBI cc = ExtendedAppContext.getDataStore().getConceptForNid(i);
//						if (cc.isUncommitted() || cc.getConceptAttributes().isUncommitted())
//						{
//							ExtendedAppContext.getDataStore().commit(/* cc */);
//						}
//					}
//					
//					HashSet<Integer> assemblageNids = getAllAssemblageNids(treeRoot_.getChildren());
//					for (Integer i : assemblageNids)
//					{
//						ConceptChronicleBI cc = ExtendedAppContext.getDataStore().getConcept(i);
//						if (!cc.isAnnotationStyleRefex() && cc.isUncommitted())
//						{
//							ExtendedAppContext.getDataStore().commit();
//						}
//					}
					Get.commitService().commit("commit of dynamic sememe").get();
				}
				catch (Exception e)
				{
					logger_.error("Error committing", e);
					AppContext.getCommonDialogs().showErrorDialog("Error", "There was an unexpected during commit", e.getMessage(), 
							(rootNode_.getScene() == null ? null : rootNode_.getScene().getWindow()));
				}
				refresh();
			});
			
			rootNode_.getChildren().add(t);
			
			Platform.runLater(new Runnable()
			{
				@Override
				public void run()
				{
					//TODO hack for bug in javafx - need to start as true, then later toggle to false.
					stampButton_.setSelected(false);
				}
			});
			
			refreshRequiredListenerHack = new UpdateableBooleanBinding()
			{
				{
					setComputeOnInvalidate(true);
					addBinding(ExtendedAppContext.getUserProfileBindings().getViewCoordinatePath(), ExtendedAppContext.getUserProfileBindings().getLanguageCoordinate());
				}

				@Override
				protected boolean computeValue()
				{
					logger_.debug("DynRefex refresh() due to change of an observed user property");
					refresh();
					return false;
				}
			};
			noRefresh_.decrementAndGet();
		}
	}

	/**
	 * @see gov.va.isaac.interfaces.gui.views.DockedViewI#getView()
	 */
	@Override
	public Region getView()
	{
		//setting up the binding stuff is causing refresh calls
		initialInit();
		return rootNode_;
	}
	
	/**
	 * @see gov.va.isaac.interfaces.gui.views.PopupViewI#showView(javafx.stage.Window)
	 */
	@Override
	public void showView(Window parent)
	{
		Stage stage = new Stage(StageStyle.DECORATED);
		stage.initModality(Modality.NONE);
		stage.initOwner(parent);
		
		BorderPane root = new BorderPane();
		
		Label title = new Label("Sememe View");
		title.getStyleClass().add("titleLabel");
		title.setAlignment(Pos.CENTER);
		title.setMaxWidth(Double.MAX_VALUE);
		title.setPadding(new Insets(5, 5, 5, 5));
		root.setTop(title);
		root.setCenter(getView());
		
		Scene scene = new Scene(root);
		stage.setScene(scene);
		stage.setTitle("Sememe View");
		stage.getScene().getStylesheets().add(SememeView.class.getResource("/isaac-shared-styles.css").toString());
		stage.setWidth(800);
		stage.setHeight(600);
		stage.onHiddenProperty().set((eventHandler) ->
		{
			stage.setScene(null);
			viewDiscarded();
		});
		stage.show();
//		drv_.setAssemblage(assemblageConcept_.getNid(), null, null, null, true);
	}

	/**
	 * @see gov.va.isaac.interfaces.gui.views.commonFunctionality.SememeViewI#setComponent(int, javafx.beans.property.ReadOnlyBooleanProperty, 
	 * javafx.beans.property.ReadOnlyBooleanProperty, javafx.beans.property.ReadOnlyBooleanProperty, boolean)
	 */
	@Override
	public void setComponent(int componentNid, ReadOnlyBooleanProperty showStampColumns, ReadOnlyBooleanProperty showActiveOnly, 
			ReadOnlyBooleanProperty showFullHistory, boolean displayFSNButton)
	{
		//disable refresh, as the bindings mucking causes many refresh calls
		noRefresh_.getAndIncrement();
		initialInit();
		viewFocus_ = ViewFocus.REFERENCED_COMPONENT;
		viewFocusNid_ = componentNid;
		handleExternalBindings(showStampColumns, showActiveOnly, showFullHistory, displayFSNButton);
		showViewUsageButton_.invalidate();
		noRefresh_.getAndDecrement();
		initColumnsLoadData();
	}

	/**
	 * @see gov.va.isaac.interfaces.gui.views.commonFunctionality.SememeViewI#setAssemblage(int, javafx.beans.property.ReadOnlyBooleanProperty, 
	 * javafx.beans.property.ReadOnlyBooleanProperty, javafx.beans.property.ReadOnlyBooleanProperty, boolean)
	 */
	@Override
	public void setAssemblage(int assemblageConceptNid, ReadOnlyBooleanProperty showStampColumns, ReadOnlyBooleanProperty showActiveOnly, 
			ReadOnlyBooleanProperty showFullHistory, boolean displayFSNButton)
	{
		//disable refresh, as the bindings mucking causes many refresh calls
		noRefresh_.getAndIncrement();
		initialInit();
		viewFocus_ = ViewFocus.ASSEMBLAGE;
		viewFocusNid_ = assemblageConceptNid;
		handleExternalBindings(showStampColumns, showActiveOnly, showFullHistory, displayFSNButton);
		noRefresh_.getAndDecrement();
		initColumnsLoadData();
	}
	
	private void handleExternalBindings(ReadOnlyBooleanProperty showStampColumns, ReadOnlyBooleanProperty showActiveOnly, 
			ReadOnlyBooleanProperty showFullHistory, boolean displayFSNButton)
	{
		showStampColumns_.clearBindings();
		showActiveOnly_.clearBindings();
		showFullHistory_.clearBindings();
		if (showStampColumns == null)
		{
			//Use our own button
			stampButton_.setVisible(true);
			showStampColumns_.addBinding(stampButton_.selectedProperty());
		}
		else
		{
			stampButton_.setVisible(false);
			showStampColumns_.addBinding(showStampColumns);
		}
		if (showActiveOnly == null)
		{
			//Use our own button
			activeOnlyButton_.setVisible(true);
			showActiveOnly_.addBinding(activeOnlyButton_.selectedProperty());
		}
		else
		{
			activeOnlyButton_.setVisible(false);
			showActiveOnly_.addBinding(showActiveOnly);
		}
		if (showFullHistory == null)
		{
			//Use our own button
			historyButton_.setVisible(true);
			showFullHistory_.addBinding(historyButton_.selectedProperty());
		}
		else
		{
			historyButton_.setVisible(false);
			showFullHistory_.addBinding(showFullHistory);
		}
		
		if (displayFSNButton)
		{
			//Use our own button
			displayFSNButton_.setVisible(true);
		}
		else
		{
			displayFSNButton_.setVisible(false);
		}
	}
	
	protected void refresh()
	{
		if (noRefresh_.get() > 0)
		{
			logger_.info("Skip refresh of dynamic sememe due to wait count {}", noRefresh_.get());
			return;
		}
		else
		{
			noRefresh_.getAndIncrement();
		}
		
		Utility.execute(() ->
		{
			try
			{
				loadRealData(); // calls addFilterCacheListeners()
			}
			catch (Exception e)
			{
				logger_.error("Unexpected error building the sememe display", e);
				//null check, as the error may happen before the scene is visible
				AppContext.getCommonDialogs().showErrorDialog("Error", "There was an unexpected error building the sememe display", e.getMessage(), 
						(rootNode_.getScene() == null ? null : rootNode_.getScene().getWindow()));
			}
			finally
			{
				noRefresh_.decrementAndGet();
			}
		});
	}
	
	private void storeTooltip(Map<String, List<String>> store, String key, String value)
	{
		List<String> list = store.get(key);
		if (list == null)
		{
			list = new ArrayList<>();
			store.put(key, list);
		}
		list.add(value);
	}

	private void initColumnsLoadData()
	{
		noRefresh_.getAndIncrement();
		Utility.execute(() -> {
			try
			{
				removeFilterCacheListeners();
				
				final ArrayList<TreeTableColumn<SememeGUI, ?>> treeColumns = new ArrayList<>();
				Map<String, List<String>> toolTipStore = new HashMap<>();
				
				TreeTableColumn<SememeGUI, SememeGUI> ttStatusCol = new TreeTableColumn<>(SememeGUIColumnType.STATUS_CONDENSED.toString());
				storeTooltip(toolTipStore, ttStatusCol.getText(), "Status Markers - for active / inactive and current / historical and uncommitted");

				ttStatusCol.setSortable(true);
				ttStatusCol.setResizable(true);
				ttStatusCol.setComparator(new Comparator<SememeGUI>()
				{
					@Override
					public int compare(SememeGUI o1, SememeGUI o2)
					{
						return o1.compareTo(SememeGUIColumnType.STATUS_CONDENSED, null, o2);
					}
				});
				ttStatusCol.setCellFactory((colInfo) -> 
				{
					return new StatusCell();
				});
				ttStatusCol.setCellValueFactory((callback) ->
				{
					return new ReadOnlyObjectWrapper<SememeGUI>(callback.getValue().getValue());
				});
				treeColumns.add(ttStatusCol);
				
				//Create columns for basic info
				if (viewFocus_ == ViewFocus.ASSEMBLAGE)
				{
					//the assemblage is always the same - don't show.
					TreeTableColumn<SememeGUI, SememeGUI>  ttCol = buildComponentCellColumn(SememeGUIColumnType.COMPONENT);
					storeTooltip(toolTipStore, ttCol.getText(), "The Referenced component of this Sememe");
					HeaderNode<String> headerNode = new HeaderNode<String>(
							filterCache_,
							ttCol,
							ColumnId.getInstance(SememeGUIColumnType.COMPONENT),
							rootNode_.getScene(),
							new HeaderNode.DataProvider<String>() {
						@Override
						public String getData(SememeGUI source) {
							return source.getDisplayStrings(SememeGUIColumnType.COMPONENT, null).getKey();
						}
					});
					ttCol.setGraphic(headerNode.getNode());
					
					treeColumns.add(ttCol);
				}
				else
				{
					//the component is always the same - don't show.
					TreeTableColumn<SememeGUI, SememeGUI>  ttCol = buildComponentCellColumn(SememeGUIColumnType.ASSEMBLAGE);
					storeTooltip(toolTipStore, ttCol.getText(), "The Assemblage concept that defines this Sememe");
					HeaderNode<String> headerNode = new HeaderNode<>(
							filterCache_,
							ttCol,
							ColumnId.getInstance(SememeGUIColumnType.ASSEMBLAGE),
							rootNode_.getScene(),
							new HeaderNode.DataProvider<String>() {
						@Override
						public String getData(SememeGUI source) {
							return source.getDisplayStrings(SememeGUIColumnType.ASSEMBLAGE, null).getKey();
						}
					});
					ttCol.setGraphic(headerNode.getNode());

					treeColumns.add(ttCol);
				}
				
				TreeTableColumn<SememeGUI, String> ttStringCol = new TreeTableColumn<>();
				ttStringCol = new TreeTableColumn<>();
				ttStringCol.setText(SememeGUIColumnType.ATTACHED_DATA.toString());
				storeTooltip(toolTipStore, ttStringCol.getText(), "The various data fields attached to this Sememe instance");
				ttStringCol.setSortable(false);
				ttStringCol.setResizable(true);
				//don't add yet - we might not need this column.  throw away later, if we don't need it
				
				/**
				 * The key of the first hashtable is the column description concept, while the key of the second hashtable is the assemblage concept
				 * Since the same column could be used in multiple assemblages - need to keep those separate, even though the rest of the column details 
				 * will be the same.  The List in the third level is for cases where a single assemblage concept re-uses the same column description 
				 * details multiple times.
				*/
				Hashtable<UUID, Hashtable<UUID, List<DynamicSememeColumnInfo>>> uniqueColumns;
				
				if (Get.identifiedObjectService().getIdentifiedObjectChronology(viewFocusNid_).get().isUncommitted())
				{
					hasUncommitted_.set(true);
				}
				else
				{
					hasUncommitted_.set(false);
				}
				
				if (viewFocus_ == ViewFocus.REFERENCED_COMPONENT)
				{
					uniqueColumns = getUniqueColumns(viewFocusNid_);
				}
				else
				{
					//This case is easy - as there is only one assemblage.  The 3 level mapping stuff is way overkill for this case... but don't
					//want to rework it at this point... might come back and cleanup this mess later.
					uniqueColumns = new Hashtable<>();
					
					DynamicSememeUsageDescription rdud;
					//Normally, we can read the info necessary from the assemblage - but in the case where we are displaying a non-dynamic sememe
					//I need to read an instance of the sememe, to find out what type it is (and then assume, that it is only used as that type)
					//yes, dangerous, bad code... the static sememes need work....
					
					try
					{
						rdud = DynamicSememeUsageDescription.read(viewFocusNid_);
					}
					catch (Exception e)
					{
						//Its either a mis-configured dynamic sememe, or its a static sememe.  Check and see...
						Optional<SememeChronology<? extends SememeVersion<?>>> sc = Get.sememeService().getSememesFromAssemblage(Get.identifierService()
								.getConceptSequence(viewFocusNid_)).findAny();
						if (sc.isPresent())
						{
							rdud = DynamicSememeUsageDescription.mockOrRead(sc.get());
						}
						else
						{
							//TODO need to figure out how to handle the case where the thing they click on isn't used as an assemblage, and isn't a dynamic assemblage
							throw new RuntimeException("Keyword", e);  //HACK alert (look in the catch)
						}
					}
					for (DynamicSememeColumnInfo col : rdud.getColumnInfo())
					{
						Hashtable<UUID, List<DynamicSememeColumnInfo>> nested = uniqueColumns.get(col.getColumnDescriptionConcept());
						if (nested == null)
						{
							nested = new Hashtable<>();
							uniqueColumns.put(col.getColumnDescriptionConcept(), nested);
						}
						
						UUID assemblyUUID = Get.identifierService().getUuidPrimordialFromConceptSequence(rdud.getDynamicSememeUsageDescriptorSequence()).get();
						List<DynamicSememeColumnInfo> doubleNested = nested.get(assemblyUUID);
						if (doubleNested == null)
						{
							doubleNested = new ArrayList<>();
							nested.put(assemblyUUID, doubleNested);
						}
						doubleNested.add(col);
					}
				}
				
				ArrayList<Hashtable<UUID, List<DynamicSememeColumnInfo>>> sortedUniqueColumns = new ArrayList<>();
				sortedUniqueColumns.addAll(uniqueColumns.values());
				Collections.sort(sortedUniqueColumns, new Comparator<Hashtable<UUID, List<DynamicSememeColumnInfo>>>()
					{
						@Override
						public int compare(Hashtable<UUID, List<DynamicSememeColumnInfo>> o1, Hashtable<UUID, List<DynamicSememeColumnInfo>> o2)
						{
							return Integer.compare(o1.values().iterator().next().get(0).getColumnOrder(),o2.values().iterator().next().get(0).getColumnOrder()); 
						}
					});
				
				//Create columns for every different type of data column we see in use
				for (Hashtable<UUID, List<DynamicSememeColumnInfo>> col : sortedUniqueColumns)
				{
					int max = 0;
					for (List<DynamicSememeColumnInfo> item : col.values())
					{
						if (item.size() > max)
						{
							max = item.size();
						}
					}
					
					for (int i = 0; i < max; i++)
					{
						final String name = col.values().iterator().next().get(0).getColumnName(); //all the same, just pick the first
						TreeTableColumn<SememeGUI, SememeGUI> nestedCol = new TreeTableColumn<>(name);
						storeTooltip(toolTipStore, nestedCol.getText(), col.values().iterator().next().get(0).getColumnDescription());
						
						// FILTER ID
						final ColumnId columnKey = ColumnId.getInstance(col.values().iterator().next().get(0).getColumnDescriptionConcept(), i);

						final Integer listItem = i;
						HeaderNode<String> ttNestedColHeaderNode = new HeaderNode<>(
								filterCache_,
								nestedCol,
								columnKey,
								rootNode_.getScene(),
								new HeaderNode.DataProvider<String>() {
							@Override
							public String getData(SememeGUI source) {
								if (source == null) {
									return null;
								}
								try
								{
									for (UUID uuid : col.keySet())
									{
										assert source != null;
										assert source.getSememe() != null;
										
										if (Get.identifierService().getConceptSequenceForUuids(uuid) == source.getSememe().getAssemblageSequence())
										{
											List<DynamicSememeColumnInfo> colInfo =  col.get(uuid);
											Integer refexColumnOrder = (colInfo.size() > listItem ? 
													(SememeGUI.getData(source.getSememe()).length <= colInfo.get(listItem).getColumnOrder() ? null 
														: colInfo.get(listItem).getColumnOrder()): null);
											
											if (refexColumnOrder != null)
											{
												return source.getDisplayStrings(SememeGUIColumnType.ATTACHED_DATA, refexColumnOrder).getKey();
											}
										}
									}
								}
								catch (Exception e)
								{
									logger_.error("Unexpected error getting string data from attribute", e);
								}
								return null;  //not applicable / blank row
							}
						});
						nestedCol.setGraphic(ttNestedColHeaderNode.getNode());
						
						nestedCol.setSortable(true);
						nestedCol.setResizable(true);
						nestedCol.setComparator(new Comparator<SememeGUI>()
						{
							@Override
							public int compare(SememeGUI o1, SememeGUI o2)
							{
								try
								{
									for (UUID uuid : col.keySet())
									{
										assert o1 != null;
										assert o1.getSememe() != null;
										
										if (Get.identifierService().getConceptSequenceForUuids(uuid) == o1.getSememe().getAssemblageSequence())
										{
											List<DynamicSememeColumnInfo> colInfo =  col.get(uuid);
											Integer refexColumnOrder = (colInfo.size() > listItem ? 
													(SememeGUI.getData(o1.getSememe()).length <= colInfo.get(listItem).getColumnOrder() ? null 
														: colInfo.get(listItem).getColumnOrder()): null);
											
											if (refexColumnOrder != null)
											{
												return o1.compareTo(SememeGUIColumnType.ATTACHED_DATA, refexColumnOrder, o2);
											}
										}
									}
								}
								catch (Exception e)
								{
									logger_.error("Unexpected error sorting data attributes", e);
								}
								return 1;  //not applicable / blank row
								
							}
						});
						
						nestedCol.setCellFactory(new AttachedDataCellFactory(col, i));
						
						nestedCol.setCellValueFactory((callback) ->
						{
							return new ReadOnlyObjectWrapper<>(callback.getValue().getValue());
						}); 
						
						ttStringCol.getColumns().add(nestedCol);
					}
				}
				
				//Only add attached data column if necessary
				if (ttStringCol.getColumns().size() > 0)
				{
					treeColumns.add(ttStringCol);
				}
				
				//Create the STAMP columns
				ttStringCol = new TreeTableColumn<>();
				ttStringCol.setText("STAMP");
				storeTooltip(toolTipStore, "STAMP", "The Status, Time, Author, Module and Path columns");
				ttStringCol.setSortable(false);
				ttStringCol.setResizable(true);
				stampColumn_ = ttStringCol;
				treeColumns.add(ttStringCol);
				
				TreeTableColumn<SememeGUI, String> nestedCol = new TreeTableColumn<>();
				nestedCol.setText(SememeGUIColumnType.STATUS_STRING.toString());
				storeTooltip(toolTipStore, nestedCol.getText(), "The status of the instance");
				HeaderNode<String> nestedColHeaderNode = new HeaderNode<>(
						filterCache_,
						nestedCol,
						ColumnId.getInstance(SememeGUIColumnType.STATUS_STRING),
						rootNode_.getScene(),
						new HeaderNode.DataProvider<String>() {
							@Override
							public String getData(SememeGUI source) {
								return source.getDisplayStrings(SememeGUIColumnType.STATUS_STRING, null).getKey();
							}
						});
				nestedCol.setGraphic(nestedColHeaderNode.getNode());
				nestedCol.setSortable(true);
				nestedCol.setResizable(true);
				nestedCol.setCellValueFactory((callback) ->
				{
					return new ReadOnlyStringWrapper(callback.getValue().getValue().getDisplayStrings(SememeGUIColumnType.STATUS_STRING, null).getKey());
				});
				ttStringCol.getColumns().add(nestedCol);
				

				nestedCol = new TreeTableColumn<>();
				nestedCol.setText(SememeGUIColumnType.TIME.toString());
				storeTooltip(toolTipStore, nestedCol.getText(), "The time when the instance was created or edited");
				nestedColHeaderNode = new HeaderNode<>(
						filterCache_,
						nestedCol,
						ColumnId.getInstance(SememeGUIColumnType.TIME),
						rootNode_.getScene(),
						new HeaderNode.DataProvider<String>() {
							@Override
							public String getData(SememeGUI source) {
								return source.getDisplayStrings(SememeGUIColumnType.TIME, null).getKey();
							}
						});
				nestedCol.setGraphic(nestedColHeaderNode.getNode());
				nestedCol.setSortable(true);
				nestedCol.setResizable(true);
				nestedCol.setCellValueFactory((callback) ->
				{
					return new ReadOnlyStringWrapper(callback.getValue().getValue().getDisplayStrings(SememeGUIColumnType.TIME, null).getKey());
				});
				ttStringCol.getColumns().add(nestedCol);
				
				TreeTableColumn<SememeGUI, SememeGUI> nestedIntCol = buildComponentCellColumn(SememeGUIColumnType.AUTHOR); 
				storeTooltip(toolTipStore, nestedIntCol.getText(), "The author of the instance");
				ttStringCol.getColumns().add(nestedIntCol);
				
				nestedIntCol = buildComponentCellColumn(SememeGUIColumnType.MODULE);
				storeTooltip(toolTipStore, nestedIntCol.getText(), "The module of the instance");
				ttStringCol.getColumns().add(nestedIntCol);
				
				nestedIntCol = buildComponentCellColumn(SememeGUIColumnType.PATH); 
				storeTooltip(toolTipStore, nestedIntCol.getText(), "The path of the instance");
				ttStringCol.getColumns().add(nestedIntCol);

				Platform.runLater(() ->
				{
					for (TreeTableColumn<SememeGUI, ?> tc : treeColumns)
					{
						ttv_.getColumns().add(tc);
					}
					
					//Horrible hack to set a reasonable default size on the columns.
					//Min width to the width of the header column.
					Font f = new Font("System Bold", 13.0);
					for (final TreeTableColumn<SememeGUI, ?> col : ttv_.getColumns())
					{
						for (TreeTableColumn<SememeGUI, ?> nCol : col.getColumns())
						{
							String text = (nCol.getGraphic() != null 
									&& (nCol.getGraphic() instanceof Label || nCol.getGraphic() instanceof HBox) 
										? (nCol.getGraphic() instanceof Label ? ((Label)nCol.getGraphic()).getText() 
												: ((Label)((HBox)nCol.getGraphic()).getChildren().get(0)).getText()) 
										: nCol.getText());
							nCol.setMinWidth(Toolkit.getToolkit().getFontLoader().computeStringWidth(text, f) + 70);
						}
						
						if (col.getColumns().size() > 0)
						{
							FloatBinding binding = new FloatBinding()
							{
								{
									for (TreeTableColumn<SememeGUI, ?> nCol : col.getColumns())
									{
										if (nCol.getText().equals("String"))
										{
											nCol.setPrefWidth(250);  //these are common, and commonly long
										}
										bind(nCol.widthProperty());
										bind(nCol.visibleProperty());
									}
								}
								@Override
								protected float computeValue()
								{
									float temp = 0;
									for (TreeTableColumn<SememeGUI, ?> nCol : col.getColumns())
									{
										if (nCol.isVisible())
										{
											temp += nCol.getWidth();
										}
									}
									float parentColWidth = Toolkit.getToolkit().getFontLoader().computeStringWidth(col.getText(), f) + 70;
									if (temp < parentColWidth)
									{
										//bump the size of the first nested column, so the parent doesn't get clipped
										col.getColumns().get(0).setMinWidth(parentColWidth);
									}
									return temp;
								}
							};
							col.minWidthProperty().bind(binding);
						}
						else
						{
							String text = col.getText();
							
							if (text == null) {
								text = (col.getGraphic() != null && (col.getGraphic() instanceof Label || col.getGraphic() instanceof HBox)
										? (col.getGraphic() instanceof Label ? ((Label)col.getGraphic()).getText() : ((Label)((HBox)col.getGraphic()).getChildren().get(0)).getText()) 
										: col.getText());
							}
							
							if (text.equalsIgnoreCase(SememeGUIColumnType.ASSEMBLAGE.toString()) 
									|| text.equalsIgnoreCase(SememeGUIColumnType.COMPONENT.toString()))
							{
								col.setPrefWidth(250);
							}
							if (text.equalsIgnoreCase("s"))
							{
								col.setPrefWidth(32);
								col.setMinWidth(32);
							}
							else
							{
								col.setMinWidth(Toolkit.getToolkit().getFontLoader().computeStringWidth(text, f) + 70);
							}
						}
					}
					
					showStampColumns_.invalidate();
				});
				
				TableHeaderRowTooltipInstaller.installTooltips(ttv_, toolTipStore);

				loadRealData();
			}
			catch (Exception e)
			{
				if (e.getMessage().equals("Keyword"))
				{
					logger_.info("The specified concept is not specified correctly as a dynamic sememe, and is not utilized as a static sememe", e);
					Platform.runLater(() ->
					{
						addButton_.setDisable(true);
						treeRoot_.getChildren().clear();
						summary_.setText(0 + " entries");
						placeholderText.setText("The specified concept is not specified correctly as a dynamic sememe, and is not utilized as a static sememe");
						ttv_.setPlaceholder(placeholderText);
						ttv_.getSelectionModel().clearSelection();
					});
				}
				else
				{
					logger_.error("Unexpected error building the sememe display", e);
					//null check, as the error may happen before the scene is visible
					AppContext.getCommonDialogs().showErrorDialog("Error", "There was an unexpected error building the sememe display", e.getMessage(), 
							(rootNode_.getScene() == null ? null : rootNode_.getScene().getWindow()));
				}
			}
			finally
			{
				noRefresh_.getAndDecrement();
			}
		});
	}
	
	private synchronized void loadRealData() throws IOException, NumberFormatException, InterruptedException, ParseException
	{
		Platform.runLater(() ->
		{
			ttv_.setPlaceholder(progressBar_);
			treeRoot_.getChildren().clear();
		});
		// If UserProfileBindings.getDisplayFSN() has changed since last data load
		// then clear all filters, because they may contain outdated display text values
		boolean currentDisplayFSNPreferenceValue = displayFSN_.get();
		if (currentDisplayFSNPreferenceValue != filterCacheLastBuildDisplayFSNValue_) {
			logger_.debug("Clearing header node filter cache during refresh because displayFSN preference value changed to {}", currentDisplayFSNPreferenceValue);
			filterCacheLastBuildDisplayFSNValue_ = currentDisplayFSNPreferenceValue;
			removeFilterCacheListeners();
			for (HeaderNode.Filter<?> filter : filterCache_.values()) {
				filter.getFilterValues().clear();
			}
		}

		//Now add the data
		ArrayList<TreeItem<SememeGUI>> rowData = getDataRows(viewFocusNid_);
		
		logger_.info("Found {} rows of data in a dynamic sememe", rowData.size());
		
		Utility.execute(() ->
		{
			checkForUncommittedRefexes(rowData);
		});
		
		Platform.runLater(() ->
		{
			addButton_.setDisable(false);
			treeRoot_.getChildren().addAll(rowData);
			summary_.setText(rowData.size() + " entries");
			ttv_.setPlaceholder(placeholderText);
			ttv_.getSelectionModel().clearSelection();
		});
		
		// ADD LISTENERS TO headerNode.getUserFilters() TO EXECUTE REFRESH WHENEVER FILTER SETS CHANGE
		addFilterCacheListeners();
	}
	
	private TreeTableColumn<SememeGUI, SememeGUI> buildComponentCellColumn(SememeGUIColumnType type)
	{
		TreeTableColumn<SememeGUI, SememeGUI> ttCol = new TreeTableColumn<>(type.toString());
		HeaderNode<String> headerNode = new HeaderNode<>(
				filterCache_,
				ttCol,
				ColumnId.getInstance(type),
				rootNode_.getScene(),
				new HeaderNode.DataProvider<String>() {
					@Override
					public String getData(SememeGUI source) {
						return source.getDisplayStrings(type, null).getKey();
					}
				});
		ttCol.setGraphic(headerNode.getNode());
		
		ttCol.setSortable(true);
		ttCol.setResizable(true);
		ttCol.setComparator(new Comparator<SememeGUI>()
		{
			@Override
			public int compare(SememeGUI o1, SememeGUI o2)
			{
				return o1.compareTo(type, null, o2);
			}
		});
		ttCol.setCellFactory((colInfo) -> {return new ComponentDataCell(type);});
		ttCol.setCellValueFactory((callback) -> {return new ReadOnlyObjectWrapper<SememeGUI>(callback.getValue().getValue());});
		return ttCol;
	}

	
	private ArrayList<TreeItem<SememeGUI>> getDataRows(int componentNid, TreeItem<SememeGUI> nestUnder) 
			throws IOException
	{
		ArrayList<TreeItem<SememeGUI>> rowData = createFilteredRowData(Get.sememeService().getSememesForComponent(componentNid));
		
		if (nestUnder != null)
		{
			nestUnder.getChildren().addAll(rowData);
			return null;
		}
		else
		{
			return rowData;
		}
	}
	
	private ArrayList<TreeItem<SememeGUI>> createFilteredRowData(Stream<SememeChronology<? extends SememeVersion<?>>> sememes) throws IOException
	{
		ArrayList<TreeItem<SememeGUI>> rowData = new ArrayList<>();
		ArrayList<SememeVersion<?>> allVersions = new ArrayList<>();
		
		sememes.forEach(sememeC ->
		{
			for (SememeVersion<?> ds :  sememeC.getVersionList())
			{
				allVersions.add(ds);
			}
		});
		
		//Sort the newest to the top.
		Collections.sort(allVersions, new Comparator<SememeVersion<?>>()
		{
			@Override
			public int compare(SememeVersion<?> o1, SememeVersion<?> o2)
			{
				if (o1.getPrimordialUuid().equals(o2.getPrimordialUuid()))
				{
					return ((Long)o2.getTime()).compareTo(o1.getTime());
				}
				else
				{
					return o1.getPrimordialUuid().compareTo(o2.getPrimordialUuid());
				}
			}
		});
		
		UUID lastSeenRefex = null;
		
		for (SememeVersion<?> r : allVersions)
		{
			if (!showFullHistory_.get() && r.getPrimordialUuid().equals(lastSeenRefex))
			{
				continue;
			}
			if (showActiveOnly_.get() == false || r.getState() == State.ACTIVE)
			{
				SememeGUI newRefexDynamicGUI = new SememeGUI(r, !r.getPrimordialUuid().equals(lastSeenRefex));  //first one we see with a new UUID is current, others are historical
				
				// HeaderNode FILTERING DONE HERE
				boolean filterOut = false;
				for (HeaderNode.Filter<?> filter : filterCache_.values()) {
					if (filter.getFilterValues().size() > 0) {
						if (! filter.accept(newRefexDynamicGUI)) {
							filterOut = true;
							break;
						}
					}
				}
				
				if (! filterOut) {
					TreeItem<SememeGUI> ti = new TreeItem<>();

					ti.setValue(newRefexDynamicGUI);
					//recurse
					getDataRows(Get.identifierService().getSememeNid(r.getSememeSequence()), ti);  
					rowData.add(ti);
				}
			}
			lastSeenRefex = r.getPrimordialUuid();
		}

		return rowData;
	}

	/**
	 * The key of the first hashtable is the column description concept, while the key of the second hashtable is the assemblage concept
	 * Since the same column could be used in multiple assemblages - need to keep those separate, even though the rest of the column details 
	 * will be the same.  The List in the third level is for cases where a single assemblage concept re-uses the same column description 
	 * details multiple times.
	 */
	private Hashtable<UUID, Hashtable<UUID, List<DynamicSememeColumnInfo>>> getUniqueColumns(int componentNid)
	{
		Hashtable<UUID, Hashtable<UUID, List<DynamicSememeColumnInfo>>> columns = new Hashtable<>();
		
		Get.sememeService().getSememesForComponent(componentNid).forEach(sememeC ->
		{
			boolean assemblageWasNull = false;
			for (DynamicSememeColumnInfo column :  DynamicSememeUsageDescription.mockOrRead(sememeC).getColumnInfo())
			{
				Hashtable<UUID, List<DynamicSememeColumnInfo>> inner = columns.get(column.getColumnDescriptionConcept());
				if (inner == null)
				{
					inner = new Hashtable<>();
					columns.put(column.getColumnDescriptionConcept(), inner);
				}
				List<DynamicSememeColumnInfo> innerValues = inner.get(column.getAssemblageConcept());
				if (innerValues == null)
				{
					assemblageWasNull = true;
					innerValues = new ArrayList<>();
					inner.put(column.getAssemblageConcept(), innerValues);
				}
				if (assemblageWasNull)  //We only want to populate this on the first pass - the columns on an assemblage will never change from one pass to another.
				{
					innerValues.add(column);
				}
			}
			
			//recurse
			Hashtable<UUID, Hashtable<UUID, List<DynamicSememeColumnInfo>>> nested = getUniqueColumns(Get.identifierService()
					.getSememeNid(sememeC.getSememeSequence()));
			for (Entry<UUID, Hashtable<UUID, List<DynamicSememeColumnInfo>>> nestedItem : nested.entrySet())
			{
				if (columns.get(nestedItem.getKey()) == null)
				{
					columns.put(nestedItem.getKey(), nestedItem.getValue());
				}
				else
				{
					Hashtable<UUID, List<DynamicSememeColumnInfo>> mergeInto = columns.get(nestedItem.getKey());
					for (Entry<UUID, List<DynamicSememeColumnInfo>> toMergeItem : nestedItem.getValue().entrySet())
					{
						if (mergeInto.get(toMergeItem.getKey()) == null)
						{
							mergeInto.put(toMergeItem.getKey(), toMergeItem.getValue());
						}
						else
						{
							//don't care - we already have this assemblage concept - the column values will be the same as what we already have.
						}
					}
				}
			}
		});
			
		return columns;
	}
	
	private void checkForUncommittedRefexes(List<TreeItem<SememeGUI>> items)
	{
		if (hasUncommitted_.get())
		{
			return;
		}
		if (items == null)
		{
			return;
		}
		for (TreeItem<SememeGUI> item : items)
		{
			if (item.getValue() != null && item.getValue().getSememe().isUncommitted())
			{
				//TODO add some indication that this is either running / finished
				Platform.runLater(() ->
				{
					hasUncommitted_.set(true);
				});
				return;
			}
			checkForUncommittedRefexes(item.getChildren());
		}
	}
	
	private HashSet<Integer> getAllAssemblageSequences(List<TreeItem<SememeGUI>> items)
	{
		HashSet<Integer> results = new HashSet<Integer>();
		if (items == null)
		{
			return results;
		}
		for (TreeItem<SememeGUI> item : items)
		{
			if (item.getValue() != null)
			{
				SememeVersion<?> refex = item.getValue().getSememe();
				results.add(refex.getAssemblageSequence());
			}
			results.addAll(getAllAssemblageSequences(item.getChildren()));
		}
		return results;
	}
	
	private void forgetAllUncommitted(List<TreeItem<SememeGUI>> items) throws IOException
	{
		
		if (items == null)
		{
			return;
		}
//		for (TreeItem<RefexDynamicGUI> item : items)
//		{
			//TODO commit / cancel / forget?
//			if (item.getValue() != null)
//			{
//				DynamicSememeVersionBI<? extends DynamicSememeVersionBI<?>> refex = item.getValue().getRefex();
//				if (refex.isUncommitted())
//				{
//					ExtendedAppContext.getDataStore().forget(refex);
//					ConceptVersionBI cv = ExtendedAppContext.getDataStore().getConceptVersion(OTFUtility.getViewCoordinate(), refex.getReferencedComponentNid());
//					//if assemblageNid != concept nid - this means it is an annotation style refex
//                                        // TODO There are no more annotation refexes, they are all stored the same...
//                                        cv.cancel();
////					if ((refex.getAssemblageNid() != refex.getConceptNid()) && cv.isUncommitted())
////					{
////						cv.cancel();
////					}
//				}
//			}
//			forgetAllUncommitted(item.getChildren());
//		}
	}
	
	private HashSet<Integer> getAllComponentNids(List<TreeItem<SememeGUI>> items)
	{
		HashSet<Integer> results = new HashSet<Integer>();
		if (items == null)
		{
			return results;
		}
		for (TreeItem<SememeGUI> item : items)
		{
			if (item.getValue() != null)
			{
				SememeVersion<?> refex = item.getValue().getSememe();
				results.add(refex.getReferencedComponentNid());
			}
			results.addAll(getAllComponentNids(item.getChildren()));
		}
		return results;
	}
	

	private ArrayList<TreeItem<SememeGUI>> getDataRows(int nid) 
			throws IOException, InterruptedException, NumberFormatException, ParseException
	{
		Platform.runLater(() ->
		{
			progressBar_.setProgress(-1);
			ttv_.setPlaceholder(progressBar_);
		});
		
		
		ArrayList<TreeItem<SememeGUI>> rowData = createFilteredRowData(viewFocus_ == ViewFocus.ASSEMBLAGE ? 
				Get.sememeService().getSememesFromAssemblage(nid) :
					Get.sememeService().getSememesForComponent(nid));

		if (rowData.size() == 0)
		{
			placeholderText.setText("No Dynamic Sememes were found using this Assemblage");
		}
		return rowData;
	}
	@Override
	public void viewDiscarded()
	{
		noRefresh_.incrementAndGet();
		refreshRequiredListenerHack.clearBindings();
	}
}
