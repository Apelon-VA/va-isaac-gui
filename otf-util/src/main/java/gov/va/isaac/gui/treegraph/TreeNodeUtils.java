package gov.va.isaac.gui.treegraph;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.scene.layout.Region;
import javafx.scene.control.Labeled;
import javafx.scene.text.Font;

public class TreeNodeUtils {
	private static final Logger LOG = LoggerFactory.getLogger(TreeNodeUtils.class);

	private TreeNodeUtils() {};
	
	public static double DEFAULT_FONT_SIZE = 12.0;
	
	public static void configureFxNode(Region fxNode, int width, int height) {
		configureFxNode(fxNode, width, height, DEFAULT_FONT_SIZE);
	}
	public static void configureFxNode(Region fxNode, int width, int height, Integer fontSize) {
		configureFxNode(fxNode, width, height, fontSize != null ? 1.0 * fontSize : null);
	}
	public static void configureFxNode(Region fxNode, int width, int height, Double fontSize) {
		fxNode.setMaxHeight(height);
		fxNode.setMaxWidth(width);
		fxNode.setPrefHeight(height);
		fxNode.setPrefWidth(width);
		fxNode.setMinHeight(height);
		fxNode.setMinWidth(width);

		if (fontSize != null && fxNode instanceof Labeled) {
			Labeled labeled = (Labeled)fxNode;
			labeled.setFont(Font.font(fontSize));
		}
	}
}
