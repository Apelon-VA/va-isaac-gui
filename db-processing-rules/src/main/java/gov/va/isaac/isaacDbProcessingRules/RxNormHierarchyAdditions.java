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
package gov.va.isaac.isaacDbProcessingRules;

import gov.va.isaac.util.OTFUtility;
import gov.vha.isaac.metadata.coordinates.StampCoordinates;
import gov.vha.isaac.metadata.coordinates.ViewCoordinates;
import gov.vha.isaac.metadata.source.IsaacMetadataAuxiliaryBinding;
import gov.vha.isaac.mojo.termstore.transforms.TransformArbitraryI;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.chronicle.LatestVersion;
import gov.vha.isaac.ochre.api.component.sememe.SememeChronology;
import gov.vha.isaac.ochre.api.component.sememe.SememeType;
import gov.vha.isaac.ochre.api.component.sememe.version.DynamicSememe;
import gov.vha.isaac.ochre.model.sememe.dataTypes.DynamicSememeUUID;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Named;
import org.ihtsdo.otf.tcc.api.blueprint.IdDirective;
import org.ihtsdo.otf.tcc.api.blueprint.RelationshipCAB;
import org.ihtsdo.otf.tcc.api.coordinate.EditCoordinate;
import org.ihtsdo.otf.tcc.api.description.DescriptionChronicleBI;
import org.ihtsdo.otf.tcc.api.description.DescriptionVersionBI;
import org.ihtsdo.otf.tcc.api.metadata.binding.Snomed;
import org.ihtsdo.otf.tcc.api.metadata.binding.TermAux;
import org.ihtsdo.otf.tcc.api.relationship.RelationshipType;
import org.ihtsdo.otf.tcc.api.store.TerminologyStoreDI;
import org.jvnet.hk2.annotations.Service;

/**
 * {@link RxNormHierarchyAdditions}
 * 
 * A Transformer that executes various rules to create more taxonomy in RxNorm
 * 
 * So far:
 * 
 * Let’s further integrate RxNorm and SNOMED CT. For every RxNorm IN concept with the string “penicillin” in its name, make it a child (is-a) 
 * of SNOMED CT fdca98cf-8720-3dbe-bb72-3377d658a85c Penicillin -class of antibiotic- (product).
 * 
 *   a.  What should happen here is that the relationship between the RxNorm penicillin ingredient concepts and the dummy “RxNorm Ingredients” 
 *       is made redundant by the classifier… it would still be there in stated view, but not in inferred view.
 * 
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
@Service
@Named(value = "RxNorm Hierarchy Additions")
public class RxNormHierarchyAdditions implements TransformArbitraryI
{
	private AtomicInteger addedRels = new AtomicInteger();
	private AtomicInteger examinedConcepts = new AtomicInteger();
	
	private final UUID penicillinProduct = UUID.fromString("fdca98cf-8720-3dbe-bb72-3377d658a85c");//Penicillin -class of antibiotic- (product)
	private final UUID termTypeIN = UUID.fromString("92cd011b-629f-5514-a545-37e946337962"); //Name for an Ingredient     // IN
	private final UUID rxNormModule= IsaacMetadataAuxiliaryBinding.RXNORM.getPrimodialUuid();
	private final UUID rxNormDescType = UUID.fromString("7476a06a-c653-5ca5-bddb-42c441305521");// "RxNorm Description Type" - refex that carries ingredient linkage

	/**
	 * @see gov.vha.isaac.mojo.termstore.transforms.TransformI#getName()
	 */
	@Override
	public String getName()
	{
		return "RxNorm Hierarchy Additions";
	}
	
	/**
	 * @see gov.vha.isaac.mojo.termstore.transforms.TransformI#configure(java.io.File, org.ihtsdo.otf.tcc.api.store.TerminologyStoreDI)
	 */
	@Override
	public void configure(File configFile, TerminologyStoreDI ts)
	{
		// noop
	}

	/**
	 * @throws Exception
	 * @see gov.vha.isaac.mojo.termstore.transforms.TransformI#transform(org.ihtsdo.otf.tcc.api.store.TerminologyStoreDI)
	 */
	@Override
	public void transform(TerminologyStoreDI ts) throws Exception
	{
		int rxNormModuleNid = ts.getNidForUuids(rxNormModule);
		int rxNormDescTypeAssemblageNid = ts.getNidForUuids(rxNormDescType);
		ts.getParallelConceptStream().forEach((cc) -> 
		{
			try
			{
				examinedConcepts.getAndIncrement();
				
				boolean isRxNormPath = false;
				for (int stampNid : cc.getConceptAttributes().getAllStamps())
				{
					if (ts.getModuleNidForStamp(stampNid) == rxNormModuleNid)
					{
						isRxNormPath = true;
						break;
					}
				}
				
				if (!isRxNormPath)
				{
					return;
				}
				
				boolean isRxNormIN = false;
				boolean hasPenicillin = false;
				for (DescriptionChronicleBI desc : cc.getDescriptions())
				{
					if (hasPenicillin && isRxNormIN)
					{
						break;
					}
					DescriptionVersionBI<?> currentDescription = OTFUtility.getLatestDescVersion(desc.getVersionList());
					
					if (currentDescription == null)
					{
						continue;
					}
					
					
					isRxNormIN = Get.sememeService().getSememesForComponentFromAssemblage(currentDescription.getNid(), 
							Get.identifierService().getConceptSequence(rxNormDescTypeAssemblageNid)).anyMatch(sememe ->
					{
						if (sememe.getSememeType() == SememeType.DYNAMIC)
						{
							@SuppressWarnings({ "unchecked", "rawtypes" })
							Optional<LatestVersion<DynamicSememe>>  latest = ((SememeChronology)sememe).getLatestVersion(DynamicSememe.class, StampCoordinates.getDevelopmentLatest());
							if (latest.isPresent() && ((DynamicSememeUUID)latest.get().value().getData()[0]).getDataUUID().equals(termTypeIN))
							{
								return true;
							}
						}
						return false;
					});
					
					if (currentDescription.getText().toLowerCase().contains("penicillin"))
					{
						hasPenicillin = true;
					}
				}
				
				if (hasPenicillin && isRxNormIN)
				{
					RelationshipCAB rCab = new RelationshipCAB(cc.getPrimordialUuid(), Snomed.IS_A.getUuids()[0], penicillinProduct,
							0, RelationshipType.STATED_ROLE, IdDirective.GENERATE_HASH);
					
					ts.getTerminologyBuilder(new EditCoordinate(TermAux.USER.getLenient().getNid(), IsaacMetadataAuxiliaryBinding.SOLOR_OVERLAY.getNid(), 
							IsaacMetadataAuxiliaryBinding.DEVELOPMENT.getNid()), ViewCoordinates.getDevelopmentStatedLatest()).construct(rCab);
					ts.addUncommitted(cc);
					
					int last = addedRels.getAndIncrement();
					if (last % 2000 == 0)
					{
						ts.commit();
					}
				}
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}
		});
		ts.commit();
	}

	/**
	 * @see gov.vha.isaac.mojo.termstore.transforms.TransformI#getDescription()
	 */
	@Override
	public String getDescription()
	{
		return "Add various relationships between RxNorm and Snomed CT";
	}

	/**
	 * @see gov.vha.isaac.mojo.termstore.transforms.TransformI#getWorkResultSummary()
	 */
	@Override
	public String getWorkResultSummary()
	{
		return "Examined " + examinedConcepts.get() + " concepts and generated " + addedRels.get() + " new relationships";
	}

	@Override
	public void writeSummaryFile(File outputFolder) throws IOException
	{
		// noop
	}
}
