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
package gov.va.isaac.gui;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import javax.inject.Singleton;
import gov.vha.isaac.ochre.api.index.IndexServiceBI;
import gov.vha.isaac.ochre.api.index.IndexStatusListenerBI;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link IndexStatusListener}
 *
 * A simple service that lets any ISAAC code tie in an action to index change events.
 * 
 * Simply pass a consumer into one of the on* methods.
 * 
 * Make sure you maintain a reference to the consumer - this class uses weak references, and 
 * will not hold them from the garbage collector.
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
@Service
@Singleton
public class IndexStatusListener implements IndexStatusListenerBI
{
	private static final Logger LOG = LoggerFactory.getLogger(IndexStatusListener.class);

	Set<Consumer<IndexServiceBI>> callbackOnIndexConfigChanged = Collections.newSetFromMap(new WeakHashMap<Consumer<IndexServiceBI>, Boolean>());
	Set<Consumer<IndexServiceBI>> callbackOnReindexStarted = Collections.newSetFromMap(new WeakHashMap<Consumer<IndexServiceBI>, Boolean>());
	Set<Consumer<IndexServiceBI>> callbackOnReindexComplete = Collections.newSetFromMap(new WeakHashMap<Consumer<IndexServiceBI>, Boolean>());

	private IndexStatusListener()
	{
		//for HK2
	}

	/**
	 * This uses weak references - you must hold onto your own consumer reference.
	 * @return the passed in reference, for convenience.
	 */
	public Consumer<IndexServiceBI> onIndexConfigChanged(Consumer<IndexServiceBI> callback)
	{
		callbackOnIndexConfigChanged.add(callback);
		return callback;
	}
	
	/**
	 * This uses weak references - you must hold onto your own consumer reference.
	 * @return the passed in reference, for convenience.
	 */
	public void onReindexStarted(Consumer<IndexServiceBI> callback)
	{
		callbackOnReindexStarted.add(callback);
	}
	
	/**
	 * This uses weak references - you must hold onto your own consumer reference.
	 * @return the passed in reference, for convenience.
	 */
	public void onReindexCompleted(Consumer<IndexServiceBI> callback)
	{
		callbackOnReindexComplete.add(callback);
	}

	/**
	 * @see gov.vha.isaac.ochre.api.index.IndexStatusListenerBI#indexConfigurationChanged(gov.vha.isaac.ochre.api.index.IndexServiceBI)
	 */
	@Override
	public void indexConfigurationChanged(IndexServiceBI indexConfigurationThatChanged)
	{
		LOG.info("Index config changed {}", indexConfigurationThatChanged.getIndexerName());
		for (Consumer<IndexServiceBI> callback : callbackOnIndexConfigChanged)
		{
			callback.accept(indexConfigurationThatChanged);
		}
	}

	/**
	 * @see gov.vha.isaac.ochre.api.index.IndexStatusListenerBI#reindexBegan(gov.vha.isaac.ochre.api.index.IndexServiceBI)
	 */
	@Override
	public void reindexBegan(IndexServiceBI index)
	{
		LOG.info("Reindex Began {}", index.getIndexerName());
		for (Consumer<IndexServiceBI> callback : callbackOnReindexStarted)
		{
			callback.accept(index);
		}
	}

	/**
	 * @see gov.vha.isaac.ochre.api.index.IndexStatusListenerBI#reindexCompleted(gov.vha.isaac.ochre.api.index.IndexServiceBI)
	 */
	@Override
	public void reindexCompleted(IndexServiceBI index)
	{
		LOG.info("Reindex Completed {}", index.getIndexerName());
		for (Consumer<IndexServiceBI> callback : callbackOnReindexComplete)
		{
			callback.accept(index);
		}
	}
}