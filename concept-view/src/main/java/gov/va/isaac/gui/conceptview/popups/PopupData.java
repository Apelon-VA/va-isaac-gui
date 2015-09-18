package gov.va.isaac.gui.conceptview.popups;

public class PopupData {
	private PopupColumnType	_type;
	private String			_value;
	
	public PopupColumnType	getType()	{ return _type; }
	public String			getValue()	{ return _value; }
	
	public boolean			isType(PopupColumnType type) { return _type == type; }
	
	public PopupData(PopupColumnType columnType, String value) {
		_type = columnType;
		_value = value;
	}
}
