package gov.va.isaac.gui.conceptview;

import gov.va.isaac.AppContext;
import gov.va.isaac.config.profiles.UserProfileManager;
import gov.va.isaac.init.SystemInit;
import gov.va.isaac.interfaces.gui.constants.SharedServiceNames;
import gov.vha.isaac.ochre.api.LookupService;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

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
    	/*
    	 * Drug coated stent 9268fbc8-50f8-38d0-bbc5-d78dc671b4ca
		 * Bleeding (finding) 89ce6b87-545b-3138-82c7-aafa76f8f9a0
		 * Fracture of radius c50138b9-70ee-3af2-b567-af2f20359925
		 * Entire skin (body structure) cbb0653c-bc87-37b0-aafb-6d020917e172
		 * Hand pain 9549a066-7d57-371d-8958-82a6a0b5b175
		 * Arthroscopy (procedure) 4bf05b37-076a-3a6a-ad53-b10bbf83cfc5
		 */
        view = LookupService.getService(ConceptView.class, SharedServiceNames.DIAGRAM_STYLE);
        
        view.setConcept(UUID.fromString("9549a066-7d57-371d-8958-82a6a0b5b175"));
        //view.setConcept(UUID.fromString("4bf05b37-076a-3a6a-ad53-b10bbf83cfc5"));
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