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
package gov.va.isaac.util;

import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeDataBI;
import gov.vha.isaac.ochre.model.sememe.dataTypes.DynamicSememeDouble;
import gov.vha.isaac.ochre.model.sememe.dataTypes.DynamicSememeFloat;
import gov.vha.isaac.ochre.model.sememe.dataTypes.DynamicSememeInteger;
import gov.vha.isaac.ochre.model.sememe.dataTypes.DynamicSememeLong;
import gov.vha.isaac.ochre.util.NumericUtils;

/**
 * {@link NumberUtilities}
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a> 
 */
public class NumberUtilities extends NumericUtils
{
	/**
	 * Return the wrapped number, or throw an exception if not parseable as an integer, long, float or double
	 */
	public static DynamicSememeDataBI wrapIntoRefexHolder(Number value) throws NumberFormatException
	{
		if (value == null)
		{
			return null;
		}
		if (value instanceof Integer)
		{
			return new DynamicSememeInteger(value.intValue());
		}
		else if (value instanceof Long)
		{
			return new DynamicSememeLong(value.longValue());
		}
		else if (value instanceof Float)
		{
			return new DynamicSememeFloat(value.floatValue());
		}
		else if (value instanceof Double)
		{
			return new DynamicSememeDouble(value.doubleValue());
		}
		else
		{
			throw new NumberFormatException("The value must be a numeric value of type int, long, float or double.");
		}
	}
}
