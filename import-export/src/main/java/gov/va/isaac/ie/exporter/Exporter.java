package gov.va.isaac.ie.exporter;

import java.io.IOException;
import java.util.Optional;
import org.ihtsdo.otf.tcc.api.conattr.ConceptAttributeVersionBI;
import org.ihtsdo.otf.tcc.api.concept.ConceptVersionBI;
import org.ihtsdo.otf.tcc.api.contradiction.ContradictionException;
import org.ihtsdo.otf.tcc.api.coordinate.ViewCoordinate;
import org.ihtsdo.otf.tcc.api.description.DescriptionChronicleBI;
import org.ihtsdo.otf.tcc.api.description.DescriptionVersionBI;
import org.ihtsdo.otf.tcc.api.refex.RefexChronicleBI;
import org.ihtsdo.otf.tcc.api.refex.RefexVersionBI;
import org.ihtsdo.otf.tcc.api.relationship.RelationshipChronicleBI;
import org.ihtsdo.otf.tcc.api.relationship.RelationshipVersionBI;
import gov.va.isaac.util.OTFUtility;
import gov.va.isaac.util.ProgressReporter;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.chronicle.LatestVersion;
import gov.vha.isaac.ochre.api.component.sememe.SememeChronology;
import gov.vha.isaac.ochre.api.component.sememe.version.SememeVersion;

/**
 * Generically represents an Exporter.
 */
public interface Exporter extends ProgressReporter {

  /**
   * Exports the nid.
   *
   * @param pathNid the path nid
   * @throws Exception
   */
  public void export(int pathNid) throws Exception;

  /**
   * Cancel.
   */
  public void cancel();
  
  public static boolean isQualifying(int conceptNid, int pathNid) throws ContradictionException, IOException {
	ViewCoordinate vc = OTFUtility.getViewCoordinate();
	
	return Exporter.isQualifying(conceptNid, pathNid, vc);
  }

  /**
   * Indicates whether or not the concept qualifies as having a component
   * with the indicated path.
   *
   * @param conceptNid the concept nid
   * @param pathNid the path nid
   * @return <code>true</code> if so, <code>false</code> otherwise
   * @throws IOException
   * @throws ContradictionException
   */
  public static boolean isQualifying(int conceptNid, int pathNid, ViewCoordinate vc)
	throws ContradictionException, IOException {
	ConceptVersionBI cv = OTFUtility.getConceptVersion(conceptNid, vc);
	if (cv.getPathNid() == pathNid)
	  return true;

	//TODO dan notes this doesn't check the path of any nested annotations
	// atts
	ConceptAttributeVersionBI<?> att =
		cv.getConceptAttributes().getVersion(vc).get();
	if (att == null)
	  return false;
	if (att.getPathNid() == pathNid)
	  return true;
	
	// descs
	for (DescriptionChronicleBI dc : cv.getDescriptions()) {
	  DescriptionVersionBI<?> dv = dc.getVersion(vc).get();
	  if (dv.getPathNid() == pathNid) 
		return true;
	}

	// rels
	for (RelationshipChronicleBI rc : cv.getRelationshipsOutgoing()) {
	  RelationshipVersionBI<?> rv = rc.getVersion(vc).get();
	  if (rv.getPathNid() == pathNid) 
		return true;
	}
	
	// refex
	for (RefexChronicleBI<?> rc : cv.getAnnotations()) {
	  RefexVersionBI<?> rv = rc.getVersion(vc).get();
	  if (rv.getPathNid() == pathNid) 
		return true;
	}
	
	try {
		return Get.sememeService().getSememesForComponent(cv.getNid()).anyMatch(sememeC ->
		{
			@SuppressWarnings({ "rawtypes", "unchecked" })
			Optional<LatestVersion<SememeVersion>> latest = ((SememeChronology)sememeC).getLatestVersion(SememeVersion.class, vc);
			if (latest.isPresent() && Get.identifierService().getConceptNid(latest.get().value().getPathSequence()) == pathNid)
			{
				return true;
			}
			return false;
		});
	} catch (Exception e) {
		e.printStackTrace();
	}
	return false;
	}
}
