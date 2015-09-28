package gov.va.isaac.gui.conceptview.popups;

import gov.va.isaac.gui.conceptview.ConceptViewColumnType;
import gov.va.isaac.gui.conceptview.data.Concept;
import gov.va.isaac.gui.conceptview.data.ConceptDescription;
import gov.va.isaac.gui.conceptview.data.ConceptId;
import gov.va.isaac.gui.conceptview.data.ConceptIdType;
import gov.vha.isaac.metadata.coordinates.StampCoordinates;
import gov.vha.isaac.metadata.source.IsaacMetadataAuxiliaryBinding;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.chronicle.LatestVersion;
import gov.vha.isaac.ochre.api.component.concept.ConceptSnapshot;
import gov.vha.isaac.ochre.api.component.concept.ConceptVersion;
import gov.vha.isaac.ochre.api.component.sememe.SememeSnapshotService;
import gov.vha.isaac.ochre.api.component.sememe.version.DescriptionSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.DynamicSememe;
import gov.vha.isaac.ochre.model.sememe.version.StringSememeImpl;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Optional;
import java.util.UUID;
import javafx.scene.layout.Region;

public class PopupHelper {
	
	private static final DateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("MM/dd/yy HH:mm:ss");
	
	public static void showConceptIdList(ConceptSnapshot concept, Region popOverRegion) {
		PopupList popup = new PopupList();
		popup.setTitle("Display IDs for Concept " + Get.conceptDescriptionText(concept.getNid()));
		popup.setColumnTypes(new ConceptViewColumnType[] { 
			ConceptViewColumnType.ID_TYPE, 
			ConceptViewColumnType.ID_VALUE, 
			ConceptViewColumnType.TIMESTAMP 
		});
		popup.setPopOverRegion(popOverRegion);
		
		SememeSnapshotService<StringSememeImpl> svc = Get.sememeService().getSnapshot(StringSememeImpl.class, concept.getStampCoordinate());
		Optional<LatestVersion<StringSememeImpl>> sctSememe = svc.getLatestSememeVersionsForComponentFromAssemblage(concept.getNid(), 
				IsaacMetadataAuxiliaryBinding.SNOMED_INTEGER_ID.getConceptSequence()).findFirst();
		if (sctSememe.isPresent()) {
			ConceptId id = new ConceptId(ConceptIdType.SCT, sctSememe.get().value().getString(), TIMESTAMP_FORMAT.format(sctSememe.get().value().getTime()));
			popup.addData(id);
		}
		
		Optional<LatestVersion<StringSememeImpl>> loincSememe = svc.getLatestSememeVersionsForComponentFromAssemblage(concept.getNid(), 
				IsaacMetadataAuxiliaryBinding.LOINC_NUM.getConceptSequence()).findFirst();
		if (loincSememe.isPresent()) {
			ConceptId id = new ConceptId(ConceptIdType.LOINC, loincSememe.get().value().getString(), TIMESTAMP_FORMAT.format(loincSememe.get().value().getTime()));
			popup.addData(id);
		}
		
		SememeSnapshotService<DynamicSememe> dsvc = Get.sememeService().getSnapshot(DynamicSememe.class, concept.getStampCoordinate());
		Optional<LatestVersion<DynamicSememe>> rxnSememe = dsvc.getLatestSememeVersionsForComponentFromAssemblage(concept.getNid(), 
				IsaacMetadataAuxiliaryBinding.RXCUI.getConceptSequence()).findFirst();
		if (rxnSememe.isPresent()) {
			ConceptId id = new ConceptId(ConceptIdType.RXNORM, rxnSememe.get().value().getData()[0].getDataObject().toString(), 
					TIMESTAMP_FORMAT.format(rxnSememe.get().value().getTime()));
			popup.addData(id);
		}
		
		// TODO RxNorm ID
		
		for (UUID uuid : concept.getUuidList()) {
			ConceptId id = new ConceptId(ConceptIdType.UUID, uuid.toString());
			popup.addData(id);
		}
		
		popup.showPopup();
	}

	public static void showDescriptionIdList(ConceptDescription conceptDescription, Region popOverRegion) {
		PopupList popup = new PopupList();
		popup.setTitle("Display IDs for Description " + conceptDescription.getType() + " " + conceptDescription.getValue());
		popup.setColumnTypes(new ConceptViewColumnType[] { 
			ConceptViewColumnType.ID_TYPE, 
			ConceptViewColumnType.ID_VALUE, 
			ConceptViewColumnType.TIMESTAMP 
		});
		popup.setPopOverRegion(popOverRegion);
		
		// TODO use default stamp coordinate?
		//SememeSnapshotService<StringSememeImpl> svc = Get.sememeService().getSnapshot(StringSememeImpl.class, Get.configurationService().getDefaultStampCoordinate());
		SememeSnapshotService<StringSememeImpl> svc = Get.sememeService().getSnapshot(StringSememeImpl.class, StampCoordinates.getDevelopmentLatest());
		int descNid = conceptDescription.getStampedVersion().getNid();
		
		Optional<LatestVersion<StringSememeImpl>> sctSememe = svc.getLatestSememeVersionsForComponentFromAssemblage(descNid, IsaacMetadataAuxiliaryBinding.SNOMED_INTEGER_ID.getConceptSequence()).findFirst();
		if (sctSememe.isPresent()) {
			ConceptId id = new ConceptId(ConceptIdType.SCT, sctSememe.get().value().getString(), TIMESTAMP_FORMAT.format(sctSememe.get().value().getTime()));
			popup.addData(id);
		}
		
		// TODO RxNorm ID ?? RxNorm does have ids assigned to the descriptions, but I'm not sure we even want to surface them, they aren't 
		//as 'official' as sctids.  LOINC doesn't have any IDs for descriptions.
		
		
		for (UUID uuid : conceptDescription.getStampedVersion().getUuidList()) {
			ConceptId id = new ConceptId(ConceptIdType.UUID, uuid.toString());
			popup.addData(id);
		}

		popup.showPopup();
}
	
	public static void showDescriptionHistory(ConceptDescription conceptDescription, Region popOverRegion) {
		PopupList popup = new PopupList();
		popup.setTitle("History of description " + conceptDescription.getType() + " \"" + conceptDescription.getValue() + "\"");
		popup.setColumnTypes(new ConceptViewColumnType[] { 
			ConceptViewColumnType.TYPE, 
			ConceptViewColumnType.TERM,
			ConceptViewColumnType.LANGUAGE,
			ConceptViewColumnType.SIGNIFICANCE,
			ConceptViewColumnType.STAMP_MODULE,
			ConceptViewColumnType.STAMP_STATE,
			ConceptViewColumnType.STAMP_TIME,
			ConceptViewColumnType.STAMP_AUTHOR,
			ConceptViewColumnType.STAMP_PATH
		});
		popup.setPopOverRegion(popOverRegion);
		
		for (DescriptionSememe<?> desc : conceptDescription.getStampedVersion().getChronology().getVersionList()) {
			ConceptDescription cd = new ConceptDescription(desc);
			popup.addData(cd);
		}
		
		popup.showPopup();
	}
	
	public static void showConceptHistory(ConceptSnapshot concept, Region popOverRegion) {
		PopupList popup = new PopupList();
		popup.setTitle("Concept History");
		popup.setColumnTypes(new ConceptViewColumnType[] { 
			ConceptViewColumnType.STAMP_MODULE,
			ConceptViewColumnType.STAMP_STATE,
			ConceptViewColumnType.STAMP_TIME,
			ConceptViewColumnType.STAMP_AUTHOR,
			ConceptViewColumnType.STAMP_PATH
		});
		popup.setPopOverRegion(popOverRegion);
		
		for (ConceptVersion<?> cv : concept.getChronology().getVersionList()) {
			Concept c = new Concept(cv);
			popup.addData(c);
		}
		
		popup.showPopup();
	}

}
