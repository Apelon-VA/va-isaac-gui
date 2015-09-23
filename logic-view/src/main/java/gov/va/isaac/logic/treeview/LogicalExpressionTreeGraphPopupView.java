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
package gov.va.isaac.logic.treeview;

import gov.va.isaac.AppContext;
import gov.va.isaac.gui.util.Images;
import gov.va.isaac.interfaces.gui.views.commonFunctionality.LogicalExpressionTreeGraphEmbeddableViewI;
import gov.va.isaac.interfaces.gui.views.commonFunctionality.LogicalExpressionTreeGraphPopupViewI;
import gov.vha.isaac.ochre.api.component.sememe.version.LogicGraphSememe;
import gov.vha.isaac.ochre.api.coordinate.TaxonomyCoordinate;
import gov.vha.isaac.ochre.api.observable.coordinate.ObservableTaxonomyCoordinate;
import java.io.IOException;
import java.util.UUID;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Refset View
 * 
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */

@Service
@PerLookup
public class LogicalExpressionTreeGraphPopupView implements LogicalExpressionTreeGraphPopupViewI
{
	private final Logger logger = LoggerFactory.getLogger(LogicalExpressionTreeGraphPopupView.class);
	private Stage stage;
	private LogicalExpressionTreeGraphView embeddableView;
	
	private LogicalExpressionTreeGraphPopupView() throws IOException
	{
		// created by HK2
		stage = new Stage();
		stage.initModality(Modality.NONE);
		stage.initStyle(StageStyle.DECORATED);
		embeddableView = (LogicalExpressionTreeGraphView) AppContext.getService(LogicalExpressionTreeGraphEmbeddableViewI.class);
		stage.setScene(new Scene(embeddableView.getView(), 800, 600));
//		stage.getScene().getStylesheets().add(LogicalExpressionTreeGraphPopupView.class.getResource("/info-model-view-style.css").toString());
//		stage.getScene().getStylesheets().add(LogicalExpressionTreeGraphPopupView.class.getResource("/isaac-shared-styles.css").toString());
		stage.setTitle("Logical Expression Tree Graph Popup View");
		stage.getIcons().add(Images.ROOT.getImage());
	}

	/**
	 * @see gov.va.isaac.interfaces.gui.views.PopupViewI#showView(javafx.stage.Window)
	 */
	@Override
	public void showView(Window parent)
	{
		if (embeddableView.getConceptId() == null)
		{
			throw new RuntimeException("Must call setConcept(...) first");
		}
		stage.initOwner(parent);
		stage.show();
		
	}

	@Override
	public Integer getConceptId() {
		return embeddableView.getConceptId();
	}
	
	@Override
	public void setConcept(int conceptId) {
		embeddableView.setConcept(conceptId);
	}

	@Override
	public void setConcept(
			TaxonomyCoordinate taxonomyCoordinate,
			int conceptId) {
		embeddableView.setConcept(taxonomyCoordinate, conceptId);
	}

	@Override
	public void setConcept(ObservableTaxonomyCoordinate taxonomyCoordinate,
			int conceptId) {
		embeddableView.setConcept(taxonomyCoordinate, conceptId);
	}

	@Override
	public void setConcept(UUID conceptUuid) {
		embeddableView.setConcept(conceptUuid);
	}

	@Override
	public void setConcept(
			TaxonomyCoordinate taxonomyCoordinate,
			UUID conceptUuid) {
		embeddableView.setConcept(taxonomyCoordinate, conceptUuid);
	}

	@Override
	public void setConcept(ObservableTaxonomyCoordinate taxonomyCoordinate,
			UUID conceptUuid) {
		embeddableView.setConcept(taxonomyCoordinate, conceptUuid);
	}
	
	@Override
	public void clear() {
		embeddableView.clear();
	}

	@Override
	public void setConcept(LogicGraphSememe<?> logicGraphSememe) {
		embeddableView.setConcept(logicGraphSememe);
	}

	@Override
	public LogicGraphSememe<?> getLogicGraphSememe() {
		return embeddableView.getLogicGraphSememe();
	}

	@Override
	public void setConcept(TaxonomyCoordinate taxonomyCoordinate,
			LogicGraphSememe<?> specifiedLogicGraphSememe) {
		embeddableView.setConcept(taxonomyCoordinate, specifiedLogicGraphSememe);
	}
	@Override
	public void setConcept(ObservableTaxonomyCoordinate taxonomyCoordinate,
			LogicGraphSememe<?> specifiedLogicGraphSememe) {
		embeddableView.setConcept(taxonomyCoordinate, specifiedLogicGraphSememe);
	}
}