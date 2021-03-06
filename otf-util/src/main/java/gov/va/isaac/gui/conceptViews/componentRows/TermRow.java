package gov.va.isaac.gui.conceptViews.componentRows;

import gov.va.isaac.gui.conceptViews.helpers.ConceptViewerLabelHelper;
import gov.va.isaac.util.OTFUtility;
import gov.va.isaac.util.OchreUtility;
import org.ihtsdo.otf.tcc.api.description.DescriptionVersionBI;
import org.ihtsdo.otf.tcc.api.metadata.binding.SnomedMetadataRf2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class TermRow extends Row {
	int counter = 0;
	static protected String prefTermTypeStr = null;
	static protected int prefTermTypeNid = 0;
	protected final Logger LOG = LoggerFactory.getLogger(this.getClass());

	public TermRow(ConceptViewerLabelHelper labelHelper) {
		super(labelHelper);
		
		if (prefTermTypeStr == null) {
			prefTermTypeNid = SnomedMetadataRf2.PREFERRED_RF2.getNid();
			prefTermTypeStr = OchreUtility.getPreferredTermForConceptNid(prefTermTypeNid, null, null).get();
		}
	}
	
	protected String getBooleanValue(boolean val) {
		if (val) {
			return "true";
		} else {
			return "false";
		}
	}
	

	abstract public void addTermRow(DescriptionVersionBI<?> rel, boolean isPrefTerm);

}
