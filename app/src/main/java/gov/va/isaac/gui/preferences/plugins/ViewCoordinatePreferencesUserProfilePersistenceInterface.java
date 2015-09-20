package gov.va.isaac.gui.preferences.plugins;

import gov.va.isaac.AppContext;
import gov.va.isaac.ExtendedAppContext;
import gov.va.isaac.config.generated.StatedInferredOptions;
import gov.va.isaac.config.profiles.UserProfile;
import gov.va.isaac.config.profiles.UserProfileManager;
import gov.va.isaac.config.users.InvalidUserException;
import gov.va.isaac.gui.preferences.PreferencesPersistenceI;
import gov.va.isaac.util.ViewCoordinateComponents;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

import org.ihtsdo.otf.tcc.api.coordinate.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ViewCoordinatePreferencesUserProfilePersistenceInterface implements PreferencesPersistenceI {
	private final Logger LOG = LoggerFactory.getLogger(this.getClass());

	private final ViewCoordinatePreferencesPluginViewController ctrl;
	
	public ViewCoordinatePreferencesUserProfilePersistenceInterface(ViewCoordinatePreferencesPluginViewController ctrl) {
		this.ctrl = ctrl;
	}

	@Override
	public UUID getPath() {
		UserProfile loggedIn = ExtendedAppContext.getCurrentlyLoggedInUserProfile();
		return loggedIn.getViewCoordinatePath();
	}

	@Override
	public StatedInferredOptions getStatedInferredOption() {
		UserProfile loggedIn = ExtendedAppContext.getCurrentlyLoggedInUserProfile();
		return loggedIn.getStatedInferredPolicy();
	}

	@Override
	public Long getTime() {
		UserProfile loggedIn = ExtendedAppContext.getCurrentlyLoggedInUserProfile();
		return loggedIn.getViewCoordinateTime();
	}

	@Override
	public Set<Status> getStatuses() {
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

//		currentStatedInferredOptionProperty.get(),
//		currentPathProperty.get(),
//		currentStatusesProperty.get(),
//		currentTimeProperty.get(),
//		selectedModules));
		
		
		//Path Property
		LOG.debug("Setting stored VC path (currently \"{}\") to {}", getPath(), ctrl.currentPathProperty().get()); 
		loggedIn.setViewCoordinatePath(ctrl.currentPathProperty().get());

		//Stated/Inferred Policy
		LOG.debug("Setting stored VC StatedInferredPolicy (currently \"{}\") to {}", getStatedInferredOption(), ctrl.currentStatedInferredOptionProperty().get()); 
		loggedIn.setStatedInferredPolicy(ctrl.currentStatedInferredOptionProperty().get());

		//Time Property
		LOG.debug("Setting stored VC time to :" + ctrl.currentTimeProperty().get());
		loggedIn.setViewCoordinateTime(ctrl.currentTimeProperty().get());

		//Statuses Property
		LOG.debug("Setting stored VC statuses to :" + ctrl.currentStatusesProperty().get());
		loggedIn.setViewCoordinateStatuses(ctrl.currentStatusesProperty().get());

		//Modules Property
		LOG.debug("Setting stored VC modules to :" + ctrl.getCurrentSelectedModules());
		loggedIn.setViewCoordinateModules(ctrl.getCurrentSelectedModules());

		try {
			AppContext.getService(UserProfileManager.class).saveChanges(loggedIn);
		} catch (InvalidUserException e) {
			String msg = "Caught " + e.getClass().getName() + " " + e.getLocalizedMessage() + " attempting to save UserProfile for " + getClass().getName();

			LOG.error(msg, e);
			throw new IOException(msg, e);
		}
	}
}