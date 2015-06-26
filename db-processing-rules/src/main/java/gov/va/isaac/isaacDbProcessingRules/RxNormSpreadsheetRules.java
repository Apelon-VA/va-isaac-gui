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

import gov.va.isaac.AppContext;
import gov.va.isaac.ExtendedAppContext;
import gov.va.isaac.config.profiles.UserProfileManager;
import gov.va.isaac.init.SystemInit;
import gov.va.isaac.isaacDbProcessingRules.spreadsheet.Operand;
import gov.va.isaac.isaacDbProcessingRules.spreadsheet.RuleDefinition;
import gov.va.isaac.isaacDbProcessingRules.spreadsheet.SelectionCriteria;
import gov.va.isaac.util.OTFUtility;
import gov.vha.isaac.metadata.source.IsaacMetadataAuxiliaryBinding;
import gov.vha.isaac.mojo.termstore.transforms.TransformConceptIterateI;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Named;
import org.ihtsdo.otf.tcc.api.conattr.ConceptAttributeVersionBI;
import org.ihtsdo.otf.tcc.api.concept.ConceptChronicleBI;
import org.ihtsdo.otf.tcc.api.contradiction.ContradictionException;
import org.ihtsdo.otf.tcc.api.description.DescriptionChronicleBI;
import org.ihtsdo.otf.tcc.api.refexDynamic.RefexDynamicVersionBI;
import org.ihtsdo.otf.tcc.api.refexDynamic.data.dataTypes.RefexDynamicUUIDBI;
import org.ihtsdo.otf.tcc.api.store.TerminologyStoreDI;
import org.jvnet.hk2.annotations.Service;

/**
 * {@link RxNormSpreadsheetRules}
 * 
 * A Transformer that implements various rules for LOINC transformations.
 * 
 * See docs/initial LOINC Rules.xlsx for the details on these that have been implemented so far.
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
@Service
@Named(value = "RxNorm spreadsheet rules")
public class RxNormSpreadsheetRules extends BaseSpreadsheetCode implements TransformConceptIterateI 
{
	private final UUID RXCUI = UUID.fromString("55598b28-5ffd-5746-820b-d90b73fb20b3");  //RXCUI
	private final UUID RxNormDescType = UUID.fromString("7476a06a-c653-5ca5-bddb-42c441305521");// "RxNorm Description Type" - refex that carries ingredient linkage
	private final UUID IN = UUID.fromString("92cd011b-629f-5514-a545-37e946337962"); //Name for an Ingredient     // IN

	private RxNormSpreadsheetRules()
	{
		super ("RxNorm spreadhsheet rules");
	}
	
	/**
	 * @throws IOException 
	 * @see gov.va.isaac.mojos.dbTransforms.TransformI#configure(java.io.File, org.ihtsdo.otf.tcc.api.store.TerminologyStoreDI)
	 */
	@Override
	public void configure(File configFile, TerminologyStoreDI ts) throws IOException
	{
		super.configure("/SOLOR RxNorm Rules v2.xlsx", ts);
	}
	

	/**
	 * 
	 * @see gov.va.isaac.mojos.dbTransforms.TransformConceptIterateI#transform(org.ihtsdo.otf.tcc.api.store.TerminologyStoreDI, 
	 *  org.ihtsdo.otf.tcc.api.concept.ConceptChronicleBI)
	 */
	@Override
	public boolean transform(TerminologyStoreDI ts, ConceptChronicleBI cc) throws Exception
	{
		examinedConcepts.incrementAndGet();
		ConceptAttributeVersionBI<?> latest = OTFUtility.getLatestAttributes(cc.getConceptAttributes().getVersionList());
		if (latest.getModuleNid() == getNid(IsaacMetadataAuxiliaryBinding.RXNORM.getPrimodialUuid()))
		{
			//Rule for all other rules - but only check for v 1
			if (spreadsheetVersion_ == 2 || ttyIs(IN, cc))
			{
				boolean commitRequired = false;
				
				for (RuleDefinition rd : rules_)
				{
					try
					{
						boolean ruleNeedsCommit = processRule(rd, cc);
						if (ruleNeedsCommit)
						{
							commitRequired = true;
						}
					}
					catch (Exception e)
					{
						AtomicInteger failCount = rulesFailed.get(rd.getId());
						if (failCount == null)
						{
							failCount = new AtomicInteger();
							rulesFailed.put(rd.getId(), failCount);
						}
						failCount.incrementAndGet();
						//Only dump the error the first time it happens (will happen many times, if the rule is bad)
						if (failCount.get() == 1)
						{
							System.err.println("!!! Failure processing rule " + rd.getId());
							e.printStackTrace();
						}
					}
				}
				return commitRequired;
			}
		}
		
		return false;
	}
	
	private boolean processRule(RuleDefinition rd, ConceptChronicleBI cc) throws Exception
	{
		for (SelectionCriteria sc : rd.getCriteria())
		{
			boolean invert = false;
			if (sc.getOperand() != null && sc.getOperand() == Operand.NOT)
			{
				invert = true;
			}

			boolean passed;
			switch (sc.getType())
			{
				case RXCUI:
					passed = rxCuiIs(sc.getValue(), cc);
					break;
				default :
					throw new RuntimeException("Unhandled type");
			}
			if (invert)
			{
				passed = (passed ? false : true);
			}
			if (!passed)
			{
				return passed;
			}
		}
		
		//passed all criteria
		ruleHits.get(rd.getId()).add(cc.getPrimordialUuid() + "," + OTFUtility.getFullySpecifiedName(cc));
		Set<Integer> rules = conceptHitsByRule.get(cc.getPrimordialUuid());
		if (rules == null)
		{
			rules = Collections.newSetFromMap(new ConcurrentHashMap<Integer, Boolean>());
			Set<Integer> oldRules = conceptHitsByRule.put(cc.getPrimordialUuid().toString() + "," + OTFUtility.getFullySpecifiedName(cc), rules);
			if (oldRules != null)
			{
				//two different threads tried to do this at the same time.  merge
				rules.addAll(oldRules);
			}
		}
		rules.add(rd.getId());
		
		UUID sctTargetConcept = findSCTTarget(rd);
		
		switch (rd.getAction())
		{
			case CHILD_OF:
				addRel(cc, sctTargetConcept);
				generatedRels.get(rd.getId()).getAndIncrement();
				break;
			case MERGE:
				mergeConcepts(cc, sctTargetConcept, IsaacMetadataAuxiliaryBinding.RXNORM.getPrimodialUuid());
				mergedConcepts.get(rd.getId()).incrementAndGet();
				break;
			default :
				throw new RuntimeException("Unhandled Action");
			
		}
		return true;
	}
	
	private boolean rxCuiIs(String component, ConceptChronicleBI cc) throws IOException, ContradictionException
	{
		for (RefexDynamicVersionBI<?> rdv : cc.getRefexesDynamicActive(vc_))
		{
			if (rdv.getAssemblageNid() == getNid(RXCUI))
			{
				if (rdv.getData(0).getDataObject().toString().equals(component))
				{
					return true;
				}
			}
		}
		return false;
	}
	
	private boolean ttyIs(UUID tty, ConceptChronicleBI cc) throws IOException, ContradictionException
	{
		for (DescriptionChronicleBI d : cc.getDescriptions())
		{
			for (RefexDynamicVersionBI<?> rdv : d.getRefexesDynamicActive(vc_))
			{
				if (rdv.getAssemblageNid() == getNid(RxNormDescType))
				{
					if (((RefexDynamicUUIDBI)rdv.getData(0)).getDataUUID().equals(IN))
					{
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * @see gov.va.isaac.mojos.dbTransforms.TransformI#getDescription()
	 */
	@Override
	public String getDescription()
	{
		return "Implementation of rules processing from a spreadsheet";
	}

	public static void main(String[] args) throws Exception
	{
		Exception dataStoreLocationInitException = SystemInit.doBasicSystemInit(new File("../../../ISAAC-DB/isaac-db-solor/target/"));
		if (dataStoreLocationInitException != null)
		{
			System.err.println("Configuration of datastore path failed.  DB will not be able to start properly!  " + dataStoreLocationInitException);
			System.exit(-1);
		}
		AppContext.getService(UserProfileManager.class).configureAutomationMode(new File("profiles"));
		
		RxNormSpreadsheetRules lsr = new RxNormSpreadsheetRules();
		lsr.configure((File)null, ExtendedAppContext.getDataStore());
		lsr.transform(ExtendedAppContext.getDataStore(), ExtendedAppContext.getDataStore().getConcept(UUID.fromString("b8a86aff-a33d-5ab9-88fe-bb3cfd8dce39")));
		System.out.println(lsr.getWorkResultSummary());
	}
}
