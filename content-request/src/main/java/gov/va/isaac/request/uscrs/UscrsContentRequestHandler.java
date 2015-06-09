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
 *	 http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gov.va.isaac.request.uscrs;

import gov.va.isaac.ExtendedAppContext;
import gov.va.isaac.gui.conceptViews.helpers.ConceptViewerHelper;
import gov.va.isaac.interfaces.gui.constants.SharedServiceNames;
import gov.va.isaac.interfaces.gui.views.commonFunctionality.ExportTaskHandlerI;
import gov.va.isaac.request.uscrs.USCRSBatchTemplate.COLUMN;
import gov.va.isaac.request.uscrs.USCRSBatchTemplate.PICKLIST_Case_Significance;
import gov.va.isaac.request.uscrs.USCRSBatchTemplate.PICKLIST_Refinability;
import gov.va.isaac.request.uscrs.USCRSBatchTemplate.PICKLIST_Relationship_Type;
import gov.va.isaac.request.uscrs.USCRSBatchTemplate.PICKLIST_Semantic_Tag;
import gov.va.isaac.request.uscrs.USCRSBatchTemplate.PICKLIST_Source_Terminology;
import gov.va.isaac.request.uscrs.USCRSBatchTemplate.SHEET;
import gov.va.isaac.util.OTFUtility;
import gov.vha.isaac.metadata.source.IsaacMetadataAuxiliaryBinding;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import javafx.concurrent.Task;
import javax.inject.Named;
import org.glassfish.hk2.api.PerLookup;
import org.ihtsdo.otf.tcc.api.chronicle.ComponentVersionBI;
import org.ihtsdo.otf.tcc.api.conattr.ConceptAttributeChronicleBI;
import org.ihtsdo.otf.tcc.api.conattr.ConceptAttributeVersionBI;
import org.ihtsdo.otf.tcc.api.concept.ConceptChronicleBI;
import org.ihtsdo.otf.tcc.api.concept.ConceptVersionBI;
import org.ihtsdo.otf.tcc.api.contradiction.ContradictionException;
import org.ihtsdo.otf.tcc.api.coordinate.Status;
import org.ihtsdo.otf.tcc.api.coordinate.ViewCoordinate;
import org.ihtsdo.otf.tcc.api.description.DescriptionChronicleBI;
import org.ihtsdo.otf.tcc.api.description.DescriptionVersionBI;
import org.ihtsdo.otf.tcc.api.metadata.binding.Snomed;
import org.ihtsdo.otf.tcc.api.relationship.RelAssertionType;
import org.ihtsdo.otf.tcc.api.relationship.RelationshipChronicleBI;
import org.ihtsdo.otf.tcc.api.relationship.RelationshipVersionBI;
import org.ihtsdo.otf.tcc.api.spec.ValidationException;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * USCRS implementation of a {@link ExportTaskHandlerI}.
 *
 * @author bcarlsenca
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 * @author <a href="mailto:vkaloidis@apelon.com">Vas Kaloidis</a>
 */
@Service
@Named(value = SharedServiceNames.USCRS)
@PerLookup
public class UscrsContentRequestHandler implements ExportTaskHandlerI
{
	Properties prop = new Properties();
	boolean filter = false;
	String invalidPropFound = "none";
	Long previousReleaseTime;
	
	private UscrsContentRequestHandler() {
		//hk2
	}
	
	/*
	 * Filters: Date (long) filters export to any modifications made after this date. Throwing
	 * 	a filter that is not listed here will throw an exception
	 * 
	 * (non-Javadoc)
	 * @see gov.va.isaac.interfaces.gui.views.commonFunctionality.ExportTaskHandlerI#setOptions(java.util.Properties)
	 */
	@Override
	public void setOptions(Properties options) throws Exception {
		prop = options;

		//Filters: author, etc...
		ArrayList<String> propKeys = new ArrayList<String>();
		propKeys.add("date");
		
		if(prop.containsKey("date")) { 
			logger.debug("USCRS Handler - properties contains date key");
			filter = true; 
			previousReleaseTime = Long.parseLong(prop.getProperty("date")); 
			logger.debug("USCRS Handler - previous release time is " + prop.getProperty("date"));
		} else {
			filter = false;
			previousReleaseTime = Long.MIN_VALUE;
		}
		
		//TODO: Throw an exception if an option is passed in that is not recognized
//		prop.values().stream().forEach(p -> { 
//											if(!propKeys.contains(p)) {
//												invalidPropFound = String.valueOf(p);
//											}
//										});
//		if(!invalidPropFound.equals("none")) {
//			throw new Exception("Invalid property: " + invalidPropFound);
//		}
//		
	}

	@Override
	public String getTitle() {
		return "USCRS Content Request Handler";
	}

	@Override
	public String getDescription() {
		return "Exports a USCRS Content Request Excel file";
	}
	
	/** The request id counter. */
	private static AtomicInteger globalRequestCounter = new AtomicInteger(1);

	/** The Constant logger. */
	private static final Logger logger = LoggerFactory.getLogger(UscrsContentRequestHandler.class);
	
	//Maps NID to (generated) request ID, which is used in place of an SCTID
	private LinkedHashMap<Integer, Integer> currentRequestMap = new LinkedHashMap<Integer, Integer>();
	
	// The list of (generated) ids that are in use on the new concept tab.
	private HashSet<Integer> newConceptRequestIds = new HashSet<>();;
	private USCRSBatchTemplate bt = null;
	private int examinedConCount = 0;
	private ViewCoordinate vcPreviousRelease;
	private ViewCoordinate viewCoordinate;
	
	private IntStream conceptStream;
	
	public static void main(String[] args) {
		UscrsContentRequestHandler ucrh = new UscrsContentRequestHandler();
		
		IntStream is = IntStream.of(-2147374144, 2143494493, -2147483620, -2147418042);
		Properties p = new Properties();
		
		p.setProperty("date", Long.toString(1104732000000L));
		
		ucrh.createTask(is, new File("C:\\Users\\vkaloidis\\Desktop\\").toPath());
	}
	
	//TODO: Document EVERYTHING
	@Override
	public Task<Integer> createTask(IntStream intStream, Path file) 
	{
		return new Task<Integer>() {
			
			@Override
			protected Integer call() throws Exception {
				updateTitle("Beginning Uscrs Content Request Export Operation");
				
				conceptStream = intStream;
				
				try {
					bt = new USCRSBatchTemplate(USCRSBatchTemplate.class.getResourceAsStream("/USCRS_Batch_Template-2015-01-27.xls"));
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				viewCoordinate = new ViewCoordinate();
				viewCoordinate.setAllowedStatus(EnumSet.of(Status.ACTIVE));
				viewCoordinate.setRelationshipAssertionType(RelAssertionType.STATED);
				
				conceptStream
					.forEach( nid -> {				
						
						if(examinedConCount % 50 == 0) { //TODO: Modify this based on what options are passed in above
							updateTitle("Uscrs Content Request Exported " + examinedConCount + " components");
						}
						if(isCancelled()) {
							logger.info("User canceled Uscrs Export Operation");
							throw new RuntimeException("User canceled operation");
						}
						
						ConceptVersionBI concept = OTFUtility.getConceptVersion(nid); 
						
						ArrayList<RelationshipVersionBI<?>> exportRelsUnFiltered = new ArrayList<RelationshipVersionBI<?>>();
						
						//TODO: Start breaking this code up into sub-methods. Because we will be adding more filters, so break up code.
						if(filter) {
							logger.debug("USCRS Handler - We are using a filter");
							if(prop.containsKey("date") && previousReleaseTime != Long.MIN_VALUE) { 
								logger.debug("USCRS Handler - We are using a Date Filter");
								boolean conceptCreated = false;
								try {
									vcPreviousRelease = new ViewCoordinate();
									vcPreviousRelease.getViewPosition().setTime(previousReleaseTime);
									vcPreviousRelease.setAllowedStatus(EnumSet.of(Status.ACTIVE));
									
									Collection<? extends ConceptAttributeChronicleBI> attributes = (Collection<? extends ConceptAttributeChronicleBI>) concept.getConceptAttributes();
									for(ConceptAttributeChronicleBI cac : attributes) {
										Optional<? extends ConceptAttributeVersionBI> caLatest = cac.getVersion(viewCoordinate);
										Optional<? extends ConceptAttributeVersionBI> caInitial = cac.getVersion(vcPreviousRelease);
										
										if(caInitial.isPresent()) {
											if(caLatest.isPresent()) {
												ConceptAttributeVersionBI<?> thisCaLatest = caLatest.get();
												ConceptAttributeVersionBI<?> thisCaInitial = caInitial.get();
												
												boolean conceptIsChanged = false;
												if(thisCaInitial.isDefined() != thisCaLatest.isDefined()) {
													conceptIsChanged = true;
												}
												
												if(conceptIsChanged) {
													//TODO: handleChangeConcept() Method
													ConceptChronicleBI thisConChronicle = OTFUtility.getConceptVersion(caLatest.get().getConceptNid());
													exportRelsUnFiltered.addAll(handleNewConcept(thisConChronicle));
													conceptCreated = true;
												} else {
													//noop
												}
											} else {
												handleRetireConcept(concept);
											}
										} else {
											if(caLatest.isPresent()) {
												ViewCoordinate vcPrActiveInactive = vcPreviousRelease;
												vcPrActiveInactive.setAllowedStatus(EnumSet.of(Status.INACTIVE, Status.ACTIVE));
												
												Optional<? extends ConceptAttributeVersionBI> cavRetiredCheck = cac.getVersion(vcPrActiveInactive);
												if(cavRetiredCheck.isPresent()) {
													// Place in the edit concept tab (un-retired)
												} else {
													exportRelsUnFiltered.addAll(handleNewConcept(concept));
													conceptCreated = true;
												}
											} else {
												//noop
											}
											
										}
									}
									
								} catch (Exception e) {
									logger.error("Error getting concept " + nid + " attributes for date / time comparison: " + e.getMessage());
									e.printStackTrace();
								}
								
								if(conceptCreated) {
									logger.info("USCRS Handler - Concept was already created, handeling components accordingly (skip description and first 3 ISA relationships");
								}
								
								//Export Descriptions
								if(!conceptCreated){ //TODO DAN Question: Do we not do this if we created a new concept
									try {
										Collection<? extends DescriptionChronicleBI> descriptions = concept.getDescriptions();
										for(DescriptionChronicleBI d : descriptions) {
											//TODO: Suppress generic type inferment warnings of entire call(). Do last
											Optional<? extends DescriptionVersionBI> dvLatest = d.getVersion(viewCoordinate);
											Optional<? extends DescriptionVersionBI> dvInitial = d.getVersion(vcPreviousRelease);
											
											if(dvInitial.isPresent()) {
												if(dvLatest.isPresent()){
													DescriptionVersionBI<?> thisDvLatest = dvLatest.get();
													DescriptionVersionBI<?> thisDvInitial = dvInitial.get();
													
													boolean hasChange = false;
													if(!thisDvLatest.getLang().equals(thisDvInitial.getLang())) {
														hasChange = true;
													} else if(!thisDvLatest.getText().equals(thisDvInitial.getText())) {
														hasChange = true;
													} else if (thisDvLatest.isInitialCaseSignificant() != thisDvInitial.isInitialCaseSignificant()) {
														hasChange = true;
													}
													if(hasChange) {
														handleChangeDesc(thisDvLatest);
													}
												} else {
													ArrayList<DescriptionVersionBI<?>> retiredDescs = new ArrayList<DescriptionVersionBI<?>>();
													retiredDescs.add(dvInitial.get());
												}
											} else {
												if(dvLatest.isPresent()) {
													ViewCoordinate vcPrActiveInactive = vcPreviousRelease;
													vcPrActiveInactive.setAllowedStatus(EnumSet.of(Status.INACTIVE, Status.ACTIVE));
													
													Optional<? extends DescriptionVersionBI> dvCheckRetired = d.getVersion(vcPrActiveInactive);
													if(dvCheckRetired.isPresent()) {
														handleChangeDesc(dvCheckRetired.get());
													} else {
														handleNewSyn(dvLatest.get());
													}
												} else {
													//noop
												}
											}
										}
									} catch (Exception e) {
										logger.error("Description Export Error: " + e.getMessage());
										e.printStackTrace();
									}
								}
								
								//Export Relationships
								if(conceptCreated) {
									//noop
								} else {
									try {
										Collection<? extends RelationshipChronicleBI> outgoingRels = concept.getRelationshipsOutgoing();
										
										outgoingRels.stream()
										.forEach( r -> {
														Optional<? extends RelationshipVersionBI<?>> thisVersion = null;
														try {
															thisVersion = r.getVersion(viewCoordinate);
														} catch (Exception e) {
															e.printStackTrace();  //TODO fix these...
														}
														if(thisVersion != null) {
															exportRelsUnFiltered.add(thisVersion.get()); 
														}
														
													});
									} catch (Exception e) {
										logger.error("Error retreiving the incoming relationshios: " + e.getMessage());
										e.printStackTrace();  //TODO don't do this - log errors properly
									}
								}
								for(RelationshipVersionBI<?> rv : exportRelsUnFiltered) {
									try {
										Optional<? extends RelationshipVersionBI<?>> rvLatest = rv.getVersion(viewCoordinate);
										Optional<? extends RelationshipVersionBI<?>> rvInitial = rv.getVersion(vcPreviousRelease);
										
										if(rvInitial.isPresent()) {
											if(rvLatest.isPresent()){
												RelationshipVersionBI<?> thisRvLatest = rvLatest.get();
												RelationshipVersionBI<?> thisRvInitial = rvInitial.get();
												
												boolean hasRelChange = false;
												if(thisRvLatest.getCharacteristicNid() != thisRvInitial.getCharacteristicNid()) {
													hasRelChange = true;
												} else if(thisRvLatest.getGroup() != thisRvInitial.getGroup()) {
													hasRelChange = true;
												} else if (thisRvLatest.getRefinabilityNid() != thisRvInitial.getRefinabilityNid()) {
													hasRelChange = true;
												} else if (thisRvLatest.isInferred() != thisRvInitial.isInferred()) {
													hasRelChange = true;
												} else if (thisRvLatest.isStated() != thisRvInitial.isStated()) {
													hasRelChange = true;
												}
												if(hasRelChange) {
													handleChangeRels(thisRvLatest);
												}
											} else {
												handleRetireRelationship(rvInitial.get());
											}
										} else {
											if(rvLatest.isPresent()) {
												ViewCoordinate rvPrActiveInactive = vcPreviousRelease;
												rvPrActiveInactive.setAllowedStatus(EnumSet.of(Status.INACTIVE, Status.ACTIVE));
												
												Optional<? extends RelationshipVersionBI<?>> rvCheckRetired = rv.getVersion(rvPrActiveInactive);
												if(rvCheckRetired.isPresent()) {
													handleChangeRels(rvCheckRetired.get());
												} else {
													handleNewRel(rvLatest.get());  //TODO what about newParent?
												}
											} else {
												//noop
											}
										}
									} catch (Exception e) {
										e.printStackTrace();
									}
								}
							} else {
								//No date filter, process everything that way
							}
							
							try {
								//TODO VAS - not sure what is going on  here-  you already exported (some?  all?) of this list of rels above, on line 385, no?
								//I think this just needs to go away... but you should look closer....
								for (RelationshipVersionBI<?> r : exportRelsUnFiltered)
								{
									//TODO how do you know these are new, and not modify?
									handleNewParent(r);
									handleNewRel(r);
								}
								
								//handleChangeDesc(descVersion, bt); Implement this
							} catch (Exception e) {
								logger.error("Could not export concept " + nid);
								e.printStackTrace();
							} 
						} else {
							 //Export ALL - (No Filter)
							logger.info("USCRS Handler -Exporting all concepts, no filters");
							try {
								exportRelsUnFiltered.addAll(handleNewConcept(concept));
								
								for (RelationshipVersionBI<?> r : exportRelsUnFiltered)
								{
									handleNewParent(r);
									handleNewRel(r);
								}
								
								Collection<? extends DescriptionChronicleBI> descriptions = concept.getDescriptions();
								
								for(DescriptionChronicleBI d : descriptions) {
									if(d.getVersion(viewCoordinate).isPresent()) {
										handleNewSyn(d.getVersion(viewCoordinate).get());
									}
									
								}
								
							} catch (Exception e) {
								logger.error("Could not export concept " + nid);
								e.printStackTrace();
							} 
							
						}
						examinedConCount++;
				});
				
				for (int genId : currentRequestMap.values())
				{
					if (!newConceptRequestIds.contains(genId))
					{
						throw new Exception("We wrote out the generated ID: " + genId + " but failed to create a new concept for that ID.  Logic failure!");
					}
				}
				
				
				if(isCancelled()) {
					return examinedConCount;
				}
				
				//TODO: Update the return messages and the progress. Do it update every ~20 concepts
				
				//info.setName(OTFUtility.getConPrefTerm(concept.getNid()));
				
				logger.info("  file = " + file);
				if (file != null)
				{
					bt.saveFile(file.toFile());
//					return new OperationResult(" " + file.getPath(), new HashSet<SimpleDisplayConcept>(), "The concepts were succesfully exported");
				}
				else
				{ 
					logger.error("File object is null, could not proceed");
					throw new RuntimeException("The Operation could not be completed because the file is null");
				}
				return examinedConCount;
			}
		};
	}
	

	/**
	 * Creates a new row in the "New Concept" tab of the workbook passed in. It returns the ISA relationships
	 * if there are more than 3.It also returns all non ISA relationships
	 *
	 * @param concept the concept
	 * @param bt the wb
	 * @return ArrayList<RelationshipVersionBI> extra relationships (if more than 3 ISA, those are returned), plus all non ISA
	 * @throws Exception the exception
	 */
	private ArrayList<RelationshipVersionBI<?>> handleNewConcept(ConceptChronicleBI concept) throws Exception
	{
		ArrayList<RelationshipVersionBI<?>> extraRels = new ArrayList<RelationshipVersionBI<?>>();
		// PARENTS
		LinkedList<Integer> parentNids = new LinkedList<Integer>();
		LinkedList<String> parentsTerms = new LinkedList<String>();
		
		LinkedList<String> definitions = new LinkedList<String>();
		
		int count = 0;
		for (RelationshipChronicleBI rel : concept.getRelationshipsOutgoing())
		{
			ViewCoordinate vc;
			vc = OTFUtility.getViewCoordinate();
			vc.setRelationshipAssertionType(RelAssertionType.STATED);
			//RelationshipVersionBI<?> relVersion = rel.getVersion(vc); //TODO: This was leading to possible issues. Needs more testing..
			
			RelationshipVersionBI<?> relVersion = rel.getVersion(OTFUtility.getViewCoordinate()).get();
			
			if(relVersion != null) {
				if(relVersion.isActive()) 
				{
					if ((relVersion.getTypeNid() == Snomed.IS_A.getLenient().getNid()))
					{
						int relDestNid = relVersion.getDestinationNid();
						parentNids.add(count, relDestNid);

						parentsTerms.add(count, this.getTerminology(
								ExtendedAppContext.getDataStore().getComponentVersion(OTFUtility.getViewCoordinate(), relDestNid).get()));
						
						if(count > 2 && relVersion != null) {
							extraRels.add(relVersion);
						}
						count++;
					} else {
						extraRels.add(relVersion);
					}
				}
			}
		}

		//Synonyms
		List<String> synonyms = new ArrayList<>();
		for (DescriptionChronicleBI desc : concept.getDescriptions())
		{
			DescriptionVersionBI<?> descVersion = desc.getVersion(OTFUtility.getViewCoordinate()).get();
			// Synonyms: find active, non FSN descriptions not matching the preferred name
			if (descVersion.isActive() && (descVersion.getTypeNid() != Snomed.FULLY_SPECIFIED_DESCRIPTION_TYPE.getLenient().getNid())
					&& !descVersion.getText().equals(OTFUtility.getConPrefTerm(concept.getNid())))
			{
				synonyms.add(descVersion.getText());
			}
			//Definition
			if(descVersion.getTypeNid() == Snomed.DEFINITION_DESCRIPTION_TYPE.getLenient().getNid()
					&& descVersion.isActive()){
				definitions.add(descVersion.getText());
			}
		}

		bt.selectSheet(SHEET.New_Concept);
		bt.addRow();
		for (COLUMN column : bt.getColumnsOfSheet(SHEET.New_Concept))
		{
			switch (column)
			{
				case Request_Id:
					long reqId = getSct(concept.getNid());
					if (reqId > Integer.MAX_VALUE)
					{
						throw new RuntimeException("We appear to have found an SCTID when we only expected a generated sequence ID");
					}
					newConceptRequestIds.add((int)reqId);
					bt.addNumericCell(column, reqId);
					break;
				case Topic:
					bt.addStringCell(column, ""); //User Input
					break;
				case Local_Code:
					bt.addStringCell(column, concept.getPrimordialUuid().toString());
					break;
				case Local_Term: 
					bt.addStringCell(column, OTFUtility.getConPrefTerm(concept.getNid()));
					break;
				case Fully_Specified_Name:
					bt.addStringCell(column, this.getFsnWithoutSemTag(concept));
					break;
				case Semantic_Tag:
					bt.addStringCell(column, this.getSemanticTag(concept));
					break;
				case Preferred_Term:
					bt.addStringCell(column, OTFUtility.getConPrefTerm(concept.getNid()));
					break;
				//Note that this logic is fragile, and will break, if we encounter a parentConcept column before the corresponding terminology column....
				//but we should be processing them in order, as far as I know.
				case Terminology_1_:
				case Terminology_2_:
				case Terminology_3_:
					if (parentNids.size() >= 1)
					{
						bt.addStringCell(column, getTerminology(OTFUtility.getConceptVersion(parentNids.get(0))));
					}
					else
					{
						bt.addStringCell(column, "");
					}
					break;
				case Parent_Concept_Id_1_:
				case Parent_Concept_Id_2_:
				case Parent_Concept__Id_3_:
					if(parentNids.size() >= 1) 
					{
						bt.addNumericCell(column, getSct(parentNids.remove(0)));
						
					} else 
					{
						bt.addStringCell(column, "");
					}
					break;
				case UMLS_CUI:
					bt.addStringCell(column, ""); //Not in API
					break;
				case Definition:
					if(definitions.size() > 0) 
					{
						bt.addStringCell(column, definitions.remove(0));
					} else {
						bt.addStringCell(column, "");
					}
					
					break;
				case Proposed_Use:
					bt.addStringCell(column, ""); //User Input
					break;
				case Justification:
					//Probably not correct because justification needs to be specific to that row
					bt.addStringCell(column, "Developed as part of extension namespace " + ExtendedAppContext.getCurrentlyLoggedInUserProfile().getExtensionNamespace());
					break;
				case Note:
					StringBuilder sb = new StringBuilder();
					
					//sb.append("SCT ID:" + this.getSct(-2143244556));
					
					
					if (concept.getConceptAttributes().getVersion(OTFUtility.getViewCoordinate()).get().isDefined())
					{
						sb.append("NOTE: this concept is fully defined. ");
					}
					
					boolean firstDef = false;
					
					//Extra Definitions
					if(definitions.size() > 0) {
						sb.append("Note: This concept has multiple definitions: ");
					}
					boolean firstSyn = false;
					while(definitions.size() > 0) 
					{
						if(firstDef) 
						{
							sb.append(", ");
						}
					}
					
					
					//Extra Synonyms
					if(synonyms.size() > 2) 
					{
						sb.append("NOTE: this concept also has the following synonyms: ");
					}
					while (synonyms.size() > 2)
					{
						if (firstSyn)
						{
							sb.append(", ");
						}
						sb.append(synonyms.remove(0));
						firstSyn = true;
					}
					bt.addStringCell(column, sb.toString());
					break;
				case Synonym:
					bt.addStringCell(column, (synonyms.size() > 0 ? synonyms.remove(0) : ""));
					break;
				default :
					throw new RuntimeException("Unexpected column type found in Sheet: " + column + " - " + SHEET.New_Concept);
			}
		}
		return extraRels;
	}
	

	/**
	 * Handle new Synonyms spreadsheet tab
	 *
	 * @param concept the concept
	 * @param bt the wb
	 * @throws Exception the exception
	 */
	private void handleNewSyn(DescriptionVersionBI<?> descVersion) throws Exception
	{	
		bt.selectSheet(SHEET.New_Synonym);
		bt.addRow();
		
		for (COLUMN column : bt.getColumnsOfSheet(SHEET.New_Synonym)) {
			switch(column)
			{
			case Topic:
				bt.addStringCell(column, ""); //User Input
				break;
			case Terminology:
				bt.addStringCell(column, getTerminology(descVersion));
				break;
			case Concept_Id:
				bt.addNumericCell(column, getSct(descVersion.getConceptNid()));
				break;
			case Term:
				bt.addStringCell(column, descVersion.getText());
				break;
			case Case_Significance:
				bt.addStringCell(column, this.getCaseSig(descVersion.isInitialCaseSignificant()));
				break;
			case Justification:
				bt.addStringCell(column, ""); //User Input
				break;
			case Note: 
				bt.addStringCell(column, "Description UUID: " + descVersion.getPrimordialUuid().toString()); 
				break;
			default :
				throw new RuntimeException("Unexpected column type found in Sheet: " + column + " - " + SHEET.New_Synonym);
			}
		}
	}
	
	/**
	 * Handle Change parent spreadsheet tab
	 *
	 * @param concept the concept
	 * @param bt the wb
	 * @throws Exception the exception
	 */
	private void handleChangeParent(ConceptVersionBI concept, RelationshipVersionBI<?> relVersion) throws Exception
	{	
		bt.selectSheet(SHEET.Change_Parent);
		bt.addRow();
		if (relVersion.getConceptNid() != concept.getConceptNid())
		{
			throw new Exception("Unexpected!");
		}
		
		for (COLUMN column : bt.getColumnsOfSheet(SHEET.Change_Parent)) {
			switch(column)
			{
				case Topic:
					bt.addStringCell(column, ""); //User Input
					break;
				case Source_Terminology:
					bt.addStringCell(column, getTerminology(concept));
					break;
				case Concept_Id:
					bt.addNumericCell(column, getSct(concept.getNid()));
					break;
				case New_Parent_Concept_Id:
					bt.addNumericCell(column, getSct(relVersion.getDestinationNid()));
					break;
				case New_Parent_Terminology:
					bt.addStringCell(column, getTerminology(OTFUtility.getConceptVersion(relVersion.getDestinationNid())));
					break;
				case Justification:
					bt.addStringCell(column, ""); //User Input
					break;
				case Note:
					bt.addStringCell(column, "");
					break;
				default :
					throw new RuntimeException("Unexpected column type found in Sheet: " + column + " - " + SHEET.Change_Parent);
			}
		}
	}
	
	private String getSemanticTag(ConceptChronicleBI concept) throws Exception {
		String fsn = OTFUtility.getFullySpecifiedName(concept);
		if (fsn.indexOf('(') != -1)
		{
			String st = fsn.substring(fsn.lastIndexOf('(') + 1, fsn.lastIndexOf(')'));
			return PICKLIST_Semantic_Tag.find(st).toString();
		} else {
			return null;
		}
	}
	
	private String getFsnWithoutSemTag(ConceptChronicleBI concept) throws Exception {
		String fsn = null;
		fsn = OTFUtility.getFullySpecifiedName(concept);
		
		String fsnOnly;
		if(fsn == null) {
			throw new Exception("FSN Could not be retreived");
		} else {
		
			fsnOnly = fsn;
			if (fsn.indexOf('(') != -1)
			{
				fsnOnly = fsn.substring(0, fsn.lastIndexOf('(') - 1);
			}
		}
		return fsnOnly;
	}
	
	private String getRelType(int nid) {
		return PICKLIST_Relationship_Type.find(OTFUtility.getConPrefTerm(nid)).toString();
	}
	
	private String getCharType(int nid) {
		String characteristic = OTFUtility.getConPrefTerm(nid); 
		//TODO: vk - Discuss the API / Spreadsheet mixup with the characteristic type
		// For now we are just going to return the preffered description retreived from
		// the characteristic type Nid until we can discuss with NLM or Jackie how to 
		// handle this problem. Inferred and stated relationships are not in the ENUM
//		return PICKLIST_Characteristic_Type.find(characteristic).toString(); // We will use this once we find a solution
		return characteristic; //But this works temporarily
	}
	
	private String getRefinability(int nid) {
		
		String desc = OTFUtility.getConPrefTerm(nid);
		String descToPicklist = desc;
		
		//Map the words optional and mandatory to their equal ENUMS b/c of API limitations
		if(desc.equals("Optional refinability")) {
			descToPicklist = "Optional";
		} else if(desc.equals("Mandatory refinability")) {
			descToPicklist = "Mandatory";
		} else {
			descToPicklist = desc;
		}
			
		return PICKLIST_Refinability.find(descToPicklist).toString();
	}
	
	private String getCaseSig(boolean caseSig) {
		if(caseSig) {
			return PICKLIST_Case_Significance.Entire_term_case_sensitive.toString();
		} else {
			return PICKLIST_Case_Significance.Entire_term_case_insensitive.toString();
		}
	}

	/**
	 * Handle new rels spreadsheet tab
	 *
	 * @param concept the concept
	 * @param bt the wb
	 * @throws Exception the exception
	 */
	public void handleNewRel(RelationshipVersionBI<?> rel) throws Exception {
		bt.selectSheet(SHEET.New_Relationship);
		if (rel.isActive() && (rel.getTypeNid() != Snomed.IS_A.getLenient().getNid())) 
		{
			bt.addRow();
			for (COLUMN column : bt.getColumnsOfSheet(SHEET.New_Relationship)) {
				switch (column) {
					case Topic:
						bt.addStringCell(column, ""); //User Input
						break;
					case Source_Terminology:
						bt.addStringCell(column, getTerminology(rel));
						break;
					case Source_Concept_Id:
						bt.addNumericCell(column, getSct(rel.getConceptNid()));
						break;
					case Relationship_Type:
						bt.addStringCell(column, getRelType(rel.getTypeNid()));
						break;
					case Destination_Terminology:
						bt.addStringCell(column, getTerminology(OTFUtility.getConceptVersion(rel.getDestinationNid())));
						break;
					case Destination_Concept_Id:
						bt.addNumericCell(column, getSct(rel.getDestinationNid()));
						break;
					case Characteristic_Type:
						bt.addStringCell(column, getCharType(rel.getCharacteristicNid()));
						break;
					case Refinability:
						bt.addStringCell(column, getRefinability(rel.getRefinabilityNid()));
						break;
					case Relationship_Group:
						bt.addNumericCell(column, rel.getGroup());
						break;
					case Justification:
						bt.addStringCell(column, "Developed as part of extension namespace " + ExtendedAppContext.getCurrentlyLoggedInUserProfile().getExtensionNamespace());
						break;
					case Note:
						bt.addStringCell(column, "This is a defining relationship expressed for the corresponding new concept request in the other tab");
						break;
					default :
						throw new RuntimeException("Unexpected column type found in Sheet: " + column + " - " + SHEET.New_Relationship);
				}
			}
		}
	}
	
	/**
	 * Figure out the appropriate terminology constant string for term related columns.
	 * 
	 * @param cv
	 * @return
	 * @throws Exception
	 */
	private String getTerminology(ComponentVersionBI cv) throws Exception {
		int moduleNid = cv.getModuleNid();
		int containingConceptNid = cv.getConceptNid();
		
		if (currentRequestMap.containsKey(containingConceptNid))
		{
			return PICKLIST_Source_Terminology.Current_Batch_Requests.toString();
		}

		//If it was done on core or us extension - assume it was pre-existing.
		//TODO This isn't 100% safe, as the user may have used this module when they did
		//a previous submission - but at the moment, we don't have any way of knowing
		//what IDs were previously submitted - so we can't choose between on of these 
		//official constants, and "New Concept Request"
		if(moduleNid == Snomed.CORE_MODULE.getNid()) {
			return PICKLIST_Source_Terminology.SNOMED_CT_International.toString();
		}
		else if (moduleNid == Snomed.US_EXTENSION_MODULE.getNid()) {
			return PICKLIST_Source_Terminology.SNOMED_CT_National_US.toString();
		}
		//These, we know would be invalid
		else if (moduleNid == IsaacMetadataAuxiliaryBinding.LOINC.getLenient().getNid()) {
			throw new Exception("Cannot export LOINC Terminology");
		}
		else if (moduleNid == IsaacMetadataAuxiliaryBinding.RXNORM.getLenient().getNid()) {
			throw new Exception("Cannot export RxNorm Terminology");
		}
		else if (!isChildOfSCT(cv.getConceptNid())) {
			throw new Exception("Cannot something that isn't part of the SCT hierarchy");
		}
		else
		{
			//The only thing we can do at this point, is assume it was a previously submitted
			//item.
			//TODO this isn't 100% safe - we need to have a permanent store of IDs that were 
			//previously submitted.
			return PICKLIST_Source_Terminology.New_Concept_Requests.toString();
		}
	}
	
	
	private long getSct(int nid) throws ContradictionException, ValidationException {
		
		ViewCoordinate vcSct = new ViewCoordinate();
		vcSct.setAllowedStatus(EnumSet.of(Status.ACTIVE, Status.INACTIVE));
		
		
		Optional<? extends ComponentVersionBI> component = OTFUtility.getComponentChronicle(nid).getVersion(vcSct);
		if(component.isPresent()) {
			Optional<? extends Long> sct = ConceptViewerHelper.getSctId(component.get());
			if(sct.isPresent()) {
				return sct.get();
			}
		}
		
		ConceptVersionBI conceptVersion = OTFUtility.getConceptVersion(nid, vcSct);
		if(conceptVersion != null) {
			Optional<? extends Long> sct = ConceptViewerHelper.getSctId(conceptVersion);
			if(sct.isPresent()) {
				return sct.get();
			}
		}
		
		//TODO this isn't 100% safe - we shouldn't just assume that it is going to be a new request.
		//It may be a request from a previous submission.  We need to have someplace to store the IDs 
		//we used in previous submissions.
		if(!currentRequestMap.containsKey(nid)) {
			currentRequestMap.put(nid, globalRequestCounter.getAndIncrement());
		}
		
		return currentRequestMap.get(nid);
	}
	
	/**
	 * Handle Change Relationships spreadsheet tab
	 *
	 * @param concept the concept
	 * @param bt the wb
	 * @throws Exception the exception
	 */
	private void handleChangeRels(RelationshipVersionBI<?> relVersion) throws Exception
	{
		bt.selectSheet(SHEET.Change_Relationship);
		bt.addRow();
		for (COLUMN column : bt.getColumnsOfSheet(SHEET.Change_Relationship))
		{
			switch (column)
			{
				case Topic:
					bt.addStringCell(column, "");
					break;
				case Source_Concept_Id:
					bt.addNumericCell(column, getSct(relVersion.getConceptNid()));
					break;
				case Relationship_Id:  
					bt.addNumericCell(column, getSct(relVersion.getNid()));
					break;
				case Relationship_Type: 
					bt.addStringCell(column, getRelType(relVersion.getTypeNid()));
					break;
				case Source_Terminology:
					bt.addStringCell(column, getTerminology(relVersion));
					break;
				case Destination_Concept_Id:
					bt.addNumericCell(column, getSct(relVersion.getDestinationNid()));
					break;
				case Destination_Terminology:
					bt.addStringCell(column, getTerminology(OTFUtility.getConceptVersion(relVersion.getDestinationNid())));
					break;
				case Characteristic_Type:
					bt.addStringCell(column, getCharType(relVersion.getCharacteristicNid()));
					break;
				case Refinability:
					bt.addStringCell(column, getRefinability(relVersion.getRefinabilityNid()));
					break;
				case Relationship_Group:
					bt.addNumericCell(column, relVersion.getGroup());
					break;
				case Justification:
					bt.addStringCell(column, ""); ///User Input
					break;
				case Note:
					bt.addStringCell(column, ""); //User Input
					break;
				default :
					throw new RuntimeException("Unexpected column type found in Sheet: " + column + " - " + SHEET.Change_Relationship);
			}
		}
	}
	
	/**
	 * Pass in an ArrayList of description versions and a workbook and a new row will be created for each description 
	 * in the corresponding notebook
	 *
	 * @param ArrayList<DescriptionVersionBI<?>> descVersion an ArrayList of DescriptionVersions that will be added
	 * @param bt the wb
	 * @throws Exception the exception
	 */
	private void handleChangeDesc(DescriptionVersionBI<?> d) throws Exception
	{
		bt.selectSheet(SHEET.Change_Description);
		bt.addRow();
		for (COLUMN column : bt.getColumnsOfSheet(SHEET.Change_Description))
		{
			switch (column)
			{
				case Topic:
					bt.addStringCell(column, "");
					break;
				case Terminology:
					bt.addStringCell(column, getTerminology(d));
					break;
				case Concept_Id:
					bt.addNumericCell(column, getSct(d.getConceptNid()));
					break;
				case Description_Id:
					bt.addNumericCell(column, getSct(d.getNid()));
					break;
				case Term: 
					bt.addStringCell(column, d.getText());
					break;
				case Case_Significance:
					bt.addStringCell(column, getCaseSig(d.isInitialCaseSignificant()));
					break;
				case Justification:
					bt.addStringCell(column, "");
					break;
				case Note:
					bt.addStringCell(column, "");
					break;
				default :
					throw new RuntimeException("Unexpected column type found in Sheet: " + column + " - " + SHEET.Change_Description);
			}
		}
	}
	
	/**
	 * Handle Retire Concept spreadsheet tab
	 *
	 * @param concept the concept
	 * @param bt the wb
	 * @throws Exception the exception
	 */
	private void handleRetireConcept(ConceptVersionBI concept) throws Exception
	{
		bt.selectSheet(SHEET.Retire_Concept);
		bt.addRow();
		for (COLUMN column : bt.getColumnsOfSheet(SHEET.Retire_Concept))
		{
			switch (column)
			{
				case Topic:
					break;
				case Terminology:
					bt.addStringCell(column, getTerminology(concept));
					break;
				case Concept_Id:
					bt.addNumericCell(column, getSct(concept.getNid()));
					break;
				case Change_Concept_Status_To: 
					bt.addStringCell(column, "");
					break;
				case Duplicate_Concept_Id:  //TODO: vk - - possibly userinput - if they are deactivating because it is a dupe, we need the SCTID of the dupe here
					bt.addStringCell(column, "");
					break;
				case Justification:
					bt.addStringCell(column, "");
					break;
				case Note:
					bt.addStringCell(column, "");
					break;
				default :
					throw new RuntimeException("Unexpected column type found in Sheet: " + column + " - " + SHEET.Retire_Concept);
			}
		}
	}
	
	/**
	 * Handle Retire Description spreadsheet tab
	 *
	 * @param concept the concept
	 * @param bt the wb
	 * @throws Exception the exception
	 */
	private void handleRetireDescription(DescriptionVersionBI<?> d) throws Exception
	{
		bt.selectSheet(SHEET.Retire_Description);
		bt.addRow();
		for (COLUMN column : bt.getColumnsOfSheet(SHEET.Retire_Description))
		{
			switch (column)
			{
				case Topic:
					bt.addStringCell(column, ""); //User Input
					break;
				case Terminology:
					bt.addStringCell(column, getTerminology(d));
					break;
				case Concept_Id:
					bt.addNumericCell(column, this.getSct(d.getConceptNid()));
					break;
				case Description_Id:
					bt.addNumericCell(column, this.getSct(d.getNid()));
					break;
				case Change_Description_Status_To:  //TODO talk to Jaqui / NLM - same status question as above
					break;
				case Justification:
					bt.addStringCell(column, ""); //User Input
					break;
				case Note:
					bt.addStringCell(column, ""); //User Input
					break;
				default :
					throw new RuntimeException("Unexpected column type found in Sheet: " + column + " - " + SHEET.Retire_Description);
			}
		}
	}

	/**
	 * Handle Retire Relationship spreadsheet tab
	 *
	 * @param concept the concept
	 * @param bt the wb
	 * @throws Exception the exception
	 */
	private void handleRetireRelationship(RelationshipVersionBI<?> relVersion) throws Exception
	{
		bt.selectSheet(SHEET.Retire_Relationship);
		bt.addRow();
		for (COLUMN column : bt.getColumnsOfSheet(SHEET.Retire_Relationship))
		{
			switch (column)
			{
				case Topic:
					bt.addStringCell(column, ""); //User Input
					break;
				case Source_Terminology:
					bt.addStringCell(column, getTerminology(relVersion));
					break;
				case Source_Concept_Id:
					//Yes, the relVersion conceptNid is the same as the source concept (at least, it should be)
					bt.addNumericCell(column, getSct(relVersion.getConceptNid()));
					break;
				case Relationship_Id:  
					bt.addNumericCell(column, getSct(relVersion.getNid()));
					break;
				case Destination_Terminology:
					bt.addStringCell(column, getTerminology(OTFUtility.getConceptVersion(relVersion.getDestinationNid())));
					break;
				case Destination_Concept_Id:
					bt.addNumericCell(column, getSct(relVersion.getDestinationNid()));
					break;
				case Relationship_Type:
					bt.addStringCell(column, this.getRelType(relVersion.getTypeNid()));
					break;
				case Justification:
					bt.addStringCell(column, ""); //User Input
					break;
				case Note:
					bt.addStringCell(column, ""); //User Input
					break;
				default :
					throw new RuntimeException("Unexpected column type found in Sheet: " + column + " - " + SHEET.Retire_Relationship);
			}
		}

	}
	
	/**
	 * Handle Add Parent spreadsheet tab
	 *
	 * @param concept the concept
	 * @param bt the wb
	 * @throws Exception the exception
	 */
	private void handleNewParent(RelationshipVersionBI<?> rel) throws Exception
	{
		bt.selectSheet(SHEET.Add_Parent);
		if (rel.isActive() && (rel.getTypeNid() == Snomed.IS_A.getLenient().getNid())) 
		{
			bt.addRow();
			for (COLUMN column : bt.getColumnsOfSheet(SHEET.Add_Parent))
			{
				switch (column)
				{
					case Topic:
						bt.addStringCell(column, "");
						break;
					case Source_Terminology: 
						bt.addStringCell(column, getTerminology(rel));
						break;
					case Child_Concept_Id:
						bt.addNumericCell(column, getSct(rel.getConceptNid()));
						break;
					case Destination_Terminology:
						bt.addStringCell(column, getTerminology(OTFUtility.getConceptVersion(rel.getDestinationNid())));
						break;
					case Parent_Concept_Id:  
						bt.addNumericCell(column, getSct(rel.getDestinationNid()));
						break;
					case Justification:
						bt.addStringCell(column, "");
						break;
					case Note:
						bt.addStringCell(column, "");
						break;
					default :
						throw new RuntimeException("Unexpected column type found in Sheet: " + column + " - " + SHEET.Add_Parent);
				}
			}
		}
	}
	
	/**
	 * Handle Other spreadsheet tab
	 *
	 * @param concept the concept
	 * @param bt the wb
	 * @throws Exception the exception
	 */
	@SuppressWarnings("unused")
	private void handleOther(ConceptChronicleBI concept) throws Exception
	{
		bt.selectSheet(SHEET.Other);
		bt.addRow();
		for (COLUMN column : bt.getColumnsOfSheet(SHEET.Other))
		{
			switch (column)
			{
				case Topic:
					bt.addStringCell(column, ""); //User Input
					break;
				case Description:
					break;
				case Justification:
					bt.addStringCell(column, ""); //User Input
					break;
				case Note:
					bt.addStringCell(column, ""); //User Input
					break;
				default :
					throw new RuntimeException("Unexpected column type found in Sheet: " + column + " - " + SHEET.Other);
			}
		}
	}
	
	private static boolean isChildOfSCT(int conceptNid) throws IOException, ContradictionException 
	{
		return ExtendedAppContext.getDataStore().isChildOf(conceptNid, IsaacMetadataAuxiliaryBinding.HEALTH_CONCEPT.getNid(), OTFUtility.getViewCoordinate());
	}
}
