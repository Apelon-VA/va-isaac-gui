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
package gov.va.isaac.search;

import gov.va.isaac.util.OchreUtility;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.component.sememe.version.DescriptionSememe;
import java.util.Comparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link Comparator} for {@link DescriptionSememe} objects that compares the descriptions by their type
 *
 * @author ocarlsen
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public class DescriptionSememeTypeComparator implements Comparator<DescriptionSememe<?>>
{
	protected static final Logger LOG = LoggerFactory.getLogger(DescriptionSememeTypeComparator.class);

	@Override
	public int compare(DescriptionSememe<?> o1, DescriptionSememe<?> o2)
	{
		String o1matchingComponentType = Get.conceptService().getOptionalConcept(o1.getDescriptionTypeConceptSequence()).get().getConceptDescriptionText();
		String o2matchingComponentType = Get.conceptService().getOptionalConcept(o2.getDescriptionTypeConceptSequence()).get().getConceptDescriptionText();

		return o1matchingComponentType.compareTo(o2matchingComponentType);
	}
}
