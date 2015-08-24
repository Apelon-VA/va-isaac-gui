package gov.va.isaac.gui.conceptview;

import java.io.IOException;
import java.net.URL;
import java.util.UUID;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import gov.va.isaac.gui.util.Images;
import gov.va.isaac.interfaces.gui.views.PopupViewI;
import gov.va.isaac.interfaces.gui.views.commonFunctionality.ConceptView2I;
import gov.va.isaac.util.Utility;

import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;

@Service
@PerLookup
public class ConceptView implements ConceptView2I {

	private ConceptViewController controller;

	public ConceptView() throws IOException {
		super();
		URL resource = ConceptViewController.class.getResource("ConceptView.fxml");
		FXMLLoader loader = new FXMLLoader(resource);
		loader.load();
		controller = loader.getController();
	}
	@Override
	public void showView(Window parent) {
		Stage s = new Stage();
		s.initOwner(parent);
		s.initModality(Modality.NONE);
		s.initStyle(StageStyle.DECORATED);

		s.setScene(new Scene(controller.getRoot()));
		s.getIcons().add(Images.CONCEPT_VIEW.getImage());
		s.getScene().getStylesheets().add(ConceptViewController.class.getResource("/isaac-shared-styles.css").toString());
		
		// Title will change after concept is set.
		s.titleProperty().bind(controller.getTitle());
		s.show();
		//doesn't come to the front unless you do this (on linux, at least)
		Platform.runLater(() -> {s.toFront();});
	} 
	
	@Override
	public Region getView() {
		return controller.getRoot();
	}
	
	/* (non-Javadoc)
	 * @see gov.va.isaac.gui.conceptview.ConceptView2I#getConcept()
	 */
	//@Override
	public Integer getConcept() {
		return controller.getConcept();
	}
	/* (non-Javadoc)
	 * @see gov.va.isaac.gui.conceptview.ConceptView2I#setConcept(int)
	 */
	//@Override
	public void setConcept(int conceptId) {
		Utility.execute(() -> controller.setConcept(conceptId));
	}
	/* (non-Javadoc)
	 * @see gov.va.isaac.gui.conceptview.ConceptView2I#setConcept(java.util.UUID)
	 */
	//@Override
	public void setConcept(UUID conceptUuid) {
		Utility.execute(() -> controller.setConcept(conceptUuid));
	}
	@Override
	public void viewDiscarded() {
		// TODO Auto-generated method stub
	}
}
