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
package gov.va.isaac.associations;

import java.io.IOException;
import java.util.Optional;
import gov.va.isaac.ExtendedAppContext;
import gov.vha.isaac.metadata.source.IsaacMetadataAuxiliaryBinding;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.chronicle.LatestVersion;
import gov.vha.isaac.ochre.api.component.concept.ConceptChronology;
import gov.vha.isaac.ochre.api.component.concept.ConceptVersion;
import gov.vha.isaac.ochre.api.component.sememe.SememeChronology;
import gov.vha.isaac.ochre.api.component.sememe.SememeType;
import gov.vha.isaac.ochre.api.component.sememe.version.ComponentNidSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.DescriptionSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.DynamicSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeDataBI;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeDataType;
import gov.vha.isaac.ochre.impl.utility.Frills;
import gov.vha.isaac.ochre.model.constants.IsaacMetadataConstants;
import gov.vha.isaac.ochre.model.sememe.dataTypes.DynamicSememeNid;
import gov.vha.isaac.ochre.model.sememe.dataTypes.DynamicSememeSequence;
import gov.vha.isaac.ochre.model.sememe.dataTypes.DynamicSememeUUID;

/**
 * {@link Association}
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public class Association
{
	private DynamicSememe<?> sememe_;

	//TODO Write the code that checks the index states on startup

	public Association(DynamicSememe<?> data)
	{
		sememe_ = data;
	}

	public ConceptChronology<? extends ConceptVersion<?>> getSourceComponent() throws IOException
	{
		return Get.conceptService().getConcept(sememe_.getReferencedComponentNid());
	}

	public ConceptChronology<? extends ConceptVersion<?>> getTargetComponent()
	{
		int targetColIndex = AssociationUtilities.findTargetColumnIndex(sememe_.getAssemblageSequence());
		if (targetColIndex >= 0)
		{
			DynamicSememeDataBI[] data = sememe_.getData();
			if (data != null && data.length > targetColIndex)
			{
				if (data[targetColIndex].getDynamicSememeDataType() == DynamicSememeDataType.UUID)
				{
					return Get.conceptService().getConcept(((DynamicSememeUUID) data[targetColIndex]).getDataUUID());
				}
				else if (data[targetColIndex].getDynamicSememeDataType() == DynamicSememeDataType.NID)
				{
					return Get.conceptService().getConcept(((DynamicSememeNid) data[targetColIndex]).getDataNid());
				}
				else if (data[targetColIndex].getDynamicSememeDataType() == DynamicSememeDataType.SEQUENCE)
				{
					return Get.conceptService().getConcept(((DynamicSememeSequence) data[targetColIndex]).getDataSequence());
				}
			}
		}
		else
		{
			throw new RuntimeException("unexpected");
		}
		
		return null;
	}

	public ConceptChronology<? extends ConceptVersion<?>> getAssociationTypeConcept() 
	{
		return Get.conceptService().getConcept(sememe_.getAssemblageSequence());
	}

	public String getAssociationName()
	{
		String best = null;
		for (DescriptionSememe<?> desc : Frills.getDescriptionsOfType(Get.identifierService().getConceptNid(sememe_.getAssemblageSequence()),
				IsaacMetadataAuxiliaryBinding.SYNONYM, ExtendedAppContext.getUserProfileBindings().getStampCoordinate().get()))
		{
			if (best == null)
			{
				best = desc.getText();
			}
			if (Frills.isDescriptionPreferred(desc.getNid(), ExtendedAppContext.getUserProfileBindings().getStampCoordinate().get()))
			{
				return desc.getText();
			}
		}
		
		if (best == null)
		{
			return "-No name on path!-";
		}
		return best;
	}

	public Optional<String> getAssociationInverseName()
	{
		String best = null;
		for (DescriptionSememe<?> desc : Frills.getDescriptionsOfType(Get.identifierService().getConceptNid(sememe_.getAssemblageSequence()),
				IsaacMetadataAuxiliaryBinding.SYNONYM, ExtendedAppContext.getUserProfileBindings().getStampCoordinate().get()))
		{
			if (best == null)
			{
				best = desc.getText();
			}
			if (Get.sememeService().getSememesForComponentFromAssemblage(desc.getNid(), 
					IsaacMetadataConstants.DYNAMIC_SEMEME_ASSOCIATION_INVERSE_NAME.getSequence()).anyMatch(nestedSememe ->
			{
				if (nestedSememe.getSememeType() == SememeType.DYNAMIC)
				{
					@SuppressWarnings({ "rawtypes", "unchecked" })
					Optional<LatestVersion<ComponentNidSememe>> latest = ((SememeChronology)nestedSememe).getLatestVersion(ComponentNidSememe.class, 
							ExtendedAppContext.getUserProfileBindings().getStampCoordinate().get());
					
					if (latest.isPresent())
					{
						return true;
					}
				}
				return false;
			}))
			{
				return Optional.of(desc.getText());
			}
		}
		
		return Optional.empty();
	}

	public DynamicSememe<?> getData()
	{
		return sememe_;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		try
		{
			return "Association [Name: " + getAssociationName() + " Inverse Name: " + getAssociationInverseName() + " Source: " + getSourceComponent().getPrimordialUuid() 
					+ " Type: " + getAssociationTypeConcept().getPrimordialUuid() + " Target: " + getTargetComponent().getPrimordialUuid() + "]";
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return sememe_.toString();
		}
	}
}
