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

import java.util.UUID;
import org.ihtsdo.otf.tcc.api.metadata.binding.Snomed;
import org.ihtsdo.otf.tcc.api.metadata.binding.Taxonomies;
import org.ihtsdo.otf.tcc.api.spec.ConceptSpec;
import org.ihtsdo.otf.tcc.api.spec.RelSpec;

/**
 * {@link ISAAC}
 * 
 * Various constants for ISAAC in ConceptSpec form for reuse.
 * 
 * The DBBuilder mojo processes this class, and creates these concept / relationships as necessary during build.
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a> 
 */
public class ISAAC
{
	//Root node
	public static ConceptSpec ISAAC_ROOT = new ConceptSpec("ISAAC Root", UUID.fromString("c767a452-41e3-5835-90b7-439f5b738035"));
	
	//Information models is a top level child of root
	public static ConceptSpec INFORMATION_MODELS = new ConceptSpec("Information Models", UUID.fromString("ab09b185-b93d-577b-a350-622be832e6c7"),
			ISAAC_ROOT);
	
	//current information models
	public static ConceptSpec INFORMATION_MODEL_CEM = new ConceptSpec("Clinical Element Model", UUID.fromString("0a9c9ba5-410e-5a40-88f4-b0cdd17325e1"),
			INFORMATION_MODELS);
	
	public static ConceptSpec INFORMATION_MODEL_FHIM = new ConceptSpec("Federal Health Information Model", UUID.fromString("9eddce80-784c-50a3-8ec6-e92278ac7691"),
			INFORMATION_MODELS);

	public static ConceptSpec INFORMATION_MODEL_HED = new ConceptSpec("Health eDecision", UUID.fromString("1cdae521-c637-526a-bf88-134de474f824"),
			INFORMATION_MODELS);
	
	
	//Other children of the root node - just reference here, so that the DB Builder can process them and create the rels.
	private static ConceptSpec REFSET_AUXILLIARY_REF = Taxonomies.REFSET_AUX;
	static 
	{
		REFSET_AUXILLIARY_REF.setRelSpecs(new RelSpec[] {new RelSpec(REFSET_AUXILLIARY_REF, Snomed.IS_A, ISAAC_ROOT)});
	}
	
	private static ConceptSpec TERMINOLOGY_AUXILLIARY_REF = Taxonomies.WB_AUX;
	static 
	{
		TERMINOLOGY_AUXILLIARY_REF.setRelSpecs(new RelSpec[] {new RelSpec(TERMINOLOGY_AUXILLIARY_REF, Snomed.IS_A, ISAAC_ROOT)});
	}
}
