package gov.va.isaac.gui.preferences.plugins;

import gov.va.isaac.AppContext;
import gov.va.isaac.ExtendedAppContext;
import gov.va.isaac.config.profiles.UserProfile;
import gov.va.isaac.config.profiles.UserProfileManager;
import gov.va.isaac.config.users.InvalidUserException;
import gov.va.isaac.interfaces.PreferencesPersistenceI;
import gov.va.isaac.interfaces.gui.views.commonFunctionality.ViewCoordinatePreferencesPluginViewI;
import gov.vha.isaac.ochre.api.State;
import gov.vha.isaac.ochre.api.coordinate.PremiseType;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ViewCoordinatePreferencesUserProfilePersistenceInterface implements PreferencesPersistenceI {
	private final Logger LOG = LoggerFactory.getLogger(this.getClass());

	private final ViewCoordinatePreferencesPluginViewI view;
	
	public ViewCoordinatePreferencesUserProfilePersistenceInterface(ViewCoordinatePreferencesPluginViewI ctrl) {
		this.view = ctrl;
	}

	@Override
	public UUID getPath() {
		UserProfile loggedIn = ExtendedAppContext.getCurrentlyLoggedInUserProfile();
		return loggedIn.getViewCoordinatePath();
	}

	@Override
	public PremiseType getStatedInferredOption() {
		UserProfile loggedIn = ExtendedAppContext.getCurrentlyLoggedInUserProfile();
		return loggedIn.getStatedInferredPolicy();
	}

	@Override
	public Long getTime() {
		UserProfile loggedIn = ExtendedAppContext.getCurrentlyLoggedInUserProfile();
		return loggedIn.getViewCoordinateTime();
	}

	@Override
	public Set<State> getStatuses() {
		UserProfile loggedIn = ExtendedAppContext.getCurrentlyLoggedInUserProfile();
		return loggedIn.getViewCoordinateStatuses();
	}

	@Override
	public Set<UUID> getModules() {
		UserProfile loggedIn = ExtendedAppContext.getCurrentlyLoggedInUserProfile();
		return loggedIn.getViewCoordinateModules();
	}
	
	@Override
	public void save() throws IOException {
		LOG.debug("Saving ViewCoordinatePreferencesPluginView data");
		UserProfile loggedIn = ExtendedAppContext.getCurrentlyLoggedInUserProfile();

		//Path Property
		LOG.debug("Setting stored VC path (currently \"{}\") to {}", getPath(), view.getCurrentPath()); 
		loggedIn.setViewCoordinatePath(view.getCurrentPath());

		//Stated/Inferred Policy
		LOG.debug("Setting stored VC StatedInferredPolicy (currently \"{}\") to {}", getStatedInferredOption(), view.getCurrentStatedInferredOption()); 
		loggedIn.setStatedInferredPolicy(view.getCurrentStatedInferredOption());

		//Time Property
		LOG.debug("Setting stored VC time to :" + view.getCurrentTime());
		loggedIn.setViewCoordinateTime(view.getCurrentTime());

		//Statuses Property
		LOG.debug("Setting stored VC statuses to :" + view.getCurrentStatuses());
		loggedIn.setViewCoordinateStatuses(view.getCurrentStatuses());

		//Modules Property
		LOG.debug("Setting stored VC modules to :" + view.getCurrentSelectedModules());
		loggedIn.setViewCoordinateModules(view.getCurrentSelectedModules());

		try {
			AppContext.getService(UserProfileManager.class).saveChanges(loggedIn);
		} catch (InvalidUserException e) {
			String msg = "Caught " + e.getClass().getName() + " " + e.getLocalizedMessage() + " attempting to save UserProfile for " + getClass().getName();

			LOG.error(msg, e);
			throw new IOException(msg, e);
		}
	}
}