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
import gov.va.isaac.gui.ConceptNode;
import gov.va.isaac.gui.util.ErrorMarkerUtils;
import gov.va.isaac.util.UpdateableBooleanBinding;
import gov.va.isaac.util.Utility;
import gov.va.isaac.util.WBUtility;
import java.beans.PropertyVetoException;
import java.io.File;
import java.nio.file.Files;
import java.util.UUID;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectProperty;
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
import org.ihtsdo.otf.tcc.api.refexDynamic.data.RefexDynamicColumnInfo;
import org.ihtsdo.otf.tcc.api.refexDynamic.data.RefexDynamicDataBI;
import org.ihtsdo.otf.tcc.api.refexDynamic.data.RefexDynamicDataType;
import org.ihtsdo.otf.tcc.api.refexDynamic.data.dataTypes.RefexDynamicBooleanBI;
import org.ihtsdo.otf.tcc.api.refexDynamic.data.dataTypes.RefexDynamicDoubleBI;
import org.ihtsdo.otf.tcc.api.refexDynamic.data.dataTypes.RefexDynamicFloatBI;
import org.ihtsdo.otf.tcc.api.refexDynamic.data.dataTypes.RefexDynamicIntegerBI;
import org.ihtsdo.otf.tcc.api.refexDynamic.data.dataTypes.RefexDynamicLongBI;
import org.ihtsdo.otf.tcc.api.refexDynamic.data.dataTypes.RefexDynamicNidBI;
import org.ihtsdo.otf.tcc.api.refexDynamic.data.dataTypes.RefexDynamicStringBI;
import org.ihtsdo.otf.tcc.api.refexDynamic.data.dataTypes.RefexDynamicUUIDBI;
import org.ihtsdo.otf.tcc.model.cc.refexDynamic.data.dataTypes.RefexDynamicBoolean;
import org.ihtsdo.otf.tcc.model.cc.refexDynamic.data.dataTypes.RefexDynamicByteArray;
import org.ihtsdo.otf.tcc.model.cc.refexDynamic.data.dataTypes.RefexDynamicDouble;
import org.ihtsdo.otf.tcc.model.cc.refexDynamic.data.dataTypes.RefexDynamicFloat;
import org.ihtsdo.otf.tcc.model.cc.refexDynamic.data.dataTypes.RefexDynamicInteger;
import org.ihtsdo.otf.tcc.model.cc.refexDynamic.data.dataTypes.RefexDynamicLong;
import org.ihtsdo.otf.tcc.model.cc.refexDynamic.data.dataTypes.RefexDynamicNid;
import org.ihtsdo.otf.tcc.model.cc.refexDynamic.data.dataTypes.RefexDynamicString;
import org.ihtsdo.otf.tcc.model.cc.refexDynamic.data.dataTypes.RefexDynamicUUID;

/**
 * {@link RefexDataTypeFXNodeBuilder}
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a> 
 */
public class RefexDataTypeFXNodeBuilder
{
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
	 */
	public static NodeDetails buildNodeForType(RefexDynamicDataType dt, RefexDynamicDataBI defaultValue, RefexDynamicDataBI currentValue, 
			SimpleStringProperty valueIsRequired, SimpleStringProperty defaultValueTooltip, ReadOnlyObjectProperty<RefexDynamicDataType> polymorphicSelection, 
			UpdateableBooleanBinding allValid)
	{
		return buildNodeForType(dt, defaultValue, currentValue, valueIsRequired, defaultValueTooltip, polymorphicSelection, allValid, true);
	}
	
	/**
	 * when valueIsRequired is null, it is understood to be optional.  If it is not null, need to tie it in to the listeners on the field - setting
	 * an appropriate message if the field is empty.
	 */
	private static NodeDetails buildNodeForType(RefexDynamicDataType dt, RefexDynamicDataBI defaultValue, RefexDynamicDataBI currentValue, 
			SimpleStringProperty valueIsRequired, SimpleStringProperty defaultValueTooltip, ReadOnlyObjectProperty<RefexDynamicDataType> polymorphicSelection, 
			UpdateableBooleanBinding allValid, boolean isFirstLevel)
	{
		//TODO support validators....
		NodeDetails returnValue = new NodeDetails();;
		if (RefexDynamicDataType.BOOLEAN == dt)
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
					if (((RefexDynamicBooleanBI)defaultValue).getDataBoolean())
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
				if (((RefexDynamicBooleanBI)currentValue).getDataBoolean())
				{
					cb.getSelectionModel().select(1);
				}
				{
					cb.getSelectionModel().select(2);
				}
			}
			if (defaultValue != null && defaultValueTooltip != null)
			{
				defaultValueTooltip.set("The default value for this field is '" + defaultValue.getDataObject().toString() + "'");
			}
			returnValue.dataField = cb;
			returnValue.nodeForDisplay = cb;
		}
		else if (RefexDynamicDataType.BYTEARRAY == dt)
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
			Tooltip tt = new Tooltip("Select a file to attach to the refex");
			Tooltip.install(choosenFile, tt);
			Button fileChooser = new Button("Choose File...");
			final ByteArrayDataHolder dataHolder = new ByteArrayDataHolder();
			
			if (currentValue != null)
			{
				dataHolder.data = ((RefexDynamicByteArray)currentValue).getData();
				choosenFile.setText("Currently has " + dataHolder.data.length + " bytes attached");
				if (valueIsRequired != null)
				{
					valueIsRequired.set("");
				}
			}
			else if (defaultValue != null)
			{
				dataHolder.data = ((RefexDynamicByteArray)defaultValue).getData();
				choosenFile.setText("Will attach the default value of " + dataHolder.data.length + " bytes");
				if (valueIsRequired != null)
				{
					valueIsRequired.set("");
				}
			}
			
			fileChooser.setOnAction((event) -> 
			{
				FileChooser fc = new FileChooser();
				fc.setTitle("Select a file to attach to the refex");
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
							dataHolder.data = ((RefexDynamicByteArray)defaultValue).getData();
							choosenFile.setText("Will attach the default value of " + dataHolder.data.length + " bytes");
							if (valueIsRequired != null)
							{
								valueIsRequired.set("");
							}
						}
						else
						{
							choosenFile.setText("- no data attached -");
							tt.setText("Select a file to attach that file to the refex");
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
			if (defaultValue != null && defaultValueTooltip != null)
			{
				defaultValueTooltip.set("If no file is selected, the default value of " + ((RefexDynamicByteArray)defaultValue).getData().length +  " bytes will be used");
			}
		}
		else if (RefexDynamicDataType.DOUBLE == dt || RefexDynamicDataType.FLOAT == dt || RefexDynamicDataType.INTEGER == dt || RefexDynamicDataType.LONG == dt
				|| RefexDynamicDataType.STRING == dt || RefexDynamicDataType.UUID == dt)
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
					else if (RefexDynamicDataType.DOUBLE == dt)
					{
						try
						{
							if (valueIsRequired != null)
							{
								valueIsRequired.set("");
							}
							Double.parseDouble(tf.getText());
							valueInvalidReason.set("");
						}
						catch (Exception e)
						{
							valueInvalidReason.set("The value (if present) must be a double");
						}

					}
					else if (RefexDynamicDataType.FLOAT == dt)
					{
						try
						{
							if (valueIsRequired != null)
							{
								valueIsRequired.set("");
							}
							Float.parseFloat(tf.getText());
							valueInvalidReason.set("");
						}
						catch (Exception e)
						{
							valueInvalidReason.set("The value (if present) must be a float");
						}
					}
					else if (RefexDynamicDataType.INTEGER == dt)
					{
						try
						{
							if (valueIsRequired != null)
							{
								valueIsRequired.set("");
							}
							Integer.parseInt(tf.getText());
							valueInvalidReason.set("");
						}
						catch (Exception e)
						{
							valueInvalidReason.set("The value (if present) must be an integer");
						}
					}
					else if (RefexDynamicDataType.LONG == dt)
					{
						try
						{
							if (valueIsRequired != null)
							{
								valueIsRequired.set("");
							}
							Long.parseLong(tf.getText());
							valueInvalidReason.set("");
						}
						catch (Exception e)
						{
							valueInvalidReason.set("The value (if present) must be a long");
						}
					}
					else if (RefexDynamicDataType.STRING == dt)
					{
						if (valueIsRequired != null)
						{
							valueIsRequired.set("");
						}
						valueInvalidReason.set("");
					}
					else if (RefexDynamicDataType.UUID == dt)
					{
						if (valueIsRequired != null)
						{
							valueIsRequired.set("");
						}
						if (Utility.isUUID(tf.getText()))
						{
							valueInvalidReason.set("");
						}
						else
						{
							valueInvalidReason.set("The value (if present) must be a properly formatted UUID");
						}
					}
				}
			});
			
			if (currentValue != null)
			{
				tf.setText(currentValue.getDataObject().toString());
			}
			if (currentValue == null && valueIsRequired != null && defaultValue == null)
			{
				valueIsRequired.set("You must specify a value for this field");
			}
			returnValue.nodeForDisplay = n;
			if (defaultValue != null && defaultValueTooltip != null)
			{
				defaultValueTooltip.set("If no value is specified the default value of '" +defaultValue.getDataObject().toString()+  "' will be used");
			}
		}
		else if (RefexDynamicDataType.NID == dt)
		{
			ConceptNode cn = new ConceptNode(null, false);
			returnValue.dataField = cn;
			returnValue.boundToAllValid.add(cn.isValid().getReasonWhyInvalid());
			allValid.addBinding(cn.isValid().getReasonWhyInvalid());
			
			if (currentValue != null)
			{
				//TODO this doesn't work, if the nid isn't a concept nid.  We need a NidNode, rather than a ConceptNode
				cn.set(WBUtility.getConceptVersion(((RefexDynamicNidBI)currentValue).getDataNid()));
			}
			
			if (valueIsRequired != null && defaultValue == null)
			{
				if (currentValue == null)
				{
					valueIsRequired.set("You must specify a value for this field");
				}
				cn.getConceptProperty().addListener((change) ->
				{
					if (cn.getConceptProperty().getValue() == null)
					{
						valueIsRequired.set("You must specify a value for this field");
					}
					else
					{
						valueIsRequired.set("");
					}
				});
			}

			returnValue.nodeForDisplay = cn.getNode();
			if (defaultValue != null && defaultValueTooltip != null)
			{
				defaultValueTooltip.set("If no value is specified the default value of '" +defaultValue.getDataObject().toString()+  "' will be used");
			}
		}
		else if (RefexDynamicDataType.POLYMORPHIC == dt)
		{
			//a slick little bit of recursion... but a bit tricky to keep the validators aligned properly...
			HBox hBox = new HBox();
			hBox.setMaxWidth(Double.MAX_VALUE);
			NestedPolymorphicData nestedData = new NestedPolymorphicData();
			
			nestedData.nestedNode = buildNodeForType(polymorphicSelection.get(), null, currentValue, valueIsRequired, defaultValueTooltip, null, allValid, false);
			
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
				nestedData.nestedNode = buildNodeForType(polymorphicSelection.get(), null, currentValue, valueIsRequired, defaultValueTooltip, null, allValid, false);
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
	
	public static RefexDynamicDataBI getDataForType(Object data, RefexDynamicColumnInfo ci) throws PropertyVetoException
	{
		if (RefexDynamicDataType.BOOLEAN == ci.getColumnDataType())
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
				value =  ((RefexDynamicBooleanBI) ci.getDefaultColumnValue()).getDataBoolean();
			}
			return (value == null ? null : new RefexDynamicBoolean(value));
		}
		else if (RefexDynamicDataType.BYTEARRAY == ci.getColumnDataType())
		{
			if (data == null && ci.getDefaultColumnValue() == null)
			{
				return null;
			}
			ByteArrayDataHolder holder = (ByteArrayDataHolder) data;
			if (holder == null || holder.data == null)
			{
				return (RefexDynamicByteArray)ci.getDefaultColumnValue();
			}
			return new RefexDynamicByteArray(holder.data);
		}
		else if (RefexDynamicDataType.DOUBLE == ci.getColumnDataType() || RefexDynamicDataType.FLOAT == ci.getColumnDataType()
				|| RefexDynamicDataType.INTEGER == ci.getColumnDataType() || RefexDynamicDataType.LONG == ci.getColumnDataType()
				|| RefexDynamicDataType.STRING == ci.getColumnDataType() || RefexDynamicDataType.UUID == ci.getColumnDataType())
		{
			TextField tf = (TextField) data;
			String text = tf.getText();
			if (text.length() == 0 && ci.getDefaultColumnValue() == null)
			{
				return null;
			}
			if (RefexDynamicDataType.DOUBLE == ci.getColumnDataType())
			{
				return (text.length() > 0 ? new RefexDynamicDouble(Double.parseDouble(text)) : (RefexDynamicDoubleBI)ci.getDefaultColumnValue());
			}
			else if (RefexDynamicDataType.FLOAT == ci.getColumnDataType())
			{
				return (text.length() > 0 ? new RefexDynamicFloat(Float.parseFloat(text)) : (RefexDynamicFloatBI)ci.getDefaultColumnValue());
			}
			else if (RefexDynamicDataType.INTEGER == ci.getColumnDataType())
			{
				return (text.length() > 0 ? new RefexDynamicInteger(Integer.parseInt(text)) : (RefexDynamicIntegerBI)ci.getDefaultColumnValue());
			}
			else if (RefexDynamicDataType.LONG == ci.getColumnDataType())
			{
				return (text.length() > 0 ? new RefexDynamicLong(Long.parseLong(text)) : (RefexDynamicLongBI)ci.getDefaultColumnValue());
			}
			else if (RefexDynamicDataType.STRING == ci.getColumnDataType())
			{
				return (text.length() > 0 ? new RefexDynamicString(text) : (RefexDynamicStringBI)ci.getDefaultColumnValue());
			}
			else if (RefexDynamicDataType.UUID == ci.getColumnDataType())
			{
				return (text.length() > 0 ? new RefexDynamicUUID(UUID.fromString(text)) : (RefexDynamicUUIDBI)ci.getDefaultColumnValue());
			}
			else
			{
				throw new RuntimeException("oops");
			}
		}
		else if (RefexDynamicDataType.NID == ci.getColumnDataType())
		{
			ConceptNode cn = (ConceptNode)data;
			if (cn.getConcept() == null)
			{
				return (RefexDynamicNid)ci.getDefaultColumnValue();
			}
			return new RefexDynamicNid(cn.getConcept().getNid());
		}
		else if (RefexDynamicDataType.POLYMORPHIC == ci.getColumnDataType())
		{
			NestedPolymorphicData nestedData = (NestedPolymorphicData)data;
			//HACK - only need the data type field... but this is the type we want.
			//override datatype, and default (default value isn't allowed for polymorphic)
			RefexDynamicColumnInfo nestedCI = new RefexDynamicColumnInfo(ci.getColumnOrder(), ci.getColumnDescriptionConcept(), nestedData.dataType, null, 
					ci.isColumnRequired(), ci.getValidator(), ci.getValidatorData());
			return getDataForType(nestedData.nestedNode.dataField, nestedCI);
		}
		else
		{
			throw new RuntimeException("Unexpected datatype " + ci.getColumnDataType());
		}
	}
}
