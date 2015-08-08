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
 *	 http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gov.va.isaac.search;

import java.util.Comparator;
import org.apache.commons.lang3.ObjectUtils;
import gov.vha.isaac.ochre.api.index.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link Comparator} for {@link SearchResult} objects.
 *
 * @author ocarlsen
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public class CompositeSearchResultComparator implements Comparator<CompositeSearchResult> {
	protected static final Logger LOG = LoggerFactory.getLogger(CompositeSearchResultComparator.class);

	/**
	 * Note, the primary getBestScore() sort is in reverse, so it goes highest to lowest
	 *
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	@Override
	public int compare(CompositeSearchResult o1, CompositeSearchResult o2) {
		if (o1.getBestScore() < o2.getBestScore()) {
			return 1;
		} else if (o1.getBestScore() > o2.getBestScore()) {
			return -1;
		}
		
		//Sort off path ones to the bottom
		if (!o1.getContainingConcept().isPresent() || !o2.getContainingConcept().isPresent())
		{
			if (!o1.getContainingConcept().isPresent() && o2.getContainingConcept().isPresent())
			{
				return 1;
			}
			else if (o1.getContainingConcept().isPresent() && !o2.getContainingConcept().isPresent())
			{
				return -1;
			}
			else
			{
				return 0;
			}
		}
		// sort on text
		int textComparison = ObjectUtils.compare(o1.getContainingConcept().get().getConceptDescriptionText(), o2.getContainingConcept().get().getConceptDescriptionText());
		if (textComparison != 0) {
			return textComparison;
		}
		
		// else same score and FSN and preferred description - sort on type
		String comp1String = o1.getMatchingComponents().iterator().next().toUserString();
		String comp2String = o2.getMatchingComponents().iterator().next().toUserString();

		return ObjectUtils.compare(comp1String, comp2String);
	}
}
