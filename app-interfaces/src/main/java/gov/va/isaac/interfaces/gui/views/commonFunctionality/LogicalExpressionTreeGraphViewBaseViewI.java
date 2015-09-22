package gov.va.isaac.interfaces.gui.views.commonFunctionality;

import gov.vha.isaac.ochre.api.component.sememe.version.LogicGraphSememe;
import gov.vha.isaac.ochre.api.coordinate.TaxonomyCoordinate;
import gov.vha.isaac.ochre.api.observable.coordinate.ObservableTaxonomyCoordinate;

import java.util.UUID;

public interface LogicalExpressionTreeGraphViewBaseViewI {

	/**
	 * @return Integer required conceptId set by user
	 */
	public Integer getConceptId();
	
	/**
	 * Return the cached LogicGraphSememe value.  Only valid after setConcept() called.
	 * 
	 * @return cached LogicGraphSememe
	 */
	public LogicGraphSememe<?> getLogicGraphSememe();

	/**
	 * Tell this view to display the Logical Expression tree graph for a particular concept
	 * Always displays latest LogicGraph sememe version.
	 * 
	 * @param conceptId - id of the concept to graph.
	 */
	public abstract void setConcept(int conceptNid);
	
	/**
	 * Tell this view to display the Logical Expression tree graph for a particular concept
	 * Always displays the LogicGraph sememe version corresponding to the passed sememeSequence
	 * 
	 * @param conceptNid id of the concept to graph.
	 * @param sememeSequence sememe sequence of the version of the sememe to graph.
	 */
	public abstract void setConcept(int conceptNid, int sememeSequence);

	public void setConcept(UUID uuid, int sememeSequence);
	
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