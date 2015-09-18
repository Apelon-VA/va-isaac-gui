package gov.va.isaac.gui.conceptview.data;

public enum ConceptIdType {
	SCT("SCT ID"),
	RXNORM("RxNorm ID"),
	LOINC("LOINC ID"),
	UUID("UUID"),
	OTHER("Other");
	
	private String niceName_;
	
	private ConceptIdType(String name) {
		niceName_ = name;
	}
	
	@Override
	public String toString()
	{
		return niceName_;
	}

}
