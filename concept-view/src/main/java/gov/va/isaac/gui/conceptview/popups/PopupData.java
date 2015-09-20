package gov.va.isaac.gui.conceptview.popups;

import gov.va.isaac.gui.conceptview.data.ConceptDescription;
import gov.va.isaac.gui.conceptview.data.ConceptId;

public class PopupData {
	private ConceptId 			_idData = null;
	private ConceptDescription	_descData = null;
	//private ConceptSnaphot		_conceptDate = null;
	
	public PopupData(ConceptId data) {
		_idData = data;
	}
	public PopupData(ConceptDescription data) {
		_descData = data;
	}
	
	public boolean isValid() {
		return (isConceptId() || isConceptDescription());
	}
	
	public boolean isConceptId() 			{ return _idData != null; }
	public boolean isConceptDescription()	{ return _descData != null; }
	
	
	public ConceptId 		  getConceptId() 			{ return _idData; }
	public ConceptDescription getConceptDescription()	{ return _descData; }
}
