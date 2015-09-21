package gov.va.isaac.interfaces.gui.views.commonFunctionality;

import gov.va.isaac.interfaces.gui.MenuItemI;
import gov.va.isaac.interfaces.gui.views.IsaacViewWithMenusI;
import gov.va.isaac.interfaces.gui.views.PopupViewI;

import java.util.List;

import org.jvnet.hk2.annotations.Contract;

import javafx.stage.Window;

@Contract
public interface PreferencesViewI extends PopupViewI, IsaacViewWithMenusI {

	/**
	 * Set/reset window title
	 * 
	 * @param value String title
	 */
	public void setTitle(String value);
	
	/**
	 * 
	 * Allow specification by name of plugins to display before calling aboutToShow().
	 * If no plugin names specified, then all available plugins will be displayed.
	 * Throws RuntimeException if called after aboutToShow()
	 * 
	 * @param requiredPluginName a required plugin name
	 * @param optionalPluginNames optional additional plugin names
	 * 
	 */
	public abstract void setRequestedPlugins(String requiredPluginName,
			String... optionalPluginNames);

	/**
	 * Load instances of all available PreferencesPluginViewI classes,
	 * unless requestedPluginNames is non-empty, in which case ignore non-specified PreferencesPluginViewI classes.
	 * PreferencesPersistenceI may be reset in plugins after this call, but cannot be reset after aboutToShow()
	 * 
	 * loadPlugins() only performs load on first call, subsequently performing noop.
	 */
	public abstract void loadPlugins();

	public abstract PreferencesPluginViewI getPlugin(String name);

	/* (non-Javadoc)
	 * @see gov.va.isaac.interfaces.gui.views.PopupViewI#showView(javafx.stage.Window)
	 */
	public abstract void showView(Window parent);

	/* (non-Javadoc)
	 * @see gov.va.isaac.interfaces.gui.views.IsaacViewWithMenusI#getMenuBarMenus()
	 */
	public abstract List<MenuItemI> getMenuBarMenus();

}