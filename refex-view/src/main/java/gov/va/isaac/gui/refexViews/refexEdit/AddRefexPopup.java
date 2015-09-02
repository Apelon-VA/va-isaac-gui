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
package gov.va.isaac.gui.refexViews.refexEdit;

import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.runlevel.RunLevelException;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sun.javafx.collections.ObservableListWrapper;
import gov.va.isaac.AppContext;
import gov.va.isaac.ExtendedAppContext;
import gov.va.isaac.gui.ConceptNode;
import gov.va.isaac.gui.SimpleDisplayConcept;
import gov.va.isaac.gui.dragAndDrop.DragRegistry;
import gov.va.isaac.gui.refexViews.util.RefexDataTypeFXNodeBuilder;
import gov.va.isaac.gui.refexViews.util.RefexDataTypeNodeDetails;
import gov.va.isaac.gui.util.CopyableLabel;
import gov.va.isaac.gui.util.ErrorMarkerUtils;
import gov.va.isaac.gui.util.FxUtils;
import gov.va.isaac.gui.util.Images;
import gov.va.isaac.interfaces.gui.views.PopupViewI;
import gov.va.isaac.util.CommonlyUsedConcepts;
import gov.va.isaac.util.OchreUtility;
import gov.va.isaac.util.UpdateableBooleanBinding;
import gov.va.isaac.util.Utility;
import gov.va.isaac.util.ValidBooleanBinding;
import gov.vha.isaac.metadata.coordinates.EditCoordinates;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.State;
import gov.vha.isaac.ochre.api.chronicle.ObjectChronologyType;
import gov.vha.isaac.ochre.api.commit.ChangeCheckerMode;
import gov.vha.isaac.ochre.api.component.concept.ConceptSnapshot;
import gov.vha.isaac.ochre.api.component.sememe.SememeChronology;
import gov.vha.isaac.ochre.api.component.sememe.SememeType;
import gov.vha.isaac.ochre.api.component.sememe.version.DynamicSememe;
import gov.vha.isaac.ochre.api.component.sememe.version.SememeVersion;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeColumnInfo;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeDataBI;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeDataType;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeUsageDescriptionBI;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeValidatorType;
import gov.vha.isaac.ochre.impl.sememe.DynamicSememeUsageDescription;
import gov.vha.isaac.ochre.model.constants.IsaacMetadataConstants;
import gov.vha.isaac.ochre.model.sememe.dataTypes.DynamicSememeData;
import gov.vha.isaac.ochre.model.sememe.dataTypes.DynamicSememeNid;
import gov.vha.isaac.ochre.model.sememe.dataTypes.DynamicSememeString;
import gov.vha.isaac.ochre.model.sememe.dataTypes.DynamicSememeUUID;
import gov.vha.isaac.ochre.model.sememe.version.DynamicSememeImpl;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.util.StringConverter;

/**
 * 
 * Refset View
 * 
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */

@Service
@PerLookup
public class AddRefexPopup extends Stage implements PopupViewI
{
	private DynamicSememeView callingView_;
	private ViewFocus createRefexFocus_;
	private int focusNid_;
	private RefexDynamicGUI editRefex_;
	private Label unselectableComponentLabel_;;
	private ScrollPane sp_;
	//TODO (artf231426) improve 'ConceptNode' - this mess of Conceptnode or TextField will work for now, if they set a component type restriction
	//But if they don't set a component type restriction, we still need the field (conceptNode) to allow nids or UUIDs of other types of things.
	//both here, and in the GUI that creates the sememe - when specifying the default value.
	private ConceptNode selectableConcept_;
	private StackPane selectableComponentNode_;
	private ValidBooleanBinding selectableComponentNodeValid_;
	private TextField selectableComponent_;
	private boolean conceptNodeIsConceptType_ = false;
	private DynamicSememeUsageDescriptionBI assemblageInfo_;
	private SimpleBooleanProperty assemblageIsValid_ = new SimpleBooleanProperty(false);
	private Logger logger_ = LoggerFactory.getLogger(this.getClass());
	private UpdateableBooleanBinding allValid_;

	private ArrayList<ReadOnlyStringProperty> currentDataFieldWarnings_ = new ArrayList<>();
	private ArrayList<RefexDataTypeNodeDetails> currentDataFields_ = new ArrayList<>();
	private ObservableList<SimpleDisplayConcept> refexDropDownOptions = FXCollections.observableArrayList();
	private GridPane gp_;
	private Label title_;
	private SememeType st_;

	private AddRefexPopup()
	{
		super();
		BorderPane root = new BorderPane();

		VBox topItems = new VBox();
		topItems.setFillWidth(true);

		title_ = new Label("Create new sememe instance");
		title_.getStyleClass().add("titleLabel");
		title_.setAlignment(Pos.CENTER);
		title_.prefWidthProperty().bind(topItems.widthProperty());
		topItems.getChildren().add(title_);
		VBox.setMargin(title_, new Insets(10, 10, 10, 10));

		gp_ = new GridPane();
		gp_.setHgap(10.0);
		gp_.setVgap(10.0);
		VBox.setMargin(gp_, new Insets(5, 5, 5, 5));
		topItems.getChildren().add(gp_);

		Label referencedComponent = new Label("Referenced Component");
		referencedComponent.getStyleClass().add("boldLabel");
		gp_.add(referencedComponent, 0, 0);

		unselectableComponentLabel_ = new CopyableLabel();
		unselectableComponentLabel_.setWrapText(true);
		AppContext.getService(DragRegistry.class).setupDragOnly(unselectableComponentLabel_, () -> 
		{
			if (editRefex_ == null)
			{
				return focusNid_ + "";
			}
			else
			{
				return Get.identifierService().getConceptNid(editRefex_.getSememe().getAssemblageSequence()) + "";
			}
		});
		//delay adding till we know which row

		Label assemblageConceptLabel = new Label("Assemblage Concept");
		assemblageConceptLabel.getStyleClass().add("boldLabel");
		gp_.add(assemblageConceptLabel, 0, 1);

		selectableConcept_ = new ConceptNode(null, true, refexDropDownOptions, null);

		selectableConcept_.getConceptProperty().addListener(new ChangeListener<ConceptSnapshot>()
		{
			@Override
			public void changed(ObservableValue<? extends ConceptSnapshot> observable, ConceptSnapshot oldValue, ConceptSnapshot newValue)
			{
				if (createRefexFocus_ != null && createRefexFocus_ == ViewFocus.REFERENCED_COMPONENT)
				{
					if (selectableConcept_.isValid().get())
					{
						//Its a valid concept, but is it a valid assemblage concept?
						try
						{
							assemblageInfo_ = DynamicSememeUsageDescription.read(selectableConcept_.getConceptNoWait().getNid());
							assemblageIsValid_.set(true);
							if (assemblageInfo_.getReferencedComponentTypeRestriction() != null)
							{
								String result = DynamicSememeValidatorType.COMPONENT_TYPE.passesValidatorStringReturn(
										new DynamicSememeNid(focusNid_), 
										new DynamicSememeString(assemblageInfo_.getReferencedComponentTypeRestriction().name()), null, null);  //don't need coordinates for component type validator
								if (result.length() > 0)
								{
									selectableConcept_.isValid().setInvalid("The selected assemblage requires the component type to be " 
											+ assemblageInfo_.getReferencedComponentTypeRestriction().toString() + ", which doesn't match the referenced component.");
									logger_.info("The selected assemblage requires the component type to be " 
											+ assemblageInfo_.getReferencedComponentTypeRestriction().toString() + ", which doesn't match the referenced component.");
									assemblageIsValid_.set(false);
								}
							}
						}
						catch (Exception e)
						{
							selectableConcept_.isValid().setInvalid("The selected concept is not properly constructed for use as an Assemblage concept");
							logger_.info("Concept not a valid concept for a sememe assemblage", e);
							assemblageIsValid_.set(false);
						}
					}
					else
					{
						assemblageInfo_ = null;
						assemblageIsValid_.set(false);
					}
					buildDataFields(assemblageIsValid_.get(), null);
				}
			}
		});

		selectableComponent_ = new TextField();
		
		selectableComponentNodeValid_ = new ValidBooleanBinding()
		{
			{
				setComputeOnInvalidate(true);
				bind(selectableComponent_.textProperty());
				invalidate();
			}
			@Override
			protected boolean computeValue()
			{
				if (createRefexFocus_ != null && createRefexFocus_ == ViewFocus.ASSEMBLAGE && !conceptNodeIsConceptType_)
				{
					//If the assembly nid was what was set - the component node may vary - validate if we are using the text field 
					String value = selectableComponent_.getText().trim();
					if (value.length() > 0)
					{
						try
						{
							if (Utility.isUUID(value))
							{
								String result = DynamicSememeValidatorType.COMPONENT_TYPE.passesValidatorStringReturn(new DynamicSememeUUID(UUID.fromString(value)), 
										new DynamicSememeString(assemblageInfo_.getReferencedComponentTypeRestriction().name()), null, null);  //component type validator doesn't use vc, so null is ok
								if (result.length() > 0)
								{
									setInvalidReason(result);
									logger_.info(result);
									return false;
								}
							}
							else if (Utility.isInt(value))
							{
								String result = DynamicSememeValidatorType.COMPONENT_TYPE.passesValidatorStringReturn(new DynamicSememeNid(Integer.parseInt(value)), 
										new DynamicSememeString(assemblageInfo_.getReferencedComponentTypeRestriction().name()), null, null);  //component type validator doesn't use vc, so null is ok
								if (result.length() > 0)
								{
									setInvalidReason(result);
									logger_.info(result);
									return false;
								}
							}
							else
							{
								setInvalidReason("Value cannot be parsed as a component identifier.  Must be a UUID or a valid NID");
								return false;
							}
						}
						catch (Exception e)
						{
							logger_.error("Error checking component type validation", e);
							setInvalidReason("Unexpected error validating entry");
							return false;
						}
					}
					else
					{
						setInvalidReason("Component identifier is required");
						return false;
					}
				}
				clearInvalidReason();
				return true;
			}
		};
		
		selectableComponentNode_ = ErrorMarkerUtils.setupErrorMarker(selectableComponent_, null, selectableComponentNodeValid_);
		
		//delay adding concept / component till we know if / where
		ColumnConstraints cc = new ColumnConstraints();
		cc.setHgrow(Priority.NEVER);
		cc.setMinWidth(FxUtils.calculateNecessaryWidthOfBoldLabel(referencedComponent));
		gp_.getColumnConstraints().add(cc);

		cc = new ColumnConstraints();
		cc.setHgrow(Priority.ALWAYS);
		gp_.getColumnConstraints().add(cc);

		Label l = new Label("Data Fields");
		l.getStyleClass().add("boldLabel");
		VBox.setMargin(l, new Insets(5, 5, 5, 5));
		topItems.getChildren().add(l);

		root.setTop(topItems);

		sp_ = new ScrollPane();
		sp_.visibleProperty().bind(assemblageIsValid_);
		sp_.setFitToHeight(true);
		sp_.setFitToWidth(true);
		root.setCenter(sp_);

		allValid_ = new UpdateableBooleanBinding()
		{
			{
				addBinding(assemblageIsValid_, selectableConcept_.isValid(), selectableComponentNodeValid_);
			}

			@Override
			protected boolean computeValue()
			{
				if (assemblageIsValid_.get() && (conceptNodeIsConceptType_ ? selectableConcept_.isValid().get() : selectableComponentNodeValid_.get()))
				{
					boolean allDataValid = true;
					for (ReadOnlyStringProperty ssp : currentDataFieldWarnings_)
					{
						if (ssp.get().length() > 0)
						{
							allDataValid = false;
							break;
						}
					}
					if (allDataValid)
					{
						clearInvalidReason();
						return true;
					}
				}
				setInvalidReason("All errors must be corrected before save is allowed");
				return false;
			}
		};

		GridPane bottomRow = new GridPane();
		//spacer col
		bottomRow.add(new Region(), 0, 0);

		Button cancelButton = new Button("Cancel");
		cancelButton.setOnAction((action) -> {
			close();
		});
		GridPane.setMargin(cancelButton, new Insets(5, 20, 5, 0));
		GridPane.setHalignment(cancelButton, HPos.RIGHT);
		bottomRow.add(cancelButton, 1, 0);

		Button saveButton = new Button("Save");
		saveButton.disableProperty().bind(allValid_.not());
		saveButton.setOnAction((action) -> {
			doSave();
		});
		Node wrappedSave = ErrorMarkerUtils.setupDisabledInfoMarker(saveButton, allValid_.getReasonWhyInvalid());
		GridPane.setMargin(wrappedSave, new Insets(5, 0, 5, 20));
		bottomRow.add(wrappedSave, 2, 0);

		//spacer col
		bottomRow.add(new Region(), 3, 0);

		cc = new ColumnConstraints();
		cc.setHgrow(Priority.SOMETIMES);
		cc.setFillWidth(true);
		bottomRow.getColumnConstraints().add(cc);
		cc = new ColumnConstraints();
		cc.setHgrow(Priority.NEVER);
		bottomRow.getColumnConstraints().add(cc);
		cc = new ColumnConstraints();
		cc.setHgrow(Priority.NEVER);
		bottomRow.getColumnConstraints().add(cc);
		cc = new ColumnConstraints();
		cc.setHgrow(Priority.SOMETIMES);
		cc.setFillWidth(true);
		bottomRow.getColumnConstraints().add(cc);
		root.setBottom(bottomRow);

		Scene scene = new Scene(root);
		scene.getStylesheets().add(AddRefexPopup.class.getResource("/isaac-shared-styles.css").toString());
		setScene(scene);
	}
	
	public void finishInit(RefexDynamicGUI refexToEdit, DynamicSememeView viewToRefresh)
	{
		callingView_ = viewToRefresh;
		createRefexFocus_ = null;
		editRefex_ = refexToEdit;
		st_ = refexToEdit.getSememe().getChronology().getSememeType();
		
		title_.setText("Edit existing sememe instance");
		
		gp_.add(unselectableComponentLabel_, 1, 1);
		unselectableComponentLabel_.setText(Get.conceptDescriptionText(editRefex_.getSememe().getAssemblageSequence()));
		
		//TODO this assuming that the referenced component is a concept,  which I don't think is a safe assumption.
		
		//don't actually put this in the view
		selectableConcept_.set(new SimpleDisplayConcept(editRefex_.getSememe().getReferencedComponentNid()));
		
		Label refComp = new CopyableLabel(Get.conceptDescriptionText(editRefex_.getSememe().getReferencedComponentNid()));
		refComp.setWrapText(true);
		AppContext.getService(DragRegistry.class).setupDragOnly(refComp, () -> {return editRefex_.getSememe().getReferencedComponentNid() + "";});
		gp_.add(refComp, 1, 0);
		refexDropDownOptions.clear();
		try
		{
			assemblageInfo_ = DynamicSememeUsageDescription.mockOrRead(((SememeVersion<?>)editRefex_.getSememe()).getChronology());
			assemblageIsValid_.set(true);
			buildDataFields(true, RefexDynamicGUI.getData(editRefex_.getSememe()));
		}
		catch (Exception e)
		{
			logger_.error("Unexpected", e);
			AppContext.getCommonDialogs().showErrorDialog("Unexpected Error reading Assembly concept", e);
		}
	}

	public void finishInit(ViewFocus viewFocus, int focusNid, DynamicSememeView viewToRefresh)
	{
		callingView_ = viewToRefresh;
		createRefexFocus_ = viewFocus;
		focusNid_ = focusNid;
		editRefex_ = null;
		st_ = SememeType.DYNAMIC;  //TODO add to the GUI, the option to choose other types
		
		title_.setText("Create new sememe instance");

		if (createRefexFocus_ == ViewFocus.REFERENCED_COMPONENT)
		{
			gp_.add(unselectableComponentLabel_, 1, 0);
			unselectableComponentLabel_.setText(Get.conceptDescriptionText(focusNid_));
			gp_.add(selectableConcept_.getNode(), 1, 1);
			refexDropDownOptions.clear();
			refexDropDownOptions.addAll(buildAssemblageConceptList());
		}
		else
		{
			try
			{
				assemblageInfo_ = DynamicSememeUsageDescription.read(focusNid_);
				gp_.add(unselectableComponentLabel_, 1, 1);
				unselectableComponentLabel_.setText(Get.conceptDescriptionText(focusNid_));
				if (assemblageInfo_.getReferencedComponentTypeRestriction() != null 
						&& ObjectChronologyType.CONCEPT != assemblageInfo_.getReferencedComponentTypeRestriction())
				{
					conceptNodeIsConceptType_ = false;
					gp_.add(selectableComponentNode_, 1, 0);
					selectableComponent_.setPromptText("UUID or NID of a " + assemblageInfo_.getReferencedComponentTypeRestriction().toString());
					selectableComponent_.setTooltip(new Tooltip("UUID or NID of a " + assemblageInfo_.getReferencedComponentTypeRestriction().toString()));
					selectableComponentNodeValid_.invalidate();
				}
				else
				{
					conceptNodeIsConceptType_ = true;
					gp_.add(selectableConcept_.getNode(), 1, 0);
				}
				refexDropDownOptions.clear();
				refexDropDownOptions.addAll(AppContext.getService(CommonlyUsedConcepts.class).getObservableConcepts());
				assemblageIsValid_.set(true);
				buildDataFields(true, null);
			}
			catch (Exception e)
			{
				logger_.error("Unexpected", e);
				AppContext.getCommonDialogs().showErrorDialog("Unexpected Error reading Assembly concept", e);
			}
		}
	}
	
	private void buildDataFields(boolean assemblageValid, DynamicSememeDataBI[] currentValues)
	{
		if (assemblageValid)
		{
			for (ReadOnlyStringProperty ssp : currentDataFieldWarnings_)
			{
				allValid_.removeBinding(ssp);
			}
			currentDataFieldWarnings_.clear();
			for (RefexDataTypeNodeDetails nd : currentDataFields_)
			{
				nd.cleanupListener();
			}
			currentDataFields_.clear();

			GridPane gp = new GridPane();
			gp.setHgap(10.0);
			gp.setVgap(10.0);
			gp.setStyle("-fx-padding: 5;");
			int row = 0;
			boolean extraInfoColumnIsRequired = false;
			for (DynamicSememeColumnInfo ci : assemblageInfo_.getColumnInfo())
			{
				SimpleStringProperty valueIsRequired = (ci.isColumnRequired() ? new SimpleStringProperty("") : null);
				SimpleStringProperty defaultValueTooltip = ((ci.getDefaultColumnValue() == null && ci.getValidator() == null) ? null : new SimpleStringProperty());
				ComboBox<DynamicSememeDataType> polymorphicType = null;
				
				Label l = new Label(ci.getColumnName());
				l.getStyleClass().add("boldLabel");
				l.setMinWidth(FxUtils.calculateNecessaryWidthOfBoldLabel(l));
				Tooltip.install(l, new Tooltip(ci.getColumnDescription()));
				int col = 0;
				gp.add(l, col++, row);
				
				if (ci.getColumnDataType() == DynamicSememeDataType.POLYMORPHIC)
				{
					polymorphicType = new ComboBox<>();
					polymorphicType.setEditable(false);
					polymorphicType.setConverter(new StringConverter<DynamicSememeDataType>()
					{
						
						@Override
						public String toString(DynamicSememeDataType object)
						{
							return object.getDisplayName();
						}
						
						@Override
						public DynamicSememeDataType fromString(String string)
						{
							throw new RuntimeException("unecessary");
						}
					});
					for (DynamicSememeDataType type : DynamicSememeDataType.values())
					{
						if (type == DynamicSememeDataType.POLYMORPHIC || type == DynamicSememeDataType.UNKNOWN)
						{
							continue;
						}
						else
						{
							polymorphicType.getItems().add(type);
						}
					}
					polymorphicType.getSelectionModel().select((currentValues == null ? DynamicSememeDataType.STRING :
						(currentValues[row] == null ? DynamicSememeDataType.STRING : currentValues[row].getDynamicSememeDataType())));
				}
				
				RefexDataTypeNodeDetails nd = RefexDataTypeFXNodeBuilder.buildNodeForType(ci.getColumnDataType(), ci.getDefaultColumnValue(), 
						(currentValues == null ? null : currentValues[row]),valueIsRequired, defaultValueTooltip, 
						(polymorphicType == null ? null : polymorphicType.getSelectionModel().selectedItemProperty()), allValid_,
						ci.getValidator(), ci.getValidatorData());
				
				currentDataFieldWarnings_.addAll(nd.getBoundToAllValid());
				if (ci.getColumnDataType() == DynamicSememeDataType.POLYMORPHIC)
				{
					nd.addUpdateParentListListener(currentDataFieldWarnings_);
				}
				
				currentDataFields_.add(nd);
				
				gp.add(nd.getNodeForDisplay(), col++, row);
				
				Label colType = new Label(ci.getColumnDataType().getDisplayName());
				colType.setMinWidth(FxUtils.calculateNecessaryWidthOfLabel(colType));
				gp.add((polymorphicType == null ? colType : polymorphicType), col++, row);
				
				if (ci.isColumnRequired() || ci.getDefaultColumnValue() != null || ci.getValidator() != null)
				{
					extraInfoColumnIsRequired = true;
					
					StackPane stackPane = new StackPane();
					stackPane.setMaxWidth(Double.MAX_VALUE);
					
					if (ci.getDefaultColumnValue() != null || ci.getValidator() != null)
					{
						ImageView information = Images.INFORMATION.createImageView();
						Tooltip tooltip = new Tooltip();
						tooltip.textProperty().bind(defaultValueTooltip);
						Tooltip.install(information, tooltip);
						tooltip.setAutoHide(true);
						information.setOnMouseClicked(event -> tooltip.show(information, event.getScreenX(), event.getScreenY()));
						stackPane.getChildren().add(information);
					}
					
					if (ci.isColumnRequired())
					{
						ImageView exclamation = Images.EXCLAMATION.createImageView();

						final BooleanProperty showExclamation = new SimpleBooleanProperty(StringUtils.isNotBlank(valueIsRequired.get()));
						valueIsRequired.addListener((ChangeListener<String>) (observable, oldValue, newValue) -> showExclamation.set(StringUtils.isNotBlank(newValue)));

						exclamation.visibleProperty().bind(showExclamation);
						Tooltip tooltip = new Tooltip();
						tooltip.textProperty().bind(valueIsRequired);
						Tooltip.install(exclamation, tooltip);
						tooltip.setAutoHide(true);
						
						exclamation.setOnMouseClicked(event -> tooltip.show(exclamation, event.getScreenX(), event.getScreenY()));
						stackPane.getChildren().add(exclamation);
					}

					gp.add(stackPane, col++, row);
				}
				row++;
			}

			ColumnConstraints cc = new ColumnConstraints();
			cc.setHgrow(Priority.NEVER);
			gp.getColumnConstraints().add(cc);

			cc = new ColumnConstraints();
			cc.setHgrow(Priority.ALWAYS);
			gp.getColumnConstraints().add(cc);

			cc = new ColumnConstraints();
			cc.setHgrow(Priority.NEVER);
			gp.getColumnConstraints().add(cc);
			
			if (extraInfoColumnIsRequired)
			{
				cc = new ColumnConstraints();
				cc.setHgrow(Priority.NEVER);
				gp.getColumnConstraints().add(cc);
			}

			if (row == 0)
			{
				sp_.setContent(new Label("This assemblage does not allow data fields"));
			}
			else
			{
				sp_.setContent(gp);
			}
			allValid_.invalidate();
		}
		else
		{
			sp_.setContent(null);
		}
	}

	@SuppressWarnings("unchecked")
	private void doSave()
	{
		try
		{
			DynamicSememeDataBI[] data = new DynamicSememeData[assemblageInfo_.getColumnInfo().length];
			int i = 0;
			for (DynamicSememeColumnInfo ci : assemblageInfo_.getColumnInfo())
			{
				data[i] = RefexDataTypeFXNodeBuilder.getDataForType(currentDataFields_.get(i++).getDataField(), ci);
			}
			
			SememeChronology<?> sememe;
			if (createRefexFocus_ != null)
			{
				int componentNid;
				int assemblageSequence;
				if (createRefexFocus_ == ViewFocus.ASSEMBLAGE)
				{
					if (conceptNodeIsConceptType_)
					{
						componentNid = selectableConcept_.getConcept().getNid();
					}
					else
					{
						String value = selectableComponent_.getText().trim();
						if (Utility.isUUID(value))
						{
							componentNid = Get.identifierService().getNidForUuids(UUID.fromString(value));
						}
						else
						{
							componentNid = Integer.parseInt(value);
						}
					}
					
					assemblageSequence = focusNid_;
				}
				else
				{
					componentNid = focusNid_;
					assemblageSequence =  selectableConcept_.getConcept().getConceptSequence();
				}
				
				if (st_ == SememeType.DYNAMIC)
				{
					sememe = Get.sememeBuilderService().getDyanmicSememeBuilder(componentNid, assemblageSequence, data)
						.build(EditCoordinates.getDefaultUserMetadata(), ChangeCheckerMode.ACTIVE);
				}
				else
				{
					//TODO implement
					throw new RuntimeException("Edit of non-dynamic sememe types not yet implemented");
				}
			}
			else
			{
				if (st_ == SememeType.DYNAMIC)
				{
					@SuppressWarnings("rawtypes")
					DynamicSememeImpl ms = (DynamicSememeImpl) ((SememeChronology)editRefex_.getSememe()).createMutableVersion(DynamicSememeImpl.class, State.ACTIVE, 
							ExtendedAppContext.getUserProfileBindings().getEditCoordinate().get());
					ms.setData(data);
					sememe = (SememeChronology<? extends DynamicSememe<?>>) ms;
				}
				else
				{
					//TODO implement
					throw new RuntimeException("Edit of non-dynamic sememe types not yet implemented");
				}
			}

			Get.commitService().addUncommitted(sememe);
			Get.commitService().commit("Editing / adding a sememe").get();
			
			
			if (callingView_ != null)
			{
				callingView_.refresh();
			}
			close();
		}
		catch (Exception e)
		{
			logger_.error("Error saving sememe", e);
			AppContext.getCommonDialogs().showErrorDialog("Unexpected error", "There was an error saving the sememe", e.getMessage(), this);
		}
	}

	/**
	 * Call setReferencedComponent first
	 * 
	 * @see gov.va.isaac.interfaces.gui.views.PopupViewI#showView(javafx.stage.Window)
	 */
	@Override
	public void showView(Window parent)
	{
		if (createRefexFocus_ == null && editRefex_ == null)
		{
			throw new RunLevelException("referenced component nid must be set first");
		}
		setTitle("Create new Sememe");
		setResizable(true);

		initOwner(parent);
		initModality(Modality.NONE);
		initStyle(StageStyle.DECORATED);

		setWidth(600);
		setHeight(400);

		show();
	}
	
	private ObservableList<SimpleDisplayConcept> buildAssemblageConceptList()
	{
		ObservableList<SimpleDisplayConcept> assemblageConcepts = new ObservableListWrapper<>(new ArrayList<SimpleDisplayConcept>());
		try
		{
			//TODO Dan should we use sememe usage instead now?
			Set<Integer> colCons = OchreUtility.getAllChildrenOfConcept(IsaacMetadataConstants.DYNAMIC_SEMEME_ASSEMBLAGES.getSequence(), false, false);

			for (Integer col : colCons) {
				assemblageConcepts.add(new SimpleDisplayConcept(col));
			}
		}
		catch (Exception e)
		{
			logger_.error("Unexpected error reading existing assemblage concepts", e);
		}

		return assemblageConcepts;
	}
}