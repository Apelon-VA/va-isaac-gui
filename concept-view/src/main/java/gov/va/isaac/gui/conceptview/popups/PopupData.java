package gov.va.isaac.gui.conceptview.popups;

import gov.va.isaac.gui.conceptview.data.Concept;
import gov.va.isaac.gui.conceptview.data.ConceptDescription;
import gov.va.isaac.gui.conceptview.data.ConceptId;
import gov.va.isaac.gui.conceptview.data.StampedItem;

public class PopupData {
	private Object _data = null;
	
	public PopupData(Object data) {
		_data = data;
		if (!isValid()) {
			_data = null;
		}
	}
	public boolean isValid() {
		return (isConceptId() || 
				isConceptDescription() ||
				isConcept() ||
				isStampedItem());
	}
	
	public boolean isConceptId() 			{ return _data instanceof ConceptId; }
	public boolean isConceptDescription()	{ return _data instanceof ConceptDescription; }
	public boolean isConcept()				{ return _data instanceof Concept; }
	public boolean isStampedItem()			{ return _data instanceof StampedItem; }
	
	public ConceptId 			getConceptId() 			{ return (isConceptId())			? (ConceptId)			_data : null; }
	public ConceptDescription	getConceptDescription()	{ return (isConceptDescription())	? (ConceptDescription)	_data : null; }
	public Concept 				getConcept()			{ return (isConcept()) 				? (Concept)				_data : null; }
	public StampedItem<?>		getStampedItem()		{ return (isStampedItem())			? (StampedItem<?>)		_data : null; }
}
