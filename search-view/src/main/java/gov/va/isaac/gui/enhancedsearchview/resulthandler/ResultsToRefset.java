package gov.va.isaac.gui.enhancedsearchview.resulthandler;

import java.beans.PropertyVetoException;
import java.io.IOException;
import org.ihtsdo.otf.tcc.api.blueprint.InvalidCAB;
import org.ihtsdo.otf.tcc.api.contradiction.ContradictionException;
import gov.va.isaac.ExtendedAppContext;
import gov.va.isaac.gui.dialog.UserPrompt.UserPromptResponse;
import gov.va.isaac.search.CompositeSearchResult;
import gov.va.isaac.util.OTFUtility;
import gov.vha.isaac.metadata.coordinates.ViewCoordinates;
import gov.vha.isaac.ochre.api.component.sememe.version.dynamicSememe.DynamicSememeColumnInfo;
import gov.vha.isaac.ochre.impl.sememe.DynamicSememeUsageDescription;
import gov.vha.isaac.ochre.impl.sememe.DynamicSememeUtility;
import javafx.scene.control.TableView;
import javafx.stage.Stage;

public class ResultsToRefset {
	
	// Create Refset out of results in Search
	public static String resultsToRefset(Stage owner, TableView<CompositeSearchResult> tableView) throws IOException, ContradictionException, InvalidCAB, PropertyVetoException {
		// Prompt for name/Desc/iSAnnot/parent
		RefsetCreationPrompt prompt = new RefsetCreationPrompt();
		prompt.showUserPrompt(owner, "Define Refset");
		
		// Create RefsetDynamic
		if (prompt.getButtonSelected() == UserPromptResponse.APPROVE) {
//			DynamicSememeUsageDescription refset = 
//					DynamicSememeUtility.createNewDynamicSememeUsageDescriptionConcept(prompt.getNameTextField().getText(), 
//																									 prompt.getNameTextField().getText(), 
//																									 prompt.getDescTextField().getText(),
//																									 new DynamicSememeColumnInfo[] {},
//																									 prompt.getParentConcept().getConcept().getPrimordialUuid(),
//																									 prompt.getAnnot().isSelected(),
//																									 null,
//																									 ViewCoordinates.getMetadataViewCoordinate());
		    // Create a dynamic sememe CAB for each result
//			for (CompositeSearchResult con : tableView.getItems()) {
//				DynamicSememeCAB refexBlueprint = new DynamicSememeCAB(con.getContainingConcept().get().getNid(), refset.getRefexUsageDescriptorNid());
//				OTFUtility.getBuilder().construct(refexBlueprint);
//				
//				if (prompt.getAnnot().isSelected()) {
//					//TODO Dan broke this - it needs to be rewritten with builder anyway
//					//ExtendedAppContext.getDataStore().addUncommitted(con.getContainingConcept().get());
//				} 
//			}
//			
//			if (!prompt.getAnnot().isSelected()) {
//				ExtendedAppContext.getDataStore().addUncommitted(ExtendedAppContext.getDataStore().getConceptForNid(refset.getRefexUsageDescriptorNid()));
//			}
//			
//			ExtendedAppContext.getDataStore().commit();
			
			return prompt.getNameTextField().getText();
		}
		
		return null;
	}
}
