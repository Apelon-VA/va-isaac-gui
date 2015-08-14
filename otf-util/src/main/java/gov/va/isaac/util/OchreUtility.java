/**
 * 
 */
package gov.va.isaac.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import gov.va.isaac.AppContext;
import gov.va.isaac.ExtendedAppContext;
import gov.va.isaac.config.profiles.UserProfile;
import gov.va.isaac.config.profiles.UserProfileBindings;
import gov.va.isaac.config.profiles.UserProfileManager;
import gov.va.isaac.config.users.InvalidUserException;
import gov.vha.isaac.metadata.coordinates.StampCoordinates;
import gov.vha.isaac.metadata.source.IsaacMetadataAuxiliaryBinding;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.IdentifierService;
import gov.vha.isaac.ochre.api.LookupService;
import gov.vha.isaac.ochre.api.chronicle.LatestVersion;
import gov.vha.isaac.ochre.api.component.concept.ConceptChronology;
import gov.vha.isaac.ochre.api.component.concept.ConceptService;
import gov.vha.isaac.ochre.api.component.concept.ConceptSnapshot;
import gov.vha.isaac.ochre.api.component.concept.ConceptSnapshotService;
import gov.vha.isaac.ochre.api.component.concept.ConceptVersion;
import gov.vha.isaac.ochre.api.component.sememe.SememeChronology;
import gov.vha.isaac.ochre.api.component.sememe.SememeSnapshotService;
import gov.vha.isaac.ochre.api.component.sememe.version.DescriptionSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.SememeVersion;
import gov.vha.isaac.ochre.api.coordinate.LanguageCoordinate;
import gov.vha.isaac.ochre.api.coordinate.PremiseType;
import gov.vha.isaac.ochre.api.coordinate.StampCoordinate;
import gov.vha.isaac.ochre.api.coordinate.TaxonomyCoordinate;
import gov.vha.isaac.ochre.api.relationship.RelationshipVersionAdaptor;
import gov.vha.isaac.ochre.api.tree.Tree;
import gov.vha.isaac.ochre.collections.ConceptSequenceSet;
import gov.vha.isaac.ochre.util.UuidFactory;

/**
 * @author joel
 *
 */
public final class OchreUtility {
	private static final Logger LOG = LoggerFactory.getLogger(OchreUtility.class);

	private OchreUtility() {}

	
	@SuppressWarnings("unchecked")
	public static List<? extends SememeChronology<? extends DescriptionSememe<?>>> getConceptDescriptionList(ConceptChronology<?> cc) {
		@SuppressWarnings("rawtypes")
		List<? extends SememeChronology<? extends DescriptionSememe>> rawList = cc.getConceptDescriptionList();
		
		return (List<? extends SememeChronology<? extends DescriptionSememe<?>>>)rawList;
	}

    @SuppressWarnings("unchecked")
	public static Optional<LatestVersion<? extends ConceptVersion<?>>> getLatestConceptVersion(ConceptChronology<? extends ConceptVersion<?>> cc, StampCoordinate<? extends StampCoordinate<?>> sc) {
		@SuppressWarnings("rawtypes")
		ConceptChronology raw = (ConceptChronology)cc;
    	
    	return raw.getLatestVersion(ConceptVersion.class, sc);
    }
    
	@SuppressWarnings("unchecked")
	public static Optional<LatestVersion<? extends RelationshipVersionAdaptor<?>>> getLatestRelationshipVersionAdaptor(SememeChronology<? extends RelationshipVersionAdaptor<?>> sc, StampCoordinate<? extends StampCoordinate<?>> stampCoordinate) {
		@SuppressWarnings("rawtypes")
		SememeChronology rawSememChronology = (SememeChronology)sc;
		
		return rawSememChronology.getLatestVersion(RelationshipVersionAdaptor.class, stampCoordinate);
	}

	/**
	 * @param nid concept nid
	 * @param stampCoordinate StampCoordinate for retrieving latest LogicGraphSememeImpl
	 * @param premiseType PremiseTypes by which to filter. Passing null disables filtering by PremiseType (returns both STATED and INFERRED)
	 * @return List of RelationshipVersionAdaptor
	 * 
	 * 
	 */
	public static List<RelationshipVersionAdaptor<?>> getRelationshipListOriginatingFromConcept(int nid, StampCoordinate<? extends StampCoordinate<?>> stampCoordinate, boolean historical, PremiseType premiseType, Integer relTypeSequence) {
		List<RelationshipVersionAdaptor<?>> allRelationships = new ArrayList<>();
		
		// Code for examining LogicGraphs only
//		LogicGraphSememeImpl latestStatedGraph = null;
//		LogicGraphSememeImpl latestInferredGraph = null;
//
//		if (premiseType == null || premiseType == PremiseType.STATED)
//		{
//			Optional<SememeChronology<? extends SememeVersion<?>>> defChronologyOptional = Get.statedDefinitionChronology(nid);
//
//			SememeChronology rawDefChronology = defChronologyOptional.get();
//			Optional<LatestVersion<LogicGraphSememeImpl>> latestGraphLatestVersionOptional = rawDefChronology.getLatestVersion(LogicGraphSememeImpl.class, stampCoordinate);
//			latestStatedGraph = latestGraphLatestVersionOptional.get().value();
//		}	
//		
//		if (premiseType == null || premiseType == PremiseType.INFERRED)
//		{
//			Optional<SememeChronology<? extends SememeVersion<?>>> defChronologyOptional = Get.inferredDefinitionChronology(nid);
//
//			SememeChronology rawDefChronology = defChronologyOptional.get();
//			Optional<LatestVersion<LogicGraphSememeImpl>> latestGraphLatestVersionOptional = rawDefChronology.getLatestVersion(LogicGraphSememeImpl.class, stampCoordinate);
//			latestInferredGraph = latestGraphLatestVersionOptional.get().value();
//		}
		
		List<? extends SememeChronology<? extends RelationshipVersionAdaptor<?>>> outgoingRelChronicles = Get.conceptService().getConcept(nid).getRelationshipListOriginatingFromConcept();
		for (SememeChronology<? extends RelationshipVersionAdaptor<?>> chronicle : outgoingRelChronicles)
		{
			if (historical) {
				for (RelationshipVersionAdaptor<?> rv : chronicle.getVersionList())
				{
					// Ensure that RelationshipVersionAdaptor corresponds to latest LogicGraph
					if (premiseType == null || premiseType == rv.getPremiseType()) {
						allRelationships.add(rv);
						LOG.debug("getLatestRelationshipListOriginatingFromConcept(" + Get.conceptDescriptionText(nid) + ", stampCoord, (PremiseType)" + (premiseType != null ? premiseType.name() : null) + ") adding " + OchreUtility.toString(rv));
					}
				}
			} else {
				Optional<LatestVersion<? extends RelationshipVersionAdaptor<?>>> latest = getLatestRelationshipVersionAdaptor(chronicle, stampCoordinate);
				if (latest.isPresent() && latest.get().value() != null
						&& (premiseType == null || premiseType == latest.get().value().getPremiseType())
						&& (relTypeSequence == null || relTypeSequence == latest.get().value().getTypeSequence())) {
					allRelationships.add(latest.get().value());
				}
			}
		}
		return allRelationships;
	}
	
	public static Set<ConceptVersion<?>> getPathConcepts() {
		Stream<SememeChronology<? extends SememeVersion<?>>> sememes = Get.sememeService().getSememesFromAssemblage(IsaacMetadataAuxiliaryBinding.PATHS_ASSEMBLAGE.getConceptSequence());
		//LOG.debug("Loaded {} sememes from assemblage {}", sememes.count(), Get.conceptDescriptionText(IsaacMetadataAuxiliaryBinding.PATHS_ASSEMBLAGE.getNid()));

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

	public static ConceptSnapshotService conceptSnapshotService(TaxonomyCoordinate<?> vc) {
		return conceptSnapshotService(vc.getStampCoordinate(), vc.getLanguageCoordinate());
	}
	public static ConceptSnapshotService conceptSnapshotService(StampCoordinate<? extends StampCoordinate<?>> stampCoordinate, LanguageCoordinate languageCoordinate) {
		return LookupService.getService(ConceptService.class).getSnapshot(stampCoordinate, languageCoordinate);
	}

	public static String conceptDescriptionText(int conceptId, ConceptSnapshotService snapshot) {
		@SuppressWarnings({ "rawtypes", "unchecked" })
		Optional<LatestVersion<DescriptionSememe>> descriptionOptional = snapshot.getDescriptionOptional(conceptId);
		if (descriptionOptional.isPresent()) {
			return descriptionOptional.get().value().getText();
		}

		return null;
	}
	public static <T extends DescriptionSememe<T>> Optional<LatestVersion<T>> getDescriptionOptional(ConceptChronology<?> conceptChronology, LanguageCoordinate languageCoordinate, StampCoordinate<? extends StampCoordinate<?>> stampCoordinate) {
		try {
			UserProfile userProfile = ExtendedAppContext.getCurrentlyLoggedInUserProfile();
			if (userProfile == null)
			{
				LOG.warn("User profile not available yet during call to getDescription() - configuring automation mode!");
				try
				{
					LookupService.getService(UserProfileManager.class).configureAutomationMode(null);
					userProfile = ExtendedAppContext.getCurrentlyLoggedInUserProfile();
				}
				catch (InvalidUserException e)
				{
					throw new RuntimeException("Problem configuring automation mode!");
				}
			}

			Optional<LatestVersion<T>> optional = null;
			if (userProfile.getDisplayFSN()) {
				optional = OchreUtility.conceptSnapshotService(stampCoordinate, languageCoordinate).getFullySpecifiedDescription(conceptChronology.getNid());
			} else {
				optional = OchreUtility.conceptSnapshotService(stampCoordinate, languageCoordinate).getPreferredDescription(conceptChronology.getNid());
			}

			return optional;
		} catch (RuntimeException e) {
			LOG.error("Failed determining correct description type. Caught " + e.getClass().getName() + " " + e.getLocalizedMessage(), e);
			
			throw e;
		}
	}
	public static <T extends DescriptionSememe<T>> Optional<LatestVersion<T>> getDescriptionOptional(int conceptId) {
		return getDescriptionOptional(Get.conceptSnapshot().getConceptSnapshot(conceptId).getChronology());
	}
	public static <T extends DescriptionSememe<T>> Optional<LatestVersion<T>> getDescriptionOptional(ConceptChronology<?> conceptChronology) {
		try {
			UserProfile userProfile = ExtendedAppContext.getCurrentlyLoggedInUserProfile();
			if (userProfile == null)
			{
				LOG.warn("User profile not available yet during call to getDescription() - configuring automation mode!");
				try
				{
					LookupService.getService(UserProfileManager.class).configureAutomationMode(null);
					userProfile = ExtendedAppContext.getCurrentlyLoggedInUserProfile();
				}
				catch (InvalidUserException e)
				{
					throw new RuntimeException("Problem configuring automation mode!");
				}
			}

			Optional<LatestVersion<T>> optional = null;
			if (userProfile.getDisplayFSN()) {
				optional = Get.conceptSnapshot().getFullySpecifiedDescription(conceptChronology.getNid());
			} else {
				optional = Get.conceptSnapshot().getPreferredDescription(conceptChronology.getNid());
			}

			return optional;
		} catch (RuntimeException e) {
			LOG.error("Failed determining correct description type. Caught " + e.getClass().getName() + " " + e.getLocalizedMessage(), e);
			
			throw e;
		}
	}

	public static String getDescription(UUID conceptUuid, TaxonomyCoordinate<?> vc) {
		return getDescription(conceptSnapshotService(vc).getConceptSnapshot(Get.identifierService().getNidForUuids(conceptUuid)).getChronology(), vc);
	}
	public static String getDescription(ConceptChronology<?> conceptChronology, TaxonomyCoordinate<?> vc) {
		return getDescription(conceptChronology, vc.getLanguageCoordinate(), vc.getStampCoordinate());
	}
	public static String getDescription(UUID conceptUuid, LanguageCoordinate languageCoordinate, StampCoordinate<? extends StampCoordinate<?>> stampCoordinate) {
		return getDescription(conceptSnapshotService(stampCoordinate, languageCoordinate).getConceptSnapshot(Get.identifierService().getNidForUuids(conceptUuid)).getChronology(), languageCoordinate, stampCoordinate);
	}
	public static <T extends DescriptionSememe<T>> String getDescription(ConceptChronology<?> conceptChronology, LanguageCoordinate languageCoordinate, StampCoordinate<? extends StampCoordinate<?>> stampCoordinate) {
		Optional<LatestVersion<T>> optional = getDescriptionOptional(conceptChronology, languageCoordinate, stampCoordinate);

		if (optional.isPresent() && optional.get().value() != null && optional.get().value().getText() != null) {
			return optional.get().value().getText();
		} else {
			String desc = conceptDescriptionText(conceptChronology.getNid(), conceptSnapshotService(stampCoordinate, languageCoordinate));
			if (desc != null) {
				return desc;
			}
			
			desc = Get.conceptDescriptionText(conceptChronology.getNid());
			if (desc != null) {
				return desc;
			}

			LOG.warn("Trying OTFUtility.getDescription({})", conceptChronology.getNid());
			desc = OTFUtility.getDescription(conceptChronology.getNid());
			if (desc != null) {
				return desc;
			}

			return null;
		}
	}
	
	public static Set<Integer> getChildrenAsConceptSequences(ConceptChronology<? extends ConceptVersion<?>> parent, Tree taxonomyTree) {
		Set<Integer> sequenceSet = new HashSet<>();
		
		for (int childSeq : taxonomyTree.getChildrenSequences(AppContext.getService(IdentifierService.class).getConceptSequence(parent.getNid()))) {
			sequenceSet.add(childSeq);
		}
		
		return sequenceSet;
	}
	public static Set<Integer> getChildrenAsConceptNids(ConceptChronology<? extends ConceptVersion<?>> parent, Tree taxonomyTree) {
		Set<Integer> nidSet = new HashSet<>();
		
		for (int childSeq : taxonomyTree.getChildrenSequences(AppContext.getService(IdentifierService.class).getConceptSequence(parent.getNid()))) {
			Integer nid = AppContext.getService(IdentifierService.class).getConceptNid(childSeq);
			if (nid != null) {
				nidSet.add(nid);
			}
		}
		
		return nidSet;
	}
	
	public static Set<ConceptSnapshot> getChildrenAsConceptSnapshots(ConceptChronology<? extends ConceptVersion<?>> parent, Tree taxonomyTree, StampCoordinate<? extends StampCoordinate<?>> sc, LanguageCoordinate lc) {
		Set<ConceptSnapshot> conceptVersions = new HashSet<>();
	
		for (Integer nid : getChildrenAsConceptNids(parent, taxonomyTree)) {
			conceptVersions.add(conceptSnapshotService(sc, lc).getConceptSnapshot(nid));
		}
		
		return conceptVersions;
	}
	public static Set<ConceptChronology<? extends ConceptVersion<?>>> getChildrenAsConceptChronologies(ConceptChronology<? extends ConceptVersion<?>> parent, Tree taxonomyTree, StampCoordinate<? extends StampCoordinate<?>> sc, LanguageCoordinate lc) {
		Set<ConceptChronology<? extends ConceptVersion<?>>> conceptVersions = new HashSet<>();
	
		for (Integer nid : getChildrenAsConceptNids(parent, taxonomyTree)) {
			conceptVersions.add(conceptSnapshotService(sc, lc).getConceptSnapshot(nid).getChronology());
		}
		
		return conceptVersions;
	}
	public static Set<ConceptChronology<? extends ConceptVersion<?>>> getChildrenAsConceptChronologies(ConceptChronology<? extends ConceptVersion<?>> parent, Tree taxonomyTree) {
		Set<ConceptChronology<? extends ConceptVersion<?>>> conceptVersions = new HashSet<>();
		
		for (Integer nid : getChildrenAsConceptNids(parent, taxonomyTree)) {
			conceptVersions.add(Get.conceptService().getConcept(nid));
		}
		
		return conceptVersions;
	}
	
	public static Set<Integer> getParentsAsConceptNids(ConceptChronology<? extends ConceptVersion<?>> child, Tree taxonomyTree) {
		//LOG.debug("Getting parents of concept {}...", Get.conceptDescriptionText(child.getNid()));
		int[] parentSequences = taxonomyTree.getParentSequences(child.getConceptSequence());
		
		Set<Integer> parentNids = new HashSet<>();
		
		for (int parentSequence : parentSequences) {
			int parentNid = Get.identifierService().getConceptNid(parentSequence);
			parentNids.add(parentNid);
		}
		
		return parentNids;
	}

	public static Set<ConceptChronology<? extends ConceptVersion<?>>> getParentsAsConceptChronologies(ConceptChronology<? extends ConceptVersion<?>> child, Tree taxonomyTree) {
		Set<ConceptChronology<? extends ConceptVersion<?>>> parentConcepts = new HashSet<>();
		for (int nid : taxonomyTree.getParentSequences(child.getConceptSequence())) {
			parentConcepts.add(Get.conceptService().getConcept(nid));
		}
		
		return parentConcepts;
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
		ConceptSequenceSet children = Get.taxonomyService().getChildOfSequenceSet(conceptSequence, AppContext.getService(UserProfileBindings.class).getTaxonomyCoordinate().get());

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
	 * @return the ConceptChronology, or an optional that indicates empty, if the id was invalid
	 */
	public static Optional<? extends ConceptChronology<? extends ConceptVersion<?>>> getConceptChronology(int conceptNidOrSequence)
	{
		return Get.conceptService().getOptionalConcept(conceptNidOrSequence);
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
		Optional<? extends ConceptChronology<? extends ConceptVersion<?>>> c = getConceptChronology(conceptNidOrSequence);
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
	 * If the passed in value is a {@link UUID}, calls {@link #getConceptVersion(UUID)}
	 * Next, if no hit, if the passed in value is parseable as a int < 0 (a nid), calls {@link #getConceptVersion(int)}
	 * Next, if no hit, if the passed in value is parseable as a long, and is a valid SCTID (checksum is valid) - treats it as 
	 * a SCTID and converts that to UUID and then calls {@link #getConceptVersion(UUID)}.  Note that is is possible for some 
	 * sequence identifiers to look like SCTIDs - if a passed in value is valid as both a SCTID and a sequence identifier - then a 
	 * runtime exception is thrown.
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
			//Note that some sequence IDs may still look like valid SCTIDs... which would mis-match... 
			UUID alternateUUID = UuidFactory.getUuidFromAlternateId(IsaacMetadataAuxiliaryBinding.SNOMED_INTEGER_ID.getPrimodialUuid(), localIdentifier);
			LOG.debug("WB DB String Lookup as SCTID converted to UUID {}", alternateUUID);
			Optional<? extends ConceptChronology<? extends ConceptVersion<?>>> cv = Get.conceptService().getOptionalConcept(alternateUUID);
			if (cv.isPresent())
			{
				//sanity check:
				if (Utility.isInt(localIdentifier))
				{
					int nidFromSequence = Get.identifierService().getConceptNid(Integer.parseInt(localIdentifier));
					if (nidFromSequence != 0)
					{
						throw new RuntimeException("Cannot distinguish " + localIdentifier + ".  Appears to be valid as a SCTID and a sequence identifier.");
					}
				}
				return cv;
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
				stamp == null ? AppContext.getService(UserProfileBindings.class).getStampCoordinate().get() : stamp); 
		
		Stream<LatestVersion<DescriptionSememe>> descriptions = ss.getLatestDescriptionVersionsForComponent(nid);
		Optional<LatestVersion<DescriptionSememe>> desc = descriptions.filter((LatestVersion<DescriptionSememe> d) -> 
		{
			if (d.value().getDescriptionTypeConceptSequence() == IsaacMetadataAuxiliaryBinding.FULLY_SPECIFIED_NAME.getConceptSequence())
			{
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
	 * If the passed in value is a {@link UUID}, calls {@link #getConceptVersion(UUID)}
	 * Next, if no hit, if the passed in value is parseable as a long, treats it as an SCTID and converts that to UUID and 
	 * then calls {@link #getConceptVersion(UUID)}
	 * Next, if no hit, if the passed in value is parseable as a int, calls {@link #getConceptVersion(int)}
	 * 
	 * All done in a background thread, method returns immediately
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
	
	public static String toString(Object obj) {
		try {
			if (obj == null) {
				return null;
			} else if (obj instanceof RelationshipVersionAdaptor) {
				//TODO Joel, why wouldn't you just write this in the impl of RelationshipVersionAdapter?
				RelationshipVersionAdaptor<?> rva = (RelationshipVersionAdaptor<?>) obj;
				String orig = Get.conceptDescriptionText(rva.getOriginSequence());
				String dest = Get.conceptDescriptionText(rva.getDestinationSequence());
				String type = Get.conceptDescriptionText(rva.getTypeSequence());
				String stamp = Get.conceptDescriptionText(rva.getStampSequence());
				return "RelationshipVersionAdaptor: origin=" + orig + " (seq=" + rva.getOriginSequence() + "), dest=" + dest + " (seq=" + rva.getDestinationSequence() + "), type=" + type + " (seq=" + rva.getTypeSequence() + "), premise=" + rva.getPremiseType().name() + ", group=" + rva.getGroup() + " stamp=" + stamp;
			} else {
				return obj.toString();
			}
		} catch (Exception e) {
			LOG.error("Caught " + e.getClass().getName() + " " + e.getLocalizedMessage(), e);
			return null;
		}
	}
}
