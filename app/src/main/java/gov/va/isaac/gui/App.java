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
import gov.va.isaac.gui.dialog.CommonDialogs;
import gov.va.isaac.gui.download.DownloadDialog;
import gov.va.isaac.init.SystemInit;
import gov.va.isaac.util.Utility;
import gov.vha.isaac.ochre.api.LookupService;
import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import org.ihtsdo.otf.tcc.api.store.TerminologyStoreDI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ISAAC {@link Application} class.
 *
 * @author ocarlsen
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a> 
 */

public class App extends Application {
    
    private final Logger LOG = LoggerFactory.getLogger(App.class);

    protected AppController controller;
    private boolean shutdown = false;
    protected Stage primaryStage_;
    private static Exception dataStoreLocationInitException_ = null;
    
    /**
     * Contructor for JavaFX only
     */
    public App()
    {
        //install this instance into HK2
        //ServiceLocatorUtilities.addOneConstant(AppContext.getServiceLocator(), this);
        //Dan notes - we used to just implement the ApplicationWindowI directly here, and inject it into HK2, per above.
        //But HK2 broke, so now we have to have an extra class.  https://java.net/jira/browse/HK2-255
        
        LookupService.getService(IsaacAppWindow.class).setAppRef(this);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage_ = primaryStage;
        

        this.controller = new AppController();

        primaryStage.getIcons().add(new Image("/icons/application-icon.png"));
        String title = AppContext.getAppConfiguration().getApplicationTitle()  + " - "+ AppContext.getAppConfiguration().getVersion();
        primaryStage.setTitle(title);
        primaryStage.setScene(new Scene(controller.getRoot()));
        primaryStage.getScene().getStylesheets().add(App.class.getResource("/isaac-shared-styles.css").toString());
        primaryStage.getScene().getStylesheets().add(App.class.getResource("App.css").toString());

        // Set minimum dimensions.
        primaryStage.setMinHeight(400);
        primaryStage.setMinWidth(400);
        
        primaryStage.setHeight(768);
        primaryStage.setWidth(1024);

        // Handle window close event.
        primaryStage.setOnHiding(new EventHandler<WindowEvent>() {

            /*
            @Override
            public void handle(WindowEvent event) {
                shutdown();
            }
            */

            @Override
            public void handle(WindowEvent event) {
                final Stage dialog = new Stage(StageStyle.UNDECORATED);
                dialog.initModality(Modality.APPLICATION_MODAL);

                dialog.setScene(new Scene(LightWeightDialogs.buildWaitDialog("Closing the ISAAC database")));
                dialog.getScene().getStylesheets().add(App.class.getResource("/isaac-shared-styles.css").toString());
                dialog.getScene().getStylesheets().add(App.class.getResource("App.css").toString());
                dialog.show();

                Runnable shutdownTask = () -> { 
                    shutdown();
                    Platform.runLater(() -> {
                        dialog.close();
                        Platform.exit();
                    });
                };
                
                Thread shutdownThread = new Thread(shutdownTask);
                shutdownThread.setDaemon(false);
                shutdownThread.setName("ISAAC Shutdown");
                shutdownThread.start();
            }
        
        });

        primaryStage.show();

        // Reduce size to fit in user's screen.
        // (Need to do after stage is shown, because otherwise
        // the primary stage width & height are NaN.)
        Screen screen = Screen.getPrimary();
        double screenW = screen.getVisualBounds().getWidth();
        double screenH = screen.getVisualBounds().getHeight();
        if (primaryStage.getWidth() > screenW) {
            LOG.debug("Resizing width to " + screenW);
            primaryStage.setWidth(screenW);
        }
        if (primaryStage.getHeight() > screenH) {
            LOG.debug("Resizing height to " + screenH);
            primaryStage.setHeight(screenH);
        }

        if (dataStoreLocationInitException_ == null)
        {
            // Kick off a thread to open the DB connection.
            loadDataStore();
        }
        else
        {
            new DownloadDialog(AppContext.getMainApplicationWindow().getPrimaryStage(), new Consumer<Boolean>()
            {
                @Override
                public void accept(Boolean t)
                {
                    if (t)
                    {
                        dataStoreLocationInitException_ = null;
                        try
                        {
                            SystemInit.configDataStorePaths(new File(""));
                        }
                        catch (IOException e)
                        {
                            //this should be impossible
                            LOG.error("Failed to find DB after download?", e);
                            // Close app since no DB to load.
                            // (The #shutdown method will be also invoked by
                            // the handler we hooked up with Stage#setOnHiding.)
                            primaryStage_.hide();
                        }
                        loadDataStore();
                    }
                    else
                    {
                        // Close app since no DB to load.
                        // (The #shutdown method will be also invoked by
                        // the handler we hooked up with Stage#setOnHiding.)
                        primaryStage_.hide();
                    }
                    
                }
            });
        }
    }

    private void loadDataStore() {

        // Do work in background.
        Task<TerminologyStoreDI> task = new Task<TerminologyStoreDI>() {

            @Override
            protected TerminologyStoreDI call() throws Exception {
                LOG.info("Opening Workbench database");
                LookupService.startupIsaac();
                TerminologyStoreDI dataStore = AppContext.getServiceLocator().getService(TerminologyStoreDI.class);
                LOG.info("Finished opening Workbench database");

                // Check if user shut down early.
                if (shutdown) {
                    dataStore.shutdown();
                    return null;
                }

                return dataStore;
            }

            @Override
            protected void succeeded() {
                controller.finishInit();
            }

            @Override
            protected void failed() {
                Throwable ex = getException();

                // Display helpful dialog to users.
                String title = "Unexpected error connecting to workbench database";
                String msg = ex.getClass().getName();
                String details = ex.getMessage();
                LOG.error(title, ex);
                AppContext.getServiceLocator().getService(CommonDialogs.class).showErrorDialog(title, msg, details);
            }
        };

        Thread t = new Thread(task, "SCT_DB_Open");
        t.setDaemon(true);
        t.start();
    }



    protected void shutdown() {
        LOG.info("Shutting down");
        shutdown = true;
        if (primaryStage_.isShowing())
        {
            primaryStage_.hide();
        }
        try {
            Utility.shutdownThreadPools();
            if (dataStoreLocationInitException_ == null)
            {
                LookupService.shutdownIsaac();
            }
            controller.shutdown();
        } catch (Throwable ex) {
            String message = "Trouble shutting down";
            LOG.warn(message, ex);
            AppContext.getServiceLocator().getService(CommonDialogs.class).showErrorDialog("Oops!", message, ex.getMessage());
        }
        LOG.info("Finished shutting down");
    }
    

    public static void main(String[] args) throws Exception {
        dataStoreLocationInitException_ = SystemInit.doBasicSystemInit(new File(""));
        if (dataStoreLocationInitException_ != null)
        {
            System.err.println("Configuration of datastore path failed.  DB will not be able to start properly!  " + dataStoreLocationInitException_);
            dataStoreLocationInitException_.printStackTrace();
        }

        Application.launch(args);
    }
}
