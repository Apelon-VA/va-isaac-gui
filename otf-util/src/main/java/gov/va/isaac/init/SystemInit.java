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
package gov.va.isaac.init;

import gov.va.isaac.util.DBLocator;
import static gov.vha.isaac.cradle.concept.ConceptProviderOchreModel.CRADLE_CONCEPT_MODEL_PROPERTY;
import static gov.vha.isaac.cradle.concept.ConceptProviderOchreModel.CRADLE_PROPERTIES_FILE_NAME;
import gov.vha.isaac.ochre.api.ConceptModel;
import gov.vha.isaac.ochre.api.ConfigurationService;
import gov.vha.isaac.ochre.api.LookupService;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import org.slf4j.LoggerFactory;

/**
 * {@link SystemInit}
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public class SystemInit
{
	/**
	 * Performs the basic init of ISAAC related applications.  Configures SLF4J Logging, configures the HK2 looker from OTF properly, 
	 * configures the DB location paths of OTF properly.
	 * @param dbLocation - optional - if not provided - uses System properties, if set, or the defaults.
	 * @return - null, if no issue configuring the data store paths - otherwise - the exception that happened while configuring the datastore paths.
	 * @throws Exception - if some other unexpected event happens
	 */
	public static Exception doBasicSystemInit(File dbLocation) throws Exception
	{
		//Configure Java logging into log4j2
		System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");
		
		//Make sure the service Locator comes up ok
		LookupService.get();
		
		LookupService.startupWorkExecutors();

		try
		{
			if (dbLocation != null)
			{
				configDataStorePaths(dbLocation);
			}
			//Otherwise, we do nothing, and it falls back to default cradle behavior, which relies on system properties 
		}
		catch (Exception e)
		{
			System.err.println("Configuration of datastore path failed.  DB will not be able to start properly!  " + e);
			return e;
		}
		return null;
	}

	public static void configDataStorePaths(File dbLocation) throws IOException
	{
		File dataStoreLocation = DBLocator.findDBFolder(dbLocation);

		if (!dataStoreLocation.exists())
		{
			throw new FileNotFoundException("Couldn't find a data store from the input of '" + dataStoreLocation.getAbsoluteFile().getAbsolutePath() + "'");
		}
		if (!dataStoreLocation.isDirectory())
		{
			throw new IOException("The specified data store: '" + dataStoreLocation.getAbsolutePath() + "' is not a folder");
		}
		LoggerFactory.getLogger(SystemInit.class).info("Configuring cradle to use the data store " + dataStoreLocation.getAbsolutePath());
		LookupService.getService(ConfigurationService.class).setDataStoreFolderPath(dataStoreLocation.toPath());
		Path propertiesPath = Paths.get(dataStoreLocation.getAbsolutePath(), "object-chronicles", CRADLE_PROPERTIES_FILE_NAME);
		LoggerFactory.getLogger(SystemInit.class).debug("Looking for Cradle properties file \"{}\"...", propertiesPath.toAbsolutePath());
		ConceptModel model = null;
		if (propertiesPath.toFile().exists()) {
			LoggerFactory.getLogger(SystemInit.class).debug("Found Cradle properties file \"{}\".", propertiesPath.toAbsolutePath());
			Properties dataStoreProperties = new Properties();
			try (FileInputStream in = new FileInputStream(propertiesPath.toFile())) {
				dataStoreProperties.load(in);
			}
			final String cradleConceptModelPropertyValue = dataStoreProperties.getProperty(CRADLE_CONCEPT_MODEL_PROPERTY);
			LoggerFactory.getLogger(SystemInit.class).debug("Loaded CRADLE_CONCEPT_MODEL_PROPERTY value \"{}\".", cradleConceptModelPropertyValue);
			model = ConceptModel.valueOf(cradleConceptModelPropertyValue);
		} else {
			model = ConceptModel.OTF_CONCEPT_MODEL;
		}
		LoggerFactory.getLogger(SystemInit.class).debug("Setting ConceptModel to \"{}\".", model.name());

		LookupService.getService(ConfigurationService.class).setConceptModel(model);
	}
}
