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

import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.chronicle.ObjectChronology;
import gov.vha.isaac.ochre.api.chronicle.StampedVersion;
import gov.vha.isaac.ochre.api.component.sememe.version.DynamicSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeDataBI;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeDataType;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.dataTypes.DynamicSememeNidBI;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.dataTypes.DynamicSememeUUIDBI;
import gov.vha.isaac.ochre.model.sememe.dataTypes.DynamicSememeNid;
import gov.vha.isaac.ochre.model.sememe.dataTypes.DynamicSememeUUID;

/**
 * {@link AssociationInstance}
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public class AssociationInstance
{
	private DynamicSememe<?> sememe_;
	
	private transient AssociationType assnType_;

	//TODO Write the code that checks the index states on startup
	
	private AssociationInstance(DynamicSememe<?> data)
	{
		sememe_ = data;
	}
	
	public static AssociationInstance read(DynamicSememe<?> data)
	{
		return new AssociationInstance(data);
	}
	
	public AssociationType getAssociationType()
	{
		if (assnType_ == null)
		{
			assnType_ = AssociationType.read(sememe_.getAssemblageSequence());
		}
		return assnType_;
	}



	/**
	 * @return the source component of the association.
	 */
	public ObjectChronology<? extends StampedVersion> getSourceComponent()
	{
		return Get.identifiedObjectService().getIdentifiedObjectChronology(sememe_.getReferencedComponentNid()).get();
	}
	
	/**
	 * @return the nid of the source component of the association
	 */
	public int getSourceComponentData()
	{
		return sememe_.getReferencedComponentNid();
	}

	/**
	 * @return - the target component (if any) linked by this association instance
	 * This may return an empty if there was no target linked, or, if the target linked
	 * was a UUID that isn't resolveable in this DB (in which case, see the {@link #getTargetComponentData()} method)
	 */
	public Optional<? extends ObjectChronology<? extends StampedVersion>> getTargetComponent()
	{
		int targetColIndex = AssociationUtilities.findTargetColumnIndex(sememe_.getAssemblageSequence());
		if (targetColIndex >= 0)
		{
			DynamicSememeDataBI[] data = sememe_.getData();
			if (data != null && data.length > targetColIndex)
			{
				int nid = 0;
				if (data[targetColIndex].getDynamicSememeDataType() == DynamicSememeDataType.UUID 
						&& Get.identifierService().hasUuid(((DynamicSememeUUID) data[targetColIndex]).getDataUUID()))
				{
					nid = Get.identifierService().getNidForUuids(((DynamicSememeUUID) data[targetColIndex]).getDataUUID());
				}
				else if (data[targetColIndex].getDynamicSememeDataType() == DynamicSememeDataType.NID)
				{
					nid = ((DynamicSememeNid) data[targetColIndex]).getDataNid();
				}
				if (nid != 0)
				{	
					return Get.identifiedObjectService().getIdentifiedObjectChronology(nid);
				}
			}
		}
		else
		{
			throw new RuntimeException("unexpected");
		}
		
		return Optional.empty();
	}
	
	/**
	 * @return the raw target component data - which will be of type {@link DynamicSememeNidBI} or {@link DynamicSememeUUIDBI}
	 * or, it may be empty, if there was not target.
	 */
	public Optional<DynamicSememeDataBI> getTargetComponentData()
	{
		int targetColIndex = AssociationUtilities.findTargetColumnIndex(sememe_.getAssemblageSequence());
		if (targetColIndex >= 0)
		{
			DynamicSememeDataBI[] data = sememe_.getData();
			if (data != null && data.length > targetColIndex)
			{
				if (data[targetColIndex].getDynamicSememeDataType() == DynamicSememeDataType.UUID)
				{
					return Optional.of(((DynamicSememeUUIDBI) data[targetColIndex]));
				}
				else if (data[targetColIndex].getDynamicSememeDataType() == DynamicSememeDataType.NID)
				{
					return Optional.of((DynamicSememeNidBI) data[targetColIndex]);
				}
			}
		}
		else
		{
			throw new RuntimeException("unexpected");
		}
		
		return Optional.empty();
	}

	/**
	 * @return the concept sequence of the association type concept (without incurring the overhead of reading the AssoicationType object)
	 */
	public int getAssociationTypeSequenece() 
	{
		return sememe_.getAssemblageSequence();
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
			return "Association [Name: " + getAssociationType().getAssociationName() + " Inverse Name: " + getAssociationType().getAssociationInverseName() 
					+ " Source: " + getSourceComponent().getPrimordialUuid() 
					+ " Type: " + getAssociationType().getAssociationTypeConcept().getPrimordialUuid() + " Target: " + getTargetComponentData().toString() + "]";
		}
		catch (Exception e)
		{
			LogManager.getLogger().error("Error formatting association instance", e);
			return sememe_.toString();
		}
	}
}
