package gov.va.isaac.interfaces.gui.views.commonFunctionality;

import gov.vha.isaac.ochre.api.State;
import gov.vha.isaac.ochre.api.coordinate.PremiseType;

import java.util.Set;
import java.util.UUID;

public interface ViewCoordinatePreferencesPluginViewI extends PreferencesPluginViewI {
	public abstract Set<State> getCurrentStatuses();
	public abstract Long getCurrentTime();
	public abstract PremiseType getCurrentStatedInferredOption();
	public abstract UUID getCurrentPath();
	public abstract Set<UUID> getCurrentSelectedModules();
}