package gov.va.isaac.gui.util;

import gov.va.isaac.util.Utility;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;


public class HeapStatusBar extends StackPane
{
	private Runtime runtime;
	private Text text = new Text();
	private ProgressBar progress = new ProgressBar();

	public HeapStatusBar()
	{
		super();
		runtime = Runtime.getRuntime();
		getChildren().add(progress);
		getChildren().add(text);
		setMinWidth(200);
		update();
		Utility.scheduleWithFixedDelay(() -> Platform.runLater(() -> update()), 1, 1, TimeUnit.SECONDS);
		Tooltip.install(text, new Tooltip("Memory Usage"));
	}

	private void update()
	{
		long free = runtime.freeMemory();
		long total = runtime.totalMemory();
		progress.setProgress((double) (total - free) / (double) total);
		text.setText(bytesToMB(total - free) + " of " + bytesToMB(total));
		progress.setMaxWidth(Double.MAX_VALUE);
	}
	
	private String bytesToMB(long input)
	{
		return (input / 1024 / 1024) + "M";
	}
}
