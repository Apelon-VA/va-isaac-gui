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
 * SearchConceptHelper
 * 
 * @author <a href="mailto:joel.kniaz@gmail.com">Joel Kniaz</a>
 */
package gov.va.isaac.gui.enhancedsearchview;

import java.beans.PropertyVetoException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import javax.naming.InvalidNameException;
import org.ihtsdo.otf.tcc.api.blueprint.ConceptAttributeAB;
import org.ihtsdo.otf.tcc.api.blueprint.InvalidCAB;
import org.ihtsdo.otf.tcc.api.blueprint.RefexDirective;
import org.ihtsdo.otf.tcc.api.blueprint.TerminologyBuilderBI;
import org.ihtsdo.otf.tcc.api.concept.ConceptChronicleBI;
import org.ihtsdo.otf.tcc.api.concept.ConceptVersionBI;
import org.ihtsdo.otf.tcc.api.contradiction.ContradictionException;
import org.ihtsdo.otf.tcc.api.coordinate.ViewCoordinate;
import org.ihtsdo.otf.tcc.api.spec.ConceptSpec;
import org.ihtsdo.otf.tcc.api.spec.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import gov.va.isaac.AppContext;
import gov.va.isaac.ExtendedAppContext;
import gov.va.isaac.gui.enhancedsearchview.SearchTypeEnums.SearchType;
import gov.va.isaac.gui.enhancedsearchview.filters.Filter;
import gov.va.isaac.gui.enhancedsearchview.filters.Invertable;
import gov.va.isaac.gui.enhancedsearchview.filters.IsAFilter;
import gov.va.isaac.gui.enhancedsearchview.filters.IsDescendantOfFilter;
import gov.va.isaac.gui.enhancedsearchview.filters.LuceneSearchTypeFilter;
import gov.va.isaac.gui.enhancedsearchview.filters.NonSearchTypeFilter;
import gov.va.isaac.gui.enhancedsearchview.filters.RegExpSearchTypeFilter;
import gov.va.isaac.gui.enhancedsearchview.filters.SearchTypeFilter;
import gov.va.isaac.gui.enhancedsearchview.filters.SememeContentSearchTypeFilter;
import gov.va.isaac.gui.enhancedsearchview.model.SearchModel;
import gov.va.isaac.gui.enhancedsearchview.model.type.sememe.SememeSearchTypeModel;
import gov.va.isaac.gui.enhancedsearchview.model.type.text.TextSearchTypeModel;
import gov.va.isaac.util.ComponentDescriptionHelper;
import gov.va.isaac.util.OTFUtility;
import gov.vha.isaac.metadata.coordinates.ViewCoordinates;
import gov.vha.isaac.ochre.api.component.concept.ConceptSnapshot;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeUsageDescriptionBI;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.dataTypes.DynamicSememeBooleanBI;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.dataTypes.DynamicSememeByteArrayBI;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.dataTypes.DynamicSememeIntegerBI;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.dataTypes.DynamicSememeStringBI;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.dataTypes.DynamicSememeUUIDBI;
import gov.vha.isaac.ochre.model.constants.QueryBuilderConstants;
import gov.vha.isaac.ochre.model.sememe.dataTypes.DynamicSememeBoolean;
import gov.vha.isaac.ochre.model.sememe.dataTypes.DynamicSememeByteArray;
import gov.vha.isaac.ochre.model.sememe.dataTypes.DynamicSememeData;
import gov.vha.isaac.ochre.model.sememe.dataTypes.DynamicSememeInteger;
import gov.vha.isaac.ochre.model.sememe.dataTypes.DynamicSememeString;
import gov.vha.isaac.ochre.model.sememe.dataTypes.DynamicSememeUUID;

/**
 * SearchConceptHelper
 * 
 * @author <a href="mailto:joel.kniaz@gmail.com">Joel Kniaz</a>
 *
 */
@SuppressWarnings("unused")
public class SearchConceptHelper {
	public static class SearchConceptException extends Exception {
		private static final long serialVersionUID = 1L;

		public SearchConceptException(String message, Throwable cause,
				boolean enableSuppression, boolean writableStackTrace) {
			super(message, cause, enableSuppression, writableStackTrace);
		}

		public SearchConceptException(String message, Throwable cause) {
			super(message, cause);
		}

		public SearchConceptException(String message) {
			super(message);
		}

		public SearchConceptException(Throwable cause) {
			super(cause);
		}
	}

	private static final Logger LOG = LoggerFactory.getLogger(SearchConceptHelper.class);

//	private static ConceptChronicleBI buildSearchConcept(
//			SearchModel model,
//			String passedSaveConceptFSN,
//			String passedSaveConceptPT) throws SearchConceptException
//	{
//		final String saveConceptFSN = passedSaveConceptFSN != null ? passedSaveConceptFSN : SearchModel.getSearchTypeSelector().getTypeSpecificModel().getName();
//		final String saveConceptPT = passedSaveConceptPT != null ? passedSaveConceptPT : SearchModel.getSearchTypeSelector().getTypeSpecificModel().getDescription();
//
//		LOG.debug("buildSearchConcept(): saving model for search fsn=\"" + saveConceptFSN + "\", pt=\"" + saveConceptPT + "\": " + model);
//		//
//		// Construct new containing Search Concept
//		//
//		// Construct Search Global Attributes refex containing vc (View Coordinate)
//		//		Populate vc from GUI 
//		//
//		// Add Search Global Attributes refex to containing Search Concept
//		//
//		// For each Filter spec in GUI
//		//		Construct appropriate new Search {Lucene|RegExp} Filter refex containing param
//		//			Populate param from Filter spec in GUI
//		//			Construct new nested Search Filter Attributes refex containing order
//		//				Populate nested Search Filter Attributes refex with order from GUI
//		//			Nest nested Search Filter Attributes refex within Search {Lucene|RegExp} Filter refex
//		//
//		//		Add Search {Lucene|RegExp} Filter refex to containing Search Concept
//		//
//
//		try {
//			ConceptChronicleBI searchConcept = OTFUtility.createNewConcept(OTFUtility.getConceptVersion(QueryBuilderConstants.STORED_QUERIES.getUuids()[0]), saveConceptFSN, saveConceptPT);
//			ConceptAttributeAB conceptAttributeBlueprintAmender = new ConceptAttributeAB(searchConcept.getConceptNid(), 
//					searchConcept.getVersion(OTFUtility.getViewCoordinate()).get().getConceptAttributesActive().get().isDefined(), RefexDirective.INCLUDE); 
//
//			{
//				// Start with Search Global Attributes
//				DynamicSememeUsageDescription searchGlobalAttributesRDUD = DynamicSememeUsageDescriptionBuilder.readDynamicSememeUsageDescriptionConcept(QueryBuilderConstants.STORED_QUERY_GLOBAL_ATTRIBUTES.getNid());
//
//				// Add View Coordinate byte[]
//				DynamicSememeData[] searchGlobalAttributesData = new DynamicSememeData[searchGlobalAttributesRDUD.getColumnInfo().length];
//
//				// Serialize passed View Coordinate into byte[]
//				ByteArrayOutputStream output = new ByteArrayOutputStream();
//				ObjectOutputStream oos = new ObjectOutputStream(output);
//				SearchModel.getSearchTypeSelector().getTypeSpecificModel().getViewCoordinate().writeExternal(oos);
//				oos.flush();
//
//				// Construct and populate DynamicSememeData for View Coordinate
//				DynamicSememeData viewCoordinateColumnData = new DynamicSememeByteArray(output.toByteArray());
//				searchGlobalAttributesData[0] = viewCoordinateColumnData;
//				DynamicSememeData maxResultsColumnData = new DynamicSememeInteger(SearchModel.getSearchTypeSelector().getTypeSpecificModel().getMaxResults());
//				searchGlobalAttributesData[1] = maxResultsColumnData;
//				if (SearchModel.getSearchTypeSelector().getTypeSpecificModel().getDroolsExpr() != null) {
//					DynamicSememeData droolsExprColumnData = new DynamicSememeString(SearchModel.getSearchTypeSelector().getTypeSpecificModel().getDroolsExpr());
//					searchGlobalAttributesData[2] = droolsExprColumnData;
//				}
//
//				DynamicSememeCAB globalAttributesCAB;
//				// cab.addAnnotationBlueprint(annotationBlueprint); for nesting
//				//if (inputType_.getRefex() == null)
//				//{
//
//				// Creates new refex
//				globalAttributesCAB = new DynamicSememeCAB(searchConcept.getPrimordialUuid(), QueryBuilderConstants.STORED_QUERY_GLOBAL_ATTRIBUTES.getUuids()[0]);
//				//}
//				//else
//				//{
//				// This only for editing existing concept
//				//cab = inputType_.getRefex().makeBlueprint(OTFUtility.getViewCoordinate(),IdDirective.PRESERVE, RefexDirective.INCLUDE);
//				//}
//				globalAttributesCAB.setData(searchGlobalAttributesData, OTFUtility.getViewCoordinate());
//
//				conceptAttributeBlueprintAmender.addAnnotationBlueprint(globalAttributesCAB);
//
//				TerminologyBuilderBI builder = ExtendedAppContext.getDataStore().getTerminologyBuilder(OTFUtility.getEditCoordinate(), OTFUtility.getViewCoordinate());
//				builder.construct(globalAttributesCAB);
//			}
//
//			if (SearchModel.getSearchTypeSelector().getCurrentType() == SearchType.TEXT) {
//				TextSearchTypeModel compModel = (TextSearchTypeModel)SearchModel.getSearchTypeSelector().getTypeSpecificModel();
//				
//				// Add search type filter as index 0
//				addFilterToRefex(searchConcept, conceptAttributeBlueprintAmender, compModel.getSearchType(), 0);
//	
//				// Add additional filter as higher indices
//				for (int filterIndex = 0; filterIndex < compModel.getFilters().size(); ++filterIndex) {
//					addFilterToRefex(searchConcept, conceptAttributeBlueprintAmender, compModel.getFilters().get(filterIndex), filterIndex + 1);
//				}
//			} else if (SearchModel.getSearchTypeSelector().getCurrentType() == SearchType.SEMEME) {
//				SememeSearchTypeModel compModel = (SememeSearchTypeModel)SearchModel.getSearchTypeSelector().getTypeSpecificModel();
//
//				addFilterToRefex(searchConcept, conceptAttributeBlueprintAmender, compModel.getSearchType(), 0);
//			}
//			
//			LOG.debug("Displaying newly created save concept refexes");
//			DynamicRefexHelper.displayDynamicRefexes(searchConcept);
//			
//			return searchConcept;
//		} catch (IOException | InvalidCAB | ContradictionException | PropertyVetoException e) {
//			throw new SearchConceptException(e.getLocalizedMessage(), e);
//		}
//	}
//
//	private static void addFilterToRefex(ConceptChronicleBI searchConcept, ConceptAttributeAB conceptAttributeBlueprintAmender, Filter<? extends Filter<?>> currentFilter, int filterIndex) throws ValidationException, PropertyVetoException, IOException, ContradictionException, SearchConceptException, InvalidCAB {
//		DynamicSememeUsageDescription filterRDUD = null;
//
//		if (currentFilter instanceof SearchTypeFilter) {
//			if (filterIndex > 0) {
//				throw new SearchConceptException("A SearchTypeFilter Filter must be first in sememe list. " + currentFilter.getClass().getName() + " is a SearchTypeFilter filter, but its specified index is " + filterIndex);
//			}
//		} else /* if (! (currentFilter instanceof SearchTypeFilter)) */ {
//			if (filterIndex == 0) {
//				throw new SearchConceptException("A non-SearchTypeFilter Filter must not be first in sememe list. " + currentFilter.getClass().getName() + " is a non-SearchTypeFilter filter, but its specified index is " + filterIndex);
//			}
//		}
//
//		ConceptSpec filterConceptSpec = null;
//		if (currentFilter instanceof LuceneSearchTypeFilter) {
//			filterConceptSpec = QueryBuilderConstants.SEARCH_LUCENE_FILTER;
//		} else if (currentFilter instanceof RegExpSearchTypeFilter) {
//			filterConceptSpec = QueryBuilderConstants.SEARCH_REGEXP_FILTER;
//		} else if (currentFilter instanceof SememeContentSearchTypeFilter) {
//			filterConceptSpec = QueryBuilderConstants.SEARCH_SEMEME_CONTENT_FILTER;
//		} else if (currentFilter instanceof IsDescendantOfFilter) {
//			filterConceptSpec = QueryBuilderConstants.SEARCH_ISDESCENDANTOF_FILTER;
//		} else if (currentFilter instanceof IsAFilter) {
//			filterConceptSpec = QueryBuilderConstants.SEARCH_ISA_FILTER;
//		} else {
//			throw new SearchConceptException("Unsupported Filter type " + currentFilter.getClass().getName());
//		}
//
//		filterRDUD = DynamicSememeUsageDescriptionBuilder.readDynamicSememeUsageDescriptionConcept(filterConceptSpec.getNid());
//
//		// First create Filter, which has its own attributes plus common Filter attributes
//		DynamicSememeData[] filterRefexData = new DynamicSememeData[filterRDUD.getColumnInfo().length];
//
//		if (currentFilter instanceof LuceneSearchTypeFilter) {
//			// Construct and populate DynamicSememeData for search parameter
//			LuceneSearchTypeFilter filter = (LuceneSearchTypeFilter)currentFilter;
//
//			if (filter.getSearchParameter() != null) {
//
//				DynamicSememeData searchParameterData = new DynamicSememeString(filter.getSearchParameter());
//				filterRefexData[0] = searchParameterData;
//			}
//		} else if (currentFilter instanceof SememeContentSearchTypeFilter) {
//			// Construct and populate DynamicSememeData for search parameter
//			SememeContentSearchTypeFilter filter = (SememeContentSearchTypeFilter)currentFilter;
//
//			if (filter.getSearchParameter() != null) {
//				DynamicSememeData searchParameterData = new DynamicSememeString(filter.getSearchParameter());
//				filterRefexData[0] = searchParameterData;
//			} else {
//				final String error = "Attempting to save SememeContentSearchTypeFilter with null search string parameter";
//				LOG.warn(error);
//				throw new SearchConceptException(error);
//			}
//			if (filter.getAssemblageConcept() != null) {
//				ConceptSnapshot concept = filter.getAssemblageConcept();
//				DynamicSememeData assemblageConceptIdData = new DynamicSememeUUID(concept.getPrimordialUuid());
//				filterRefexData[1] = assemblageConceptIdData;
//			}
//		} else if (currentFilter instanceof RegExpSearchTypeFilter) {
//			// Construct and populate DynamicSememeData for search parameter
//			RegExpSearchTypeFilter filter = (RegExpSearchTypeFilter)currentFilter;
//
//			if (filter.getSearchParameter() != null) {
//
//				DynamicSememeData searchParameterData = new DynamicSememeString(filter.getSearchParameter());
//				filterRefexData[0] = searchParameterData;
//			}
//		} else if (currentFilter instanceof IsDescendantOfFilter) {
//			// Construct and populate DynamicSememeData for search ascendant uuid
//			IsDescendantOfFilter isDescendantOfFilter = (IsDescendantOfFilter)currentFilter;
//
//			if (isDescendantOfFilter.getNid() != 0) {
//				UUID uuid = OTFUtility.getConceptVersion(isDescendantOfFilter.getNid()).getPrimordialUuid();
//				DynamicSememeData ascendantUuidData = new DynamicSememeUUID(uuid);
//				filterRefexData[0] = ascendantUuidData;
//			}
//		} else if (currentFilter instanceof IsAFilter) {
//			// Construct and populate DynamicSememeData for search ascendant uuid
//			IsAFilter isAFilter = (IsAFilter)currentFilter;
//
//			if (isAFilter.getNid() != 0) {
//				UUID uuid = OTFUtility.getConceptVersion(isAFilter.getNid()).getPrimordialUuid();
//				DynamicSememeData matchUuidData = new DynamicSememeUUID(uuid);
//				filterRefexData[0] = matchUuidData;
//			}
//		}
//
//		DynamicSememeCAB filterRefexCAB;
//		// cab.addAnnotationBlueprint(annotationBlueprint); for nesting
//		//if (inputType_.getRefex() == null)
//		//{
//
//		// Creates new refex
//		filterRefexCAB = new DynamicSememeCAB(searchConcept.getPrimordialUuid(), filterConceptSpec.getUuids()[0]);
//		//}
//		//else
//		//{
//		// This only for editing existing concept
//		//cab = inputType_.getRefex().makeBlueprint(OTFUtility.getViewCoordinate(),IdDirective.PRESERVE, RefexDirective.INCLUDE);
//		//}
//		filterRefexCAB.setData(filterRefexData, OTFUtility.getViewCoordinate());
//
//		conceptAttributeBlueprintAmender.addAnnotationBlueprint(filterRefexCAB);
//
//		// Handle Search Filter Attributes for Filter
//		DynamicSememeUsageDescriptionBI nestedFilterAttributesRDUD = DynamicSememeUsageDescriptionBuilder.readDynamicSememeUsageDescriptionConcept(QueryBuilderConstants.SEARCH_FILTER_ATTRIBUTES.getNid());
//
//		// Common Filter attributes
//		DynamicSememeData[] nestedFilterAttributesRefexData = new DynamicSememeData[nestedFilterAttributesRDUD.getColumnInfo().length];
//
//		// Construct and populate DynamicSememeData for search order
//		DynamicSememeData filterOrderData = new DynamicSememeInteger(filterIndex);
//		nestedFilterAttributesRefexData[0] = filterOrderData;
//		// If relevant, populate with invert boolean
//		if (currentFilter instanceof Invertable) {
//			DynamicSememeData filterInvertData = new DynamicSememeBoolean(((Invertable)currentFilter).getInvert());
//			nestedFilterAttributesRefexData[1] = filterInvertData;
//		}
//
//		DynamicSememeCAB nestedFilterAttributesCAB;
//		// cab.addAnnotationBlueprint(annotationBlueprint); for nesting
//		//if (inputType_.getRefex() == null)
//		//{
//
//		// Creates new refex
//		nestedFilterAttributesCAB = new DynamicSememeCAB(filterRefexCAB.getComponentUuid(), QueryBuilderConstants.SEARCH_FILTER_ATTRIBUTES.getUuids()[0]);
//		//}
//		//else
//		//{
//		// This only for editing existing concept
//		//cab = inputType_.getRefex().makeBlueprint(OTFUtility.getViewCoordinate(),IdDirective.PRESERVE, RefexDirective.INCLUDE);
//		//}
//		nestedFilterAttributesCAB.setData(nestedFilterAttributesRefexData, OTFUtility.getViewCoordinate());
//
//		filterRefexCAB.addAnnotationBlueprint(nestedFilterAttributesCAB);
//
//		TerminologyBuilderBI builder = ExtendedAppContext.getDataStore().getTerminologyBuilder(OTFUtility.getEditCoordinate(), OTFUtility.getViewCoordinate());
//		builder.construct(filterRefexCAB);
//	}

//	public static void buildAndSaveSearchConcept(SearchModel model) throws SearchConceptException {
//		buildAndSaveSearchConcept(model, null, null);
//	}
//	public static void buildAndSaveSearchConcept(
//			SearchModel model,
//			String passedSaveConceptFSN,
//			String passedSaveConceptPT) throws SearchConceptException
//	{
//		final String saveConceptFSN = passedSaveConceptFSN != null ? passedSaveConceptFSN : SearchModel.getSearchTypeSelector().getTypeSpecificModel().getName();
//		final String saveConceptPT = passedSaveConceptPT != null ? passedSaveConceptPT : SearchModel.getSearchTypeSelector().getTypeSpecificModel().getDescription();
//
//		LOG.debug("buildAndSaveSearchConcept(): saving concept for search fsn=\"" + saveConceptFSN + "\", pt=\"" + saveConceptPT + "\": " + model);
//
//		try {
//			ConceptChronicleBI searchConcept = buildSearchConcept(model, saveConceptFSN, saveConceptPT);
//
//			ExtendedAppContext.getDataStore().addUncommitted(searchConcept);
//		    try
//			{
//				AppContext.getRuntimeGlobals().disableAllCommitListeners();
//				// TODO OCHRE add add confirm to alert user that all uncommitted concepts will be committed on persist of save criteria
//				ExtendedAppContext.getDataStore().commit(/* searchConcept */);
//			} catch (Exception e) {
//				LOG.error("Coudn't Disable WF Init & Commit Creation of Search Concept", e);
//			}
//			finally
//			{
//			    AppContext.getRuntimeGlobals().enableAllCommitListeners();
//			}
//
//		} catch (IOException e) {
//			throw new SearchConceptException(e.getLocalizedMessage(), e);
//		}
//	}
//
//	private static <T extends SearchTypeFilter<T>> void loadEmbeddedSearchTypeFilterAttributes(DynamicSememeVersionBI<?> refex, T newFilter) throws InvalidNameException, IndexOutOfBoundsException, IOException, ContradictionException, SearchConceptException {
//		loadEmbeddedSearchFilterAttributes(refex, newFilter, null);
//	}
//
//	private static <T extends NonSearchTypeFilter<T>> void loadEmbeddedSearchFilterAttributes(DynamicSememeVersionBI<?> refex, Filter<?> newFilter, Map<Integer, Collection<T>> filterOrderMap) throws InvalidNameException, IndexOutOfBoundsException, IOException, ContradictionException, SearchConceptException {
//		LOG.debug("Loading data into model from embedded Search Filter Attributes sememe");
//
//		SearchTypeFilter<?> searchTypeFilter = null;
//		T nonSearchTypeFilter = null;
//		
//		if (newFilter instanceof SearchTypeFilter) {
//			searchTypeFilter = (SearchTypeFilter<?>)newFilter;
//			if (filterOrderMap != null) {
//				throw new SearchConceptException("Cannot call this method with a non-null filterOrderMap when passing a SearchTypeFilter. " + newFilter.getClass().getName() + " is a SearchTypeFilter filter.");
//			}
//		} else if (newFilter instanceof NonSearchTypeFilter) {
//			nonSearchTypeFilter = (T)newFilter;
//			if (filterOrderMap == null) {
//				throw new SearchConceptException("Cannot call this method with a null filterOrderMap when passing a non-SearchTypeFilter. " + newFilter.getClass().getName() + " is a non-SearchTypeFilter filter.");
//			}
//		} else {
//			throw new SearchConceptException("Unsupported Filter type " + newFilter.getClass().getName() + ". Must be of either " + SearchTypeFilter.class.getName() + " or " + NonSearchTypeFilter.class.getName());
//		}
//		
//		// Now read SEARCH_FILTER_ATTRIBUTES refex column
//		for (DynamicSememeVersionBI<?> embeddedRefex : refex.getRefexesDynamicActive(OTFUtility.getViewCoordinate())) {
//			DynamicRefexHelper.displayDynamicRefex(embeddedRefex);
//
//			DynamicSememeUsageDescription embeddedRefexDUD = null;
//			try {
//				embeddedRefexDUD = embeddedRefex.getDynamicSememeUsageDescription();
//			} catch (IOException | ContradictionException e) {
//				LOG.error("Failed performing getDynamicSememeUsageDescription() on embedded sememe: caught " + e.getClass().getName() + " \"" + e.getLocalizedMessage() + "\"", e);
//
//				return;
//			}
//
//			if (embeddedRefexDUD.getRefexName().equals(QueryBuilderConstants.SEARCH_FILTER_ATTRIBUTES.getConceptDescriptionText() /*"Search Filter Attributes"*/)) {
//				DynamicSememeIntegerBI filterOrderCol = (DynamicSememeIntegerBI)embeddedRefex.getData(QueryBuilderConstants.ORDER_COLUMN.getConceptDescriptionText());
//
//				if (newFilter instanceof SearchTypeFilter) {
//					if (filterOrderCol != null && filterOrderCol.getDataInteger() > 0) {
//						throw new SearchConceptException("A SearchTypeFilter Filter must be first in list. " + newFilter.getClass().getName() + " is a SearchTypeFilter filter, but its stored index is " + filterOrderCol.getDataInteger());
//					}
//				} else /* if (! (currentFilter instanceof SearchTypeFilter)) */ {
//					if (filterOrderMap.get(filterOrderCol.getDataInteger()) == null) {
//						filterOrderMap.put(filterOrderCol.getDataInteger(), new ArrayList<>());
//					}
//					filterOrderMap.get(filterOrderCol.getDataInteger()).add(nonSearchTypeFilter);
//
//					if (filterOrderCol == null || filterOrderCol.getDataInteger() == 0) {
//						throw new SearchConceptException("A non-SearchTypeFilter Filter must not be first in list. " + newFilter.getClass().getName() + " is a non-SearchTypeFilter filter, but its stored index is " + (filterOrderCol != null ? filterOrderCol.getDataInteger() : filterOrderCol));
//					}
//				}
//
//				LOG.debug("Read Integer filter order from " + embeddedRefexDUD.getRefexName() + " sememe: \"" + (filterOrderCol != null ? filterOrderCol.getDataInteger() : filterOrderCol) + "\"");
//
//				DynamicSememeBooleanBI filterInvertCol = (DynamicSememeBooleanBI)embeddedRefex.getData(QueryBuilderConstants.FILTER_INVERT_COLUMN.getConceptDescriptionText());
//				if (filterInvertCol != null) {
//					if (newFilter instanceof Invertable) {
//						((Invertable) newFilter).setInvert(filterInvertCol.getDataBoolean());
//						LOG.debug("Read Boolean filter invert from " + embeddedRefexDUD.getRefexName() + " sememe: \"" + filterInvertCol.getDataBoolean() + "\"");
//
//					} else if (filterInvertCol.getDataBoolean()) {
//						LOG.error("Cannot make invertable non-invertable Filter of type " + newFilter.getClass().getName());
//
//						throw new SearchConceptException("Cannot make invertable non-invertable Filter of type " + newFilter.getClass().getName());
//					}
//				}
//			} else {
//				LOG.warn("Encountered unexpected embedded sememe \"" + embeddedRefexDUD.getRefexName() + "\". Ignoring...");
//			}
//		}
//	}

	public static SearchModel loadSavedSearch(SearchDisplayConcept displayConcept) throws SearchConceptException {
		LOG.info("loadSavedSearch(" + displayConcept + ")");

		SearchModel model = null;

		try {
			throw new RuntimeException("load saved search disabled");
//			ConceptVersionBI matchingConcept = OTFUtility.getConceptVersion(displayConcept.getNid());
//
//			if (matchingConcept != null) {
//				LOG.debug("loadSavedSearch(): savedSearchesComboBox has concept: " + matchingConcept);
//
//				Map<Integer, Collection<NonSearchTypeFilter>> filterOrderMap = new TreeMap<>();
//
//				model = new SearchModel();
//				
//				if (SearchModel.getSearchTypeSelector() != null) {
//					if (SearchModel.getSearchTypeSelector().getTypeSpecificModel() == null) {
//						LOG.debug("SearchModel.getSearchTypeSelector().getTypeSpecificModel() == null");
//					} else {
//						LOG.debug("SearchModel.getSearchTypeSelector().getTypeSpecificModel(): name=\"{}\", desc=\"{}\", class={}",
//								SearchModel.getSearchTypeSelector().getTypeSpecificModel().getName(),
//								SearchModel.getSearchTypeSelector().getTypeSpecificModel().getDescription(),
//								SearchModel.getSearchTypeSelector().getTypeSpecificModel().getClass().getName());
//						
//						if (SearchModel.getSearchTypeSelector().getTypeSpecificModel() instanceof TextSearchTypeModel) {
//							TextSearchTypeModel compModel = (TextSearchTypeModel)SearchModel.getSearchTypeSelector().getTypeSpecificModel();
//							compModel.getFilters().clear();
//							compModel.getDroolsExprProperty().set(null);
//						}
//					}
//				}
//
//				SearchModel.getSearchTypeSelector().getTypeSpecificModel().setName(OTFUtility.getFullySpecifiedName(matchingConcept));
//				SearchModel.getSearchTypeSelector().getTypeSpecificModel().setDescription(OTFUtility.getConPrefTerm(matchingConcept.getNid()));
//
//				try {
//					LOG.debug("loadSavedSearch(): concept \"" + displayConcept + "\" all refexes: " +  matchingConcept.getRefexes().size());
//					LOG.debug("loadSavedSearch(): concept \"" + displayConcept + "\" all dynamic sememes: " +  matchingConcept.getRefexesDynamic().size());
//					LOG.debug("loadSavedSearch(): concept \"" + displayConcept + "\" active dynamic sememes (StandardViewCoordinates.getWbAuxiliary()): " 
//							+  matchingConcept.getRefexesDynamicActive(ViewCoordinates.getMetadataViewCoordinate()).size());
//					LOG.debug("loadSavedSearch(): concept \"" + displayConcept + "\" active dynamic sememes (OTFUtility.getViewCoordinate()): " 
//							+  matchingConcept.getRefexesDynamicActive(OTFUtility.getViewCoordinate()).size());
//			
//					LOG.debug("Displaying newly loaded save concept refexes");
//					DynamicRefexHelper.displayDynamicRefexes(matchingConcept);
//
//				} catch (IOException e) {
//					LOG.warn("Failed displaying attached refexes. Caught " + e.getClass().getName() + " \"" + e.getLocalizedMessage() + "\"");
//					e.printStackTrace();
//				}
//
//				int i = 0;
//				Collection<? extends DynamicSememeVersionBI<?>> refexes = matchingConcept.getRefexesDynamicActive(OTFUtility.getViewCoordinate());
//				for (DynamicSememeVersionBI<?> refex : refexes) {
//					LOG.debug("Displaying sememe #" + (++i) + " of " + refexes.size());
//					DynamicRefexHelper.displayDynamicRefex(refex);
//
//					DynamicSememeUsageDescription dud = null;
//					try {
//						dud = refex.getDynamicSememeUsageDescription();
//					} catch (IOException | ContradictionException e) {
//						LOG.error("Failed performing getDynamicSememeUsageDescription(): caught " + e.getClass().getName() + " \"" + e.getLocalizedMessage() + "\"", e);
//
//						return null;
//					}
//
//					if (SearchModel.getSearchTypeSelector().getCurrentType() == SearchType.TEXT) {
//						TextSearchTypeModel compModel = (TextSearchTypeModel)SearchModel.getSearchTypeSelector().getTypeSpecificModel();
//						
//						if (dud.getRefexName().equals(QueryBuilderConstants.STORED_QUERY_GLOBAL_ATTRIBUTES.getConceptDescriptionText() /*"Search Global Attributes"*/)) {
//							// handle "Search Global Attributes"
//	
//							LOG.debug("Loading data into model from Search Global Attributes sememe");
//	
//							// Loading view coordinate
//							DynamicSememeByteArrayBI serializedViewCoordinate = (DynamicSememeByteArrayBI)refex.getData(QueryBuilderConstants.VIEW_COORDINATE_COLUMN.getConceptDescriptionText());
//	
//							// Serialize passed View Coordinate into byte[]serializedViewCoordinate.getData()
//							ByteArrayInputStream input = new ByteArrayInputStream(serializedViewCoordinate.getDataByteArray());
//	
//							ObjectInputStream oos = new ObjectInputStream(input);
//							ViewCoordinate vc = new ViewCoordinate();
//							vc.readExternal(oos);
//							SearchModel.getSearchTypeSelector().getTypeSpecificModel().setViewCoordinate(vc);
//							LOG.debug("Read View Coordinate from " + dud.getRefexName() + " sememe: " + SearchModel.getSearchTypeSelector().getTypeSpecificModel().getViewCoordinate());
//	
//							// Loading maxResults
//							DynamicSememeIntegerBI maxResults = (DynamicSememeIntegerBI)refex.getData(QueryBuilderConstants.MAX_RESULTS_COLUMN.getConceptDescriptionText());
//							SearchModel.getSearchTypeSelector().getTypeSpecificModel().setMaxResults(maxResults.getDataInteger());
//							LOG.debug("Read max results from " + dud.getRefexName() + " sememe: " + SearchModel.getSearchTypeSelector().getTypeSpecificModel().getMaxResults());
//	
//							// Loading drools expression
//							DynamicSememeStringBI droolsExpr = (DynamicSememeStringBI)refex.getData(QueryBuilderConstants.DROOLS_EXPR_COLUMN.getConceptDescriptionText());
//							SearchModel.getSearchTypeSelector().getTypeSpecificModel().setDroolsExpr(droolsExpr != null ? droolsExpr.getDataString() : null);
//							LOG.debug("Read drools expression from " + dud.getRefexName() + " sememe: " + SearchModel.getSearchTypeSelector().getTypeSpecificModel().getDroolsExpr());
//	
//						} else if (dud.getRefexName().equals(QueryBuilderConstants.SEARCH_LUCENE_FILTER.getConceptDescriptionText() /*"Search Lucene Filter"*/)) {
//							// handle "Search Lucene Filter"
//	
//							LOG.debug("Loading data into model from Search Lucene Filter sememe");
//	
//							LuceneSearchTypeFilter newFilter = new LuceneSearchTypeFilter();
//	
//							DynamicSememeStringBI searchParamCol = (DynamicSememeStringBI)refex.getData(QueryBuilderConstants.PARAMETER_COLUMN.getConceptDescriptionText());
//							newFilter.setSearchParameter(searchParamCol != null ? searchParamCol.getDataString() : null);
//							LOG.debug("Read String search parameter from " + dud.getRefexName() + " sememe: \"" + newFilter.getSearchParameter() + "\"");
//	
//							loadEmbeddedSearchTypeFilterAttributes(refex, newFilter);
//	
////							if (compModel.getSearchType() != null) {
////								throw new SearchConceptException("Model already contains SearchTypeFilter of type " + compModel.getSearchType().getClass().getName() + ". Cannot add second SearchTypeFilter of type " + newFilter.getClass().getName());
////							} else {
//								compModel.setSearchType(newFilter);
////							}
//						} else if (dud.getRefexName().equals(QueryBuilderConstants.SEARCH_REGEXP_FILTER.getConceptDescriptionText() /*"Search RegExp Filter"*/)) {
//							// handle "Search RegExp Filter"
//	
//							LOG.debug("Loading data into model from Search RegExp Filter sememe");
//	
//							RegExpSearchTypeFilter newFilter = new RegExpSearchTypeFilter();
//	
//							DynamicSememeStringBI searchParamCol = (DynamicSememeStringBI)refex.getData(QueryBuilderConstants.PARAMETER_COLUMN.getConceptDescriptionText());
//							newFilter.setSearchParameter(searchParamCol != null ? searchParamCol.getDataString() : null);
//							LOG.debug("Read String search parameter from " + dud.getRefexName() + " sememe: \"" + newFilter.getSearchParameter() + "\"");
//	
//							loadEmbeddedSearchTypeFilterAttributes(refex, newFilter);
//	
//							compModel.setSearchType(newFilter);
//						} else if (dud.getRefexName().equals(QueryBuilderConstants.SEARCH_ISDESCENDANTOF_FILTER.getConceptDescriptionText() /*"Search IsKindOf Filter"*/)) {
//							// handle "Search RegExp Filter"
//	
//							LOG.debug("Loading data into model from Search IsKindOf Filter sememe");
//	
//							IsDescendantOfFilter newFilter = new IsDescendantOfFilter();
//	
//							DynamicSememeUUIDBI ascendantUuidCol = (DynamicSememeUUIDBI)refex.getData(QueryBuilderConstants.ANCESTOR_COLUMN.getConceptDescriptionText());
//							if (ascendantUuidCol != null) {
//								UUID uuid = ascendantUuidCol.getDataUUID();
//								int nid = OTFUtility.getConceptVersion(uuid).getNid();
//								newFilter.setNid(nid);
//								LOG.debug("Read UUID (nid=" + nid + ") from " + dud.getRefexName() + " sememe: \"" + uuid + "\"");
//							}
//	
//							loadEmbeddedSearchFilterAttributes(refex, newFilter, filterOrderMap);
//						} else if (dud.getRefexName().equals(QueryBuilderConstants.SEARCH_ISA_FILTER.getConceptDescriptionText() /*"Search IsA Filter"*/)) {
//							// handle "Search IsA Filter"
//	
//							LOG.debug("Loading data into model from Search IsA Filter sememe");
//	
//							IsAFilter newFilter = new IsAFilter();
//	
//							DynamicSememeUUIDBI matchUuidCol = (DynamicSememeUUIDBI)refex.getData(QueryBuilderConstants.MATCH_COLUMN.getConceptDescriptionText());
//							if (matchUuidCol != null) {
//								UUID uuid = matchUuidCol.getDataUUID();
//								int nid = OTFUtility.getConceptVersion(uuid).getNid();
//								newFilter.setNid(nid);
//								LOG.debug("Read UUID (nid=" + nid + ") from " + dud.getRefexName() + " sememe: \"" + uuid + "\"");
//							}
//	
//							loadEmbeddedSearchFilterAttributes(refex, newFilter, filterOrderMap);
//						} else {
//							// handle or ignore
//							LOG.warn("Concept \"" + displayConcept + "\" contains unexpected sememe \"" + dud.getRefexName() + "\".  Ignoring...");
//						}
//	
//						// At this point the search should have a SearchTypeFilter
//						if (compModel.getSearchType() == null) {
//							throw new SearchConceptException("Model does not contain a SearchTypeFilter");
//						}
//						
//						// At this point the filterOrderMap should NOT contain any SearchTypeFilter filter
//						for (int order : filterOrderMap.keySet()) {
//							for (NonSearchTypeFilter<?> f : filterOrderMap.get(order)) {
//								compModel.getFilters().add(f);
//							}
//						}
//					} else if (SearchModel.getSearchTypeSelector().getCurrentType() == SearchType.SEMEME) {
//						SememeSearchTypeModel compModel = (SememeSearchTypeModel)SearchModel.getSearchTypeSelector().getTypeSpecificModel();
//						
//						if (dud.getRefexName().equals(QueryBuilderConstants.STORED_QUERY_GLOBAL_ATTRIBUTES.getConceptDescriptionText() /*"Search Global Attributes"*/)) {
//							// handle "Search Global Attributes"
//	
//							LOG.debug("Loading data into model from Search Global Attributes sememe");
//	
//							// Loading view coordinate
//							DynamicSememeByteArrayBI serializedViewCoordinate = (DynamicSememeByteArrayBI)refex.getData(QueryBuilderConstants.VIEW_COORDINATE_COLUMN.getConceptDescriptionText());
//	
//							// Serialize passed View Coordinate into byte[]serializedViewCoordinate.getData()
//							ByteArrayInputStream input = new ByteArrayInputStream(serializedViewCoordinate.getDataByteArray());
//	
//							ObjectInputStream oos = new ObjectInputStream(input);
//							ViewCoordinate vc = new ViewCoordinate();
//							vc.readExternal(oos);
//							SearchModel.getSearchTypeSelector().getTypeSpecificModel().setViewCoordinate(vc);
//							LOG.debug("Read View Coordinate from " + dud.getRefexName() + " sememe: " + SearchModel.getSearchTypeSelector().getTypeSpecificModel().getViewCoordinate());
//	
//							// Loading maxResults
//							DynamicSememeIntegerBI maxResults = (DynamicSememeIntegerBI)refex.getData(QueryBuilderConstants.MAX_RESULTS_COLUMN.getConceptDescriptionText());
//							SearchModel.getSearchTypeSelector().getTypeSpecificModel().setMaxResults(maxResults.getDataInteger());
//							LOG.debug("Read max results from " + dud.getRefexName() + " sememe: " + SearchModel.getSearchTypeSelector().getTypeSpecificModel().getMaxResults());
//	
//							// Loading drools expression
//							DynamicSememeStringBI droolsExpr = (DynamicSememeStringBI)refex.getData(QueryBuilderConstants.DROOLS_EXPR_COLUMN.getConceptDescriptionText());
//							SearchModel.getSearchTypeSelector().getTypeSpecificModel().setDroolsExpr(droolsExpr != null ? droolsExpr.getDataString() : null);
//							LOG.debug("Read drools expression from " + dud.getRefexName() + " sememe: " + SearchModel.getSearchTypeSelector().getTypeSpecificModel().getDroolsExpr());
//	
//						} else if (dud.getRefexName().equals(QueryBuilderConstants.SEARCH_SEMEME_CONTENT_FILTER.getConceptDescriptionText() /*"Search Sememe Filter"*/)) {	
//							LOG.debug("Loading data into model from Search Sememe Filter sememe");
//	
//							SememeContentSearchTypeFilter newFilter = new SememeContentSearchTypeFilter();
//	
//							DynamicSememeStringBI searchParamCol = (DynamicSememeStringBI)refex.getData(QueryBuilderConstants.PARAMETER_COLUMN.getConceptDescriptionText());
//							newFilter.setSearchParameter(searchParamCol != null ? searchParamCol.getDataString() : null);
//							LOG.debug("Read String search parameter from " + dud.getRefexName() + " sememe: \"" + newFilter.getSearchParameter() + "\"");
//
//							DynamicSememeUUID uuidCol = (DynamicSememeUUID)refex.getData(QueryBuilderConstants.UUID_COLUMN.getConceptDescriptionText());
//							if (uuidCol != null) {
//								ConceptVersionBI conceptVersion = OTFUtility.getConceptVersion(uuidCol.getDataUUID());
//								newFilter.setAssemblageConcept(conceptVersion);
//							}
//							LOG.debug("Read assemblage concept from " + dud.getRefexName() + " sememe assemblage: \"" + newFilter.getAssemblageConcept() != null ? ComponentDescriptionHelper.getComponentDescription(newFilter.getAssemblageConcept().getNid())
//									: null + "\"");
//	
//							loadEmbeddedSearchTypeFilterAttributes(refex, newFilter);
//	
////							if (compModel.getSearchType() != null) {
////								throw new SearchConceptException("Model already contains SearchTypeFilter of type " + compModel.getSearchType().getClass().getName() + ". Cannot add second SearchTypeFilter of type " + newFilter.getClass().getName());
////							} else {
//								compModel.setSearchType(newFilter);
////							}
//						} else {
//							// handle or ignore
//							LOG.warn("Concept \"" + displayConcept + "\" contains unexpected sememe \"" + dud.getRefexName() + "\".  Ignoring...");
//						}
//	
//						// At this point the search should have a SearchTypeFilter
////						if (compModel.getSearchType() == null) {
////							throw new SearchConceptException("Model does not contain a SearchTypeFilter");
////						}
//					}
//				}
//				
//				LOG.debug("loadSavedSearch() loaded search view model for \"" + matchingConcept + "\": " + model);
//
//				return model;
//			} else {
//				LOG.error("Failed loading saved search " + displayConcept);
//				return null;
//			}
		} catch (Exception e) {
			LOG.error("Failed loading saved search. ", e);
			throw new SearchConceptException(e.getLocalizedMessage(), e);
		}
	}
}