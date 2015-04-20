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
package gov.va.isaac;

import gov.va.isaac.interfaces.RuntimeGlobalsI;
import gov.va.isaac.interfaces.config.IsaacAppConfigI;
import gov.va.isaac.interfaces.gui.ApplicationWindowI;
import gov.va.isaac.interfaces.gui.CommonDialogsI;
import gov.va.isaac.interfaces.gui.constants.SharedServiceNames;
import gov.va.isaac.interfaces.gui.views.commonFunctionality.PopupConceptViewI;
import gov.vha.isaac.ochre.api.LookupService;
import org.glassfish.hk2.api.ServiceLocator;

/**
 * AppContext
 *
 * Provides convenience methods for retrieving implementations of various interfaces
 * from the HK2 dependency management system.
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public class AppContext
{
	public static IsaacAppConfigI getAppConfiguration()
	{
		return getService(IsaacAppConfigI.class);
	}

	public static CommonDialogsI getCommonDialogs()
	{
		return getService(CommonDialogsI.class);
	}

	public static ApplicationWindowI getMainApplicationWindow()
	{
		return getService(ApplicationWindowI.class);
	}
	
	public static RuntimeGlobalsI getRuntimeGlobals()
	{
		return getService(RuntimeGlobalsI.class);
	}

	public static PopupConceptViewI createConceptViewWindow()
	{
		return LookupService.getNamedServiceIfPossible(PopupConceptViewI.class, SharedServiceNames.LEGACY_STYLE);
	}
	
	//These methods below are just convenience methods at this point, all of the logic that used to be in this class to manage the service
	//locator has been moved down into the LookupService class.
	
	/**
	 * @return {@link LookupService#get()}
	 */
	public static ServiceLocator getServiceLocator()
	{
		return LookupService.get();
	}
	
	/**
	 * @return {@link LookupService#getService(Class)}
	 */
	public static <T> T getService(Class<T> contractOrService)
	{
		return LookupService.getService(contractOrService);
	}
}
