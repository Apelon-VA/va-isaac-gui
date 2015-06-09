package gov.va.isaac.gui.conceptViews.helpers;

import gov.va.isaac.util.OTFUtility;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.ihtsdo.otf.tcc.api.chronicle.ComponentVersionBI;
import org.ihtsdo.otf.tcc.api.conattr.ConceptAttributeVersionBI;
import org.ihtsdo.otf.tcc.api.concept.ConceptVersionBI;
import org.ihtsdo.otf.tcc.api.id.IdBI;
import org.ihtsdo.otf.tcc.api.metadata.binding.SnomedMetadataRf2;
import org.ihtsdo.otf.tcc.api.metadata.binding.TermAux;
import org.ihtsdo.otf.tcc.api.refex.RefexChronicleBI;
import org.ihtsdo.otf.tcc.api.refex.RefexVersionBI;
import org.ihtsdo.otf.tcc.api.refex.type_long.RefexLongVersionBI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConceptViewerHelper {
	private static Integer snomedAssemblageNid;
	private static final Logger LOG = LoggerFactory.getLogger(ConceptViewerHelper.class);

	private ConceptViewerHelper()
	{
		//helper, don't construct....
	}

	public static int getSnomedAssemblageNid() {
		if (snomedAssemblageNid == null)
		{
			snomedAssemblageNid = OTFUtility.getConceptVersion(TermAux.SNOMED_IDENTIFIER.getUuids()[0]).getNid();
		}
		return snomedAssemblageNid;
	}

	public static Optional<Long> getSctId(ComponentVersionBI attr)  {
		// Official approach found int AlternativeIdResource.class
		
		try {
			for (RefexChronicleBI<?> annotation : attr.getAnnotations()) {
				if (annotation.getAssemblageNid() == getSnomedAssemblageNid()) {
					RefexLongVersionBI<?> sctid = (RefexLongVersionBI<?>) annotation.getPrimordialVersion();
					return Optional.of(sctid.getLong1());
				}
			}

			// legacy representation of SCTID for use with older econcepts files
			for (IdBI id : attr.getAllIds()) {
				// Identify "SCT" identifiers
				if (id.getAuthorityNid() == TermAux.SNOMED_IDENTIFIER.getLenient().getNid()) {
					// Found SCTID, return it
					return Optional.of(Long.parseLong(id.getDenotation().toString()));
				}
			}

		} catch (Exception e) {
			LOG.warn("Could not access annotations for: " + attr.getPrimordialUuid(), e);
			Optional.empty();
		}
		
		return Optional.empty();
	}


	public static String getPrimDef(ConceptAttributeVersionBI<?> attr) {
		String status = "Primitive";
		if (attr.isDefined()) {
			status = "Fully Defined";
		}
		
		return status;
	}
	
	public static int getPrimDefNid(ConceptAttributeVersionBI<?> attr) {
		try {
			int nid = SnomedMetadataRf2.PRIMITIVE_RF2.getLenient().getNid();
			if (attr.isDefined()) {
				nid = SnomedMetadataRf2.DEFINED_RF2.getLenient().getNid();
			}
			return nid;
		} catch (Exception e) {
			LOG.error("Unable to access ConceptSpec Nids", e);
			return -1;
		}
	}


	public static ConceptAttributeVersionBI<?> getConceptAttributes(ConceptVersionBI con) {
		try {
			Optional<? extends ConceptAttributeVersionBI> attr = con.getConceptAttributesActive();
			if (!attr.isPresent()) {
				attr = con.getConceptAttributes().getVersion(OTFUtility.getViewCoordinate());
				if (!attr.isPresent()) {
					// handle Unhandled functionality
					//TODO what on earth is this method doing?  why would we display arbitrary attributes, if nothign is available on the path?
					List<? extends ConceptAttributeVersionBI> x = con.getConceptAttributes().getVersions();
					if (x.size() > 0)
					{
						attr = Optional.of(x.toArray(new ConceptAttributeVersionBI[x.size()])[x.size() - 1]);
					}
				}
			}
		
			return attr.orElse(null);
		} catch (Exception e) {
			LOG.debug("Cannot access concept's attributes for concept: " + con.getNid(), e);
			return null;
		}
	}

	public static Set<RefexVersionBI<?>> getAnnotations(ComponentVersionBI comp) {
		Set<RefexVersionBI<?>> retSet = new HashSet<>();
		
		try {
			for (RefexVersionBI<?> annot : comp.getAnnotationsActive(OTFUtility.getViewCoordinate())) {
				if (annot.getAssemblageNid() != getSnomedAssemblageNid()) {
					retSet.add(annot);
				}
			}
		} catch (IOException e) {
			LOG.error("Unable to access annotations", e);
		}
		
		return retSet;
	}
	
}
