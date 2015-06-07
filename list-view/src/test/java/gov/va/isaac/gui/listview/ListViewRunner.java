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
package gov.va.isaac.gui.listview;

import gov.va.isaac.AppContext;
import gov.va.isaac.config.profiles.UserProfileManager;
import gov.va.isaac.init.SystemInit;
import gov.va.isaac.interfaces.gui.ApplicationWindowI;
import gov.va.isaac.interfaces.gui.views.DockedViewI;
import java.io.File;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.glassfish.hk2.api.Rank;
import org.jvnet.hk2.annotations.Service;

/**
 * {@link ListViewRunner}
 * 
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
@Service
@Rank (value = -50)
public class ListViewRunner extends Application implements ApplicationWindowI
{
	Stage primaryStage_;
	/**
	 * @see javafx.application.Application#start(javafx.stage.Stage)
	 */
	@Override
	public void start(Stage primaryStage) throws Exception
	{
		primaryStage_ = primaryStage;
		ListBatchView lv = AppContext.getService(ListBatchView.class);
		primaryStage.setScene(new Scene(lv.getView(), 1024, 800));
		primaryStage.show();
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

	/**
	 * @see gov.va.isaac.interfaces.gui.ApplicationWindowI#getPrimaryStage()
	 */
	@Override
	public Stage getPrimaryStage()
	{
		return primaryStage_;
	}

	/**
	 * @see gov.va.isaac.interfaces.gui.ApplicationWindowI#ensureDockedViewIsVisble(gov.va.isaac.interfaces.gui.views.DockedViewI)
	 */
	@Override
	public void ensureDockedViewIsVisble(DockedViewI view)
	{
		
	}

	/**
	 * @see gov.va.isaac.interfaces.gui.ApplicationWindowI#browseURL(java.lang.String)
	 */
	@Override
	public void browseURL(String url)
	{
	}


	@Override
	public void addBackgroundTask(Task<?> task)
	{
	}
}
