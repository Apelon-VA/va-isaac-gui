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
package gov.va.isaac.util;

import org.junit.Assert;
import org.junit.Test;

/**
 * {@link SctIdTest}
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public class SctIdTest
{
	@Test
	public void sctIdTestOne() throws Exception
	{
		Assert.assertTrue(SctId.isValidSctId(9120001000004100l));
		Assert.assertTrue(SctId.isValidSctId("9120001000004100"));
		
		Assert.assertFalse(SctId.isValidSctId(9120001000004103l));
		Assert.assertFalse(SctId.isValidSctId("9120001000004103"));
	}
	
	@Test
	public void sctIdTestTwo() throws Exception
	{
		Assert.assertTrue(SctId.isValidSctId(9110001000004108l));
		Assert.assertTrue(SctId.isValidSctId("9110001000004108"));
	}
	
	@Test
	public void sctIdTestThree() throws Exception
	{
		Assert.assertFalse(SctId.isValidSctId("91a0001000004108"));
	}
	
	@Test
	public void sctIdTestFour() throws Exception
	{
		Assert.assertTrue(SctId.isValidSctId(47448006));
		Assert.assertTrue(SctId.isValidSctId("47448006"));
		
		Assert.assertFalse(SctId.isValidSctId(47248006));
		Assert.assertFalse(SctId.isValidSctId("47248006"));
	}
	
	@Test
	public void sctIdTestFive() throws Exception
	{
		Assert.assertFalse(SctId.isValidSctId(-9110001000004108l));
		Assert.assertFalse(SctId.isValidSctId("-9110001000004108"));
	}
}
