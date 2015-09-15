package gov.va.isaac.interfaces.gui.views.commonFunctionality;

import gov.vha.isaac.ochre.api.coordinate.TaxonomyCoordinate;
import gov.vha.isaac.ochre.api.observable.coordinate.ObservableTaxonomyCoordinate;

import java.util.UUID;

public interface LogicalExpressionTreeGraphViewBaseViewI {

	/**
	 * @return Integer conceptId set by user
	 */
	public Integer getConceptId();
	
	/**
	 * Tell this view to display the Logical Expression tree graph for a particular concept 
	 * 
	 * @param conceptId - the id of the concept to graph.
	 */
	public abstract void setConcept(int componentNid);

	public abstract void setConcept(
			TaxonomyCoordinate taxonomyCoordinate,
			int componentNid);

	public abstract void setConcept(
			ObservableTaxonomyCoordinate taxonomyCoordinate, int componentNid);

	/**
	 * Tell this view to display the Logical Expression tree graph for a particular concept 
	 * 
	 * @param conceptUuid - the UUID of the concept to graph.
	 */
	public abstract void setConcept(UUID uuid);

	public abstract void setConcept(
			TaxonomyCoordinate taxonomyCoordinate,
			UUID uuid);

	public abstract void setConcept(
			ObservableTaxonomyCoordinate taxonomyCoordinate, UUID uuid);
	
	/**
	 * Clear display and unset conceptId
	 */
	public void clear();
}