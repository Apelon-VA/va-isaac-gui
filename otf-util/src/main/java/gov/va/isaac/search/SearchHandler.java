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

import gov.va.isaac.AppContext;
import gov.va.isaac.util.OCHREUtility;
import gov.va.isaac.util.TaskCompleteCallback;
import gov.va.isaac.util.Utility;
import gov.vha.isaac.metadata.source.IsaacMetadataAuxiliaryBinding;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.chronicle.ObjectChronology;
import gov.vha.isaac.ochre.api.chronicle.StampedVersion;
import gov.vha.isaac.ochre.api.component.concept.ConceptChronology;
import gov.vha.isaac.ochre.api.component.concept.ConceptVersion;
import gov.vha.isaac.ochre.api.component.sememe.version.DescriptionSememe;
import gov.vha.isaac.ochre.api.index.SearchResult;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import org.ihtsdo.otf.query.lucene.LuceneDescriptionIndexer;
import org.ihtsdo.otf.query.lucene.LuceneDescriptionType;
import org.ihtsdo.otf.query.lucene.LuceneDynamicRefexIndexer;
import org.ihtsdo.otf.tcc.api.blueprint.ComponentProperty;
import org.ihtsdo.otf.tcc.api.chronicle.ComponentChronicleBI;
import org.ihtsdo.otf.tcc.model.cc.refex.RefexService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for handling the ISAAC search functionality.
 * <p>
 * Logic has been mostly copied from LEGO {@code WBDataStore#search}, but now rewritten and updated to use the newer
 * search APIs provided in OTF
 * 
 * @author ocarlsen
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
   */

//TODO need to rework these APIs to take in path info - so that the path for the search can easily be customized from the search GUI
public class SearchHandler
{
	private static final Logger LOG = LoggerFactory.getLogger(SearchHandler.class);

	/**
	 * Execute a Query against the description indexes in a background thread, hand back a handle to the search object which will 
	 * allow you to get the results (when they are ready) and also cancel an in-progress query.
	 * 
	 * If there is a problem with the internal indexes - an error will be logged, and the exception will be re-thrown when the 
	 * {@link SearchHandle#getResults()} method of the SearchHandle is called.
	 * 
	 * @param query - The query string
	 * @param resultLimit - limit to X results.  Use {@link Integer#MAX_VALUE} for no limit.
	 * @param prefixSearch - true to use the "prefex search" algorithm.  False to use the standard lucene algorithm.  
	 *   See {@link LuceneDescriptionIndexer#query(String, boolean, ComponentProperty, int, Long)} for more details on this algorithm.
	 * @param callback - (optional) Pass the handle that you want to have notified when the search is complete, and the results are ready for use.
	 * @param taskId - An optional field that is simply handed back during the callback when results are complete.  Useful for matching 
	 *   requests to this method with callbacks.
	 * @param filter - An optional filter than can add or remove items from the tentative result set before it is returned.
	 * @param comparator - The comparator to use for sorting the results - optional - uses {@link CompositeSearchResultComparator} if none is
	 *  provided.
	 * @param mergeOnConcepts - If true, when multiple description objects within the same concept match the search, this will be returned 
	 *   as a single result representing the concept - with each matching string listed, and the score being the best score of any of the 
	 *   matching strings.  When false, you will get one search result per description match - so concepts can be returned multiple times.
	 * @param includeOffPathResults - true to give back results for all hits (which may have unresolvable concepts) or false to filter those
	 *   out that are not on THE CURRENT COORDINATE configuration 
	 * @return A handle to the running search.
	 */
	public static SearchHandle descriptionSearch(
			String query, 
			final int resultLimit, 
			final boolean prefixSearch, 
			final TaskCompleteCallback callback, 
			final Integer taskId, 
			final Function<List<CompositeSearchResult>, List<CompositeSearchResult>> filter,
			Comparator<CompositeSearchResult> comparator,
			boolean mergeOnConcepts,
			boolean includeOffPathResults)
	{
		return descriptionSearch(query, 
				(index, queryString) ->
				{
					try
					{
						return index.query(queryString, prefixSearch, ComponentProperty.DESCRIPTION_TEXT, resultLimit, Long.MIN_VALUE);
					}
					catch (Exception e)
					{
						throw new RuntimeException(e);
					}
				},
		prefixSearch, callback, taskId, filter, comparator, mergeOnConcepts, includeOffPathResults); 
	}
	
	/**
	 * Execute a Query against a specific type of description in a background thread, hand back a handle to the search object which will 
	 * allow you to get the results (when they are ready) and also cancel an in-progress query.
	 * 
	 * If there is a problem with the internal indexes - an error will be logged, and the exception will be re-thrown when the 
	 * {@link SearchHandle#getResults()} method of the SearchHandle is called.
	 * 
	 * @param query - The query text
	 * @param resultLimit - limit to X results.  Use {@link Integer#MAX_VALUE} for no limit.
	 * @param prefixSearch - true to use the "prefex search" algorithm.  False to use the standard lucene algorithm.  
	 *   See {@link LuceneDescriptionIndexer#query(String, boolean, ComponentProperty, int, Long)} for more details on this algorithm.
	 * @param descriptionType - The type to search within (FSN / Synonym / Description)
	 * @param callback - (optional) Pass the handle that you want to have notified when the search is complete, and the results are ready for use.
	 * @param taskId - An optional field that is simply handed back during the callback when results are complete.  Useful for matching 
	 *   requests to this method with callbacks.
	 * @param filter - An optional filter than can add or remove items from the tentative result set before it is returned.
	 * @param comparator - The comparator to use for sorting the results - optional - uses {@link CompositeSearchResultComparator} if none is
	 *  provided.
	 * @param mergeOnConcepts - If true, when multiple description objects within the same concept match the search, this will be returned 
	 *   as a single result representing the concept - with each matching string listed, and the score being the best score of any of the 
	 *   matching strings.  When false, you will get one search result per description match - so concepts can be returned multiple times.
	 * @param includeOffPathResults - true to give back results for all hits (which may have unresolvable concepts) or false to filter those
	 *   out that are not on THE CURRENT COORDINATE configuration 
	 * @return A handle to the running search.
	 */
	public static SearchHandle descriptionSearch(
			String query, 
			final int resultLimit, 
			final boolean prefixSearch, 
			final LuceneDescriptionType descriptionType,
			final TaskCompleteCallback callback, 
			final Integer taskId, 
			final Function<List<CompositeSearchResult>, List<CompositeSearchResult>> filter,
			Comparator<CompositeSearchResult> comparator,
			boolean mergeOnConcepts,
			boolean includeOffPathResults)
	{
		
		return descriptionSearch(query, 
				(index, queryString) ->
				{
					try
					{
						return index.query(queryString, descriptionType, resultLimit, Long.MIN_VALUE);
					}
					catch (Exception e)
					{
						throw new RuntimeException(e);
					}
				},
		prefixSearch, callback, taskId, filter, comparator, mergeOnConcepts, includeOffPathResults); 
	}
	
	/**
	 * Execute a Query against a specific type of description in a background thread, hand back a handle to the search object which will 
	 * allow you to get the results (when they are ready) and also cancel an in-progress query.
	 * 
	 * If there is a problem with the internal indexes - an error will be logged, and the exception will be re-thrown when the 
	 * {@link SearchHandle#getResults()} method of the SearchHandle is called.
	 * 
	 * @param query - The query text
	 * @param resultLimit - limit to X results.  Use {@link Integer#MAX_VALUE} for no limit.
	 * @param prefixSearch - true to use the "prefex search" algorithm.  False to use the standard lucene algorithm.  
	 *   See {@link LuceneDescriptionIndexer#query(String, boolean, ComponentProperty, int, Long)} for more details on this algorithm.
	 * @param extendedDescriptionType - The UUID of the concept that represents the extended (terminology specific) description type.
	 * This concept should be a child of {@link IsaacMetadataAuxiliaryBinding#DESCRIPTION_TYPE_IN_SOURCE_TERMINOLOGY}
	 * @param callback - (optional) Pass the handle that you want to have notified when the search is complete, and the results are ready for use.
	 * @param taskId - An optional field that is simply handed back during the callback when results are complete.  Useful for matching 
	 *   requests to this method with callbacks.
	 * @param filter - An optional filter than can add or remove items from the tentative result set before it is returned.
	 * @param comparator - The comparator to use for sorting the results - optional - uses {@link CompositeSearchResultComparator} if none is
	 *  provided.
	 * @param mergeOnConcepts - If true, when multiple description objects within the same concept match the search, this will be returned 
	 *   as a single result representing the concept - with each matching string listed, and the score being the best score of any of the 
	 *   matching strings.  When false, you will get one search result per description match - so concepts can be returned multiple times.
	 * @param includeOffPathResults - true to give back results for all hits (which may have unresolvable concepts) or false to filter those
	 *   out that are not on THE CURRENT COORDINATE configuration 
	 * @return A handle to the running search.
	 */
	public static SearchHandle descriptionSearch(
			String query, 
			final int resultLimit, 
			final boolean prefixSearch, 
			final UUID extendedDescriptionType,
			final TaskCompleteCallback callback, 
			final Integer taskId, 
			final Function<List<CompositeSearchResult>, List<CompositeSearchResult>> filter,
			Comparator<CompositeSearchResult> comparator,
			boolean mergeOnConcepts,
			boolean includeOffPathResults)
	{
		
		return descriptionSearch(query, 
				(index, queryString) ->
				{
					try
					{
						return index.query(queryString, extendedDescriptionType, resultLimit, Long.MIN_VALUE);
					}
					catch (Exception e)
					{
						throw new RuntimeException(e);
					}
				},
		prefixSearch, callback, taskId, filter, comparator, mergeOnConcepts, includeOffPathResults); 
	}
	
	/**
	 * Calls {@link #descriptionSearch(String, int, boolean, TaskCompleteCallback, Integer, SearchResultsFilter, Comparator, boolean, Supplier)}
	 * passing false for prefixSearch, null for the taskID, null for the filter, null for the comparator
	 * @param query - The query string
	 * @param resultLimit - limit to X results.  Use {@link Integer#MAX_VALUE} for no limit.
	 * @param callback - (optional) Pass the handle that you want to have notified when the search is complete, and the results are ready for use.
	 * @param mergeOnConcepts - If true, when multiple description objects within the same concept match the search, this will be returned 
	 *   as a single result representing the concept - with each matching string listed, and the score being the best score of any of the 
	 *   matching strings.  When false, you will get one search result per description match - so concepts can be returned multiple times.
	 * @param includeOffPathResults - true to give back results for all hits (which may have unresolvable concepts) or false to filter those
	 *   out that are not on THE CURRENT COORDINATE configuration 
	 * @return A handle to the running search.
	 */
	public static SearchHandle descriptionSearch(String query, int resultLimit, TaskCompleteCallback callback, boolean mergeResultsOnConcepts, boolean includeOffPathResults) {
		return descriptionSearch(query, resultLimit, false, callback, (Integer)null, null, null, mergeResultsOnConcepts, includeOffPathResults);
	}
	
	
	/**
	 * ** ADVANCED API** - you probably don't want this method....
	 * 
	 * 	 This is really just a convenience wrapper with threading and results conversion on top of the APIs available in {@link LuceneDescriptionIndexer}
	 * 
	 * Execute a Query against the description indexes in a background thread, hand back a handle to the search object which will 
	 * allow you to get the results (when they are ready) and also cancel an in-progress query.
	 * 
	 * If there is a problem with the internal indexes - an error will be logged, and the exception will be re-thrown when the 
	 * {@link SearchHandle#getResults()} method of the SearchHandle is called.
	 * 
	 * @param query - The query string
	 * @param searchFunction -  A function that will call one of the query(...) methods within {@link LuceneDescriptionIndexer}.  See
	 * that class for documentation on the various search types supported.
	 * @param prefixSearch - true to use the "prefex search" algorithm.  False to use the standard lucene algorithm.  
	 *   See {@link LuceneDescriptionIndexer#query(String, boolean, ComponentProperty, int, Long)} for more details on this algorithm.
	 * @param callback - (optional) Pass the handle that you want to have notified when the search is complete, and the results are ready for use.
	 * @param taskId - An optional field that is simply handed back during the callback when results are complete.  Useful for matching 
	 *   requests to this method with callbacks.
	 * @param filter - An optional filter than can add or remove items from the tentative result set before it is returned.
	 * @param comparator - The comparator to use for sorting the results - optional - uses {@link CompositeSearchResultComparator} if none is
	 *  provided.
	 * @param mergeOnConcepts - If true, when multiple description objects within the same concept match the search, this will be returned 
	 *   as a single result representing the concept - with each matching string listed, and the score being the best score of any of the 
	 *   matching strings.  When false, you will get one search result per description match - so concepts can be returned multiple times.
	 * @param includeOffPathResults - true to give back results for all hits (which may have unresolvable concepts) or false to filter those
	 *   out that are not on THE CURRENT COORDINATE configuration 
	 * @return A handle to the running search.
	 */
	public static SearchHandle descriptionSearch(
			String query,
			final BiFunction<LuceneDescriptionIndexer, String, List<SearchResult>> searchFunction,
			final boolean prefixSearch, 
			final TaskCompleteCallback callback, 
			final Integer taskId, 
			final Function<List<CompositeSearchResult>, List<CompositeSearchResult>> filter,
			Comparator<CompositeSearchResult> comparator,
			boolean mergeOnConcepts,
			boolean includeOffPathResults)
	{
		final SearchHandle searchHandle = new SearchHandle();

		if (!prefixSearch)
		{
			// Just strip out parens, which are common in FSNs, but also lucene search operators (which our users likely won't use)
			query = query.replaceAll("\\(", "");
			query = query.replaceAll("\\)", "");
		}

		final String localQuery = query;

		// Do search in background.
		Runnable r = new Runnable()
		{
			@Override
			public void run()
			{
				List<CompositeSearchResult> initialSearchResults = new ArrayList<>();
				try
				{
					if (localQuery.length() > 0)
					{
						// If search query is an ID, look up concept and add the result.
						if (Utility.isUUID(localQuery) || Utility.isLong(localQuery))
						{
							Optional<? extends ConceptChronology<? extends ConceptVersion<?>>> temp = OCHREUtility.getConceptForUnknownIdentifier(localQuery);
							if (temp.isPresent())
							{
								CompositeSearchResult gsr = new CompositeSearchResult(temp.get(), 2.0f);
								initialSearchResults.add(gsr);
							}
						}

						LOG.debug("Lucene Search: '" + localQuery + "'");

						LuceneDescriptionIndexer descriptionIndexer = AppContext.getService(LuceneDescriptionIndexer.class);
						if (descriptionIndexer == null)
						{
							LOG.warn("No description indexer found, aborting.");
							searchHandle.setError(new Exception("No description indexer available, cannot search"));
						}
						else
						{
							// Look for description matches.
							List<SearchResult> searchResults = searchFunction.apply(descriptionIndexer, localQuery);
							final int resultCount = searchResults.size();
							LOG.debug(resultCount + " results");

							if (resultCount > 0)
							{
								// Compute the max score of all results.
								float maxScore = 0.0f;
								for (SearchResult searchResult : searchResults)
								{
									float score = searchResult.getScore();
									if (score > maxScore)
									{
										maxScore = score;
									}
								}

								for (SearchResult searchResult : searchResults)
								{
									// Abort if search has been cancelled.
									if (searchHandle.isCancelled())
									{
										break;
									}

									// Get the description object.
									Optional<? extends ObjectChronology<? extends StampedVersion>> io = Get.identifiedObjectService()
											.getIdentifiedObjectChronology(searchResult.getNid());

									// normalize the scores between 0 and 1
									float normScore = (searchResult.getScore() / maxScore);
									CompositeSearchResult csr = (io.isPresent() ? new CompositeSearchResult(io.get(), normScore) : 
										new CompositeSearchResult(searchResult.getNid(), normScore));
									initialSearchResults.add(csr);
									

									// add one to the scores when we are doing a prefix search, and it hits.
									if (prefixSearch && csr.getBestScore() <= 1.0f && io.isPresent() && io.get() instanceof DescriptionSememe<?>)
									{
										String matchingString = ((DescriptionSememe<?>) io.get()).getText();
										float adjustValue = 0f;

										if (matchingString.toLowerCase().equals(localQuery.trim().toLowerCase()))
										{
											// "exact match, bump by 2"
											adjustValue = 2.0f;
										}
										else if (matchingString.toLowerCase().startsWith(localQuery.trim().toLowerCase()))
										{
											// "add 1, plus a bit more boost based on the length of the matches (shorter matches get more boost)"
											adjustValue = 1.0f + (1.0f - ((float) (matchingString.length() - localQuery.trim().length()) / (float) matchingString
													.length()));
										}
										if (adjustValue > 0f)
										{
											csr.adjustScore(csr.getBestScore() + adjustValue);
										}
									}
								}
							}
						}
					} 
					// sort, filter and merge the results as necessary
					processResults(searchHandle, initialSearchResults, filter, comparator, mergeOnConcepts, includeOffPathResults);
				}
				catch (Exception ex)
				{
					LOG.error("Unexpected error during lucene search", ex);
					searchHandle.setError(ex);
				}
				if (callback != null)
				{
					callback.taskComplete(searchHandle.getSearchStartTime(), taskId);
				}
			}
		};

		Utility.execute(r);
		return searchHandle;
	}
	
	
	/**
	 * Execute a Query against the dynamic refex indexes in a background thread, hand back a handle to the search object which will 
	 * allow you to get the results (when they are ready) and also cancel an in-progress query.
	 * 
	 * If there is a problem with the internal indexes - an error will be logged, and the exception will be re-thrown when the 
	 * {@link SearchHandle#getResults()} method of the SearchHandle is called.
	 * 
	 * This is really just a convenience wrapper with threading and results conversion on top of the APIs available in {@link LuceneDynamicRefexIndexer}
	 * 
	 * @param searchFunction A function that will call one of the query(...) methods within {@link LuceneDynamicRefexIndexer}.  See
	 * that class for documentation on the various search types supported.
	 * @param callback - Pass the handle that you want to have notified when the search is complete, and the results are ready for use.
	 * @param taskId - An optional field that is simply handed back during the callback when results are complete.  Useful for matching 
	 *   requests to this method with callbacks.
	 * @param filter - An optional filter than can add or remove items from the tentative result set before it is returned.
	 * @param comparator - The comparator to use for sorting the results
	 * @param mergeOnConcepts - If true, when multiple description objects within the same concept match the search, this will be returned 
	 *   as a single result representing the concept - with each matching string listed, and the score being the best score of any of the 
	 *   matching strings.  When false, you will get one search result per description match - so concepts can be returned multiple times.
	 * @param includeOffPathResults - true to give back results for all hits (which may have unresolvable concepts) or false to filter those
	 *   out that are not on THE CURRENT COORDINATE configuration 
	 * @return A handle to the running search.
	 */
	public static SearchHandle dynamicRefexSearch(
			Function<LuceneDynamicRefexIndexer, List<SearchResult>> searchFunction,
			final TaskCompleteCallback callback, 
			final Integer taskId, 
			final Function<List<CompositeSearchResult>, List<CompositeSearchResult>> filter,
			Comparator<CompositeSearchResult> comparator,
			boolean mergeOnConcepts,
			boolean includeOffPathResults)
	{
		final SearchHandle searchHandle = new SearchHandle();
		
		// Do search in background.
		Runnable r = new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					List<CompositeSearchResult> initialSearchResults = new ArrayList<>();
					LuceneDynamicRefexIndexer refexIndexer = AppContext.getService(LuceneDynamicRefexIndexer.class);
					
					if (refexIndexer == null)
					{
						LOG.warn("No sememe indexer found, aborting.");
						searchHandle.setError(new Exception("No sememe indexer available, cannot search"));
					}
					else
					{
						
						List<SearchResult> searchResults = searchFunction.apply(refexIndexer);

						LOG.debug(searchResults.size() + " results");

						if (searchResults.size() > 0)
						{
							// Compute the max score of all results.
							float maxScore = 0.0f;
							for (SearchResult searchResult : searchResults)
							{
								float score = searchResult.getScore();
								if (score > maxScore)
								{
									maxScore = score;
								}
							}

							for (SearchResult searchResult : searchResults)
							{
								// Abort if search has been cancelled.
								if (searchHandle.isCancelled())
								{
									break;
								}

								// Get the match object.
								Optional<? extends ObjectChronology<? extends StampedVersion>> io = Get.identifiedObjectService()
										.getIdentifiedObjectChronology(searchResult.getNid());
								if (!io.isPresent())
								{
									//TODO OCHRE this code should be unecessary - the getComponent call above 
									//currently isn't working correctly.  Wait for fix from Keith...
									ComponentChronicleBI<? extends StampedVersion> cc =  AppContext.getService(RefexService.class).getRefex(searchResult.getNid());
									if (cc == null)
									{
										cc = AppContext.getService(RefexService.class).getRefexDynamic(searchResult.getNid());
									}
									
									if (cc != null)
									{
										io = Optional.of(cc);
									}
									else
									{
										io = Optional.empty();
									}
								}
								
								// normalize the scores between 0 and 1
								float normScore = (searchResult.getScore() / maxScore);
								CompositeSearchResult csr = (io.isPresent() ? new CompositeSearchResult(io.get(), normScore) :
									new CompositeSearchResult(searchResult.getNid(), normScore));
								initialSearchResults.add(csr);
							}
						}
					}

					// sort, filter and merge the results as necessary
					processResults(searchHandle, initialSearchResults, filter, comparator, mergeOnConcepts, includeOffPathResults);
				}
				catch (Exception ex)
				{
					LOG.error("Unexpected error during lucene search", ex);
					searchHandle.setError(ex);
				}
				callback.taskComplete(searchHandle.getSearchStartTime(), taskId);
			}
		};

		Utility.execute(r);
		return searchHandle;
	}
	
	/**
	 * A convenience wrapper for {@link #dynamicRefexSearch(Function, TaskCompleteCallback, Integer, Function, Comparator, boolean)}
	 * which builds a function that handles basic string searches.
	 * 
	 * @param searchString - The value to search for within the refex index
	 * @param resultLimit - cap the number of results
	 * @param prefixSearch - use the prefix search text algorithm.  See {@link LuceneDescriptionIndexer#query(String, boolean, ComponentProperty, int, Long)} for 
	 *  a description on the prefix search algorithm.
	 * @param assemblageNid - (optional) limit the search to the specified assemblage type, or all assemblage objects (if null)
	 * @param callback - Pass the handle that you want to have notified when the search is complete, and the results are ready for use.
	 * @param taskId - An optional field that is simply handed back during the callback when results are complete.  Useful for matching 
	 *   requests to this method with callbacks.
	 * @param filter - An optional filter than can add or remove items from the tentative result set before it is returned.
	 * @param comparator - The comparator to use for sorting the results
	 * @param mergeOnConcepts - If true, when multiple description objects within the same concept match the search, this will be returned 
	 *   as a single result representing the concept - with each matching string listed, and the score being the best score of any of the 
	 *   matching strings.  When false, you will get one search result per description match - so concepts can be returned multiple times.
	 * @param includeOffPathResults - true to give back results for all hits (which may have unresolvable concepts) or false to filter those
	 *   out that are not on THE CURRENT COORDINATE configuration 
	 * @return A handle to the running search.
	 */
	public static SearchHandle dynamicRefexSearch(
			String searchString, 
			int resultLimit,
			boolean prefixSearch,
			Integer assemblageNid,
			final TaskCompleteCallback callback, 
			final Integer taskId, 
			final Function<List<CompositeSearchResult>, List<CompositeSearchResult>> filter,
			Comparator<CompositeSearchResult> comparator,
			boolean mergeOnConcepts, 
			boolean includeOffPathResults)
	{
		return dynamicRefexSearch(index -> 
			{
				try
				{
					return index.query(searchString, assemblageNid, prefixSearch, resultLimit, null);
				}
				catch (Exception e)
				{
					throw new RuntimeException(e);
				}
			}, callback, taskId, filter, comparator, mergeOnConcepts, includeOffPathResults);
	}
	
	private static void processResults(SearchHandle searchHandle, List<CompositeSearchResult> rawResults, 
			final Function<List<CompositeSearchResult>, List<CompositeSearchResult>>  filter, Comparator<CompositeSearchResult> comparator,
			boolean mergeOnConcepts, boolean includeOffPathResults)
					throws SearchResultsFilterException {
		
		//filter and sort the results
		
		if (filter != null) {
			LOG.debug("Applying SearchResultsFilter " + filter + " to " + rawResults.size() + " search results");
			rawResults = filter.apply(rawResults);

			LOG.debug(rawResults.size() + " results remained after running the filter");
		} 
		if (!includeOffPathResults)
		{
			LOG.debug("Applying Path filter to " + rawResults.size() + " search results");
			rawResults = new Function<List<CompositeSearchResult>, List<CompositeSearchResult>>()
			{
				@Override
				public List<CompositeSearchResult> apply(List<CompositeSearchResult> t)
				{
					Iterator<CompositeSearchResult> it = t.iterator();
					while (it.hasNext())
					{
						if (!it.next().getContainingConcept().isPresent())
						{
							it.remove();
						}
					}
					return t;
				}
			}.apply(rawResults);
			
			LOG.debug(rawResults.size() + " results remained after running path filter");
		}
		
		if (mergeOnConcepts)
		{
			Hashtable<Integer, CompositeSearchResult> merged = new Hashtable<>();
			ArrayList<CompositeSearchResult> unmergeable = new ArrayList<>();
			for (CompositeSearchResult csr : rawResults)
			{
				if (!csr.getContainingConcept().isPresent())
				{
					unmergeable.add(csr);
					continue;
				}
				CompositeSearchResult found = merged.get(csr.getContainingConcept().get().getNid());
				if (found == null)
				{
					merged.put(csr.getContainingConcept().get().getNid(), csr);
				}
				else
				{
					found.merge(csr);
				}
			}
			rawResults.clear();
			rawResults.addAll(merged.values());
			rawResults.addAll(unmergeable);
		}
		
		Collections.sort(rawResults, (comparator == null ? new CompositeSearchResultComparator() : comparator));
		
		searchHandle.setResults(rawResults);
	}
}
