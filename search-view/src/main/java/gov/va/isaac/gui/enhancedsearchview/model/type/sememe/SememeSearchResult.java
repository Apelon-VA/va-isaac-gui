package gov.va.isaac.gui.enhancedsearchview.model.type.sememe;

import gov.va.isaac.search.CompositeSearchResult;
import gov.vha.isaac.ochre.api.chronicle.IdentifiedObjectLocal;
import org.ihtsdo.otf.tcc.api.concept.ConceptVersionBI;

public class SememeSearchResult extends CompositeSearchResult {

	private ConceptVersionBI assembCon;
	private String attachedData;

	public SememeSearchResult(IdentifiedObjectLocal matchingComponent, ConceptVersionBI assembCon, String attachedData) {
		super(matchingComponent, 0);

		this.assembCon = assembCon;
		this.attachedData = attachedData;
	}

	public ConceptVersionBI getAssembCon() {
		return assembCon;
	}

	public String getAttachedData() {
		return attachedData;
	}
}
