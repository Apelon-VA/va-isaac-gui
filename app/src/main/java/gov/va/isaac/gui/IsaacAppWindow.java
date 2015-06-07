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

import javafx.concurrent.Task;
import javafx.stage.Stage;
import javax.inject.Singleton;
import org.glassfish.hk2.api.Rank;
import org.jvnet.hk2.annotations.Service;
import gov.va.isaac.interfaces.gui.ApplicationWindowI;
import gov.va.isaac.interfaces.gui.views.DockedViewI;

/**
 * This entire Class is a hack that shouldn't need to exist...
 * https://java.net/jira/browse/HK2-255
 * 
 * {@link IsaacAppWindow}
 * 
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */

@Service
@Singleton
@Rank(value = 100)
public class IsaacAppWindow implements ApplicationWindowI
{
	private App app_;

	private IsaacAppWindow()
	{
		//For HK2
	}

	protected void setAppRef(App app)
	{
		app_ = app;
	}

	/**
	 * @see gov.va.isaac.interfaces.gui.ApplicationWindowI#getPrimaryStage()
	 */
	@Override
	public Stage getPrimaryStage()
	{
		return app_.primaryStage_;
	}

	/**
	 * @see gov.va.isaac.interfaces.gui.ApplicationWindowI#ensureDockedViewIsVisble(gov.va.isaac.interfaces.gui.views.DockedViewI)
	 */
	@Override
	public void ensureDockedViewIsVisble(DockedViewI view)
	{
		app_.controller.ensureDockedViewIsVisible(view);
	}

	/**
	 * @see gov.va.isaac.interfaces.gui.ApplicationWindowI#browseURL(java.lang.String)
	 */
	@Override
	public void browseURL(String url)
	{
		app_.getHostServices().showDocument(url);
	}

	@Override
	public void addBackgroundTask(Task<?> task)
	{
		app_.controller.addBackgroundTask(task);
	}
}
