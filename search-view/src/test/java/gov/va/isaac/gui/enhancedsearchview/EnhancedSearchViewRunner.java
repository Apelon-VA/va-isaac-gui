package gov.va.isaac.gui.enhancedsearchview;

import gov.va.isaac.AppContext;
import gov.va.isaac.config.profiles.UserProfileManager;
import gov.va.isaac.init.SystemInit;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import javax.inject.Singleton;
import org.ihtsdo.otf.query.lucene.LuceneIndexer;
import org.ihtsdo.otf.tcc.api.store.TerminologyStoreDI;
import org.ihtsdo.otf.tcc.lookup.Hk2Looker;
import org.jvnet.hk2.annotations.Service;

/**
 * EnhancedSearchViewController
 * 
 * @author <a href="mailto:joel.kniaz@gmail.com">Joel Kniaz</a>
 */
@Service
@Singleton
public class EnhancedSearchViewRunner extends Application {
    final private EnhancedSearchView view;

    private Stage primaryStage;
    private Region rootLayout;
    
    public EnhancedSearchViewRunner() throws IOException {
        view = new EnhancedSearchView();
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.primaryStage.setTitle("EnhancedSearchView");

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

    public static void main(String[] args) throws Exception {
        Exception dataStoreLocationInitException = SystemInit.doBasicSystemInit(new File("../../isaac-pa/app/"));
        if (dataStoreLocationInitException != null)
        {
            System.err.println("Configuration of datastore path failed.  DB will not be able to start properly!  " + dataStoreLocationInitException);
            System.exit(-1);
        }
        AppContext.getService(UserProfileManager.class).configureAutomationMode(new File("profiles"));

        launch(args);
    }
}