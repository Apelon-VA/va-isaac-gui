package gov.va.isaac.gui.preferences.plugins;

import gov.va.isaac.interfaces.gui.views.commonFunctionality.PreferencesPluginViewI;
import gov.vha.isaac.ochre.api.State;
import gov.vha.isaac.ochre.api.coordinate.PremiseType;

import java.util.UUID;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlySetProperty;

public interface ViewCoordinatePreferencesPluginViewI extends PreferencesPluginViewI {
	public abstract ReadOnlySetProperty<State> currentStatusesProperty();
	public abstract ReadOnlyObjectProperty<Long> currentTimeProperty();
	public abstract ReadOnlyObjectProperty<PremiseType> currentStatedInferredOptionProperty();
	public abstract ReadOnlyObjectProperty<UUID> currentPathProperty();
	public abstract ReadOnlySetProperty<UUID> currentSelectedModulesProperty();
}