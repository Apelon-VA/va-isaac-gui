package gov.va.isaac.gui.conceptview.popups;

public enum PopupColumnType {
	TYPE		("Type", 		false),
	ID			("Value",		false),
	TIMESTAMP	("Timestamp",	false);
	
	private String niceName_;
	private boolean isConcept_ = false;
	
	private PopupColumnType(String name) {
		this(name, false);
	}
	
	private PopupColumnType(String name, boolean isConcept)
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
