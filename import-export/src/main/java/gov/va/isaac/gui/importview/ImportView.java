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
package gov.va.isaac.gui.importview;

import gov.va.isaac.AppContext;
import gov.va.isaac.gui.util.FxUtils;
import gov.va.isaac.gui.util.GridPaneBuilder;
import gov.va.isaac.model.InformationModelType;
import gov.va.isaac.models.cem.importer.CEMImporter;

import java.io.File;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.concurrent.Task;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;

import org.ihtsdo.otf.tcc.api.concept.ConceptChronicleBI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

/**
 * A GUI for handling imports.
 *
 * @author ocarlsen
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public class ImportView extends GridPane {

    private static final Logger LOG = LoggerFactory.getLogger(ImportView.class);

    private final Label modelTypeLabel = new Label();
    private final Label fileNameLabel = new Label();
    private final Label resultLabel = new Label();

    public ImportView() {
        super();

        // GUI placeholders.
        GridPaneBuilder builder = new GridPaneBuilder(this);
        builder.addRow("Information Model: ", modelTypeLabel);
        builder.addRow("File Name: ", fileNameLabel);
        builder.addRow("Result: ", resultLabel);

        setConstraints();

        // Set minimum dimensions.
        setMinHeight(200);
        setMinWidth(600);
    }

    public void doImport(InformationModelType modelType, final String fileName) {
        Preconditions.checkNotNull(modelType);
        Preconditions.checkNotNull(fileName);

        // Make sure in application thread.
        FxUtils.checkFxUserThread();

        // Update UI.
        modelTypeLabel.setText(modelType.getDisplayName());
        fileNameLabel.setText(fileName);

        if (modelType == InformationModelType.CEM) {
            importCEM(modelType, fileName);
        } else {
            throw new UnsupportedOperationException(modelType.getDisplayName() +
                    " import not yet supported in ISAAC.");
        }
    }

    private void importCEM(InformationModelType modelType, final String fileName) {

        // Do work in background.
        Task<ConceptChronicleBI> task = new Task<ConceptChronicleBI>() {

            @Override
            protected ConceptChronicleBI call() throws Exception {

                // Do work.
                CEMImporter importer = new CEMImporter();
                return importer.importModel(new File(fileName));
            }

            @Override
            protected void succeeded() {
                ConceptChronicleBI result = this.getValue();

                // Update UI.
                resultLabel.setText("Successfully imported concept: " + result.toUserString());
           }

            @Override
            protected void failed() {
                Throwable ex = getException();

                // Update UI.
                resultLabel.setText("Failed to import model: " + ex.getMessage());

                // Show dialog.
                String title = ex.getClass().getName();
                String msg = String.format("Unexpected error importing from file \"%s\"", fileName);
                LOG.error(msg, ex);
                AppContext.getCommonDialogs().showErrorDialog(title, msg, ex.getMessage());
            }
        };

        // Bind cursor to task state.
        ObjectBinding<Cursor> cursorBinding = Bindings.when(task.runningProperty()).then(Cursor.WAIT).otherwise(Cursor.DEFAULT);
        this.getScene().cursorProperty().bind(cursorBinding);

        Thread t = new Thread(task, "Importer_" + modelType);
        t.setDaemon(true);
        t.start();
    }

    private void setConstraints() {

        // Column 1 has empty constraints.
        this.getColumnConstraints().add(new ColumnConstraints());

        // Column 2 should grow to fill space.
        ColumnConstraints column2 = new ColumnConstraints();
        column2.setHgrow(Priority.ALWAYS);
        this.getColumnConstraints().add(column2);

        // Rows 1-4 have empty constraints.
        this.getRowConstraints().add(new RowConstraints());
        this.getRowConstraints().add(new RowConstraints());
        this.getRowConstraints().add(new RowConstraints());
        this.getRowConstraints().add(new RowConstraints());

        // Row 5 should
        RowConstraints row5 = new RowConstraints();
        row5.setVgrow(Priority.ALWAYS);
        this.getRowConstraints().add(row5);
    }
}
