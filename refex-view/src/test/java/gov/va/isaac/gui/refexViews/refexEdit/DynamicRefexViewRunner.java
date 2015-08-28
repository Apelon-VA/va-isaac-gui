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

import java.io.File;
import gov.va.isaac.AppContext;
import gov.va.isaac.config.profiles.UserProfileManager;
import gov.va.isaac.init.SystemInit;
import gov.vha.isaac.ochre.api.LookupService;
import gov.vha.isaac.ochre.model.constants.IsaacMetadataConstants;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * {@link DynamicRefexViewRunner}
 * 
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public class DynamicRefexViewRunner extends Application
{

	/**
	 * @see javafx.application.Application#start(javafx.stage.Stage)
	 */
	@Override
	public void start(Stage primaryStage) throws Exception
	{
		primaryStage.setTitle("Sememe View");

		DynamicSememeView refexView = AppContext.getService(DynamicSememeView.class);
		refexView.setComponent(IsaacMetadataConstants.DYNAMIC_SEMEME_EXTENSION_DEFINITION.getNid(), null, null, null, true);

		primaryStage.setScene(new Scene(refexView.getView(), 800, 600));

		primaryStage.show();
	}

	public static void main(String[] args) throws Exception
	{
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
