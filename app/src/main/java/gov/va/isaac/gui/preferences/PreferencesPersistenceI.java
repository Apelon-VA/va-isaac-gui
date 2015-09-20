package gov.va.isaac.gui.preferences;

import gov.va.isaac.config.generated.StatedInferredOptions;
import gov.vha.isaac.ochre.api.State;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

public interface PreferencesPersistenceI {

	// ViewCoordinate
	public UUID getPath();
	public StatedInferredOptions getStatedInferredOption();
	public Long getTime();
	public Set<State> getStatuses();
	public Set<UUID> getModules();
	
	public void save() throws IOException;
}