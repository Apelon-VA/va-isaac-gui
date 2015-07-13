/**
 * 
 */
package gov.va.isaac.util;

import gov.va.isaac.AppContext;
import gov.va.isaac.ExtendedAppContext;
import gov.va.isaac.config.profiles.UserProfile;
import gov.va.isaac.config.profiles.UserProfileManager;
import gov.va.isaac.config.users.InvalidUserException;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.IdentifierService;
import gov.vha.isaac.ochre.api.LookupService;
import gov.vha.isaac.ochre.api.chronicle.LatestVersion;
import gov.vha.isaac.ochre.api.component.concept.ConceptChronology;
import gov.vha.isaac.ochre.api.component.concept.ConceptService;
import gov.vha.isaac.ochre.api.component.concept.ConceptSnapshotService;
import gov.vha.isaac.ochre.api.component.concept.ConceptVersion;
import gov.vha.isaac.ochre.api.component.sememe.version.DescriptionSememe;
import gov.vha.isaac.ochre.api.coordinate.LanguageCoordinate;
import gov.vha.isaac.ochre.api.coordinate.StampCoordinate;
import gov.vha.isaac.ochre.api.tree.Tree;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.ihtsdo.otf.tcc.api.coordinate.ViewCoordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author joel
 *
 */
public final class ConceptChronologyUtil {
	private static final Logger LOG = LoggerFactory.getLogger(ConceptChronologyUtil.class);

	private ConceptChronologyUtil() {}

	public static ConceptSnapshotService conceptSnapshot(ViewCoordinate vc) {
		return conceptSnapshot(vc, vc);
	}
	public static ConceptSnapshotService conceptSnapshot(StampCoordinate stampCoordinate, LanguageCoordinate languageCoordinate) {
		return LookupService.getService(ConceptService.class).getSnapshot(stampCoordinate, languageCoordinate);
	}

	public static String conceptDescriptionText(int conceptId, ViewCoordinate vc) {
		return conceptDescriptionText(conceptId, conceptSnapshot(vc));
	}
	public static String conceptDescriptionText(int conceptId, ConceptSnapshotService snapshot) {
		Optional<LatestVersion<DescriptionSememe>> descriptionOptional = 
				snapshot.getDescriptionOptional(conceptId);
		if (descriptionOptional.isPresent()) {
			return descriptionOptional.get().value().getText();
		}
		return "No desc for: " + conceptId;
	}
	public static String conceptDescriptionText(int conceptId) {
		return Get.conceptDescriptionText(conceptId);
	}

	public static Optional<LatestVersion<DescriptionSememe>> getDescriptionOptional(ConceptChronology<?> conceptChronology) {
		ViewCoordinate vc = OTFUtility.getViewCoordinate();
		return getDescriptionOptional(conceptChronology, vc);
	}
	public static Optional<LatestVersion<DescriptionSememe>> getDescriptionOptional(ConceptChronology<?> conceptChronology, ViewCoordinate vc) {
		return getDescriptionOptional(conceptChronology, vc, vc);
	}
	public static Optional<LatestVersion<DescriptionSememe>> getDescriptionOptional(ConceptChronology<?> conceptChronology, LanguageCoordinate languageCoordinate, StampCoordinate stampCoordinate) {
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

			Optional<LatestVersion<DescriptionSememe>> optional = null;
			if (userProfile.getDisplayFSN()) {
				optional = ConceptChronologyUtil.conceptSnapshot(stampCoordinate, languageCoordinate).getFullySpecifiedDescription(conceptChronology.getNid());
				//optional = conceptChronology.getFullySpecifiedDescription(languageCoordinate, stampCoordinate);
			} else {
				optional = ConceptChronologyUtil.conceptSnapshot(stampCoordinate, languageCoordinate).getPreferredDescription(conceptChronology.getNid());
				//optional = conceptChronology.getPreferredDescription(languageCoordinate, stampCoordinate);
			}

			return optional;
		} catch (RuntimeException e) {
			LOG.error("Failed determining correct description type. Caught " + e.getClass().getName() + " " + e.getLocalizedMessage(), e);
			
			throw e;
		}
	}

	public static String getDescription(ConceptChronology<?> conceptChronology) {
		ViewCoordinate vc = OTFUtility.getViewCoordinate();
		return getDescription(conceptChronology, vc);
	}
	public static String getDescription(ConceptChronology<?> conceptChronology, ViewCoordinate vc) {
		return getDescription(conceptChronology, vc, vc);
	}
	public static String getDescription(ConceptChronology<?> conceptChronology, LanguageCoordinate languageCoordinate, StampCoordinate stampCoordinate) {
		Optional<LatestVersion<DescriptionSememe>> optional = getDescriptionOptional(conceptChronology, languageCoordinate, stampCoordinate);

		if (optional.isPresent() && optional.get().value() != null && optional.get().value().getText() != null) {
			return optional.get().value().getText();
		} else {
			String otfDesc = OTFUtility.getDescription(conceptChronology.getNid());
			
			if (otfDesc != null) {
				return otfDesc;
			} else {
				return null;
			}
		}
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

	public static Set<ConceptChronology<? extends ConceptVersion>> getChildrenAsConceptVersions(ConceptChronology<? extends ConceptVersion> parent, Tree taxonomyTree, ViewCoordinate vc) {
		Set<ConceptChronology<? extends ConceptVersion>> conceptVersions = new HashSet<>();
		
		for (Integer nid : getChildrenAsConceptNids(parent, taxonomyTree)) {
			conceptVersions.add(Get.conceptService().getConcept(nid));
		}
		
		return conceptVersions;
	}
	
	
	// TODO this Tree API should work...
	//		Tree ancestorTree = taxonomyTree.createAncestorTree(child.getConceptSequence());
//	
//	return getChildrenAsConceptNids(child, ancestorTree);
	public static Set<Integer> getParentsAsConceptNids(ConceptChronology<? extends ConceptVersion> child, Tree taxonomyTree, ViewCoordinate vc) {
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

	public static Set<ConceptChronology<? extends ConceptVersion>> getParentsAsConceptVersions(ConceptChronology<? extends ConceptVersion> child, Tree taxonomyTree, ViewCoordinate vc) {
		Set<ConceptChronology<? extends ConceptVersion>> parentConcepts = new HashSet<>();
		for (int nid : getParentsAsConceptNids(child, taxonomyTree, vc)) {
			parentConcepts.add(Get.conceptService().getConcept(nid));
		}
		
		return parentConcepts;
		
		// TODO this Tree API should work...
		//		Tree ancestorTree = taxonomyTree.createAncestorTree(child.getConceptSequence());
//	
//		return getChildrenAsConceptVersions(child, ancestorTree, vc);
	}
}
