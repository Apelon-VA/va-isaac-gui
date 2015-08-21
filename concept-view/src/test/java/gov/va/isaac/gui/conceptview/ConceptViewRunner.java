package gov.va.isaac.gui.conceptview;

import gov.va.isaac.AppContext;
import gov.va.isaac.config.profiles.UserProfileManager;
import gov.va.isaac.init.SystemInit;
import gov.vha.isaac.ochre.api.LookupService;

import java.io.File;
import java.io.IOException;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

import javax.inject.Singleton;

import org.jvnet.hk2.annotations.Service;

@Service
@Singleton
public class ConceptViewRunner extends Application {
    final private ConceptView view;

    private Stage primaryStage;
    private Region rootLayout;
    
    public ConceptViewRunner() throws IOException {
        view = new ConceptView();
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.primaryStage.setTitle("Concept View");

        initRootLayout();
    }

    /**
     * Initializes the root layout.
     */
    public void initRootLayout() {
        // Load root layout from fxml file.
        rootLayout = view.getView();

        // Show the scene containing the root layout
        primaryStage.setScene(new Scene(rootLayout));
        primaryStage.show();
    }

	public static void main(String[] args) throws Exception
	{
		//Configure Java logging into log4j2
		System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");

		Exception dataStoreLocationInitException = SystemInit.doBasicSystemInit(new File("../../va-isaac-gui-pa/app-assembly/"));
		if (dataStoreLocationInitException != null)
		{
			System.err.println("Configuration of datastore path failed.  DB will not be able to start properly!  " + dataStoreLocationInitException);
			System.exit(-1);
		}
		LookupService.startupIsaac();
		AppContext.getService(UserProfileManager.class).configureAutomationMode(new File("profiles"));
		launch(args);
	}
    
}