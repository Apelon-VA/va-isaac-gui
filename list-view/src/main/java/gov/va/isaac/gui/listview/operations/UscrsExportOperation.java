package gov.va.isaac.gui.listview.operations;

/*
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

import gov.va.isaac.gui.SimpleDisplayConcept;
import gov.va.isaac.gui.util.ErrorMarkerUtils;
import gov.va.isaac.gui.util.Images;
import gov.va.isaac.interfaces.gui.constants.SharedServiceNames;
import gov.va.isaac.interfaces.gui.views.commonFunctionality.ExportTaskHandlerI;
import gov.va.isaac.util.Utility;
import gov.va.isaac.util.ValidBooleanBinding;
import gov.vha.isaac.ochre.api.LookupService;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.IntStream;

import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

import org.apache.commons.lang3.time.DateUtils;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* {@link EConceptExportOperation}
* 
* 
* @author <a href="mailto:vkaloidis@apelon.com">Vas Kaloidis</a>
*/
@Service
@PerLookup
public class UscrsExportOperation extends Operation
{
	protected final ObjectProperty<File> fileProperty = new SimpleObjectProperty<>();
	protected final ObjectProperty<TextField> outputProperty = new SimpleObjectProperty<>();
	
	private Logger logger_ = LoggerFactory.getLogger(this.getClass());
	
	private Map<String, Set<String>> successCons = new HashMap<>();
	
	private final FileChooser fileChooser = new FileChooser();
	private Button openFileChooser = new Button(null, Images.FOLDER.createImageView());
	private GridPane root = new GridPane();
	public File file = null;
	private String filePath = "";
	private TextField outputField = new TextField();
	private DatePicker datePicker = new DatePicker();
	private CheckBox skipFilterCheckbox = new CheckBox();
	
	private ValidBooleanBinding allFieldsValid;
//	private DataOutputStream dos_;
	public String fileName = "";
	
	private UscrsExportOperation()
	{
		//For HK2 to init
	}
	
	public void setFileName(String fn)
	{
		fileName = fn;
	}
	
	public String getFileName()
	{
		return fileName;
	}
	
	public void chooseFileName() {
		String date = new SimpleDateFormat("MM-dd-yyyy").format(new Date());
		if(conceptList_.size() > 1) {
			fileName = "VA_USCRS_Submission_File_Multiple_Concepts_" + date + "";
		} else if(conceptList_.size() == 1) {
			String singleConceptName = conceptList_.get(0).getDescription();
			fileName = "VA_USCRS_Submission_File_" + singleConceptName  + "_" + date; 
		} else {
			fileName = "VA_USCRS_Submission_File_" + date;
		}
	}
	
	/**
	 * Pass in a Date and the function will return a Date, but at the start of that day.
	 * @param Date the day and time you would like to modify
	 * @return Date and time at the beginning of the day
	 */
	public static Date getStartOfDay(Date date) {
	    return DateUtils.truncate(date, Calendar.DATE);
	}
	
	@Override
	public void init(ObservableList<SimpleDisplayConcept> conceptList)
	{
		root.setHgap(10); 
		root.setVgap(10);
		
		super.init(conceptList);
		
		String date = new SimpleDateFormat("MM-dd-yyyy HH-mm-ss").format(new Date());
		fileName = "VA_USCRS_Submission_File_" + date;

		//this.chooseFileName(); TODO: Finish the file name system
		fileChooser.setTitle("Save USCRS Concept Request File");
		fileChooser.getExtensionFilters().addAll(new ExtensionFilter("Excel Files .xls .xlsx", "*.xls", "*.xlsx"));
		fileChooser.setInitialFileName(fileName);
//		outputProperty.set(outputField.textProperty());
		
		openFileChooser.setOnAction(
			new EventHandler<ActionEvent>() {
				@Override
				public void handle(final ActionEvent e) {
					file = fileChooser.showSaveDialog(null);
					if(file != null)
					{
						if(file.getAbsolutePath() != null && file.getAbsolutePath() != "")
						{
							outputField.setText(file.getAbsolutePath());
							filePath = file.getAbsolutePath();
							logger_.info("File Path Changed: " + filePath);
						} 
					}
				}
			});
		outputField.setOnAction( //TODO: So you can manually type in a file path and it validates (break this out)
			new EventHandler<ActionEvent>() {
				@Override
				public void handle(final ActionEvent e) {
					filePath = outputField.getText();
					file = new File(filePath);
				}
			});
		
		allFieldsValid = new ValidBooleanBinding() {
			{
				bind(outputField.textProperty());
				setComputeOnInvalidate(true);
			}
			/* (non-Javadoc)
			 * @see javafx.beans.binding.BooleanBinding#computeValue()
			 */
			@Override
			protected boolean computeValue()
			{
				if(outputField.getText() != "" || !outputField.getText().trim().isEmpty()) 
				{
					String fieldOutput = outputField.getText();
					if(filePath != null && !fieldOutput.isEmpty() && file != null) //fieldOutput is repetetive but necessary
					{
						int lastSeperatorPosition = outputField.getText().lastIndexOf(File.separator);
						String path = "";
						if(lastSeperatorPosition > 0) {
							path = outputField.getText().substring(0, lastSeperatorPosition);
						} else {
							path = outputField.getText();
						}
						
						logger_.debug("Output Directory: " + path);
						File f = new File(path);
						if(file.isFile()) { //If we want to prevent file overwrite
							this.setInvalidReason("The file " + filePath + " already exists");
							return false;
						} else if(f.isDirectory()) {
							return true;
						} else { 
							this.setInvalidReason("Output Path is not a directory");
							return false;
						}
					} else {
						this.setInvalidReason("File Output Directory is not set - output field null!!");
						return false;
					}
				}
				else
				{
					this.setInvalidReason("Output field is empty, output directory is not set");
					return false;
				} 
			}
		};

		root.add(openFileChooser, 2, 0); 
		GridPane.setHalignment(openFileChooser, HPos.LEFT);
		
		Label outputLocationLabel = new Label("Output Location");
		root.add(outputLocationLabel, 0, 0);
		
		GridPane.setHalignment(outputLocationLabel, HPos.LEFT);
		
		
		StackPane sp = ErrorMarkerUtils.setupErrorMarker(outputField, null, allFieldsValid);
		root.add(sp, 1, 0);
		GridPane.setHgrow(sp, Priority.ALWAYS); 
		GridPane.setHalignment(sp, HPos.LEFT);
		
		Label datePickerLabel = new Label("Export Date Filter");
		root.add(datePickerLabel, 0, 1); 
		GridPane.setHalignment(datePickerLabel, HPos.LEFT);
		root.add(datePicker, 1, 1);
		
		Label allDatesLabel = new Label("Export All Concepts");
		root.add(allDatesLabel, 0, 2);
		skipFilterCheckbox.setText("Export All Concepts (No Filters)");
		skipFilterCheckbox.setSelected(false);
		root.add(skipFilterCheckbox, 1, 2);
		
		
		super.root_ = root;
	}
	/**
	 * @see gov.va.isaac.gui.listview.operations.Operation#getTitle()
	 */
	@Override
	public String getTitle()
	{
		return "USCRS Content Request Export";
	}

	/**
	 * @see gov.va.isaac.gui.listview.operations.Operation#conceptListChanged()
	 */
	@Override
	protected void conceptListChanged()
	{
		//noop
	}

	/**
	 * @see gov.va.isaac.gui.listview.operations.Operation#isValid()
	 */
	@Override
	public BooleanExpression isValid()
	{
		return allFieldsValid;
	}

	/**
	 * @see gov.va.isaac.gui.listview.operations.Operation#getOperationDescription()
	 */
	@Override
	public String getOperationDescription()
	{
		return "Performa  USCRS Content Export to an Excel File";
	}

	/**
	 * @see gov.va.isaac.gui.listview.operations.Operation#createTask()
	 */
	@Override
	public CustomTask<OperationResult> createTask()
	{
		return new CustomTask<OperationResult>(UscrsExportOperation.this)
		{
			@Override
			protected OperationResult call() throws Exception
			{
				IntStream nidStream = conceptList_.stream().mapToInt(c -> c.getNid());
				int count = 0;
				ExportTaskHandlerI uscrsExporter = LookupService.getService(ExportTaskHandlerI.class, SharedServiceNames.USCRS);
				if(uscrsExporter != null) {
					
					updateMessage("Beginning USCRS Export ");
					
					if(cancelRequested_) {
						return new OperationResult(UscrsExportOperation.this.getTitle(), cancelRequested_);
					}
					
					if(!skipFilterCheckbox.isSelected() && datePicker.getValue() != null) {
						
						Properties options = new Properties();
						Instant instant = Instant.from(datePicker.getValue().atStartOfDay(ZoneId.systemDefault()));
						Long dateSelected = Date.from(instant).getTime();
						updateMessage("USCRS Export - Date filter selected: " + dateSelected.toString());
						options.setProperty("date", Long.toString(dateSelected));
						uscrsExporter.setOptions(options);
					}
					
					if(cancelRequested_) {
						return new OperationResult(UscrsExportOperation.this.getTitle(), cancelRequested_);
					}
					
					updateMessage("Beginning USCRS Export Handler Task");
					
					try {
						Task<Integer> task = uscrsExporter.createTask(nidStream, file.toPath());
						Utility.execute(task);
						count = task.get();
					} catch(FileNotFoundException fnfe) {
						String errorMsg = "File is being used by another application. Close the other application to continue.";
						updateMessage(errorMsg);
						throw new RuntimeException(errorMsg);
					}
					
					
					return new OperationResult("The USCRS Content request was succesfully generated in: " + file.getPath(), new HashSet<SimpleDisplayConcept>(), "The concepts were succesfully exported");
				} else {
					throw new RuntimeException("The USCRS Content Request Handler is not available on the class path");
				}
			}
		};
	}


}
