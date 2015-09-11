package gov.va.isaac.gui.conceptview.data;

import gov.vha.isaac.metadata.coordinates.StampCoordinates;
import gov.vha.isaac.metadata.source.IsaacMetadataAuxiliaryBinding;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.component.sememe.version.DescriptionSememe;
import gov.vha.isaac.ochre.impl.lang.LanguageCode;
import gov.vha.isaac.ochre.impl.utility.Frills;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

public class AcceptabilitiesHelper {
	private AcceptabilitiesHelper() {}
	
	static String getFormattedAcceptabilities(DescriptionSememe<?> description) {
		return getAcceptabilities(description, FORMATTED_DIALECT_SEQUENCE_RENDERER, FORMATTED_ACCEPTABILITY_NID_RENDERER);
	}
	static String getDefaultAcceptabilities(DescriptionSememe<?> description) {
		return getAcceptabilities(description, null, null);
	}

	final static Function<Integer, String> DEFAULT_DIALECT_SEQUENCE_RENDERER = new Function<Integer, String>() {
		@Override
		public String apply(Integer t) {
			return Get.conceptDescriptionText(t);
		}
	};
	final static Function<Integer, String> FORMATTED_DIALECT_SEQUENCE_RENDERER = new Function<Integer, String>() {
		@Override
		public String apply(Integer t) {
			String dialectDesc = null;
			Optional<UUID> uuidForDialectSequence = Get.identifierService().getUuidPrimordialFromConceptSequence(t);
			if (uuidForDialectSequence.isPresent()) {
				LanguageCode code = LanguageCode.safeValueOf(uuidForDialectSequence.get());
				if (code != null) {
					dialectDesc = code.getFormatedLanguageCode();
				}
			}
			
			if (dialectDesc == null) {
				dialectDesc = Get.conceptDescriptionText(t);
			}

			return dialectDesc;
		}
	};
	final static Function<Integer, String> DEFAULT_ACCEPTABILITY_NID_RENDERER = new Function<Integer, String>() {
		@Override
		public String apply(Integer t) {
			return Get.conceptDescriptionText(t);
		}
	};
	final static Function<Integer, String> FORMATTED_ACCEPTABILITY_NID_RENDERER = new Function<Integer, String>() {
		@Override
		public String apply(Integer t) {
			if (t == IsaacMetadataAuxiliaryBinding.PREFERRED.getNid()) {
				return "PT";
			} else if (t == IsaacMetadataAuxiliaryBinding.ACCEPTABLE.getNid()) {
				return "AC";
			} else {
//				String error = "Unexpected acceptability NID " + t + "(" + Get.conceptDescriptionText(t) + ")";
//				throw new RuntimeException(error);
				return Get.conceptDescriptionText(t);
			}
		}
	};
	static int getAcceptabilitySortValue(DescriptionSememe<?> description) {
		int rval = 100;
		Map<Integer, Integer> dialectSequenceToAcceptabilityNidMap = Frills.getAcceptabilities(description.getNid(), StampCoordinates.getDevelopmentLatest());
		for (Map.Entry<Integer, Integer> entry : dialectSequenceToAcceptabilityNidMap.entrySet()) {
			if (entry.getKey() != null && entry.getValue() != null) {
				if (entry.getValue() == IsaacMetadataAuxiliaryBinding.PREFERRED.getNid()) {
					rval = 0;
					break;
				} else if (entry.getValue() == IsaacMetadataAuxiliaryBinding.ACCEPTABLE.getNid()) {
					rval = 1;
				}
			}
		}
		return rval;
	}
	static String getAcceptabilities(DescriptionSememe<?> description, Function<Integer, String> passedDialectSequenceRenderer, Function<Integer, String> passedAcceptabilityRenderer) {
		final Function<Integer, String> acceptabilityRenderer = passedAcceptabilityRenderer != null ? passedAcceptabilityRenderer : DEFAULT_ACCEPTABILITY_NID_RENDERER;
		final Function<Integer, String> dialectSequenceRenderer = passedDialectSequenceRenderer != null ? passedDialectSequenceRenderer : DEFAULT_DIALECT_SEQUENCE_RENDERER;
		
		Map<Integer, Integer> dialectSequenceToAcceptabilityNidMap = Frills.getAcceptabilities(description.getNid(), StampCoordinates.getDevelopmentLatest());
		
		StringBuilder builder = new StringBuilder();
		for (Map.Entry<Integer, Integer> entry : dialectSequenceToAcceptabilityNidMap.entrySet()) {
			if (entry.getKey() != null && entry.getValue() != null) {
				if (builder.toString().length() > 0) {
					builder.append(", ");
				}
				
				builder.append(dialectSequenceRenderer.apply(entry.getKey()) + ":" + acceptabilityRenderer.apply(entry.getValue()));
			}
		}
		
		return builder.toString();
	}
}
