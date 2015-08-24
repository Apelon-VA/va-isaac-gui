package gov.va.isaac.interfaces.gui.views.commonFunctionality;

import gov.va.isaac.interfaces.gui.views.EmbeddableViewI;
import gov.va.isaac.interfaces.gui.views.PopupViewI;

import java.util.UUID;

import org.jvnet.hk2.annotations.Contract;
/**
 * {@link ConceptViewI}
 * 
 * An interface that requests a popup window that displays the details of a concept.
 *
 * @author <a href="mailto:dtriglianos@apelon.com">Dave Triglianos</a> 
 */
@Contract
public interface ConceptView2I extends PopupViewI, EmbeddableViewI {

	public abstract Integer getConcept();

	public abstract void setConcept(int conceptId);

	public abstract void setConcept(UUID conceptUuid);
}