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
package gov.va.isaac.gui.dialog;

import gov.va.isaac.AppContext;
import gov.va.isaac.ExtendedAppContext;
import gov.va.isaac.gui.util.FxUtils;
import gov.va.isaac.gui.util.Images;
import gov.va.isaac.interfaces.gui.constants.ConceptViewMode;
import gov.va.isaac.interfaces.gui.constants.SharedServiceNames;
import gov.va.isaac.interfaces.gui.views.commonFunctionality.PopupConceptViewI;
import gov.va.isaac.util.OTFUtility;
import gov.va.isaac.util.Utility;
import java.io.IOException;
import java.net.URL;
import java.util.UUID;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javax.inject.Named;
import org.glassfish.hk2.api.PerLookup;
import org.ihtsdo.otf.tcc.ddo.concept.ConceptChronicleDdo;
import org.ihtsdo.otf.tcc.ddo.fetchpolicy.RefexPolicy;
import org.ihtsdo.otf.tcc.ddo.fetchpolicy.RelationshipPolicy;
import org.ihtsdo.otf.tcc.ddo.store.FxTerminologyStoreDI;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link Stage} which can be used to show a concept detail view .
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a> 
 * @author ocarlsen
 */
@Service @Named(value=SharedServiceNames.LEGACY_STYLE)
@PerLookup
public class ConceptView implements PopupConceptViewI {

	private final Logger LOG = LoggerFactory.getLogger(this.getClass());
	private final ConceptViewController controller;

	private ConceptView() throws IOException {
		//This is for HK2 to construct...
		super();

		// Load from FXML.
		URL resource = this.getClass().getResource("ConceptView.fxml");
		FXMLLoader loader = new FXMLLoader(resource);
		loader.load();
		controller = loader.getController();
	}

	public void setConcept(ConceptChronicleDdo concept) {
		// Make sure in application thread.
		FxUtils.checkFxUserThread();
		controller.setConcept(concept);
	}
	
	/**
	 * @see gov.va.isaac.interfaces.gui.views.commonFunctionality.ConceptViewI#setConcept(java.util.UUID)
	 */
	@Override
	public void setConcept(UUID conceptUUID)
	{
		Task<ConceptChronicleDdo> task = new Task<ConceptChronicleDdo>()
		{
			@Override
			protected ConceptChronicleDdo call() throws Exception
			{
				LOG.info("Loading concept with UUID " + conceptUUID);
				ConceptChronicleDdo concept = ExtendedAppContext.getService(FxTerminologyStoreDI.class).getFxConcept(conceptUUID, OTFUtility.getViewCoordinateAllowInactive(),
						RefexPolicy.NONE, RelationshipPolicy.ORIGINATING_RELATIONSHIPS);
				LOG.info("Finished loading concept with UUID " + conceptUUID);

				return concept;
			}

			@Override
			protected void succeeded()
			{
				try
				{
					ConceptChronicleDdo result = this.getValue();
					setConcept(result);
				}
				catch (Exception e)
				{
					String title = "Unexpected error loading concept with UUID " + conceptUUID;
					String msg = e.getClass().getName();
					LOG.error(title, e);
					AppContext.getCommonDialogs().showErrorDialog(title, msg, e.getMessage());
				}
			}

			@Override
			protected void failed()
			{
				Throwable ex = getException();
				String title = "Unexpected error loading concept with UUID " + conceptUUID;
				String msg = ex.getClass().getName();
				LOG.error(title, ex);
				AppContext.getCommonDialogs().showErrorDialog(title, msg, ex.getMessage());
			}
		};

		Utility.execute(task);
	}

	/**
	 * @see gov.va.isaac.interfaces.gui.views.commonFunctionality.ConceptViewI#setConcept(int)
	 */
	@Override
	public void setConcept(int conceptNid)
	{
		Task<ConceptChronicleDdo> task = new Task<ConceptChronicleDdo>()
		{
			@Override
			protected ConceptChronicleDdo call() throws Exception
			{
				LOG.info("Loading concept with nid " + conceptNid);
				ConceptChronicleDdo concept = ExtendedAppContext.getService(FxTerminologyStoreDI.class).getFxConcept(ExtendedAppContext.getDataStore().getUuidPrimordialForNid(conceptNid), 
						OTFUtility.getViewCoordinateAllowInactive(),
						RefexPolicy.NONE, RelationshipPolicy.ORIGINATING_RELATIONSHIPS);
				LOG.info("Finished loading concept with nid " + conceptNid);
				return concept;
			}

			@Override
			protected void succeeded()
			{
				try
				{
					ConceptChronicleDdo result = this.getValue();
					if (result == null)
					{
						throw new Exception("Failed to load concept");
					}
					else
					{
						setConcept(result);
					}
				}
				catch (Exception e)
				{
					String title = "Unexpected error loading concept with nid " + conceptNid;
					String msg = e.getClass().getName();
					LOG.error(title, e);
					AppContext.getCommonDialogs().showErrorDialog(title, msg, e.getMessage());
				}
			}

			@Override
			protected void failed()
			{
				Throwable ex = getException();
				String title = "Unexpected error loading concept with nid " + conceptNid;
				String msg = ex.getClass().getName();
				LOG.error(title, ex);
				AppContext.getCommonDialogs().showErrorDialog(title, msg, ex.getMessage());
			}
		};
		Utility.execute(task);
	}

	/**
	 * @see gov.va.isaac.interfaces.gui.views.PopupViewI#showView(javafx.stage.Window)
	 */
	@Override
	public void showView(Window parent)
	{
		Stage s = new Stage();
		s.initOwner(parent);
		s.initModality(Modality.NONE);
		s.initStyle(StageStyle.DECORATED);

		s.setScene(new Scene(getView()));
		s.getScene().getStylesheets().add(ConceptView.class.getResource("/isaac-shared-styles.css").toString());
		s.getIcons().add(Images.CONCEPT_VIEW.getImage());

		// Title will change after concept is set.
		s.titleProperty().bind(controller.getTitle());
		
		s.onHiddenProperty().set((eventHandler) ->
		{
			controller.viewDiscarded();
			s.setScene(null);
		});
		
		s.show();
		//doesn't come to the front unless you do this (on linux, at least)
		Platform.runLater(() -> {s.toFront();});
	}

	/**
	 * @see gov.va.isaac.interfaces.gui.views.ViewI#getView()
	 */
	@Override
	public Region getView()
	{
		return controller.getRootNode();
	}

	@Override
	public UUID getConceptUuid() {
		return controller.getConceptUuid();
	}

	@Override
	public int getConceptNid() {
		return controller.getConceptNid();
	}

	@Override
	public void setViewMode(ConceptViewMode mode) {
		// Not Implemented in ConceptView
	}

	@Override
	public ConceptViewMode getViewMode() {
		// Not Implemented in ConceptView
		return null;
	}
	
	@Override
	public void viewDiscarded()
	{
		controller.viewDiscarded();
	}
}
