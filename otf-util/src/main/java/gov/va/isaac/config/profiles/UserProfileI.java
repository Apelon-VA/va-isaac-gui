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

import gov.va.isaac.config.generated.RoleOption;
import gov.va.isaac.config.generated.StatedInferredOptions;
import gov.va.isaac.config.profiles.UserProfileBindings.RelationshipDirection;
import gov.vha.isaac.ochre.api.State;
import gov.vha.isaac.ochre.api.coordinate.LanguageCoordinate;

import java.util.Set;
import java.util.UUID;

/**
 * {@link UserProfileI}
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public interface UserProfileI
{
	public boolean isCorrectPassword(String password);

	/**
	 * This call sets both the clearTextPassword field, and the hashedPassword field - hashing as appropriate.
	 * 
	 * This call saves the changes to the preferences file.
	 */
	public void setPassword(String currentPassword, String newPassword) throws InvalidPasswordException;
	public String getUserLogonName();

	public StatedInferredOptions getStatedInferredPolicy();

	public void setStatedInferredPolicy(StatedInferredOptions statedInferredPolicy);
	
	public void setLanguageCoordinate(LanguageCoordinate languageCoordinate);
	
	public LanguageCoordinate getLanguageCoordinate();
	
	public void setDisplayRelDirection(RelationshipDirection displayRelationshipDirection);
	
	public RelationshipDirection getDisplayRelDirection();
	
	public String getWorkflowUsername();

	public void setWorkflowUsername(String workflowUsername);

	public String getSyncUsername();

	public void setSyncUsername(String syncUsername);
	
	public void setWorkflowPassword(String workflowPassword);

	public String getWorkflowPassword() throws InvalidPasswordException;

	public void setSyncPassword(String syncPassword);
	
	public String getSyncPassword();
	
	/**
	 * The UUID of the concept in the DB that represents this user.
	 */
	public UUID getConceptUUID();

	public boolean hasRole(RoleOption role);

	/**
	 * @return the launchWorkflowForEachCommit
	 */
	public boolean isLaunchWorkflowForEachCommit();

	/**
	 * @param launchWorkflowForEachCommit the launchWorkflowForEachCommit to set
	 */
	public void setLaunchWorkflowForEachCommit(boolean launchWorkflowForEachCommit);

	/**
	 * @return the runDroolsBeforeEachCommit
	 */
	public boolean isRunDroolsBeforeEachCommit();

	/**
	 * @param runDroolsBeforeEachCommit the runDroolsBeforeEachCommit to set
	 */
	public void setRunDroolsBeforeEachCommit(boolean runDroolsBeforeEachCommit);

	/**
	 * @return workflowServerDeploymentId
	 */
	public String getWorkflowServerDeploymentId();
	/**
	 * @param workflowServerDeploymentId
	 */
	public void setWorkflowServerDeploymentId(String workflowServerDeploymentId);
	
	public Long getViewCoordinateTime();
	
	public void setViewCoordinateTime(Long time);
	
	/**
	 * @return viewCoordinatePath
	 */
	public UUID getViewCoordinatePath();
	/**
	 * @param viewCoordinatePath
	 */
	public void setViewCoordinatePath(UUID viewCoordinatePath);

	/**
	 * @return editCoordinatePath
	 */
	public UUID getEditCoordinatePath();
	/**
	 * @param editCoordinatePath
	 */
	public void setEditCoordinatePath(UUID editCoordinatePath);
	
	/**
	 * @return editCoordinatePath
	 */
	public UUID getEditCoordinateModule();
	/**
	 * @param editCoordinatePath
	 */
	public void setEditCoordinateModule(UUID editCoordinateModule);

	/**
	 * @return workflowPromotionPath
	 */
	public UUID getWorkflowPromotionPath();
	/**
	 * @param workflowPromotionPath
	 */
	public void setWorkflowPromotionPath(UUID workflowPromotionPath);
	
	/**
	 * @return workflowServerUrl
	 */
	public String getWorkflowServerUrl();
	/**
	 * @param workflowServerUrl
	 */
	public void setWorkflowServerUrl(String workflowServerUrl);

	/**
	 * @return changeSetUrl
	 */
	public String getChangeSetUrl();
	/**
	 * @param changeSetUrl
	 */
	public void setChangeSetUrl(String changeSetUrl);

	/**
	 * @return releaseVersion
	 */
	public String getReleaseVersion();
	/**
	 * @param releaseVersion
	 */
	public void setReleaseVersion(String releaseVersion);

	/**
	 * @return extensionNamespace
	 */
	public String getExtensionNamespace();
	/**
	 * @param extensionNamespace
	 */
	public void setExtensionNamespace(String extensionNamespace);
	
	/**
	 * @return viewCoordinateStatuses unmodifiable set of viewCoordinateStatus
	 * 
	 * Always returns a unique unmodifiable set of 1 or more State values.
	 * Returns unmodifiable set of default status values if stored array is null or contains no non-null values
	 */
	public Set<State> getViewCoordinateStatuses();

	/**
	 * @param viewCoordinateStatuses set of viewCoordinateStatus
	 * 
	 * Sets a unique set of non-null State values.
	 * If passed set is null or contains no non-null values then empty array is used.
	 */
	public void setViewCoordinateStatuses(Set<State> viewCoordinateStatusesSet);
	/**
	 * @param viewCoordinateStatuses variable length parameter array of viewCoordinateStatus
	 * 
	 * Sets a unique set of non-null State values.
	 * If passed parameter array is null or contains no non-null values then empty array is used.
	 */
	public void setViewCoordinateStatuses(State...viewCoordinateStatusesSet);

	/**
	 * @return viewCoordinateModules unmodifiable set of viewCoordinateModules
	 * 
	 * Always returns a unique unmodifiable set of 0 or more module UUIDs.
	 * An empty returned set means NO RESTRICTION for the purposes of filtering.
	 */
	public Set<UUID> getViewCoordinateModules();
	
	/**
	 * @param viewCoordinateModules set of viewCoordinateModule UUIDs
	 * 
	 * Sets a unique set of zero or more non-null UUID values.
	 * If passed set is null or contains no non-null values then empty array is used.
	 */
	public void setViewCoordinateModules(Set<UUID> viewCoordinateModulesSet);
	/**
	 * @param viewCoordinateModules variable length parameter array of viewCoordinateModule UUIDs
	 * 
	 * Sets a unique set of zero or more non-null UUID values.
	 * If passed variable length parameter array is null or contains no non-null values then empty array is used.
	 */
	public void setViewCoordinateModules(UUID...viewCoordinateModulesSet);
}
