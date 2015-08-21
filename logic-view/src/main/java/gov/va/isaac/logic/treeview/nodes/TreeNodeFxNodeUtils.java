package gov.va.isaac.logic.treeview.nodes;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.chronicle.LatestVersion;
import gov.vha.isaac.ochre.api.component.sememe.version.DescriptionSememe;
import gov.vha.isaac.ochre.api.coordinate.LanguageCoordinate;
import gov.vha.isaac.ochre.api.coordinate.StampCoordinate;

public class TreeNodeFxNodeUtils {
	private TreeNodeFxNodeUtils() {}

	public static Function<Integer, String> newDescriptionRenderer(StampCoordinate<? extends StampCoordinate<?>> stampCoordinate, LanguageCoordinate languageCoordinate) {
		return (conceptId) -> getDescription(conceptId, stampCoordinate, languageCoordinate);
	}
	public static <T extends DescriptionSememe<T>> String getDescription(int conceptId, StampCoordinate<? extends StampCoordinate<?>> stampCoordinate, LanguageCoordinate languageCoordinate) {
		Optional<LatestVersion<T>> opt = Get.conceptService().getSnapshot(stampCoordinate, languageCoordinate).getDescriptionOptional(conceptId);
		
		return opt.isPresent() ? opt.get().value().getText() : "No description found for concept (id=" + conceptId + ", uuid=" + Get.identifierService().getUuidPrimordialFromConceptSequence(conceptId) + ")";
	}
}
