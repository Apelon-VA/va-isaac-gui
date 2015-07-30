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
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gov.va.isaac.gui;

import gov.va.isaac.util.OCHREUtility;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.component.concept.ConceptChronology;
import gov.vha.isaac.ochre.api.component.concept.ConceptSnapshot;
import gov.vha.isaac.ochre.api.component.concept.ConceptVersion;

import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;

/**
 * 
 * {@link OCHRESimpleDisplayConcept}
 *
 * A very simple concept container, useful for things like ComboBoxes, or lists
 * where we want to display workbench concepts, and still have a link to the underlying
 * concept (via the nid)
 *
 * @author <a href="mailto:joel.kniaz@gmail.com">Joel Kniaz</a> 
 */
public class OCHRESimpleDisplayConcept implements Comparable<OCHRESimpleDisplayConcept>
{
	protected String description_;
	private int nid_;
	private boolean ignoreChange_ = false;
	private boolean uncommitted_ = false;
	
	/**
	 * 
	 * @param description
	 * @param nid
	 * @param ignoreChange - typically used to allow a changeListener to ignore a change.  
	 * See {@link #shouldIgnoreChange()}
	 */
	public OCHRESimpleDisplayConcept(String description, int nid, boolean ignoreChange)
	{
		description_ = description;
		nid_ = nid;
		ignoreChange_ = ignoreChange;
	}
	
	public OCHRESimpleDisplayConcept(ConceptSnapshot c)
	{
		this(c, null);
	}
	
	public OCHRESimpleDisplayConcept(ConceptSnapshot c, Function<ConceptSnapshot, String> descriptionReader)
	{
		Function<ConceptSnapshot, String> dr = (descriptionReader == null ? (conceptVersion) -> 
			{return (conceptVersion == null ? "" : OCHREUtility.getDescription(conceptVersion.getChronology(), conceptVersion.getLanguageCoordinate(), conceptVersion.getStampCoordinate()));} : descriptionReader);
		description_ = dr.apply(c);
		nid_ = c == null ? 0 : c.getNid();
		ignoreChange_ = false;
	}
	
	public OCHRESimpleDisplayConcept(ConceptChronology c, Function<ConceptSnapshot, String> descriptionReader)
	{
		this((c == null ? null : Get.conceptSnapshot().getConceptSnapshot(c.getNid())), descriptionReader);
	}
	
	public OCHRESimpleDisplayConcept(ConceptChronology c)
	{
		this((c == null ? null : Get.conceptSnapshot().getConceptSnapshot(c.getNid())), null);
	}
	
	public OCHRESimpleDisplayConcept(ConceptVersion c)
	{
		this((c == null ? null : Get.conceptSnapshot().getConceptSnapshot(c.getChronology().getNid())), null);
	}
	
	public OCHRESimpleDisplayConcept(String description)
	{
		this(description, 0);
	}

	public OCHRESimpleDisplayConcept(String description, int nid)
	{
		this(description, nid, false);
	}

	public String getDescription()
	{
		return description_;
	}

	public int getNid()
	{
		return nid_;
	}

	public void setUncommitted(boolean val) {
		uncommitted_ = val;
	}
	
	public boolean isUncommitted() {
		return uncommitted_;
	}
	
	protected void setNid(int nid)
	{
		nid_ = nid;
	}
	/**
	 * Note - this can only be read once - if it returns true after the first call, 
	 * it resets itself to false for every subsequent call.  It will only return 
	 * true if this item was constructed with the ignoreChange property set to true.
	 * If not, it will always return false.
	 */
	public synchronized boolean shouldIgnoreChange()
	{
		boolean temp = ignoreChange_;
		ignoreChange_ = false;
		return temp;
	}
	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((description_ == null) ? 0 : description_.hashCode());
		result = prime * result + nid_;
		return result;
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj)
	{
		if (obj == null)
		{
			return false;
		}
		if (obj instanceof OCHRESimpleDisplayConcept)
		{
			OCHRESimpleDisplayConcept other = (OCHRESimpleDisplayConcept) obj;
			return nid_ == other.nid_ && StringUtils.equals(description_, other.description_);
		}
		return false;
	}

	@Override
	public String toString()
	{
		return description_;
	}
	
	@Override
	public OCHRESimpleDisplayConcept clone()
	{
		return new OCHRESimpleDisplayConcept(this.description_, this.nid_, false);
	}

	/**
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(OCHRESimpleDisplayConcept o)
	{
		return new OCHRESimpleDisplayConceptComparator().compare(this, o);
	}
}