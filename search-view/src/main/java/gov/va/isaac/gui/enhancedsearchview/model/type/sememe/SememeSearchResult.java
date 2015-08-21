package gov.va.isaac.gui.enhancedsearchview.model.type.sememe;

import gov.va.isaac.search.CompositeSearchResult;
import gov.vha.isaac.ochre.api.chronicle.IdentifiedObjectLocal;

public class SememeSearchResult extends CompositeSearchResult {

	private int assembSequence;
	private String attachedData;

	public SememeSearchResult(IdentifiedObjectLocal matchingComponent, int assembSequence, String attachedData) {
		super(matchingComponent, 0);

		this.assembSequence = assembSequence;
		this.attachedData = attachedData;
	}

	public int getAssembSequence() {
		return assembSequence;
	}

	public String getAttachedData() {
		return attachedData;
	}
}
