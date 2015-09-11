package gov.va.isaac.gui.dialog;

import javafx.stage.Window;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;

import org.controlsfx.control.PopOver;

public class DetachablePopOverHelper {
	private DetachablePopOverHelper() {}

	public static void showDetachachablePopOver(Region anchor, PopOver popOver) {		
		Point2D point = anchor.localToScreen(anchor.getWidth(), -32);
		popOver.show(anchor.getScene().getWindow(), point.getX(), point.getY());
	}
	
	public static PopOver newDetachachablePopover(String titleText, Node nodeToDisplay) {
		return newDetachachablePopover(titleText, nodeToDisplay, new BorderPane());
	}
	public static PopOver newDetachachablePopover(String titleText, Node nodeToDisplay, BorderPane borderPane) {		
		Label title = new Label(titleText);
		title.setMaxWidth(Double.MAX_VALUE);
		title.setAlignment(Pos.CENTER);
		title.setPadding(new Insets(10));
		title.getStyleClass().add("boldLabel");
		title.getStyleClass().add("headerBackground");
		
		borderPane.setTop(title);
		borderPane.setCenter(nodeToDisplay);

		PopOver po = new PopOver();
		po.setContentNode(borderPane);
		po.setAutoHide(true);
		po.detachedTitleProperty().set(titleText);
		po.detachedProperty().addListener((change) ->
		{
			po.setAutoHide(false);
		});

		return po;
	}

	public static PopOver newDetachachablePopoverWithCloseButton(String titleText, Node nodeToDisplay) {	
		BorderPane borderPane = new BorderPane();
		
		PopOver popOver = newDetachachablePopover(titleText, nodeToDisplay, borderPane);
		
		Button closeButton = new Button("Close");
		closeButton.setOnAction((event) -> {
			popOver.hide();
		});
		
		HBox lowerPanel = new HBox();
		lowerPanel.setAlignment(Pos.BOTTOM_RIGHT);
		lowerPanel.setPadding(new Insets(10, 10, 10, 10));
		lowerPanel.getChildren().add(closeButton);
		borderPane.setBottom(lowerPanel);
		
		return popOver;
	}
}
