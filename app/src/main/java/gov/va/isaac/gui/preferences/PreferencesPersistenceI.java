package gov.va.isaac.gui.preferences;

import gov.va.isaac.config.generated.StatedInferredOptions;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

import org.ihtsdo.otf.tcc.api.coordinate.Status;

public interface PreferencesPersistenceI {

	// ViewCoordinate
	public UUID getPath();
	public StatedInferredOptions getStatedInferredOption();
	public Long getTime();
	public Set<Status> getStatuses();
	public Set<UUID> getModules();
	
	public void save() throws IOException;
}