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

/**
 * NonSearchTypeFilter
 * 
 * @author <a href="mailto:joel.kniaz@gmail.com">Joel Kniaz</a>
 */
package gov.va.isaac.gui.enhancedsearchview.filters;

import gov.va.isaac.ExtendedAppContext;
import gov.va.isaac.util.OTFUtility;
import gov.vha.isaac.metadata.source.IsaacMetadataAuxiliaryBinding;
import java.util.HashSet;
import java.util.Set;
import javafx.beans.property.IntegerProperty;
import org.ihtsdo.otf.tcc.api.concept.ConceptVersionBI;
import org.ihtsdo.otf.tcc.api.nid.NativeIdSetBI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NonSearchTypeFilter
 * 
 * @author <a href="mailto:joel.kniaz@gmail.com">Joel Kniaz</a>
 *
 */
public abstract class NonSearchTypeFilter<T extends NonSearchTypeFilter<T>> implements Filter<T> {
	
	Logger logger = LoggerFactory.getLogger(NonSearchTypeFilter.class);
	
	public abstract Set<Integer> gatherNoSearchTermCaseList(Set<Integer> startList);
	abstract IntegerProperty getSingleNid();

	protected Set<Integer> getSingleNidNoSearchTermCaseList(Set<Integer> startList) {
		Set<Integer> mergedSet = new HashSet<Integer>();

		try {
			ConceptVersionBI con = OTFUtility.getConceptVersion(getSingleNid().get());

			//TODO   um... really?  This may be the most inefficient code I've ever seen.
			//Why on earth are we building an all concept searcher by iterating all concepts in the DB, but then checking every single concept 
			//in the DB to see if it extends from ISAAC_ROOT?  The only thing that would do is exclude orphans... which, certainly doesn't seem 
			//to be the intent... On top of that... it doesn't even start with the startList... rather starting with the entire DB.
			//This needs to be completely thrown out / rewritten from scratch.
			NativeIdSetBI allConcepts = ExtendedAppContext.getDataStore().getAllConceptNids();
			NoSearchTermConcurrentSearcher searcher = new NoSearchTermConcurrentSearcher(allConcepts, IsaacMetadataAuxiliaryBinding.ISAAC_ROOT.getNid());
			ExtendedAppContext.getDataStore().iterateConceptDataInParallel(searcher);

			if (!startList.isEmpty()) {
				for (Integer examCon : startList) {
					if (searcher.getResults().contains(examCon)) {
						mergedSet.add(examCon);
					}
				}
			} else {
				mergedSet.addAll(searcher.getResults());
			}
		} catch (Exception e) {
			logger.error("Cannot find calculate the NoSearchTermCaseList", e);
		}

		return mergedSet;
	}
}
