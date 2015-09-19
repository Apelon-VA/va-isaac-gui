package gov.va.isaac.gui.conceptview.data;

import gov.va.isaac.util.Utility;
import gov.vha.isaac.metadata.source.IsaacMetadataAuxiliaryBinding;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.chronicle.LatestVersion;
import gov.vha.isaac.ochre.api.component.concept.ConceptChronology;
import gov.vha.isaac.ochre.api.component.concept.ConceptVersion;
import gov.vha.isaac.ochre.api.component.sememe.SememeChronology;
import gov.vha.isaac.ochre.api.component.sememe.version.DescriptionSememe;
import gov.vha.isaac.ochre.api.coordinate.StampCoordinate;
import gov.vha.isaac.ochre.impl.utility.Frills;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Concept extends StampedItem<ConceptVersion<?>> {
	private static final Logger LOG = LoggerFactory.getLogger(Concept.class);
	
	private boolean _hasSememes = false;
	
	@SuppressWarnings("rawtypes")
	public static ConceptVersion extractDescription(
			ConceptChronology<? extends ConceptVersion> conceptChronology,
			StampCoordinate stampCoordinate) {
		ConceptVersion concept = null;
		Optional optDS = ((ConceptChronology)conceptChronology).getLatestVersion(ConceptVersion.class, stampCoordinate);
		
		if (optDS.isPresent()) {
			LatestVersion<ConceptVersion> lv = (LatestVersion) optDS.get();
			concept = (ConceptVersion) lv.value();
		}
		
		return concept;
	}

	public static ObservableList<Concept> makeConceptVersionList(
			ConceptChronology<? extends ConceptVersion<?>> conceptChronology)
	{
		ObservableList<Concept> conceptVersionList = FXCollections.observableArrayList();
		
		for (ConceptVersion<?> conceptVersion : conceptChronology.getVersionList())
		{
			Concept concept = new Concept(conceptVersion);
			conceptVersionList.add(concept);
		}
		
		return conceptVersionList;
	}

	public Concept(ConceptVersion<?> concept)  
	{
		readStampDetails(concept);
	}
}
