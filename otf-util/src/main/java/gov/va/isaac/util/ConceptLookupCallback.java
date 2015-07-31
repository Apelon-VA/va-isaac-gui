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
package gov.va.isaac.util;

import gov.vha.isaac.ochre.api.component.concept.ConceptSnapshot;

/**
 * {@link ConceptLookupCallback}
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a> 
 */
public interface ConceptLookupCallback
{
	/**
	 * Called when the lookup completes, and the concept object is ready.
	 * @param concept - the found concept (or NULL if the concept couldn't be found)
	 * @param submitTime - the time that this request was submitted
	 * @param callId - the optional arbitrary identifier passed in by the caller when the lookup began.
	 */
	public void lookupComplete(ConceptSnapshot concept, long submitTime, Integer callId);
}
