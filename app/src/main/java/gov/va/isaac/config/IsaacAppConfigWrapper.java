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
 *		 http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gov.va.isaac.config;

import gov.va.isaac.AppContext;
import gov.va.isaac.config.generated.IsaacAppConfig;
import gov.va.isaac.config.profiles.UserProfileManager;
import gov.va.isaac.interfaces.config.IsaacAppConfigI;
import gov.va.isaac.util.OchreUtility;
import gov.vha.isaac.metadata.source.IsaacMetadataAuxiliaryBinding;
import gov.vha.isaac.ochre.api.ConfigurationService;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.inject.Singleton;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * {@link IsaacAppConfigWrapper}
 * 
 * An ugly hack to make the generated (@link IsaacAppConfig} class implement the {@link IsaacAppConfigI}
 * But it also gives us a place to set up some defaults, if the user provided data was unreadable / missing.
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a> 
 */
@Service
@Singleton
public class IsaacAppConfigWrapper extends IsaacAppConfig implements IsaacAppConfigI
{
	private static Logger log_ = LoggerFactory.getLogger(IsaacAppConfigWrapper.class);
	
	//things we read from other pom based property files
	private String dbGroupId, dbArtifactId, dbVersion, dbClassifier, dbType;
	private String scmUrl, isaacVersion, version;
	private String metadataVersion = "?";

	private final Set<Map<String, String>> appLicenses = new HashSet<>();
	private final Set<Map<String, String>> dbLicenses = new HashSet<>();
	
	private final Set<Map<String, String>> dbDependencies = new HashSet<>();
	
	private IsaacAppConfigWrapper()
	{
		//This is contructed by HK2
		//Default values
		setApplicationTitle("Default (unbranded) ISAAC Application");
		
		try
		{
			InputStream in = AppContext.class.getResourceAsStream("/app.xml");
			if (in != null)
			{
				IsaacAppConfig temp = unmarshallStream(in);
				copyHack(temp);
			}
			else
			{
				log_.warn("App configuration file not found, using defaults");
			}
		}
		catch (Exception ex)
		{
			log_.warn("Unexpected error reading app configuration file, using defaults", ex);
		}
		
		//Read in other information from the package (pom.properties file during normal runtime, pom.xml files if running in a dev env)
		try
		{
			AtomicBoolean readDbMetadataFromProperties = new AtomicBoolean(false);
			AtomicBoolean readDbMetadataFromPom = new AtomicBoolean(false);
			AtomicBoolean readAppMetadata = new AtomicBoolean(false);
			
			//Read the db metadata
			Path dbLocation = AppContext.getService(ConfigurationService.class).getChronicleFolderPath().getParent();
			//find the pom.properties file in the hierarchy
			File dbMetadata = new File(dbLocation.toFile(), "META-INF");
			if (dbMetadata.isDirectory())
			{
				Files.walkFileTree(dbMetadata.toPath(), new SimpleFileVisitor<Path>()
				{
					/**
					 * @see java.nio.file.SimpleFileVisitor#visitFile(java.lang.Object, java.nio.file.attribute.BasicFileAttributes)
					 */
					@Override
					public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException
					{
						File f = path.toFile();
						if (f.isFile() && f.getName().toLowerCase().equals("pom.properties"))
						{
							Properties p = new Properties();
							p.load(new FileReader(f));

							dbGroupId = p.getProperty("project.groupId");
							dbArtifactId = p.getProperty("project.artifactId");
							dbVersion = p.getProperty("project.version");
							dbClassifier = p.getProperty("resultArtifactClassifier");
							dbType = p.getProperty("chronicles.type");
							metadataVersion = p.getProperty("isaac-metadata.version");
							readDbMetadataFromProperties.set(true);
							return readDbMetadataFromPom.get() ? FileVisitResult.TERMINATE : FileVisitResult.CONTINUE;
						} else if (f.isFile() && f.getName().toLowerCase().equals("pom.xml")) {
							DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
							DocumentBuilder builder;
							Document dDoc = null;
							XPath xPath = XPathFactory.newInstance().newXPath();

							try {
								builder = domFactory.newDocumentBuilder();

								dDoc = builder.parse(f);
								
								{
									NodeList dbLicensesNodes = ((NodeList) xPath.evaluate("/project/licenses/license/name", dDoc, XPathConstants.NODESET));

									log_.debug("Found {} license names in DB pom.xml", dbLicensesNodes.getLength());
									for (int i = 0; i < dbLicensesNodes.getLength(); i++) {
										Node currentLicenseNameNode = dbLicensesNodes.item(i);
										String name = currentLicenseNameNode.getTextContent();

										Map<String, String> license = new HashMap<>();
										license.put("name", name);
										license.put("url", ((Node)xPath.evaluate("/project/licenses/license[name='" + name + "']/url", dDoc, XPathConstants.NODE)).getTextContent());
										license.put("comments", ((Node)xPath.evaluate("/project/licenses/license[name='" + name + "']/comments", dDoc, XPathConstants.NODE)).getTextContent());

										dbLicenses.add(Collections.unmodifiableMap(license));

										log_.debug("Extracted license \"{}\" from DB pom.xml: {}", name, license.toString());
									}
								}
								
								{
									NodeList dbDependenciesNodes = ((NodeList) xPath.evaluate("/project/dependencies/dependency/artifactId", dDoc, XPathConstants.NODESET));

									log_.debug("Found {} dependency artifactIds in DB pom.xml", dbDependenciesNodes.getLength());
									for (int i = 0; i < dbDependenciesNodes.getLength(); i++) {
										Node currentDbDependencyArtifactIdNode = dbDependenciesNodes.item(i);
										String artifactId = currentDbDependencyArtifactIdNode.getTextContent();

										Map<String, String> dependency = new HashMap<>();
										dependency.put("artifactId", artifactId);
										dependency.put("groupId", ((Node)xPath.evaluate("/project/dependencies/dependency[artifactId='" + artifactId + "']/groupId", dDoc, XPathConstants.NODE)).getTextContent());
										dependency.put("version", ((Node)xPath.evaluate("/project/dependencies/dependency[artifactId='" + artifactId + "']/version", dDoc, XPathConstants.NODE)).getTextContent());
							
										try {
											dependency.put("classifier", ((Node)xPath.evaluate("/project/dependencies/dependency[artifactId='" + artifactId + "']/classifier", dDoc, XPathConstants.NODE)).getTextContent());
										} catch (Throwable t) {
											log_.debug("Problem reading \"classifier\" element for {}", artifactId);
										}
										try {
										dependency.put("type", ((Node)xPath.evaluate("/project/dependencies/dependency[artifactId='" + artifactId + "']/type", dDoc, XPathConstants.NODE)).getTextContent());
										} catch (Throwable t) {
											log_.debug("Problem reading \"type\" element for {}", artifactId);
										}

										dbDependencies.add(dependency);

										log_.debug("Extracted dependency \"{}\" from DB pom.xml: {}", artifactId, dependency.toString());
									}
								}
							} catch (XPathExpressionException | SAXException | ParserConfigurationException e) {
								e.printStackTrace();
								throw new IOException(e);
							}


							readDbMetadataFromPom.set(true);
							return readDbMetadataFromProperties.get() ? FileVisitResult.TERMINATE : FileVisitResult.CONTINUE;
						}

						return FileVisitResult.CONTINUE;
					}
				});
			}
			
			if (!readDbMetadataFromProperties.get())
			{
				log_.warn("Failed to read the metadata about the database from the database package.");
			}
			else
			{
				for (Map<String, String> dependency : dbDependencies)
				{
					if ("${isaac-metadata.version}".equals(dependency.get("version")))
					{
						dependency.put("version", metadataVersion);
					}
				}
				log_.debug("Successfully read db properties from maven config files.  dbGroupId: {} dbArtifactId: {} dbVersion: {} dbClassifier: {} dbType: {}", 
						dbGroupId, dbArtifactId, dbVersion, dbClassifier, dbType);
			}
			
			//read the app metadata
			
			//if running from eclipse - our launch folder should be "app".  Go up one directory, read the pom file.
			File f = new File("").getAbsoluteFile();
			if (f.getName().endsWith("app-assembly"))
			{
				File pom = new File(f.getParent(), "pom.xml");
				if (pom.isFile())
				{
					DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
					DocumentBuilder builder = domFactory.newDocumentBuilder();
					Document dDoc = builder.parse(pom);

					XPath xPath = XPathFactory.newInstance().newXPath();
					isaacVersion = ((Node) xPath.evaluate("/project/properties/va-isaac-gui.version", dDoc, XPathConstants.NODE)).getTextContent();
					scmUrl= ((Node) xPath.evaluate("/project/scm/url", dDoc, XPathConstants.NODE)).getTextContent();
					version= ((Node) xPath.evaluate("/project/version", dDoc, XPathConstants.NODE)).getTextContent();
					
					NodeList appLicensesNodes = ((NodeList) xPath.evaluate("/project/licenses/license/name", dDoc, XPathConstants.NODESET));

					log_.debug("Found {} license names", appLicensesNodes.getLength());
					for (int i = 0; i < appLicensesNodes.getLength(); i++) {
						Node currentLicenseNameNode = appLicensesNodes.item(i);
						String name = currentLicenseNameNode.getTextContent();
						
						Map<String, String> license = new HashMap<>();
						license.put("name", name);
						license.put("url", ((Node)xPath.evaluate("/project/licenses/license[name='" + name + "']/url", dDoc, XPathConstants.NODE)).getTextContent());
						license.put("comments", ((Node)xPath.evaluate("/project/licenses/license[name='" + name + "']/comments", dDoc, XPathConstants.NODE)).getTextContent());
						
						appLicenses.add(Collections.unmodifiableMap(license));
						
						log_.debug("Extracted license \"{}\" from app pom.xml: {}", name, license.toString());
					}

					readAppMetadata.set(true);
				}
			}
			//otherwise, running from an installation - we should have a META-INF folder
			File appMetadata = new File("META-INF");
			if (appMetadata.isDirectory())
			{
				Files.walkFileTree(appMetadata.toPath(), new SimpleFileVisitor<Path>()
				{
					/**
					 * @see java.nio.file.SimpleFileVisitor#visitFile(java.lang.Object, java.nio.file.attribute.BasicFileAttributes)
					 */
					@Override
					public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException
					{
						File visitFile = path.toFile();
						if (visitFile.isFile() && visitFile.getName().toLowerCase().equals("pom.properties"))
						{
							Properties p = new Properties();
							p.load(new FileReader(visitFile));

							scmUrl = p.getProperty("scm.url");
							isaacVersion = p.getProperty("va-isaac-gui.version");
							version = p.getProperty("project.version");
							readAppMetadata.set(true);
							return FileVisitResult.TERMINATE;
						}
						return FileVisitResult.CONTINUE;
					}
					
				});
			}
			
			if (!readAppMetadata.get())
			{
				log_.warn("Failed to read the metadata about the app");
			}
			else
			{
				log_.debug("Successfully read app properties from maven config files.  version: {} scmUrl: {} isaacVersion: {}", version, scmUrl, isaacVersion);
			}
		}
		catch (Exception ex)
		{
			log_.warn("Unexpected error reading app configuration information", ex);
		}
	}
	
	/**
	 * Exposed publicly as a utility method, for reuse in MOJO classes that need to read the app.xml file when the 
	 * rest of the application is not running.
	 * @param in - A stream that will read app.xml - validating to AppConfigSchema.xsd.
	 * @return - The parsed java representation
	 * @throws IOException
	 * @throws JAXBException
	 * @throws SAXException
	 */
	public static IsaacAppConfig unmarshallStream(InputStream in) throws IOException, JAXBException, SAXException
	{
		JAXBContext jaxbContext = JAXBContext.newInstance(IsaacAppConfig.class);
		SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		Schema schema = factory.newSchema(new StreamSource(AppContext.class.getResourceAsStream("/xsd/AppConfigSchema.xsd")));
		Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
		jaxbUnmarshaller.setSchema(schema);
		IsaacAppConfig temp = (IsaacAppConfig) jaxbUnmarshaller.unmarshal(in);
		in.close();
		return temp;
	}

	/**
	 * Copy all of the data from the deserialized data - which doesn't implement our interface - to this instance, which does.
	 *
	 * @param read the read
	 */
	private void copyHack(IsaacAppConfig read)
	{
		if (read.getApplicationTitle() != null && read.getApplicationTitle().length() > 0)
		{
			setApplicationTitle(read.getApplicationTitle());
		}
		setArchetypeGroupId(read.getArchetypeGroupId());
		setArchetypeArtifactId(read.getArchetypeArtifactId());
		setArchetypeVersion(read.getArchetypeVersion());
		setChangeSetUrl(read.getChangeSetUrl());
		setChangeSetUrlType(read.getChangeSetUrlType());
		setWorkflowServerUrl(read.getWorkflowServerUrl());
		setWorkflowServerDeploymentId(read.getWorkflowServerDeploymentId());
		setWorkflowPromotionPathName(read.getWorkflowPromotionPathName());
		setWorkflowPromotionPathUuid(read.getWorkflowPromotionPathUuid());
	}

	/* (non-Javadoc)
	 * @see gov.va.isaac.interfaces.config.IsaacAppConfigI#getDefaultWorkflowServerUrlAsURL()
	 */
	@Override
	public URL getDefaultWorkflowServerUrlAsURL() {
		return IsaacAppConfigI.getUrlForString(getDefaultWorkflowServerUrl());
	}

	/* (non-Javadoc)
	 * @see gov.va.isaac.interfaces.config.IsaacAppConfigI#getScmUrlAsURL()
	 */
	@Override
	public URL getScmUrlAsURL() {
		return IsaacAppConfigI.getUrlForString(getScmUrl());
	}

	/* (non-Javadoc)
	 * @see gov.va.isaac.interfaces.config.IsaacAppConfigI#getPromotionPathAsUUID()
	 */
	@Override
	public UUID getDefaultWorkflowPromotionPathUuidAsUUID() {
		UUID temp = IsaacAppConfigI.getUuidForString(getWorkflowPromotionPathUuid());
		if (temp == null)
		{
			return IsaacMetadataAuxiliaryBinding.MASTER.getPrimodialUuid();
		}
		else
		{
			return temp;
		}
	}

	/*
	 * @see gov.va.isaac.interfaces.config.IsaacAppConfigI#getChangeSetUrlTypeName()
	 */
	@Override
	public String getChangeSetUrlTypeName()
	{
		return getChangeSetUrlType() == null ? null : getChangeSetUrlType().name();
	}

	/**
	 * @see gov.va.isaac.interfaces.config.IsaacAppConfigI#getDefaultChangeSetUrl()
	 */
	@Override
	public String getDefaultChangeSetUrl()
	{
		return getChangeSetUrl();
	}

	/**
	 * @see gov.va.isaac.interfaces.config.IsaacAppConfigI#getCurrentChangeSetUrl()
	 */
	@Override
	public String getCurrentChangeSetUrl()
	{
		return AppContext.getService(UserProfileManager.class).getCurrentlyLoggedInUserProfile().getChangeSetUrl();
	}

	/**
	 * @see gov.va.isaac.interfaces.config.IsaacAppConfigI#getCurrentEditPathName()
	 */
	@Override
	public String getCurrentEditPathName()
	{
		return OchreUtility.getDescription(IsaacAppConfigI.getUuidForString(getCurrentEditPathUuid()), null).get();
	}

	/**
	 * @see gov.va.isaac.interfaces.config.IsaacAppConfigI#getCurrentEditPathUuid()
	 */
	@Override
	public String getCurrentEditPathUuid()
	{
		return AppContext.getService(UserProfileManager.class).getCurrentlyLoggedInUserProfile().getEditCoordinatePath().toString();
	}

	/**
	 * @see gov.va.isaac.interfaces.config.IsaacAppConfigI#getCurrentViewPathName()
	 */
	@Override
	public String getCurrentViewPathName()
	{
		return OchreUtility.getDescription(IsaacAppConfigI.getUuidForString(getCurrentViewPathUuid()), null).get();
	}

	/**
	 * @see gov.va.isaac.interfaces.config.IsaacAppConfigI#getCurrentViewPathUuid()
	 */
	@Override
	public String getCurrentViewPathUuid()
	{
		return AppContext.getService(UserProfileManager.class).getCurrentlyLoggedInUserProfile().getViewCoordinatePath().toString();
	}

	/**
	 * @see gov.va.isaac.interfaces.config.IsaacAppConfigI#getDefaultWorkflowServerUrl()
	 */
	@Override
	public String getDefaultWorkflowServerUrl()
	{
		return getWorkflowServerUrl();
	}

	/**
	 * @see gov.va.isaac.interfaces.config.IsaacAppConfigI#getCurrentWorkflowServerUrl()
	 */
	@Override
	public String getCurrentWorkflowServerUrl()
	{
		return AppContext.getService(UserProfileManager.class).getCurrentlyLoggedInUserProfile().getWorkflowServerUrl();
	}

	/**
	 * @see gov.va.isaac.interfaces.config.IsaacAppConfigI#getCurrentWorkflowServerUrlAsURL()
	 */
	@Override
	public URL getCurrentWorkflowServerUrlAsURL()
	{
		return IsaacAppConfigI.getUrlForString(getCurrentWorkflowServerUrl());
	}

	/**
	 * @see gov.va.isaac.interfaces.config.IsaacAppConfigI#getDefaultWorkflowServerDeploymentId()
	 */
	@Override
	public String getDefaultWorkflowServerDeploymentId()
	{
		return getWorkflowServerDeploymentId();
	}

	/**
	 * @see gov.va.isaac.interfaces.config.IsaacAppConfigI#getCurrentWorkflowServerDeploymentId()
	 */
	@Override
	public String getCurrentWorkflowServerDeploymentId()
	{
		return AppContext.getService(UserProfileManager.class).getCurrentlyLoggedInUserProfile().getWorkflowServerDeploymentId();
	}

	/**
	 * @see gov.va.isaac.interfaces.config.IsaacAppConfigI#getDefaultWorkflowPromotionPathName()
	 */
	@Override
	public String getDefaultWorkflowPromotionPathName()
	{
		return OchreUtility.getDescription(getDefaultWorkflowPromotionPathUuidAsUUID(), null).get();
	}

	/**
	 * @see gov.va.isaac.interfaces.config.IsaacAppConfigI#getCurrentWorkflowPromotionPathName()
	 */
	@Override
	public String getCurrentWorkflowPromotionPathName()
	{
		return OchreUtility.getDescription(getCurrentWorkflowPromotionPathUuidAsUUID(), null).get();
	}

	/**
	 * @see gov.va.isaac.interfaces.config.IsaacAppConfigI#getDefaultWorkflowPromotionPathUuid()
	 */
	@Override
	public String getDefaultWorkflowPromotionPathUuid()
	{
		return getDefaultWorkflowPromotionPathUuidAsUUID().toString();
	}

	/**
	 * @see gov.va.isaac.interfaces.config.IsaacAppConfigI#getCurrentWorkflowPromotionPathUuid()
	 */
	@Override
	public String getCurrentWorkflowPromotionPathUuid()
	{
		UUID current = AppContext.getService(UserProfileManager.class).getCurrentlyLoggedInUserProfile().getWorkflowPromotionPath();
		return current != null ? current.toString() : null;
	}

	/**
	 * @see gov.va.isaac.interfaces.config.IsaacAppConfigI#getCurrentWorkflowPromotionPathUuidAsUUID()
	 */
	@Override
	public UUID getCurrentWorkflowPromotionPathUuidAsUUID()
	{
		return IsaacAppConfigI.getUuidForString(getCurrentWorkflowPromotionPathUuid());
	}

	/**
	 * @see gov.va.isaac.interfaces.config.IsaacAppConfigI#getIsaacVersion()
	 */
	@Override
	public String getIsaacVersion()
	{
		return isaacVersion;
	}

	/**
	 * @see gov.va.isaac.interfaces.config.IsaacAppConfigI#getScmUrl()
	 */
	@Override
	public String getScmUrl()
	{
		return scmUrl;
	}

	/**
	 * @see gov.va.isaac.interfaces.config.IsaacAppConfigI#getDbGroupId()
	 */
	@Override
	public String getDbGroupId()
	{
		return dbGroupId;
	}

	/**
	 * @see gov.va.isaac.interfaces.config.IsaacAppConfigI#getDbArtifactId()
	 */
	@Override
	public String getDbArtifactId()
	{
		return dbArtifactId;
	}

	/**
	 * @see gov.va.isaac.interfaces.config.IsaacAppConfigI#getDbVersion()
	 */
	@Override
	public String getDbVersion()
	{
		return dbVersion;
	}

	/**
	 * @see gov.va.isaac.interfaces.config.IsaacAppConfigI#getDbClassifier()
	 */
	@Override
	public String getDbClassifier()
	{
		return dbClassifier;
	}

	/**
	 * @see gov.va.isaac.interfaces.config.IsaacAppConfigI#getDbType()
	 */
	@Override
	public String getDbType()
	{
		return dbType;
	}

	/**
	 * @see gov.va.isaac.interfaces.config.IsaacAppConfigI#getVersion()
	 */
	@Override
	public String getVersion()
	{
		return version;
	}

	public Set<Map<String, String>> getAppLicenses() {
		return Collections.unmodifiableSet(appLicenses);
	}
	
	public Set<Map<String, String>> getDbLicenses() {
		return Collections.unmodifiableSet(dbLicenses);
	}
	
	public Set<Map<String, String>> getDbDependencies() {
		return Collections.unmodifiableSet(dbDependencies);
	}
}
