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
package gov.va.isaac.mojos.datastore.export;

import gov.va.isaac.AppContext;
import gov.va.isaac.ExtendedAppContext;
import gov.va.isaac.config.generated.StatedInferredOptions;
import gov.va.isaac.config.profiles.UserProfile;
import gov.va.isaac.config.profiles.UserProfileManager;
import gov.va.isaac.config.users.InvalidUserException;
import gov.va.isaac.ie.exporter.EConceptExporter;
import gov.va.isaac.init.SystemInit;
import gov.va.isaac.interfaces.gui.constants.SharedServiceNames;
import gov.va.isaac.interfaces.gui.views.commonFunctionality.ExportTaskHandlerI;
import gov.va.isaac.mojos.conceptSpec.MojoConceptSpec;
import gov.va.isaac.util.OTFUtility;
import gov.va.isaac.util.ProgressEvent;
import gov.va.isaac.util.ProgressListener;
import gov.va.isaac.util.Utility;
import gov.va.isaac.util.ViewCoordinateFactory;
import gov.vha.isaac.ochre.api.LookupService;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javafx.concurrent.Task;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.ihtsdo.otf.tcc.api.chronicle.ComponentVersionBI;
import org.ihtsdo.otf.tcc.api.concept.ConceptChronicleBI;
import org.ihtsdo.otf.tcc.api.concept.ConceptVersionBI;
import org.ihtsdo.otf.tcc.api.contradiction.ContradictionException;
import org.ihtsdo.otf.tcc.api.coordinate.Status;
import org.ihtsdo.otf.tcc.api.coordinate.ViewCoordinate;
import org.ihtsdo.otf.tcc.api.metadata.binding.Snomed;
import org.ihtsdo.otf.tcc.api.relationship.RelAssertionType;
import org.ihtsdo.otf.tcc.api.store.TerminologyStoreDI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * Exports a jBin file type (an eConcept) .
 * 
 * @author <a href="mailto:vkaloidis@apelon.com">Vas Kaloidis</a>
 *  
 *  
 */
@Mojo( name = "release-export")
public class ReleaseExporter extends AbstractMojo // implements ProcessUnfetchedConceptDataBI, Exporter
{
	public enum ExportMojoFormat
	{
		Econcept ("EConcept", "EConcept Files .jbin", ".jbin"),
		Uscrs ("Uscrs", "Excel Files .xls", ".xls");
		
		private final String name;
		private final String extensionDesc;
		private final String extensionFormat;
		
		ExportMojoFormat(String name, 
				String extensionDesc, 
				String extensionFormat) 
		{
			this.name = name;
			this.extensionDesc = extensionDesc;
			this.extensionFormat = extensionFormat;
		}
	}
	
	private EConceptExporter eConExporter;
	private DataOutputStream ecDos_;
	private DataOutputStream uscrsDos_;
	private List<ConceptChronicleBI> allPaths = null;
	private String econceptFileName = "";
	private String uscrsFileName = "";
	private ViewCoordinate vc;
	private int pathNid;
	private UUID selectedPath;
	private int selectedPathNid;
	private static UserProfile userProfileMain = null;
	private int streamCount = 0;
	
	IntStream uscrsConcepts;
	ArrayList<ConceptVersionBI> uscrsNids = new ArrayList<ConceptVersionBI>();
	
	//ProcessUnfetchedConceptDataBI
	private boolean requestCancel = false;
	
	//REQUIRED
	@Parameter (name = "outputFolder", required = true)
	public File outputFolder;
 
	@Parameter (name = "exportType", required = true)
	public ExportReleaseType[] exportType;
	
	@Parameter (name = "path", required = true)
	public MojoConceptSpec path;
	
	//OPTIONAL
	@Parameter (name="namespace")
	public String namespace;
	
	@Parameter (name = "modules")
	public HashSet<String> modules;
	
	static DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
	static Date date = new Date();
	public static final String today = dateFormat.format(date);
	
	@Parameter (name="releaseDate", defaultValue = "${maven.build.timestamp}")
	public Date releaseDate;
	
	@Parameter (name = "pathFilter")
	public MojoConceptSpec[] pathFilter;
	
	@Parameter (name="skipExportAssembly", defaultValue="false")
	public boolean skipExportAssembly;
	
	@Parameter (name = "exportFormat", defaultValue="all")
	public String exportFormat;
	
	@Parameter (name = "uscrsDateFilter")
	public Date uscrsDateFilter;
	//TODO: Document Mojo Params
	
	@Override
	public void execute() throws MojoExecutionException 
	{
		if(skipExportAssembly)
		{
			getLog().info("Content Export Mojo will not run due to '-DskipExportAssembly' being set to true.  Set -DskipExportAssembly=false to enable the Content Export" );
		}
		else
		{ 
			if(uscrsDateFilter != null) {
				getLog().info("Date Filter Mojo Param Set: " + uscrsDateFilter.toString());
			}
			try {
				Thread.sleep(4000);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			
			vc = OTFUtility.getViewCoordinate();
			vc.getViewPosition().setTime(System.currentTimeMillis());
			vc.setAllowedStatus(EnumSet.of(Status.ACTIVE));
			vc.setRelationshipAssertionType(RelAssertionType.STATED);
			
			getLog().info("Executing Content Export" );
			econceptFileName = "SOLOR_Snapshot_" + path.getFsn() + "";
					
			getLog().debug("Export Mojo Started with PATH: " + path);
			
			//Check if requireed Parameters are Empty
			if(outputFolder != null) {
				if(outputFolder.exists()) {
					getLog().info("Output directory exists: " + outputFolder.getAbsolutePath());
				} else {
					if(outputFolder.mkdir()) {
						getLog().info("Folder created succesfully: " + outputFolder.getAbsolutePath());
					} else {
						throw new MojoExecutionException("Error creating folder: " + outputFolder.getAbsolutePath());
					}
					getLog().info("Output directory created: " + outputFolder.getAbsolutePath());
				}
			} else {
				getLog().error("Missing outputFolder parameter");
				throw new MojoExecutionException("Missing outputFolder");
			}
			
			if(exportType.length < 1) {
				getLog().error("exportType array parameter is emnpty");
				throw new MojoExecutionException("Missing exportType");
			} else {
				getLog().info("exportType: " + exportType);
			}
			
			
			if(namespace != null) {
				econceptFileName = econceptFileName + "_[" + namespace + "]";
			}
			if(releaseDate != null) {
				econceptFileName = econceptFileName + "_[" + releaseDate + "]";
			}
			
			try
			{
				boolean isPathValid = validPath(path);
				
				if(isPathValid && allPaths != null) {
					pathNid = path.getConceptSpec().getNid();
					
					if(exportFormat.equalsIgnoreCase(ExportMojoFormat.Uscrs.name()) || exportFormat.equalsIgnoreCase("all")) //USCRS EXPORT
					{
						getLog().info("Starting a USCRS Export");
						
						int componentsExported = exportUscrs(selectedPath);
						
						if(componentsExported < 0) {
							getLog().error("No Concepts exported");
						}
					} 
					else if(!exportFormat.equalsIgnoreCase(ExportMojoFormat.Uscrs.name()) && !exportFormat.equalsIgnoreCase("all") && uscrsDateFilter != null) {
						//If USCRS Date Filter Flag set (not equal to todays timestamp) but Output Format not equal to all or USCRS, we throw an error.
						getLog().error("You set the -DuscrsDateFilter (to export USCRS Content Request by date) but did not set the Export Format (-DexportFormat) to 'USCRS' or 'all'... Ignoring USCRS Date Filter");
					}
					
					if(exportFormat.equalsIgnoreCase(ExportMojoFormat.Econcept.toString()) || exportFormat.equalsIgnoreCase("all")) //ECONCEPT EXPORT
					{
						exportEconcept();
					} 
				} else {
					//getLog().error("PATH " + path.getConceptSpec().getPrimodialUuid() + " is NOT valid, cannot export");
					throw new MojoExecutionException("PATH ERROR - Path is invalid: " + path); 
				}
			}
			catch (Exception e) {
				getLog().error("Error exporting", e);
				throw new MojoExecutionException("Unexpected error exporting the DB", e);
			} finally {
				try {
					if(ecDos_ != null) {
						ecDos_.close();
					}
				} catch (IOException e) {
					getLog().error("IOException closing DOS", e);
				}
				try {
					if(uscrsDos_ != null) {
						uscrsDos_.close();
					}
				} catch (IOException e) {
					getLog().error("IOException closing DOS", e);
				}
			}
		}
	}
	
	/**
	 * Exports all the Concepts on a set path, as EConcepts
	 * 
	 */
	public boolean exportEconcept() {
		getLog().info("Executing an EConcept Export");
		try {
			//TODO: Replace seperator with default FS Seperator
			ecDos_ = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(new File(outputFolder.toString() + "\\" + econceptFileName + ExportMojoFormat.Econcept.extensionFormat))));
		} catch (FileNotFoundException e) {
			getLog().error("Error generating EConcept DOS", e);
		}
		eConExporter = new EConceptExporter(ecDos_);
		
		ProgressListener pListener = new ProgressListener() {
			@Override
			public void updateProgress(ProgressEvent pe) {
				long percent = pe.getPercent();
				if((percent % 10) == 0){
					getLog().info(percent + "% complete - " + eConExporter.getCount() + " concepts exported");
				} else if(percent == 1) {
					getLog().info("Export started succesfully (1% complete)");
				}
			}
		};
		eConExporter.addProgressListener(pListener);

		try {
			eConExporter.export(selectedPathNid);
		} catch (Exception e) {
			getLog().error("Error exporting EConcepts", e);
			return false;
		}
		return true;
	}
	
	/**
	 * Executes a USCRS Export, by taking a list of concepts to be exported. To export only
	 * 	content modified after a specific date, you must set dateFilter to a date other than 
	 *  today. It returns the amount of concepts exported.
	 * @param inputConcepts A list of concepts to be exported
	 * @return  
	 * @throws IOException 
	 */
	public int exportUscrs(UUID path) throws IOException {
		Class<UserProfileManager> userProfileManagerClass = UserProfileManager.class;
		userProfileMain = ExtendedAppContext.getService(userProfileManagerClass).getCurrentlyLoggedInUserProfile();
		StatedInferredOptions relAssertionType = userProfileMain.getStatedInferredPolicy();
		Set<Status> statuses = userProfileMain.getViewCoordinateStatuses();
		long time = userProfileMain.getViewCoordinateTime();
		Set<UUID> vcModules = userProfileMain.getViewCoordinateModules();
		
		ViewCoordinate pathVc = ViewCoordinateFactory.getViewCoordinate(path, relAssertionType, statuses, time, vcModules);
		TerminologyStoreDI dataStore;
		Stream<? extends ConceptChronicleBI> conceptStream = null;
		try {
			dataStore = ExtendedAppContext.getDataStore();
			conceptStream = dataStore.getConceptStream();
		} catch(Exception e) {
			getLog().error("USCRS Export Mojo Error - problem loading datastore and Concept-Stream", e);
		} finally {
			//TODO: Shutdown Issue: it hangs, need a shutdown script, but this one does NOT work -> dataStore.shutdown();
		}
		if(modules != null) {
			for(String thisModule :  modules) {
				getLog().info("Module Filter Loaded: " + thisModule);
			}
		} else {
			getLog().info("No Module Filter Set");
		}
		
		IntStream nidStream =  conceptStream.filter( 
													(ConceptChronicleBI concept) ->{ 
														try { 
															Optional<? extends ConceptVersionBI> cv = concept.getVersion(pathVc);
															if(cv.isPresent()) { //Testing:  add this clause (&& streamCount <= 20)
																ConceptVersionBI cvg = cv.get();
																if(modules != null) {
																	ConceptVersionBI cvmv = OTFUtility.getConceptVersion(cvg.getModuleNid());
																	if(cvmv != null){
																		//String thisModDesc = OTFUtility.getDescription(cvmv.getNid());
																		String cvmvUuid = cvmv.getPrimordialUuid().toString();
																		if(modules.contains(cvmvUuid)) {
																			streamCount++;
																			return true;
																		} else {
																			return false;
																		}
																	} else{
																		return false;
																	}
																} else {
																	streamCount++;
																	return true;
																}
															} else {
																return false;
															}
														} catch(ContradictionException e) { 
															return false;
														} })
											  .mapToInt(c -> c.getNid());
		
		String date = new SimpleDateFormat("MM-dd-yyyy HH-mm-ss").format(new Date());
		uscrsFileName = "VA_USCRS_Submission_File_" + date;
		Path uscrsFileNamePath = Paths.get(outputFolder.getAbsolutePath() + "\\" + uscrsFileName  + ExportMojoFormat.Uscrs.extensionFormat);//TODO: Replace seperator with default FS Seperator
		
		ExportTaskHandlerI uscrsExporter = LookupService.getService(ExportTaskHandlerI.class, SharedServiceNames.USCRS);
		if(uscrsDateFilter != null) { //Filter USCRS by date if we have one
			Properties uscrsProps = new Properties();
			uscrsProps.setProperty("date", String.valueOf(uscrsDateFilter.getTime()));
			getLog().debug("Date Param: " + String.valueOf(uscrsDateFilter.getTime()));
			try {
				uscrsExporter.setOptions(uscrsProps);
			} catch (Exception e) {
				getLog().error("Error generating Settting Date Option filter for USCRS Export", e);
			}
		}
		Task<Integer> task = null;
		try {
			getLog().info("USCRS Concepts prepared for Export: " + streamCount + " concepts");
			getLog().info("Preparing to write USCRS Content Request to: " + uscrsFileNamePath.toString());
			task = uscrsExporter.createTask(nidStream, uscrsFileNamePath);
		} catch (FileNotFoundException e) {
			getLog().error("Error generating USCRS Export Task", e);
		}
		
		int count = 0;
		try {
			Utility.execute(task);
			count = task.get();
			getLog().info("Succesfully exported " + count + " concepts into " + uscrsFileNamePath.toString());
		} catch (Exception e) {
			getLog().error("Error Execuging USCRS Content Request Handler task", e);
		}
		return count;
	}
	
	public boolean validPath(MojoConceptSpec pathSpec) {
		try {
			allPaths = OTFUtility.getPathConcepts();
		} catch (Exception e) {
			getLog().error("Error getting all PATHS for path validation - OTFUtility.getPathConcepts()", e);
		} 
		if(allPaths != null) {
			for(ConceptChronicleBI thisPath : allPaths) {
				try {
					if(thisPath.getPrimordialUuid().equals(pathSpec.getConceptSpec().getPrimodialUuid())) {
						selectedPath = pathSpec.getConceptSpec().getPrimodialUuid();
						selectedPathNid = pathSpec.getConceptSpec().getNid();
						getLog().info("Path is valid: " + OTFUtility.getDescription(selectedPathNid));
						return true;
					}
				} catch (Exception e) {
					getLog().error("Exception Error");
					e.printStackTrace();
				}
			}
		} else {
			getLog().error("Error getting paths for validation from - OTFUtility.getPathConcepts()");
		}
		return false;
	}

	public static void main(String[] args) {
		Logger logger_ = LoggerFactory.getLogger(ReleaseExporter.class);
		
		Exception dataStoreLocationInitException = null;
		try {
			dataStoreLocationInitException = SystemInit.doBasicSystemInit(new File("../../va-isaac-gui-pa/app-assembly/"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (dataStoreLocationInitException != null)
		{
			System.err.println("Configuration of datastore path failed.  DB will not be able to start properly!  " + dataStoreLocationInitException);
			System.exit(-1);
		}
		try {
			UserProfileManager userProfileManager = AppContext.getService(UserProfileManager.class);
			userProfileManager.configureAutomationMode(new File("profiles"));
		} catch (InvalidUserException e) {
			e.printStackTrace();
		}
		LookupService.startupIsaac();
		
		ReleaseExporter export = new ReleaseExporter();
		export.outputFolder = new File("target/output"); //Channge to target/output
		export.exportType = new ExportReleaseType[]{ExportReleaseType.SNAPSHOT};
		export.skipExportAssembly = false;

		MojoConceptSpec mojoConceptSpec = new MojoConceptSpec();
		
		UserProfile userProfile = null;
		Class<UserProfileManager> userProfileManagerClass = UserProfileManager.class;
		userProfile = ExtendedAppContext.getService(userProfileManagerClass).getCurrentlyLoggedInUserProfile();
		
		UUID userPath = userProfile.getViewCoordinatePath(); 
		String pathFsn = "";
		try {
			pathFsn = OTFUtility.getDescription(userPath, OTFUtility.getViewCoordinate()); //development (ISAAC)
			mojoConceptSpec.setFsn(pathFsn);
		} catch (Exception e) {
			logger_.error("Error setting PATHS FSN", e);
		}
		String userPathUuid = userPath.toString();
		mojoConceptSpec.setUuid(userPathUuid); //32d7e06d-c8ae-516d-8a33-df5bcc9c9ec7
		
		HashSet<String> modules = new HashSet<String>();
		modules.add(Snomed.US_EXTENSION_MODULE.getPrimodialUuid().toString());
		modules.add(Snomed.CORE_MODULE.getPrimodialUuid().toString());
		export.modules = modules;
		
		export.path = mojoConceptSpec;
		
		//export.exportFormat = ExportMojoFormat.Econcept.name.toString();
		export.exportFormat = ExportMojoFormat.Uscrs.name.toString();
		
		//export.uscrsDateFilter = new Date(668822400000L); // 3/13/1991
		//export.uscrsDateFilter = new Date(1331596800000L); //3/13/2012
		export.uscrsDateFilter = new Date(1296432000000L); // 1/31/2011
		try {
			export.execute();
		} catch (MojoExecutionException e) {
			e.printStackTrace();
		}
		
		//SHUTDOWN
		LookupService.shutdownIsaac();
		
	}
}