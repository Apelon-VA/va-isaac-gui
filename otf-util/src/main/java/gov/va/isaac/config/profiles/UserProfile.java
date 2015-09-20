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
package gov.va.isaac.config.profiles;

import gov.va.isaac.AppContext;
import gov.va.isaac.config.generated.RoleOption;
import gov.va.isaac.config.generated.StatedInferredOptions;
import gov.va.isaac.config.profiles.UserProfileBindings.RelationshipDirection;
import gov.va.isaac.util.PasswordHasher;
import gov.va.isaac.util.json.InterfaceAdapter;
import gov.vha.isaac.ochre.api.coordinate.LanguageCoordinate;
import gov.vha.isaac.ochre.model.coordinate.LanguageCoordinateImpl;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.ihtsdo.otf.tcc.api.coordinate.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * {@link UserProfile}
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public class UserProfile implements UserProfileI
{
	private static Logger logger = LoggerFactory.getLogger(UserProfile.class);

	//This is a cache of what they typed when the logged in - so it can be used later for logging into workflow, or sync.
	private transient char[] clearTextPassword;
	
	//What we actually perform login comparisons against - what is stored in the user prefs file.

	private String hashedPassword;

	private String userLogonName;
	
	private UUID conceptUUID;

	private StatedInferredOptions statedInferredPolicy = UserProfileDefaults.getDefaultStatedInferredPolicy();
	
	private RelationshipDirection displayRelDirection = UserProfileDefaults.getDefaultDisplayRelDirection();
	
	private String workflowUsername = null;
	
	//This field will be encrypted using the clearTextPassword
	private String workflowPasswordEncrypted = null;
	
	private String syncUsername = null;
	
	//This field will be encrypted using the clearTextPassword
	private String syncPasswordEncrypted = null;
	
	private boolean launchWorkflowForEachCommit = UserProfileDefaults.getDefaultLaunchWorkflowForEachCommit();
	
	private boolean runDroolsBeforeEachCommit = UserProfileDefaults.getDefaultRunDroolsBeforeEachCommit();
	
	private String workflowServerDeploymentId = null;

	private UUID viewCoordinatePath = null;
	
	private Long viewCoordinateTime = null;
	
	private UUID editCoordinatePath = null;
	
	private UUID editCoordinateModule = null;

	private UUID workflowPromotionPath = null;

	private String workflowServerUrl = null;

	private String changeSetUrl = null;

	private String releaseVersion = null;

	private String extensionNamespace = null;

	private Status[] viewCoordinateStatuses = null;

	private UUID[] viewCoordinateModules = null;
	
	private LanguageCoordinate languageCoordinate = null;
	
	/*
	 *  *** Update clone() method when adding parameters
	 *  
	 */
	@Override
	protected UserProfile clone()
	{
		UserProfile clone = new UserProfile();
		clone.userLogonName = this.userLogonName;
		clone.clearTextPassword = this.clearTextPassword;
		clone.hashedPassword = this.hashedPassword;
		clone.statedInferredPolicy = this.statedInferredPolicy;
		clone.languageCoordinate = (this.languageCoordinate == null ? null : 
			new LanguageCoordinateImpl(this.languageCoordinate.getLanguageConceptSequence(), this.languageCoordinate.getDialectAssemblagePreferenceList(),
					this.languageCoordinate.getDescriptionTypePreferenceList()));
		clone.displayRelDirection = this.displayRelDirection;
		clone.syncPasswordEncrypted = this.syncPasswordEncrypted;
		clone.syncUsername = this.syncUsername;
		clone.workflowPasswordEncrypted = this.workflowPasswordEncrypted;
		clone.workflowUsername = this.workflowUsername;
		clone.conceptUUID = this.conceptUUID;
		clone.launchWorkflowForEachCommit = this.launchWorkflowForEachCommit;
		clone.runDroolsBeforeEachCommit = this.runDroolsBeforeEachCommit;
		clone.workflowServerDeploymentId = this.workflowServerDeploymentId;
		clone.viewCoordinatePath = this.viewCoordinatePath;
		clone.viewCoordinateTime = this.viewCoordinateTime;
		clone.editCoordinatePath = this.editCoordinatePath;
		clone.editCoordinateModule = this.editCoordinateModule;
		clone.workflowPromotionPath = this.workflowPromotionPath;
		clone.workflowServerUrl = this.workflowServerUrl;
		clone.changeSetUrl = this.changeSetUrl;
		clone.releaseVersion = this.releaseVersion;
		clone.extensionNamespace = this.extensionNamespace;
		if (this.viewCoordinateStatuses != null) {
			clone.viewCoordinateStatuses = Arrays.copyOf(this.viewCoordinateStatuses, this.viewCoordinateStatuses.length);
		} else {
			clone.viewCoordinateStatuses = null;
		}
		if (this.viewCoordinateModules != null) {
			clone.viewCoordinateModules = new UUID[this.viewCoordinateModules.length];
			for (int i = 0; i < this.viewCoordinateModules.length; ++i) {
				clone.viewCoordinateModules[i] = UUID.fromString(this.viewCoordinateModules[i].toString());
			}
		} else {
			clone.viewCoordinateModules = null;
		}

		return clone;
	}

	/**
	 * do not use - only for jaxb
	 */
	private UserProfile()
	{
		//for jaxb and clone
	}

	/**
	 * Do not call - use {@link UserProfileManager#createUserProfile(gov.va.isaac.config.generated.User, gov.va.isaac.config.generated.NewUserDefaults)}
	 * 
	 * Only public due to a testing quirk  - BaseTest - in workflow needs this, as may some JUnit tests, eventually.
	 */
	public UserProfile(String userLogonName, String password, UUID conceptUUID)
	{
		this.userLogonName = userLogonName;
		this.conceptUUID = conceptUUID;
		try
		{
			this.hashedPassword = PasswordHasher.getSaltedHash(password);
			this.clearTextPassword = password.toCharArray();
		}
		catch (Exception e)
		{
			logger.error("Unexpected error hashing password", e);
			this.hashedPassword = "foo";
		}
	}

	@Override
	public boolean isCorrectPassword(String password)
	{
		try
		{
			boolean result = PasswordHasher.check(password, hashedPassword);
			if (result)
			{
				clearTextPassword = password.toCharArray();
			}
			return result;
		}
		catch (Exception e)
		{
			logger.error("Unexpected error validating password", e);
			return false;
		}
	}

	/**
	 * This call sets both the clearTextPassword field, and the hashedPassword field - hashing as appropriate.
	 * 
	 * This call saves the changes to the preferences file.
	 */
	@Override
	public void setPassword(String currentPassword, String newPassword) throws InvalidPasswordException
	{
		if (!isCorrectPassword(currentPassword))
		{
			throw new InvalidPasswordException("Incorrect current password");
		}
		if (newPassword == null || newPassword.length() == 0)
		{
			throw new InvalidPasswordException("The password must be provided");
		}
		try
		{
			//Need to decrypt and reencrypt the workflow and sync passwords, since these are encrypted with the clearTextPassword.
			String wfPass = getWorkflowPassword();
			String syncPass = getSyncPassword();
			
			this.clearTextPassword = newPassword.toCharArray();
			this.hashedPassword = PasswordHasher.getSaltedHash(newPassword);
			
			if (wfPass != null)
			{
				setWorkflowPassword(wfPass);
			}
			if (syncPass != null)
			{
				setSyncPassword(syncPass);
			}
			AppContext.getService(UserProfileManager.class).saveChanges(this);
		}
		catch (Exception e)
		{
			logger.error("Unexpected error hashing password", e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public String getUserLogonName()
	{
		return userLogonName;
	}

	@Override
	public StatedInferredOptions getStatedInferredPolicy()
	{
		return statedInferredPolicy;
	}

	@Override
	public void setStatedInferredPolicy(StatedInferredOptions statedInferredPolicy)
	{
		this.statedInferredPolicy = statedInferredPolicy;
	}

	@Override
	public void setLanguageCoordinate(LanguageCoordinate languageCoordinate)
	{
		this.languageCoordinate = languageCoordinate;
	}

	@Override
	public LanguageCoordinate getLanguageCoordinate()
	{
		if (languageCoordinate == null)
		{
			languageCoordinate = UserProfileDefaults.getDefaultLanguageCoordinate();
		}
		return languageCoordinate;
	}

	@Override
	public void setDisplayRelDirection(RelationshipDirection displayRelationshipDirection)
	{
		this.displayRelDirection = displayRelationshipDirection;
	}

	@Override
	public RelationshipDirection getDisplayRelDirection()
	{
		return displayRelDirection;
	}

	@Override
	public String getWorkflowUsername()
	{
		return workflowUsername;
	}

	@Override
	public void setWorkflowUsername(String workflowUsername)
	{
		this.workflowUsername = workflowUsername;
	}

	@Override
	public String getSyncUsername()
	{
		return syncUsername;
	}

	@Override
	public void setSyncUsername(String syncUsername)
	{
		this.syncUsername = syncUsername;
	}

	@Override
	public void setWorkflowPassword(String workflowPassword)
	{
		if (clearTextPassword == null)
		{
			throw new RuntimeException("Cannot encrypt a workflow password until successfully logged in");
		}
		try
		{
			this.workflowPasswordEncrypted = PasswordHasher.encrypt(new String(clearTextPassword), workflowPassword);
		}
		catch (Exception e)
		{
			logger.error("Unexpected error encrypting password", e);
			throw new RuntimeException("Unexpected error encrypting workflow password");
		}
	}

	@Override
	public String getWorkflowPassword() throws InvalidPasswordException
	{
		if (clearTextPassword == null)
		{
			throw new RuntimeException("Cannot decrypt a workflow password until successfully logged in");
		}
		if (workflowPasswordEncrypted == null)
		{
			return null;
		}
		try
		{
			return PasswordHasher.decryptToString(new String(clearTextPassword), this.workflowPasswordEncrypted);
		}
		catch (Exception e)
		{
			throw new InvalidPasswordException("Invalid password for decrypting the workflow password");
		}
	}

	@Override
	public void setSyncPassword(String syncPassword)
	{
		if (clearTextPassword == null)
		{
			throw new RuntimeException("Cannot encrypt a sync password until successfully logged in");
		}
		try
		{
			this.syncPasswordEncrypted = PasswordHasher.encrypt(new String(clearTextPassword), syncPassword);
		}
		catch (Exception e)
		{
			logger.error("Unexpected error encrypting password", e);
			throw new RuntimeException("Unexpected error encrypting sync password");
		}
	}

	@Override
	public String getSyncPassword()
	{
		if (clearTextPassword == null)
		{
			throw new RuntimeException("Cannot decrypt a sync password until successfully logged in");
		}
		if (syncPasswordEncrypted == null)
		{
			return null;
		}
		try
		{
			return PasswordHasher.decryptToString(new String(clearTextPassword), this.syncPasswordEncrypted);
		}
		catch (Exception e)
		{
			throw new InvalidPasswordException("Invalid password for decrypting the sync password");
		}
	}
	
	/**
	 * The UUID of the concept in the DB that represents this user.
	 */
	@Override
	public UUID getConceptUUID()
	{
		return conceptUUID;
	}

	@Override
	public boolean hasRole(RoleOption role)
	{
		if (role == null)
		{
			return false;
		}
		//TODO implement role checking - probably store these on the user concept in a refex?
		return true;
	}

	/**
	 * @return the launchWorkflowForEachCommit
	 */
	@Override
	public boolean isLaunchWorkflowForEachCommit()
	{
		return launchWorkflowForEachCommit;
	}

	/**
	 * @param launchWorkflowForEachCommit the launchWorkflowForEachCommit to set
	 */
	@Override
	public void setLaunchWorkflowForEachCommit(boolean launchWorkflowForEachCommit)
	{
		this.launchWorkflowForEachCommit = launchWorkflowForEachCommit;
	}

	/**
	 * @return the runDroolsBeforeEachCommit
	 */
	@Override
	public boolean isRunDroolsBeforeEachCommit()
	{
		return runDroolsBeforeEachCommit;
	}

	/**
	 * @param runDroolsBeforeEachCommit the runDroolsBeforeEachCommit to set
	 */
	@Override
	public void setRunDroolsBeforeEachCommit(boolean runDroolsBeforeEachCommit)
	{
		this.runDroolsBeforeEachCommit = runDroolsBeforeEachCommit;
	}

	/**
	 * @return workflowServerDeploymentId
	 */
	@Override
	public String getWorkflowServerDeploymentId()
	{
		if (StringUtils.isBlank(workflowServerDeploymentId))
		{
			return UserProfileDefaults.getDefaultWorkflowServerDeploymentId();
		}
		return workflowServerDeploymentId;
	}
	/**
	 * @param workflowServerDeploymentId
	 */
	@Override
	public void setWorkflowServerDeploymentId(String workflowServerDeploymentId)
	{
		this.workflowServerDeploymentId = workflowServerDeploymentId;
	}

	@Override
	public Long getViewCoordinateTime() {
		if(viewCoordinateTime == null) {
			return UserProfileDefaults.getDefaultViewCoordinateTime();
		}
		return viewCoordinateTime;
	}

	@Override
	public void setViewCoordinateTime(Long time) {
		this.viewCoordinateTime = time;
	}
	
	/**
	 * @return viewCoordinatePath
	 */
	@Override
	public UUID getViewCoordinatePath()
	{
		if (viewCoordinatePath == null)
		{
			return UserProfileDefaults.getDefaultViewCoordinatePath();
		}
		return viewCoordinatePath;
	}
	/**
	 * @param viewCoordinatePath
	 */
	@Override
	public void setViewCoordinatePath(UUID viewCoordinatePath)
	{
		this.viewCoordinatePath = viewCoordinatePath;
	}

	/**
	 * @return editCoordinatePath
	 */
	@Override
	public UUID getEditCoordinatePath()
	{
		if (editCoordinatePath == null)
		{
			return UserProfileDefaults.getDefaultEditCoordinatePath();
		}
		return editCoordinatePath;
	}
	/**
	 * @param editCoordinatePath
	 */
	@Override
	public void setEditCoordinatePath(UUID editCoordinatePath)
	{
		this.editCoordinatePath = editCoordinatePath;
	}
	
	/**
	 * @return editCoordinatePath
	 */
	@Override
	public UUID getEditCoordinateModule()
	{
		if (editCoordinateModule == null)
		{
			return UserProfileDefaults.getDefaultEditCoordinateModule();
		}
		return editCoordinateModule;
	}
	/**
	 * @param editCoordinatePath
	 */
	@Override
	public void setEditCoordinateModule(UUID editCoordinateModule)
	{
		this.editCoordinateModule = editCoordinateModule;
	}

	/**
	 * @return workflowPromotionPath
	 */
	@Override
	public UUID getWorkflowPromotionPath()
	{
		if (workflowPromotionPath == null)
		{
			return UserProfileDefaults.getDefaultWorkflowPromotionPath();
		}
		return workflowPromotionPath;
	}
	/**
	 * @param workflowPromotionPath
	 */
	@Override
	public void setWorkflowPromotionPath(UUID workflowPromotionPath)
	{
		this.workflowPromotionPath = workflowPromotionPath;
	}
	
	/**
	 * @return workflowServerUrl
	 */
	@Override
	public String getWorkflowServerUrl()
	{
		if (StringUtils.isBlank(workflowServerUrl))
		{
			return UserProfileDefaults.getDefaultWorkflowServerUrl();
		}
		return workflowServerUrl;
	}
	/**
	 * @param workflowServerUrl
	 */
	@Override
	public void setWorkflowServerUrl(String workflowServerUrl)
	{
		this.workflowServerUrl = workflowServerUrl;
	}

	/**
	 * @return changeSetUrl
	 */
	@Override
	public String getChangeSetUrl()
	{
		if (StringUtils.isBlank(changeSetUrl))
		{
			return UserProfileDefaults.getDefaultChangeSetUrl();
		}
		return changeSetUrl;
	}
	/**
	 * @param changeSetUrl
	 */
	@Override
	public void setChangeSetUrl(String changeSetUrl)
	{
		this.changeSetUrl = changeSetUrl;
	}

	/**
	 * @return releaseVersion
	 */
	@Override
	public String getReleaseVersion()
	{
		if (StringUtils.isBlank(releaseVersion))
		{
			return UserProfileDefaults.getDefaultReleaseVersion();
		}
		return releaseVersion;
	}
	/**
	 * @param releaseVersion
	 */
	@Override
	public void setReleaseVersion(String releaseVersion)
	{
		this.releaseVersion = releaseVersion;
	}

	/**
	 * @return extensionNamespace
	 */
	@Override
	public String getExtensionNamespace()
	{
		if (StringUtils.isBlank(extensionNamespace))
		{
			return UserProfileDefaults.getDefaultExtensionNamespace();
		}
		return extensionNamespace;
	}
	/**
	 * @param extensionNamespace
	 */
	@Override
	public void setExtensionNamespace(String extensionNamespace)
	{
		this.extensionNamespace = extensionNamespace;
	}
	
	/**
	 * @return viewCoordinateStatuses unmodifiable set of viewCoordinateStatus
	 * 
	 * Always returns a unique unmodifiable set of 1 or more Status values.
	 * Returns unmodifiable set of default status values if stored array is null or contains no non-null values
	 */
	@Override
	public Set<Status> getViewCoordinateStatuses()
	{
		Set<Status> statuses = new HashSet<>();
		if (viewCoordinateStatuses != null)
		{
			for (Status status : viewCoordinateStatuses) {
				if (status != null) {
					statuses.add(status);
				}
			}
		}
		
		if (statuses.size() == 0) {
			statuses.addAll(UserProfileDefaults.getDefaultViewCoordinateStatuses());
		}

		return Collections.unmodifiableSet(statuses);
	}

	/**
	 * @param viewCoordinateStatuses set of viewCoordinateStatus
	 * 
	 * Sets a unique set of non-null Status values.
	 * If passed set is null or contains no non-null values then empty array is used.
	 */
	@Override
	public void setViewCoordinateStatuses(Set<Status> viewCoordinateStatusesSet) {
		setViewCoordinateStatuses(viewCoordinateStatusesSet != null ? viewCoordinateStatusesSet.toArray(new Status[viewCoordinateStatusesSet.size()]) : new Status[0]);
	}
	/**
	 * @param viewCoordinateStatuses variable length parameter array of viewCoordinateStatus
	 * 
	 * Sets a unique set of non-null Status values.
	 * If passed parameter array is null or contains no non-null values then empty array is used.
	 */
	public void setViewCoordinateStatuses(Status...viewCoordinateStatusesSet) {
		Set<Status> validPassedStatuses = new HashSet<>();
		if (viewCoordinateStatusesSet != null) {
			for (Status status : viewCoordinateStatusesSet) {
				if (status != null) {
					validPassedStatuses.add(status);
				}
			}
		}
		
		if (validPassedStatuses.size() > 0) {
			viewCoordinateStatuses = validPassedStatuses.toArray(new Status[validPassedStatuses.size()]);
		} else {
			viewCoordinateStatuses = new Status[0];
		}
	}

	/**
	 * @return viewCoordinateModules unmodifiable set of viewCoordinateModules
	 * 
	 * Always returns a unique unmodifiable set of 0 or more module UUIDs.
	 * An empty returned set means NO RESTRICTION for the purposes of filtering.
	 */
	@Override
	public Set<UUID> getViewCoordinateModules() {
		Set<UUID> modules = new HashSet<>();
		if (viewCoordinateModules != null) {
			for (UUID uuid : viewCoordinateModules) {
				if (uuid != null) {
					modules.add(uuid);
				}
			}
		} else {
			modules.addAll(UserProfileDefaults.getDefaultViewCoordinateModules());
		}
		
		return Collections.unmodifiableSet(modules);
	}
	
	/**
	 * @param viewCoordinateModules set of viewCoordinateModule UUIDs
	 * 
	 * Sets a unique set of zero or more non-null UUID values.
	 * If passed set is null or contains no non-null values then empty array is used.
	 */
	@Override
	public void setViewCoordinateModules(Set<UUID> viewCoordinateModulesSet) {
		setViewCoordinateModules(viewCoordinateModulesSet != null ? viewCoordinateModulesSet.toArray(new UUID[viewCoordinateModulesSet.size()]) : new UUID[0]);
	}
	/**
	 * @param viewCoordinateModules variable length parameter array of viewCoordinateModule UUIDs
	 * 
	 * Sets a unique set of zero or more non-null UUID values.
	 * If passed variable length parameter array is null or contains no non-null values then empty array is used.
	 */
	@Override
	public void setViewCoordinateModules(UUID...viewCoordinateModulesSet) {
		Set<UUID> validPassedUuids = new HashSet<>();
		if (viewCoordinateModulesSet != null) {
			for (UUID uuid : viewCoordinateModulesSet) {
				if (uuid != null) {
					validPassedUuids.add(uuid);
				}
			}
		}
		
		if (validPassedUuids.size() > 0) {
			viewCoordinateModules = validPassedUuids.toArray(new UUID[validPassedUuids.size()]);
		} else {
			viewCoordinateModules = new UUID[0];
		}
	}

	// Persistence methods
	protected void store(File fileToWrite) throws IOException
	{
		try
		{
			Gson gson = new GsonBuilder().serializeNulls().setPrettyPrinting()
					.registerTypeAdapter(LanguageCoordinate.class, new InterfaceAdapter<LanguageCoordinate>()).create();
			FileWriter fw = new FileWriter(fileToWrite);
			gson.toJson(this, fw);
			fw.close();
		}
		catch (Exception e)
		{
			throw new IOException("Problem storings UserProfile to " + fileToWrite.getAbsolutePath(), e);
		}
	}

	protected static UserProfile read(File path) throws IOException
	{
		try
		{
			//Register type adapters for classes we serialize that are interfaces...
			Gson gson = new GsonBuilder().registerTypeAdapter(LanguageCoordinate.class, new InterfaceAdapter<LanguageCoordinate>()).create();
			FileReader fr = new FileReader(path);
			UserProfile up = gson.fromJson(fr, UserProfile.class);
			fr.close();
			
			return up;
		}
		catch (Exception e)
		{
			logger.error("Problem reading user profile from " + path.getAbsolutePath(), e);
			throw new IOException("Problem reading user profile", e);
		}
	}
}
