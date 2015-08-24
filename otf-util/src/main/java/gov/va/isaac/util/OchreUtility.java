/**
 * 
 */
package gov.va.isaac.util;

import gov.va.isaac.AppContext;
import gov.va.isaac.ExtendedAppContext;
import gov.va.isaac.config.profiles.UserProfileBindings;
import gov.va.isaac.gui.SimpleDisplayConcept;
import gov.vha.isaac.metadata.coordinates.StampCoordinates;
import gov.vha.isaac.metadata.source.IsaacMetadataAuxiliaryBinding;
import gov.vha.isaac.ochre.api.ConceptProxy;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.LookupService;
import gov.vha.isaac.ochre.api.chronicle.LatestVersion;
import gov.vha.isaac.ochre.api.component.concept.ConceptChronology;
import gov.vha.isaac.ochre.api.component.concept.ConceptService;
import gov.vha.isaac.ochre.api.component.concept.ConceptSnapshot;
import gov.vha.isaac.ochre.api.component.concept.ConceptVersion;
import gov.vha.isaac.ochre.api.component.sememe.SememeChronology;
import gov.vha.isaac.ochre.api.component.sememe.SememeSnapshotService;
import gov.vha.isaac.ochre.api.component.sememe.version.DescriptionSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.SememeVersion;
import gov.vha.isaac.ochre.api.coordinate.LanguageCoordinate;
import gov.vha.isaac.ochre.api.coordinate.PremiseType;
import gov.vha.isaac.ochre.api.coordinate.StampCoordinate;
import gov.vha.isaac.ochre.api.coordinate.TaxonomyCoordinate;
import gov.vha.isaac.ochre.api.index.SearchResult;
import gov.vha.isaac.ochre.api.relationship.RelationshipVersionAdaptor;
import gov.vha.isaac.ochre.collections.ConceptSequenceSet;
import gov.vha.isaac.ochre.impl.utility.Frills;
import gov.vha.isaac.ochre.model.constants.IsaacMetadataConstants;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.ihtsdo.otf.query.lucene.indexers.SememeIndexer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author joel
 *
 */
public final class OchreUtility {
	private static final Logger LOG = LoggerFactory.getLogger(OchreUtility.class);

	private OchreUtility() {}


	/**
	 * Simple utility method to get the latest version of a concept without having to do the class conversion stuff
	 * @param conceptChronology the chronlogy to get the concept version for
	 * @param stampCoordinate - optional - if not provided, uses the current stamp coordinate from the user profile.
	 */
	@SuppressWarnings("unchecked")
	public static Optional<LatestVersion<? extends ConceptVersion<?>>> getLatestConceptVersion(ConceptChronology<? extends ConceptVersion<?>> conceptChronology, 
			StampCoordinate<? extends StampCoordinate<?>> stampCoordinate) {
		@SuppressWarnings("rawtypes")
		ConceptChronology raw = (ConceptChronology)conceptChronology;
		
		return raw.getLatestVersion(ConceptVersion.class, 
				(stampCoordinate == null ? ExtendedAppContext.getUserProfileBindings().getStampCoordinate().get() : stampCoordinate));
	}

	/**
	 * Simple utility method to return a relationship version adapater, handling the class conversion stuff
	 * @param sememeChronology the relationship sememe chronology to lookup 
	 * @param stampCoordinate - optional - if not provided, uses the current stamp coordinate from the user profile
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static Optional<LatestVersion<? extends RelationshipVersionAdaptor<?>>> getLatestRelationshipVersionAdaptor(
			SememeChronology<? extends RelationshipVersionAdaptor<?>> sememeChronology, StampCoordinate<? extends StampCoordinate<?>> stampCoordinate) {
		@SuppressWarnings("rawtypes")
		SememeChronology rawSememChronology = (SememeChronology)sememeChronology;
		
		return rawSememChronology.getLatestVersion(RelationshipVersionAdaptor.class, 
				(stampCoordinate == null ? ExtendedAppContext.getUserProfileBindings().getStampCoordinate().get() : stampCoordinate));
	}

	/**
	 * @param id concept nid or sequence
	 * @param stampCoordinate - optional - if not provided, taken from user preferences.  StampCoordinate for retrieving the LogicGraphSememeImpl
	 * @param historical - if true, return all rels, if false, only return the latest
	 * @param premiseType PremiseTypes by which to filter. Passing null disables filtering by PremiseType (returns both STATED and INFERRED)
	 * @param relTypeSequence - optional - if provided - only rels that match the specified type sequence will be returned
	 * @return List of RelationshipVersionAdaptor
	 */
	public static List<RelationshipVersionAdaptor<?>> getRelationshipListOriginatingFromConcept(int id, 
			StampCoordinate<? extends StampCoordinate<?>> stampCoordinate, boolean historical, PremiseType premiseType, Integer relTypeSequence) {
		List<RelationshipVersionAdaptor<?>> allRelationships = new ArrayList<>();
	
		List<? extends SememeChronology<? extends RelationshipVersionAdaptor<?>>> outgoingRelChronicles = Get.conceptService().getConcept(id)
				.getRelationshipListOriginatingFromConcept();
		for (SememeChronology<? extends RelationshipVersionAdaptor<?>> chronicle : outgoingRelChronicles)
		{
			if (historical) {
				for (RelationshipVersionAdaptor<?> rv : chronicle.getVersionList())
				{
					// Ensure that RelationshipVersionAdaptor corresponds to latest LogicGraph
					if ((premiseType == null || premiseType == rv.getPremiseType())
							&& (relTypeSequence == null || relTypeSequence == rv.getTypeSequence())) {
						allRelationships.add(rv);
						LOG.debug("getLatestRelationshipListOriginatingFromConcept(" + Get.conceptDescriptionText(id) + ", stampCoord, (PremiseType)" 
						+ (premiseType != null ? premiseType.name() : null) + ", relTypeSequence=" + relTypeSequence + ") adding " + rv);
					}
				}
			} else {
				Optional<LatestVersion<? extends RelationshipVersionAdaptor<?>>> latest = getLatestRelationshipVersionAdaptor(chronicle, 
						(stampCoordinate == null ? ExtendedAppContext.getUserProfileBindings().getStampCoordinate().get() : stampCoordinate));
				if (latest.isPresent() && latest.get().value() != null
						&& (premiseType == null || premiseType == latest.get().value().getPremiseType())
						&& (relTypeSequence == null || relTypeSequence == latest.get().value().getTypeSequence())) {
					allRelationships.add(latest.get().value());
					LOG.debug("getLatestRelationshipListOriginatingFromConcept(" + Get.conceptDescriptionText(id) + ", stampCoord, (PremiseType)" 
					+ (premiseType != null ? premiseType.name() : null) + ", relTypeSequence=" + relTypeSequence + ") adding " + latest.get().value());
				}
			}
		}
		return allRelationships;
	}
	
	/**
	 * Utility method to find all concepts present and active as a member of the PATHS_ASSEMBLAGE
	 * @return The path concepts
	 */
	public static Set<ConceptVersion<?>> getPathConcepts() {
		Stream<SememeChronology<? extends SememeVersion<?>>> sememes = Get.sememeService()
				.getSememesFromAssemblage(IsaacMetadataAuxiliaryBinding.PATHS_ASSEMBLAGE.getConceptSequence());

		final Set<ConceptVersion<?>> pathConcepts = new HashSet<>();
		Consumer<? super SememeChronology<? extends SememeVersion<?>>> action = new Consumer<SememeChronology<? extends SememeVersion<?>>>() {
			@Override
			public void accept(SememeChronology<? extends SememeVersion<?>> t) {
				ConceptChronology<? extends ConceptVersion<?>> pathCC = Get.conceptService().getConcept(t.getReferencedComponentNid());
				
				Optional<LatestVersion<? extends ConceptVersion<?>>> latestPathConceptVersion = getLatestConceptVersion(pathCC, StampCoordinates.getDevelopmentLatest());
				if (latestPathConceptVersion.isPresent()) {
					pathConcepts.add(latestPathConceptVersion.get().value());
				}
			}
		};
		sememes.distinct().forEach(action);

		if (pathConcepts.isEmpty()) {
			LOG.error("No paths loaded based on membership in {}", IsaacMetadataAuxiliaryBinding.PATHS_ASSEMBLAGE);
		} else {
			LOG.debug("Loaded {} paths: {}", pathConcepts.size(), pathConcepts);
		}
			
		return Collections.unmodifiableSet(pathConcepts);
	}
	
	/**
	 * Utility method to get the best text value description for a concept, according to the user preferences.  
	 * Calls {@link #getDescription(UUID, LanguageCoordinate, StampCoordinate)} with nulls. 
	 * @param conceptUUID - identifier for a concept
	 * @return
	 */
	public static Optional<String> getDescription(UUID conceptUUID) {
		return getDescription(conceptUUID, null, null);
	}

	/**
	 * Utility method to get the best text value description for a concept, according to the user preferences.  
	 * Calls {@link #getDescription(int, LanguageCoordinate, StampCoordinate)}. 
	 * @param conceptId - either a sequence or a nid
	 * @return
	 */
	public static Optional<String> getDescription(int conceptId) {
		return getDescription(conceptId, null, null);
	}
	
	/**
	 * Utility method to get the best text value description for a concept, according to the passed in options, 
	 * or the user preferences.  Calls {@link #getDescription(UUID, LanguageCoordinate, StampCoordinate)} with values 
	 * extracted from the taxonomyCoordinate, or null. 
	 * @param conceptUUID - identifier for a concept
	 * @param tc - optional - if not provided, defaults to user preferences values
	 * @return
	 */
	public static Optional<String> getDescription(UUID conceptUUID, TaxonomyCoordinate<?> taxonomyCoordinate) {
		return getDescription(conceptUUID, taxonomyCoordinate == null ? null : taxonomyCoordinate.getLanguageCoordinate(), 
				taxonomyCoordinate == null ? null : taxonomyCoordinate.getStampCoordinate());
	}

	/**
	 * Utility method to get the best text value description for a concept, according to the passed in options, 
	 * or the user preferences.  Calls {@link #getDescription(int, LanguageCoordinate, StampCoordinate)} with values 
	 * extracted from the taxonomyCoordinate, or null. 
	 * @param conceptId - either a sequence or a nid
	 * @param tc - optional - if not provided, defaults to user preferences values
	 * @return
	 */
	public static Optional<String> getDescription(int conceptId, TaxonomyCoordinate<?> taxonomyCoordinate) {
		return getDescription(conceptId, taxonomyCoordinate == null ? null : taxonomyCoordinate.getLanguageCoordinate(), 
				taxonomyCoordinate == null ? null : taxonomyCoordinate.getStampCoordinate());
	}
	
	/**
	 * Utility method to get the best text value description for a concept, according to the passed in options, 
	 * or the user preferences.  Calls {@link #getDescription(int, LanguageCoordinate, StampCoordinate)} with values 
	 * extracted from the taxonomyCoordinate, or null. 
	 * @param conceptId - either a sequence or a nid
	 * @param languageCoordinate - optional - if not provided, defaults to user preferences values
	 * @param stampCoordinate - optional - if not provided, defaults to user preference values
	 * @return
	 */
	public static Optional<String> getDescription(UUID conceptUUID, LanguageCoordinate languageCoordinate, StampCoordinate<? extends StampCoordinate<?>> stampCoordinate) 
	{
		return getDescription(Get.identifierService().getConceptSequenceForUuids(conceptUUID), languageCoordinate, stampCoordinate);
	}

	/**
	 * Utility method to get the best text value description for a concept, according to the passed in options, 
	 * or the user preferences. 
	 * @param conceptId - either a sequence or a nid
	 * @param languageCoordinate - optional - if not provided, defaults to user preferences values
	 * @param stampCoordinate - optional - if not provided, defaults to user preference values
	 * @return
	 */
	public static Optional<String> getDescription(int conceptId, LanguageCoordinate languageCoordinate, StampCoordinate<? extends StampCoordinate<?>> stampCoordinate) 
	{
		@SuppressWarnings({ "unchecked", "rawtypes" })
		Optional<LatestVersion<DescriptionSememe>> desc = Get.conceptService()
			.getSnapshot(stampCoordinate == null ? ExtendedAppContext.getUserProfileBindings().getStampCoordinate().get() : stampCoordinate,
						languageCoordinate == null ? ExtendedAppContext.getUserProfileBindings().getLanguageCoordinate().get() : languageCoordinate)
					.getDescriptionOptional(conceptId);
		
		return desc.isPresent() ? Optional.of(desc.get().value().getText()) : Optional.empty();
	}

	/**
	 * Get isA children of a concept.
	 * @param conceptSequence The concept to look at
	 * @param recursive recurse down from the concept
	 * @param leafOnly only return leaf nodes
	 * @return the set of concept sequence ids that represent the children
	 */
	public static Set<Integer> getAllChildrenOfConcept(int conceptSequence, boolean recursive, boolean leafOnly)
	{
		return getAllChildrenOfConcept(new HashSet<Integer>(), conceptSequence, recursive, leafOnly);
	}
	
	/**
	 * Recursively get Is a children of a concept
	 */
	private static Set<Integer> getAllChildrenOfConcept(Set<Integer> handledConceptSequenceIds, int conceptSequence, boolean recursive, boolean leafOnly)
	{
		Set<Integer> results = new HashSet<>();
		
		// This both prevents infinite recursion and avoids processing or returning of duplicates
		if (handledConceptSequenceIds.contains(conceptSequence)) {
			return results;
		}

		AtomicInteger count = new AtomicInteger();
		//TODO should be getTaxonomyChildSequences(conceptSequence); but that is broken
		//TODO take in a taxonomy coord, if we leave it like this - Keith convo on slack on 7/31
		ConceptSequenceSet children = Get.taxonomyService().getChildOfSequenceSet(conceptSequence, 
				ExtendedAppContext.getUserProfileBindings().getTaxonomyCoordinate().get());

		children.stream().forEach((conSequence) ->
		{
			count.getAndIncrement();
			if (!leafOnly)
			{
				results.add(conSequence);
			}
			if (recursive)
			{
				results.addAll(getAllChildrenOfConcept(handledConceptSequenceIds, conSequence, recursive, leafOnly));
			}
		});
		
		
		if (leafOnly && count.get() == 0)
		{
			results.add(conceptSequence);
		}
		handledConceptSequenceIds.add(conceptSequence);
		return results;
	}
	
	
	/**
	 * @param conceptNidOrSequence
	 * @param stampCoord - optional - what stamp to use when returning the ConceptSnapshot (defaults to user prefs)
	 * @param langCoord - optional - what lang coord to use when returning the ConceptSnapshot (defaults to user prefs)
	 * @return the ConceptSnapshot, or an optional that indicates empty, if the identifier was invalid, or if the concept didn't 
	 *   have a version available on the specified stampCoord
	 */
	public static Optional<ConceptSnapshot> getConceptSnapshot(int conceptNidOrSequence, 
			StampCoordinate<? extends StampCoordinate<?>> stampCoord, LanguageCoordinate langCoord)
	{
		Optional<? extends ConceptChronology<? extends ConceptVersion<?>>> c = Get.conceptService().getOptionalConcept(conceptNidOrSequence);
		if (c.isPresent())
		{
			if (c.get().isLatestVersionActive(stampCoord == null ? AppContext.getService(UserProfileBindings.class).getStampCoordinate().get() : stampCoord))
			{
				return Optional.of(Get.conceptService().getSnapshot(stampCoord == null ? AppContext.getService(UserProfileBindings.class).getStampCoordinate().get() : stampCoord,
						langCoord == null ? AppContext.getService(UserProfileBindings.class).getLanguageCoordinate().get() : langCoord)
							.getConceptSnapshot(c.get().getConceptSequence()));
			}
		}
		return Optional.empty();
	}
	
	/**
	 * @param conceptUUID
	 * @param stampCoord - optional - what stamp to use when returning the ConceptSnapshot (defaults to user prefs)
	 * @param langCoord - optional - what lang coord to use when returning the ConceptSnapshot (defaults to user prefs)
	 * @return the ConceptSnapshot, or an optional that indicates empty, if the identifier was invalid, or if the concept didn't 
	 *   have a version available on the specified stampCoord
	 */
	public static Optional<ConceptSnapshot> getConceptSnapshot(UUID conceptUUID, StampCoordinate<? extends StampCoordinate<?>> stampCoord, LanguageCoordinate langCoord)
	{
		return getConceptSnapshot(Get.identifierService().getNidForUuids(conceptUUID), stampCoord, langCoord);
	}
	
	/**
	 * If the passed in value is a {@link UUID}, calls {@link ConceptService#getOptionalConcept(int)} after converting the UUID to nid.
	 * Next, if no hit, if the passed in value is parseable as a int < 0 (a nid), calls {@link ConceptService#getOptionalConcept(int)}
	 * Next, if no hit, if the passed in value is parseable as a long, and is a valid SCTID (checksum is valid) - treats it as 
	 * a SCTID and attempts to look up the SCTID in the lucene index.  Note that is is possible for some 
	 * sequence identifiers to look like SCTIDs - if a passed in value is valid as both a SCTID and a sequence identifier - it will be 
	 * treated as an SCTID.
	 * Finally, if it is a positive integer, it treats is as a sequence identity, converts it to a nid, then looks up the nid.
	 */
	public static Optional<? extends ConceptChronology<? extends ConceptVersion<?>>> getConceptForUnknownIdentifier(String identifier)
	{
		LOG.debug("WB DB String Lookup '{}'", identifier);

		if (StringUtils.isBlank(identifier))
		{
			return Optional.empty();
		}
		String localIdentifier = identifier.trim();

		UUID uuid = Utility.getUUID(localIdentifier);
		if (uuid != null)
		{
			return Get.conceptService().getOptionalConcept(uuid);
		}
		
		//if it is a negative integer, assume nid
		Optional<Integer> nid = Utility.getNID(localIdentifier);
		if (nid.isPresent()) {
			return Get.conceptService().getOptionalConcept(nid.get());
		}
		
		if (SctId.isValidSctId(localIdentifier))
		{
			
			SememeIndexer si = LookupService.get().getService(SememeIndexer.class);
			if (si != null)
			{
				List<SearchResult> result = si.query(Long.parseLong(localIdentifier), 
						IsaacMetadataAuxiliaryBinding.SNOMED_INTEGER_ID.getConceptSequence(), 5, Long.MIN_VALUE);
				if (result.size() > 0)
				{
					return Get.conceptService().getOptionalConcept(Get.sememeService().getSememe(result.get(0).getNid()).getReferencedComponentNid());
				}
			}
			else
			{
				LOG.warn("Sememe Index not available - can't lookup SCTID");
			}
		}
		else if (Utility.isInt(localIdentifier))
		{
			//Must be a postive integer, which wasn't a valid SCTID - it may be a sequence ID.
			int nidFromSequence = Get.identifierService().getConceptNid(Integer.parseInt(localIdentifier));
			if (nidFromSequence != 0)
			{
				return Get.conceptService().getOptionalConcept(nidFromSequence);
			}
		}
		return Optional.empty();
	}
	
	/**
	 * @param nid concept nid (must be a nid)
	 * @param stamp - optional
	 * @return the text of the description, if found
	 */
	@SuppressWarnings("rawtypes")
	public static Optional<String> getFSNForConceptNid(int nid, StampCoordinate<? extends StampCoordinate<?>> stamp)
	{
		SememeSnapshotService<DescriptionSememe> ss = Get.sememeService().getSnapshot(DescriptionSememe.class, 
				stamp == null ? ExtendedAppContext.getUserProfileBindings().getStampCoordinate().get() : stamp); 
		
		Stream<LatestVersion<DescriptionSememe>> descriptions = ss.getLatestDescriptionVersionsForComponent(nid);
		Optional<LatestVersion<DescriptionSememe>> desc = descriptions.filter((LatestVersion<DescriptionSememe> d) -> 
		{
			if (d.value().getDescriptionTypeConceptSequence() == IsaacMetadataAuxiliaryBinding.FULLY_SPECIFIED_NAME.getConceptSequence())
			{
				//shouldn't need to check for preferred, I don't believe you can have multiple FSN's.
				return true;
			}
			return false;
		}).findFirst();
		
		if (desc.isPresent())
		{
			return Optional.of(desc.get().value().getText());
		}
		else return Optional.empty();
	}
	
	/**
	 * @param nid concept nid (must be a nid)
	 * @param stamp - optional - defaults to user prefs if not provided
	 * @param language - optional - defaults to {@link IsaacMetadataAuxiliaryBinding#ENGLISH} if not provided.
	 * If provided, it should be a child of {@link IsaacMetadataAuxiliaryBinding#LANGUAGE}
	 * @return the text of the description, if found
	 */
	@SuppressWarnings("rawtypes")
	public static Optional<String> getPreferredTermForConceptNid(int nid, StampCoordinate<? extends StampCoordinate<?>> stamp, ConceptProxy language)
	{
		SememeSnapshotService<DescriptionSememe> ss = Get.sememeService().getSnapshot(DescriptionSememe.class, 
				stamp == null ? ExtendedAppContext.getUserProfileBindings().getStampCoordinate().get() : stamp); 
		
		int langMatch = language == null ? IsaacMetadataAuxiliaryBinding.ENGLISH.getConceptSequence() : language.getConceptSequence();
		
		Stream<LatestVersion<DescriptionSememe>> descriptions = ss.getLatestDescriptionVersionsForComponent(nid);
		Optional<LatestVersion<DescriptionSememe>> desc = descriptions.filter((LatestVersion<DescriptionSememe> d) -> 
		{
			if (d.value().getDescriptionTypeConceptSequence() == IsaacMetadataAuxiliaryBinding.SYNONYM.getConceptSequence()
					&& d.value().getLanguageConceptSequence() == langMatch)
			{
				if (Frills.isDescriptionPreferred(d.value().getNid(), stamp == null ? ExtendedAppContext.getUserProfileBindings().getStampCoordinate().get() : stamp))
				{
					return true;
				}
			}
			return false;
		}).findFirst();
		
		if (desc.isPresent())
		{
			return Optional.of(desc.get().value().getText());
		}
		else return Optional.empty();
	}
	
	/**
	 * Calls {@link #getConceptForUnknownIdentifier(String)} in a background thread.  returns immediately. 
	 * 
	 * 
	 * @param identifier - what to search for
	 * @param callback - who to inform when lookup completes
	 * @param callId - An arbitrary identifier that will be returned to the caller when this completes
	 * @param stampCoord - optional - what stamp to use when returning the ConceptSnapshot (defaults to user prefs)
	 * @param langCoord - optional - what lang coord to use when returning the ConceptSnapshot (defaults to user prefs)
	 */
	public static void lookupConceptForUnknownIdentifier(
			final String identifier,
			final ConceptLookupCallback callback,
			final Integer callId,
			final StampCoordinate<? extends StampCoordinate<?>> stampCoord,
			final LanguageCoordinate langCoord)
	{
		LOG.debug("Threaded Lookup: '{}'", identifier);
		final long submitTime = System.currentTimeMillis();
		Runnable r = new Runnable()
		{
			@Override
			public void run()
			{
				ConceptSnapshot result = null;
				Optional<? extends ConceptChronology<? extends ConceptVersion<?>>> c = getConceptForUnknownIdentifier(identifier);
				if (c.isPresent())
				{
					Optional<ConceptSnapshot> temp = getConceptSnapshot(c.get().getConceptSequence(), stampCoord, langCoord);
					if (temp.isPresent())
					{
						result = temp.get();
					}
				}
				callback.lookupComplete(result, submitTime, callId);
			}
		};
		Utility.execute(r);
	}
	
	/**
	 * 
	 * All done in a background thread, method returns immediately
	 * 
	 * @param identifier - The NID to search for
	 * @param callback - who to inform when lookup completes
	 * @param callId - An arbitrary identifier that will be returned to the caller when this completes
	 * @param stampCoord - optional - what stamp to use when returning the ConceptSnapshot (defaults to user prefs)
	 * @param langCoord - optional - what lang coord to use when returning the ConceptSnapshot (defaults to user prefs)
	 */
	public static void lookupConceptSnapshot(
			final int nid,
			final ConceptLookupCallback callback,
			final Integer callId,
			final StampCoordinate<? extends StampCoordinate<?>> stampCoord,
			final LanguageCoordinate langCoord)
	{
		LOG.debug("Threaded Lookup: '{}'", nid);
		final long submitTime = System.currentTimeMillis();
		Runnable r = new Runnable()
		{
			@Override
			public void run()
			{
				Optional<ConceptSnapshot> c = getConceptSnapshot(nid, stampCoord, langCoord);
				callback.lookupComplete(c.isPresent() ? c.get() : null, submitTime, callId);
			}
		};
		Utility.execute(r);
	}
	
	/**
	 * Return a sorted list of SimpleDisplayConcept objects that represent all dynamic sememe assemblages in the system (active or inactive)
	 */
	public static List<SimpleDisplayConcept> getAllDynamicSememeAssemblageConcepts()
	{
		List<SimpleDisplayConcept> allDynamicSememeDefConcepts = new ArrayList<>();

		Get.sememeService().getSememesFromAssemblage(IsaacMetadataConstants.DYNAMIC_SEMEME_DEFINITION_DESCRIPTION.getSequence()).forEach(sememeC ->
		{
			//This will be a nid of a description - need to get the referenced component of that description
			int annotatedDescriptionNid = sememeC.getReferencedComponentNid();
			allDynamicSememeDefConcepts.add(new SimpleDisplayConcept(Get.sememeService().getSememe(annotatedDescriptionNid).getReferencedComponentNid()));
		});

		Collections.sort(allDynamicSememeDefConcepts);
		return allDynamicSememeDefConcepts;
	}
}
