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
package gov.va.isaac.isaacDbProcessingRules;

import gov.va.isaac.isaacDbProcessingRules.spreadsheet.RuleDefinition;
import gov.va.isaac.isaacDbProcessingRules.spreadsheet.SpreadsheetReader;
import gov.va.isaac.search.CompositeSearchResult;
import gov.va.isaac.search.SearchHandle;
import gov.va.isaac.search.SearchHandler;
import gov.vha.isaac.metadata.coordinates.ViewCoordinates;
import gov.vha.isaac.metadata.source.IsaacMetadataAuxiliaryBinding;
import gov.vha.isaac.mojo.termstore.transforms.TransformConceptIterateI;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.ihtsdo.otf.tcc.api.blueprint.IdDirective;
import org.ihtsdo.otf.tcc.api.blueprint.InvalidCAB;
import org.ihtsdo.otf.tcc.api.blueprint.RelationshipCAB;
import org.ihtsdo.otf.tcc.api.concept.ConceptChronicleBI;
import org.ihtsdo.otf.tcc.api.contradiction.ContradictionException;
import org.ihtsdo.otf.tcc.api.coordinate.EditCoordinate;
import org.ihtsdo.otf.tcc.api.coordinate.ViewCoordinate;
import org.ihtsdo.otf.tcc.api.metadata.binding.Snomed;
import org.ihtsdo.otf.tcc.api.metadata.binding.TermAux;
import org.ihtsdo.otf.tcc.api.relationship.RelationshipType;
import org.ihtsdo.otf.tcc.api.spec.ValidationException;
import org.ihtsdo.otf.tcc.api.store.TerminologyStoreDI;
import org.ihtsdo.otf.tcc.api.uuid.UuidFactory;
import org.ihtsdo.otf.tcc.dto.TtkConceptChronicle;
import org.ihtsdo.otf.tcc.dto.component.identifier.TtkIdentifier;
import org.ihtsdo.otf.tcc.dto.component.identifier.TtkIdentifierUuid;
import org.ihtsdo.otf.tcc.model.cc.concept.ConceptChronicle;

/**
 * {@link BaseSpreadsheetCode}
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a> 
 */
public abstract class BaseSpreadsheetCode implements TransformConceptIterateI 
{
	protected HashMap<UUID, Integer> uuidToNidMap_ = new HashMap<>();
	protected HashMap<Integer, AtomicInteger> generatedRels = new HashMap<>();
	protected HashMap<Integer, AtomicInteger> mergedConcepts = new HashMap<>();
	protected AtomicInteger examinedConcepts = new AtomicInteger();
	protected TreeMap<Integer, Set<String>> ruleHits = new TreeMap<>();  //rule id to the set of UUIDs impacted by the rule
	protected ConcurrentHashMap<String, Set<Integer>> conceptHitsByRule = new ConcurrentHashMap<>();  //inverse of above - hit concepts to rules that hit them
	protected List<RuleDefinition> rules_;
	protected long startTime;

	protected TerminologyStoreDI ts_;
	protected ViewCoordinate vc_;
	
	private String name_;
	private static final String eol = System.getProperty("line.separator");
	
	public BaseSpreadsheetCode(String name)
	{
		name_ = name;
	}
	
	/**
	 * @see gov.va.isaac.mojos.dbTransforms.TransformI#getName()
	 */
	@Override
	public String getName()
	{
		return name_;
	}
	
	protected void configure(String spreadsheetFileName, TerminologyStoreDI ts) throws IOException
	{
		startTime = System.currentTimeMillis();
		//TODO pass in the spreadsheet?  But then where to store it?
		ts_ = ts;
		vc_ = ViewCoordinates.getMasterStatedLatest();
		
		rules_ = new SpreadsheetReader().readSpreadSheet(SpreadsheetReader.class.getResourceAsStream(spreadsheetFileName));
		for (RuleDefinition rd : rules_)
		{
			ruleHits.put(rd.getId(), Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>()));
			generatedRels.put(rd.getId(), new AtomicInteger());
			mergedConcepts.put(rd.getId(), new AtomicInteger());
		}
	}
	
	protected int getNid(UUID uuid)
	{
		Integer nid = uuidToNidMap_.get(uuid);
		if (nid == null)
		{
			try
			{
				nid = ts_.getConcept(uuid).getNid();
				uuidToNidMap_.put(uuid, nid);
			}
			catch (IOException e)
			{
				throw new RuntimeException(e);
			}
		}
		if (nid == null || nid == 0)
		{
			throw new RuntimeException("Failed to find nid for uuid " + uuid);
		}
		return nid;
	}
	
	protected void addRel(ConceptChronicleBI source, UUID target) throws ValidationException, IOException, InvalidCAB, ContradictionException
	{
		RelationshipCAB rCab = new RelationshipCAB(source.getPrimordialUuid(), Snomed.IS_A.getUuids()[0], target, 0, RelationshipType.STATED_ROLE, 
				IdDirective.GENERATE_HASH);
		
		ts_.getTerminologyBuilder(new EditCoordinate(TermAux.USER.getLenient().getConceptNid(), IsaacMetadataAuxiliaryBinding.SOLOR_OVERLAY.getNid(), 
				getNid(IsaacMetadataAuxiliaryBinding.MASTER.getPrimodialUuid())), ViewCoordinates.getDevelopmentStatedLatest()).construct(rCab);
		ts_.addUncommitted(source);
	}
	
	protected void mergeConcepts(ConceptChronicleBI source, UUID mergeOnto, UUID authority) throws IOException, InvalidCAB, ContradictionException
	{
		//TODO this doesn't work - 

		//Create a TtkConcept of the thing we want to merge - but change the primary UUID to the thing we want to merge onto.
		//Keep our UUID as a secondary.
		//TRY 1 - this still doesn't seem quite right - the other id ends up as an alternate identifier, instead of a primary... which seems wrong.
		TtkConceptChronicle tcc = new TtkConceptChronicle(source);
		
		UUID temp = tcc.getPrimordialUuid();
		tcc.setPrimordialUuid(mergeOnto);
		if (tcc.getConceptAttributes().getAdditionalIdComponents() == null)
		{
			tcc.getConceptAttributes().setAdditionalIdComponents(new ArrayList<TtkIdentifier>());
		}
		
		TtkIdentifier id = new TtkIdentifierUuid(temp);
		id.setStatus(tcc.getConceptAttributes().getStatus());
		id.setTime(tcc.getConceptAttributes().getTime());
		id.setAuthorUuid(tcc.getConceptAttributes().getAuthorUuid());
		id.setModuleUuid(IsaacMetadataAuxiliaryBinding.SOLOR_OVERLAY.getPrimodialUuid());
		id.setPathUuid(tcc.getConceptAttributes().getPathUuid());
		id.setAuthorityUuid(authority);

		tcc.getConceptAttributes().getAdditionalIdComponents().add(id);
		
		//THIS didn't work either
//		ConceptVersionBI sourceVersion = source.getVersion(vc);
//		ConceptAttributeAB cab = new ConceptAttributeAB(sourceVersion.getConceptNid(), true, RefexDirective.EXCLUDE);
//		cab.setStatus(Status.INACTIVE);
//		
//		ts.getTerminologyBuilder(TermAux.USER.getLenient().getConceptNid(), IsaacMetadataAuxiliaryBinding.SOLOR_OVERLAY.getNid(), 
//		getNid(IsaacMetadataAuxiliaryBinding.MASTER.getPrimodialUuid())), StandardViewCoordinates.getWbAuxiliary()).construct(cab);
//		ts.addUncommitted(sourceVersion);
		
		ConceptChronicle.mergeAndWrite(tcc);
		
		
		
		//TRY 2 - this still lost the other UUID - need to ask Keith about this
//		ConceptChronicle mergeOntoCC = ConceptChronicle.get(ts.getNidForUuids(mergeOnto));
//		
//		ConceptChronicle.mergeWithEConcept(tcc, mergeOntoCC);
//		ts.addUncommittedNoChecks(mergeOntoCC);
	}
	
	protected int sum(Collection<AtomicInteger> values)
	{
		int total = 0;
		for (AtomicInteger ai : values)
		{
			total += ai.get();
		}
		return total;
	}

	
	@Override
	public void writeSummaryFile(File outputFolder) throws IOException
	{
		writeWorkResultDocBookTable(new File(outputFolder, getName() + " - Docbook.html"));
		writeWorkResultSummary(new File(outputFolder, getName() + " - Summary.txt"));
	}
	
	@Override
	public String getWorkResultSummary()
	{
		StringBuilder sb = new StringBuilder();
		
		int totalRelCount = sum(generatedRels.values());
		int totalMergedCount = sum(mergedConcepts.values());
		
		sb.append("Loaded " + rules_.size() + " rules" + eol);
		
		sb.append("Examined " + examinedConcepts.get() + " concepts and added hierarchy linkages to " + totalRelCount + " concepts.  "
				+ "Merged " + totalMergedCount + " concepts" + eol);
		
		return sb.toString();
	}

	private void writeWorkResultDocBookTable(File writeToFile) throws IOException
	{
		BufferedWriter fw = new BufferedWriter(new FileWriter(writeToFile));

		fw.append("<table frame='all'>" + eol);
		fw.append("\t<title>Results</title>" + eol);
		fw.append("\t<tgroup cols='6' align='center' colsep='1' rowsep='1'>" + eol);
		for (int i = 1; i <= 6; i++)
		{
			fw.append("\t\t<colspec colname='c" + i + "' colwidth='1*' />" + eol);
		}
		fw.append("\t\t<thead>" + eol);
		fw.append("\t\t\t<row>" + eol);
		fw.append("\t\t\t\t<entry>Transform</entry>" + eol);
		fw.append("\t\t\t\t<entry>Examined Concepts</entry>" + eol);
		fw.append("\t\t\t\t<entry>Generated Descriptions</entry>" + eol);
		fw.append("\t\t\t\t<entry>Generated Relationships</entry>" + eol);
		fw.append("\t\t\t\t<entry>Merged Concepts</entry>" + eol);
		fw.append("\t\t\t\t<entry>Execution Time</entry>" + eol);
		fw.append("\t\t\t</row>" + eol);
		fw.append("\t\t</thead>" + eol);
		
		fw.append("\t\t<tbody>" + eol);
		
		int i = 0;
		for (Entry<Integer, Set<String>> x : ruleHits.entrySet())
		{
			i++;
			fw.append("\t\t\t<row>" + eol);
			fw.append("\t\t\t\t<entry>" + x.getKey() + "</entry>" + eol);
			if (i == 1)
			{
				fw.append("\t\t\t\t<entry morerows='" + (ruleHits.size() - 1) + "'>" + examinedConcepts.get() + "</entry>" + eol);
				fw.append("\t\t\t\t<entry morerows='" + (ruleHits.size() - 1) + "'>-</entry>" + eol);  //no descriptions generated here
			}
			fw.append("\t\t\t\t<entry>" + generatedRels.get(x.getKey()).get() + "</entry>" + eol);
			fw.append("\t\t\t\t<entry>" + mergedConcepts.get(x.getKey()).get() + "</entry>" + eol);
			if (i == 1)
			{
				long temp = System.currentTimeMillis() - startTime;
				int seconds = (int)(temp / 1000l);
				fw.append("\t\t\t\t<entry morerows='" + (ruleHits.size() - 1) + "'>" + seconds + " seconds</entry>" + eol);
			}
			
			fw.append("\t\t\t</row>" + eol);
		}
	
		fw.append("\t\t</tbody>" + eol);
		fw.append("\t</tgroup>" + eol);
		fw.append("</table>" + eol);
		
		fw.close();
	}
	
	private void writeWorkResultSummary(File outputFile) throws IOException
	{
		BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile));
		
		int totalRelCount = sum(generatedRels.values());
		int totalMergedCount = sum(mergedConcepts.values());
		
		bw.append("Examined " + examinedConcepts.get() + " concepts and added hierarchy linkages to " + totalRelCount + " concepts.  "
				+ "Merged " + totalMergedCount + " concepts" + eol);
		
		bw.append("Rule,Concept Count,Concept UUID,Concept FSN,Concept UUID,Concept FSN" + eol);
		for (Entry<Integer, Set<String>> x : ruleHits.entrySet())
		{
			bw.append(x.getKey() + "," + x.getValue().size());
			if (x.getValue().size() <= 50)
			{
				for (String s : x.getValue())
				{
					bw.append("," + s);
				}
			}
			bw.append(eol);
		}
		
		bw.append(eol);
		bw.append(eol);
		bw.append("Concepts modified by more than one rule:" + eol);
		bw.append("Concept UUID,Concept FSN,Rule ID,Rule ID" + eol);
		for (Entry<String, Set<Integer>> x : conceptHitsByRule.entrySet())
		{
			if (x.getValue().size() > 1)
			{
				bw.append(x.getKey());
				for (Integer ruleId : x.getValue())
				{
					bw.append("," + ruleId);
				}
				bw.append(eol);
			}
		}
		bw.close();
	}
	
	protected UUID findSCTTarget(RuleDefinition rd) throws Exception
	{
		UUID sctTargetConcept = null;
		if (rd.getSctID() != null)
		{
			sctTargetConcept = UuidFactory.getUuidFromAlternateId(TermAux.SNOMED_IDENTIFIER.getUuids()[0], rd.getSctID().toString());
		}
		if (sctTargetConcept == null || !ts_.hasConcept(sctTargetConcept))
		{
			//try to find by FSN
			sctTargetConcept = null;
			SearchHandle sh = SearchHandler.descriptionSearch("\"" + rd.getSctFSN() + "\"", 5, null, true);
			for (CompositeSearchResult csr : sh.getResults())
			{
				for (String s : csr.getMatchingStrings())
				{
					if (rd.getSctFSN().equals(s))
					{
						//this is the concept we wanted.
						sctTargetConcept = csr.getContainingConcept().getPrimordialUuid();
						break;
					}
				}
				if (sctTargetConcept != null)
				{
					break;
				}
			}
		}
		
		if (sctTargetConcept == null)
		{
			throw new RuntimeException("Couldn't find target concept");
		}
		return sctTargetConcept;
	}
}
