package gov.va.isaac.gui.preferences;

import gov.vha.isaac.ochre.api.State;
import gov.vha.isaac.ochre.api.coordinate.PremiseType;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

public interface PreferencesPersistenceI {

	// ViewCoordinate
	public UUID getPath();
	public PremiseType getStatedInferredOption();
	public Long getTime();
	public Set<State> getStatuses();
	public Set<UUID> getModules();
	
	public void save() throws IOException;
}