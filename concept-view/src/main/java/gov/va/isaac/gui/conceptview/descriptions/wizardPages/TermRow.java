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
package gov.va.isaac.gui.conceptview.descriptions.wizardPages;

import gov.va.isaac.gui.conceptview.data.ConceptDescription;
import gov.va.isaac.gui.util.ErrorMarkerUtils;
import gov.va.isaac.util.UpdateableBooleanBinding;
import gov.vha.isaac.metadata.source.IsaacMetadataAuxiliaryBinding;
import gov.vha.isaac.ochre.api.ConceptProxy;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
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
	Node synonymNode;
	Node typeNode;
        Node significanceNode;
	SimpleStringProperty textFieldInvalidReason_ = new SimpleStringProperty("The Term is required");
	SimpleStringProperty typeFieldInvalidReason_ = new SimpleStringProperty("A Type selection is required");
	SimpleStringProperty significanceFieldInvalidReason_ = new SimpleStringProperty("A Significance selection is required");
	SimpleStringProperty languageFieldInvalidReason_ = new SimpleStringProperty("A Significance selection is required");
	SimpleStringProperty termRowInvalidReason_ = new SimpleStringProperty("Must fill out all fields or none");

	TextField term;
	ChoiceBox<String> type;
	ChoiceBox<String> significance;
	ChoiceBox<String> language;
        
	private UpdateableBooleanBinding rowValid;
	
	public TermRow()
	{
		term = new TextField();
		term.textProperty().addListener(new ChangeListener<String>()
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
		synonymNode = ErrorMarkerUtils.setupErrorMarker(term, textFieldInvalidReason_);
		
		type = new ChoiceBox<>(FXCollections.observableArrayList("synonym (ISAAC)", "definition (ISAAC)"));
		type.valueProperty().addListener(new ChangeListener<String>()
		{
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue)
			{
				
				String type = newValue.trim();
				
				if (type.length() == 0)
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
		
		language = new ChoiceBox<>(FXCollections.observableArrayList("English (ISAAC)", "Spanish (ISAAC)", "French (ISAAC)", "Danish (ISAAC)", "Polish (ISAAC)", "Dutch (ISAAC)", "Lithuanian (ISAAC)", "Chinese (ISAAC)", "Japanese (ISAAC)", "Swedish (ISAAC)"));
		language.valueProperty().addListener(new ChangeListener<String>()
		{
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue)
			{
				
				String language = newValue.trim();
				
				if (language.length() == 0)
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

                significance = new ChoiceBox<>(FXCollections.observableArrayList("initial case is NOT significant (ISAAC)", "initial case IS significant (ISAAC)"));
		significance.valueProperty().addListener(new ChangeListener<String>()
		{
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue)
			{
				
				String sig = newValue.trim();
				
				if (sig.length() == 0)
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
//                                return true;
                        }
		};
	}
	
        public void populateRow(ConceptDescription desc) {
            String descTerm = desc.getValue();
            String typeTerm = desc.getType();
            String languageTerm = desc.getLanguage();
            String significanceString = desc.getSignificance();
            
            term.setText(descTerm);
            significance.setValue(significanceString);
            type.setValue(typeTerm);
            language.setValue(languageTerm);
        }
        
	public BooleanBinding isValid()
	{
		return rowValid;
	}
	
	public Node getTermNode()
	{
		return synonymNode;
	}
	
	public Node getTypeNode()
	{
		return typeNode;
	}
	
	public String getTypeString()
	{
		return type.getValue();
	}
	
	public ConceptProxy getType() //TODO -
	{
		try
		{
			if ("synonym (ISAAC)".equals(type.getSelectionModel().getSelectedItem()))
			{
				return IsaacMetadataAuxiliaryBinding.SYNONYM;
			}
			else if ("definition (ISAAC)".equals(type.getSelectionModel().getSelectedItem()))
			{
				return IsaacMetadataAuxiliaryBinding.DEFINITION_DESCRIPTION_TYPE;
			}
			else
			{
				throw new Exception("Incorrect Description Type");
			}
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}

	}
	
	public Node getLanguageNode()
	{
		return languageNode;
	}
	
	public String getLanguageString()
	{
		return language.getValue();
	}
	
	public ConceptProxy getLanguage() //TODO -
	{
		try
		{
			if ("English (ISAAC)".equals(language.getSelectionModel().getSelectedItem()))
			{
				return IsaacMetadataAuxiliaryBinding.ENGLISH;
			}
			else if ("Spanish (ISAAC)".equals(language.getSelectionModel().getSelectedItem()))
			{
				return IsaacMetadataAuxiliaryBinding.SPANISH;
			}
			else if ("French (ISAAC)".equals(language.getSelectionModel().getSelectedItem()))
			{
				return IsaacMetadataAuxiliaryBinding.FRENCH;
			}
			else if ("Danish (ISAAC)".equals(language.getSelectionModel().getSelectedItem()))
			{
				return IsaacMetadataAuxiliaryBinding.DANISH;
			}
			else if ("Polish (ISAAC)".equals(language.getSelectionModel().getSelectedItem()))
			{
				return IsaacMetadataAuxiliaryBinding.POLISH;
			}
			else if ("Dutch (ISAAC)".equals(language.getSelectionModel().getSelectedItem()))
			{
				return IsaacMetadataAuxiliaryBinding.DUTCH;
			}
			else if ("Lithuanian (ISAAC)".equals(language.getSelectionModel().getSelectedItem()))
			{
				return IsaacMetadataAuxiliaryBinding.LITHUANIAN;
			}
			else if ("Chinese (ISAAC)".equals(language.getSelectionModel().getSelectedItem()))
			{
				return IsaacMetadataAuxiliaryBinding.CHINESE;
			}
			else if ("Japanese (ISAAC)".equals(language.getSelectionModel().getSelectedItem()))
			{
				return IsaacMetadataAuxiliaryBinding.JAPANESE;
			}
			else if ("Swedish (ISAAC)".equals(language.getSelectionModel().getSelectedItem()))
			{
				return IsaacMetadataAuxiliaryBinding.SWEDISH;
			}
			else
			{
				throw new Exception("Incorrect Description Language");
			}
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}

	}
	
        public Node getSignificanceNode()
	{
 		return significanceNode;
	}
	
	public String getSignificanceString()
	{
		return significance.getValue();
	}
	
	public ConceptProxy getSignificance() //TODO -
	{
		try
		{
			if ("initial case is NOT significant (ISAAC)".equals(significance.getSelectionModel().getSelectedItem()))
			{
				return IsaacMetadataAuxiliaryBinding.INITIAL_CASE_IS_NOT_SIGNIFICANT;
			}
			else if ("initial case IS significant (ISAAC)".equals(significance.getSelectionModel().getSelectedItem()))
			{
				return IsaacMetadataAuxiliaryBinding.INITIAL_CASE_IS_SIGNIFICANT;
			}
			else
			{
				throw new Exception("Incorrect Description Significance");
			}
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}

        }
        

	public String getTerm()
	{
		return term.getText();
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
//	};
///*		langInvalidReason = new UpdateableStringBinding() 
//	{
//		@Override
//		protected String computeValue()
//		{
//			for (int i = 0; i < langVBox.getChildren().size(); i++)
//			{
//				// Check that not partially filled out
//				TextField tf = (TextField) langVBox.getChildren().get(i);
//				String lang = tf.getText().trim();
//				
//				String term = ((TextField)synonymVBox.getChildren().get(i)).getText().trim();
//				
//				if (!lang.isEmpty() && term.trim().isEmpty()) {
//					return "Cannot fill out Term and not Language";
//				} else if (!StringUtils.isAlpha(lang)) {
//					return "Language must be filled with only alphabetically letters";
//				} else if (lang.length() != 2) {
//					return "Language must be filled out with a 2-character string";
//				}
//			}
//
//			return "";
//		}
//	};

}
