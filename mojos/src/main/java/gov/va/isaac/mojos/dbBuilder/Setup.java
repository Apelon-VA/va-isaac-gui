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
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gov.va.isaac.mojos.dbBuilder;

import gov.va.isaac.AppContext;
import gov.va.isaac.config.profiles.UserProfileManager;
import gov.va.isaac.init.SystemInit;
import gov.vha.isaac.ochre.api.ConfigurationService;
import gov.vha.isaac.ochre.api.LookupService;
import java.io.File;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Goal which opens (and creates if necessary) an OTF Versioning Store DB.
 */
@Mojo (defaultPhase = LifecyclePhase.PROCESS_SOURCES, name = "setup-terminology-store")
public class Setup extends AbstractMojo
{

	/**
	 * See {@link ConfigurationService#setDataStoreFolderPath(java.nio.file.Path) for details on what should
	 * be in the passed in folder location.
	 * 
	 * @parameter
	 * @required
	 */
	@Parameter (required = true)
	private File dataStoreLocation;
	
	/**
	 * Location of the folder that contains the user profiles
	 */
	@Parameter (required = false)
	private File userProfileFolderLocation;

	/**
	 * @see org.apache.maven.plugin.Mojo#execute()
	 */
	@Override
	public void execute() throws MojoExecutionException
	{
		getLog().info("Setup terminology store");
		try
		{
			
			Exception dataStoreLocationInitException = SystemInit.doBasicSystemInit(dataStoreLocation);
			if (dataStoreLocationInitException != null)
			{
				System.err.println("Configuration of datastore path failed.  DB will not be able to start properly!  " + dataStoreLocationInitException);
				System.exit(-1);
			}

			getLog().info("  Setup AppContext, data store location = " + dataStoreLocation.getCanonicalPath());

			LookupService.startupIsaac();
			
			getLog().info("Done setting up terminology store");
			
			getLog().info("Set Automation User");
			AppContext.getService(UserProfileManager.class).configureAutomationMode(userProfileFolderLocation);
		}
		catch (Exception e)
		{
			throw new MojoExecutionException("Database build failure", e);
		}
	}
	
	
	public void setDataStoreLocation(File inputBdbFolderlocation) {
		dataStoreLocation = inputBdbFolderlocation;
	}
	
	public void setUserProfileFolderLocation(File inputUserProfileLocation) {
		userProfileFolderLocation = inputUserProfileLocation;
	}
	
}
