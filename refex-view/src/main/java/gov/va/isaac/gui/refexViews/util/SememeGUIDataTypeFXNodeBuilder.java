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
package gov.va.isaac.gui.refexViews.util;

import gov.va.isaac.AppContext;
import gov.va.isaac.ExtendedAppContext;
import gov.va.isaac.drools.manager.DroolsExecutorsManager;
import gov.va.isaac.drools.refexUtils.SememeDroolsValidator;
import gov.va.isaac.drools.refexUtils.SememeDroolsValidatorImplInfo;
import gov.va.isaac.gui.ConceptNode;
import gov.va.isaac.gui.util.ErrorMarkerUtils;
import gov.va.isaac.util.OchreUtility;
import gov.va.isaac.util.UpdateableBooleanBinding;
import gov.va.isaac.util.Utility;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeColumnInfo;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeDataBI;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeDataType;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeValidatorType;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.dataTypes.DynamicSememeBooleanBI;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.dataTypes.DynamicSememeDoubleBI;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.dataTypes.DynamicSememeFloatBI;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.dataTypes.DynamicSememeIntegerBI;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.dataTypes.DynamicSememeLongBI;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.dataTypes.DynamicSememeNidBI;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.dataTypes.DynamicSememeStringBI;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.dataTypes.DynamicSememeUUIDBI;
import gov.vha.isaac.ochre.model.sememe.dataTypes.DynamicSememeBoolean;
import gov.vha.isaac.ochre.model.sememe.dataTypes.DynamicSememeByteArray;
import gov.vha.isaac.ochre.model.sememe.dataTypes.DynamicSememeDouble;
import gov.vha.isaac.ochre.model.sememe.dataTypes.DynamicSememeFloat;
import gov.vha.isaac.ochre.model.sememe.dataTypes.DynamicSememeInteger;
import gov.vha.isaac.ochre.model.sememe.dataTypes.DynamicSememeLong;
import gov.vha.isaac.ochre.model.sememe.dataTypes.DynamicSememeNid;
import gov.vha.isaac.ochre.model.sememe.dataTypes.DynamicSememeString;
import gov.vha.isaac.ochre.model.sememe.dataTypes.DynamicSememeUUID;
import java.beans.PropertyVetoException;
import java.io.File;
import java.nio.file.Files;
import java.util.Optional;
import java.util.UUID;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link SememeGUIDataTypeFXNodeBuilder}
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a> 
 */
public class SememeGUIDataTypeFXNodeBuilder
{
	private static Logger logger = LoggerFactory.getLogger(SememeGUIDataTypeFXNodeBuilder.class);
	
	/**
	 * @param dt - The datatype of the node to be built
	 * @param defaultValue - The default value for the node (null allowed)
	 * @param currentValue - The current value for the node to prepopulate GUI (null allowed)
	 * @param valueIsRequired - If this value must be set, pass in a SimpleStringProperty - it will be updated with a suitable message when the node is blank.
	 * If this value is not required, pass null. 
	 * @param defaultValueTooltip - If this field has a default value - as a convenience, this pass in a SimpleStringProperty here, and it will update the value
	 * as appropriate with a user friendly string explaining the default value.  (null allowed)
	 * @param polymorphicSelection - If the specified datatype of this node is polymorphic - the user has to choose a type for this instance.  This is for the users
	 * choice.  Null allowed, if the datatype is not polymorphic.
	 * @param allValid - Bindings will be added to this UpdateableBooleanBinding depending on the node construction.  The objects that were bound will be returned in the 
	 * result object.
	 * @param validatorType - if not null, data entered into the field will be validated against the validator.
	 * @param validatorData - if not null (and validatorType is not null) data entered into the field will be validated against the validator.
	 * 
	 */
	public static SememeGUIDataTypeNodeDetails buildNodeForType(DynamicSememeDataType dt, DynamicSememeDataBI defaultValue, DynamicSememeDataBI currentValue, 
			SimpleStringProperty valueIsRequired, SimpleStringProperty defaultValueTooltip, ReadOnlyObjectProperty<DynamicSememeDataType> polymorphicSelection, 
			UpdateableBooleanBinding allValid, ObjectProperty<DynamicSememeValidatorType> validatorType, ObjectProperty<DynamicSememeDataBI> validatorData)
	{
		return buildNodeForType(dt, defaultValue, currentValue, valueIsRequired, defaultValueTooltip, polymorphicSelection, allValid, validatorType, validatorData, true);
	}
	
	public static SememeGUIDataTypeNodeDetails buildNodeForType(DynamicSememeDataType dt, DynamicSememeDataBI defaultValue, DynamicSememeDataBI currentValue, 
			SimpleStringProperty valueIsRequired, SimpleStringProperty defaultValueTooltip, ReadOnlyObjectProperty<DynamicSememeDataType> polymorphicSelection, 
			UpdateableBooleanBinding allValid, DynamicSememeValidatorType[] validatorType, DynamicSememeDataBI[] validatorData)
	{
		
		//TODO get rid of this silly hack code when we update the GUI to support multiple validators
		ObjectProperty<DynamicSememeValidatorType> dsvt = ((validatorType == null || validatorType.length == 0) ? null 
				: new SimpleObjectProperty<>(validatorType[0])); 
			
		ObjectProperty<DynamicSememeDataBI> dsd = ((validatorData == null || validatorData.length == 0) ? null 
				: new SimpleObjectProperty<>(validatorData[0]));
		
		return buildNodeForType(dt, defaultValue, currentValue, valueIsRequired, defaultValueTooltip, polymorphicSelection, allValid, dsvt, dsd, true);
	}
	
	/**
	 * when valueIsRequired is null, it is understood to be optional.  If it is not null, need to tie it in to the listeners on the field - setting
	 * an appropriate message if the field is empty.
	 */
	private static SememeGUIDataTypeNodeDetails buildNodeForType(DynamicSememeDataType dt, DynamicSememeDataBI defaultValue, DynamicSememeDataBI currentValue, 
			SimpleStringProperty valueIsRequired, SimpleStringProperty defaultValueAndValidatorTooltip, ReadOnlyObjectProperty<DynamicSememeDataType> polymorphicSelection, 
			UpdateableBooleanBinding allValid, ObjectProperty<DynamicSememeValidatorType> validatorType, ObjectProperty<DynamicSememeDataBI> validatorData,
			boolean isFirstLevel)
	{
		if (validatorType != null && validatorData == null)
		{
			throw new RuntimeException("If a validator type is supplied, you need a validator data reference");
		}
		
		SememeGUIDataTypeNodeDetails returnValue = new SememeGUIDataTypeNodeDetails();;
		if (DynamicSememeDataType.BOOLEAN == dt)
		{
			ChoiceBox<String> cb = new ChoiceBox<>();
			cb.getItems().add("No Value");
			cb.getItems().add("True");
			cb.getItems().add("False");
			
			if (valueIsRequired != null)
			{
				cb.getSelectionModel().selectedIndexProperty().addListener((change) ->
				{
					if (cb.getSelectionModel().getSelectedIndex() == 0)
					{
						valueIsRequired.set("You must select True or False");
					}
					else
					{
						valueIsRequired.set("");
					}
				});
			}
			
			if (currentValue == null)
			{
				if (defaultValue != null)
				{
					if (((DynamicSememeBooleanBI)defaultValue).getDataBoolean())
					{
						cb.getSelectionModel().select(1);
					}
					else
					{
						cb.getSelectionModel().select(2);
					}
				}
				else
				{
					cb.getSelectionModel().select(0);
				}
			}
			else
			{
				if (((DynamicSememeBooleanBI)currentValue).getDataBoolean())
				{
					cb.getSelectionModel().select(1);
				}
				{
					cb.getSelectionModel().select(2);
				}
			}
			if (defaultValue != null && defaultValueAndValidatorTooltip != null)
			{
				defaultValueAndValidatorTooltip.set("The default value for this field is '" + defaultValue.getDataObject().toString() + "'");
			}
			if (validatorType != null && validatorType.get() != null && validatorType.get() != DynamicSememeValidatorType.UNKNOWN)
			{
				throw new RuntimeException("It doesn't makse sense to assign a validator to a boolean");
			}
			returnValue.dataField = cb;
			returnValue.nodeForDisplay = cb;
		}
		else if (DynamicSememeDataType.BYTEARRAY == dt)
		{
			HBox hbox = new HBox();
			hbox.setMaxWidth(Double.MAX_VALUE);
			Label choosenFile = new Label("- no data attached -");
			if (valueIsRequired != null)
			{
				valueIsRequired.set("You must select a file to attach");
			}
			choosenFile.setAlignment(Pos.CENTER_LEFT);
			choosenFile.setMaxWidth(Double.MAX_VALUE);
			choosenFile.setMaxHeight(Double.MAX_VALUE);
			Tooltip tt = new Tooltip("Select a file to attach to the sememe");
			Tooltip.install(choosenFile, tt);
			Button fileChooser = new Button("Choose File...");
			final ByteArrayDataHolder dataHolder = new ByteArrayDataHolder();
			
			if (currentValue != null)
			{
				dataHolder.data = ((DynamicSememeByteArray)currentValue).getData();
				choosenFile.setText("Currently has " + dataHolder.data.length + " bytes attached");
				if (valueIsRequired != null)
				{
					valueIsRequired.set("");
				}
			}
			else if (defaultValue != null)
			{
				dataHolder.data = ((DynamicSememeByteArray)defaultValue).getData();
				choosenFile.setText("Will attach the default value of " + dataHolder.data.length + " bytes");
				if (valueIsRequired != null)
				{
					valueIsRequired.set("");
				}
			}
			
			fileChooser.setOnAction((event) -> 
			{
				FileChooser fc = new FileChooser();
				fc.setTitle("Select a file to attach to the sememe");
				File selectedFile = fc.showOpenDialog(fileChooser.getScene().getWindow());
				if (selectedFile != null && selectedFile.isFile())
				{
					
					try
					{
						dataHolder.data = Files.readAllBytes(selectedFile.toPath());
						Platform.runLater(() -> 
						{
							choosenFile.setText(selectedFile.getName());
							tt.setText(selectedFile.getAbsolutePath());
							if (valueIsRequired != null)
							{
								valueIsRequired.set("");
							}
						});
					}
					catch (Exception e)
					{
						AppContext.getCommonDialogs().showErrorDialog("Error reading the selected file", e);
					}
				}
				else
				{
					Platform.runLater(() -> 
					{
						if (defaultValue != null)
						{
							dataHolder.data = ((DynamicSememeByteArray)defaultValue).getData();
							choosenFile.setText("Will attach the default value of " + dataHolder.data.length + " bytes");
							if (valueIsRequired != null)
							{
								valueIsRequired.set("");
							}
						}
						else
						{
							choosenFile.setText("- no data attached -");
							tt.setText("Select a file to attach that file to the sememe");
							if (valueIsRequired != null)
							{
								valueIsRequired.set("You must select a file to attach");
							}
						}
					});
				}
			});
			
			returnValue.dataField = dataHolder;
			
			hbox.getChildren().add(choosenFile);
			hbox.getChildren().add(fileChooser);
			HBox.setHgrow(choosenFile, Priority.ALWAYS);
			HBox.setHgrow(fileChooser, Priority.NEVER);
			returnValue.nodeForDisplay = hbox;
			if (defaultValue != null && defaultValueAndValidatorTooltip != null)
			{
				defaultValueAndValidatorTooltip.set("If no file is selected, the default value of " + ((DynamicSememeByteArray)defaultValue).getData().length +  " bytes will be used");
			}
			if (validatorType != null && validatorType.get() != null && validatorType.get() != DynamicSememeValidatorType.UNKNOWN)
			{
				throw new RuntimeException("There are currently no supported cases for a validator on a byte array");
			}
		}
		else if (DynamicSememeDataType.DOUBLE == dt || DynamicSememeDataType.FLOAT == dt || DynamicSememeDataType.INTEGER == dt || DynamicSememeDataType.LONG == dt
				|| DynamicSememeDataType.STRING == dt || DynamicSememeDataType.UUID == dt)
		{
			TextField tf = new TextField();
			returnValue.dataField = tf;

			if (defaultValue != null)
			{
				tf.setPromptText(defaultValue.getDataObject().toString());
			}
			SimpleStringProperty valueInvalidReason = new SimpleStringProperty("");
			returnValue.boundToAllValid.add(valueInvalidReason);
			allValid.addBinding(valueInvalidReason);
			Node n = ErrorMarkerUtils.setupErrorMarker(tf, valueInvalidReason);

			tf.textProperty().addListener(new ChangeListener<String>()
			{
				@Override
				public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue)
				{
					if (newValue.length() == 0)
					{
						valueInvalidReason.setValue("");
						if (valueIsRequired != null && defaultValue == null)
						{
							valueIsRequired.set("You must specify a value for this field");
						}
					}
					else if (DynamicSememeDataType.DOUBLE == dt)
					{
						try
						{
							if (valueIsRequired != null)
							{
								valueIsRequired.set("");
							}
							DynamicSememeDouble data = new DynamicSememeDouble(Double.parseDouble(tf.getText()));
							if (validatorType != null && validatorType.get() != null && validatorType.get() != DynamicSememeValidatorType.UNKNOWN)
							{
								valueInvalidReason.set(validatorType.get().passesValidatorStringReturn(data, validatorData.get(), null, null));
							}
							else
							{
								valueInvalidReason.set("");
							}
						}
						catch (Exception e)
						{
							valueInvalidReason.set("The value (if present) must be a double");
						}

					}
					else if (DynamicSememeDataType.FLOAT == dt)
					{
						try
						{
							if (valueIsRequired != null)
							{
								valueIsRequired.set("");
							}
							DynamicSememeFloat data = new DynamicSememeFloat(Float.parseFloat(tf.getText()));
							if (validatorType != null && validatorType.get() != null && validatorType.get() != DynamicSememeValidatorType.UNKNOWN)
							{
								valueInvalidReason.set(validatorType.get().passesValidatorStringReturn(data, validatorData.get(), null, null));
							}
							else
							{
								valueInvalidReason.set("");
							}
						}
						catch (Exception e)
						{
							valueInvalidReason.set("The value (if present) must be a float");
						}
					}
					else if (DynamicSememeDataType.INTEGER == dt)
					{
						try
						{
							if (valueIsRequired != null)
							{
								valueIsRequired.set("");
							}
							DynamicSememeInteger data = new DynamicSememeInteger(Integer.parseInt(tf.getText()));
							if (validatorType != null && validatorType.get() != null && validatorType.get() != DynamicSememeValidatorType.UNKNOWN)
							{
								valueInvalidReason.set(validatorType.get().passesValidatorStringReturn(data, validatorData.get(), null, null));
							}
							else
							{
								valueInvalidReason.set("");
							}
						}
						catch (Exception e)
						{
							valueInvalidReason.set("The value (if present) must be an integer");
						}
					}
					else if (DynamicSememeDataType.LONG == dt)
					{
						try
						{
							if (valueIsRequired != null)
							{
								valueIsRequired.set("");
							}
							DynamicSememeLong data = new DynamicSememeLong(Long.parseLong(tf.getText()));
							if (validatorType != null && validatorType.get() != null && validatorType.get() != DynamicSememeValidatorType.UNKNOWN)
							{
								valueInvalidReason.set(validatorType.get().passesValidatorStringReturn(data, validatorData.get(), null, null));
							}
							else
							{
								valueInvalidReason.set("");
							}
						}
						catch (Exception e)
						{
							valueInvalidReason.set("The value (if present) must be a long");
						}
					}
					else if (DynamicSememeDataType.STRING == dt)
					{
						if (valueIsRequired != null)
						{
							valueIsRequired.set("");
						}
						if (validatorType != null && validatorType.get() != null && validatorType.get() != DynamicSememeValidatorType.UNKNOWN)
						{
							valueInvalidReason.set(validatorType.get().passesValidatorStringReturn(new DynamicSememeString(tf.getText()), validatorData.get(), 
									null, null));
						}
						else
						{
							valueInvalidReason.set("");
						}
					}
					else if (DynamicSememeDataType.UUID == dt)
					{
						if (valueIsRequired != null)
						{
							valueIsRequired.set("");
						}
						if (Utility.isUUID(tf.getText()))
						{
							if (validatorType != null && validatorType.get() != null && validatorType.get() != DynamicSememeValidatorType.UNKNOWN)
							{
								valueInvalidReason.set(validatorType.get().passesValidatorStringReturn(new DynamicSememeUUID(UUID.fromString(tf.getText())), 
									validatorData.get(), null ,null));
							}
							else
							{
								valueInvalidReason.set("");
							}
						}
						else
						{
							valueInvalidReason.set("The value (if present) must be a properly formatted UUID");
						}
					}
				}
			});
			
			if (validatorType != null)
			{
				validatorType.addListener((change) -> 
				{
					//hack way to re-run the validator logic above...
					tf.setText(tf.getText() + " ");
					tf.setText(tf.getText().substring(0, tf.getText().length() - 1));
				});
				
				validatorData.addListener((change) ->
				{
					//hack way to re-run the validator logic above...
					tf.setText(tf.getText() + " ");
					tf.setText(tf.getText().substring(0, tf.getText().length() - 1));
				});
			}
			
			if (currentValue != null)
			{
				tf.setText(currentValue.getDataObject().toString());
			}
			if (currentValue == null && valueIsRequired != null && defaultValue == null)
			{
				valueIsRequired.set("You must specify a value for this field");
			}
			returnValue.nodeForDisplay = n;
			setupInfoTooltip(defaultValue, defaultValueAndValidatorTooltip, validatorType, validatorData);
		}
		else if (DynamicSememeDataType.NID == dt)
		{
			ConceptNode cn = new ConceptNode(null, false);
			returnValue.dataField = cn;
			returnValue.boundToAllValid.add(cn.isValid().getReasonWhyInvalid());
			allValid.addBinding(cn.isValid().getReasonWhyInvalid());
			
			if (currentValue != null)
			{
				//TODO (artf231429) this doesn't work, if the nid isn't a concept nid.  We need a NidNode, rather than a ConceptNode
				cn.set(OchreUtility.getConceptSnapshot(((DynamicSememeNidBI)currentValue).getDataNid(), null, null).get());
			}
			
			if (valueIsRequired != null && defaultValue == null)
			{
				if (currentValue == null)
				{
					valueIsRequired.set("You must specify a value for this field");
				}
			}
			
			cn.getConceptProperty().addListener((change) ->
			{
				if (cn.getConceptProperty().getValue() == null && valueIsRequired != null && defaultValue == null)
				{
					valueIsRequired.set("You must specify a value for this field");
				}
				else
				{
					if (cn.isValid().get() && valueIsRequired != null)
					{
						valueIsRequired.setValue("");
					}
					if (validatorType != null && validatorType.get() != null && cn.isValid().get() && cn.getConceptProperty().get() != null &&
							validatorType.get() != DynamicSememeValidatorType.UNKNOWN)
					{
						String isInvalid = validatorType.get().passesValidatorStringReturn(new DynamicSememeNid(cn.getConceptProperty().get().getNid()), 
								validatorData.get(), 
								ExtendedAppContext.getUserProfileBindings().getStampCoordinate().get(), 
								ExtendedAppContext.getUserProfileBindings().getTaxonomyCoordinate().get());
						if (isInvalid.length() > 0)
						{
							cn.isValid().setInvalid(isInvalid);
						}
					}
				}
			});
			
			
			if (validatorType != null)
			{
				validatorType.addListener((change) -> 
				{
					//hack way to re-run the validator logic above...
					cn.revalidate();
				});
				
				validatorData.addListener((change) ->
				{
					//hack way to re-run the validator logic above...
					cn.revalidate();
				});
			}

			returnValue.nodeForDisplay = cn.getNode();
			setupInfoTooltip(defaultValue, defaultValueAndValidatorTooltip, validatorType, validatorData);
		}
		else if (DynamicSememeDataType.POLYMORPHIC == dt)
		{
			//a slick little bit of recursion... but a bit tricky to keep the validators aligned properly...
			HBox hBox = new HBox();
			hBox.setMaxWidth(Double.MAX_VALUE);
			NestedPolymorphicData nestedData = new NestedPolymorphicData();
			
			nestedData.nestedNode = buildNodeForType(polymorphicSelection.get(), null, currentValue, valueIsRequired, defaultValueAndValidatorTooltip, null, allValid, 
					validatorType, validatorData, false);
			
			hBox.getChildren().add(nestedData.nestedNode.nodeForDisplay);
			HBox.setHgrow(hBox.getChildren().get(0), Priority.ALWAYS);
			
			nestedData.dataType = polymorphicSelection.get();
			returnValue.dataField = nestedData;
			if (nestedData.nestedNode.boundToAllValid.size() == 1 )
			{
				returnValue.boundToAllValid.add(nestedData.nestedNode.boundToAllValid.get(0));
				allValid.invalidate(); //the new binding will already be made to allValid_ during the recursion - but it may have computed without ths full list...
			}
			
			polymorphicSelection.addListener((change) ->
			{
				hBox.getChildren().remove(0);
				if (nestedData.nestedNode.boundToAllValid.size() == 1)
				{
					returnValue.boundToAllValid.remove(nestedData.nestedNode.boundToAllValid.get(0));
					allValid.removeBinding(nestedData.nestedNode.boundToAllValid.get(0));
				}
				nestedData.nestedNode = buildNodeForType(polymorphicSelection.get(), null, currentValue, valueIsRequired, defaultValueAndValidatorTooltip, null, allValid, 
						validatorType, validatorData, false);
				hBox.getChildren().add(nestedData.nestedNode.nodeForDisplay);
				HBox.setHgrow(hBox.getChildren().get(0), Priority.ALWAYS);
				nestedData.dataType = polymorphicSelection.get();
				if (nestedData.nestedNode.boundToAllValid.size() == 1)
				{
					returnValue.boundToAllValid.add(nestedData.nestedNode.boundToAllValid.get(0));
					allValid.invalidate(); //the new binding will already be made to allValid_ during the recursion - but it may have computed without ths full list...
				}
			});
			returnValue.nodeForDisplay = hBox;
		}
		else
		{
			throw new RuntimeException("Unexpected datatype " + dt);
		}
		if (isFirstLevel && valueIsRequired != null)
		{
			returnValue.boundToAllValid.add(valueIsRequired);
			allValid.addBinding(valueIsRequired);
		}
		return returnValue;
	}
	
	public static DynamicSememeDataBI getDataForType(Object data, DynamicSememeColumnInfo ci) throws PropertyVetoException
	{
		//TODO the way this is currently set up - if there is a default value, and the value is not required - it is impossible to leave the row blank.
		//not sure if that is a necessary use case, or not.
		if (DynamicSememeDataType.BOOLEAN == ci.getColumnDataType())
		{
			@SuppressWarnings("unchecked")
			ChoiceBox<String> cb = (ChoiceBox<String>) data;

			Boolean value = null;
			if (cb.getSelectionModel().getSelectedItem().equals("True"))
			{
				value = true;
			}
			else if (cb.getSelectionModel().getSelectedItem().equals("False"))
			{
				value = false;
			}
			else if (ci.getDefaultColumnValue() != null)
			{
				value =  ((DynamicSememeBooleanBI) ci.getDefaultColumnValue()).getDataBoolean();
			}
			return (value == null ? null : new DynamicSememeBoolean(value));
		}
		else if (DynamicSememeDataType.BYTEARRAY == ci.getColumnDataType())
		{
			if (data == null && ci.getDefaultColumnValue() == null)
			{
				return null;
			}
			ByteArrayDataHolder holder = (ByteArrayDataHolder) data;
			if (holder == null || holder.data == null)
			{
				return (DynamicSememeByteArray)ci.getDefaultColumnValue();
			}
			return new DynamicSememeByteArray(holder.data);
		}
		else if (DynamicSememeDataType.DOUBLE == ci.getColumnDataType() || DynamicSememeDataType.FLOAT == ci.getColumnDataType()
				|| DynamicSememeDataType.INTEGER == ci.getColumnDataType() || DynamicSememeDataType.LONG == ci.getColumnDataType()
				|| DynamicSememeDataType.STRING == ci.getColumnDataType() || DynamicSememeDataType.UUID == ci.getColumnDataType())
		{
			TextField tf = (TextField) data;
			String text = tf.getText();
			if (text.length() == 0 && ci.getDefaultColumnValue() == null)
			{
				return null;
			}
			if (DynamicSememeDataType.DOUBLE == ci.getColumnDataType())
			{
				return (text.length() > 0 ? new DynamicSememeDouble(Double.parseDouble(text)) : (DynamicSememeDoubleBI)ci.getDefaultColumnValue());
			}
			else if (DynamicSememeDataType.FLOAT == ci.getColumnDataType())
			{
				return (text.length() > 0 ? new DynamicSememeFloat(Float.parseFloat(text)) : (DynamicSememeFloatBI)ci.getDefaultColumnValue());
			}
			else if (DynamicSememeDataType.INTEGER == ci.getColumnDataType())
			{
				return (text.length() > 0 ? new DynamicSememeInteger(Integer.parseInt(text)) : (DynamicSememeIntegerBI)ci.getDefaultColumnValue());
			}
			else if (DynamicSememeDataType.LONG == ci.getColumnDataType())
			{
				return (text.length() > 0 ? new DynamicSememeLong(Long.parseLong(text)) : (DynamicSememeLongBI)ci.getDefaultColumnValue());
			}
			else if (DynamicSememeDataType.STRING == ci.getColumnDataType())
			{
				return (text.length() > 0 ? new DynamicSememeString(text) : (DynamicSememeStringBI)ci.getDefaultColumnValue());
			}
			else if (DynamicSememeDataType.UUID == ci.getColumnDataType())
			{
				return (text.length() > 0 ? new DynamicSememeUUID(UUID.fromString(text)) : (DynamicSememeUUIDBI)ci.getDefaultColumnValue());
			}
			else
			{
				throw new RuntimeException("oops");
			}
		}
		else if (DynamicSememeDataType.NID == ci.getColumnDataType())
		{
			ConceptNode cn = (ConceptNode)data;
			if (cn.getConcept() == null)
			{
				return (DynamicSememeNid)ci.getDefaultColumnValue();
			}
			return new DynamicSememeNid(cn.getConcept().getNid());
		}
		else if (DynamicSememeDataType.POLYMORPHIC == ci.getColumnDataType())
		{
			NestedPolymorphicData nestedData = (NestedPolymorphicData)data;
			// only need the data type field... (default value isn't allowed for polymorphic)
			DynamicSememeColumnInfo nestedCI = new DynamicSememeColumnInfo();
			nestedCI.setColumnDataType(nestedData.dataType);
			return getDataForType(nestedData.nestedNode.dataField, nestedCI);
		}
		else
		{
			throw new RuntimeException("Unexpected datatype " + ci.getColumnDataType());
		}
	}
	
	private static void setupInfoTooltip(DynamicSememeDataBI defaultValue, SimpleStringProperty defaultValueAndValidatorTooltip, 
			ObjectProperty<DynamicSememeValidatorType> validatorType, ObjectProperty<DynamicSememeDataBI> validatorData)
	{
		if ((defaultValue != null || (validatorType != null && validatorType.get() != null)) && defaultValueAndValidatorTooltip != null)
		{
			String tip = "";
			if (defaultValue != null)
			{
				
				Optional<String> temp = null;
				if (defaultValue.getDynamicSememeDataType() == DynamicSememeDataType.NID)
				{
					temp = OchreUtility.getDescription(((DynamicSememeNid) defaultValue).getDataNid());
				}
				else if (defaultValue.getDynamicSememeDataType() == DynamicSememeDataType.UUID)
				{
					temp = OchreUtility.getDescription(((DynamicSememeUUID) defaultValue).getDataUUID());
				}
				if (!temp.isPresent())
				{
					temp = Optional.of(defaultValue.getDataObject().toString());
				}
				
				tip = "If no value is specified the default value of '" + temp.get() + "' will be used";
			}
			if (validatorType != null && validatorType.get() != null)
			{
				if (tip.length() > 0)
				{
					tip = tip + "\n";
				}
				tip += "The validator type for this field is '" + 
						(validatorType.get() == DynamicSememeValidatorType.EXTERNAL ? "Drools" : validatorType.get().getDisplayName()) + "'";
				if (validatorData != null && validatorData.get() != null)
				{
					String temp = null;
					if (validatorData.get().getDynamicSememeDataType() == DynamicSememeDataType.NID)
					{
						temp = OchreUtility.getDescription(((DynamicSememeNid) validatorData.get()).getDataNid()).orElse(null);
					}
					else if (validatorData.get().getDynamicSememeDataType() == DynamicSememeDataType.UUID)
					{
						temp = OchreUtility.getDescription(((DynamicSememeUUID) validatorData.get()).getDataUUID()).orElse(null);
					}
					if (temp == null)
					{
						if (validatorType.get() == DynamicSememeValidatorType.EXTERNAL)
						{
							SememeDroolsValidatorImplInfo rdvii = SememeDroolsValidator.readFromData(validatorData.get());
							if (rdvii != null)
							{
								temp = "'" + rdvii.getDisplayName() + "'";
								try
								{
									for (String s : AppContext.getService(DroolsExecutorsManager.class).getDroolsExecutor(rdvii.getDroolsPackageName()).getAllRuleNames())
									{
										temp += "\n  " + s;
									}
								}
								catch (Exception e)
								{
									logger.error("Error reading drools rule definitions", e);
								}
							}
							else
							{
								//should be impossible
								logger.error("Failed to parse the Drools info from " + validatorData.get());
								temp = "!ERROR!";
							}
						}
						else
						{
							temp = "'" + validatorData.get().getDataObject().toString() + "'";
						}
					}
					
					tip += "\nThe validation data for this field is " + temp;
				}
			}
			defaultValueAndValidatorTooltip.set(tip);
		}
	}
}
