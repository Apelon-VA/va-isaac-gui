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

import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import gov.va.isaac.ExtendedAppContext;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.chronicle.LatestVersion;
import gov.vha.isaac.ochre.api.component.sememe.SememeChronology;
import gov.vha.isaac.ochre.api.component.sememe.SememeType;
import gov.vha.isaac.ochre.api.component.sememe.version.DynamicSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeColumnInfo;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeDataBI;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeDataType;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeUsageDescriptionBI;

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
	
	public static void displayDynamicRefexes(int componentNid) {
		Get.sememeService().getSememesForComponent(componentNid).forEach(sememeC ->
		{
			if (sememeC.getSememeType() == SememeType.DYNAMIC)
			{
				@SuppressWarnings({ "unchecked", "rawtypes" })
				Optional<LatestVersion<DynamicSememe>> latest = ((SememeChronology)sememeC)
						.getLatestVersion(DynamicSememe.class, ExtendedAppContext.getUserProfileBindings().getStampCoordinate().get());
			
				if (latest.isPresent())
				{
					DynamicRefexHelper.displayDynamicRefex(latest.get().value());
				}
			}
		});
	}
	public static void displayDynamicRefex(DynamicSememe<?> refex) {
		displayDynamicSememe(refex, 0);
	}
	public static void displayDynamicSememe(DynamicSememe<?> refex, int depth) {
		String indent = "";
		
		for (int i = 0; i < depth; ++i) {
			indent += "\t";
		}
		
		DynamicSememeUsageDescriptionBI dud = null;
		try {
			dud = refex.getDynamicSememeUsageDescription();
		} catch (RuntimeException e) {
			LOG.error("Failed executing getDynamicSememeUsageDescription().  Caught " + e.getClass().getName(), e);
			return;
		}
		DynamicSememeColumnInfo[] colInfo = dud.getColumnInfo();
		DynamicSememeDataBI[] data = refex.getData();
		LOG.debug(indent + "dynamic sememe nid=" + refex.getNid() + ", uuid=" + refex.getPrimordialUuid());
		LOG.debug(indent + "dynamic sememe name=\"" + dud.getDyanmicSememeName() + "\": " + refex.toUserString() + " with " + colInfo.length + " columns:");
		for (int colIndex = 0; colIndex < colInfo.length; ++colIndex) {
			DynamicSememeColumnInfo currentCol = colInfo[colIndex];
			String name = currentCol.getColumnName();
			DynamicSememeDataType type = currentCol.getColumnDataType();
			UUID colUuid = currentCol.getColumnDescriptionConcept();
			DynamicSememeDataBI colData = data[colIndex];

			LOG.debug(indent + "\t" + "dynamic sememe: " + refex.toUserString() + " col #" + colIndex + " (uuid=" + colUuid + ", type=" + type.getDisplayName() + "): " + name + "=" + (colData != null ? colData.getDataObject() : null));
		}
		
		displayDynamicRefexes(refex.getNid());
	}
}
