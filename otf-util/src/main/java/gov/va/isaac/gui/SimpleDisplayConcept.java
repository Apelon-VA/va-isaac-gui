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

import gov.va.isaac.AppContext;
import gov.va.isaac.config.profiles.UserProfileBindings;
import gov.va.isaac.util.OchreUtility;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.component.concept.ConceptSnapshot;
import java.util.function.Function;
import org.apache.commons.lang3.StringUtils;

/**
 * 
 * {@link SimpleDisplayConcept}
 *
 * A very simple concept container, useful for things like ComboBoxes, or lists
 * where we want to display workbench concepts, and still have a link to the underlying
 * concept (via the nid)
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a> 
 */
public class SimpleDisplayConcept implements Comparable<SimpleDisplayConcept>
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
	public SimpleDisplayConcept(String description, int nid, boolean ignoreChange)
	{
		description_ = description;
		nid_ = nid;
		ignoreChange_ = ignoreChange;
	}
	
	public SimpleDisplayConcept(ConceptSnapshot c)
	{
		this(c, null);
	}
	
	public SimpleDisplayConcept(ConceptSnapshot c, Function<ConceptSnapshot, String> descriptionReader)
	{
		Function<ConceptSnapshot, String> dr = (descriptionReader == null ? (conceptVersion) -> 
			{return (conceptVersion == null ? "" : OchreUtility.getDescription(conceptVersion.getConceptSequence(), 
					conceptVersion.getStampCoordinate(), conceptVersion.getLanguageCoordinate()).get());} : descriptionReader);
		description_ = dr.apply(c);
		nid_ = c == null ? 0 : c.getNid();
		ignoreChange_ = false;
	}
	
	/**
	 * @param conceptSeq nid or sequence
	 * @param descriptionReader - optional
	 */
	public SimpleDisplayConcept(Integer conceptSeq, Function<ConceptSnapshot, String> descriptionReader)
	{
		this((conceptSeq == null ? null : Get.conceptService().getSnapshot(AppContext.getService(UserProfileBindings.class).getStampCoordinate().get(),
				AppContext.getService(UserProfileBindings.class).getLanguageCoordinate().get()).getConceptSnapshot(conceptSeq)), descriptionReader);
	}
	
	public SimpleDisplayConcept(Integer conceptSeq)
	{
		this(conceptSeq, null);
	}
	
	public SimpleDisplayConcept(String description)
	{
		this(description, 0);
	}

	public SimpleDisplayConcept(String description, int nid)
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
		if (obj instanceof SimpleDisplayConcept)
		{
			SimpleDisplayConcept other = (SimpleDisplayConcept) obj;
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
	public SimpleDisplayConcept clone()
	{
		return new SimpleDisplayConcept(this.description_, this.nid_, false);
	}

	/**
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(SimpleDisplayConcept o)
	{
		return new SimpleDisplayConceptComparator().compare(this, o);
	}
}