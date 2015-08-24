package gov.va.isaac.logic.treeview.nodes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import javafx.scene.layout.Region;
import javafx.scene.control.ContextMenu;
import gov.va.isaac.util.CommonMenuBuilderI;
import gov.va.isaac.util.CommonMenus;
import gov.va.isaac.util.CommonMenusNIdProvider;
import gov.va.isaac.util.CommonMenus.CommonMenuItem;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.chronicle.LatestVersion;
import gov.vha.isaac.ochre.api.component.concept.ConceptChronology;
import gov.vha.isaac.ochre.api.component.concept.ConceptVersion;
import gov.vha.isaac.ochre.api.component.sememe.version.DescriptionSememe;
import gov.vha.isaac.ochre.api.coordinate.LanguageCoordinate;
import gov.vha.isaac.ochre.api.coordinate.StampCoordinate;

public class TreeNodeFxNodeUtils {
	private TreeNodeFxNodeUtils() {}

	static void addContextMenu(AbstractTreeNodeFxNodeWithConcept treeNodeFxNode) {
		ContextMenu cm = new ContextMenu();

		// Add a Menus item.

		CommonMenuBuilderI builder = CommonMenus.getDefaultMenuBuilder();

		CommonMenus.addCommonMenus(cm, builder, new CommonMenusNIdProvider()
		{
			@Override
			public Collection<Integer> getNIds()
			{
				List<Integer> nidList = new ArrayList<>();
				nidList.add(treeNodeFxNode.getConceptId());
				return nidList;
			}
		}); 

		treeNodeFxNode.setContextMenu(cm);
	}

	public static Function<Integer, String> newDescriptionRenderer(StampCoordinate<? extends StampCoordinate<?>> stampCoordinate, LanguageCoordinate languageCoordinate) {
		return (conceptId) -> getDescription(conceptId, stampCoordinate, languageCoordinate);
	}
	public static <T extends DescriptionSememe<T>> String getDescription(int conceptId, StampCoordinate<? extends StampCoordinate<?>> stampCoordinate, LanguageCoordinate languageCoordinate) {
		Optional<LatestVersion<T>> opt = Get.conceptService().getSnapshot(stampCoordinate, languageCoordinate).getDescriptionOptional(conceptId);

		return opt.isPresent() ? opt.get().value().getText() : "No description found for concept (id=" + conceptId + ", uuid=" + Get.identifierService().getUuidPrimordialFromConceptSequence(conceptId) + ")";
	}
}
