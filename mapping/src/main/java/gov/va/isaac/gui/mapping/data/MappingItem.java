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

import gov.va.isaac.util.OchreUtility;
import gov.va.isaac.util.Utility;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.component.sememe.version.DynamicSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeDataBI;
import gov.vha.isaac.ochre.model.sememe.dataTypes.DynamicSememeUUID;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link MappingItem}
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public class MappingItem extends MappingObject
{
	private static final Logger LOG = LoggerFactory.getLogger(MappingItem.class);

	private static final String NO_MAP_NAME = "(not mapped)";
	
	private UUID primordialUUID, mappingSetIDConcept, qualifierConcept, sourceConcept, targetConcept;
	private int	sourceConceptNid, targetConceptNid, qualifierConceptNid;
	protected final SimpleStringProperty sourceConceptProperty    = new SimpleStringProperty();
	protected final SimpleStringProperty targetConceptProperty    = new SimpleStringProperty();
	protected final SimpleStringProperty qualifierConceptProperty = new SimpleStringProperty();
	protected final SimpleStringProperty commentsProperty		  = new SimpleStringProperty();
	
	protected MappingItem(DynamicSememe<?> sememe) throws RuntimeException
	{
		read(sememe);
	}
	
	private void read(DynamicSememe<?> sememe) throws RuntimeException
	{
		sourceConceptNid = sememe.getReferencedComponentNid();
		
		primordialUUID = sememe.getPrimordialUuid();
		setSourceConcept(Get.identifierService().getUuidPrimordialForNid(sourceConceptNid).get());
		mappingSetIDConcept = Get.identifierService().getUuidPrimordialForNid(sememe.getAssemblageSequence()).get();
		readStampDetails(sememe);
		
		DynamicSememeDataBI[] data = sememe.getData();
		setTargetConcept      (((data != null && data.length > 0 && data[0] != null) ? ((DynamicSememeUUID) data[0]).getDataUUID() : null));
		setQualifierConcept   (((data != null && data.length > 1 && data[1] != null) ? ((DynamicSememeUUID) data[1]).getDataUUID() : null)); 
		setEditorStatusConcept(((data != null && data.length > 2 && data[2] != null) ? ((DynamicSememeUUID) data[2]).getDataUUID() : null));
		
		targetConceptNid    = getNidForUuidSafe(targetConcept);
		qualifierConceptNid = getNidForUuidSafe(qualifierConcept);
		
		refreshCommentsProperty();
	}

	public int getSourceConceptNid() 	{ return sourceConceptNid; }
	public int getTargetConceptNid() 	{ return targetConceptNid; }
	public int getQualifierConceptNid() { return qualifierConceptNid; }
	
	public String getSummary() {
		return  (isActive() ? "Active " : "Retired ") + "Mapping: " + OchreUtility.getDescription(sourceConcept).get() 
				+ "-" + OchreUtility.getDescription(mappingSetIDConcept).get() 
				+ "-" + (targetConcept == null ? "not mapped" : OchreUtility.getDescription(targetConcept).get() ) + "-" 
				+ (qualifierConcept == null ? "no qualifier" : OchreUtility.getDescription(qualifierConcept).get() ) 
				+ "-" + (editorStatusConcept == null ? "no status" : OchreUtility.getDescription(editorStatusConcept).get() ) + "-" + primordialUUID.toString();
	}
	
	/**
	 * @return Any comments attached to this mapping set.
	 * @throws IOException 
	 */
	public List<MappingItemComment> getComments() throws IOException
	{
		return MappingItemCommentDAO.getComments(getPrimordialUUID(), true);
	}
	
	/**
	 * Add a comment to this mapping set
	 * @param commentText - the text of the comment
	 * @return - the added comment
	 * @throws IOException
	 */
	public MappingItemComment addComment(String commentText) throws IOException
	{
		//TODO do we want to utilize the other comment field (don't have to)
		return MappingItemCommentDAO.createMappingItemComment(this.getPrimordialUUID(), commentText, null);
	}

	/**
	 * @return the primordialUUID of this Mapping Item.  Note that this doesn't uniquely identify a mapping item within the system
	 * as changes to the mapping item will retain the same ID - there will now be multiple versions.  They will differ by date.
	 */
	public UUID getPrimordialUUID()
	{
		return primordialUUID;
	}

	public UUID getMappingSetIDConcept() { return mappingSetIDConcept;	}
	public UUID getSourceConcept()		 { return sourceConcept;	}
	public UUID getTargetConcept()		 { return targetConcept;	}
	public UUID getQualifierConcept()	 { return qualifierConcept;	}
	
	public SimpleStringProperty getSourceConceptProperty()	{ return sourceConceptProperty; }
	public SimpleStringProperty getTargetConceptProperty()	{ return targetConceptProperty;	}
	public SimpleStringProperty getQualifierConceptProperty() { return qualifierConceptProperty; }
	public SimpleStringProperty getCommentsProperty()			{ return commentsProperty; }
	
	private void setSourceConcept(UUID sourceConcept) {
		this.sourceConcept = sourceConcept;
		propertyLookup(sourceConcept, sourceConceptProperty);
	}
	
	private void setTargetConcept(UUID targetConcept) {
		this.targetConcept = targetConcept;
		if (targetConcept == null) {
			targetConceptProperty.set(NO_MAP_NAME);
		} else {
			propertyLookup(targetConcept, targetConceptProperty);
		}
	}
	
	private void setQualifierConcept(UUID qualifierConcept) {
		this.qualifierConcept = qualifierConcept;
		propertyLookup(qualifierConcept, qualifierConceptProperty);
	}
	
	public void refreshCommentsProperty() {
		Utility.execute(() ->
		{
			StringBuilder commentValue = new StringBuilder();
			try
			{
				List<MappingItemComment> comments = getComments();
				if (comments.size() > 0) {
					commentValue.append(comments.get(0).getCommentText());
				}
				if (comments.size() > 1) {
					commentValue.append(" (+" + Integer.toString(comments.size() - 1) + " more)");
				}
			}
			catch (IOException e)
			{
				LOG.error("Error reading comments!", e);
			}
			Platform.runLater(() ->
			{
				commentsProperty.set(commentValue.toString());
			});
		});
	}

	public static final Comparator<MappingItem> sourceComparator = new Comparator<MappingItem>() {
		@Override
		public int compare(MappingItem o1, MappingItem o2) {
			return Utility.compareStringsIgnoreCase(o1.getSourceConceptProperty().get(), o2.getSourceConceptProperty().get());
		}
	};
	
	public static final Comparator<MappingItem> targetComparator = new Comparator<MappingItem>() {
		@Override
		public int compare(MappingItem o1, MappingItem o2) {
			return Utility.compareStringsIgnoreCase(o1.getTargetConceptProperty().get(), o2.getTargetConceptProperty().get());
		}
	};
	
	public static final Comparator<MappingItem> qualifierComparator = new Comparator<MappingItem>() {
		@Override
		public int compare(MappingItem o1, MappingItem o2) {
			return Utility.compareStringsIgnoreCase(o1.getQualifierConceptProperty().get(), o2.getQualifierConceptProperty().get());
		}
	};
	
	public static final Comparator<MappingItem> commentsComparator = new Comparator<MappingItem>() {
		@Override
		public int compare(MappingItem o1, MappingItem o2) {
			return Utility.compareStringsIgnoreCase(o1.getCommentsProperty().get(), o2.getCommentsProperty().get());
		}
	};
	
	
}
