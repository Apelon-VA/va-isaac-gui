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

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.ihtsdo.otf.tcc.api.metadata.binding.Snomed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.va.isaac.AppContext;
import gov.va.isaac.ExtendedAppContext;
import gov.va.isaac.config.profiles.UserProfile;
import gov.va.isaac.config.profiles.UserProfileManager;
import gov.va.isaac.config.users.InvalidUserException;
import gov.va.isaac.ie.exporter.EConceptExporter;
import gov.va.isaac.init.SystemInit;
import gov.va.isaac.interfaces.gui.constants.SharedServiceNames;
import gov.va.isaac.interfaces.gui.views.commonFunctionality.ExportTaskHandlerI;
import gov.va.isaac.mojos.conceptSpec.MojoConceptSpec;
import gov.va.isaac.util.OchreUtility;
import gov.va.isaac.util.ProgressEvent;
import gov.va.isaac.util.ProgressListener;
import gov.va.isaac.util.Utility;
import gov.vha.isaac.metadata.coordinates.LanguageCoordinates;
import gov.vha.isaac.metadata.source.IsaacMetadataAuxiliaryBinding;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.LookupService;
import gov.vha.isaac.ochre.api.component.concept.ConceptChronology;
import gov.vha.isaac.ochre.api.component.concept.ConceptSnapshot;
import gov.vha.isaac.ochre.api.component.concept.ConceptVersion;
import gov.vha.isaac.ochre.api.coordinate.LanguageCoordinate;
import gov.vha.isaac.ochre.api.coordinate.StampCoordinate;
import gov.vha.isaac.ochre.api.coordinate.StampPrecedence;
import gov.vha.isaac.ochre.collections.ConceptSequenceSet;
import gov.vha.isaac.ochre.model.coordinate.StampCoordinateImpl;
import gov.vha.isaac.ochre.model.coordinate.StampPositionImpl;
import javafx.concurrent.Task;



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

	private static final Function ConceptChronology = null;
	
	private EConceptExporter eConExporter;
	private DataOutputStream ecDos_;
	private DataOutputStream uscrsDos_;
	private Set<ConceptVersion<?>> allPaths = null;
	private String econceptFileName = "";
	private String uscrsFileName = "";
	private int pathSequence;
	private UUID selectedPath;
	private int selectedPathNid;
	private static UserProfile userProfileMain = null;
	private int streamCount = 0;
	private StampPositionImpl sp = null; 
	private StampCoordinate sc = null;
	private LanguageCoordinate lc = LanguageCoordinates.getUsEnglishLanguageFullySpecifiedNameCoordinate();
	
	IntStream uscrsConcepts;
	ArrayList<ConceptSnapshot> uscrsNids = new ArrayList<ConceptSnapshot>();
	
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
	@Parameter (name="namespace", defaultValue="0")
	public int namespace;
	
	@Parameter (name = "modules")
	public HashSet<String> modules;
	
	@Parameter (name="releaseDate")
	public Date releaseDate;
	
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
			//Initial Filename
			String date = new SimpleDateFormat("MM-dd-yyyy HH-mm-ss").format(new Date());
			uscrsFileName = "VA_USCRS_Submission_File_" + date;
			econceptFileName = "SOLOR_Snapshot_" + path.getFsn();
			
			getLog().info("Executing Content Export" );
			
			getLog().debug("PATH Param Loaded: " + path.getFsn() + " - " + path.getUuid().toString());
			
			//Check if output folder exists, if not then create it
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
				getLog().info("Output Folder: " + outputFolder.getAbsolutePath());
			} else {
				getLog().error("Missing outputFolder parameter");
				throw new MojoExecutionException("Missing outputFolder");
			}
			
			if(exportType.length < 1) {
				getLog().error("exportType array parameter is emnpty");
				throw new MojoExecutionException("Missing exportType");
			} else {
				for(ExportReleaseType etp : exportType) {
					getLog().info("Export-Type Param Loaded: " + etp.toString());
				}
			}
			
			if(uscrsDateFilter != null) {
				getLog().info("USCRS-Date-Filter Param Loaded: " + uscrsDateFilter.toString() + " - " + uscrsDateFilter.getTime());
			}
			
			if(namespace != 0) {
				econceptFileName = econceptFileName + "_[" + namespace + "]";
				getLog().info("Namespace Param Loaded: " + namespace);
			}
			if(releaseDate != null) {
				econceptFileName = econceptFileName + "_[" + releaseDate + "]";
				getLog().info("Release-Date Param Loaded: " + releaseDate.getTime());
			}
			
			if(modules != null) {
				for(String thisModule :  modules) {
					getLog().info("Module Param Loaded: " + thisModule);
				}
			}
			getLog().info("Export-Format Param Loaded: " + exportFormat);
			
			try
			{
				//if(validPath(path)) { //allPaths != null && 
				if(true) {
					if(exportFormat.equalsIgnoreCase(ExportMojoFormat.Uscrs.name()) || exportFormat.equalsIgnoreCase("all")) //USCRS EXPORT
					{
						getLog().info("Starting a USCRS Export");
						
						int componentsExported = exportUscrs(path);
						
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
					getLog().error("PATH " + path.getUuid() + " is NOT valid, cannot export. Or OCHREUtil did not return any paths.");
					throw new MojoExecutionException("PATH ERROR - Path is invalid: " + path.getUuid()); 
				}
			}
			catch (Exception e) 
			{
				getLog().error("Error exporting", e);
				throw new MojoExecutionException("Unexpected error exporting the DB", e);
			} 
			finally 
			{
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
	public int exportUscrs(MojoConceptSpec path) throws IOException {
		
		//Convert Module UUID HashSet to IntSTream for StampCoordinate Construction
		IntStream moduleSequences = modules.stream()
											.mapToInt( module -> {
												String error = "USCRS Export Mojo Config Error - Invalid Module";
												if(Get.identifierService().hasUuid(UUID.fromString(module))) {
													int moduleSeq = Get.identifierService().getConceptSequenceForUuids(UUID.fromString(module));
													//if(Get.taxonomyService().isChildOf(moduleSeq, IsaacMetadataAuxiliaryBinding.MODULE.getNid(), 
													//		ExtendedAppContext.getUserProfileBindings().getTaxonomyCoordinate().get())) {
														return moduleSeq;
													//} else {
													//	return moduleSeq;
														//throw new RuntimeException(error);
													// }
												} else {
													throw new RuntimeException(error);
												}
											} );
		
		pathSequence = Get.identifierService().getConceptSequenceForUuids(UUID.fromString(path.getUuid())); 
		if(uscrsDateFilter != null) { 
			sp = new StampPositionImpl(uscrsDateFilter.getTime(), pathSequence);
			sc = new StampCoordinateImpl(StampPrecedence.PATH, sp, 
					ConceptSequenceSet.of(moduleSequences), gov.vha.isaac.ochre.api.State.ANY_STATE_SET);
		} else {
			sp = new StampPositionImpl(System.currentTimeMillis(), pathSequence);
			sc = new StampCoordinateImpl(StampPrecedence.PATH, sp, 
					ConceptSequenceSet.of(moduleSequences), gov.vha.isaac.ochre.api.State.ACTIVE_ONLY_SET);
		}
		
		Stream<ConceptChronology<? extends ConceptVersion<?>>> stream = null;
		try {
			stream = Get.conceptService().getConceptChronologyStream();
		} catch(Exception e) {
			getLog().error("USCRS Export Mojo Error - problem loading GET Concept-Stream", e);
		} 
		IntStream conceptStream = stream.filter( 
										(ConceptChronology<? extends ConceptVersion<?>> cc) ->  {
											try {
												int conSeq = ((ConceptChronology<?>)cc).getConceptSequence();
												Optional<ConceptSnapshot> cs = OchreUtility.getConceptSnapshot(conSeq, sc, lc); //Concept Snapshot
												if(cs.isPresent()) {
													List<UUID> modUuid = Get.identifierService().getUuidsForNid(cs.get().getModuleSequence());
													return true;
												}else {
													return false;
												}
											} catch(Exception e) {
												getLog().error("Error during IntStream filter", e);
												return false;
											}
										}).mapToInt(
											(ConceptChronology<? extends ConceptVersion<?>> cc) ->  { 
												return cc.getNid(); 
												} 
										);
		
		Path uscrsFileNamePath = Paths.get(outputFolder.getAbsolutePath() + "\\" + uscrsFileName  + ExportMojoFormat.Uscrs.extensionFormat);//TODO: Replace seperator with default FS Seperator
		
		ExportTaskHandlerI uscrsExporter = LookupService.getService(ExportTaskHandlerI.class, SharedServiceNames.USCRS);
		Properties uscrsProps = new Properties();
		if(uscrsDateFilter != null) { //Filter USCRS by date if we have one
			uscrsProps.setProperty("date", String.valueOf(uscrsDateFilter.getTime()));
			getLog().debug("Date Param: " + String.valueOf(uscrsDateFilter.getTime()));
			try {
				uscrsExporter.setOptions(uscrsProps);
			} catch (Exception e) {
				getLog().error("Error generating Settting Date Option filter for USCRS Export", e);
			}
		}
		if(namespace != 0) {
			uscrsProps.setProperty("namespace", String.valueOf(namespace));
		}
		Task<Integer> task = null;
		try {
			getLog().info("Preparing to write USCRS Content Request to: " + uscrsFileNamePath.toString());
			task = uscrsExporter.createTask(conceptStream, uscrsFileNamePath);
		} catch (FileNotFoundException e) {
			getLog().error("Error generating USCRS Export Task", e);
		}
		
		int count = 0;
		try {
			Utility.execute(task);
			count = task.get();
			getLog().info("Succesfully exported " + count + " concepts into " + uscrsFileNamePath.toString());
			getLog().info("The Stream prepared " + streamCount + " concepts for export");
		} catch (Exception e) {
			getLog().error("Error Execuging USCRS Content Request Handler task", e);
		}
		return count;
	}
	
	public boolean validPath(MojoConceptSpec pathSpec) {
		try {
			allPaths = OchreUtility.getPathConcepts();
			//Set<ConceptVersion<?>> pathConcepts = OCHREUtility.getPathConcepts();
		} catch (Exception e) {
			getLog().error("Error getting all PATHS for path validation - OchreUtility.getPathConcepts()", e);
		} 
		if(allPaths != null) {
			for(ConceptVersion<?> thisPath : allPaths) {
				try {
					if(thisPath.getChronology().getPrimordialUuid().equals(pathSpec.getConceptSpec().getPrimodialUuid())) {
						selectedPath = pathSpec.getConceptSpec().getPrimodialUuid();
						selectedPathNid = pathSpec.getConceptSpec().getNid();
						getLog().info("Path is valid: " + OchreUtility.getDescription(selectedPathNid));
						return true; 
					}
				} catch (Exception e) {
					getLog().error("Error checking if path valid (validPath())", e);
				}
			}
		} else {
			getLog().error("Error getting paths for validation from - OchreUtility.getPathConcepts()");
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
		
		LookupService.startupIsaac();
		
		try {
			UserProfileManager userProfileManager = AppContext.getService(UserProfileManager.class);
			userProfileManager.configureAutomationMode(new File("profiles"));
		} catch (InvalidUserException e) {
			e.printStackTrace();
		}
		
		ReleaseExporter export = new ReleaseExporter();
		
		export.outputFolder = new File("target/output"); //todo - Add FS Seperator 
		export.exportType = new ExportReleaseType[]{ExportReleaseType.SNAPSHOT};
		export.skipExportAssembly = false;
		
		MojoConceptSpec mojoConceptSpec = new MojoConceptSpec();
		mojoConceptSpec.setFsn("ISAAC Development Path");
		mojoConceptSpec.setUuid(IsaacMetadataAuxiliaryBinding.DEVELOPMENT.getPrimodialUuid().toString()); //32d7e06d-c8ae-516d-8a33-df5bcc9c9ec7
		
		HashSet<String> modules = new HashSet<String>();
		modules.add("45dc5146-b0bb-3ca9-8876-0e702fa29f42"); //The module was manually copied from one of the concepts, uknown name
		//modules.add(Snomed.US_EXTENSION_MODULE.getPrimodialUuid().toString());
		//modules.add(Snomed.CORE_MODULE.getPrimodialUuid().toString());
		export.modules = modules;
		
		export.path = mojoConceptSpec;
		
		//export.exportFormat = ExportMojoFormat.Econcept.name.toString();
		export.exportFormat = ExportMojoFormat.Uscrs.name.toString();
		
		//export.uscrsDateFilter = new Date(668822400000L); // 3/13/1991
		//export.uscrsDateFilter = new Date(1331596800000L); //3/13/2012
		export.uscrsDateFilter = new Date(1296432000000L); // 1/31/2011
		//export.uscrsDateFilter  = new Date(1422747000000L); // 1/31/2015 11:30 PM
		
		export.namespace = 1000161;
		try {
			export.execute();
		} catch (MojoExecutionException e) {
			e.printStackTrace();
		}
		
		//SHUTDOWN
		LookupService.shutdownIsaac();
		
	}
}