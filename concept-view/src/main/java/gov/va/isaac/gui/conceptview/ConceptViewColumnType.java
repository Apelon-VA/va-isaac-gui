package gov.va.isaac.gui.conceptview;

public enum ConceptViewColumnType {
	STATE_CONDENSED("Status Condensed"),
	TERM("Description Term"),
	TYPE("Description Type"),
	LANGUAGE("Language - Dialect"),
	ACCEPTABILITY("Acceptability"),
	SIGNIFICANCE("Case Significance"),
	SEMEMES("Sememes"),
	
	STAMP_HEADING("STAMP"),
	STAMP_STATE("Status"),
	STAMP_TIME("Time"),
	STAMP_AUTHOR("Author",true),
	STAMP_MODULE("Module",true),
	STAMP_PATH("Path",true);
	
	private String niceName_;
	private boolean isConcept_ = false;
	
	private ConceptViewColumnType(String name) {
		this(name, false);
	}
	
	private ConceptViewColumnType(String name, boolean isConcept)
	{
		niceName_ = name;
		isConcept_ = isConcept;
	}

	@Override
	public String toString()
	{
		return niceName_;
	}
	
	public boolean isConcept() {
		return isConcept_;
	}

}
