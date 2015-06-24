package gov.va.isaac.util;

import gov.vha.isaac.metadata.coordinates.ViewCoordinates;
import gov.vha.isaac.metadata.source.IsaacMetadataAuxiliaryBinding;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

import org.apache.mahout.math.Arrays;
import org.ihtsdo.otf.tcc.api.concept.ConceptVersionBI;
import org.ihtsdo.otf.tcc.api.coordinate.Position;
import org.ihtsdo.otf.tcc.api.coordinate.Status;
import org.ihtsdo.otf.tcc.api.coordinate.ViewCoordinate;
import org.ihtsdo.otf.tcc.api.relationship.RelAssertionType;
import org.ihtsdo.otf.tcc.api.store.Ts;

public class ViewCoordinateFactory {
	public static ViewCoordinate get(UUID path, EnumSet<Status> statuses, long time, RelAssertionType relAssertionType, Set<UUID> modules) throws IOException {
		if (path.equals(IsaacMetadataAuxiliaryBinding.DEVELOPMENT.getPrimodialUuid())
				&& statuses.size() == 2 && statuses.contains(Status.ACTIVE) && statuses.contains(Status.INACTIVE)
				&& relAssertionType == RelAssertionType.INFERRED
				&& time == Long.MAX_VALUE
				&& (modules == null || modules.size() == 0)) {
			return ViewCoordinates.getDevelopmentInferredLatest();
		} else if  (path.equals(IsaacMetadataAuxiliaryBinding.DEVELOPMENT.getPrimodialUuid())
				&& statuses.size() == 1 && statuses.contains(Status.ACTIVE)
				&& relAssertionType == RelAssertionType.INFERRED
				&& time == Long.MAX_VALUE
				&& (modules == null || modules.size() == 0)) {
			return ViewCoordinates.getDevelopmentInferredLatestActiveOnly();
		} else if (path.equals(IsaacMetadataAuxiliaryBinding.DEVELOPMENT.getPrimodialUuid())
				&& statuses.size() == 2 && statuses.contains(Status.ACTIVE) && statuses.contains(Status.INACTIVE)
				&& relAssertionType == RelAssertionType.STATED
				&& time == Long.MAX_VALUE
				&& (modules == null || modules.size() == 0)) {
			return ViewCoordinates.getDevelopmentStatedLatest();
		} else if (path.equals(IsaacMetadataAuxiliaryBinding.DEVELOPMENT.getPrimodialUuid())
				&& statuses.size() == 1 && statuses.contains(Status.ACTIVE)
				&& relAssertionType == RelAssertionType.STATED
				&& time == Long.MAX_VALUE
				&& (modules == null || modules.size() == 0)) {
			return ViewCoordinates.getDevelopmentStatedLatestActiveOnly();
		} else if (path.equals(IsaacMetadataAuxiliaryBinding.MASTER.getPrimodialUuid())
				&& statuses.size() == 2 && statuses.contains(Status.ACTIVE) && statuses.contains(Status.INACTIVE)
				&& relAssertionType == RelAssertionType.INFERRED
				&& time == Long.MAX_VALUE
				&& (modules == null || modules.size() == 0)) {
			return ViewCoordinates.getMasterInferredLatest();
		} else if (path.equals(IsaacMetadataAuxiliaryBinding.MASTER.getPrimodialUuid())
				&& statuses.size() == 1 && statuses.contains(Status.ACTIVE)
				&& relAssertionType == RelAssertionType.INFERRED
				&& time == Long.MAX_VALUE
				&& (modules == null || modules.size() == 0)) {
			return ViewCoordinates.getMasterInferredLatest();
		} else if (path.equals(IsaacMetadataAuxiliaryBinding.MASTER.getPrimodialUuid())
				&& statuses.size() == 2 && statuses.contains(Status.ACTIVE) && statuses.contains(Status.INACTIVE)
				&& relAssertionType == RelAssertionType.STATED
				&& time == Long.MAX_VALUE
				&& (modules == null || modules.size() == 0)) {
			return ViewCoordinates.getMasterStatedLatest();
		} else if (path.equals(IsaacMetadataAuxiliaryBinding.MASTER.getPrimodialUuid())
				&& statuses.size() == 1 && statuses.contains(Status.ACTIVE)
				&& relAssertionType == RelAssertionType.STATED
				&& time == Long.MAX_VALUE
				&& (modules == null || modules.size() == 0)) {
			return ViewCoordinates.getMasterStatedLatestActiveOnly();
		} else {
			ConceptVersionBI pathConcept = OTFUtility.getConceptVersion(path);
			String pathDesc = OTFUtility.getDescription(pathConcept).toLowerCase();
			String statusDesc = null;
			if (statuses.size() == 2 && statuses.contains(Status.ACTIVE) && statuses.contains(Status.INACTIVE)) {
				// noop
			} else if (statuses.size() == 1 && statuses.contains(Status.ACTIVE)) {
				statusDesc = "active-only";
			} else if (statuses.size() == 1 && statuses.contains(Status.INACTIVE)) {
				statusDesc = "inactive-only";
			}
			String relAssertionTypeDesc = relAssertionType.name().toLowerCase();
			String timeDesc = time == Long.MAX_VALUE ? "latest" : new SimpleDateFormat().format(new Date(time));
			String modulesDesc = null;
			if (modules != null && modules.size() > 0) {
				modulesDesc = Arrays.toString(modules.toArray());
			}

			String desc = pathDesc + " " + relAssertionTypeDesc;
			if (statusDesc != null) {
				desc += " " + statusDesc;
			}
			desc += " " + timeDesc;
			if (modulesDesc != null) {
				desc += " " + modulesDesc;
			}

			ViewCoordinate viewCoordinate = new ViewCoordinate(UUID.randomUUID(),
					desc, Ts.get().getMetadataVC());
			Position viewPosition
					= Ts.get().newPosition(Ts.get().getPath(pathConcept.getNid()),
					time);

			viewCoordinate.setViewPosition(viewPosition);
			viewCoordinate.setRelationshipAssertionType(relAssertionType);
			viewCoordinate.setAllowedStatus(statuses);
			viewCoordinate.setDescriptionLogicProfileSpec(IsaacMetadataAuxiliaryBinding.EL_PLUS_PLUS);
			viewCoordinate.setStatedAssemblageSpec(IsaacMetadataAuxiliaryBinding.EL_PLUS_PLUS_STATED_FORM);
			viewCoordinate.setInferredAssemblageSpec(IsaacMetadataAuxiliaryBinding.EL_PLUS_PLUS_INFERRED_FORM);
			viewCoordinate.setClassifierSpec(IsaacMetadataAuxiliaryBinding.SNOROCKET);
			viewCoordinate.getDescriptionTypePrefSpecs().add(IsaacMetadataAuxiliaryBinding.SYNONYM);
			viewCoordinate.getDescriptionTypePrefSpecs().add(IsaacMetadataAuxiliaryBinding.FULLY_SPECIFIED_NAME);
			viewCoordinate.getLangPrefSpecs().clear();
			viewCoordinate.getLangPrefSpecs().add(IsaacMetadataAuxiliaryBinding.US_ENGLISH_DIALECT);
			viewCoordinate.getLangPrefSpecs().add(IsaacMetadataAuxiliaryBinding.GB_ENGLISH_DIALECT);

			if (modules != null && modules.size() > 0) {
				throw new IllegalArgumentException("Passing modules not yet supported");
			}

			return viewCoordinate;
		}
	}
}