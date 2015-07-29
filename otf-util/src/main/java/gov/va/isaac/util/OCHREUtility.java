/**
 * 
 */
package gov.va.isaac.util;

import gov.va.isaac.AppContext;
import gov.va.isaac.ExtendedAppContext;
import gov.va.isaac.config.profiles.UserProfile;
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
import gov.vha.isaac.ochre.api.component.sememe.version.DescriptionSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.SememeVersion;
import gov.vha.isaac.ochre.api.coordinate.LanguageCoordinate;
import gov.vha.isaac.ochre.api.coordinate.StampCoordinate;
import gov.vha.isaac.ochre.api.coordinate.TaxonomyCoordinate;
import gov.vha.isaac.ochre.api.tree.Tree;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.ihtsdo.otf.tcc.api.contradiction.ContradictionException;
import org.ihtsdo.otf.tcc.api.spec.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author joel
 *
 */
public final class OCHREUtility {
	private static final Logger LOG = LoggerFactory.getLogger(OCHREUtility.class);

	private OCHREUtility() {}

	public static Set<ConceptVersion<?>> getPathConcepts() throws ValidationException, IOException, ContradictionException {
		Stream<SememeChronology<? extends SememeVersion<?>>> sememes = Get.sememeService().getSememesFromAssemblage(IsaacMetadataAuxiliaryBinding.PATHS_ASSEMBLAGE.getSequence());
		//LOG.debug("Loaded {} sememes from assemblage {}", sememes.count(), Get.conceptDescriptionText(IsaacMetadataAuxiliaryBinding.PATHS_ASSEMBLAGE.getNid()));

		final Set<ConceptVersion<?>> pathConcepts = new HashSet<>();
		Consumer<? super SememeChronology<? extends SememeVersion>> action = new Consumer<SememeChronology<? extends SememeVersion>>() {
			@Override
			public void accept(SememeChronology<? extends SememeVersion> t) {
				ConceptChronology<? extends ConceptVersion<?>> pathCC = Get.conceptService().getConcept(t.getReferencedComponentNid());
				
				ConceptChronology pathCCTemp = (ConceptChronology)pathCC;
				Optional<LatestVersion<ConceptVersion<?>>> latestPathConceptVersion = pathCCTemp.getLatestVersion(ConceptVersion.class, StampCoordinates.getDevelopmentLatest());
				if (latestPathConceptVersion.isPresent()) {
					pathConcepts.add(latestPathConceptVersion.get().value());
				}
			}
		};
		sememes.distinct().forEach(action);

//		Iterator<SememeChronology<? extends SememeVersion>> it = sememes.iterator();
//		for (SememeChronology<? extends SememeVersion> current = it.next(); it.hasNext(); current = it.next()) {
//			action.accept(current);
//		}

		if (pathConcepts.size() == 0) {
			LOG.error("No paths loaded based on membership in {}", IsaacMetadataAuxiliaryBinding.PATHS_ASSEMBLAGE);
		} else {
			LOG.debug("Loaded {} paths: {}", pathConcepts.size(), pathConcepts);
		}
			
		return Collections.unmodifiableSet(pathConcepts);
	}

	public static ConceptSnapshotService conceptSnapshotService(TaxonomyCoordinate vc) {
		return conceptSnapshotService(vc.getStampCoordinate(), vc.getLanguageCoordinate());
	}
	public static ConceptSnapshotService conceptSnapshotService(StampCoordinate stampCoordinate, LanguageCoordinate languageCoordinate) {
		return LookupService.getService(ConceptService.class).getSnapshot(stampCoordinate, languageCoordinate);
	}

	public static String conceptDescriptionText(int conceptId, TaxonomyCoordinate vc) {
		return conceptDescriptionText(conceptId, conceptSnapshotService(vc));
	}
	public static String conceptDescriptionText(int conceptId, ConceptSnapshotService snapshot) {
		Optional<LatestVersion<DescriptionSememe<?>>> descriptionOptional = snapshot.getDescriptionOptional(conceptId);
		if (descriptionOptional.isPresent()) {
			return descriptionOptional.get().value().getText();
		}

		return null;
	}
	public static String conceptDescriptionText(int conceptId) {
		return Get.conceptDescriptionText(conceptId);
	}

	public static Optional<LatestVersion<DescriptionSememe<?>>> getDescriptionOptional(ConceptChronology<?> conceptChronology, TaxonomyCoordinate vc) {
		return getDescriptionOptional(conceptChronology, vc.getLanguageCoordinate(), vc.getStampCoordinate());
	}
	public static Optional<LatestVersion<DescriptionSememe<?>>> getDescriptionOptional(ConceptChronology<?> conceptChronology, LanguageCoordinate languageCoordinate, StampCoordinate stampCoordinate) {
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

			Optional<LatestVersion<DescriptionSememe<?>>> optional = null;
			if (userProfile.getDisplayFSN()) {
				optional = OCHREUtility.conceptSnapshotService(stampCoordinate, languageCoordinate).getFullySpecifiedDescription(conceptChronology.getNid());
			} else {
				optional = OCHREUtility.conceptSnapshotService(stampCoordinate, languageCoordinate).getPreferredDescription(conceptChronology.getNid());
			}

			return optional;
		} catch (RuntimeException e) {
			LOG.error("Failed determining correct description type. Caught " + e.getClass().getName() + " " + e.getLocalizedMessage(), e);
			
			throw e;
		}
	}
	public static Optional<LatestVersion<DescriptionSememe<?>>> getDescriptionOptional(int conceptId) {
		return getDescriptionOptional(Get.conceptSnapshot().getConceptSnapshot(conceptId).getChronology());
	}
	public static Optional<LatestVersion<DescriptionSememe<?>>> getDescriptionOptional(ConceptChronology<?> conceptChronology) {
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

			Optional<LatestVersion<DescriptionSememe<?>>> optional = null;
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

	public static String getDescription(UUID conceptUuid, TaxonomyCoordinate vc) {
		return getDescription(conceptSnapshotService(vc).getConceptSnapshot(Get.identifierService().getNidForUuids(conceptUuid)).getChronology(), vc);
	}
	public static String getDescription(ConceptChronology<?> conceptChronology, TaxonomyCoordinate vc) {
		return getDescription(conceptChronology, vc.getLanguageCoordinate(), vc.getStampCoordinate());
	}
	public static String getDescription(UUID conceptUuid, LanguageCoordinate languageCoordinate, StampCoordinate stampCoordinate) {
		return getDescription(conceptSnapshotService(stampCoordinate, languageCoordinate).getConceptSnapshot(Get.identifierService().getNidForUuids(conceptUuid)).getChronology(), languageCoordinate, stampCoordinate);
	}
	public static String getDescription(ConceptChronology<?> conceptChronology, LanguageCoordinate languageCoordinate, StampCoordinate stampCoordinate) {
		Optional<LatestVersion<DescriptionSememe<?>>> optional = getDescriptionOptional(conceptChronology, languageCoordinate, stampCoordinate);

		if (optional.isPresent() && optional.get().value() != null && optional.get().value().getText() != null) {
			return optional.get().value().getText();
		} else {
			String desc = conceptDescriptionText(conceptChronology.getNid(), conceptSnapshotService(stampCoordinate, languageCoordinate));
			if (desc != null) {
				return desc;
			}
			
			desc = conceptDescriptionText(conceptChronology.getNid());
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
	public static String getDescription(int conceptId) {
		return getDescription(Get.conceptService().getConcept(conceptId));
	}
	public static String getDescription(ConceptChronology<?> conceptChronology) {
		Optional<LatestVersion<DescriptionSememe<?>>> optional = getDescriptionOptional(conceptChronology);

		if (optional.isPresent() && optional.get().value() != null && optional.get().value().getText() != null) {
			return optional.get().value().getText();
		} else {
			String desc = conceptDescriptionText(conceptChronology.getNid());
			if (desc != null) {
				return desc;
			}

			desc = OTFUtility.getDescription(conceptChronology.getNid());
			if (desc != null) {
				return desc;
			}

			return null;
		}
	}
	public static String getDescription(UUID uuid) {
		return getDescription(Get.conceptService().getConcept(uuid));
	}
	
	public static Set<Integer> getChildrenAsConceptNids(ConceptChronology<? extends ConceptVersion> parent, Tree taxonomyTree) {
	
		Set<Integer> nidSet = new HashSet<>();
		
		for (int childSeq : taxonomyTree.getChildrenSequences(AppContext.getService(IdentifierService.class).getConceptSequence(parent.getNid()))) {
			Integer nid = AppContext.getService(IdentifierService.class).getConceptNid(childSeq);
			if (nid != null) {
				nidSet.add(nid);
			}
		}
		
		return nidSet;
	}
	
	public static Set<ConceptSnapshot> getChildrenAsConceptSnapshots(ConceptChronology<? extends ConceptVersion> parent, Tree taxonomyTree, StampCoordinate sc, LanguageCoordinate lc) {
		Set<ConceptSnapshot> conceptVersions = new HashSet<>();
	
		for (Integer nid : getChildrenAsConceptNids(parent, taxonomyTree)) {
			conceptVersions.add(conceptSnapshotService(sc, lc).getConceptSnapshot(nid));
		}
		
		return conceptVersions;
	}
	public static Set<ConceptChronology<? extends ConceptVersion>> getChildrenAsConceptChronologies(ConceptChronology<? extends ConceptVersion> parent, Tree taxonomyTree, StampCoordinate sc, LanguageCoordinate lc) {
		Set<ConceptChronology<? extends ConceptVersion>> conceptVersions = new HashSet<>();
	
		for (Integer nid : getChildrenAsConceptNids(parent, taxonomyTree)) {
			conceptVersions.add(conceptSnapshotService(sc, lc).getConceptSnapshot(nid).getChronology());
		}
		
		return conceptVersions;
	}
	public static Set<ConceptChronology<? extends ConceptVersion<?>>> getChildrenAsConceptChronologies(ConceptChronology<? extends ConceptVersion> parent, Tree taxonomyTree, TaxonomyCoordinate vc) {
		Set<ConceptChronology<? extends ConceptVersion<?>>> conceptVersions = new HashSet<>();
		
		for (Integer nid : getChildrenAsConceptNids(parent, taxonomyTree)) {
			conceptVersions.add(Get.conceptService().getConcept(nid));
		}
		
		return conceptVersions;
	}
	
	
	// TODO this Tree API should work...
	//		Tree ancestorTree = taxonomyTree.createAncestorTree(child.getConceptSequence());
//	
//	return getChildrenAsConceptNids(child, ancestorTree);
	public static Set<Integer> getParentsAsConceptNids(ConceptChronology<? extends ConceptVersion> child, Tree taxonomyTree, TaxonomyCoordinate vc) {
		int[] parentSequences = taxonomyTree.getParentSequences(child.getConceptSequence());
		
		Set<Integer> parentNids = new HashSet<>();
		
		for (int parentSequence : parentSequences) {
			int parentNid = Get.identifierService().getConceptNid(parentSequence);
			if (Get.taxonomyService().isChildOf(child.getNid(), parentNid, vc)) {
				if (! Get.taxonomyService().isChildOf(parentNid, child.getNid(), vc)) {
					parentNids.add(parentNid);
				} else {
					LOG.debug("{} is both child and parent of concept (retrieved by taxonomyTree.getParentSequences()) {}", getDescription(child), getDescription(Get.conceptService().getConcept(parentNid)));
				}
			} else {
				LOG.debug("{} is not a child of concept (retrieved by taxonomyTree.getParentSequences()) {}", getDescription(child), getDescription(Get.conceptService().getConcept(parentNid)));
			}
		}
		
		return parentNids;
	}

	public static Set<ConceptChronology<? extends ConceptVersion<?>>> getParentsAsConceptChronologies(ConceptChronology<? extends ConceptVersion> child, Tree taxonomyTree, TaxonomyCoordinate vc) {
		Set<ConceptChronology<? extends ConceptVersion<?>>> parentConcepts = new HashSet<>();
		for (int nid : getParentsAsConceptNids(child, taxonomyTree, vc)) {
			parentConcepts.add(Get.conceptService().getConcept(nid));
		}
		
		return parentConcepts;
		
		// TODO this Tree API should work...
		//		Tree ancestorTree = taxonomyTree.createAncestorTree(child.getConceptSequence());
//	
//		return getChildrenAsConceptVersions(child, ancestorTree, vc);
	}
	
	/**
	 * Recursively get Is a children of a concept
	 */
//	private static Set<ConceptSnapshot> getAllChildrenOfConcept(Set<Integer> handledConceptNids, ConceptSnapshot concept, StampCoordinate sc, LanguageCoordinate lc, boolean recursive, boolean leafOnly) 
//	{
//		Set<ConceptSnapshot> results = new HashSet<>();
//		
//		// This both prevents infinite recursion and avoids processing or returning of duplicates
//		if (handledConceptNids.contains(concept.getNid())) {
//			LOG.debug("Encountered already-handled concept \"{}\".  May be result of duplicate or source of potential infinite loop", OCHREUtility.getDescription(concept.getNid()));
//			return results;
//		}
//
//		Taxonomy taxonomy = Get.taxonomyService().getChildOfSequenceSet(concept.getConceptSequence(), TaxonomyCoordinates.)
//		concept.
//		int size = 0;
//		for (RelationshipVersionBI<?> r : concept.getRelationshipsIncomingActiveIsa())
//		{
//			size++;
//			if (handledConceptNids.contains(r.getOriginNid())) {
//				// avoids processing or returning of duplicates
//				LOG.debug("Encountered already-handled ORIGIN child concept \"{}\".  May be result of OTF-returned duplicate or source of potential infinite loop", OTFUtility.getDescription(r.getOriginNid(), vc));
//
//				continue;
//			}
//
//			ConceptVersionBI originConcept = getConceptVersion(r.getOriginNid(), vc);
//			if (!leafOnly)
//			{
//				results.add(originConcept);
//			}
//			if (recursive)
//			{
//				results.addAll(getAllChildrenOfConcept(handledConceptNids, originConcept, vc, recursive, leafOnly));
//			}
//		}
//		
//		if (leafOnly && size == 0 && !handledConceptNids.contains(concept.getNid()))
//		{
//			results.add(concept);
//		}
//		handledConceptNids.add(concept.getNid());
//		return results;
//	}
}
