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
package gov.va.isaac.logic.treeview;

import gov.va.isaac.AppContext;
import gov.va.isaac.config.profiles.UserProfileManager;
import gov.va.isaac.init.SystemInit;
import gov.va.isaac.interfaces.gui.views.commonFunctionality.LogicalExpressionTreeGraphEmbeddableViewI;
import gov.vha.isaac.ochre.api.LookupService;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.UUID;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import org.ihtsdo.otf.query.lucene.LuceneIndexer;
import org.ihtsdo.otf.tcc.api.metadata.binding.RefexDynamic;
import org.ihtsdo.otf.tcc.api.store.TerminologyStoreDI;
import org.ihtsdo.otf.tcc.lookup.Hk2Looker;

/**
 * {@link LogicalExpressionTreeGraphViewRunner}
 * 
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public class LogicalExpressionTreeGraphViewRunner extends Application
{

	/**
	 * @see javafx.application.Application#start(javafx.stage.Stage)
	 */
	@Override
	public void start(Stage primaryStage) throws Exception
	{
		primaryStage.setTitle("LogicalExpressionTreeGraphView");
		
		/*
		 * Bleeding (finding) 89ce6b87-545b-3138-82c7-aafa76f8f9a0
		 * Fracture of radius c50138b9-70ee-3af2-b567-af2f20359925
		 * Entire skin (body structure) cbb0653c-bc87-37b0-aafb-6d020917e172
		 * Hand pain 9549a066-7d57-371d-8958-82a6a0b5b175
		 * Arthroscopy (procedure) 4bf05b37-076a-3a6a-ad53-b10bbf83cfc5
		 */

		LogicalExpressionTreeGraphEmbeddableViewI view = AppContext.getService(LogicalExpressionTreeGraphEmbeddableViewI.class);
		view.setConcept(UUID.fromString("4bf05b37-076a-3a6a-ad53-b10bbf83cfc5"));

		primaryStage.setScene(new Scene(view.getView(), 800, 600));

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
		AppContext.getService(UserProfileManager.class).configureAutomationMode(new File("profiles"));
		LookupService.startupIsaac();
		launch(args);
	}

}
