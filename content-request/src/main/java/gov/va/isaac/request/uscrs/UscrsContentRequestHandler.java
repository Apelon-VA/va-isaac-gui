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
import org.ihtsdo.otf.tcc.api.relationship.RelationshipType;
import org.ihtsdo.otf.tcc.api.relationship.RelationshipVersionBI;
import org.ihtsdo.otf.tcc.api.spec.ValidationException;
import org.ihtsdo.otf.tcc.api.store.Ts;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import gov.va.isaac.ExtendedAppContext;
import gov.va.isaac.interfaces.gui.constants.SharedServiceNames;
import gov.va.isaac.interfaces.gui.views.commonFunctionality.ExportTaskHandlerI;
import gov.va.isaac.request.uscrs.USCRSBatchTemplate.COLUMN;
import gov.va.isaac.request.uscrs.USCRSBatchTemplate.PICKLIST_Case_Significance;
import gov.va.isaac.request.uscrs.USCRSBatchTemplate.PICKLIST_Characteristic_Type;
import gov.va.isaac.request.uscrs.USCRSBatchTemplate.PICKLIST_Refinability;
import gov.va.isaac.request.uscrs.USCRSBatchTemplate.PICKLIST_Relationship_Type;
import gov.va.isaac.request.uscrs.USCRSBatchTemplate.PICKLIST_Semantic_Tag;
import gov.va.isaac.request.uscrs.USCRSBatchTemplate.PICKLIST_Source_Terminology;
import gov.va.isaac.request.uscrs.USCRSBatchTemplate.SHEET;
import gov.va.isaac.util.OTFUtility;
import gov.va.isaac.util.OchreUtility;
import gov.vha.isaac.metadata.source.IsaacMetadataAuxiliaryBinding;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.impl.utility.Frills;
import javafx.concurrent.Task;


/**
 * USCRS implementation of a {@link ExportTaskHandlerI}.
 *
 * @author bcarlsenca
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 * @author <a href="mailto:vkaloidis@apelon.com">Vas Kaloidis</a>
 */
@Service @Named(value=SharedServiceNames.USCRS)
@PerLookup
public class UscrsContentRequestHandler implements ExportTaskHandlerI
{
	Properties prop = new Properties();
	boolean filter = false;
	String invalidPropFound = "none";
	Long previousReleaseTime;
	int namespace = 0;
	ArrayList<String> propKeys = new ArrayList<String>();
	
	int changeDescCount = 0;
	
	 //Enable a (checker) that throws an error if the "Request-ID's" are not
	//  generated correctly (Request-IDs are placed both in the 'newConceptRequest' 
	//  HashMap and 'newConceptRequestIds' HashSet), this checks both are set correctly
	boolean dateFilterChecking = false;
	
	// Generate Useful Testing Data in Spreadsheet for debugging. ** Setet to FALSE in production **
	boolean testing = true;
	
	
	private UscrsContentRequestHandler() {
		//hk2
	}
	
	/*
	 * Pass in properties to configure the USCRS Export, such as filters.
	 * 
	 * Filters Supported: 
	 * 
	 * Date (Long): Pass in the long date and the exporter will only export
	 * concepts and the corresponding components that have been modified or
	 * created on or after the date passed-in. The lack of a date filter will
	 * result in the exporter treating all concepts and components as new.
	 * 
	 * (non-Javadoc)
	 * @see gov.va.isaac.interfaces.gui.views.commonFunctionality.ExportTaskHandlerI#setOptions(java.util.Properties)
	 */
	@Override
	public void setOptions(Properties options) throws Exception {
		prop = options;

		//Propkey holds all VALID Property Keys (filters etc..). 
		// Place accepted KEYS HERE and add logic below
		propKeys.add("date");
		propKeys.add("namespace");
		
		//Logic for Property Keys, Set filters here
		if(prop.containsKey("date")) {
			previousReleaseTime = Long.parseLong(prop.getProperty("date")); 
			logger.info("USCRS Handler - previous release time is " + previousReleaseTime);
		} else {
			previousReleaseTime = Long.MIN_VALUE;
		}
		
		if(prop.containsKey("namespace")) {
			namespace = Integer.valueOf(prop.getProperty("namespace"));
			logger.info("USCRS Handler - namespace is " + namespace);
		} else {
			namespace = 0;
		}
		//Add any filters (that change the export logic) here. IE: Author, Date or any other modifications to export, set filter to true
		if(previousReleaseTime != Long.MIN_VALUE) {
			filter = true;
			logger.info("USCRS Handler - properties contains date key");
		} else {
			filter = false;
		}
		
		prop.keySet().stream().forEach(p -> { 
											if(!propKeys.contains(p.toString())) {
												invalidPropFound = String.valueOf(p);
											}
										});
		if(!invalidPropFound.equals("none")) {
			throw new Exception("Invalid property set: " + invalidPropFound);
		}
		
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
	private HashSet<Integer> newConceptRequestIds = new HashSet<>();
	private USCRSBatchTemplate bt = null;
	private int examinedConCount = 0;
	private ViewCoordinate vcPreviousRelease;
	private ViewCoordinate viewCoordinate;
	private ViewCoordinate vcAllStatus;
	
	/**
	 * Creates a USCRS Content Request Exporter Task by taking in an IntStream 
	 * of Concept NID's and the desired location to place the generated
	 * content-request.
	 * 
	 * To pass-in a date filter, set a 'date' property and pass in the Properties
	 * object to the setOptions() method.
	 *  
	 *	(non-Javadoc)
	 * @see gov.va.isaac.interfaces.gui.views.commonFunctionality.ExportTaskHandlerI#createTask(java.util.stream.IntStream, java.nio.file.Path)
	 */
	@Override
	public Task<Integer> createTask(IntStream intStream, Path file) 
	{
		return new Task<Integer>() {
			
			@SuppressWarnings("rawtypes")
			@Override
			protected Integer call() throws Exception {
				updateTitle("Beginning Uscrs Content Request Export Operation");
				
				try {
					bt = new USCRSBatchTemplate(USCRSBatchTemplate.class.getResourceAsStream("/USCRS_Batch_Template-2015-01-27-Id-Cell-Formatted.xls"));
					logger.info("Batch Template Generated");
				} catch (IOException e) {
					logger.error("Error generating USCRS Batch Template, or loading Excel template", e);
				}
				
				viewCoordinate = OTFUtility.getViewCoordinate();
				viewCoordinate.getViewPosition().setTime(System.currentTimeMillis());
				viewCoordinate.setAllowedStatus(EnumSet.of(Status.ACTIVE));
				viewCoordinate.setRelationshipAssertionType(RelAssertionType.STATED);
				
				vcAllStatus = OTFUtility.getViewCoordinateAllowInactive();
				
				logger.info("USCRS Content Request Handler - Starting Concept Stream Iterator");
				logger.debug("USCRS Date Filter " + previousReleaseTime);
				intStream.forEach( nid -> {	
						
						if(examinedConCount % 100 == 0) {
							updateTitle("Uscrs Content Request Exporter Progress - We have exported " + examinedConCount + " components / concepts");
						}
						if(isCancelled()) {
							logger.info("User canceled Uscrs Export Operation");
							throw new RuntimeException("User canceled operation");
						}
						
						ConceptVersionBI concept = OTFUtility.getConceptVersion(nid); 
						ArrayList<RelationshipVersionBI<?>> exportRelsUnFiltered = new ArrayList<RelationshipVersionBI<?>>();
						
						//TODO: Start breaking this code up into sub-methods. Because we will be adding more filters
						if(filter) {
							if(prop.containsKey("date") && previousReleaseTime != Long.MIN_VALUE) { 
								boolean conceptCreated = false;
								try {
									vcPreviousRelease = OTFUtility.getViewCoordinate();
									vcPreviousRelease.getViewPosition().setTime(previousReleaseTime);
									vcPreviousRelease.setAllowedStatus(EnumSet.of(Status.ACTIVE));
									
									//Export Concept
									ConceptAttributeChronicleBI cac = concept.getConceptAttributes();
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
												//TODO: Handle Concept Changes
												//conceptCreated = true;
											} else {
												//noop
											}
										} else {
											handleRetireConcept(concept);
										}
									} else {
										if(caLatest.isPresent()) {
											ViewCoordinate vcPrActiveInactive = vcPreviousRelease;
											OTFUtility.getViewCoordinateAllowInactive();
											vcPrActiveInactive.getAllowedStatus().add(Status.INACTIVE);
											vcPrActiveInactive.getAllowedStatus().add(Status.ACTIVE);
											
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
									
								} catch (Exception e) {
									logger.error("Error getting concept " + nid + " attributes for date / time comparison", e);
								}
								
								//Export Descriptions
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
												handleRetireDescription(dvInitial.get());
											}
										} else {
											if(dvLatest.isPresent()) {
												ViewCoordinate vcPrActiveInactive = vcPreviousRelease;
												vcPrActiveInactive.getAllowedStatus().add(Status.INACTIVE);
												vcPrActiveInactive.getAllowedStatus().add(Status.ACTIVE);
												
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
									logger.error("Description Export Error", e);
								}
								
								//Export Relationships
								if(conceptCreated) {
									logger.debug("USCRS Handler - Concept was already created, handeling components accordingly (skip first 3 ISA relationships");
								} else {
									logger.debug("Concept NOT previously created - generating relationships now instead");
									try {
										Collection<? extends RelationshipChronicleBI> outgoingRels = concept.getRelationshipsOutgoing();
										Collection<? extends RelationshipChronicleBI> incomingRels = concept.getRelationshipsIncoming();
										outgoingRels.stream()
										.forEach( r -> {
														Optional<? extends RelationshipVersionBI<?>> thisVersion = null;
														try {
															thisVersion = r.getVersion(vcAllStatus);
														} catch (Exception e) {
															logger.error("Error getting relationship version on the proper View Coordinate", e);
														}
														if(thisVersion.isPresent()) {
															RelationshipVersionBI<?> tvg = thisVersion.get();
															exportRelsUnFiltered.add(tvg);
														}
														
													});
										
									} catch (Exception e) {
										logger.error("Error retreiving the incoming relationships", e);
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
												
												boolean hasRelAttrChange = false;
												if(thisRvLatest.getCharacteristicNid() != thisRvInitial.getCharacteristicNid()) {
													hasRelAttrChange = true;
												} else if(thisRvLatest.getGroup() != thisRvInitial.getGroup()) {
													hasRelAttrChange = true;
												} else if (thisRvLatest.getRefinabilityNid() != thisRvInitial.getRefinabilityNid()) {
													hasRelAttrChange = true;
												} else if (thisRvLatest.isInferred() != thisRvInitial.isInferred()) {
													hasRelAttrChange = true;
												} else if (thisRvLatest.isStated() != thisRvInitial.isStated()) {
													hasRelAttrChange = true;
												}
												
												if(hasRelAttrChange) {
													if (thisRvLatest.getTypeNid() == Snomed.IS_A.getLenient().getNid()) {
														handleChangeParent(thisRvLatest);
													} else {
														handleChangeRels(thisRvLatest);
													}
												}
											} else {
												handleRetireRelationship(rvInitial.get());
											}
										} else {
											if(rvLatest.isPresent()) {
												RelationshipVersionBI<?> rvGetLatest = rvLatest.get();
												ViewCoordinate rvPrActiveInactive = vcPreviousRelease;
												rvPrActiveInactive.getAllowedStatus().add(Status.INACTIVE);
												rvPrActiveInactive.getAllowedStatus().add(Status.ACTIVE);
												
												Optional<? extends RelationshipVersionBI<?>> rvCheckRetired = rv.getVersion(rvPrActiveInactive);
												if(rvCheckRetired.isPresent()) {
													RelationshipVersionBI<?> retiredRel = rvCheckRetired.get();
													if (retiredRel.getTypeNid() == Snomed.IS_A.getLenient().getNid()) {
														handleChangeParent(retiredRel);
													} else {
														handleChangeRels(retiredRel);
													}
												} else {
													if (rvGetLatest.isActive() && (rvGetLatest.getTypeNid() == Snomed.IS_A.getLenient().getNid())) {
														handleNewParent(rvGetLatest);
													} else {
														handleNewRel(rvGetLatest);  
													}
												}
											} else {
												//noop
											}
										}
									} catch (Exception e) {
										logger.error("Error exporting Relationship", e);
									}
								}
							} else {
								//No date filter, process everything that way
								logger.debug("USCRS Handler -Filter Set. Not a Date Filter. Exporting all concepts.");
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
									logger.error("Could not export concept " + nid, e);
								} 
							}
						} else {
							 //Export ALL - (No Filter)
							logger.debug("USCRS Handler -Exporting all concepts, no filters");
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
								logger.error("Could not export concept " + nid, e);
							} 
							
						}
						logger.info("USCRS Content Request Handler - ITERATED CONCEPT " + examinedConCount++);
				});
				
				logger.info("*** EXPORT FINISHED *** Concept Stream Iterator: " + examinedConCount + " concepts iterated");
				
				if(dateFilterChecking) {
					for (int genId : currentRequestMap.values())
					{
						if (!newConceptRequestIds.contains(genId))
						{
							throw new Exception("We wrote out the generated ID: " + genId + " but failed to create a new concept for that ID.  Logic failure!");
						}
					}
				}
				
				if(isCancelled()) {
					return examinedConCount;
				}
				
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
	 *  Prints the column cell, but if testing is disabled, just print nothing in the cell.
	 * @param nid
	 * @throws ContradictionException 
	 * @throws ValidationException 
	 */
	private void getNote(int nid) throws ValidationException, ContradictionException {
		this.getNote(nid, "");
	}
	
	/**
	 * Create a Note Cell, and print the row's NID and Description for testing. If testing is
	 *  disabled then print the value of alternateNote
	 * @param nid
	 * @param alternateNote if testing is disabled, print this instead
	 * @throws ContradictionException 
	 * @throws ValidationException 
	 */
	private void getNote(int nid, String alternateNote) throws ValidationException, ContradictionException {
		if(testing) {
			bt.addStringCell(COLUMN.Note, "SCT ID: " + getSct(nid));
		} else {
			bt.addStringCell(COLUMN.Note, alternateNote);
		}
	}

	/**
	 * Takes a concept and it returns the semantic tag, pulled from the FSN, 
	 * and selected from the PICKLIST (todo: enable PICKLIST selection)
	 * @param concept
	 * @return the Semantic tag from the FSN
	 * @throws Exception
	 */
	private String getSemanticTag(ConceptChronicleBI concept) throws Exception {
		String fsn = OchreUtility.getFSNForConceptNid(concept.getNid(), null).get();
		if (fsn.indexOf('(') != -1)
		{
			String st = fsn.substring(fsn.lastIndexOf('(') + 1, fsn.lastIndexOf(')'));
			try {
				return PICKLIST_Semantic_Tag.find(st).toString();
			} catch(Exception e) {
				logger.error("Error choosing Semtantic Tag for " + fsn + " from PICKLIST USCRS Batch Templte for Concept UUID: " + concept.getPrimordialUuid());
			}
			return st; //T0DO** FIX Task: The picklist does not support all the various semantic tags that the API returns
		} else {
			return null;
		}
	}
	
	/**
	 * This returns the semantic tag, with no regards to the PICKLIST. 
	 * @param concept
	 * @return
	 * @throws Exception
	 */
	private String getTopic(ConceptChronicleBI concept) throws Exception {
		String fsn = OchreUtility.getFSNForConceptNid(concept.getNid(), null).get();
		if (fsn.indexOf('(') != -1)
		{
			String st = fsn.substring(fsn.lastIndexOf('(') + 1, fsn.lastIndexOf(')'));
			return st;
		} else {
			return null;
		}
	}
	
	/**
	 * Returns the FSN, with the sematic tag removed.
	 * 
	 * @param concept
	 * @return FSN with-out the semantic tag
	 * @throws Exception
	 */
	private String getFsnWithoutSemTag(ConceptChronicleBI concept) throws Exception {
		String fsn = OchreUtility.getFSNForConceptNid(concept.getNid(), null).get();
		String fsnOnly = fsn;
		if (fsn.indexOf('(') != -1)
		{
			fsnOnly = fsn.substring(0, fsn.lastIndexOf('(') - 1);
		}
		return fsnOnly;
	}
	
	/**
	 * Takes the Relationships NID and returns the Relationship Type from the PICKLIST
	 * 
	 * @param nid of the relationship
	 * @return String of the relationship type
	 */
	private String getRelType(int nid) {
		String relType = OchreUtility.getPreferredTermForConceptNid(nid, null, null).get();
		if(relType.equalsIgnoreCase("is-a")) {
			relType = "Is a";
		}
		return PICKLIST_Relationship_Type.find(relType).toString();
	}
	
	private String getJustification() {
		
		if(namespace != 0) {
			return "Developed as part of extension namespace " + String.valueOf(namespace); 
		} else {
			String userNamespace = "";
			try {
				userNamespace = ExtendedAppContext.getCurrentlyLoggedInUserProfile().getExtensionNamespace();
			} catch (Exception e) {
				logger.error("Error getting namespace for Justification column", e);
			}
			if(userNamespace.trim()!= "") {
				return userNamespace;
			} else {
				return "";
			}
		}
	}
	
	/**
	 * Takes the relationship NID and returns the characteristic type of that relationship from PICKLIST (todo PICKLIST)
	 * 
	 * 	TODO: Discuss the API / Spreadsheet mixup with the characteristic type
	 *    For now we are just going to return the preffered description retreived from
	 *    the characteristic type Nid until we can discuss with NLM or Jackie how to 
	 *    handle this problem. Inferred and stated relationships are not in the ENUM
	 *    
	 *    PICKLIST: Qualifying, Additional, Defining
	 * 
	 * @param nid of the relationship
	 * @return characteristic type from PICKLIST
	 */
	private String getCharType(int nid) {
		String characteristic = OchreUtility.getPreferredTermForConceptNid(nid, null, null).get();
		if(characteristic.equalsIgnoreCase("stated")) {
			return RelationshipType.STATED_ROLE.toString();
			//return PICKLIST_Characteristic_Type.Defining_relationship.toString(); 
		} else if(characteristic.equalsIgnoreCase("other-term")) {
			return PICKLIST_Characteristic_Type.Qualifying_relationship.toString(); //TOODO: Map Correct
		} else if(characteristic.equalsIgnoreCase("other-term")) {
			return PICKLIST_Characteristic_Type.Additional_relationship.toString(); //TOODO: Map Correct
		}
		
		return characteristic; //But this works temporarily
	}
	
	private String getRefinability(int nid) {
		
		String desc = OchreUtility.getPreferredTermForConceptNid(nid, null, null).get();
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
	 * Figure out the appropriate terminology constant string for term related columns.
	 * 
	 * @param cv
	 * @return
	 * @throws Exception
	 */
	private String getTerminology(ComponentVersionBI cv) throws Exception {
		int moduleNid = cv.getModuleNid();
		int containingConceptNid = cv.getAssociatedConceptNid();
		
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
		else if (!isChildOfSCT(cv.getAssociatedConceptNid())) {
			logger.error("Cannot export concepts or components that are not in SCT");
			return "NOT in SCT";
			//TODO: Fix this throw new Exception("Cannot something that isn't part of the SCT hierarchy");
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
		boolean isTest = false;
//		if(jesseTest) {
//			if(OTFUtility.getConceptVersion(nid).getPrimordialUuid().compareTo(UUID.fromString("8d75e292-f957-3a04-a570-8462ab9b336b")) == 0) {
//				//TODO: Remove this hard-code test
//				isTest = true;
//			}
//		}
		Optional<? extends Long> sct = Frills.getSctId(nid, null);
		if(sct.isPresent() && !isTest) {
			return sct.get();
		} else if(isTest) {
			// Test UUID: 8d75e292-f957-3a04-a570-8462ab9b336b
			// Test SCT: 447633005
			//TODO this isn't 100% safe - we shouldn't just assume that it is going to be a new request.
			//It may be a request from a previous submission.  We need to have someplace to store the IDs 
			//we used in previous submissions.
			if(!currentRequestMap.containsKey(nid)) {
				currentRequestMap.put(nid, globalRequestCounter.getAndIncrement());
			}
			return currentRequestMap.get(nid);
		}
		return nid;
	}
	
	private static boolean isChildOfSCT(int conceptSequenceOrNid) 
	{
		return Get.taxonomyService().isChildOf(conceptSequenceOrNid, IsaacMetadataAuxiliaryBinding.HEALTH_CONCEPT.getConceptSequence(), 
				ExtendedAppContext.getUserProfileBindings().getTaxonomyCoordinate().get());
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
		
		int thisNewReqId = 0;
		
		int count = 0;
		for (RelationshipChronicleBI rel : concept.getRelationshipsOutgoing())
		{
			Optional<? extends RelationshipVersionBI<?>> relOption = rel.getVersion(vcAllStatus);
			if(relOption.isPresent()) {
				RelationshipVersionBI relVersion = relOption.get();
				if(relVersion.isActive()) 
				{
					if ((relVersion.getTypeNid() == Snomed.IS_A.getLenient().getNid())) 
					{
						int relDestNid = relVersion.getDestinationNid();
						parentNids.add(count, relDestNid);

						parentsTerms.add(count, this.getTerminology(
								Ts.get().getComponentVersion(OTFUtility.getViewCoordinate(), relDestNid).get()));
						
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
			Optional<? extends DescriptionVersionBI> descVersionOpt= desc.getVersion(OTFUtility.getViewCoordinate());
			if(descVersionOpt.isPresent()) {
				DescriptionVersionBI<?> descVersion = descVersionOpt.get();
				// Synonyms: find active, non FSN descriptions not matching the preferred name
				if (descVersion.isActive() && (descVersion.getTypeNid() != Snomed.FULLY_SPECIFIED_DESCRIPTION_TYPE.getLenient().getNid())
						&& !descVersion.getText().equals(OchreUtility.getPreferredTermForConceptNid(concept.getNid(), null, null).get()))
				{
					synonyms.add(descVersion.getText());
				}
				//Definition
				if(descVersion.getTypeNid() == Snomed.DEFINITION_DESCRIPTION_TYPE.getLenient().getNid()
						&& descVersion.isActive()){
					definitions.add(descVersion.getText());
				}
			}
		}

		bt.selectSheet(SHEET.New_Concept);
		bt.addRow();
		for (COLUMN column : bt.getColumnsOfSheet(SHEET.New_Concept))
		{
			switch (column)
			{
				case Request_Id:
					//long reqId = getSct(concept.getNid());
					//if (reqId > Integer.MAX_VALUE) //TODO: This isn't 100% safe when detecting if we got an SCT or not. It's just guessing.
					//{
					//	if(dateFilterChecking) {
					//		throw new RuntimeException("We appear to have found an SCTID when we only expected a generated sequence ID");
					//	} else {
					//		break; //TODO: Verify this doesen't leave blank rows
					//	}
					//}
					//newConceptRequestIds.add((int)reqId);
					//bt.addNumericCell(column, reqId);
					bt.addStringCell(column, "");
					break;
				case Topic:
					bt.addStringCell(column, this.getTopic(concept));
					break;
				case Local_Code:
					bt.addStringCell(column, concept.getPrimordialUuid().toString());
					break;
				case Local_Term: 
					bt.addStringCell(column, OchreUtility.getPreferredTermForConceptNid(concept.getNid(), null, null).get());
					break;
				case Fully_Specified_Name:
					bt.addStringCell(column, this.getFsnWithoutSemTag(concept));
					break;
				case Semantic_Tag:
					bt.addStringCell(column, this.getSemanticTag(concept));
					break;
				case Preferred_Term:
					bt.addStringCell(column, OchreUtility.getPreferredTermForConceptNid(concept.getNid(), null, null).get());
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
					bt.addStringCell(column, "Needed for VA purposes");
					break;
				case Proposed_Use:
					bt.addStringCell(column, ""); //User Input
					break;
				case Justification:
					bt.addStringCell(column, getJustification());
					break;
				case Note:
					StringBuilder sb = new StringBuilder();
					
					//sb.append("SCT ID:" + this.getSct(-2143244556));
					
					Optional<? extends ConceptAttributeVersionBI> conceptDefined = concept.getConceptAttributes().getVersion(OTFUtility.getViewCoordinate());
					if(conceptDefined.isPresent()) {
						if (conceptDefined.get().isDefined())
						{
							sb.append("NOTE: this concept is fully defined. ");
						}
					}
					
					//Extra Definitions
					if(definitions.size() > 0) {
						sb.append("Definitions: ");
					}
					while(definitions.size() > 0) 
					{
						if(definitions.size() > 0)
						{
							sb.append(definitions.remove(0));
						} 
						if(definitions.size() > 0) {
							sb.append(", ");
						}
					}
					
					
					//Extra Synonyms
					boolean firstSyn = false;
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
					if(testing) {
						sb.append("Concept: " + concept.getNid()+ " - " + OchreUtility.getPreferredTermForConceptNid(concept.getNid(), null, null).get());
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
				bt.addStringCell(column, this.getTopic(OTFUtility.getConceptVersion(descVersion.getConceptNid())));
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
				bt.addStringCell(column, getJustification());
				break;
			case Note: 
				getNote(descVersion.getNid());
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
	 * @throws Exception the exception
	 */
	private void handleChangeParent(RelationshipVersionBI<?> rel) throws Exception
	{	
		if (rel.getTypeNid() == Snomed.IS_A.getLenient().getNid()) 
		{
			bt.selectSheet(SHEET.Change_Parent);
			bt.addRow();
			for (COLUMN column : bt.getColumnsOfSheet(SHEET.Change_Parent)) {
				switch(column)
				{
					case Topic:
						bt.addStringCell(column, this.getTopic(OTFUtility.getConceptVersion(rel.getConceptNid())));
						break;
					case Source_Terminology:
						bt.addStringCell(column, getTerminology(OTFUtility.getConceptVersion(rel.getConceptNid())));
						break;
					case Concept_Id:
						bt.addNumericCell(column, getSct(rel.getConceptNid()));
						break;
					case New_Parent_Concept_Id:
						bt.addNumericCell(column, getSct(rel.getDestinationNid()));
						break;
					case New_Parent_Terminology:
						bt.addStringCell(column, getTerminology(OTFUtility.getConceptVersion(rel.getDestinationNid())));
						break;
					case Justification:
						if(ExtendedAppContext.getCurrentlyLoggedInUserProfile().getExtensionNamespace() != null) {
							bt.addStringCell(column, "Developed as part of extension namespace " + ExtendedAppContext.getCurrentlyLoggedInUserProfile().getExtensionNamespace());
						} else {
							bt.addStringCell(column, "");
						}
						break;
					case Note:
						getNote(rel.getDestinationNid());
						break;
					default :
						throw new RuntimeException("Unexpected column type found in Sheet: " + column + " - " + SHEET.Change_Parent);
				}
			}
		}
	}
	
	/**
	 * Handle new rels spreadsheet tab
	 *
	 * @param concept the concept
	 * @throws Exception the exception
	 */
	private void handleNewRel(RelationshipVersionBI<?> rel) throws Exception {
		if (rel.getTypeNid() != Snomed.IS_A.getLenient().getNid()) 
		{
			bt.selectSheet(SHEET.New_Relationship);
			bt.addRow();
			for (COLUMN column : bt.getColumnsOfSheet(SHEET.New_Relationship)) {
				switch (column) {
					case Topic:
						bt.addStringCell(column, this.getTopic(OTFUtility.getConceptVersion(rel.getConceptNid())));
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
						if(ExtendedAppContext.getCurrentlyLoggedInUserProfile().getExtensionNamespace() != null) {
							bt.addStringCell(column, "Developed as part of extension namespace " + ExtendedAppContext.getCurrentlyLoggedInUserProfile().getExtensionNamespace());
						} else {
							bt.addStringCell(column, "");
						}
						break;
					case Note:
						getNote(rel.getNid(), "This is a defining relationship expressed for the corresponding new concept request in the other tab");
						break;
					default :
						throw new RuntimeException("Unexpected column type found in Sheet: " + column + " - " + SHEET.New_Relationship);
				}
			}
		}
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
		if (relVersion.getTypeNid() != Snomed.IS_A.getLenient().getNid()) 
		{
			bt.selectSheet(SHEET.Change_Relationship);
			bt.addRow();
			for (COLUMN column : bt.getColumnsOfSheet(SHEET.Change_Relationship))
			{
				switch (column)
				{
					case Topic:
						bt.addStringCell(column, this.getTopic(OTFUtility.getConceptVersion(relVersion.getConceptNid())));
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
						if(ExtendedAppContext.getCurrentlyLoggedInUserProfile().getExtensionNamespace() != null) {
							bt.addStringCell(column, "Developed as part of extension namespace " + ExtendedAppContext.getCurrentlyLoggedInUserProfile().getExtensionNamespace());
						} else {
							bt.addStringCell(column, "");
						}
						break;
					case Note:
						getNote(relVersion.getNid());
						break;
					default :
						throw new RuntimeException("Unexpected column type found in Sheet: " + column + " - " + SHEET.Change_Relationship);
				}
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
		changeDescCount++;
		bt.selectSheet(SHEET.Change_Description);
		bt.addRow();
		for (COLUMN column : bt.getColumnsOfSheet(SHEET.Change_Description))
		{
			switch (column)
			{
				case Topic:
					try {
						bt.addStringCell(column, this.getTopic(OTFUtility.getConceptVersion(d.getConceptNid())));
					} catch(Exception e) {
						logger.error("Error Creating Desc Topic", e);
					}
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
					bt.addStringCell(column, getJustification());
					break;
				case Note:
					getNote(d.getNid());
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
					bt.addStringCell(column, this.getTopic(concept));
					break;
				case Terminology:
					bt.addStringCell(column, getTerminology(concept));
					break;
				case Concept_Id:
					bt.addNumericCell(column, getSct(concept.getNid()));
					break;
				case Change_Concept_Status_To: 
					bt.addStringCell(column, USCRSBatchTemplate.PICKLIST_Change_Concept_Status_To.Retired.toString());
					break;
				case Duplicate_Concept_Id: 
					bt.addStringCell(column, "");
					break;
				case Justification:
					bt.addStringCell(column, getJustification());
					break;
				case Note:
						getNote(concept.getNid());
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
					bt.addStringCell(column, this.getTopic(OTFUtility.getConceptVersion(d.getConceptNid())));
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
				case Change_Description_Status_To:
					bt.addStringCell(column, USCRSBatchTemplate.PICKLIST_Change_Concept_Status_To.Retired.toString());
					break;
				case Justification:
					bt.addStringCell(column, getJustification());
					break;
				case Note:
					getNote(d.getNid());
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
					bt.addStringCell(column, this.getTopic(OTFUtility.getConceptVersion(relVersion.getConceptNid())));
					break;
				case Source_Terminology:
					bt.addStringCell(column, getTerminology(relVersion));
					break;
				case Source_Concept_Id:
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
					bt.addStringCell(column, getJustification());
					break;
				case Note:
					getNote(relVersion.getNid());
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
		if (rel.isActive() && (rel.getTypeNid() == Snomed.IS_A.getLenient().getNid())) 
		{
			bt.selectSheet(SHEET.Add_Parent);
			bt.addRow();
			for (COLUMN column : bt.getColumnsOfSheet(SHEET.Add_Parent))
			{
				switch (column)
				{
					case Topic:
						bt.addStringCell(column, this.getTopic(OTFUtility.getConceptVersion(rel.getDestinationNid())));
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
						bt.addStringCell(column, getJustification());
						break;
					case Note:
						getNote(rel.getNid());
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
					bt.addStringCell(column, this.getTopic(concept));
					break;
				case Description:
					break;
				case Justification:
					bt.addStringCell(column, getJustification());
					break;
				case Note:
					bt.addStringCell(column, ""); //User Input
					break;
				default :
					throw new RuntimeException("Unexpected column type found in Sheet: " + column + " - " + SHEET.Other);
			}
		}
	}
	

}
