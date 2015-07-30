package gov.va.isaac.gui;

import org.ihtsdo.otf.tcc.api.concept.ConceptVersionBI;

/**
 * An extension of SimpleDisplayConcept that can be used when you need to change
 * the description shown in combos and lists.
 * 
 * @author dtriglianos
 *
 */
public class RenameableDisplayConcept extends SimpleDisplayConcept {

	//TODO Dan question - the existence of this class makes me feel something has gone wrong.... when is this ever a use case?
	public void setDescription(String description) {
		this.description_ = description;
	}

	public RenameableDisplayConcept(String description, int nid, boolean ignoreChange) {
		super(description, nid, ignoreChange);
	}

	public RenameableDisplayConcept(ConceptVersionBI c) {
		super(c);
	}

	public RenameableDisplayConcept(String description) {
		super(description);
	}

	public RenameableDisplayConcept(String description, int nid) {
		super(description, nid);
	}

}
