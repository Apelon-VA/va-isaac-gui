package gov.va.isaac.gui.conceptview.data;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import gov.va.isaac.util.Utility;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.chronicle.LatestVersion;
import gov.vha.isaac.ochre.api.component.sememe.SememeChronology;
import gov.vha.isaac.ochre.api.component.sememe.version.DescriptionSememe;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class ConceptDescription extends StampedItem {


	private DescriptionSememe<?> _description;
	
	private final SimpleStringProperty typeSSP 			= new SimpleStringProperty("-");
	private final SimpleStringProperty valueSSP 		= new SimpleStringProperty("-");
	private final SimpleStringProperty languageSSP 		= new SimpleStringProperty("-");
	private final SimpleStringProperty acceptabilitySSP = new SimpleStringProperty("-");
	private final SimpleStringProperty significanceSSP 	= new SimpleStringProperty("-");

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

	public static ObservableList<ConceptDescription> makeDescriptionList(List<? extends SememeChronology<? extends DescriptionSememe>> sememeChronologyList)
	{
		ObservableList<ConceptDescription> descriptionList = FXCollections.observableArrayList();
		
		for (SememeChronology<? extends DescriptionSememe> sememeChronology : sememeChronologyList)
		{
			DescriptionSememe descSememe = extractDescription(sememeChronology);
			if (descSememe != null) 
			{
				ConceptDescription description = new ConceptDescription(descSememe);
				descriptionList.add(description);
			}
		}
		
		return descriptionList;
	}
	
	public ConceptDescription(DescriptionSememe description)  
	{
		readDescription(description);
	}
	
	private void readDescription(DescriptionSememe description) 
	{
		_description = description;
		if (description != null) 
		{
			readStampDetails(description);

			Utility.execute(() ->
			{
				String typeName			 = Get.conceptDescriptionText(getAuthorSequence());
				String valueName 		 = Get.conceptDescriptionText(_description.getSememeSequence());
				String languageName 	 = Get.conceptDescriptionText(getLanguageSequence());
				String acceptabilityName = "";
				String significanceName	 = Get.conceptDescriptionText(getSignificanceSequence());
				
				
				Platform.runLater(() -> {
					typeSSP.set(typeName);
					valueSSP.set(valueName);
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
	
	
	public static final Comparator<ConceptDescription> typeComparator = new Comparator<ConceptDescription>() {
		@Override
		public int compare(ConceptDescription o1, ConceptDescription o2) {
			return Utility.compareStringsIgnoreCase(o1.getType(), o2.getType());
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
			return Utility.compareStringsIgnoreCase(o1.getAcceptability(), o2.getAcceptability());
		}
	};
	
	public static final Comparator<ConceptDescription> significanceComparator = new Comparator<ConceptDescription>() {
		@Override
		public int compare(ConceptDescription o1, ConceptDescription o2) {
			return Utility.compareStringsIgnoreCase(o1.getSignificance(), o2.getSignificance());
		}
	};
	

}
