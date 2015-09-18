package gov.va.isaac.gui.conceptview.popups;

public enum PopupDataType {
	SCT_ID("SCT ID"),
	UUID("UUID"),
	
	OTHER("Other");
	
	private String niceName_;
	
	private PopupDataType(String name) {
		niceName_ = name;
	}
	
	@Override
	public String toString()
	{
		return niceName_;
	}
	
}
