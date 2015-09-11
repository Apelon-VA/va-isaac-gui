package gov.va.isaac.gui.dialog;

import javafx.stage.Window;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;

import org.controlsfx.control.PopOver;

public class DetachablePopOverHelper {
	private DetachablePopOverHelper() {}
	
	public static void showDetachachablePopOver(Window window, String titleText, Node nodeToDisplay) {
		newDetachachablePopover(titleText, nodeToDisplay).show(window);
	}
	public static void showDetachachablePopOver(Window window, String titleText, Node nodeToDisplay, double x, double y) {
		newDetachachablePopover(titleText, nodeToDisplay).show(window, x, y);
	}

	public static void showDetachachablePopOver(Region anchor, String titleText, Node nodeToDisplay) {
		PopOver po = newDetachachablePopover(titleText, nodeToDisplay);
		
		Point2D point = anchor.localToScreen(anchor.getWidth(), -32);
		po.show(anchor.getScene().getWindow(), point.getX(), point.getY());
	}
	
	public static PopOver newDetachachablePopover(String titleText, Node nodeToDisplay) {
		BorderPane bp = new BorderPane();
		
		Label title = new Label(titleText);
		title.setMaxWidth(Double.MAX_VALUE);
		title.setAlignment(Pos.CENTER);
		title.setPadding(new Insets(10));
		title.getStyleClass().add("boldLabel");
		title.getStyleClass().add("headerBackground");
		
		bp.setTop(title);
		bp.setCenter(nodeToDisplay);

		PopOver po = new PopOver();
		po.setContentNode(bp);
		po.setAutoHide(true);
		po.detachedTitleProperty().set(titleText);
		po.detachedProperty().addListener((change) ->
		{
			po.setAutoHide(false);
		});
		
		return po;
	}
}
