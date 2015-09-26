/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gov.vha.isaac.gui.integration.tests;

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;
import gov.va.isaac.AppContext;
import gov.va.isaac.config.profiles.UserProfileManager;
import gov.va.isaac.config.users.InvalidUserException;
import gov.va.isaac.init.SystemInit;
import gov.va.isaac.request.uscrs.UscrsContentRequestHandler;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.LookupService;
import gov.vha.isaac.ochre.api.component.concept.ConceptSnapshot;
import gov.vha.isaac.ochre.api.component.sememe.SememeChronology;
import gov.vha.isaac.ochre.api.relationship.RelationshipVersionAdaptor;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.api.MultiException;
import org.jvnet.testing.hk2testng.HK2;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 *
 * @author Vas Kaloidis
 */
// https://www.jfokus.se/jfokus08/pres/jf08-HundredKilobytesKernelHK2.pdf
// https://github.com/saden1/hk2-testng
@HK2("cradle")
public class IsaacIntegrationTests {

	ArrayList<ConceptSnapshot> concepts = new ArrayList<>();
	
//	public static void main(String[] args) {
//		try {
//			IsaacIntegrationTests cit = new IsaacIntegrationTests();
//			try {
//				cit.setUpSuite();
//				cit.testLoad();
//			} finally {
//				cit.tearDownSuite();
//			}
//		} catch (Exception ex) {
//			log.fatal(ex.getLocalizedMessage(), ex);
//		}
//		System.exit(0);
//	}

	private static final Logger log = LogManager.getLogger();
	private boolean dbExists = false;

	@BeforeGroups(groups = {"db"})
	public void setUpSuite() throws Exception {
		loadDatabase();
		
		//Add UUID's of concepts to load for testing here
		ArrayList<UUID> conceptsToLoad = new ArrayList<>();
		conceptsToLoad.add(UUID.fromString("a942bd99-0fb7-3ec4-9198-bb037e6e2b85")); //Cerebellar infarction
		conceptsToLoad.add(UUID.fromString("df691661-f29b-3409-a2b4-606c17ed53df")); //Macular infarction
		
		for(UUID u : conceptsToLoad) {
			concepts.add(Get.conceptSnapshot().getConceptSnapshot(Get.identifierService().getConceptSequenceForUuids(u)));
		}
	}

	@AfterGroups(groups = {"db"})
	public void tearDownSuite() throws Exception {
		//SHUTDOWN
		LookupService.shutdownIsaac();
	}

	@BeforeMethod
	public void setUp() throws Exception {
		
	}

	@AfterMethod
	public void tearDown() throws Exception {

	}

	@Test(groups = {"db"})
	public void testLoad() throws Exception {
		if (!dbExists) {
			loadDatabase();
		}
		
		testUscrsContentRequestHandler();

	}
	
	public ArrayList<String> getUscrsTestMethods() {
		ArrayList<String> returnList = new ArrayList<String>(){
			private static final long serialVersionUID = 1144429503912286887L;
		{
			add("caseSig"); add("charType"); add("getFsn"); add("getJustification"); add("getRefinability"); add("getRelType"); 
			add("getSct"); add("getSemanticTag"); add("getTerminology"); add("getTitle"); add("getTopic"); 
		}};
		return returnList;
	}
	
	public void testUscrsContentRequestHandler() {
		
		//USCRS Content Request Handler
		UscrsContentRequestHandler ucrh = new UscrsContentRequestHandler();
		Properties uscrsProps = new Properties();
		uscrsProps.setProperty("date", "1079136000000"); //3/13/2004));
		try {
			ucrh.setOptions(uscrsProps);
		} catch (Exception e) {
			log.error("Error setting USCRS Handler Options");
		}
		
		ArrayList<String> methodsToTest = getUscrsTestMethods();
		HashMap<ConceptSnapshot, HashMap<String, String>> testAnswers = getUscrsExpectedResults();

		for(ConceptSnapshot c : concepts) {
			HashMap<String, String> answers = testAnswers.get(c); //EXPECTED RESULTS (ANSWERS)
			HashMap<String, String> results = new HashMap<String, String>();
			
			//CONCEPT REALTIONSHIPS
			List<? extends SememeChronology<? extends RelationshipVersionAdaptor>> incomingRelChronicles 
					= c.getChronology().getRelationshipListWithConceptAsDestination();
			for (SememeChronology<? extends RelationshipVersionAdaptor> chronicle : incomingRelChronicles) {
			}
			
			//GET USCRS DATA
			try {
				results.put("caseSig", ucrh.getCaseSig(c.getNid()));
				results.put("charType", ucrh.getCharType(c.getNid())); 
				results.put("getFsn", ucrh.getFsnWithoutSemTag(c)); 
				results.put("getJustification", ucrh.getJustification());
				results.put("getRefinability", ucrh.getRefinability(c.getNid())); 
				results.put("getRelType", ucrh.getRelType(c.getNid()));
				results.put("getSct", String.valueOf(ucrh.getSct(c.getNid())));
				results.put("getSemanticTag", ucrh.getSemanticTag(c));
				results.put("getTerminology", ucrh.getTerminology(c));
				results.put("getTitle", ucrh.getTitle());
				results.put("getTopic", ucrh.getTopic(c));
				//boolean notFsnOrPref = notFsnOrPref(DescriptionSememe);
			} catch(Exception e) {
				log.error("Error executing USCRS Content  Request handler - ", e);
			}
			
			//TEST DATA
			for(String test : methodsToTest) {
				log.info("TEST (Test - Result): " + test + ": " + answers.get(test) + " - " + results.get(test));
				assertTrue(equivalent(answers.get(test), results.get(test)));
				log.info("TEST " + test + " Good!");
			}
		}
	}
	
	public boolean equivalent(String s1, String s2) {
		if(s1.equalsIgnoreCase(s2)) {
			return true;
		} else {
			return false;
		}
	}
	
	public boolean equivalent(String s, Long l) {
		if(s.equalsIgnoreCase(String.valueOf(l))) {
			return true;
		} else {
			return false;
		}
	}
	
	public HashMap<ConceptSnapshot, HashMap<String, String>> getUscrsExpectedResults() {
		HashMap<ConceptSnapshot, HashMap<String, String>> uscrsExpectedResults = new HashMap<ConceptSnapshot, HashMap<String,String>>();
		ArrayList<String> methods = getUscrsTestMethods();
		
		for(ConceptSnapshot c : concepts) {
			HashMap<String, String> thisMap = new HashMap<String, String>();
			if(c.getPrimordialUuid().equals(UUID.fromString("a942bd99-0fb7-3ec4-9198-bb037e6e2b85"))) { //CONCEPT EXPECTED RESULTS
				thisMap.put("caseSig", "Entire term case insensitive");
				thisMap.put("charType", "Cerebellar infarction");
				thisMap.put("getFsn", "Cerebellar infarction");
				thisMap.put("getJustification", "");
				thisMap.put("getRefinability", "");
				thisMap.put("getRelType", "");
				thisMap.put("getSct", "95460007");
				thisMap.put("getSemanticTag", "disorder");
				thisMap.put("getTerminology", "SNOMED CT International");
				thisMap.put("getTitle", "USCRS Content Request Handler");
				thisMap.put("getTopic", "disorder");
			} else if(c.getPrimordialUuid().equals(UUID.fromString("df691661-f29b-3409-a2b4-606c17ed53df"))) { //CONCEPT EXPECTED RESULTS
				thisMap.put( "caseSig", "Entire term case insensitive");
				thisMap.put("charType", "Macular infarction");
				thisMap.put( "getFsn", "Macular infarction");
				thisMap.put("getJustification", "");
				thisMap.put("getRefinability", "");
				thisMap.put("getRelType", "");
				thisMap.put("getSct", "418918009");
				thisMap.put("getSemanticTag", "disorder");
				thisMap.put("getTerminology", "SNOMED CT International");
				thisMap.put("getTitle", "USCRS Content Request Handler");
				thisMap.put("getTopic", "disorder");
			}
			uscrsExpectedResults.put(c, thisMap);
		}
		
		return uscrsExpectedResults;
	}

	private void loadDatabase() throws ExecutionException, IOException, MultiException, InterruptedException {
//		LOAD USCRS AS A SERVICE
//		ExportTaskHandlerI uscrsExporter = LookupService.getService(ExportTaskHandlerI.class, SharedServiceNames.USCRS); //TASK Operation
//		Task<Integer> task = uscrsExporter.createTask(conceptStream, uscrsFileNamePath);//TASK Operation
//		Utility.execute(task);//TASK Operation
//		task.get();//TASK Operation
		
//		CRADLE DB CONNECTION CODE
//		Path snomedDataFile = Paths.get("target/data/SnomedCoreEConcepts.jbin");
//		Path isaacMetadataFile = Paths.get("target/data/isaac/metadata/econ/IsaacMetadataAuxiliary.econ");
//		Task<Integer> loadTask = tts.startLoadTask(ConceptModel.OCHRE_CONCEPT_MODEL, IsaacMetadataAuxiliaryBinding.DEVELOPMENT, isaacMetadataFile, snomedDataFile);
//		int conceptCount = loadTask.get();
//		Instant finish = Instant.now();
//		Duration duration = Duration.between(start, finish);
//		double nsPerConcept = 1.0d * duration.toNanos() / conceptCount;
//		double msPerConcept = 1.0d * duration.toMillis() / conceptCount;
		
		Exception dataStoreLocationInitException = null;
		try {
			dataStoreLocationInitException = SystemInit.doBasicSystemInit(new File("db/"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (dataStoreLocationInitException != null)
		{
			System.err.println("Configuration of datastore path failed.  DB will not be able to start properly!  " + dataStoreLocationInitException);
			fail("Configuration of datastore path failed.  DB will not be able to start properly!  " + dataStoreLocationInitException);
		}
		
		LookupService.startupIsaac();
		
		try {
			UserProfileManager userProfileManager = AppContext.getService(UserProfileManager.class);
			userProfileManager.configureAutomationMode(new File("profiles"));
		} catch (InvalidUserException e) {
			e.printStackTrace();
		}
		dbExists=true;
	}
}
