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
 * DynamicRefexHelper
 * 
 * @author <a href="mailto:joel.kniaz@gmail.com">Joel Kniaz</a>
 */
package gov.va.isaac.gui.enhancedsearchview;

import java.io.IOException;
import java.util.Collection;
import java.util.UUID;
import org.ihtsdo.otf.tcc.api.concept.ConceptChronicleBI;
import org.ihtsdo.otf.tcc.api.concept.ConceptVersionBI;
import org.ihtsdo.otf.tcc.api.contradiction.ContradictionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import gov.va.isaac.util.OTFUtility;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeColumnInfo;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeDataBI;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeDataType;

/**
 * DynamicRefexHelper
 * 
 * @author <a href="mailto:joel.kniaz@gmail.com">Joel Kniaz</a>
 *
 */
public class DynamicRefexHelper {
	private static final Logger LOG = LoggerFactory.getLogger(DynamicRefexHelper.class);

	/**
	 * Do not instantiate
	 */
	private DynamicRefexHelper() {}
	
	public static void displayDynamicRefexes(ConceptChronicleBI conceptContainingRefexes) {
		OTFUtility.getConceptVersion(conceptContainingRefexes.getConceptNid());
	}
	public static void displayDynamicRefexes(ConceptVersionBI conceptContainingRefexes) {
		String desc = null;
		try {
			desc = OTFUtility.getDescription(conceptContainingRefexes);
			for (DynamicSememeVersionBI<?> refex : conceptContainingRefexes.getRefexesDynamicActive(OTFUtility.getViewCoordinate())) {
				DynamicRefexHelper.displayDynamicRefex(refex);
			}
		} catch (IOException e) {
			LOG.warn("Failed diplaying sememes in concept " + (desc != null ? desc : "") + ". Caught " + e.getClass().getName() + " \"" + e.getLocalizedMessage() + "\"");
			e.printStackTrace();
		}
	}
	public static void displayDynamicRefex(DynamicSememeVersionBI<?> refex) {
		displayDynamicRefex(refex, 0);
	}
	public static void displayDynamicRefex(DynamicSememeVersionBI<?> refex, int depth) {
		String indent = "";
		
		for (int i = 0; i < depth; ++i) {
			indent += "\t";
		}
		
		DynamicSememeUsageDescription dud = null;
		try {
			dud = refex.getDynamicSememeUsageDescription();
		} catch (IOException | ContradictionException e) {
			LOG.error("Failed executing getDynamicSememeUsageDescription().  Caught " + e.getClass().getName() + " " + e.getLocalizedMessage());
			e.printStackTrace();
			
			return;
		}
		DynamicSememeColumnInfo[] colInfo = dud.getColumnInfo();
		DynamicSememeDataBI[] data = refex.getData();
		LOG.debug(indent + "dynamic sememe nid=" + refex.getNid() + ", uuid=" + refex.getPrimordialUuid());
		LOG.debug(indent + "dynamic sememe name=\"" + dud.getRefexName() + "\": " + refex.toUserString() + " with " + colInfo.length + " columns:");
		for (int colIndex = 0; colIndex < colInfo.length; ++colIndex) {
			DynamicSememeColumnInfo currentCol = colInfo[colIndex];
			String name = currentCol.getColumnName();
			DynamicSememeDataType type = currentCol.getColumnDataType();
			UUID colUuid = currentCol.getColumnDescriptionConcept();
			DynamicSememeDataBI colData = data[colIndex];

			LOG.debug(indent + "\t" + "dynamic sememe: " + refex.toUserString() + " col #" + colIndex + " (uuid=" + colUuid + ", type=" + type.getDisplayName() + "): " + name + "=" + (colData != null ? colData.getDataObject() : null));
		}
		
		Collection<? extends DynamicSememeVersionBI<?>> embeddedRefexes = null;
		try {
			embeddedRefexes = refex.getRefexesDynamicActive(OTFUtility.getViewCoordinate());

			for (DynamicSememeVersionBI<?> embeddedRefex : embeddedRefexes) {
				displayDynamicRefex(embeddedRefex, depth + 1);
			}
		} catch (IOException e) {
			LOG.error("Failed executing getRefexesDynamicActive(OTFUtility.getViewCoordinate()).  Caught " + e.getClass().getName() + " " + e.getLocalizedMessage());
			e.printStackTrace();
		}
	}
}
