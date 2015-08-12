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
package gov.va.isaac.refexDynamic;

import gov.va.isaac.AppContext;
import gov.va.isaac.ExtendedAppContext;
import gov.va.isaac.gui.SimpleDisplayConcept;
import gov.va.isaac.util.OTFUtility;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javafx.application.Platform;
import javafx.scene.control.ProgressIndicator;
import org.ihtsdo.otf.query.lucene.indexers.DynamicSememeIndexer;
import org.ihtsdo.otf.query.lucene.indexers.DynamicSememeIndexerConfiguration;
import org.ihtsdo.otf.tcc.api.concept.ConceptVersionBI;
import org.ihtsdo.otf.tcc.api.metadata.binding.DynamicSememe;
import org.ihtsdo.otf.tcc.api.refexDynamic.DynamicSememeChronicleBI;
import gov.vha.isaac.ochre.api.index.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link DynamicSememeUtil}
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a> 
 */
public class DynamicSememeUtil
{
	private static Logger logger_ = LoggerFactory.getLogger(DynamicSememeUtil.class);
	
	@SuppressWarnings("unchecked")
	public static Collection<DynamicSememeChronicleBI<?>> readMembers(int dynamicRefexConceptNid, boolean allowUnindexedScan, ProgressIndicator progress) 
			throws Exception
	{
		Collection<DynamicSememeChronicleBI<?>> refexMembers;
		
		ConceptVersionBI assemblageConceptFull = OTFUtility.getConceptVersion(dynamicRefexConceptNid);
		if (assemblageConceptFull.isAnnotationStyleRefex())
		{
			refexMembers = new ArrayList<>();
			
			if (DynamicSememeIndexerConfiguration.isAssemblageIndexed(assemblageConceptFull.getNid()))
			{
				logger_.debug("Using index to read annotation style refex members");
				if (progress != null)
				{
					Platform.runLater(() ->
					{
						progress.setProgress(-1);
					});
				}
				
				DynamicSememeIndexer ldri = AppContext.getService(DynamicSememeIndexer.class);
				
				List<SearchResult> results = ldri.queryAssemblageUsage(assemblageConceptFull.getNid(), Integer.MAX_VALUE, Long.MIN_VALUE);
				for (SearchResult sr : results)
				{
					refexMembers.add((DynamicSememeChronicleBI<?>)ExtendedAppContext.getDataStore().getComponent(sr.getNid()));
				}
			}
			else
			{
				if (allowUnindexedScan)
				{
					logger_.debug("Using full database scan to read annotation style refex members");
					RefexAnnotationSearcher processor = new RefexAnnotationSearcher((refex) -> 
					{
						if (refex.getAssemblageNid() == assemblageConceptFull.getConceptNid())
						{
							return true;
						}
						return false;
					}, progress);

					ExtendedAppContext.getDataStore().iterateConceptDataInParallel(processor);
					refexMembers.addAll(processor.getResults());
				}
				else
				{
					throw new RuntimeException("No index available and full scan not allowed!");
				}
			}
		}
		else
		{
			logger_.debug("Reading member style refex members");
			refexMembers = (Collection<DynamicSememeChronicleBI<?>>)assemblageConceptFull.getRefsetDynamicMembers();
		}
		return refexMembers;
	}

	public static List<SimpleDisplayConcept> getAllRefexDefinitions() throws IOException {
		List<SimpleDisplayConcept> allRefexDefinitions = new ArrayList<>();

		try {
			DynamicSememeIndexer indexer = AppContext.getService(DynamicSememeIndexer.class);
			List<SearchResult> refexes = indexer.queryAssemblageUsage(DynamicSememe.DYNAMIC_SEMEME_DEFINITION_DESCRIPTION.getNid(), 1000, Long.MAX_VALUE);
			for (SearchResult sr : refexes) {
				DynamicSememeChronicleBI<?> rc = (DynamicSememeChronicleBI<?>) ExtendedAppContext.getDataStore().getComponent(sr.getNid());
				if (rc == null) {
					logger_.info("Out of date index?  Search result for refexes contained a NID that can't be resolved: {}" + sr.getNid());
					continue;
				}
				//These are nested refex references - it returns a description component - concept we want is the parent of that.
				allRefexDefinitions.add(new SimpleDisplayConcept(rc.getReferencedComponentNid(), null));
			}
		} catch (Exception e) {
			throw new IOException(e);
		}
		
		Collections.sort(allRefexDefinitions);
		return allRefexDefinitions;
	}

}
