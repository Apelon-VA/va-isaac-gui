package gov.va.isaac.gui.conceptview.popups;

import java.util.Optional;
import java.util.UUID;

import javafx.scene.layout.Region;
import gov.va.isaac.gui.conceptview.ConceptViewColumnType;
import gov.va.isaac.gui.conceptview.data.ConceptId;
import gov.va.isaac.gui.conceptview.data.ConceptIdType;
import gov.vha.isaac.ochre.api.component.concept.ConceptSnapshot;
import gov.vha.isaac.ochre.impl.utility.Frills;

public class PopupHelper {
	
	public static void showConceptIdList(ConceptSnapshot concept, Region popOverRegion) {
		PopupList popup = new PopupList();
		popup.setTitle("Display IDs");
		popup.setColumnTypes(new ConceptViewColumnType[] { ConceptViewColumnType.ID_TYPE, ConceptViewColumnType.ID_VALUE, ConceptViewColumnType.TIMESTAMP });
		popup.setPopOverRegion(popOverRegion);
		
		Optional<Long> sctId = Frills.getSctId(concept.getNid(), concept.getStampCoordinate());
		if (sctId.isPresent()) {
			ConceptId id = new ConceptId(ConceptIdType.SCT, sctId.get().toString(), "");
			popup.addData(id);
		}
		
		for (UUID uuid : concept.getUuidList()) {
			ConceptId id = new ConceptId(ConceptIdType.UUID, uuid.toString(), "");
			popup.addData(id);
		}
		
		popup.showPopup();
	}

}
