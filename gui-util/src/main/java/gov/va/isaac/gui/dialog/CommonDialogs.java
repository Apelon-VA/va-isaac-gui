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
package gov.va.isaac.gui.dialog;

import gov.va.isaac.AppContext;
import gov.va.isaac.gui.util.FxUtils;
import gov.va.isaac.interfaces.gui.ApplicationWindowI;
import gov.va.isaac.interfaces.gui.CommonDialogsI;
import gov.va.isaac.interfaces.gui.views.commonFunctionality.PopupConceptViewI;
import gov.va.isaac.interfaces.utility.DialogResponse;
import java.io.IOException;
import java.util.UUID;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.stage.Window;
import javax.inject.Singleton;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sun.javafx.tk.Toolkit;

/**
 * CommonDialogs
 * 
 * @author ocarlsen
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */

@Service
@Singleton
public class CommonDialogs implements CommonDialogsI
{
	private final Logger LOG = LoggerFactory.getLogger(this.getClass());
	
	private ErrorDialog errorDialog_;
	private InformationDialog informationDialog_;
	private YesNoDialog yesNoDialog_;


	private CommonDialogs() throws IOException
	{
		// hidden - constructed by HK2
	}
	
	private void init()
	{
		if (errorDialog_ == null)
		{
			try
			{
				ApplicationWindowI mainAppWindow = AppContext.getService(ApplicationWindowI.class);
				this.errorDialog_ = new ErrorDialog(mainAppWindow == null ? null : mainAppWindow.getPrimaryStage());
				this.informationDialog_ = new InformationDialog(mainAppWindow == null ? null : mainAppWindow.getPrimaryStage());
				this.yesNoDialog_ = new YesNoDialog(mainAppWindow == null ? null : mainAppWindow.getPrimaryStage());
			}
			catch (IOException e)
			{
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * @see gov.va.isaac.interfaces.gui.CommonDialogsI#showInformationDialog(java.lang.String, java.lang.String)
	 */
	@Override
	public void showInformationDialog(String title, String message)
	{
		// Make sure in application thread.
		FxUtils.checkFxUserThread();
		init();
		informationDialog_.setVariables(title, message);
		informationDialog_.showAndWait();
	}

	/**
	 * @see gov.va.isaac.interfaces.gui.CommonDialogsI#showInformationDialog(java.lang.String, javafx.scene.Node)
	 */
	@Override
	public void showInformationDialog(String title, Node content)
	{
		// Make sure in application thread.
		FxUtils.checkFxUserThread();
		init();
		informationDialog_.setVariables(title, content);
		informationDialog_.showAndWait();
	}
	
	/**
	 * @see gov.va.isaac.interfaces.gui.CommonDialogsI#showErrorDialog(java.lang.String, java.lang.Throwable)
	 */
	@Override
	public void showErrorDialog(String message, Throwable throwable)
	{
		String title = throwable.getClass().getName();
		String details = throwable.getMessage();
		showErrorDialog(title, message, details);
	}

	/**
	 * @see gov.va.isaac.interfaces.gui.CommonDialogsI#showErrorDialog(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public synchronized void showErrorDialog(String title, String message, String details)
	{
		boolean calledFromFXThread = Toolkit.getToolkit().isFxUserThread();
		Runnable r = new Runnable()
		{
			@Override
			public void run()
			{
				ErrorDialog ed;
				init();
				//If we already have our cached one up, create a new one.
				if (errorDialog_.isShowing())
				{
					try
					{
						ed = new ErrorDialog(errorDialog_.getOwner());
					}
					catch (IOException e)
					{
						LOG.error("Unexpected error creating an error dialog!", e);
						throw new RuntimeException("Can't display error dialog!");
					}
				}
				else
				{
					ed = errorDialog_;
				}
		
				ed.setVariables(title, message, details);
				if (!calledFromFXThread)
				{
//					ed.show();  //don't block
//					Platform.runLater(() -> 
//					{
//						ed.toFront();  //bug hack fix
//					});
					LOG.error("Dan needs to fix something! " + message + " : " + details);
				}
				else
				{
					ed.showAndWait();  //block
					LOG.error("Dan needs to fix something! " + message + " : " + details);
				}
			}
		};
		if (calledFromFXThread)
		{
			r.run();
		}
		else
		{
			Platform.runLater(r);
		}
	}

	/**
	 * @see gov.va.isaac.interfaces.gui.CommonDialogsI#showConceptDialog(java.util.UUID)
	 */
	@Override
	public void showConceptDialog(UUID uuid)
	{
		try
		{
			PopupConceptViewI dialog = AppContext.createConceptViewWindow();
			dialog.setConcept(uuid);
			dialog.showView(null);
		}
		catch (Exception ex)
		{
			String message = "Unexpected error displaying concept view";
			LOG.warn(message, ex);
			showErrorDialog("Unexpected Error", message, ex.getMessage());
		}
	}

	/**
	 * @see gov.va.isaac.interfaces.gui.CommonDialogsI#showConceptDialog(int)
	 */
	@Override
	public void showConceptDialog(int conceptNID)
	{
		try
		{
			PopupConceptViewI dialog = AppContext.createConceptViewWindow();
			dialog.setConcept(conceptNID);
			dialog.showView(null);
		}
		catch (Exception ex)
		{
			String message = "Unexpected error displaying concept view";
			LOG.warn(message, ex);
			showErrorDialog("Unexpected Error", message, ex.getMessage());
		}
	}
	
	/**
	 * @see gov.va.isaac.interfaces.gui.CommonDialogsI#showYesNoDialog(java.lang.String, java.lang.String)
	 */
	@Override
	public DialogResponse showYesNoDialog(String title, String question)
	{
		//init();
		//return yesNoDialog_.showYesNoDialog(title, question);
		return showYesNoDialog(title, question, null);
	}

	public DialogResponse showYesNoDialog(String title, String question, Window parentWindow)
	{
		init();
		YesNoDialog ynd = yesNoDialog_;
		if (parentWindow != null) {
			ynd = new YesNoDialog(parentWindow);
		}
		return ynd.showYesNoDialog(title, question);
	}
	
	/**
	 * @see gov.va.isaac.interfaces.gui.CommonDialogsI#showErrorDialog(java.lang.String, java.lang.String, java.lang.String, javafx.stage.Window)
	 */
	@Override
	public void showErrorDialog(String title, String message, String details, Window parentWindow)
	{
		boolean calledFromFXThread = Toolkit.getToolkit().isFxUserThread();
		Runnable r = new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					ErrorDialog ed = new ErrorDialog(parentWindow);
					ed.setVariables(title, message, details);
					if (!calledFromFXThread)
					{
						ed.show();  //don't block
						Platform.runLater(() -> 
						{
							ed.toFront();  //bug hack fix
						});
					}
					else
					{
						ed.showAndWait();  //block
					}
				}
				catch (IOException e)
				{
					LOG.error("Unexpected error creating an error dialog!", e);
				}
			}
		};
		if (calledFromFXThread)
		{
			r.run();
		}
		else
		{
			Platform.runLater(r);
		}
	}
	
	@Override
	public void showInformationDialog(String title, String message, Window parentWindow)
	{
		boolean calledFromFXThread = Toolkit.getToolkit().isFxUserThread();
		Runnable r = new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					InformationDialog id = new InformationDialog(parentWindow);
					id.setVariables(title, message);
					if (!calledFromFXThread)
					{
						id.show();  //don't block
						Platform.runLater(() -> 
						{
							id.toFront();  //bug hack fix
						});
					}
					else
					{
						id.showAndWait();  //block
					}
				}
				catch (IOException e)
				{
					LOG.error("Unexpected error creating an information dialog!", e);
				}
			}
		};
		if (calledFromFXThread)
		{
			r.run();
		}
		else
		{
			Platform.runLater(r);
		}
	}
}
