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
 * QueryBasedIsDescendantOfSearchResultsFilter
 * 
 * @author <a href="mailto:joel.kniaz@gmail.com">Joel Kniaz</a>
 */
package gov.va.isaac.gui.enhancedsearchview.searchresultsfilters;

import gov.va.isaac.ExtendedAppContext;
import gov.va.isaac.gui.enhancedsearchview.filters.Invertable;
import gov.va.isaac.gui.enhancedsearchview.filters.IsAFilter;
import gov.va.isaac.gui.enhancedsearchview.filters.IsDescendantOfFilter;
import gov.va.isaac.gui.enhancedsearchview.filters.IsRefsetMemberFilter;
import gov.va.isaac.gui.enhancedsearchview.filters.NonSearchTypeFilter;
import gov.va.isaac.search.CompositeSearchResult;
import gov.va.isaac.search.SearchResultsFilterException;
import gov.va.isaac.util.OTFUtility;
import gov.va.isaac.util.OchreUtility;
import gov.vha.isaac.ochre.collections.NidSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import org.ihtsdo.otf.query.implementation.Clause;
import org.ihtsdo.otf.query.implementation.ComponentCollectionTypes;
import org.ihtsdo.otf.query.implementation.ForSetSpecification;
import org.ihtsdo.otf.query.implementation.Query;
import org.ihtsdo.otf.tcc.api.concept.ConceptVersionBI;
import org.ihtsdo.otf.tcc.api.spec.ConceptSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * is kind of filter (ConceptIsDescendentOf(concept UUID) clause)
 * rel kind filter (RelType clause)
 * Concept in member filter (RefsetContainsConcept(concept UUID, refset UUID) clause)
 * text in member filter
 * refset member filter
 * 
 */
class QueryBasedSearchResultsIntersectionFilter implements Function<List<CompositeSearchResult>, List<CompositeSearchResult>>  {
	private static final Logger LOG = LoggerFactory.getLogger(QueryBasedSearchResultsIntersectionFilter.class);

	private final List<NonSearchTypeFilter<?>> filters = new ArrayList<>();
	
	public QueryBasedSearchResultsIntersectionFilter(NonSearchTypeFilter<?>...passedFilters) throws SearchResultsFilterException {
		SearchResultsFilterHelper.validateFilters(passedFilters);
		for (NonSearchTypeFilter<?> filter : passedFilters) {
			filters.add(filter);
		}
	}
	public QueryBasedSearchResultsIntersectionFilter(Collection<NonSearchTypeFilter<?>> passedFilters) throws SearchResultsFilterException {
		this(passedFilters == null ? (NonSearchTypeFilter<?>[])null : new ArrayList<>(passedFilters).toArray(new NonSearchTypeFilter<?>[passedFilters.size()]));
	}
	
	
	/**
	 * @see java.util.function.Function#apply(java.lang.Object)
	 */
	@Override
	public List<CompositeSearchResult> apply(List<CompositeSearchResult> results)
	{
		SearchResultsFilterHelper.validateFilters(filters);
		
		final List<CompositeSearchResult> filteredResults = new ArrayList<>(results.size());

		// If no filters, pass all results straight through
		if (filters.size() == 0) {
			filteredResults.addAll(results);
			
			return filteredResults;
		}

		// TODO OTF Replacement NEEDS TESTING
		final Set<UUID> forSetCustomCollection = new HashSet<>();

		for (CompositeSearchResult result : results) {
			if (result.getContainingConcept() != null)
			{
				forSetCustomCollection.add(result.getContainingConcept().get().getPrimordialUuid());
			}
		}

		SearchResultsFilterHelper.LOG.debug("Building Query to filter " + forSetCustomCollection.size() + " search results");

		final ForSetSpecification forSetSpecification = new ForSetSpecification(ComponentCollectionTypes.CUSTOM_SET);
		forSetSpecification.setCustomCollection(forSetCustomCollection);

		Query q = null;

		q = new Query() {
			@Override
			public void Let() {
				for (NonSearchTypeFilter<?> filter : filters) {
					if (filter instanceof IsDescendantOfFilter) {
						ConceptVersionBI concept = OTFUtility.getConceptVersion(((IsDescendantOfFilter)filter).getNid());

						let(concept.getPrimordialUuid().toString(), new ConceptSpec(OchreUtility.getDescription(concept.getNid()).get(), concept.getPrimordialUuid()));
					} else if (filter instanceof IsAFilter) {
						ConceptVersionBI concept = OTFUtility.getConceptVersion(((IsAFilter)filter).getNid());

						let(concept.getPrimordialUuid().toString(), new ConceptSpec(OchreUtility.getDescription(concept.getNid()).get(), concept.getPrimordialUuid()));
					} else if (filter instanceof IsRefsetMemberFilter) {
						ConceptVersionBI concept = OTFUtility.getConceptVersion(((IsRefsetMemberFilter)filter).getNid());

						let(concept.getPrimordialUuid().toString(), new ConceptSpec(OchreUtility.getDescription(concept.getNid()).get(), concept.getPrimordialUuid()));
					} else {
						// This should never happen, since validateFilters(filters) was already called
						throw new RuntimeException(new SearchResultsFilterException(QueryBasedSearchResultsIntersectionFilter.this, "Unsupported NonSearchTypeFilter " + filter.getClass().getName() + ". Curently only IsDescendantOfFilter and IsAFilter supported"));
					}
				}
			}

			@Override
			public Clause Where() {
				//						return And(ConceptIsKindOf("Physical force"),
				//									Xor(ConceptIsKindOf("Motion"),
				//			
//				Clause c = (first one)
//						for (filter f : filters)
//						{
//						 c = And(c, new one)
//						}ConceptIsDescendentOf("Motion")));
						
				ArrayList<Clause> subclauses = new ArrayList<>();
				
				for (NonSearchTypeFilter<?> filter : filters) {
					Clause currentClause = null;
					if (filter instanceof IsDescendantOfFilter) {
						ConceptVersionBI concept = OTFUtility.getConceptVersion(((IsDescendantOfFilter)filter).getNid());

						currentClause = ConceptIsDescendentOf(concept.getPrimordialUuid().toString());
					} else if (filter instanceof IsAFilter) {
						ConceptVersionBI concept = OTFUtility.getConceptVersion(((IsAFilter)filter).getNid());

						currentClause = ConceptIs(concept.getPrimordialUuid().toString());
					}
					else if (filter instanceof IsRefsetMemberFilter) {
						ConceptVersionBI concept = OTFUtility.getConceptVersion(((IsRefsetMemberFilter)filter).getNid());

						currentClause = RefsetContainsConcept(concept.getPrimordialUuid().toString(), "dummy");
					} 
					else {
						// This should never happen, since validateFilters(filters) was already called
						String msg = "Unsupported NonSearchTypeFilter " + filter.getClass().getName() + ". Curently only IsDescendantOfFilter and IsAFilter supported";
						LOG.error(msg);
						throw new RuntimeException(new SearchResultsFilterException(QueryBasedSearchResultsIntersectionFilter.this, msg));
					}
					
					if (filter instanceof Invertable && ((Invertable)filter).getInvert()) {
						currentClause = Not(currentClause);
					}
					
					subclauses.add(currentClause);
				}
				
				if (subclauses.size() > 1) {
					return And(subclauses.toArray(new Clause[subclauses.size()]));
				} else if (subclauses.size() == 1) {
					return subclauses.get(0);
				} else {
					// Should never happen.  If no filters, then should never get here.
					LOG.error("No clauses generated for " + filters.size() + " filters: " + Arrays.toString(filters.toArray()));

					return null;
				}
			}

			@Override
			protected ForSetSpecification ForSetSpecification() {
				return forSetSpecification;
			}
		};

		NidSet outputNids = null;
		try {
			SearchResultsFilterHelper.LOG.debug("Applying " + filters.size() + " clauses to " + forSetCustomCollection.size() + " uuids");

			q.setTaxonomyCoordinate(ExtendedAppContext.getUserProfileBindings().getTaxonomyCoordinate().get());
			outputNids = q.compute();
			
			SearchResultsFilterHelper.LOG.debug(outputNids.size() + " nids remained after applying " + filters.size() + " clauses to filtering a total of " + forSetCustomCollection.size() + " uuids");
		} catch (Exception e) {
			String msg = "Failed calling Query.compute() for " + filters.size() + " filters: " + Arrays.toString(filters.toArray());
			LOG.error(msg);
			throw new SearchResultsFilterException(this, msg, e);
		}
		
		for (CompositeSearchResult result : results) {
			if (result.getContainingConcept() != null)
			{
				if (outputNids.contains(result.getContainingConcept().get().getNid())) {
					filteredResults.add(result);
				}
			}
		}
		
		return filteredResults;
	}

	@Override
	public String toString() {
		return "QueryBasedSearchResultsIntersectionFilter [filters=" + Arrays.toString(filters.toArray()) + "]";
	}
}
