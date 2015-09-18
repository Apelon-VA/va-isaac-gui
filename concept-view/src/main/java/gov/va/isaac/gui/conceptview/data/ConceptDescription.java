package gov.va.isaac.gui.conceptview.data;

import gov.va.isaac.util.Utility;
import gov.vha.isaac.metadata.source.IsaacMetadataAuxiliaryBinding;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.chronicle.LatestVersion;
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

public class ConceptDescription extends StampedItem<DescriptionSememe<?>> {
	private static final Logger LOG = LoggerFactory.getLogger(ConceptDescription.class);

	private final SimpleStringProperty typeSSP 			= new SimpleStringProperty("-");
	private final SimpleStringProperty valueSSP 		= new SimpleStringProperty("-");
	private final SimpleStringProperty languageSSP 		= new SimpleStringProperty("-");
	private final SimpleStringProperty acceptabilitySSP = new SimpleStringProperty("-");
	private final SimpleStringProperty significanceSSP 	= new SimpleStringProperty("-");
	
	private int _acceptabilitySortValue = Integer.MAX_VALUE;
	private int _typeSortValue = Integer.MAX_VALUE;
	
	private boolean _hasSememes = false;
	
	@SuppressWarnings("rawtypes")
	public static DescriptionSememe extractDescription(
			SememeChronology<? extends DescriptionSememe> sememeChronology,
			StampCoordinate stampCoordinate) {
		DescriptionSememe description = null;
		SememeChronology sc = (SememeChronology) sememeChronology;
		Optional optDS = sc.getLatestVersion(DescriptionSememe.class, stampCoordinate);
		
		if (optDS.isPresent()) {
			LatestVersion<DescriptionSememe> lv = (LatestVersion) optDS.get();
			description = (DescriptionSememe) lv.value();
		}
		
		return description;
	}

	public static ObservableList<ConceptDescription> makeDescriptionList(
			List<? extends SememeChronology<? extends DescriptionSememe<?>>> sememeChronologyList,
			StampCoordinate stampCoordinate,
			boolean activeOnly)
	{
		ObservableList<ConceptDescription> descriptionList = FXCollections.observableArrayList();
		
		for (SememeChronology<? extends DescriptionSememe<?>> sememeChronology : sememeChronologyList)
		{
			if (activeOnly && ! sememeChronology.isLatestVersionActive(stampCoordinate)) {
				// Ignore inactive descriptions when activeOnly true
				continue;
			} else {
				DescriptionSememe<?> descSememe = extractDescription(sememeChronology, stampCoordinate);
				if (descSememe != null) 
				{
					ConceptDescription description = new ConceptDescription(descSememe);
					descriptionList.add(description);
				}
			}
		}
		
		return descriptionList;
	}
	public static ObservableList<ConceptDescription> makeDescriptionVersionList(
			SememeChronology<? extends DescriptionSememe<?>> sememeChronology)
	{
		ObservableList<ConceptDescription> descriptionVersionList = FXCollections.observableArrayList();
		
		for (DescriptionSememe<?> descriptionSememeVersion : sememeChronology.getVersionList())
		{
			ConceptDescription description = new ConceptDescription(descriptionSememeVersion);
			descriptionVersionList.add(description);
		}
		
		return descriptionVersionList;
	}

	public ConceptDescription(DescriptionSememe<?> description)  
	{
		readDescription(description);
	}
	
	private void readDescription(DescriptionSememe<?> description) 
	{
		if (description != null) 
		{
			_acceptabilitySortValue = AcceptabilitiesHelper.getAcceptabilitySortValue(description);
			_typeSortValue = (getTypeSequence() == IsaacMetadataAuxiliaryBinding.FULLY_SPECIFIED_NAME.getConceptSequence())			? 0 :
							 (getTypeSequence() == IsaacMetadataAuxiliaryBinding.SYNONYM.getConceptSequence()) 						? 1 :
							 (getTypeSequence() == IsaacMetadataAuxiliaryBinding.DEFINITION_DESCRIPTION_TYPE.getConceptSequence()) 	? 2 :
							 Integer.MAX_VALUE;
			_hasSememes = Frills.hasNestedSememe(description.getChronology());
			readStampDetails(description);

			Utility.execute(() ->
			{
				String typeName			= Get.conceptDescriptionText(description.getDescriptionTypeConceptSequence());
				String valueName		= description.getText();
				String languageName 	 = Get.conceptDescriptionText(getLanguageSequence());
				String acceptabilityName = AcceptabilitiesHelper.getFormattedAcceptabilities(description);
				String significanceName	 = Get.conceptDescriptionText(getSignificanceSequence());

				final String finalValueName = valueName;
				Platform.runLater(() -> {
					typeSSP.set(typeName);
					valueSSP.set(finalValueName);
					languageSSP.set(languageName);
					acceptabilitySSP.set(acceptabilityName);
					significanceSSP.set(significanceName);
				});
			});
		}
	}
	
	public int getSequence()				{ return getStampedVersion().getSememeSequence(); }
	public int getTypeSequence()			{ return getStampedVersion().getDescriptionTypeConceptSequence(); }
	public int getLanguageSequence()		{ return getStampedVersion().getLanguageConceptSequence(); }
	public int getAcceptabilitySequence()	{ return 0; }
	public int getSignificanceSequence()	{ return getStampedVersion().getCaseSignificanceConceptSequence(); }
	
	public SimpleStringProperty getTypeProperty()			{ return typeSSP; }
	public SimpleStringProperty getValueProperty()			{ return valueSSP; }
	public SimpleStringProperty getLanguageProperty()		{ return languageSSP; }
	public SimpleStringProperty getAcceptabilityProperty()	{ return acceptabilitySSP; }
	public SimpleStringProperty getSignificanceProperty()	{ return significanceSSP; }
	
	public String getType()				{ return typeSSP.get(); }
	public String getValue()			{ return valueSSP.get(); }
	public String getLanguage()			{ return languageSSP.get(); }
	public String getAcceptability()	{ return acceptabilitySSP.get(); }
	public String getSignificance()		{ return significanceSSP.get(); }
	
	public boolean hasSememes()				{ return _hasSememes; }
	public int getAcceptabilitySortValue()	{ return _acceptabilitySortValue; }
	public int getTypeSortValue() 			{ return _typeSortValue; }
	public int getSememesSortValue() 		{ return (_hasSememes)? Integer.MIN_VALUE : Integer.MAX_VALUE; }
	
	public static final Comparator<ConceptDescription> typeComparator = new Comparator<ConceptDescription>() {
		@Override
		public int compare(ConceptDescription o1, ConceptDescription o2) {
			return Integer.compare(o1.getTypeSortValue(), o2.getTypeSortValue());
		}
	};
	
	public static final Comparator<ConceptDescription> valueComparator = new Comparator<ConceptDescription>() {
		@Override
		public int compare(ConceptDescription o1, ConceptDescription o2) {
			return Utility.compareStringsIgnoreCase(o1.getValue(), o2.getValue());
		}
	};
	
	public static final Comparator<ConceptDescription> languageComparator = new Comparator<ConceptDescription>() {
		@Override
		public int compare(ConceptDescription o1, ConceptDescription o2) {
			return Utility.compareStringsIgnoreCase(o1.getLanguage(), o2.getLanguage());
		}
	};
	
	public static final Comparator<ConceptDescription> acceptabilityComparator = new Comparator<ConceptDescription>() {
		@Override
		public int compare(ConceptDescription o1, ConceptDescription o2) {
			return Integer.compare(o1.getAcceptabilitySortValue(), o2.getAcceptabilitySortValue());
		}
	};
	
	public static final Comparator<ConceptDescription> significanceComparator = new Comparator<ConceptDescription>() {
		@Override
		public int compare(ConceptDescription o1, ConceptDescription o2) {
			return Utility.compareStringsIgnoreCase(o1.getSignificance(), o2.getSignificance());
		}
	};
	public static final Comparator<ConceptDescription> sememesComparator = new Comparator<ConceptDescription>() {
		@Override
		public int compare(ConceptDescription o1, ConceptDescription o2) {
			return Integer.compare(o1.getSememesSortValue(), o2.getSememesSortValue());
		}
	};
}
