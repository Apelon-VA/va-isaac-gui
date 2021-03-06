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
package gov.va.isaac.drools.refexUtils;

import gov.va.isaac.drools.manager.DroolsExecutorsManager;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeDataType;

/**
 * {@link SememeDroolsValidatorImplInfo}
 * 
 * Stores various information and mappings about the known .drl files that are shipped with the application, so that the
 * GUI can dynamically do useful things with them.
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a> 
 */
public enum SememeDroolsValidatorImplInfo
{
	SEMEME_STRING_RULES("builtin.refex-string-rules", "Drools Rules for String values", DynamicSememeDataType.STRING),
	SEMEME_CONCEPT_RULES("builtin.refex-concept-rules", "Drools Rules for Concept values", DynamicSememeDataType.UUID, DynamicSememeDataType.NID);
	
	private String droolsPackageName_, displayName_;
	private DynamicSememeDataType[] applicableDataTypes_;
	
	private SememeDroolsValidatorImplInfo(String droolsPackageName, String displayName, DynamicSememeDataType ... applicableDataTypes)
	{
		displayName_ = displayName;
		droolsPackageName_ = droolsPackageName;
		applicableDataTypes_ = applicableDataTypes;
	}
	
	public static SememeDroolsValidatorImplInfo getByDroolsPackageName(String droolsPackageName)
	{
		for (SememeDroolsValidatorImplInfo rdv : SememeDroolsValidatorImplInfo.values())
		{
			if (rdv.getDroolsPackageName().equals(droolsPackageName))
			{
				return rdv;
			}
		}
		return null;
	}

	/**
	 * @return the droolsPackageName - used for mapping to {@link DroolsExecutorsManager#getDroolsExecutor(String)}
	 */
	public String getDroolsPackageName()
	{
		return droolsPackageName_;
	}

	/**
	 * @return the displayName - A user friendly name for GUI useage
	 */
	public String getDisplayName()
	{
		return displayName_;
	}

	/**
	 * @return the applicableDataTypes - which data type will the drools package run on (drools packages ignore datatypes they don't know about)
	 */
	public DynamicSememeDataType[] getApplicableDataTypes()
	{
		return applicableDataTypes_;
	}
}
