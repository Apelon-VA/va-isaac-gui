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
package gov.va.isaac.gui;

import gov.va.isaac.AppContext;
import gov.va.isaac.ExtendedAppContext;
import gov.va.isaac.RuntimeGlobals;
import gov.va.isaac.gui.util.FxUtils;
import gov.va.isaac.gui.util.HeapStatusBar;
import gov.va.isaac.gui.util.ToolTipDefaultsFixer;
import gov.va.isaac.interfaces.gui.ApplicationMenus;
import gov.va.isaac.interfaces.gui.CheckMenuItemI;
import gov.va.isaac.interfaces.gui.MenuItemI;
import gov.va.isaac.interfaces.gui.views.DockedViewI;
import gov.va.isaac.interfaces.gui.views.IsaacViewWithMenusI;
import gov.va.isaac.interfaces.utility.ServicesToPreloadI;
import gov.va.isaac.util.UpdateableDoubleBinding;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Separator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.ToolBar;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javax.inject.Inject;
import org.glassfish.hk2.api.IterableProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller class for {@link App}.
 *
 * @author ocarlsen
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public class AppController {

    private static final Logger LOG = LoggerFactory.getLogger(AppController.class);

    private BorderPane root_;
    private ToolBar toolbar_;
    private SplitPane mainSplitPane;
    private MenuBar menuBar;
    private BorderPane loadWait;
    
    private ProgressBar backgroundTasksBar_;
    private Text backgroundTasksLabel_;
    private UpdateableDoubleBinding backgroundTasksProgress_;
    private ArrayList<Task<?>> backgroundTasks_ = new ArrayList<>();
    
    private boolean preloadExecuted = false;


    @Inject
    private IterableProvider<IsaacViewWithMenusI> moduleViews_;
    @Inject
    private IterableProvider<DockedViewI> dockedViews_;
    @Inject
    private IterableProvider<ServicesToPreloadI> preloadRequested_;

    //Just a hashed view of all of the menus (including nested menus)
    private final Hashtable<String, Menu> allMenus_ = new Hashtable<>();

    public AppController() {

        AppContext.getServiceLocator().inject(this);
        ToolTipDefaultsFixer.setTooltipTimers(100, 20000, 200);
        
        root_ = new BorderPane();
        mainSplitPane = new SplitPane();
        mainSplitPane.setDividerPositions(0.60);
        mainSplitPane.setOrientation(Orientation.HORIZONTAL);
        mainSplitPane.getStyleClass().add("hashedBackground");
        
        root_.setCenter(mainSplitPane);
        root_.setMaxHeight(Double.MAX_VALUE);
        root_.setMaxWidth(Double.MAX_VALUE);
        
        loadWait = new BorderPane();
        loadWait.setCenter(LightWeightDialogs.buildWaitDialog("Loading the database"));
        mainSplitPane.getItems().add(loadWait);
        mainSplitPane.setMaxWidth(Double.MAX_VALUE);
        mainSplitPane.setMaxHeight(Double.MAX_VALUE);
        
        menuBar = new MenuBar();
        for (ApplicationMenus menu : ApplicationMenus.values())
        {
            Menu m = new Menu(menu.getMenuName());
            m.setId(menu.getMenuId());
            if (!menu.getAlwaysAvailable())
            {
                m.setDisable(true);
            }
            m.setMnemonicParsing(false);
            menuBar.getMenus().add(m);
            //index these for ease in adding module menus
            allMenus_.put(m.getId(), m);
        }
        
        root_.setTop(menuBar);

        //Sort them... per parent menu
        HashMap<String, TreeSet<MenuItemI>> menusToAdd = new HashMap<>();
        for (IsaacViewWithMenusI view : moduleViews_)
        {
            for (MenuItemI menuItem : view.getMenuBarMenus())
            {
                TreeSet<MenuItemI> treeSet = menusToAdd.get(menuItem.getParentMenuId());
                if (treeSet == null)
                {
                    treeSet = new TreeSet<>();
                    menusToAdd.put(menuItem.getParentMenuId(), treeSet);
                }
                treeSet.add(menuItem);
            }
        }

        for (final TreeSet<MenuItemI> groupedMenuItemsToCreate : menusToAdd.values())
        {
            for (final MenuItemI menuItemToCreate : groupedMenuItemsToCreate)
            {
                Menu parentMenu = allMenus_.get(menuItemToCreate.getParentMenuId());
                if (parentMenu == null)
                {
                    LOG.error("Cannot add module menu '" + menuItemToCreate.getMenuId() + "' because the specified parent menu doesn't exist");
                }
                else
                {
                    MenuItem menuItem;
                    if (menuItemToCreate instanceof CheckMenuItemI)
                    {
                        menuItem = new CheckMenuItem();
                        ((CheckMenuItem)menuItem).setSelected(((CheckMenuItemI)menuItemToCreate).initialState());
                        menuItem.setOnAction(new EventHandler<ActionEvent>()
                        {
                            @Override
                            public void handle(ActionEvent arg0)
                            {
                                menuItemToCreate.handleMenuSelection(root_.getScene().getWindow(), (CheckMenuItem)menuItem);
                            }
                        });
                    }
                    else
                    {
                        menuItem = new MenuItem();
                    }
                    
                    menuItem.setId(menuItemToCreate.getMenuId());
                    menuItem.setText(menuItemToCreate.getMenuName());
                    menuItem.setMnemonicParsing(menuItemToCreate.enableMnemonicParsing());
                    menuItem.setOnAction(new EventHandler<ActionEvent>()
                    {
                        @Override
                        public void handle(ActionEvent arg0)
                        {
                            menuItemToCreate.handleMenuSelection(root_.getScene().getWindow(), menuItem);
                        }
                    });
                    if (menuItemToCreate.getImage() != null)
                    {
                        menuItem.setGraphic(new ImageView(menuItemToCreate.getImage()));
                    }
                    if (menuItemToCreate.getDisableBinding() != null)
                    {
                        menuItem.disableProperty().bind(menuItemToCreate.getDisableBinding());
                    }
                    parentMenu.getItems().add(menuItem);
                }
            }
        }

        LOG.debug("Menus configured");
        
        ArrayList<DockedViewI> sortedDockedViews = new ArrayList<>();
        for (DockedViewI dv : dockedViews_)
        {
            sortedDockedViews.add(dv);
        }
        Collections.sort(sortedDockedViews, new Comparator<DockedViewI>()
        {
            @Override
            public int compare(DockedViewI o1, DockedViewI o2)
            {
                return o1.getMenuBarMenuToShowView().compareTo(o2.getMenuBarMenuToShowView());
            }
        });
        
       
        for (final DockedViewI dv : sortedDockedViews)
        {
            try
            {
                Menu parentMenu = allMenus_.get(dv.getMenuBarMenuToShowView().getParentMenuId());
                if (parentMenu == null)
                {
                    LOG.error("Cannot add module menu '" + dv.getMenuBarMenuToShowView().getMenuId() + "' because the specified parent menu doesn't exist");
                }
                else
                {
                    final BorderPane bp = buildPanelForView(dv);
                    final CheckMenuItem mi = new CheckMenuItem();
                    mi.setText(dv.getMenuBarMenuToShowView().getMenuName());
                    mi.setId(dv.getMenuBarMenuToShowView().getMenuId());
                    mi.setMnemonicParsing(dv.getMenuBarMenuToShowView().enableMnemonicParsing());
                    mi.selectedProperty().addListener(new InvalidationListener()
                    {
                        @Override
                        public void invalidated(Observable observable)
                        {
                            //This is a convenience call... not expected to actually show the view.
                            dv.getMenuBarMenuToShowView().handleMenuSelection(root_.getScene().getWindow(), mi);
                            //call both APIs, not all callers may have implemented the newer one (which has a default)
                            dv.getMenuBarMenuToShowView().handleMenuSelection(root_.getScene().getWindow(), (MenuItem)mi);  
                            if (mi.isSelected() && !mainSplitPane.getItems().contains(bp))
                            {
                                mainSplitPane.getItems().add(bp);
                            }
                            else if (!mi.isSelected() && mainSplitPane.getItems().contains(bp))
                            {
                                hidePanelView(bp);
                            }
                        }
                    });
                    if (dv.getMenuBarMenuToShowView().getImage() != null)
                    {
                        mi.setGraphic(new ImageView(dv.getMenuBarMenuToShowView().getImage()));
                    }
                    mi.selectedProperty().bindBidirectional(bp.visibleProperty());
                    parentMenu.getItems().add(mi);
                }
            }
            catch (Exception e)
            {
                LOG.error("Unexpected error configuring DockedViewI " + (dv == null ? "?" : dv.getViewTitle()), e);
            }
        }
        LOG.debug("Docked Views configured");
        
        toolbar_ = new ToolBar();
        toolbar_.setPrefSize(Double.MAX_VALUE, 24.0);
        toolbar_.setOrientation(Orientation.HORIZONTAL);
        
        StackPane sp = new StackPane();
        sp.setPrefWidth(200.0);
        
        backgroundTasksBar_ = new ProgressBar(0.0);
        backgroundTasksBar_.setPrefWidth(Double.MAX_VALUE);
        sp.getChildren().add(backgroundTasksBar_);
        
        backgroundTasksLabel_ = new Text("0 background tasks");
        sp.getChildren().add(backgroundTasksLabel_);
        
        backgroundTasksProgress_ = new UpdateableDoubleBinding()
        {
            @Override
            protected double computeValue()
            {
                double total = 0;
                double current = 0;
                boolean indeterminite = false;
                
                Iterator<Task<?>> taskIterator = backgroundTasks_.iterator();
                try
                {
                    while (taskIterator.hasNext())
                    {
                        Task<?> t = taskIterator.next();
                        if (t.isDone())
                        {
                            taskIterator.remove();
                            backgroundTasksProgress_.removeBinding(t.workDoneProperty());
                            continue;
                        }
                        
                        if (t.getTotalWork() == -1)
                        {
                            indeterminite = true;
                            //don't break - want to check if any are done
                        }
                        else
                        {
                            total += t.getTotalWork();
                            current += t.getWorkDone();
                        }
                    }
                }
                catch (ConcurrentModificationException e)
                {
                    // don't care, clean up next time
                }
                
                backgroundTasksLabel_.setText(backgroundTasks_.size() + " background task" + (backgroundTasks_.size() == 1 ? "" : "s"));
                
                if (indeterminite)
                {
                    return -1;
                }
                else
                {
                    return (total == 0 ? 0 : current / total);
                }
            }
        };
        
        backgroundTasksBar_.progressProperty().bind(backgroundTasksProgress_);
        
        toolbar_.getItems().add(sp);
        Region spacer = new Region();
        spacer.setPrefWidth(Region.USE_PREF_SIZE);
        HBox.setHgrow(spacer, Priority.ALWAYS);
        toolbar_.getItems().add(spacer);
        toolbar_.getItems().add(new Separator(Orientation.VERTICAL));
        toolbar_.getItems().add(new HeapStatusBar());
        
        root_.setBottom(toolbar_);
        
        LOG.debug("Toolbar configured");
    }
    
    public BorderPane getRoot()
    {
        return root_;
    }

    protected void finishInit() {
        // Make sure in application thread.
        FxUtils.checkFxUserThread();
        
        //Kick off other preloads
        for (ServicesToPreloadI service : preloadRequested_)
        {
            LOG.debug("Preloading {}", service);
            service.loadRequested();
        }
        preloadExecuted = true;
        LOG.debug("Preloads complete");
        
        loadWait.getChildren().clear();
        
        AtomicLong loginFailCount = new AtomicLong(0);
        loadWait.setCenter(LightWeightDialogs.buildLoginDialog(new Consumer<Boolean>()
        {
            @Override
            public void accept(Boolean t)
            {
                if (!t)
                {
                    if (loginFailCount.incrementAndGet() > 3)
                    {
                        ((App)ExtendedAppContext.getMainApplicationWindow()).shutdown();
                    }
                }
                else
                {
                    mainSplitPane.getItems().remove(loadWait);
                    loadWait = null;
                    // Enable the menus.
                    for (Menu menu : menuBar.getMenus())
                    {
                        menu.setDisable(false);
                    }
                }
                
            }
        }));
        BorderPane.setAlignment(loadWait.getCenter(), Pos.CENTER);
    }

    private BorderPane buildPanelForView(DockedViewI dockedView)
    {
        final BorderPane bp = new BorderPane();
        bp.setVisible(false);
        AnchorPane ap = new AnchorPane();
        ap.getStyleClass().add("headerBackground");

        Label l = new Label(dockedView.getViewTitle());
        l.getStyleClass().add("titleLabel");
        AnchorPane.setLeftAnchor(l, 5.0);
        AnchorPane.setTopAnchor(l, 2.0);
        ap.getChildren().add(l);

        Button b = new Button();
        b.setMnemonicParsing(false);
        b.setStyle("-fx-cursor:hand");
        b.getStyleClass().add("tab-close-button");
        b.setOnAction(new EventHandler<ActionEvent>()
        {
            @Override
            public void handle(ActionEvent arg0)
            {
                hidePanelView(bp);
            }
        });
        AnchorPane.setTopAnchor(b, 0.0);
        AnchorPane.setRightAnchor(b, 5.0);
        ap.getChildren().add(b);

        bp.setTop(ap);
        Label placeholder = new Label("");
        bp.setCenter(placeholder);
        //Delay this, so we don't force the init of all of these panels up front
        bp.visibleProperty().addListener((change) -> 
        {
            if (bp.isVisible() && bp.getCenter() == placeholder)
            {
                bp.setCenter(dockedView.getView());
            }
        });
        return bp;
    }

    private void hidePanelView(BorderPane bp)
    {
        bp.setVisible(false);
        mainSplitPane.getItems().remove(bp);
    }
    
    public void ensureDockedViewIsVisible(DockedViewI view)
    {
        Menu parentMenu = allMenus_.get(view.getMenuBarMenuToShowView().getParentMenuId());
        String id = view.getMenuBarMenuToShowView().getMenuId();
        for (MenuItem menu : parentMenu.getItems())
        {
            if (menu.getId().equals(id))
            {
                CheckMenuItem cmi = (CheckMenuItem)menu;
                cmi.selectedProperty().set(true);
                break;
            }
        }
    }
    
    protected void shutdown()
    {
        AppContext.getService(RuntimeGlobals.class).shutdown();
        if (preloadExecuted)
        {
            //notify anything that was preloaded
            for (ServicesToPreloadI service : preloadRequested_)
            {
                LOG.debug("Shutdown notify {}", service);
                service.shutdown();
            }
        }
    }
    
    protected void addBackgroundTask(Task<?> task)
    {
        backgroundTasks_.add(task);
        Platform.runLater(() -> 
        {    
            task.runningProperty().addListener((change) -> backgroundTasksProgress_.invalidate());
            backgroundTasksProgress_.addBinding(task.workDoneProperty());
        });
    }
}