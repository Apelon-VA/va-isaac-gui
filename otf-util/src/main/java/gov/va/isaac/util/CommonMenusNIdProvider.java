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
package gov.va.isaac.util;

import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.chronicle.ObjectChronologyType;
import gov.vha.isaac.ochre.api.component.concept.ConceptChronology;
import gov.vha.isaac.ochre.api.component.concept.ConceptSnapshot;
import gov.vha.isaac.ochre.impl.utility.Frills;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;
import javafx.application.Platform;
import javafx.beans.binding.IntegerExpression;
import javafx.beans.binding.StringExpression;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

/**
 * {@link CommonMenusNIdProvider}
 *
 * @author <a href="mailto:joel.kniaz@gmail.com">Joel Kniaz</a>
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public abstract class CommonMenusNIdProvider
{
	SimpleIntegerProperty nidCount = new SimpleIntegerProperty(0);
	SimpleStringProperty uuidString = new SimpleStringProperty("");
	SimpleStringProperty sctIdString = new SimpleStringProperty("");
	
	private static final CommonMenusNIdProvider emptyCommonMenusNIdProvider =  new CommonMenusNIdProvider() {
		private final Collection<Integer> collection = Collections.unmodifiableSet(new HashSet<>());
		
		@Override
		public Collection<Integer> getNIds() {
			return collection;
		}
	};
	
	public static CommonMenusNIdProvider getEmptyCommonMenusNIdProvider() { return emptyCommonMenusNIdProvider; }

	protected CommonMenusNIdProvider()
	{
		nidCount.addListener(change ->
		{
			updateCaches();
		});
	}

	public abstract Collection<Integer> getNIds();

	public IntegerExpression getObservableNidCount()
	{
		return nidCount;
	}
	
	public StringExpression getObservableUUIDString()
	{
		return uuidString;
	}
	
	public StringExpression getObservableSctIdString()
	{
		return sctIdString;
	}

	public void invalidateAll()
	{
		Collection<Integer> nids = getNIds();
		nidCount.set(nids == null ? 0 : nids.size());
	}
		
	private void updateCaches()
	{
		uuidString.set("");
		sctIdString.set("");
		Utility.execute(() ->
		{
			StringBuilder uuids = new StringBuilder();
			StringBuilder sctIds = new StringBuilder();
			for (Integer i : getNIds()) {
				ConceptChronology<?> ochreConceptChronology = Get.conceptService().getConcept(i);
		
				//LOG.debug("Get.conceptService().getConcept({}) returned component {}", i, ochreConceptChronology.getPrimordialUuid());
		
				if (ochreConceptChronology != null && ochreConceptChronology.getPrimordialUuid() != null) {
					for (UUID uuid : ochreConceptChronology.getUuidList())
					{
						uuids.append(uuid + ", ");
					}
					if (Get.identifierService().getChronologyTypeForNid(i) == ObjectChronologyType.CONCEPT)
					{
						ConceptSnapshot conceptSnapshot = Get.conceptSnapshot().getConceptSnapshot(i);
						if (conceptSnapshot != null) {
							Optional<Long> conceptSct = Frills.getSctId(conceptSnapshot.getNid(), null);
							if(conceptSct.isPresent()) {
								sctIds.append(conceptSct.get() + ", ");
							}
						}
					}
				}
			}
			if (uuids.length() > 2)
			{
				uuids.setLength(uuids.length() - 2);
			}
			if (sctIds.length() > 2)
			{
				sctIds.setLength(sctIds.length() - 2);
			}
			Platform.runLater(() ->
			{
				sctIdString.set(sctIds.toString());
				uuidString.set(uuids.toString());
			});
		});
	}
}
