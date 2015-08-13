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
package gov.va.isaac.drools.refexUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.inject.Named;
import javax.inject.Singleton;
import org.ihtsdo.otf.tcc.api.store.Ts;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import gov.va.isaac.AppContext;
import gov.va.isaac.drools.helper.ResultsCollector;
import gov.va.isaac.drools.helper.ResultsItem;
import gov.va.isaac.drools.helper.TerminologyHelperDrools;
import gov.va.isaac.drools.manager.DroolsExecutor;
import gov.va.isaac.drools.manager.DroolsExecutorsManager;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeDataBI;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeDataType;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeExternalValidatorBI;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.dataTypes.DynamicSememeArrayBI;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.dataTypes.DynamicSememeNidBI;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.dataTypes.DynamicSememeStringBI;
import gov.vha.isaac.ochre.api.coordinate.StampCoordinate;
import gov.vha.isaac.ochre.api.coordinate.TaxonomyCoordinate;
import gov.vha.isaac.ochre.model.sememe.dataTypes.DynamicSememeArray;
import gov.vha.isaac.ochre.model.sememe.dataTypes.DynamicSememeString;
import gov.vha.isaac.ochre.model.sememe.dataTypes.DynamicSememeUUID;

/**
 * {@link RefexDroolsValidator}
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */

@Service
@Singleton
@Named("RefexDroolsValidator")
public class RefexDroolsValidator implements DynamicSememeExternalValidatorBI
{
	private static Logger logger = LoggerFactory.getLogger(RefexDroolsValidator.class);

	/**
	 * Returns true if the data is valid, otherwise, throws an error
	 * 
	 * @param droolsValidatorName - The validator to use. Must be present in {@link DroolsExecutorsManager#getLoadedExecutors()}
	 * @param dataToValidate - The data to pass into the validator.
	 * @return true if valid. Exception otherwise.
	 * @throws RuntimeException - if the validation fails. User-friendly message will be in the error.
	 */
	public static boolean validate(String droolsValidatorName, DynamicSememeDataBI dataToValidate)
	{
		try
		{
			DroolsExecutorsManager dem = AppContext.getService(DroolsExecutorsManager.class);

			DroolsExecutor de = dem.getDroolsExecutor(droolsValidatorName);

			if (de == null)
			{
				throw new RuntimeException("The requested drools validator '" + droolsValidatorName + "' is not available");
			}

			ArrayList<Object> facts = new ArrayList<>();

			if (dataToValidate.getRefexDataType() == DynamicSememeDataType.NID)
			{
				//switch it to a UUID, for drools purposes.
				UUID temp = Ts.get().getUuidsForNid(((DynamicSememeNidBI)dataToValidate).getDataNid()).get(0);
				facts.add(new DynamicSememeUUID(temp));
			}
			else
			{
				facts.add(dataToValidate);
			}

			Map<String, Object> globals = new HashMap<>();

			ResultsCollector rc = new ResultsCollector();

			globals.put("resultsCollector", rc);
			globals.put("terminologyHelper", new TerminologyHelperDrools());

			int fireCount = de.fireAllRules(globals, facts);

			if (fireCount > 0)
			{
				StringBuilder sb = new StringBuilder();
				for (ResultsItem r : rc.getResultsItems())
				{
					logger.debug("Drools rule fired during sememe validation with severity {}, error code {}, rule ID {}, message {}", r.getSeverity().getName(),
							r.getErrorCode(), r.getRuleUuid(), r.getMessage());
					sb.append(r.getMessage());
					sb.append(", ");
				}

				if (sb.length() > 2)
				{
					sb.setLength(sb.length() - 2);
				}
				else
				{
					logger.error("Oops.  Rule fired - but no message?");
					sb.append("Error - rule fired, but no message?");
				}
				throw new RuntimeException(sb.toString());
			}

		}
		catch (RuntimeException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			logger.error("Unexpected error validating with drools", e);
			throw new RuntimeException("Unexpected error validating with drools");
		}
		return true;
	}

	public static DynamicSememeArray<DynamicSememeString> createValidatorDefinitionData(RefexDroolsValidatorImplInfo rdvii)
	{
		return new DynamicSememeArray<DynamicSememeString>(
				new DynamicSememeString[]{new DynamicSememeString("RefexDroolsValidator"), new DynamicSememeString(rdvii.name())});
	}

	/**
	 * In our implementation - the validatorDefinitionData contains two things - the first - is the @name of this implementation of an
	 * {@link DynamicSememeExternalValidatorBI} - for example "RefexDroolsValidator" - the rest is corresponding name from the 
	 * {@link RefexDroolsValidatorImplInfo} enum String[]{"RefexDroolsValidator", "REFEX_STRING_RULES"}
	 * 
	 * @param validatorDefinitionData
	 * @throws RuntimeException if the input data can't be parsed as expected.
	 * @return - null, if input is null, or no drools impl is mapped to the 2nd part of the data. 
	 */
	public static RefexDroolsValidatorImplInfo readFromData(DynamicSememeDataBI validatorDefinitionData) throws RuntimeException
	{
		try
		{
			if (validatorDefinitionData == null)
			{
				return null;
			}
			@SuppressWarnings("unchecked")
			DynamicSememeStringBI[] validatorInfo = ((DynamicSememeArrayBI<DynamicSememeStringBI>)validatorDefinitionData).getDataArray();
			if (validatorInfo[0].getDataString().equals("RefexDroolsValidator"))
			{
				return RefexDroolsValidatorImplInfo.valueOf(validatorInfo[1].getDataString());
			}
			else
			{
				throw new RuntimeException("The name mapping for the validator does not match this class!");
			}
		}
		catch (ClassCastException | IndexOutOfBoundsException e)
		{
			throw new RuntimeException("The incoming value (" + validatorDefinitionData.getDataObject().toString() + " doesn't match the expected format");
		}
		catch (RuntimeException e)
		{
			throw e;
		}
	}

	/**
	 * @see gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeExternalValidatorBI#validate(org.ihtsdo.otf.tcc.api.refexDynamic.data.DynamicSememeDataBI,
	 * org.ihtsdo.otf.tcc.api.refexDynamic.data.dataTypes.DynamicSememeStringBI, org.ihtsdo.otf.tcc.api.coordinate.ViewCoordinate)
	 */
	@Override
	public boolean validate(DynamicSememeDataBI userData, DynamicSememeArrayBI<DynamicSememeStringBI> validatorDefinitionData, StampCoordinate<?> sc, 
			TaxonomyCoordinate<?> tc) throws RuntimeException
	{
		RefexDroolsValidatorImplInfo rdvi = readFromData(validatorDefinitionData);
		if (rdvi == null)
		{
			throw new RuntimeException("The specified validator is not mapped - cannot validate");
		}

		for (DynamicSememeDataType rddt : rdvi.getApplicableDataTypes())
		{
			if (userData.getRefexDataType() == rddt)
			{
				return RefexDroolsValidator.validate(rdvi.getDroolsPackageName(), userData);
			}
		}

		throw new RuntimeException("The selected drools validator doesn't apply to the datatype '" + userData.getRefexDataType().getDisplayName() + "'");
	}

	/**
	 * @see gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeExternalValidatorBI#validatorSupportsType(org.ihtsdo.otf.tcc.api.refexDynamic.data.dataTypes.DynamicSememeStringBI, org.ihtsdo.otf.tcc.api.refexDynamic.data.DynamicSememeDataType)
	 */
	@Override
	public boolean validatorSupportsType(DynamicSememeArrayBI<DynamicSememeStringBI> validatorDefinitionData, DynamicSememeDataType dataType)
	{
		RefexDroolsValidatorImplInfo rdvi = readFromData(validatorDefinitionData);
		if (rdvi == null)
		{
			throw new RuntimeException("The specified validator is not mapped - cannot validate");
		}

		for (DynamicSememeDataType rddt : rdvi.getApplicableDataTypes())
		{
			if (rddt == dataType)
			{
				return true;
			}
		}
		return false;
	}
}
