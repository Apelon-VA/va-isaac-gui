package gov.va.isaac.gui.conceptview.popups;

import java.util.Optional;
import java.util.UUID;

import javafx.scene.layout.Region;
import gov.vha.isaac.ochre.api.component.concept.ConceptSnapshot;
import gov.vha.isaac.ochre.impl.utility.Frills;

public class PopupHelper {
	protected static final String HEADER_TYPE 		= "Type";
	protected static final String HEADER_VALUE		= "Value";
	protected static final String HEADER_TIMESTAMP	= "Timestamp";
	
	public static void showConceptIdList(ConceptSnapshot concept, Region popOverRegion) {
		PopupList popup = new PopupList();
		popup.setTitle("Display IDs");
		popup.setHeaders(new String[]{HEADER_TYPE, HEADER_VALUE, HEADER_TIMESTAMP});
		popup.setPopOverRegion(popOverRegion);
		
		Optional<Long> sctId = Frills.getSctId(concept.getNid(), concept.getStampCoordinate());
		if (sctId.isPresent()) {
			popup.addValues(new String[]{"SCT ID", sctId.get().toString(), ""});
		}
		
		for (UUID uuid : concept.getUuidList()) {
			popup.addValues(new String[]{"UUID", uuid.toString(), ""});
		}
		
		popup.showPopup();
	}

}
