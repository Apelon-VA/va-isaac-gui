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
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * ConceptIdProviderHelper
 * 
 * @author <a href="mailto:joel.kniaz@gmail.com">Joel Kniaz</a>
 */
package gov.va.isaac.util;

import org.ihtsdo.otf.tcc.api.concept.ConceptVersionBI;

import gov.va.isaac.gui.conceptViews.helpers.ConceptViewerHelper;
import gov.va.isaac.gui.dragAndDrop.ConceptIdProvider;

import java.util.Optional;
import java.util.UUID;

/**
 * ConceptIdProviderHelper
 * 
 * @author <a href="mailto:joel.kniaz@gmail.com">Joel Kniaz</a>
 *
 */
public class ConceptIdProviderHelper {
	private static class ConceptIdProviderBean implements ConceptIdProvider {
		private final Integer nid;
		private final UUID uuid;
		private final Optional<Long> sctId;
		
		private ConceptIdProviderBean(Integer nid, UUID uuid, Optional<Long> sctId) {
			super();
			this.nid = nid;
			this.uuid = uuid;
			this.sctId = sctId;
		}

		@Override
		public Optional<Long> getSctId() {
			return sctId;
		}
		@Override
		public UUID getUUID() {
			return uuid;
		}
		@Override
		public Integer getNid() {
			return nid;
		}
	}
	public static ConceptIdProvider getPopulatedConceptIdProvider(ConceptIdProvider idProvider) {
		Integer tmpNid = idProvider != null ? idProvider.getNid() : null;
		UUID tmpUuid = idProvider != null ? idProvider.getUUID() : null;
		Optional<Long> tmpSctId = idProvider != null ? idProvider.getSctId() : Optional.empty();

		ConceptVersionBI concept = null;
		if (tmpNid != null) {
			concept = OTFUtility.getConceptVersion(tmpNid);
			if (tmpUuid == null) {
				tmpUuid = concept != null ? concept.getPrimordialUuid() : null;
			}
			if (!tmpSctId.isPresent()) {
				if(concept != null) {
					tmpSctId = ConceptViewerHelper.getSctId(concept.getNid());
				}
			}
		} else if (tmpUuid != null) {
			concept = OTFUtility.getConceptVersion(tmpUuid);
			if (tmpNid == null) {
				tmpNid = concept != null ? concept.getConceptNid() : null;
			}
			if (!tmpSctId .isPresent()) {
				if(concept != null) {
					tmpSctId = ConceptViewerHelper.getSctId(concept.getNid());
				}
			}
		}

		return new ConceptIdProviderBean(tmpNid, tmpUuid, tmpSctId);
	}
}
