package gov.va.isaac.gui.conceptview;

public enum ConceptViewColumnType {
	STATE_CONDENSED("Status Condensed"),
	TERM("Description Term"),
	TYPE("Description Type"),
	LANGUAGE("Language - Dialect"),
	ACCEPTABILITY("Acceptability"),
	SIGNIFICANCE("Case Significance"),
	SEMEMES("Sememes"),
	
	STAMP_HEADING	("STAMP"),
	STAMP_STATE		("Status",	false,	75.0),
	STAMP_TIME		("Time",	false,	125.0),
	STAMP_AUTHOR	("Author",	true,	125.0),
	STAMP_MODULE	("Module",	true,	125.0),
	STAMP_PATH		("Path",	true,	125.0),
	
	ID_TYPE		("Type", 		false, 75.0),
	ID_VALUE	("Value",		false, 250.0),
	TIMESTAMP	("Timestamp",	false, 125.0);

	private String niceName_;
	private boolean isConcept_ = false;
	private double columnWidth_ = 0;
	
	private ConceptViewColumnType(String name) {
		this(name, false, 0);
	}
	
	private ConceptViewColumnType(String name, boolean isConcept)
	{
		this(name, isConcept, 0);
	}
	
	private ConceptViewColumnType(String name, boolean isConcept, double columnWidth) {
		niceName_ = name;
		isConcept_ = isConcept;
		columnWidth_ = columnWidth;
	}

	@Override
	public String toString()
	{
		return niceName_;
	}
	
	public boolean isConcept() {
		return isConcept_;
	}

	public double getColumnWidth() {
		return columnWidth_;
	}
}
