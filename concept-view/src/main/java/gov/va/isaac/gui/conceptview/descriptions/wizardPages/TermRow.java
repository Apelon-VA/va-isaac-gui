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
package gov.va.isaac.gui.conceptview.descriptions.wizardPages;

import gov.va.isaac.ExtendedAppContext;
import gov.va.isaac.gui.SimpleDisplayConcept;
import gov.va.isaac.gui.conceptview.data.ConceptDescription;
import gov.va.isaac.gui.util.ErrorMarkerUtils;
import gov.va.isaac.util.UpdateableBooleanBinding;
import gov.vha.isaac.metadata.source.IsaacMetadataAuxiliaryBinding;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.collections.ConceptSequenceSet;
import java.util.ArrayList;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;

/**
 * {@link TermRow}
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a> 
 */
public class TermRow
{
	Node languageNode;
	Node textNode;
	Node typeNode;
	Node significanceNode;
	SimpleStringProperty textFieldInvalidReason_ = new SimpleStringProperty("The Term is required");
	SimpleStringProperty typeFieldInvalidReason_ = new SimpleStringProperty("A Type selection is required");
	SimpleStringProperty significanceFieldInvalidReason_ = new SimpleStringProperty("A Significance selection is required");
	SimpleStringProperty languageFieldInvalidReason_ = new SimpleStringProperty("A Significance selection is required");
	SimpleStringProperty termRowInvalidReason_ = new SimpleStringProperty("Must fill out all fields or none");

	TextField text;
	ChoiceBox<SimpleDisplayConcept> type;
	ChoiceBox<SimpleDisplayConcept> significance;
	ChoiceBox<SimpleDisplayConcept> language;
	
	private UpdateableBooleanBinding rowValid;
	
	public TermRow()
	{
		text = new TextField();
		text.textProperty().addListener(new ChangeListener<String>()
		{
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue)
			{
				
				String term = newValue.trim();
				
				if (term.length() == 0)
				{
					textFieldInvalidReason_.set("The Term is required");
				}
				else
				{
					int frontParenCount = countChar(term, "(");
					int backParenCount = countChar(term, ")");
	
					if (frontParenCount != 0 || backParenCount != 0)
					{
						//really?  That seems like an completely silly restriction.
						textFieldInvalidReason_.set("Cannot have parenthesis in synonym or it may be confused with the FSN");
						return;
					}
					else
					{
						textFieldInvalidReason_.set("");
					}
				}
			}
		});
		textNode = ErrorMarkerUtils.setupErrorMarker(text, textFieldInvalidReason_);
		
		type = new ChoiceBox<>();
		type.setItems(populateDescriptionTypes());
		type.valueProperty().addListener(new ChangeListener<SimpleDisplayConcept>()
		{
			@Override
			public void changed(ObservableValue<? extends SimpleDisplayConcept> observable, SimpleDisplayConcept oldValue, SimpleDisplayConcept newValue)
			{
				
				if (newValue == null)
				{
					typeFieldInvalidReason_.set("A Type selection is required");
				}
				else
				{
					typeFieldInvalidReason_.set("");
				}
			}
		});
		
		typeNode = ErrorMarkerUtils.setupErrorMarker(type, typeFieldInvalidReason_);
		
		language = new ChoiceBox<>();
		language.setItems(populateLanguageTypes());
		language.valueProperty().addListener(new ChangeListener<SimpleDisplayConcept>()
		{
			@Override
			public void changed(ObservableValue<? extends SimpleDisplayConcept> observable, SimpleDisplayConcept oldValue, SimpleDisplayConcept newValue)
			{
				if (newValue == null)
				{
					languageFieldInvalidReason_.set("A Language selection is required");
				}
				else
				{
					languageFieldInvalidReason_.set("");
				}
			}
		});
		
		languageNode = ErrorMarkerUtils.setupErrorMarker(language, languageFieldInvalidReason_);

				
		significance = new ChoiceBox<>();
		significance.setItems(populateInitCapTypes());
		significance.valueProperty().addListener(new ChangeListener<SimpleDisplayConcept>()
		{
			@Override
			public void changed(ObservableValue<? extends SimpleDisplayConcept> observable, SimpleDisplayConcept oldValue, SimpleDisplayConcept newValue)
			{
				
				if (newValue == null)
				{
					significanceFieldInvalidReason_.set("A Significance selection is required");
				}
				else
				{
					significanceFieldInvalidReason_.set("");
				}
			}
		});
		
		significanceNode = ErrorMarkerUtils.setupErrorMarker(significance, significanceFieldInvalidReason_);
		
		
		rowValid = new UpdateableBooleanBinding()
		{
			{	
				setComputeOnInvalidate(true);
				bind(textFieldInvalidReason_, typeFieldInvalidReason_, languageFieldInvalidReason_, significanceFieldInvalidReason_);
			}
			@Override
			protected boolean computeValue()
			{
				return (textFieldInvalidReason_.get().length() == 0 && typeFieldInvalidReason_.get().length() == 0 && languageFieldInvalidReason_.get().length() == 0 && significanceFieldInvalidReason_.get().length() == 0);
			}
		};
	}
	
	public void populateRow(ConceptDescription desc) {
		String descTerm = desc.getValue();
		
		text.setText(descTerm);
		significance.setValue(new SimpleDisplayConcept(desc.getSignificanceSequence()));
		type.setValue(new SimpleDisplayConcept(desc.getTypeSequence()));
		language.setValue(new SimpleDisplayConcept(desc.getLanguageSequence()));
	}
	
	public BooleanBinding isValid()
	{
		return rowValid;
	}
	
	public Node getTextNode()
	{
		return textNode;
	}
	
	public Node getTypeNode()
	{
		return typeNode;
	}
	
	public String getTypeString()
	{
		return type.getValue().getDescription();
	}
	
	public int getType() //TODO -
	{
		return Get.conceptService().getConcept(type.getSelectionModel().getSelectedItem().getNid()).getConceptSequence();
	}
	
	public Node getLanguageNode()
	{
		return languageNode;
	}
	
	public String getLanguageString()
	{
		return language.getValue().getDescription();
	}
	
	public int getLanguage() //TODO -
	{
		return Get.conceptService().getConcept(language.getSelectionModel().getSelectedItem().getNid()).getConceptSequence();

	}
	
	public Node getSignificanceNode()
	{
 		return significanceNode;
	}
	
	public String getSignificanceString()
	{
		return significance.getValue().getDescription();
	}
	
	public int getSignificance() //TODO -
	{
		return Get.conceptService().getConcept(significance.getSelectionModel().getSelectedItem().getNid()).getConceptSequence();
	}
	

	public String Text()
	{
		return text.getText();
	}

	private int countChar(String str, String c)
	{
		int count = 0;
		int idx = 0;
		while ((idx = str.indexOf(c, idx)) != -1)
		{
			count++;
			idx += c.length();
		}
		return count;
	}

	private ObservableList<SimpleDisplayConcept> populateDescriptionTypes() {
		ObservableList<SimpleDisplayConcept> descriptionConcepts = FXCollections.observableArrayList(new ArrayList<SimpleDisplayConcept>());
		descriptionConcepts.add(new SimpleDisplayConcept(IsaacMetadataAuxiliaryBinding.SYNONYM.getConceptSequence()));
		descriptionConcepts.add(new SimpleDisplayConcept(IsaacMetadataAuxiliaryBinding.DEFINITION_DESCRIPTION_TYPE.getConceptSequence()));
	
		return descriptionConcepts;
	}

	private ObservableList<SimpleDisplayConcept> populateLanguageTypes() {
		ObservableList<SimpleDisplayConcept> languageConcepts = FXCollections.observableArrayList(new ArrayList<SimpleDisplayConcept>());

		try {
			ConceptSequenceSet children = Get.taxonomyService().getChildOfSequenceSet(IsaacMetadataAuxiliaryBinding.LANGUAGE.getConceptSequence(), 
												ExtendedAppContext.getUserProfileBindings().getTaxonomyCoordinate().get());
	
			for (int conSeq : children.asArray()) {
				SimpleDisplayConcept sdc = new SimpleDisplayConcept(conSeq); 
				languageConcepts.add(sdc);
			}
		} catch (Exception e) {
		}

		return languageConcepts;
	}

	private ObservableList<SimpleDisplayConcept> populateInitCapTypes() {
		ObservableList<SimpleDisplayConcept> initCapConcepts = FXCollections.observableArrayList(new ArrayList<SimpleDisplayConcept>());
		initCapConcepts.add(new SimpleDisplayConcept(IsaacMetadataAuxiliaryBinding.INITIAL_CASE_IS_NOT_SIGNIFICANT.getConceptSequence()));
		initCapConcepts.add(new SimpleDisplayConcept(IsaacMetadataAuxiliaryBinding.INITIAL_CASE_IS_SIGNIFICANT.getConceptSequence()));
	
		return initCapConcepts;
	}
}

