package gov.va.isaac.gui.conceptview.data;

import gov.va.isaac.util.Utility;
import gov.vha.isaac.metadata.source.IsaacMetadataAuxiliaryBinding;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.chronicle.LatestVersion;
import gov.vha.isaac.ochre.api.component.sememe.SememeChronology;
import gov.vha.isaac.ochre.api.component.sememe.version.DescriptionSememe;
import gov.vha.isaac.ochre.api.coordinate.StampCoordinate;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConceptDescription extends StampedItem {
	private static final Logger LOG = LoggerFactory.getLogger(ConceptDescription.class);

	private DescriptionSememe<?> _description;
	
	private final SimpleStringProperty typeSSP 			= new SimpleStringProperty("-");
	private final SimpleStringProperty valueSSP 		= new SimpleStringProperty("-");
	private final SimpleStringProperty languageSSP 		= new SimpleStringProperty("-");
	private final SimpleStringProperty acceptabilitySSP = new SimpleStringProperty("-");
	private final SimpleStringProperty significanceSSP 	= new SimpleStringProperty("-");
	
	private int _acceptabilitySortValue = Integer.MAX_VALUE;
	private int _typeSortValue = Integer.MAX_VALUE;
	
	@SuppressWarnings("rawtypes")
	public static DescriptionSememe extractDescription(SememeChronology<? extends DescriptionSememe> sememeChronology) {
		DescriptionSememe description = null;
		SememeChronology sc = (SememeChronology) sememeChronology;
		Optional optDS = sc.getLatestVersion(DescriptionSememe.class, Get.configurationService().getDefaultStampCoordinate());
		
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
				DescriptionSememe<?> descSememe = extractDescription(sememeChronology);
				if (descSememe != null) 
				{
					ConceptDescription description = new ConceptDescription(descSememe);
					descriptionList.add(description);
				}
			}
		}
		
		return descriptionList;
	}
	
	public ConceptDescription(DescriptionSememe<?> description)  
	{
		readDescription(description);
	}
	
	private void readDescription(DescriptionSememe<?> description) 
	{
		_description = description;
		if (description != null) 
		{
			_acceptabilitySortValue = AcceptabilitiesHelper.getAcceptabilitySortValue(_description);
			_typeSortValue = (getTypeSequence() == IsaacMetadataAuxiliaryBinding.FULLY_SPECIFIED_NAME.getConceptSequence())			? 0 :
							 (getTypeSequence() == IsaacMetadataAuxiliaryBinding.SYNONYM.getConceptSequence()) 						? 1 :
							 (getTypeSequence() == IsaacMetadataAuxiliaryBinding.DEFINITION_DESCRIPTION_TYPE.getConceptSequence()) 	? 2 :
							 Integer.MAX_VALUE;

			readStampDetails(description);

			Utility.execute(() ->
			{
				String typeName			= Get.conceptDescriptionText(_description.getDescriptionTypeConceptSequence());
				String valueName		= _description.getText();
				String languageName 	 = Get.conceptDescriptionText(getLanguageSequence());
				String acceptabilityName = AcceptabilitiesHelper.getFormattedAcceptabilities(_description);
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
	
	public int getSequence()				{ return _description.getSememeSequence(); }
	public int getTypeSequence()			{ return _description.getDescriptionTypeConceptSequence(); }
	public int getLanguageSequence()		{ return _description.getLanguageConceptSequence(); }
	public int getAcceptabilitySequence()	{ return 0; }
	public int getSignificanceSequence()	{ return _description.getCaseSignificanceConceptSequence(); }
	
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
	
	public int getAcceptabilitySortValue()	{ return _acceptabilitySortValue; }
	public int getTypeSortValue() 			{ return _typeSortValue; }
	
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
}
