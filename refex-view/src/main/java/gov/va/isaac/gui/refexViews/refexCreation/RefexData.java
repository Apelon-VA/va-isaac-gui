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
package gov.va.isaac.gui.refexViews.refexCreation;

import java.util.ArrayList;
import java.util.List;
import org.ihtsdo.otf.tcc.api.concept.ConceptVersionBI;
import gov.vha.isaac.ochre.api.chronicle.ObjectChronologyType;
import gov.vha.isaac.ochre.api.component.concept.ConceptSnapshot;
import gov.vha.isaac.ochre.api.component.sememe.SememeType;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeColumnInfo;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeDataBI;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeDataType;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeValidatorType;

/**
 * 
 * {@link RefexData}
 *
 * @author <a href="jefron@apelon.com">Jesse Efron</a>
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public class RefexData
{
	private String refexName_;
	private String refexDescription_;
	private ConceptSnapshot parentConcept_;
	private ObjectChronologyType componentTypeRestriction_;
	private SememeType componentTypeSubRestriction_;
	private ArrayList<DynamicSememeColumnInfo> columnInfo_ = new ArrayList<>();

	public RefexData(String name, String description, ConceptSnapshot parentConcept, int extendedFieldsCount, ObjectChronologyType componentTypeRestriction,
			SememeType componentTypeSubRestriction)
	{
		this.refexName_ = name;
		this.refexDescription_ = description;
		this.parentConcept_ = parentConcept;
		this.componentTypeRestriction_ = componentTypeRestriction;
		this.componentTypeSubRestriction_ = componentTypeSubRestriction;
		for (int i = 0; i < extendedFieldsCount; i++)
		{
			DynamicSememeColumnInfo rdci = new DynamicSememeColumnInfo();
			rdci.setColumnOrder(i);
			columnInfo_.add(rdci);
		}
	}

	public void setColumnVals(int currentCol, ConceptVersionBI colNameConcept, DynamicSememeDataType type, DynamicSememeDataBI defaultValueObject, boolean isMandatory,
			DynamicSememeValidatorType validatorType, DynamicSememeDataBI validatorData)
	{
		adjustColumnCount(currentCol);
		DynamicSememeColumnInfo rdci = columnInfo_.get(currentCol);
		rdci.setColumnDescriptionConcept(colNameConcept.getPrimordialUuid());
		rdci.setColumnDataType(type);
		rdci.setColumnDefaultData(defaultValueObject);
		rdci.setColumnRequired(isMandatory);
		rdci.setValidatorType(new DynamicSememeValidatorType[] {validatorType});  //TODO support multiple validators
		rdci.setValidatorData(new DynamicSememeDataBI[] {validatorData});
	}
	
	public void adjustColumnCount(int col)
	{
		while (col > columnInfo_.size())
		{
			DynamicSememeColumnInfo rdci = new DynamicSememeColumnInfo();
			rdci.setColumnOrder(columnInfo_.size());
			columnInfo_.add(rdci);
		}
		
		while (columnInfo_.size() > col)
		{
			columnInfo_.remove(columnInfo_.size() - 1);
		}
	}

	public String getRefexName()
	{
		return refexName_;
	}
	
	public void setRefexName(String refexName)
	{
		refexName_ = refexName;
	}

	public String getRefexDescription()
	{
		return refexDescription_;
	}
	
	public void setRefexDescription(String refexDescription)
	{
		refexDescription_ = refexDescription;
	}

	public ConceptSnapshot getParentConcept()
	{
		return parentConcept_;
	}
	
	public void setParentConcept(ConceptSnapshot parentConcept)
	{
		parentConcept_ = parentConcept;
	}

	public int getExtendedFieldsCount()
	{
		return columnInfo_.size();
	}
	
	public void setComponentRestrictionType(ObjectChronologyType ct)
	{
		this.componentTypeRestriction_ = ct;
	}
	
	public ObjectChronologyType getComponentRestrictionType()
	{
		return componentTypeRestriction_;
	}
	
	public void setComponentSubRestrictionType(SememeType st)
	{
		this.componentTypeSubRestriction_ = st;
	}
	
	public SememeType getComponentSubRestrictionType()
	{
		return componentTypeSubRestriction_;
	}

	public List<DynamicSememeColumnInfo> getColumnInfo()
	{
		return columnInfo_;
	}
}
