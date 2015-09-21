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
package gov.va.isaac.gui.conceptCreation;

import java.io.File;

import gov.va.isaac.AppContext;
import gov.va.isaac.config.profiles.UserProfileManager;
import gov.va.isaac.init.SystemInit;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

/**
 * RefsetPanelRunner
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public class NewConceptCreationWizardRunner extends Application
{
	/**
	 * @see javafx.application.Application#start(javafx.stage.Stage)
	 */
	@Override
	public void start(Stage primaryStage) throws Exception
	{
		primaryStage.setTitle("New Concept Panel");

		ConceptCreationWizard refsetPanel = AppContext.getService(ConceptCreationWizard.class);
		//refsetPanel.setRefsetAndComponent(gov.va.isaac.models.cem.importer.CEMMetadataBinding.CEM_DATA_REFSET.getUuids()[0], diastolicBP);

		primaryStage.setScene(new Scene(new Label("hello world"), 200, 100));
		primaryStage.show();
		refsetPanel.showView(primaryStage.getOwner());
	}

	public static void main(String[] args) throws Exception
	{
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
