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
import org.ihtsdo.otf.tcc.api.chronicle.ComponentChronicleBI;
import org.ihtsdo.otf.tcc.api.concept.ConceptChronicleBI;
import org.ihtsdo.otf.tcc.api.concept.ConceptVersionBI;
import org.ihtsdo.otf.tcc.api.contradiction.ContradictionException;
import org.ihtsdo.otf.tcc.api.description.DescriptionVersionBI;
import org.ihtsdo.otf.tcc.api.metadata.binding.Snomed;
import gov.va.isaac.ExtendedAppContext;
import gov.va.isaac.util.OTFUtility;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeDataBI;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeDataType;
import gov.vha.isaac.ochre.api.constants.ISAAC;
import gov.vha.isaac.ochre.model.sememe.dataTypes.DynamicSememeNid;
import gov.vha.isaac.ochre.model.sememe.dataTypes.DynamicSememeUUID;

/**
 * {@link Association}
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public class Association
{
	private DynamicSememeVersionBI<?> refex_;

	//TODO Write the code that checks the index states on startup

	public Association(DynamicSememeVersionBI<?> data)
	{
		refex_ = data;
	}

	public ComponentChronicleBI<?> getSourceComponent() throws IOException
	{
		return ExtendedAppContext.getDataStore().getComponent(refex_.getReferencedComponentNid());
	}

	public ComponentChronicleBI<?> getTargetComponent() throws IOException, ContradictionException
	{
		int targetColIndex = AssociationUtilities.findTargetColumnIndex(refex_.getAssemblageNid());
		if (targetColIndex >= 0)
		{
		
			DynamicSememeDataBI[] data = refex_.getData();
			if (data != null && data.length > targetColIndex)
			{
				if (data[targetColIndex].getRefexDataType() == DynamicSememeDataType.UUID)
				{
					return ExtendedAppContext.getDataStore().getComponent(((DynamicSememeUUID) data[targetColIndex]).getDataUUID());
				}
				else if (data[targetColIndex].getRefexDataType() == DynamicSememeDataType.NID)
				{
					return ExtendedAppContext.getDataStore().getComponent(((DynamicSememeNid) data[targetColIndex]).getDataNid());
				}
			}
		}
		else
		{
			throw new RuntimeException("unexpected");
		}
		
		return null;
	}

	public ConceptChronicleBI getAssociationTypeConcept() throws IOException
	{
		return ExtendedAppContext.getDataStore().getConcept(refex_.getAssemblageNid());
	}

	public String getAssociationName() throws IOException, ContradictionException
	{
		Optional<? extends ConceptVersionBI> cc = getAssociationTypeConcept().getVersion(OTFUtility.getViewCoordinate());
		if (!cc.isPresent())
		{
			return "NOT ON PATH!";
		}
		
		String best = null;
		for (DescriptionVersionBI<?> desc : cc.get().getDescriptionsActive(Snomed.SYNONYM_DESCRIPTION_TYPE.getNid()))
		{
			if (best == null)
			{
				best = desc.getText();
			}
			if (OTFUtility.isPreferred(desc.getAnnotations()))
			{
				return desc.getText();
			}
		}
		return best;
	}

	public String getAssociationInverseName() throws ContradictionException, IOException
	{
		Optional<? extends ConceptVersionBI> cc = getAssociationTypeConcept().getVersion(OTFUtility.getViewCoordinate());
		if (!cc.isPresent())
		{
			return "NOT ON PATH!";
		}
		
		for (DescriptionVersionBI<?> desc : cc.get().getDescriptionsActive(Snomed.SYNONYM_DESCRIPTION_TYPE.getNid()))
		{
			for (DynamicSememeVersionBI<?> descNestedType : desc.getRefexesDynamicActive(OTFUtility.getViewCoordinate()))
			{
				if (descNestedType.getAssemblageNid() == ISAAC.ASSOCIATION_INVERSE_NAME.getNid())
				{
					return desc.getText();
				}
			}
		}
		return null;
	}

	public DynamicSememeVersionBI<?> getData()
	{
		return refex_;
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
			return refex_.toString();
		}
	}
}
