package gov.va.isaac.gui.conceptview.data;

import javafx.beans.property.SimpleStringProperty;

public class ConceptId {

	private ConceptIdType _type = ConceptIdType.OTHER;

	private final SimpleStringProperty typeSSP 			= new SimpleStringProperty("-");
	private final SimpleStringProperty valueSSP 		= new SimpleStringProperty("-");
	private final SimpleStringProperty timestampSSP 	= new SimpleStringProperty("-");

	public ConceptIdType getType()		{ return _type; }
	
	public SimpleStringProperty getTypeProperty()			{ return typeSSP; }
	public SimpleStringProperty getValueProperty()			{ return valueSSP; }
	public SimpleStringProperty getTimestampProperty()		{ return timestampSSP; }
	
	public String getTypeName()			{ return typeSSP.get(); }
	public String getValue()			{ return valueSSP.get(); }
	public String getTimestamp()		{ return timestampSSP.get(); }
	
	public ConceptId(ConceptIdType type, String value, String timestamp) {
		_type = type;
		typeSSP.set(_type.toString());
		valueSSP.set(value);
		timestampSSP.set(timestamp);
	}
}
