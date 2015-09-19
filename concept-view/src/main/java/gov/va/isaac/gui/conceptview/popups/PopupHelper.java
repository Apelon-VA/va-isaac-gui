package gov.va.isaac.gui.conceptview.popups;

import gov.va.isaac.gui.conceptview.ConceptViewColumnType;
import gov.va.isaac.gui.conceptview.data.ConceptDescription;
import gov.va.isaac.gui.conceptview.data.ConceptId;
import gov.va.isaac.gui.conceptview.data.ConceptIdType;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.component.concept.ConceptSnapshot;
import gov.vha.isaac.ochre.api.component.sememe.version.DescriptionSememe;
import gov.vha.isaac.ochre.impl.utility.Frills;

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
		popup.setColumnTypes(new ConceptViewColumnType[] { ConceptViewColumnType.ID_TYPE, ConceptViewColumnType.ID_VALUE, ConceptViewColumnType.TIMESTAMP });
		popup.setPopOverRegion(popOverRegion);
		
		Optional<Long> sctId = Frills.getSctId(concept.getNid(), concept.getStampCoordinate());
		if (sctId.isPresent()) {
			// TODO get timestamp?
			ConceptId id = new ConceptId(ConceptIdType.SCT, sctId.get().toString(), "");
			popup.addData(id);
		}
		
		// TODO LOINC ID
		
		// TODO RxNorm ID
		
		for (UUID uuid : concept.getUuidList()) {
			String ts = "";
			try {
				ts = TIMESTAMP_FORMAT.format(uuid.timestamp());
			} catch (Exception e) {
				// ignore
			}
			ConceptId id = new ConceptId(ConceptIdType.UUID, uuid.toString(), ts);
			popup.addData(id);
		}
		
		popup.showPopup();
	}

	public static void showDescriptionIdList(ConceptDescription conceptDescription, Region popOverRegion) {
		PopupList popup = new PopupList();
		popup.setTitle("Display IDs for Description " + conceptDescription.getType() + " " + conceptDescription.getValue());
		popup.setColumnTypes(new ConceptViewColumnType[] { ConceptViewColumnType.ID_TYPE, ConceptViewColumnType.ID_VALUE, ConceptViewColumnType.TIMESTAMP });
		popup.setPopOverRegion(popOverRegion);
		
		// TODO get SCT ID. I don't know how to get the description's stamp coordinate
		//Optional<Long> sctId = Frills.getSctId();
		
		// TODO LOINC ID
		
		// TODO RxNorm ID
		
		
		for (UUID uuid : conceptDescription.getStampedVersion().getUuidList()) {
			String ts = "";
			try {
				ts = TIMESTAMP_FORMAT.format(uuid.timestamp());
			} catch (Exception e) {
				// ignore
			}
			ConceptId id = new ConceptId(ConceptIdType.UUID, uuid.toString(), ts);
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
		// TODO this.
	}
}
