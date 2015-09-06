package gov.va.isaac.gui.conceptview;

import gov.va.isaac.gui.util.Images;
import gov.va.isaac.interfaces.gui.constants.ConceptViewMode;
import gov.va.isaac.interfaces.gui.constants.SharedServiceNames;
import gov.va.isaac.interfaces.gui.views.EmbeddableViewI;
import gov.va.isaac.interfaces.gui.views.commonFunctionality.PopupConceptViewI;
import gov.va.isaac.util.Utility;
import gov.vha.isaac.ochre.api.component.concept.ConceptSnapshot;
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
import javax.inject.Named;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;

@Service @Named(value=SharedServiceNames.DIAGRAM_STYLE)
@PerLookup
public class ConceptView implements EmbeddableViewI, PopupConceptViewI  {

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

	@Override
	public void setConcept(int conceptId) {
		Utility.execute(() -> controller.setConcept(conceptId));
	}

	@Override
	public void setConcept(UUID conceptUuid) {
		Utility.execute(() -> controller.setConcept(conceptUuid));
	}
	@Override
	public void viewDiscarded() {
		// TODO need to stop any background operations, dispose of any javaFX hooks that might cause leaks
	}
	@Override
	public UUID getConceptUuid()
	{
		ConceptSnapshot cs = controller.getConceptSnapshot();
		return (cs == null ? null : cs.getPrimordialUuid());
	}
	@Override
	public int getConceptNid()
	{
		ConceptSnapshot cs = controller.getConceptSnapshot();
		return (cs == null ? Integer.MIN_VALUE : cs.getNid());
	}
	@Override
	public void setViewMode(ConceptViewMode mode)
	{
		throw new UnsupportedOperationException();
	}
	@Override
	public ConceptViewMode getViewMode()
	{
		throw new UnsupportedOperationException();
	}
}
