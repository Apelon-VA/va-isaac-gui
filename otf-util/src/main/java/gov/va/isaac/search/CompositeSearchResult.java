/**
 * Copyright Notice
 * 
 * This is a work of the U.S. Government and is not subject to copyright
 * protection in the United States. Foreign copyrights may apply.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *	 http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gov.va.isaac.search;

import gov.va.isaac.util.OTFUtility;
import gov.vha.isaac.metadata.coordinates.ViewCoordinates;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.ihtsdo.otf.tcc.api.chronicle.ComponentChronicleBI;
import org.ihtsdo.otf.tcc.api.chronicle.ComponentVersionBI;
import org.ihtsdo.otf.tcc.api.concept.ConceptVersionBI;
import org.ihtsdo.otf.tcc.api.coordinate.ViewCoordinate;
import org.ihtsdo.otf.tcc.api.description.DescriptionAnalogBI;

/**
 * Encapsulates a data store search result.
 * <p>
 * Logic has been mostly copied from LEGO {@code SnomedSearchResult}.
 *
 * @author ocarlsen
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public class CompositeSearchResult {

	private ConceptVersionBI containingConcept = null;
	private final Set<ComponentVersionBI> matchingComponents = new HashSet<>();
	private int matchingComponentNid_;
	private float bestScore; // best score, rather than score, as multiple matches may go into a SearchResult
	
	private static ViewCoordinate vc;
	{
		try
		{
			vc = ViewCoordinates.getDevelopmentStatedLatest();
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	public CompositeSearchResult(ComponentVersionBI matchingComponent, float score) {
		this.matchingComponents.add(matchingComponent);
		this.bestScore = score;
		//matchingComponent may be null, if the match is not on our view path...
		if (matchingComponent == null) {
			throw new RuntimeException("Please call the constructor that takes a nid, if matchingComponent is null...");
		} else {
			this.matchingComponentNid_ = matchingComponent.getNid();
		}
		this.containingConcept = OTFUtility.getConceptVersion(matchingComponent.getAssociatedConceptNid());

                //TODO - we need to evaluate / design proper behavior for how view coordinate should work with search
		//default back to just using this for the moment, rather than what OTFUtility says.
		//this.containingConcept = OTFUtility.getConceptVersion(matchingComponent.getConceptNid(), vc);
	}
	public CompositeSearchResult(int matchingComponentNid, float score) {
		this.bestScore = score;
		//matchingComponent may be null, if the match is not on our view path...
		this.containingConcept = null;
		this.matchingComponentNid_ = matchingComponentNid;
		
	}
	
	protected void adjustScore(float newScore) {
		bestScore = newScore;
	}

	public float getBestScore() {
		return bestScore;
	}
	
	/**
	 * This may return null, if the concept and/or matching component was not on the path
	 */
	public ConceptVersionBI getContainingConcept() {
		return containingConcept;
	}

	/**
	 * A convenience method to get string values from the matching Components
	 */
	public List<String> getMatchingStrings() {
		ArrayList<String> strings = new ArrayList<>();
		if (matchingComponents.size() == 0)
		{
			if (containingConcept == null)
			{
				strings.add("Match to NID (not on path):" + matchingComponentNid_);
			}
			else
			{
				throw new RuntimeException("Unexpected");
			}
		}
		for (ComponentVersionBI cc : matchingComponents)
		{
			if (cc instanceof DescriptionAnalogBI)
			{
				strings.add(((DescriptionAnalogBI<?>) cc).getText());
			}
			else if (cc instanceof ConceptVersionBI)
			{
				//This means they matched on a UUID or other ID lookup.
				//Return UUID for now - matches on other ID types will be handled differently 
				//in the near future - so ignore the SCTID case for now.
				strings.add(cc.getPrimordialUuid().toString());
			}
			else
			{
				strings.add("ERROR: No string extractor available for " + cc.getClass().getName());
			}
		}
		return strings;
	}

	public Set<ComponentVersionBI> getMatchingComponents() {
		return matchingComponents;
	}
	
	/**
	 * Convenience method to return a filtered list of matchingComponents such that it only returns
	 * Description type components
	 */
	public Set<DescriptionAnalogBI<?>> getMatchingDescriptionComponents() {
		Set<DescriptionAnalogBI<?>> setToReturn = new HashSet<>();
		for (ComponentVersionBI comp : matchingComponents) {
			if (comp instanceof DescriptionAnalogBI) {
				setToReturn.add((DescriptionAnalogBI<?>)comp);
			}
		}
		
		return Collections.unmodifiableSet(setToReturn);
	}
	
	protected void merge(CompositeSearchResult other)
	{
		if (containingConcept.getNid() !=  other.containingConcept.getNid())
		{
			throw new RuntimeException("Unmergeable!");
		}
		if (other.bestScore > bestScore)
		{
			bestScore = other.bestScore;
		}
		matchingComponents.addAll(other.getMatchingComponents());
	}
	
	public String toShortString() {
		StringBuilder builder = new StringBuilder();
		builder.append("CompositeSearchResult [containingConcept=");
		builder.append(containingConcept != null ? containingConcept.getNid() : null);
		
		if (matchingComponentNid_ != 0) {
			builder.append(", matchingComponentNid_=");
			builder.append(matchingComponentNid_);
		}

		builder.append(", bestScore=");
		builder.append(bestScore);

		if (matchingComponents != null && matchingComponents.size() > 0) {
			builder.append(", matchingComponents=");
			List<Integer> matchingComponentNids = new ArrayList<>();
			for (ComponentVersionBI matchingComponent : matchingComponents) {
				matchingComponentNids.add(matchingComponent != null ? matchingComponent.getNid() : null);
			}
			builder.append(matchingComponentNids);
		}

		builder.append("]");
		return builder.toString();
	}

	public String toStringWithDescriptions() {
		StringBuilder builder = new StringBuilder();
		builder.append("CompositeSearchResult [containingConcept=");
		String containingConceptDesc = null;
		if (containingConcept != null) {
			try {
				containingConceptDesc = OTFUtility.getDescriptionIfConceptExists(containingConcept.getConceptNid());
			} catch (Exception e) {
				containingConceptDesc = "{nid=" + containingConcept.getConceptNid() + "}";
			}
		}
		builder.append(containingConceptDesc);

		builder.append(", matchingComponentNid_=");
		builder.append(matchingComponentNid_);
		
		String matchingComponentDesc = null;
		if (matchingComponentNid_ != 0) {
			try {
				ComponentChronicleBI<?> cc = OTFUtility.getComponentChronicle(matchingComponentNid_);
				matchingComponentDesc = cc.toUserString();
			} catch (Exception e) {
				// NOOP
			}
		}
		if (matchingComponentDesc != null) {
			builder.append(", matchingComponent=");
			builder.append(matchingComponentDesc);
		}

		builder.append(", bestScore=");
		builder.append(bestScore);

		builder.append(", numMatchingComponents=");
		List<Integer> matchingComponentNids = new ArrayList<>();
		for (ComponentVersionBI matchingComponent : getMatchingComponents()) {
			matchingComponentNids.add(matchingComponent != null ? matchingComponent.getNid() : null);
		}
		builder.append(matchingComponentNids);

		builder.append("]");
		return builder.toString();
	}
	public String toLongString() {
		StringBuilder builder = new StringBuilder();
		builder.append("CompositeSearchResult [containingConcept=");
		builder.append(containingConcept);
		builder.append(", matchingComponentNid_=");
		builder.append(matchingComponentNid_);
		builder.append(", bestScore=");
		builder.append(bestScore);
		builder.append(", getMatchingComponents()=");
		List<String> matchingComponentDescs = new ArrayList<>();
		for (ComponentVersionBI matchingComponent : getMatchingComponents()) {
			matchingComponentDescs.add(matchingComponent != null ? matchingComponent.toUserString() : null);
		}
		builder.append(matchingComponentDescs);
		
		builder.append("]");
		return builder.toString();
	}
}
