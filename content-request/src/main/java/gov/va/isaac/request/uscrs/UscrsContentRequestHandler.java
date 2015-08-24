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
import gov.va.isaac.config.profiles.UserProfile;
import gov.va.isaac.config.profiles.UserProfileManager;
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
import gov.va.isaac.util.OchreUtility;
import gov.vha.isaac.metadata.coordinates.LanguageCoordinates;
import gov.vha.isaac.metadata.source.IsaacMetadataAuxiliaryBinding;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.chronicle.LatestVersion;
import gov.vha.isaac.ochre.api.chronicle.StampedVersion;
import gov.vha.isaac.ochre.api.component.concept.ConceptChronology;
import gov.vha.isaac.ochre.api.component.concept.ConceptSnapshot;
import gov.vha.isaac.ochre.api.component.sememe.SememeChronology;
import gov.vha.isaac.ochre.api.component.sememe.version.DescriptionSememe;
import gov.vha.isaac.ochre.api.coordinate.LanguageCoordinate;
import gov.vha.isaac.ochre.api.coordinate.StampCoordinate;
import gov.vha.isaac.ochre.api.coordinate.StampPosition;
import gov.vha.isaac.ochre.api.coordinate.StampPrecedence;
import gov.vha.isaac.ochre.api.relationship.RelationshipVersionAdaptor;
import gov.vha.isaac.ochre.collections.ConceptSequenceSet;
import gov.vha.isaac.ochre.impl.utility.Frills;
import gov.vha.isaac.ochre.model.coordinate.StampCoordinateImpl;
import gov.vha.isaac.ochre.model.coordinate.StampPositionImpl;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
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
import org.ihtsdo.otf.tcc.api.metadata.binding.Snomed;
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
	int pathSequence = 0;
	
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
		propKeys.add("path");
		
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
		if(prop.containsKey("path")) {
			pathSequence = Integer.valueOf(prop.getProperty("path"));
		} else {
			pathSequence = IsaacMetadataAuxiliaryBinding.DEVELOPMENT.getConceptSequence();
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
	
	private StampCoordinate sc;
	private LanguageCoordinate lc = LanguageCoordinates.getUsEnglishLanguageFullySpecifiedNameCoordinate();
	
	public int max_rows = 65000;
	
	Class<? extends DescriptionSememe> dsClass = DescriptionSememe.class;
	
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
				
				StampPosition spLatest = new StampPositionImpl(System.currentTimeMillis(), pathSequence);
				StampPosition spInitial = new StampPositionImpl(previousReleaseTime, pathSequence);
				
				StampCoordinate scLatestActive = new StampCoordinateImpl(StampPrecedence.PATH, spLatest, 
						ConceptSequenceSet.EMPTY, gov.vha.isaac.ochre.api.State.ACTIVE_ONLY_SET);
				StampCoordinate scLatestAll = new StampCoordinateImpl(StampPrecedence.PATH, spLatest, 
						ConceptSequenceSet.EMPTY, gov.vha.isaac.ochre.api.State.ANY_STATE_SET);
				StampCoordinate scInitialActive = new StampCoordinateImpl(StampPrecedence.PATH, spInitial, 
						ConceptSequenceSet.EMPTY, gov.vha.isaac.ochre.api.State.ACTIVE_ONLY_SET);
				StampCoordinate scInitialAll = new StampCoordinateImpl(StampPrecedence.PATH, spInitial, 
						ConceptSequenceSet.EMPTY, gov.vha.isaac.ochre.api.State.ANY_STATE_SET);
				sc = scLatestActive; //We use this one for all general stamp needs
				
				logger.info("USCRS Content Request Handler - Starting Concept Stream Iterator");
				intStream
					.limit(65000)
					.forEach( nid -> {	
						
						if(examinedConCount % 100 == 0) {
							updateTitle("Uscrs Content Request Exporter Progress - We have exported " + examinedConCount + " components / concepts");
						}
						if(isCancelled()) {
							logger.info("User canceled Uscrs Export Operation");
							throw new RuntimeException("User canceled operation");
						}
						
						ArrayList<SememeChronology<?>> relationships = new ArrayList<SememeChronology<?>>();
						//TODO: Start breaking this code up into sub-methods. Because we will be adding more filters
						if(filter) {
							if(prop.containsKey("date") && previousReleaseTime != Long.MIN_VALUE) { 
								ConceptSnapshot concept = null;
								boolean conceptCreated = false;
								
								try {
									//Export Concept
									Optional<ConceptSnapshot> csLatest = OchreUtility.getConceptSnapshot(nid, scLatestActive, lc);
									Optional<ConceptSnapshot> csInitial = OchreUtility.getConceptSnapshot(nid, scInitialActive, lc); 
									
									if(csInitial.isPresent()) {
										if(csLatest.isPresent()) {
//											todo - Changed Concept SHEET
//											if(conceptIsChanged) {
//												//TODO: Handle Concept Changes
//												//conceptCreated = true;
//											} else {
//												//noop
//											}
											concept = csLatest.get();
										} else {
											concept = csInitial.get();
											handleRetireConcept(concept);
										}
									} else {
										if(csLatest.isPresent()) {
											Optional<ConceptSnapshot> csRetiredCheck = OchreUtility.getConceptSnapshot(nid, scInitialAll, lc); 
											if(csRetiredCheck.isPresent()) {
												concept = csRetiredCheck.get();
												// todo - Changed Concept SHEET (un-retired)
											} else {
												concept = csLatest.get();
												relationships.addAll(handleNewConcept(concept));
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
									ConceptChronology<? extends StampedVersion> chronology = concept.getChronology();
//									chronology
//										.getConceptDescriptionList()
//										.stream()
//										.filter( sc -> {
//											Optional<? extends LatestVersion<? extends DescriptionSememe>> lv = sc.getLatestVersion((Class<? extends DescriptionSememe>) DescriptionSememe.class, sclatestAll);
//											if(lv.isPresent()) {}
//											return true;
//										});
									ArrayList<DescriptionSememe> descriptions = new ArrayList<DescriptionSememe>();
									for(SememeChronology sc : chronology.getConceptDescriptionList()) { //The stream above would be better if we didn't get a cast issue
										Optional<? extends LatestVersion<? extends DescriptionSememe>> lvO = sc.getLatestVersion(DescriptionSememe.class, scLatestAll);
										if(lvO.isPresent()) {
											LatestVersion<? extends DescriptionSememe> lvd = lvO.get();
											descriptions.add(lvd.value());
										}
									}
									
									for(DescriptionSememe d : descriptions) {
										@SuppressWarnings("unchecked")
										Optional<LatestVersion<DescriptionSememe>> dsLatest = Get.conceptService().getSnapshot(scLatestActive, lc)
												.getDescriptionOptional(chronology.getConceptSequence()); 
										@SuppressWarnings("unchecked")
										Optional<LatestVersion<DescriptionSememe>> dsInitial = Get.conceptService().getSnapshot(scInitialActive, lc)
												.getDescriptionOptional(chronology.getConceptSequence()); 

										
										if(dsInitial.isPresent()) {
											if(dsLatest.isPresent()){
												DescriptionSememe dsLatestV = dsLatest.get().value();
												DescriptionSememe dsInitialV = dsInitial.get().value();
												
												boolean hasChange = false;
												
												if(dsLatestV.getLanguageConceptSequence()!=dsInitialV.getLanguageConceptSequence()) {
													hasChange = true;
												} else if(!dsLatestV.getText().equals(dsInitialV.getText())) {
													hasChange = true;
												} else if (dsLatestV.getCaseSignificanceConceptSequence() != dsInitialV.getCaseSignificanceConceptSequence()) {
													hasChange = true;
												}
												if(hasChange) {
													handleChangeDesc(dsLatestV, concept);
												}
											} else {
												handleRetireDescription(dsLatest.get().value(), concept);
											}
										} else {
											if(dsLatest.isPresent()) {
												@SuppressWarnings("unchecked")
												Optional<LatestVersion<DescriptionSememe>> dvCheckRetired = Get.conceptService().getSnapshot(scInitialAll, lc)
														.getDescriptionOptional(concept.getConceptSequence());  
												if(dvCheckRetired.isPresent()) {
													handleChangeDesc(dvCheckRetired.get().value(), concept);
												} else {
													DescriptionSememe dvLatestG = dsLatest.get().value();
													if(notFsnOrPref(dvLatestG)) {
														handleNewSyn(dvLatestG, concept);
													}
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
										List<? extends SememeChronology<? extends RelationshipVersionAdaptor>> incomingRelChronicles = concept.getChronology().getRelationshipListWithConceptAsDestination();
										for (SememeChronology<? extends RelationshipVersionAdaptor> chronicle : incomingRelChronicles)
										{
											relationships.add(chronicle);
										}
									} catch (Exception e) {
										logger.error("Error retreiving the incoming relationships", e);
									}
								}
								for(SememeChronology sc : relationships) {
									try {
										Optional<? extends RelationshipVersionAdaptor> scLatest = sc.getLatestVersion(RelationshipVersionAdaptor.class, scLatestActive);
										Optional<? extends RelationshipVersionAdaptor> scInitial = sc.getLatestVersion(RelationshipVersionAdaptor.class, scInitialActive);
										
										if(scInitial.isPresent()) {
											if(scLatest.isPresent()){
												RelationshipVersionAdaptor thisRvLatest = scLatest.get();
												RelationshipVersionAdaptor thisRvInitial = scInitial.get();
												
												boolean hasRelAttrChange = false;
												if(thisRvLatest.getOriginSequence() != thisRvInitial.getOriginSequence()) {
													hasRelAttrChange = true;
												} else if(thisRvLatest.getDestinationSequence() != thisRvInitial.getDestinationSequence()) {
													hasRelAttrChange = true;
												} else if (thisRvLatest.getTypeSequence() != thisRvInitial.getTypeSequence()) {
													hasRelAttrChange = true;
												} else if (thisRvLatest.getPremiseType() != thisRvInitial.getPremiseType()) {
													hasRelAttrChange = true;
												} else if (thisRvLatest.getNodeSequence() != thisRvInitial.getNodeSequence()) {
													hasRelAttrChange = true;
												} else if (thisRvLatest.getChronicleKey() != thisRvInitial.getChronicleKey()) {
													hasRelAttrChange = true;
												}
												
												if(hasRelAttrChange) {
													if (thisRvLatest.getTypeSequence() == IsaacMetadataAuxiliaryBinding.IS_A.getLenient().getNid()) {
														handleChangeParent(thisRvLatest, scLatestActive);
													} else {
														handleChangeRels(thisRvLatest);
													}
												}
											} else {
												handleRetireRelationship(scInitial.get());
											}
										} else {
											if(scLatest.isPresent()) {
												RelationshipVersionAdaptor scLatestG = scLatest.get();
												Optional<? extends RelationshipVersionAdaptor> rvCheckRetired = sc.getLatestVersion(RelationshipVersionAdaptor.class, scInitialAll);
												if(rvCheckRetired.isPresent()) {
													RelationshipVersionAdaptor retiredRel = rvCheckRetired.get();
													if (retiredRel.getTypeSequence() == IsaacMetadataAuxiliaryBinding.IS_A.getLenient().getNid()) {
														handleChangeParent(retiredRel, scInitialAll);
													} else {
														handleChangeRels(retiredRel);
													}
												} else {
													if (scLatestG.getTypeSequence() == IsaacMetadataAuxiliaryBinding.IS_A.getLenient().getNid()) {
														handleNewParent(scLatestG);
													} else {
														handleNewRel(scLatestG);  
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
									Optional<ConceptSnapshot> conceptO = OchreUtility.getConceptSnapshot(nid, scLatestAll, lc);
									if(conceptO.isPresent()) {
										ConceptSnapshot concept = conceptO.get();
										relationships.addAll(handleNewConcept(concept));
										for(SememeChronology sc : relationships) {
											Optional<? extends RelationshipVersionAdaptor> rel = sc.getLatestVersion(RelationshipVersionAdaptor.class, scLatestActive);
											if(rel.isPresent()) {
												RelationshipVersionAdaptor r = rel.get();
												handleNewParent(r);
												handleNewRel(r);
											}
										}
										
										ArrayList<DescriptionSememe> descriptions = new ArrayList<DescriptionSememe>();
										for(SememeChronology dsc : concept.getChronology().getConceptDescriptionList()) { //The stream above would be better if we didn't get a cast issue
											Optional<? extends LatestVersion<? extends DescriptionSememe>> lvdO = dsc.getLatestVersion(DescriptionSememe.class, scLatestAll);
											if(lvdO.isPresent()) {
												DescriptionSememe lvds = lvdO.get().value();
												if(notFsnOrPref(lvds)){ 
													handleNewSyn(lvds, concept);
												}
											}
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
								Optional<ConceptSnapshot> conceptO = OchreUtility.getConceptSnapshot(nid, scLatestAll, lc);
								if(conceptO.isPresent()) {
									ConceptSnapshot concept = conceptO.get();
									relationships.addAll(handleNewConcept(concept));
									for(SememeChronology sc : relationships) {
										Optional<? extends RelationshipVersionAdaptor> rel = sc.getLatestVersion(RelationshipVersionAdaptor.class, scLatestActive);
										if(rel.isPresent()) {
											RelationshipVersionAdaptor r = rel.get();
											handleNewParent(r);
											handleNewRel(r);
										}
									}
								
									ArrayList<DescriptionSememe> descriptions = new ArrayList<DescriptionSememe>();
									for(SememeChronology dsc : concept.getChronology().getConceptDescriptionList()) { //The stream above would be better if we didn't get a cast issue
										Optional<? extends LatestVersion<? extends DescriptionSememe>> lvdO = dsc.getLatestVersion(DescriptionSememe.class, scLatestAll);
										if(lvdO.isPresent()) {
											DescriptionSememe lvds = lvdO.get().value();
											if(notFsnOrPref(lvds)){
												handleNewSyn(lvds, concept);
											}
										}
									}
								}
							} catch (Exception e) {
								logger.error("Could not export concept " + nid, e);
							} 
							
						}
						logger.info("USCRS Content Request Handler - ITERATED CONCEPT " + examinedConCount++);
				});
				
				logger.info("*** EXPORT FINISHED *** Concept Stream Iterator: " + examinedConCount + " concepts iterated");
				
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
	 */
	private void getNote(int nid)  {
		this.getNote(nid, "");
	}
	
	/**
	 * Create a Note Cell, and print the row's NID and Description for testing. If testing is
	 *  disabled then print the value of alternateNote
	 * @param nid
	 * @param alternateNote if testing is disabled, print this instead
	 */
	private void getNote(int nid, String alternateNote) {
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
	private String getSemanticTag(ConceptSnapshot concept) throws Exception {
		Optional<? extends String> fsnO = OchreUtility.getFSNForConceptNid(concept.getNid(), concept.getStampCoordinate());
		if(fsnO.isPresent()) {
			String fsn = fsnO.get();
			if (fsn.indexOf('(') != -1) {
				String st = fsn.substring(fsn.lastIndexOf('(') + 1, fsn.lastIndexOf(')'));
				try {
					return PICKLIST_Semantic_Tag.find(st).toString();
				} catch(EnumConstantNotPresentException ecnpe) {
					logger.error("USCRS PICKLIST API Missing Semantic Tag Value " + ecnpe.constantName());
					return st;
				} catch(Exception e) {
					logger.error("USCRS Rel Type Error");
					return "";
				}
			} else {
				return null;
			}
		} else {
			return null;
		}
	}
	
	private String getTopic(Optional<ConceptSnapshot> concept) throws Exception {
		if(concept.isPresent()) {
			return getTopic(concept.get());
		} else {
			return "";
		}
	}
	
	/**
	 * This returns the semantic tag, with no regards to the PICKLIST. 
	 * @param concept
	 * @return
	 * @throws Exception
	 */
	private String getTopic(ConceptSnapshot concept) throws Exception {
		Optional<? extends String> fsnO = OchreUtility.getFSNForConceptNid(concept.getNid(), concept.getStampCoordinate());
		if (fsnO.isPresent()) {
			String fsn = fsnO.get();
			if(fsn.indexOf('(') != -1) {
				return  fsn.substring(fsn.lastIndexOf('(') + 1, fsn.lastIndexOf(')'));
			} else {
				return "";
			}
		} else {
			return "";
		}
	}
	
	/**
	 * Returns the FSN, with the sematic tag removed.
	 * 
	 * @param concept
	 * @return FSN with-out the semantic tag
	 * @throws Exception
	 */
	private String getFsnWithoutSemTag(ConceptSnapshot concept) throws Exception {
		Optional<? extends String> fsnO = OchreUtility.getFSNForConceptNid(concept.getNid(), concept.getStampCoordinate());
		if(fsnO.isPresent()) {
			String fsn = fsnO.get();
			if (fsn.indexOf('(') != -1)
			{
				return fsn.substring(0, fsn.lastIndexOf('(') - 1);
			} else {
				return null;
			}
		} else {
			return null;
		}
	}
	
	/**
	 * Takes the Relationships NID and returns the Relationship Type from the PICKLIST
	 * 
	 * @param nid of the relationship
	 * @return String of the relationship type
	 */
	private String getRelType(int nid) {
		try {
			Optional<String> rtPrefTerm = Frills.getPreferredTermForConceptNid(nid, sc);
			Optional<String> rtFsn = OchreUtility.getFSNForConceptNid(nid, sc); 
			if(rtPrefTerm.isPresent()) {
				return PICKLIST_Relationship_Type.find(rtPrefTerm.get()).toString();
			} else if(rtFsn.isPresent()) {
				return PICKLIST_Relationship_Type.find(rtFsn.get()).toString();
			} else {
				return "";
			}
		} catch(EnumConstantNotPresentException ecnpe) {
			logger.error("USCRS PICKLIST API Missing Relationship Type Value " + ecnpe.constantName());
			return "";
		} catch(Exception e) {
			logger.error("USCRS Rel Type Error");
			return "";
		}
	}
	
	private String getJustification() {
		try {
			if(namespace != 0) {
				return "Developed as part of extension namespace " + String.valueOf(namespace); 
			} else {
				String userNamespace = "";
				try {
					UserProfile userProfile = ExtendedAppContext.getService(UserProfileManager.class).getCurrentlyLoggedInUserProfile();
					userNamespace = userProfile.getExtensionNamespace();
				} catch (Exception e) {
					logger.error("Error getting namespace for Justification column", e);
				}
				if(!userNamespace.trim().equals("")) {
					return "Developed as part of extension namespace " + userNamespace;
				} else {
					logger.error("Namespace Extension could not be found");
					return "Developed as part of extension namespace " + "";
				}
			}
		} catch(EnumConstantNotPresentException ecnpe) {
			logger.error("USCRS PICKLIST API Missing Justification Value " + ecnpe.constantName());
			return "";
		} catch(Exception e) {
			logger.error("USCRS Justification Type Error");
			return "";
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
		try {
			String characteristic = OchreUtility.getPreferredTermForConceptNid(nid, null, null).get();
			if(characteristic.equalsIgnoreCase("stated")) {
				return "STATED"; //TODO - we need to map this correctly
				//return RelationshipType.STATED_ROLE.toString();
				//return PICKLIST_Characteristic_Type.Defining_relationship.toString(); 
			} else if(characteristic.equalsIgnoreCase("other-term")) {
				return PICKLIST_Characteristic_Type.Qualifying_relationship.toString(); //TOODO: Map Correct
			} else if(characteristic.equalsIgnoreCase("other-term")) {
				return PICKLIST_Characteristic_Type.Additional_relationship.toString(); //TOODO: Map Correct
			}
			return characteristic; //But this works temporarily
		} catch(EnumConstantNotPresentException ecnpe) {
			logger.error("USCRS PICKLIST API Missing Characteristic Type Value " + ecnpe.constantName());
			return "";
		} catch(Exception e) {
			logger.error("USCRS Characteristic Type Error");
			return "";
		}
	}
	
	private String getRefinability(int nid) {
		try {
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
		} catch(EnumConstantNotPresentException ecnpe) {
			logger.error("USCRS PICKLIST API Missing Refinability Value " + ecnpe.constantName());
			return "";
		} catch(Exception e) {
			logger.error("USCRS Refinability Type Error");
			return "";
		}
	}
	
	/**
	 * Pass in the case significance sequence.
	 * 
	 * @param caseSig
	 * @return
	 */
	private String getCaseSig(int caseSig) {
		try {
			if(caseSig == IsaacMetadataAuxiliaryBinding.CASE_SIGNIFICANCE_CONCEPT_SEQUENCE_FOR_DESCRIPTION.getLenient().getNid()) {
				return PICKLIST_Case_Significance.Entire_term_case_sensitive.toString();
			} else {
				return PICKLIST_Case_Significance.Entire_term_case_insensitive.toString();
			}
		} catch(EnumConstantNotPresentException ecnpe) {
			logger.error("USCRS PICKLIST API Missing Case Sifnificance " + ecnpe.constantName());
			return "";
		} catch(Exception e) {
			logger.error("USCRS Case Sifnificance Error");
			return "";
		}
	}
	
	private String getTerminology(Optional<? extends ConceptSnapshot> concept) throws Exception {
		if(concept.isPresent()) {
			return getTerminology(concept.get());
		} else {
			return "";
		}
	}
	
	/**
	 * Figure out the appropriate terminology constant string for term related columns.
	 * 
	 * @param cv
	 * @return
	 * @throws Exception
	 */
	private String getTerminology(ConceptSnapshot concept) throws Exception {
		try {
			int moduleNid = concept.getModuleSequence();
			
			if (currentRequestMap.containsKey(concept.getNid()))
			{
				return PICKLIST_Source_Terminology.Current_Batch_Requests.toString();
			}
	
			//If it was done on core or us extension - assume it was pre-existing.
			//TODO This isn't 100% safe, as the user may have used this module when they did
			//a previous submission - but at the moment, we don't have any way of knowing
			//what IDs were previously submitted - so we can't choose between on of these 
			//official constants, and "New Concept Request"
			if(moduleNid == Snomed.CORE_MODULE.getNid()) {
				return PICKLIST_Source_Terminology.SNOMED_CT_International.toString();  //TODO Dan notes these won't work in OCHRE - need to talk to me and/or Keith
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
			else if (!isChildOfSCT(concept.getNid())) {
				logger.error("Cannot export concepts or components that are not in SCT");
				return "NOT in SCT";
				//TODO: Fix this throw new Exception("Cannot export something that isn't part of the SCT hierarchy");
			}
			else {
				//The only thing we can do at this point, is assume it was a previously submitted
				//item.
				//TODO this isn't 100% safe - we need to have a permanent store of IDs that were 
				//previously submitted.
				return PICKLIST_Source_Terminology.New_Concept_Requests.toString();
			}
		} catch(EnumConstantNotPresentException ecnpe) {
			logger.error("USCRS PICKLIST API Missing Justification Value " + ecnpe.constantName());
			return "";
		} catch(Exception e) {
			logger.error("USCRS Justification Type Error");
			return "";
		}
	}
	
	
	private long getSct(int nid) {
		Optional<? extends Long> sct = Frills.getSctId(nid, sc); // Possibly change Stamp Coordinate here
		if(sct.isPresent()) {
			return sct.get();
		} else { 
			if(!currentRequestMap.containsKey(nid)) { //TODO This needs to be hammered out, this dosen't always work.
				currentRequestMap.put(nid, globalRequestCounter.getAndIncrement());
			}
			return currentRequestMap.get(nid);
		}
	}
	
	private boolean notFsnOrPref(DescriptionSememe ds) {
		try {
			if(ds.getDescriptionTypeConceptSequence() != IsaacMetadataAuxiliaryBinding.FULLY_SPECIFIED_NAME.getLenient().getNid() &&
					ds.getDescriptionTypeConceptSequence() != IsaacMetadataAuxiliaryBinding.PREFERRED.getLenient().getNid() ){ //Not Preferred Term
				return true;
			} else {
				return false;
			}
		} catch(Exception e) {
			logger.error("Error determing if Description Sememe is a FSN or Preferred Term using IsaacMetadataAuxBindings", e);
		}
		return false;
	}
	
	private static boolean isChildOfSCT(int conceptNid) throws IOException 
	{
		try {
			return Get.taxonomyService().isChildOf(conceptNid, IsaacMetadataAuxiliaryBinding.HEALTH_CONCEPT.getNid(), 
					ExtendedAppContext.getUserProfileBindings().getTaxonomyCoordinate().get());
		} catch (Exception e) {
			logger.error("USCRS Error retreiving isChildOfSct", e);
		}
		return false;
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
	private ArrayList<SememeChronology<? extends RelationshipVersionAdaptor>> handleNewConcept(ConceptSnapshot concept) throws Exception
	{
		ArrayList<SememeChronology<? extends RelationshipVersionAdaptor>> extraRels = new ArrayList<SememeChronology<? extends RelationshipVersionAdaptor>>();
		// PARENTS
		LinkedList<Integer> parentNids = new LinkedList<Integer>();
		LinkedList<String> parentsTerms = new LinkedList<String>();
		LinkedList<String> definitions = new LinkedList<String>();
		
		int thisNewReqId = 0;
		
		int isaCount = 0;
		List<? extends SememeChronology<? extends RelationshipVersionAdaptor>> incomingRelChronicles = concept.getChronology().getRelationshipListWithConceptAsDestination();
		for (SememeChronology<? extends RelationshipVersionAdaptor> chronicle : incomingRelChronicles)
		{
			for (RelationshipVersionAdaptor<?> rv : chronicle.getVersionList())
			{
				if(rv.getTypeSequence() == IsaacMetadataAuxiliaryBinding.IS_A.getLenient().getNid()) {
					Optional<ConceptSnapshot> destConcept = OchreUtility.getConceptSnapshot(rv.getDestinationSequence(), concept.getStampCoordinate(), concept.getLanguageCoordinate());
					if(destConcept.isPresent()) {
						ConceptSnapshot destConceptG = destConcept.get();
						parentNids.add(isaCount, destConceptG.getNid());
						parentsTerms.add(isaCount, this.getTerminology(destConceptG));
						if(isaCount > 2 && rv != null && !extraRels.contains(chronicle)) {
							extraRels.add(chronicle);
						}
						isaCount++;
					}
				} else {
					if(!extraRels.contains(chronicle)) {
						extraRels.add(chronicle);
					}
				}
			}
		}
		
		bt.selectSheet(SHEET.New_Concept);
		bt.addRow();
		for (COLUMN column : bt.getColumnsOfSheet(SHEET.New_Concept))
		{
			switch (column) {
				case Request_Id:
					long reqId = getSct(concept.getNid());
					if (reqId > Integer.MAX_VALUE) //TODO: This isn't 100% safe when detecting if we got an SCT or not. It's just guessing.
					{
						if(dateFilterChecking) {
							throw new RuntimeException("We appear to have found an SCTID when we only expected a generated sequence ID");
						} else {
							break; //TODO: Verify this doesen't leave blank rows
						}
					}
					newConceptRequestIds.add((int)reqId);
					bt.addNumericCell(column, reqId);
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
						bt.addStringCell(column, getTerminology(OchreUtility.getConceptSnapshot(parentNids.get(0), null, null)));
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
					
					sb.append("SCT ID: " + this.getSct(concept.getNid()));
					bt.addStringCell(column, sb.toString());
					break;
				case Synonym:
					bt.addStringCell(column, "");
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
	private void handleNewSyn(DescriptionSememe descVersion, ConceptSnapshot concept) throws Exception
	{	
		bt.selectSheet(SHEET.New_Synonym);
		bt.addRow();
		for (COLUMN column : bt.getColumnsOfSheet(SHEET.New_Synonym)) {
			switch(column)
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
			case Term:
				bt.addStringCell(column, descVersion.getText());
				break;
			case Case_Significance:
				bt.addStringCell(column, this.getCaseSig(descVersion.getCaseSignificanceConceptSequence()));
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
	private void handleChangeParent(RelationshipVersionAdaptor rel, StampCoordinate sc) throws Exception
	{	
		if (rel.getTypeSequence() == IsaacMetadataAuxiliaryBinding.IS_A.getLenient().getNid()) 
		{
			bt.selectSheet(SHEET.Change_Parent);
			bt.addRow();
			for (COLUMN column : bt.getColumnsOfSheet(SHEET.Change_Parent)) {
				switch(column)
				{
					case Topic:
						Optional<ConceptSnapshot> topic = OchreUtility.getConceptSnapshot(rel.getOriginSequence(), sc, lc);
						bt.addStringCell(column, getTopic(topic));
						break;
					case Source_Terminology:
						Optional<ConceptSnapshot> st = OchreUtility.getConceptSnapshot(rel.getOriginSequence(), sc, lc);
						bt.addStringCell(column,  getTerminology(st));
						break;
					case Concept_Id:
						bt.addNumericCell(column, getSct(rel.getOriginSequence()));
						break;
					case New_Parent_Concept_Id:
						bt.addNumericCell(column, getSct(rel.getDestinationSequence()));
						break;
					case New_Parent_Terminology:
						Optional<ConceptSnapshot> dt = OchreUtility.getConceptSnapshot(rel.getDestinationSequence(), sc, lc);
						bt.addStringCell(column,  getTerminology(dt));
						break;
					case Justification:
						bt.addStringCell(column, getJustification());
						break;
					case Note:
						getNote(rel.getDestinationSequence());
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
	private void handleNewRel(RelationshipVersionAdaptor<?> rel) throws Exception {
		if (rel.getTypeSequence() != IsaacMetadataAuxiliaryBinding.IS_A.getLenient().getNid()) 
		{
			bt.selectSheet(SHEET.New_Relationship);
			bt.addRow();
			for (COLUMN column : bt.getColumnsOfSheet(SHEET.New_Relationship)) {
				switch (column) {
					case Topic:
					Optional<ConceptSnapshot> topic = OchreUtility.getConceptSnapshot(rel.getOriginSequence(), sc, lc);
					bt.addStringCell(column, getTopic(topic));
					break;
					case Source_Terminology:
						Optional<ConceptSnapshot> st = OchreUtility.getConceptSnapshot(rel.getOriginSequence(), sc, lc);
						bt.addStringCell(column,  getTerminology(st));
						break;
					case Source_Concept_Id:
						bt.addNumericCell(column, getSct(rel.getOriginSequence()));
						break;
					case Relationship_Type:
						bt.addStringCell(column, getRelType(rel.getTypeSequence())); 
						break;
					case Destination_Terminology:
						Optional<ConceptSnapshot> dt = OchreUtility.getConceptSnapshot(rel.getDestinationSequence(), sc, lc);
						bt.addStringCell(column,  getTerminology(dt));
						break;
					case Destination_Concept_Id:
						bt.addNumericCell(column, getSct(rel.getDestinationSequence()));
						break;
					case Characteristic_Type:
//						bt.addStringCell(column, getCharType(rel.getCharacteristicNid())); // todo - relationship characteristic type
						break;
					case Refinability:
//						bt.addStringCell(column, getRefinability(rel.));
						break;
					case Relationship_Group:
						bt.addNumericCell(column, rel.getGroup());
						break;
					case Justification:
						bt.addStringCell(column, getJustification());
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
	private void handleChangeRels(RelationshipVersionAdaptor rel) throws Exception
	{
		if (rel.getTypeSequence() != IsaacMetadataAuxiliaryBinding.IS_A.getLenient().getNid()) 
		{
			bt.selectSheet(SHEET.Change_Relationship);
			bt.addRow();
			for (COLUMN column : bt.getColumnsOfSheet(SHEET.Change_Relationship))
			{
				switch (column)
				{
					case Topic:
						Optional<ConceptSnapshot> topic = OchreUtility.getConceptSnapshot(rel.getOriginSequence(), sc, lc);
						bt.addStringCell(column, getTopic(topic));
					break;
					case Source_Concept_Id:
						bt.addNumericCell(column, getSct(rel.getOriginSequence()));
						break;
					case Relationship_Id:  
						bt.addNumericCell(column, getSct(rel.getNid()));
						break;
					case Relationship_Type: 
						bt.addStringCell(column, getRelType(rel.getTypeSequence()));
						break;
					case Source_Terminology:
						Optional<ConceptSnapshot> st = OchreUtility.getConceptSnapshot(rel.getOriginSequence(), sc, lc);
						bt.addStringCell(column,  getTerminology(st));
						break;
					case Destination_Concept_Id:
						bt.addNumericCell(column, getSct(rel.getDestinationSequence()));
						break;
					case Destination_Terminology:
						Optional<ConceptSnapshot> dt = OchreUtility.getConceptSnapshot(rel.getDestinationSequence(), sc, lc);
						bt.addStringCell(column,  getTerminology(dt));
						break;
					case Characteristic_Type:
//						bt.addStringCell(column, getCharType(rel.getCharacteristic())); // todo - API missing
						break;
					case Refinability:
//						bt.addStringCell(column, getRefinability(rel.getRefinability())); //todo - api missing
						break;
					case Relationship_Group:
						bt.addNumericCell(column, rel.getGroup());
						break;
					case Justification:
						bt.addStringCell(column, getJustification());
						break;
					case Note:
						getNote(rel.getNid());
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
	 * @param ArrayList<DescriptionSememe> descVersion an ArrayList of DescriptionVersions that will be added
	 * @param bt the wb
	 * @throws Exception the exception
	 */
	private void handleChangeDesc(DescriptionSememe d, ConceptSnapshot concept) throws Exception
	{
		bt.selectSheet(SHEET.Change_Description);
		bt.addRow();
		for (COLUMN column : bt.getColumnsOfSheet(SHEET.Change_Description))
		{
			switch (column)
			{
				case Topic:
					try {
						bt.addStringCell(column, this.getTopic(concept));
					} catch(Exception e) {
						logger.error("Error Creating Desc Topic", e);
					}
					break;
				case Terminology:
					bt.addStringCell(column, getTerminology(concept));
					break;
				case Concept_Id:
					bt.addNumericCell(column, getSct(concept.getNid()));
					break;
				case Description_Id:
					bt.addNumericCell(column, getSct(d.getNid()));
					break;
				case Term: 
					bt.addStringCell(column, d.getText());
					break;
				case Case_Significance:
					bt.addStringCell(column, getCaseSig(d.getCaseSignificanceConceptSequence()));
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
	private void handleRetireConcept(ConceptSnapshot concept) throws Exception
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
	private void handleRetireDescription(DescriptionSememe d, ConceptSnapshot concept) throws Exception
	{
		bt.selectSheet(SHEET.Retire_Description);
		bt.addRow();
		for (COLUMN column : bt.getColumnsOfSheet(SHEET.Retire_Description))
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
					bt.addNumericCell(column, this.getSct(concept.getNid()));
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
	private void handleRetireRelationship(RelationshipVersionAdaptor rel) throws Exception
	{
		bt.selectSheet(SHEET.Retire_Relationship);
		bt.addRow();
		for (COLUMN column : bt.getColumnsOfSheet(SHEET.Retire_Relationship))
		{
			switch (column)
			{
				case Topic:
					Optional<ConceptSnapshot> topic = OchreUtility.getConceptSnapshot(rel.getOriginSequence(), sc, lc);
					bt.addStringCell(column, getTopic(topic));
					break;
				case Source_Terminology:
					Optional<ConceptSnapshot> st = OchreUtility.getConceptSnapshot(rel.getOriginSequence(), sc, lc);
					bt.addStringCell(column,  getTerminology(st));
					break;
				case Source_Concept_Id:
					bt.addNumericCell(column, getSct(rel.getNid()));
					break;
				case Relationship_Id:  
					bt.addNumericCell(column, getSct(rel.getNid()));
					break;
				case Destination_Terminology:
					Optional<ConceptSnapshot> dt = OchreUtility.getConceptSnapshot(rel.getDestinationSequence(), sc, lc);
					bt.addStringCell(column, getTerminology(dt));
					break;
				case Destination_Concept_Id:
					bt.addNumericCell(column, getSct(rel.getDestinationSequence()));
					break;
				case Relationship_Type:
					bt.addStringCell(column, this.getRelType(rel.getTypeSequence()));
					break;
				case Justification:
					bt.addStringCell(column, getJustification());
					break;
				case Note:
					getNote(rel.getNid());
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
	private void handleNewParent(RelationshipVersionAdaptor rel) throws Exception
	{
		if (rel.getTypeSequence() == IsaacMetadataAuxiliaryBinding.IS_A.getLenient().getNid()) 
		{
			bt.selectSheet(SHEET.Add_Parent);
			bt.addRow();
			for (COLUMN column : bt.getColumnsOfSheet(SHEET.Add_Parent))
			{
				switch (column)
				{
					case Topic:
						Optional<ConceptSnapshot> topic = OchreUtility.getConceptSnapshot(rel.getOriginSequence(), sc, lc);
						bt.addStringCell(column, getTopic(topic));
						break;
					case Source_Terminology: 
						Optional<ConceptSnapshot> st = OchreUtility.getConceptSnapshot(rel.getOriginSequence(), sc, lc);
						bt.addStringCell(column,  getTerminology(st));
						break;
					case Child_Concept_Id:
						bt.addNumericCell(column, getSct(rel.getDestinationSequence()));
						break;
					case Destination_Terminology:
						Optional<ConceptSnapshot> dt = OchreUtility.getConceptSnapshot(rel.getDestinationSequence(), sc, lc);
						bt.addStringCell(column, getTerminology(dt));
						break;
					case Parent_Concept_Id:  
						bt.addNumericCell(column, getSct(rel.getOriginSequence()));
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
	private void handleOther(ConceptSnapshot concept) throws Exception
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
