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

import gov.va.isaac.gui.OCHRESimpleDisplayConcept;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.ReadOnlyListWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javax.inject.Singleton;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link OCHRECommonlyUsedConcepts}
 * 
 * @author <a href="mailto:joel.kniaz@gmail.com">Joel Kniaz</a>
 */
@Service
@Singleton
public class OCHRECommonlyUsedConcepts
{
	private static Logger logger = LoggerFactory.getLogger(OCHRECommonlyUsedConcepts.class);
	private static int CACHE_SIZE = 10;
	private ObservableList<OCHRESimpleDisplayConcept> lruCache_ = FXCollections.observableArrayList();
	private ObservableList<OCHRESimpleDisplayConcept> lruCacheReadOnly_ = new ReadOnlyListWrapper<OCHRESimpleDisplayConcept>(lruCache_);

	private OCHRECommonlyUsedConcepts()
	{
		// created by HK2
		logger.debug("CommonlyUsedConcepts created");
	}
	
	public synchronized void addConcept(OCHRESimpleDisplayConcept sdc)
	{
		int index = lruCache_.indexOf(sdc);
		if (index >= 0)
		{
			lruCache_.add(0, sdc.clone());
			lruCache_.remove(index + 1);
		}
		else
		{
			while (lruCache_.size() >= CACHE_SIZE - 1)
			{
				lruCache_.remove(lruCache_.size() - 1);
			}
			lruCache_.add(0, sdc.clone());
		}
	}
	
	public List<OCHRESimpleDisplayConcept> getConcepts()
	{
		ArrayList<OCHRESimpleDisplayConcept> result = new ArrayList<>(lruCache_.size());
		result.addAll(lruCache_);
		return result;
	}
	
	public ObservableList<OCHRESimpleDisplayConcept> getObservableConcepts()
	{
		return lruCacheReadOnly_;
	}
}
