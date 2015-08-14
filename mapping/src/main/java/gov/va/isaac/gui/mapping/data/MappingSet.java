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
package gov.va.isaac.gui.mapping.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import gov.va.isaac.ExtendedAppContext;
import gov.va.isaac.util.Utility;
import gov.vha.isaac.metadata.source.IsaacMetadataAuxiliaryBinding;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.chronicle.LatestVersion;
import gov.vha.isaac.ochre.api.component.concept.ConceptSnapshot;
import gov.vha.isaac.ochre.api.component.sememe.SememeChronology;
import gov.vha.isaac.ochre.api.component.sememe.version.DescriptionSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.DynamicSememe;
import gov.vha.isaac.ochre.impl.utility.Frills;
import gov.vha.isaac.ochre.model.constants.IsaacMetadataConstants;
import gov.vha.isaac.ochre.model.sememe.dataTypes.DynamicSememeString;
import gov.vha.isaac.ochre.model.sememe.dataTypes.DynamicSememeUUID;
import javafx.beans.property.SimpleStringProperty;

/**
 * {@link MappingSet}
 *
 * A Convenience class to hide unnecessary OTF bits from the Mapping APIs.
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public class MappingSet extends MappingObject
{
	private static final Logger LOG = LoggerFactory.getLogger(MappingSet.class);

	//private String name, inverseName, description, purpose;
	private UUID primordialUUID;

	private final SimpleStringProperty nameProperty        = new SimpleStringProperty();
	private final SimpleStringProperty purposeProperty 	   = new SimpleStringProperty();
	private final SimpleStringProperty descriptionProperty = new SimpleStringProperty();
	private final SimpleStringProperty inverseNameProperty = new SimpleStringProperty();
	
	/**
	 * 
	 * Read an existing mapping set from the database
	 * 
	 * @param refex DynamicSememeChronicleBI<?>
	 * @throws IOException
	 */
	protected MappingSet(DynamicSememe<?> refex) throws RuntimeException
	{
		this.readFromRefex(refex); //Sets Name, inverseName and Description, etc
	}

	public List<MappingItem> getMappingItems(boolean activeOnly)
	{
		List<MappingItem> mappingItems = null;
		try
		{
			mappingItems = MappingItemDAO.getMappingItems(this.getPrimordialUUID(), activeOnly);
		}
		catch (Exception e)
		{
			LOG.error("Error retrieving Mapping Items for " + this.getName(), e);
			mappingItems = new ArrayList<MappingItem>();
		}
		return mappingItems;
	}
	
	public SimpleStringProperty getNameProperty() 		 { return nameProperty; }
	public SimpleStringProperty getPurposeProperty() 	 { return purposeProperty; }
	public SimpleStringProperty getDescriptionProperty() { return descriptionProperty; }
	public SimpleStringProperty getInverseNameProperty() { return inverseNameProperty; }
	
	/**
	 * @param purpose - The 'purpose' of the mapping set. May specify null.
	 */
	public void setPurpose(String purpose)
	{
		purposeProperty.set(purpose);
	}

	/**
	 * @return - the 'purpose' of the mapping set - may be null
	 */
	public String getPurpose()
	{
		return purposeProperty.get();
	}

	/**
	 * @return the name of the mapping set
	 */
	public String getName()
	{
		return nameProperty.get();
	}

	/**
	 * @return - The inverse name of the mapping set - may return null
	 */
	public String getInverseName()
	{
		return inverseNameProperty.get();
	}

	/**
	 * @return - The user specified description of the mapping set.
	 */
	public String getDescription()
	{
		return descriptionProperty.get();
	}

	/**
	 * @param name - Change the name of the mapping set
	 */
	public void setName(String name)
	{
		this.nameProperty.set(name);
	}

	/**
	 * @param inverseName - Change the inverse name of the mapping set
	 */
	public void setInverseName(String inverseName)
	{
		this.inverseNameProperty.set(inverseName);
	}

	/**
	 * @param description - specify the description of the mapping set
	 */
	public void setDescription(String description)
	{
		this.descriptionProperty.set(description);
	}

	/**
	 * @return The summary of the mapping set
	 */
	public String getSummary(boolean activeOnly)
	{
		List<MappingItem> mappingItems;
		mappingItems = this.getMappingItems(activeOnly);
		return Integer.toString(mappingItems.size()) + " Mapping Items";
	}

	/**
	 * @return the identifier of this mapping set
	 */
	public UUID getPrimordialUUID()
	{
		return primordialUUID;
	}

	/**
	 * @return Any comments attached to this mapping set.
	 * @throws RuntimeException
	 */
	public List<MappingItemComment> getComments() throws RuntimeException
	{
		return MappingItemCommentDAO.getComments(getPrimordialUUID(), false);
	}

	private void readFromRefex(DynamicSememe<?> refex) throws RuntimeException
	{
		ConceptSnapshot mappingConcept = MappingSetDAO.getMappingConcept(refex); 
		if (mappingConcept != null)
		{
			primordialUUID = mappingConcept.getPrimordialUuid();
			readStampDetails(mappingConcept);
			setEditorStatusConcept((refex.getData().length > 0 && refex.getData()[0] != null ? ((DynamicSememeUUID) refex.getData()[0]).getDataUUID() : null));
			if (refex.getData().length > 1 && refex.getData()[1] != null)
			{
				setPurpose(((DynamicSememeString) refex.getData()[1]).getDataString());
			}

			Get.sememeService().getSememesForComponentFromAssemblage(mappingConcept.getNid(), 
					IsaacMetadataAuxiliaryBinding.DESCRIPTION_ASSEMBLAGE.getConceptSequence())
				.forEach(descriptionC ->
				{
					if (getName() != null && getDescription() != null && getInverseName() != null)
					{
						//noop... sigh... can't short-circuit in a forEach....
					}
					else
					{
						@SuppressWarnings({ "rawtypes", "unchecked" })
						Optional<LatestVersion<DescriptionSememe<?>>> latest = ((SememeChronology)descriptionC).getLatestVersion(DescriptionSememe.class, 
								ExtendedAppContext.getUserProfileBindings().getStampCoordinate().get());
						if (latest.isPresent())
						{
							DescriptionSememe<?> ds = latest.get().value();
							if (ds.getDescriptionTypeConceptSequence() == IsaacMetadataAuxiliaryBinding.SYNONYM.getConceptSequence())
							{
								if (Frills.isDescriptionPreferred(ds.getNid(), null))
								{
									setName(ds.getText());
								}
								else
								//see if it is the inverse name
								{
									if (Get.sememeService().getSememesForComponentFromAssemblage(ds.getNid(), 
											IsaacMetadataConstants.DYNAMIC_SEMEME_ASSOCIATION_INVERSE_NAME.getSequence()).anyMatch(sememeC -> 
											{
												return sememeC.isLatestVersionActive(ExtendedAppContext.getUserProfileBindings().getStampCoordinate().get());
											}))
									{
										setInverseName(ds.getText()); 
									}
								}
							}
							else if (ds.getDescriptionTypeConceptSequence() == IsaacMetadataAuxiliaryBinding.DEFINITION_DESCRIPTION_TYPE.getConceptSequence())
							{
								if (Frills.isDescriptionPreferred(ds.getNid(), null))
								{
									setDescription(ds.getText());
								}
							}
						}
					}
				});
		}
		else
		{
			String error = "cannot read mapping concept!";
			LOG.error(error);
			throw new RuntimeException(error);
		}
	}
	
	public static final Comparator<MappingSet> nameComparator = new Comparator<MappingSet>() {
		@Override
		public int compare(MappingSet o1, MappingSet o2) {
			return Utility.compareStringsIgnoreCase(o1.getName(), o2.getName());
		}
	};
	
	public static final Comparator<MappingSet> purposeComparator = new Comparator<MappingSet>() {
		@Override
		public int compare(MappingSet o1, MappingSet o2) {
			return Utility.compareStringsIgnoreCase(o1.getPurpose(), o2.getPurpose());
		}
	};
	
	public static final Comparator<MappingSet> descriptionComparator = new Comparator<MappingSet>() {
		@Override
		public int compare(MappingSet o1, MappingSet o2) {
			return Utility.compareStringsIgnoreCase(o1.getDescription(), o2.getDescription());
		}
	};
	
}
