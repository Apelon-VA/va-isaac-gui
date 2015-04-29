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
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gov.va.isaac.constants;

import gov.vha.isaac.metadata.source.IsaacMetadataAuxiliaryBinding;
import java.util.UUID;
import org.ihtsdo.otf.tcc.api.metadata.binding.RefexDynamic;
import org.ihtsdo.otf.tcc.api.metadata.binding.Snomed;
import org.ihtsdo.otf.tcc.api.refexDynamic.data.RefexDynamicColumnInfo;
import org.ihtsdo.otf.tcc.api.refexDynamic.data.RefexDynamicDataType;
import org.ihtsdo.otf.tcc.api.spec.ConceptSpec;
import org.ihtsdo.otf.tcc.api.spec.ConceptSpecWithDescriptions;
import org.ihtsdo.otf.tcc.api.spec.DynamicRefexConceptSpec;
import org.ihtsdo.otf.tcc.api.spec.RelSpec;

/**
 * {@link InformationModels}
 *
 * InformationModel related constants for ISAAC in ConceptSpec form for reuse.
 * 
 * The DBBuilder mojo processes this class, and creates these concept / relationships as necessary during build.
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a> 
 */
public class InformationModels
{
	//Information models is a top level child of root
		public static ConceptSpec INFORMATION_MODELS = new ConceptSpec("Information Models", UUID.fromString("ab09b185-b93d-577b-a350-622be832e6c7"),
			new RelSpec(null, Snomed.IS_A, IsaacMetadataAuxiliaryBinding.ISAAC_ROOT));
	static
	{
		//can't set these properly up above, without causing an infinite loop...
		INFORMATION_MODELS.getRelSpecs()[0].setOriginSpec(INFORMATION_MODELS);
	}
	
	//current information models
	public static ConceptSpec CEM = new ConceptSpec("Clinical Element Model", UUID.fromString("0a9c9ba5-410e-5a40-88f4-b0cdd17325e1"),
			INFORMATION_MODELS);
	
	public static ConceptSpec FHIM = new ConceptSpec("Federal Health Information Model", UUID.fromString("9eddce80-784c-50a3-8ec6-e92278ac7691"),
			INFORMATION_MODELS);

	public static ConceptSpec HED = new ConceptSpec("Health eDecision", UUID.fromString("1cdae521-c637-526a-bf88-134de474f824"),
			INFORMATION_MODELS);
	
	public static ConceptSpec CEM_ENUMERATIONS = new ConceptSpec("Clinical Element Model Enumerations", UUID.fromString("ee5da47f-562f-555d-b7dd-e18697e11ece"), CEM);	

	public static ConceptSpec FHIM_ENUMERATIONS= new ConceptSpec("Federal Health Information Model Enumerations",  UUID.fromString("78e5feff-faf7-5666-a2e1-21bdfe688e13"), FHIM);
			
	public static ConceptSpec HED_ENUMERATIONS = new ConceptSpec("Health eDecision Enumerations",  UUID.fromString("5f4cf488-38bd-54b0-8d08-809599d6db82"), HED);
	
	//Required columns
	public static ConceptSpecWithDescriptions PROPERTY_LABEL = new ConceptSpecWithDescriptions("info model property label", 
			UUID.fromString("7f1102be-2fe4-57e3-9b9d-80087d6ee054"),
			new String[] {"info model property label"},
			new String[] {"Used to capture the label for a property as used by the native information model type, e.g. 'qual' in CEM"},
			RefexDynamic.DYNAMIC_SEMEME_COLUMNS);
	
	public static ConceptSpecWithDescriptions PROPERTY_TYPE = new ConceptSpecWithDescriptions("info model property type", 
			UUID.fromString("302e90ab-c149-5a0f-b64e-b189de5e2292"),
			new String[] {"info model property type"},
			new String[] {"Used to capture the property type as expressed in the model, e.g. 'MethodDevice' in CEM"},
			RefexDynamic.DYNAMIC_SEMEME_COLUMNS);
	
	public static ConceptSpecWithDescriptions PROPERTY_NAME = new ConceptSpecWithDescriptions("info model property name", 
			UUID.fromString("2dc47ed8-9b53-57f0-a844-b15b2275e8e8"),
			new String[] {"info model property name"},
			new String[] {"Used to capture the property name as expressed in the model"},
			RefexDynamic.DYNAMIC_SEMEME_COLUMNS);
	
	public static ConceptSpecWithDescriptions PROPERTY_DEFAULT_VALUE = new ConceptSpecWithDescriptions("info model property default value", 
			UUID.fromString("a5e2412f-b27b-5dcf-aba0-f6a2869296b4"),
			new String[] {"info model property default value"},
			new String[] {"Used to capture any default value the property has in the model"},
			RefexDynamic.DYNAMIC_SEMEME_COLUMNS);
	
	public static ConceptSpecWithDescriptions PROPERTY_VALUE = new ConceptSpecWithDescriptions("info model property value", 
			UUID.fromString("a856f12e-b1dc-5521-ae85-2c232aba79e4"),
			new String[] {"info model property value"},
			new String[] {"Used to capture any actual value the property has (for demo purposes)"},
			RefexDynamic.DYNAMIC_SEMEME_COLUMNS);
	
	public static ConceptSpecWithDescriptions PROPERTY_CARDINALITY_MIN = new ConceptSpecWithDescriptions("info model property cardinality min", 
			UUID.fromString("a6d7fda7-bd08-5712-a4e4-19cf49e2702e"),
			new String[] {"info model property cardinality min"},
			new String[] {"Used to capture the cardinality lower limit in the model"},
			RefexDynamic.DYNAMIC_SEMEME_COLUMNS);
	
	public static ConceptSpecWithDescriptions PROPERTY_CARDINALITY_MAX = new ConceptSpecWithDescriptions("info model property cardinality max", 
			UUID.fromString("a6f17770-1256-5d5b-b298-90a71858f391"),
			new String[] {"info model property cardinality max"},
			new String[] {"Used to capture the cardinality upper limit in the model"},
			RefexDynamic.DYNAMIC_SEMEME_COLUMNS);
	
	public static ConceptSpecWithDescriptions PROPERTY_VISIBILITY = new ConceptSpecWithDescriptions("info model property visibility", 
			UUID.fromString("ff3653ec-61f8-5382-9d6f-d12a26f425d4"),
			new String[] {"info model property visibility"},
			new String[] {"Used to capture the property visibility in the model"},
			RefexDynamic.DYNAMIC_SEMEME_COLUMNS);
	
	
	public static DynamicRefexConceptSpec INFORMATION_MODEL_PROPERTIES = new DynamicRefexConceptSpec("Information model property refset", 
			UUID.fromString("ef4a1189-8fe0-56c3-9ca9-334c40b78fc1"),
			true, 
			"Used to capture information about information model properties", 
			new RefexDynamicColumnInfo[] {
				new RefexDynamicColumnInfo(0, PROPERTY_LABEL.getPrimodialUuid(), RefexDynamicDataType.STRING, null, false, null, null),
				new RefexDynamicColumnInfo(1, PROPERTY_TYPE.getPrimodialUuid(), RefexDynamicDataType.STRING, null, false, null, null),
				new RefexDynamicColumnInfo(2, PROPERTY_NAME.getPrimodialUuid(), RefexDynamicDataType.STRING, null, false, null, null),
				new RefexDynamicColumnInfo(3, PROPERTY_DEFAULT_VALUE.getPrimodialUuid(), RefexDynamicDataType.STRING, null, false, null, null),
				new RefexDynamicColumnInfo(4, PROPERTY_VALUE.getPrimodialUuid(), RefexDynamicDataType.STRING, null, false, null, null),
				new RefexDynamicColumnInfo(5, PROPERTY_CARDINALITY_MIN.getPrimodialUuid(), RefexDynamicDataType.STRING, null, false, null, null),
				new RefexDynamicColumnInfo(6, PROPERTY_CARDINALITY_MAX.getPrimodialUuid(), RefexDynamicDataType.STRING, null, false, null, null),
				new RefexDynamicColumnInfo(7, PROPERTY_VISIBILITY.getPrimodialUuid(), RefexDynamicDataType.STRING, null, false, null, null)
			}, RefexDynamic.DYNAMIC_SEMEME_IDENTITY);
	
	public static ConceptSpecWithDescriptions HAS_TERMINOLOGY_CONCEPT = new ConceptSpecWithDescriptions("Has terminology concept", 
			UUID.fromString("890b36d9-655f-5acb-9339-dd8628dced65"), 
			new String[] {"Has terminology concept"},
			new String[] {},
			IsaacMetadataAuxiliaryBinding.TAXONOMY_OPERATOR);
}

