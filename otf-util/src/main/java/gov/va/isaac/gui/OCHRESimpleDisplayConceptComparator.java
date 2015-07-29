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
package gov.va.isaac.gui;

import gov.va.isaac.util.AlphanumComparator;
import java.util.Comparator;

/**
 * {@link OCHRESimpleDisplayConceptComparator}
 *
 * @author <a href="mailto:joel.kniaz@gmail.com">Joel Kniaz</a> 
 */
public class OCHRESimpleDisplayConceptComparator implements Comparator<OCHRESimpleDisplayConcept>
{
	private static AlphanumComparator ac = new AlphanumComparator(true);
	/**
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	@Override
	public int compare(OCHRESimpleDisplayConcept o1, OCHRESimpleDisplayConcept o2)
	{
		if (o1 == null)
		{
			return 1;
		}
		else if (o2 ==  null)
		{
			return -1;
		}
		else
		{
			return ac.compare(o1.getDescription(), o2.getDescription());
		}
	}
}
