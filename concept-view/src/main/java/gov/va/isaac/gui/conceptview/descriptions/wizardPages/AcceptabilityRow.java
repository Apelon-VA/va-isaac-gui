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
package gov.va.isaac.gui.conceptview.descriptions.wizardPages;

import gov.va.isaac.ExtendedAppContext;
import gov.va.isaac.gui.SimpleDisplayConcept;
import gov.va.isaac.gui.conceptViews.componentRows.RelRow;
import gov.va.isaac.gui.util.ErrorMarkerUtils;
import gov.va.isaac.util.OTFUtility;
import gov.va.isaac.util.UpdateableBooleanBinding;
import gov.vha.isaac.metadata.source.IsaacMetadataAuxiliaryBinding;
import gov.vha.isaac.ochre.api.ConceptProxy;
import gov.vha.isaac.ochre.api.Get;
import gov.vha.isaac.ochre.api.component.concept.ConceptChronology;
import gov.vha.isaac.ochre.api.component.concept.ConceptVersion;
import gov.vha.isaac.ochre.collections.ConceptSequenceSet;
import java.util.ArrayList;
import java.util.Observable;
import java.util.UUID;
import java.util.stream.IntStream;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.util.Callback;

/**
 * {@link RelRow}
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public class AcceptabilityRow
{
	Node dialectNode;
        
	SimpleStringProperty dialectFieldInvalidReason_ = new SimpleStringProperty("A Type selection is required");

	ChoiceBox<SimpleDisplayConcept> dialect;
        RadioButton preferred;
        RadioButton acceptable;
        RadioButton neither;

        final ToggleGroup acceptableRadioGroup = new ToggleGroup();

	private UpdateableBooleanBinding rowValid;
	
	public AcceptabilityRow()
	{

		dialect = new ChoiceBox();
                dialect.setItems(populateDialectFromLanguage());

                dialect.valueProperty().addListener(new ChangeListener<SimpleDisplayConcept>()
		{
			@Override
			public void changed(ObservableValue<? extends SimpleDisplayConcept> observable, SimpleDisplayConcept oldValue, SimpleDisplayConcept newValue)
			{
				
				
				if (newValue == null)
				{
					dialectFieldInvalidReason_.set("A Dialect selection is required");
				}
				else
				{
					dialectFieldInvalidReason_.set("");
				}
			}
		});
		
		dialectNode = ErrorMarkerUtils.setupErrorMarker(dialect, dialectFieldInvalidReason_);

		preferred = new RadioButton();
		acceptable = new RadioButton();
		neither = new RadioButton();
                
                preferred.setToggleGroup(acceptableRadioGroup);
                acceptable.setToggleGroup(acceptableRadioGroup);
                neither.setToggleGroup(acceptableRadioGroup);
                
                
                
		rowValid = new UpdateableBooleanBinding()
		{
			{	
				setComputeOnInvalidate(true);
				bind(dialectFieldInvalidReason_);
			}
			@Override
			protected boolean computeValue()
			{
				return (dialectFieldInvalidReason_.get().length() == 0 );
//                                return true;
                        }
		};
	}
	
        void populateRows(Integer dialectSeq, Integer acceptabilitySeq) {
            dialect.setValue(new SimpleDisplayConcept(dialectSeq));
            
            if (acceptabilitySeq == IsaacMetadataAuxiliaryBinding.ACCEPTABLE.getNid()) {
                acceptable.setSelected(true);
            } else if (acceptabilitySeq == IsaacMetadataAuxiliaryBinding.PREFERRED.getNid()) {
                preferred.setSelected(true);
            }
        }

        public void populateBlankRow() {
            neither.setSelected(true);
        }
	
        public void populateDefaultRow() {
            dialect.setValue(getPreferredDefaultDialect());
            neither.setSelected(true);
        }
        
	public BooleanBinding isValid()
	{
		return rowValid;
	}
	
	public Node getDialectNode()
	{
		return dialectNode;
	}
	
	public String getDialectString()
	{
		return dialect.getValue().getDescription();
	}
	
	public int getDialect() 
	{
            return Get.conceptService().getConcept(dialect.getSelectionModel().getSelectedItem().getNid()).getConceptSequence();
	}
	

        public Node getPreferredNode()
	{
 		return preferred;
	}
	
	public Node getAcceptableNode()
	{
		return acceptable;
	}

        	
	public Node getNeitherNode()
	{
		return neither;
	}
        
        public boolean isPreferred() {
            return preferred.isSelected();
        }
        
        public boolean isAcceptable() {
            return acceptable.isSelected();
        }
        
        public boolean isNeither() {
            return neither.isSelected();
        }

    private ObservableList<SimpleDisplayConcept> populateDialectFromLanguage() {
        ObservableList<SimpleDisplayConcept> languageConcepts = FXCollections.observableArrayList(new ArrayList<SimpleDisplayConcept>());

            try {
                ConceptSequenceSet children = Get.taxonomyService().getChildOfSequenceSet(IsaacMetadataAuxiliaryBinding.DIALECT_ASSEMBLAGE.getConceptSequence(), 
                                                                                          ExtendedAppContext.getUserProfileBindings().getTaxonomyCoordinate().get());

                for (int conSeq : children.asArray()) {
                    SimpleDisplayConcept sdc = new SimpleDisplayConcept(conSeq); 
                    languageConcepts.add(sdc);
                }
            } catch (Exception e) {
            }

            return languageConcepts;
    }

    private SimpleDisplayConcept getPreferredDefaultDialect() {
        return new SimpleDisplayConcept(IsaacMetadataAuxiliaryBinding.US_ENGLISH_DIALECT.getConceptSequence());
    }
}
