package gov.va.isaac.gui;

import gov.va.isaac.gui.dialog.ErrorDialog;
import gov.va.isaac.gui.provider.ConceptDialogProvider;
import javafx.stage.Stage;

import com.sun.javafx.tk.Toolkit;

/**
 * Application utility for ISAAC app.
 *
 * @author ocarlsen
 */
public class AppUtil {

    private final Stage primaryStage;
    private final ErrorDialog errorDialog;
    private final ConceptDialogProvider conceptDialogProvider;

    public AppUtil(Stage primaryStage, ConceptDialogProvider conceptDialogProvider) throws Exception {
        this.primaryStage = primaryStage;
        this.conceptDialogProvider = conceptDialogProvider;

        // Reusable error dialog.
        this.errorDialog = new ErrorDialog(primaryStage);
    }

    /**
     * @return A reference to the primary {@link Stage} of the app.
     */
    public Stage getPrimaryStage() {
        return primaryStage;
    }

    /**
     * Shows an error dialog with the specified title, message, and details.
     * @param title
     * @param message
     * @param details
     */
    public void showErrorDialog(final String title, final String message, final String details) {

        // Make sure in application thread.
        Toolkit.getToolkit().checkFxUserThread();

        errorDialog.setVariables(title, message, details);
        errorDialog.showAndWait();
    }

    public ConceptDialogProvider getConceptDialogProvider() {
        return conceptDialogProvider;
    }
}
